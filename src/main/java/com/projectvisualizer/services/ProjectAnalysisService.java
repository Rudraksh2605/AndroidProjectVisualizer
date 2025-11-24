package com.projectvisualizer.services;

import com.projectvisualizer.parsers.*;
import com.projectvisualizer.model.*;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectAnalysisService {
    private JavaFileParser javaParser;
    private KotlinParser kotlinParser;
    private XmlParser xmlParser;
    private AndroidManifestParser manifestParser;
    private Map<String, String> activityLayoutMap;


    public ProjectAnalysisService() {
        this.xmlParser = new XmlParser();
        this.manifestParser = new AndroidManifestParser();
    }

    public AnalysisResult analyzeProject(File projectDir) {
        AnalysisResult result = new AnalysisResult();

        try {
            activityLayoutMap = parseAndroidManifest(projectDir);
            this.javaParser = new JavaFileParser(activityLayoutMap);
            this.kotlinParser = new KotlinParser(activityLayoutMap);
            List<CodeComponent> allComponents = new ArrayList<>();
            scanAndParseDirectory(projectDir, allComponents);
            resolveDependencies(allComponents);
            List<NavigationFlow> navigationFlows = extractNavigationFlows(allComponents);
            List<UserFlowComponent> userFlows = extractUserFlows(allComponents, navigationFlows);
            Map<String, List<CodeComponent>> categorizedComponents = categorizeComponents(allComponents);
            result.setComponents(allComponents);
            result.setNavigationFlows(navigationFlows);
            result.setUserFlows(userFlows);
            result.setActivityLayoutMap(activityLayoutMap);
            result.setCategorizedComponents(categorizedComponents);


        } catch (Exception e) {
            result.setError(e.getMessage());
        }

        return result;
    }

    private Map<String, String> parseAndroidManifest(File projectDir) {
        File manifestFile = findAndroidManifest(projectDir);
        if (manifestFile != null && manifestFile.exists()) {
            try {
                return manifestParser.parseManifest(manifestFile);
            } catch (Exception e) {
                System.err.println("Failed to parse AndroidManifest: " + e.getMessage());
            }
        }
        return new HashMap<>();
    }

    private void scanAndParseDirectory(File dir, List<CodeComponent> components) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip build directories and hidden folders
                if (!file.getName().startsWith(".") && !file.getName().equals("build")) {
                    scanAndParseDirectory(file, components);
                }
            } else {
                parseFile(file, components);
            }
        }
    }

    private void parseFile(File file, List<CodeComponent> components) {
        if (file == null) return;

        try {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".java")) {
                if (javaParser != null) {
                    List<CodeComponent> parsedComponents = javaParser.parse(file);
                    if (parsedComponents != null) {
                        components.addAll(parsedComponents);
                    }
                } else {
                    System.err.println("Java parser not initialized before parsing " + file.getPath());
                }
            } else if (fileName.endsWith(".kt")) {
                if (kotlinParser != null) {
                    List<CodeComponent> parsedComponents = kotlinParser.parse(file);
                    if (parsedComponents != null) {
                        components.addAll(parsedComponents);
                    }
                } else {
                    System.err.println("Kotlin parser not initialized before parsing " + file.getPath());
                }
            } else if (fileName.endsWith(".xml")) {
                List<CodeComponent> parsedComponents = xmlParser.parse(file);
                if (parsedComponents != null) {
                    components.addAll(parsedComponents);
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing file " + file.getName() + ": " + e.getMessage());
        }
    }

    private List<NavigationFlow> extractNavigationFlows(List<CodeComponent> components) {
        List<NavigationFlow> allFlows = new ArrayList<>();
        if (components == null || components.isEmpty()) return allFlows;

        IntentAnalyzer intentAnalyzer = new IntentAnalyzer();

        for (CodeComponent component : components) {
            if (component == null || component.getFilePath() == null) {
                continue; // Skip null components
            }

            String lang = component.getLanguage();
            if (lang == null) continue;

            // Analyze both Java and Kotlin source files for intent/navigation flows
            if (lang.equalsIgnoreCase("java") || lang.equalsIgnoreCase("kotlin") || lang.equalsIgnoreCase("kt")) {
                try {
                    File sourceFile = new File(component.getFilePath());
                    if (sourceFile.exists()) {
                        List<NavigationFlow> flows = intentAnalyzer.analyzeIntentFlows(sourceFile);
                        if (flows != null && !flows.isEmpty()) {
                            allFlows.addAll(flows);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error analyzing navigation flows for " +
                            (component.getName() != null ? component.getName() : "unknown") +
                            ": " + e.getMessage());
                }
            }
        }
        return allFlows;
    }

    private List<UserFlowComponent> extractUserFlows(List<CodeComponent> components,
                                                     List<NavigationFlow> navigationFlows) {
        ScreenFlowDetector flowDetector = new ScreenFlowDetector();
        return flowDetector.detectUserFlows(components, navigationFlows);
    }

    private File findAndroidManifest(File projectDir) {
        if (projectDir == null) return null;

        // Look for AndroidManifest.xml in common locations
        File[] possiblePaths = {
                new File(projectDir, "app/src/main/AndroidManifest.xml"),
                new File(projectDir, "src/main/AndroidManifest.xml"),
                new File(projectDir, "AndroidManifest.xml")
        };

        for (File path : possiblePaths) {
            if (path.exists()) {
                return path;
            }
        }
        return null;
    }

    private void resolveDependencies(List<CodeComponent> allComponents) {
        if (allComponents == null || allComponents.isEmpty()) return;

        // Build a map of id (usually FQN) -> component. If duplicate ids exist, keep the first seen.
        Map<String, CodeComponent> byId = allComponents.stream()
                .filter(c -> c != null && c.getId() != null)
                .collect(Collectors.toMap(CodeComponent::getId, Function.identity(), (existing, replacement) -> existing));

        // Also build a fallback map of simple class name -> component (first occurrence wins)
        Map<String, CodeComponent> bySimpleName = new HashMap<>();
        for (CodeComponent c : allComponents) {
            if (c == null) continue;
            String id = c.getId();
            String simple = null;
            if (id != null && id.contains(".")) {
                simple = id.substring(id.lastIndexOf('.') + 1);
            } else if (c.getName() != null) {
                simple = c.getName();
            }
            if (simple != null) {
                bySimpleName.putIfAbsent(simple, c);
            }
        }

        for (CodeComponent component : allComponents) {
            if (component == null) continue;

            List<CodeComponent> originalDeps = component.getDependencies();
            if (originalDeps == null || originalDeps.isEmpty()) {
                component.setDependencies(Collections.emptyList());
                continue;
            }

            List<CodeComponent> resolvedDependencies = new ArrayList<>();

            for (CodeComponent dependency : originalDeps) {
                if (dependency == null) continue;

                // Try resolve by id (FQN) first
                CodeComponent resolved = null;
                if (dependency.getId() != null) {
                    resolved = byId.get(dependency.getId());
                }

                // If not found, try by simple name derived from dep id or name
                if (resolved == null) {
                    String key = null;
                    String depId = dependency.getId();
                    if (depId != null) {
                        key = depId.contains(".") ? depId.substring(depId.lastIndexOf('.') + 1) : depId;
                    }
                    if ((key == null || key.isEmpty()) && dependency.getName() != null) {
                        key = dependency.getName();
                    }
                    if (key != null) {
                        resolved = bySimpleName.get(key);
                    }
                }

                if (resolved != null && resolved != component) {
                    resolvedDependencies.add(resolved);
                } else {
                    // Keep the stub but try to detect its layer for coloring/grouping
                    detectComponentLayer(dependency);
                    resolvedDependencies.add(dependency);
                }
            }

            component.setDependencies(resolvedDependencies);
        }
    }

    private void detectComponentLayer(CodeComponent component) {
        if (component == null || component.getName() == null) return;

        String name = component.getName().toLowerCase();
        if (name.endsWith("repository") || name.endsWith("datasource") || name.endsWith("dao")) {
            component.setLayer("Data");
        } else if (name.endsWith("viewmodel") || name.endsWith("presenter") || name.endsWith("usecase")) {
            component.setLayer("Business Logic");
        } else if (name.endsWith("activity") || name.endsWith("fragment") || name.endsWith("adapter")) {
            component.setLayer("UI");
        } else {
            // leave as unknown; could add more heuristics here
        }
    }

    private String detectComponentCategory(CodeComponent component) {
        return ComponentCategorizer.detectCategory(component);
    }

    public Map<String, List<CodeComponent>> categorizeComponents(List<CodeComponent> components) {
        Map<String, List<CodeComponent>> categorized = new HashMap<>();

        for (String category : Arrays.asList("UI", "DATA_MODEL", "BUSINESS_LOGIC", "NAVIGATION", "UNKNOWN")) {
            categorized.put(category, new ArrayList<>());
        }

        for (CodeComponent component : components) {
            String category = detectComponentCategory(component);
            categorized.get(category).add(component);
        }

        return categorized;
    }

}
