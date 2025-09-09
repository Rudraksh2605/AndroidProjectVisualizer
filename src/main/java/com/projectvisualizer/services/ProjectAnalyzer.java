package com.projectvisualizer.services;

import com.projectvisualizer.models.*;
import com.projectvisualizer.parsers.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

public class ProjectAnalyzer {
    private  JavaFileParser javaParser;
    private final KotlinParser kotlinParser;
    private final XmlParser xmlParser;
    private final DartParser dartParser;
    private final JavaScriptParser jsParser;
    private final RelationshipBuilder relationshipBuilder;
    private final AndroidManifestParser androidManifestParser;
    private Map<String, String> activityLayoutMap;

    public ProjectAnalyzer() {
        this.javaParser = new JavaFileParser();
        this.kotlinParser = new KotlinParser();
        this.xmlParser = new XmlParser();
        this.dartParser = new DartParser();
        this.jsParser = new JavaScriptParser();
        this.relationshipBuilder = new RelationshipBuilder();
        this.androidManifestParser = new AndroidManifestParser();
    }

    public ProjectAnalysisResult analyze(File projectDir, AnalysisProgressListener progressListener) {
        ProjectAnalysisResult result = new ProjectAnalysisResult();
        result.setProjectName(projectDir.getName());
        result.setProjectPath(projectDir.getAbsolutePath());

        File manifestFile = new File(projectDir, "src/main/AndroidManifest.xml");
        if (manifestFile.exists()) {
            try {
                activityLayoutMap = androidManifestParser.parseManifest(manifestFile);
                // Update Java parser with activity information
                javaParser = new JavaFileParser(activityLayoutMap);
            } catch (Exception e) {
                System.err.println("Error parsing AndroidManifest.xml: " + e.getMessage());
            }
        }

        // Count total files for progress tracking
        int totalFiles = countFiles(projectDir);
        AtomicInteger processedFiles = new AtomicInteger(0);

        // Recursively process all files in the project
        processDirectory(projectDir, result, processedFiles, totalFiles, progressListener);

        // Build relationships between components
        relationshipBuilder.buildRelationships(result);

        addNavigationRelationships(result);


        return result;
    }

    private void addNavigationRelationships(ProjectAnalysisResult result) {
        for (CodeComponent component : result.getComponents()) {
            if (component.getNavigationDestinations() != null &&
                    !component.getNavigationDestinations().isEmpty()) {

                for (NavigationDestination destination : component.getNavigationDestinations()) {
                    ComponentRelationship navRel = new ComponentRelationship();
                    navRel.setSourceId(component.getId());
                    navRel.setTargetId(destination.getDestinationId());
                    navRel.setType("NAVIGATES_TO");
                    result.addRelationship(navRel);
                }
            }
        }
    }

    private int countFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory() && !shouldSkipDirectory(file.getName())) {
                count += countFiles(file);
            } else if (file.isFile() && !shouldSkipFile(file.getName())) {
                count++;
            }
        }
        return count;
    }

    private void processDirectory(File directory, ProjectAnalysisResult result,
                                  AtomicInteger processedFiles, int totalFiles,
                                  AnalysisProgressListener progressListener) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!shouldSkipDirectory(file.getName())) {
                    processDirectory(file, result, processedFiles, totalFiles, progressListener);
                }
            } else {
                processFile(file, result);
                int processed = processedFiles.incrementAndGet();
                double progress = (double) processed / totalFiles;
                progressListener.onProgress(progress, "Processing: " + file.getName());
            }
        }
    }

    private void processFile(File file, ProjectAnalysisResult result) {
        String fileName = file.getName().toLowerCase();

        try {
            if (fileName.endsWith(".java")) {
                result.addComponents(javaParser.parse(file));
            } else if (fileName.endsWith(".kt")) {
                result.addComponents(kotlinParser.parse(file));
            } else if (fileName.endsWith(".xml")) {
                result.addComponents(xmlParser.parse(file));
            } else if (fileName.endsWith(".dart")) {
                result.addComponents(dartParser.parse(file));
            } else if (fileName.endsWith(".js") || fileName.endsWith(".jsx") ||
                    fileName.endsWith(".ts") || fileName.endsWith(".tsx")) {
                result.addComponents(jsParser.parse(file));
            } else if (fileName.equals("build.gradle") || fileName.equals("build.gradle.kts")) {
                result.addGradleDependencies(parseGradleDependencies(file));
            } else if (fileName.equals("pubspec.yaml")) {
                result.addFlutterDependencies(parsePubspecDependencies(file));
            } else if (fileName.equals("package.json")) {
                result.addJSDependencies(parsePackageJsonDependencies(file));
            }
        } catch (Exception e) {
            System.err.println("Error parsing file: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private boolean shouldSkipDirectory(String dirName) {
        List<String> skipDirs = List.of("build", "node_modules", ".git", ".idea", "dist", "target", ".gradle");
        return skipDirs.contains(dirName);
    }

    private boolean shouldSkipFile(String fileName) {
        List<String> skipFiles = List.of(".DS_Store", "Thumbs.db");
        return skipFiles.contains(fileName);
    }

    private void buildRelationships(ProjectAnalysisResult result) {
        RelationshipBuilder builder = new RelationshipBuilder();
        builder.buildRelationships(result);
    }

    // Additional parsing methods for dependency files
    private List<Dependency> parseGradleDependencies(File file) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            // Pattern to match dependencies in Gradle: implementation, api, etc.
            Pattern pattern = Pattern.compile("(implementation|api|compile)\\s+['\"]([^:]+):([^:]+):([^'\"]+)['\"]");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String type = matcher.group(1);
                String group = matcher.group(2);
                String name = matcher.group(3);
                String version = matcher.group(4);
                Dependency dep = new Dependency();
                dep.setName(group + ":" + name);
                dep.setVersion(version);
                dep.setType("GRADLE");
                dependencies.add(dep);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gradle file: " + e.getMessage());
        }
        return dependencies;
    }

    private List<Dependency> parsePubspecDependencies(File file) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            // Pattern for dependencies in pubspec.yaml
            boolean inDependenciesSection = false;
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().equals("dependencies:")) {
                    inDependenciesSection = true;
                    continue;
                }
                if (inDependenciesSection) {
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }
                    if (line.trim().contains(":") && !line.trim().startsWith(" ")) {
                        break; // Another section started
                    }
                    Matcher m = Pattern.compile("\\s+(\\w+):\\s+([^\\s]+)").matcher(line);
                    if (m.find()) {
                        String name = m.group(1);
                        String version = m.group(2);
                        Dependency dep = new Dependency();
                        dep.setName(name);
                        dep.setVersion(version);
                        dep.setType("FLUTTER");
                        dependencies.add(dep);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing pubspec.yaml: " + e.getMessage());
        }
        return dependencies;
    }

    private List<Dependency> parsePackageJsonDependencies(File file) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            // Find the dependencies section
            Pattern pattern = Pattern.compile("\"dependencies\"\\s*:\\s*\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String depsSection = matcher.group(1);
                // Parse each dependency
                Pattern depPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
                Matcher depMatcher = depPattern.matcher(depsSection);
                while (depMatcher.find()) {
                    String name = depMatcher.group(1);
                    String version = depMatcher.group(2);
                    Dependency dep = new Dependency();
                    dep.setName(name);
                    dep.setVersion(version);
                    dep.setType("JS");
                    dependencies.add(dep);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing package.json: " + e.getMessage());
        }
        return dependencies;
    }

    public interface AnalysisProgressListener {
        void onProgress(double progress, String message);
    }
}