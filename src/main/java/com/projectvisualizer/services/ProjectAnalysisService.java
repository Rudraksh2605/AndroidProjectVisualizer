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

            // OPTIMIZED: O(N) dependency resolution
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
            e.printStackTrace();
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
                }
            } else if (fileName.endsWith(".kt")) {
                if (kotlinParser != null) {
                    List<CodeComponent> parsedComponents = kotlinParser.parse(file);
                    if (parsedComponents != null) {
                        components.addAll(parsedComponents);
                    }
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
            if (component == null || component.getFilePath() == null) continue;

            String lang = component.getLanguage();
            if (lang != null && (lang.equalsIgnoreCase("java") || lang.equalsIgnoreCase("kotlin"))) {
                try {
                    File sourceFile = new File(component.getFilePath());
                    if (sourceFile.exists()) {
                        List<NavigationFlow> flows = intentAnalyzer.analyzeIntentFlows(sourceFile);
                        if (flows != null && !flows.isEmpty()) {
                            allFlows.addAll(flows);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error analyzing navigation flows: " + e.getMessage());
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
        File[] possiblePaths = {
                new File(projectDir, "app/src/main/AndroidManifest.xml"),
                new File(projectDir, "src/main/AndroidManifest.xml"),
                new File(projectDir, "AndroidManifest.xml")
        };
        for (File path : possiblePaths) {
            if (path.exists()) return path;
        }
        return null;
    }

    /**
     * OPTIMIZED RESOLUTION ALGORITHM (O(N) Time Complexity)
     * Replaces the O(N^2) nested loop with Map-based lookups.
     */
    private void resolveDependencies(List<CodeComponent> allComponents) {
        if (allComponents == null || allComponents.isEmpty()) return;

        // 1. Build Symbol Tables (O(N))
        Map<String, CodeComponent> idToComponent = new HashMap<>();
        Map<String, CodeComponent> simpleNameToComponent = new HashMap<>();

        for (CodeComponent c : allComponents) {
            if (c.getId() != null) {
                idToComponent.put(c.getId(), c);
            }
            if (c.getName() != null) {
                // If duplicates exist, the last one wins, which is acceptable for simple name fallback
                simpleNameToComponent.put(c.getName(), c);
            }
        }

        // 2. Resolve References (O(N * AvgDeps)) -> Effectively Linear
        for (CodeComponent component : allComponents) {
            List<CodeComponent> unresolvedDeps = component.getDependencies();
            if (unresolvedDeps == null || unresolvedDeps.isEmpty()) continue;

            List<CodeComponent> resolvedDependencies = new ArrayList<>();
            Set<String> processedIds = new HashSet<>(); // Prevent duplicates

            for (CodeComponent dependencyStub : unresolvedDeps) {
                CodeComponent realComponent = null;

                // Strategy A: Exact ID Match
                if (dependencyStub.getId() != null) {
                    realComponent = idToComponent.get(dependencyStub.getId());
                }

                // Strategy B: Simple Name Match (Fallback)
                if (realComponent == null && dependencyStub.getName() != null) {
                    realComponent = simpleNameToComponent.get(dependencyStub.getName());
                }

                // Strategy C: Infer from package (if stub has package info)
                if (realComponent == null && dependencyStub.getId() != null && dependencyStub.getId().contains(".")) {
                    String simpleName = dependencyStub.getId().substring(dependencyStub.getId().lastIndexOf('.') + 1);
                    realComponent = simpleNameToComponent.get(simpleName);
                }

                if (realComponent != null && !realComponent.getId().equals(component.getId())) {
                    if (processedIds.add(realComponent.getId())) {
                        resolvedDependencies.add(realComponent);
                    }
                } else {
                    // It's likely an external library or SDK class not in our source tree
                    // We keep the stub but mark it as External
                    detectComponentLayer(dependencyStub);
                    resolvedDependencies.add(dependencyStub);
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
            // leave as unknown
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