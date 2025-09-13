package com.projectvisualizer.parsers;

import com.projectvisualizer.models.*;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.file.Files;

public class IntentAnalyzer {

    public List<NavigationFlow> analyzeIntentFlows(File javaFile) throws Exception {
        List<NavigationFlow> flows = new ArrayList<>();
        String content = new String(Files.readAllBytes(javaFile.toPath()));

        // Pattern to match Intent creation and startActivity calls
        Pattern intentPattern = Pattern.compile(
                "Intent\\s+(\\w+)\\s*=\\s*new\\s+Intent\\s*\\([^)]*\\)\\s*;?|" +
                        "startActivity\\s*\\([^)]*\\)|" +
                        "startActivityForResult\\s*\\([^)]*\\)"
        );

        Matcher matcher = intentPattern.matcher(content);

        while (matcher.find()) {
            NavigationFlow flow = extractNavigationFlow(matcher.group(), content, javaFile);
            if (flow != null) {
                flows.add(flow);
            }
        }

        return flows;
    }

    private NavigationFlow extractNavigationFlow(String intentCode, String fullContent, File sourceFile) {
        NavigationFlow flow = new NavigationFlow();

        // Extract target activity class
        Pattern targetPattern = Pattern.compile("([\\w.]+\\.class|\"[^\"]+\")");
        Matcher targetMatcher = targetPattern.matcher(intentCode);

        if (targetMatcher.find()) {
            String target = targetMatcher.group();
            flow.setFlowId(UUID.randomUUID().toString());
            flow.setSourceScreenId(extractClassName(sourceFile.getName()));
            flow.setTargetScreenId(extractClassNameFromTarget(target));
            flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);

            // Analyze navigation conditions
            extractNavigationConditions(intentCode, fullContent, flow);

            return flow;
        }

        return null;
    }

    private void extractNavigationConditions(String intentCode, String fullContent, NavigationFlow flow) {
        // Look for putExtra calls that indicate data passing
        Pattern extraPattern = Pattern.compile("putExtra\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*([^)]+)\\)");
        Matcher extraMatcher = extraPattern.matcher(fullContent);

        while (extraMatcher.find()) {
            String key = extraMatcher.group(1);
            String value = extraMatcher.group(2);

            NavigationFlow.NavigationCondition condition =
                    new NavigationFlow.NavigationCondition("DATA_EXTRA", key + "=" + value, false);
            flow.addCondition(condition);
        }

        // Look for conditional navigation (if statements around startActivity)
        String[] lines = fullContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("startActivity") && i > 0) {
                String prevLine = lines[i-1].trim();
                if (prevLine.contains("if") && prevLine.contains("(")) {
                    String condition = extractConditionFromIf(prevLine);
                    if (!condition.isEmpty()) {
                        NavigationFlow.NavigationCondition navCondition =
                                new NavigationFlow.NavigationCondition("CONDITIONAL", condition, true);
                        flow.addCondition(navCondition);
                    }
                }
            }
        }
    }

    private String extractConditionFromIf(String ifStatement) {
        Pattern conditionPattern = Pattern.compile("if\\s*\\(([^)]+)\\)");
        Matcher matcher = conditionPattern.matcher(ifStatement);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractClassName(String fileName) {
        return fileName.replace(".java", "").replace(".kt", "");
    }

    private String extractClassNameFromTarget(String target) {
        if (target.contains(".class")) {
            return target.replace(".class", "").substring(target.lastIndexOf('.') + 1);
        } else if (target.contains("\"")) {
            String className = target.replace("\"", "");
            return className.substring(className.lastIndexOf('.') + 1);
        }
        return target;
    }
}
