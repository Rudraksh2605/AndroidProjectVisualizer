package com.projectvisualizer.parsers;

import com.projectvisualizer.model.CodeComponent;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, line-by-line Intent navigation parser.
 *
 * Responsibilities:
 * - Scans Java/Kotlin source files line-by-line (no full AST) to detect Intent-based navigation.
 * - Supports explicit intents and common setters (setClass, setClassName, setComponent) and inline startActivity.
 * - Partially supports implicit intents (records ACTION_* as placeholder targets).
 * - Produces an Activity -> Targets mapping (targets are simple names or placeholders).
 */
public class IntentNavigationParser {

    private static final Pattern NEW_INTENT_JAVA = Pattern.compile("new\\s+Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class");
    private static final Pattern NEW_INTENT_KOTLIN = Pattern.compile("Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)::class\\.java\\)");
    private static final Pattern START_ACTIVITY_INLINE_JAVA = Pattern.compile("startActivity\\s*\\(\\s*new\\s+Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class");
    private static final Pattern START_ACTIVITY_GENERIC_KT = Pattern.compile("startActivity\\s*<\\s*([A-Z][a-zA-Z0-9_.$]+)\\s*>");
    private static final Pattern SET_CLASS = Pattern.compile("setClass\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class\\)");
    private static final Pattern SET_CLASS_NAME_TWO_ARGS = Pattern.compile("setClassName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern SET_CLASS_NAME_ONE_ARG = Pattern.compile("setClassName\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern SET_COMPONENT = Pattern.compile("setComponent\\s*\\(\\s*new\\s+ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern COMPONENT_NAME = Pattern.compile("ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern IMPLICIT_ACTION = Pattern.compile("Intent\\.(ACTION_[A-Z_]+)");
    // Note: We don't currently use a full intent var declaration regex; keep logic simple via LHS inference.
    private static final Pattern START_ACTIVITY_VAR = Pattern.compile("startActivity\\s*\\(\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\)");
    private static final Pattern START_ACTIVITY_FOR_RESULT_VAR = Pattern.compile("startActivityForResult\\s*\\(\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*,");

    public Map<String, Set<String>> buildIntentMap(List<CodeComponent> components) {
        Map<String, Set<String>> map = new HashMap<>();

        for (CodeComponent comp : components) {
            if (!isLikelyActivity(comp)) continue;
            if (comp.getFilePath() == null) continue;
            try {
                File file = new File(comp.getFilePath());
                if (!file.exists()) continue;
                List<String> lines = Files.readAllLines(file.toPath());

                // Track varName -> targets detected for that variable
                Map<String, Set<String>> intentVarTargets = new HashMap<>();
                Set<String> directTargets = new LinkedHashSet<>();

                for (String line : lines) {
                    String trimmed = line.trim();

                    // 1) Inline startActivity(new Intent(..., Target.class))
                    collectMatchGroup(START_ACTIVITY_INLINE_JAVA, trimmed).ifPresent(t -> directTargets.add(toSimpleName(t)));

                    // 2) Direct new Intent(context, Target.class)
                    collectMatchGroup(NEW_INTENT_JAVA, trimmed).ifPresent(t -> addToSet(intentVarTargets, inferLhsVarName(trimmed), toSimpleName(t))); // Java
                    collectMatchGroup(NEW_INTENT_KOTLIN, trimmed).ifPresent(t -> addToSet(intentVarTargets, inferLhsVarName(trimmed), toSimpleName(t))); // Kotlin

                    // 3) startActivity<Target>() (Kotlin)
                    collectMatchGroup(START_ACTIVITY_GENERIC_KT, trimmed).ifPresent(t -> directTargets.add(toSimpleName(t)));

                    // 4) setClass / setClassName / setComponent patterns possibly after var creation
                    collectMatchGroup(SET_CLASS, trimmed).ifPresent(t -> addToSet(intentVarTargets, inferReceiverVarName(trimmed), toSimpleName(t)));
                    collectMatchGroup(SET_CLASS_NAME_TWO_ARGS, trimmed).ifPresent(fqn -> addToSet(intentVarTargets, inferReceiverVarName(trimmed), toSimpleName(fqn)));
                    collectMatchGroup(SET_CLASS_NAME_ONE_ARG, trimmed).ifPresent(fqn -> addToSet(intentVarTargets, inferReceiverVarName(trimmed), toSimpleName(fqn)));
                    collectMatchGroup(SET_COMPONENT, trimmed).ifPresent(fqn -> addToSet(intentVarTargets, inferReceiverVarName(trimmed), toSimpleName(fqn)));
                    collectMatchGroup(COMPONENT_NAME, trimmed).ifPresent(fqn -> addToSet(intentVarTargets, inferReceiverVarName(trimmed), toSimpleName(fqn)));

                    // 5) Implicit ACTION_* (record as placeholder)
                    collectMatchGroup(IMPLICIT_ACTION, trimmed).ifPresent(action -> directTargets.add("[Implicit] " + action));

                    // 6) startActivity(var) / startActivityForResult(var, ...)
                    Optional<String> var1 = collectMatchGroup(START_ACTIVITY_VAR, trimmed);
                    Optional<String> var2 = collectMatchGroup(START_ACTIVITY_FOR_RESULT_VAR, trimmed);
                    String usedVar = var1.orElseGet(() -> var2.orElse(null));
                    if (usedVar != null) {
                        Set<String> targets = intentVarTargets.get(usedVar);
                        if (targets != null) directTargets.addAll(targets);
                    }
                }

                if (!directTargets.isEmpty()) {
                    map.put(comp.getName(), directTargets);
                }

            } catch (Exception e) {
                System.err.println("IntentNavigationParser: Failed to parse file: " + comp.getFilePath() + " - " + e.getMessage());
            }
        }
        return map;
    }

    private boolean isLikelyActivity(CodeComponent c) {
        if (c == null) return false;
        if ("Activity".equalsIgnoreCase(c.getComponentType())) return true;
        String layer = c.getLayer();
        if (layer != null && layer.equalsIgnoreCase("UI")) {
            String name = c.getName() != null ? c.getName() : "";
            if (name.endsWith("Activity") || name.endsWith("Screen")) return true;
        }
        // fallback simple heuristic
        String name = c.getName() != null ? c.getName() : "";
        return name.endsWith("Activity");
    }

    private Optional<String> collectMatchGroup(Pattern p, String line) {
        Matcher m = p.matcher(line);
        if (m.find()) {
            String g = null;
            for (int i = 1; i <= m.groupCount(); i++) {
                try {
                    String gi = m.group(i);
                    if (gi != null && !gi.isEmpty()) { g = gi; break; }
                } catch (Exception ignored) {}
            }
            return Optional.ofNullable(g);
        }
        return Optional.empty();
    }

    private void addToSet(Map<String, Set<String>> varMap, String var, String value) {
        if (var == null || var.isEmpty() || value == null || value.isEmpty()) return;
        varMap.computeIfAbsent(var, k -> new LinkedHashSet<>()).add(value);
    }

    private String inferLhsVarName(String line) {
        // Attempts to extract variable name on the left side of '='
        // Examples: Intent intent = new Intent(...)
        //           val intent = Intent(...)
        int eq = line.indexOf('=');
        if (eq > 0) {
            String left = line.substring(0, eq);
            // strip common prefixes and types
            left = left.replace("public", "").replace("private", "").replace("final", "");
            left = left.replace(": Intent", " "); // Kotlin typed var
            left = left.replace("Intent", " ");
            left = left.replace("val", " ").replace("var", " ");
            left = left.trim();
            // keep last token as var name
            String[] parts = left.split("\\s+");
            if (parts.length > 0) return parts[parts.length - 1].trim();
        }
        return null;
    }

    private String inferReceiverVarName(String line) {
        // tries to detect receiver var before '.setClass...' etc.
        int dot = line.indexOf('.') ;
        if (dot > 0) {
            String left = line.substring(0, dot).trim();
            // simple invalid tokens filter
            if (!left.contains(" ") && !left.contains("(") && left.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                return left;
            }
        }
        return null;
    }

    private String toSimpleName(String fqnOrName) {
        if (fqnOrName == null) return null;
        String s = fqnOrName;
        if (s.contains(".")) s = s.substring(s.lastIndexOf('.') + 1);
        if (s.endsWith("$")) s = s.replace("$", "");
        return s;
    }
}
