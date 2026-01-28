package com.projectvisualizer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured data from build.gradle files for documentation.
 */
public class GradleDataExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(GradleDataExtractor.class);
    
    private String projectPath;
    private String gradleContent = "";
    private boolean parsed = false;
    
    // Extracted data
    private String applicationId = "";
    private String minSdk = "";
    private String targetSdk = "";
    private String compileSdk = "";
    private String versionName = "";
    private String versionCode = "";
    private String kotlinVersion = "";
    private String javaVersion = "";
    private boolean usesKotlin = false;
    private boolean usesCompose = false;
    private boolean usesViewBinding = false;
    private boolean usesDataBinding = false;
    
    private List<DependencyInfo> dependencies = new ArrayList<>();
    
    // Categorized dependencies
    private List<String> firebaseDeps = new ArrayList<>();
    private List<String> networkDeps = new ArrayList<>();
    private List<String> databaseDeps = new ArrayList<>();
    private List<String> diDeps = new ArrayList<>();
    private List<String> uiDeps = new ArrayList<>();
    private List<String> testDeps = new ArrayList<>();
    
    public GradleDataExtractor(String projectPath) {
        this.projectPath = projectPath;
        parseGradle();
    }
    
    private void parseGradle() {
        String[] possiblePaths = {
            projectPath + "/app/build.gradle.kts",
            projectPath + "/app/build.gradle",
            projectPath + "/build.gradle.kts",
            projectPath + "/build.gradle"
        };
        
        File gradleFile = null;
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                gradleFile = f;
                break;
            }
        }
        
        if (gradleFile == null) {
            logger.warn("build.gradle not found in project: {}", projectPath);
            return;
        }
        
        try {
            gradleContent = Files.readString(gradleFile.toPath(), StandardCharsets.UTF_8);
            
            extractApplicationId();
            extractSdkVersions();
            extractVersionInfo();
            extractLanguageInfo();
            extractBuildFeatures();
            extractDependencies();
            categorizeDependencies();
            
            parsed = true;
            logger.info("Parsed build.gradle: appId={}, minSdk={}, deps={}", 
                applicationId, minSdk, dependencies.size());
                
        } catch (IOException e) {
            logger.error("Failed to read build.gradle", e);
        }
    }
    
    private void extractApplicationId() {
        // pattern: applicationId = "com.example.app" or applicationId "com.example.app"
        Pattern pattern = Pattern.compile("applicationId\\s*[=]?\\s*[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(gradleContent);
        if (matcher.find()) {
            applicationId = matcher.group(1);
        }
    }
    
    private void extractSdkVersions() {
        // minSdk
        Pattern minPattern = Pattern.compile("minSdk\\s*[=]?\\s*(\\d+)");
        Matcher minMatcher = minPattern.matcher(gradleContent);
        if (minMatcher.find()) {
            minSdk = minMatcher.group(1);
        }
        
        // targetSdk
        Pattern targetPattern = Pattern.compile("targetSdk\\s*[=]?\\s*(\\d+)");
        Matcher targetMatcher = targetPattern.matcher(gradleContent);
        if (targetMatcher.find()) {
            targetSdk = targetMatcher.group(1);
        }
        
        // compileSdk
        Pattern compilePattern = Pattern.compile("compileSdk\\s*[=]?\\s*(\\d+)");
        Matcher compileMatcher = compilePattern.matcher(gradleContent);
        if (compileMatcher.find()) {
            compileSdk = compileMatcher.group(1);
        }
    }
    
    private void extractVersionInfo() {
        // versionName
        Pattern namePattern = Pattern.compile("versionName\\s*[=]?\\s*[\"']([^\"']+)[\"']");
        Matcher nameMatcher = namePattern.matcher(gradleContent);
        if (nameMatcher.find()) {
            versionName = nameMatcher.group(1);
        }
        
        // versionCode
        Pattern codePattern = Pattern.compile("versionCode\\s*[=]?\\s*(\\d+)");
        Matcher codeMatcher = codePattern.matcher(gradleContent);
        if (codeMatcher.find()) {
            versionCode = codeMatcher.group(1);
        }
    }
    
    private void extractLanguageInfo() {
        // Check for Kotlin
        if (gradleContent.contains("kotlin") || gradleContent.contains("org.jetbrains.kotlin")) {
            usesKotlin = true;
            
            // Extract Kotlin version
            Pattern kotlinPattern = Pattern.compile("kotlin[\"']?\\s*version\\s*[\"']?([\\d.]+)");
            Matcher kotlinMatcher = kotlinPattern.matcher(gradleContent);
            if (kotlinMatcher.find()) {
                kotlinVersion = kotlinMatcher.group(1);
            }
        }
        
        // Java version
        Pattern javaPattern = Pattern.compile("jvmTarget\\s*[=]?\\s*[\"']?(\\d+)[\"']?");
        Matcher javaMatcher = javaPattern.matcher(gradleContent);
        if (javaMatcher.find()) {
            javaVersion = javaMatcher.group(1);
        }
    }
    
    private void extractBuildFeatures() {
        usesCompose = gradleContent.contains("compose") && 
                      (gradleContent.contains("compose = true") || gradleContent.contains("compose.set(true)"));
        usesViewBinding = gradleContent.contains("viewBinding") && 
                          (gradleContent.contains("viewBinding = true") || gradleContent.contains("viewBinding.set(true)"));
        usesDataBinding = gradleContent.contains("dataBinding") && 
                          (gradleContent.contains("dataBinding = true") || gradleContent.contains("dataBinding.set(true)"));
    }
    
    private void extractDependencies() {
        // Match implementation("group:artifact:version") or implementation "group:artifact:version"
        Pattern depPattern = Pattern.compile("(implementation|api|kapt|ksp|testImplementation|androidTestImplementation)\\s*[\"(]([^\"()]+)[\"\\)]");
        Matcher matcher = depPattern.matcher(gradleContent);
        
        while (matcher.find()) {
            String scope = matcher.group(1);
            String depString = matcher.group(2);
            
            // Skip direct variable references that look like "Libs.xxx" if captured by the generic regex
            if (depString.startsWith("Libs.") || depString.startsWith("deps.")) {
                continue; 
            }

            DependencyInfo info = new DependencyInfo();
            info.scope = scope;
            info.full = depString;
            
            // Parse artifact name
            String[] parts = depString.split(":");
            if (parts.length >= 2) {
                info.group = parts[0];
                info.artifact = parts[1];
                info.version = parts.length > 2 ? parts[2] : "";
                info.shortName = extractFriendlyName(info.artifact);
            } else {
                // If it's a version catalog ref inside string (rare but possible) or just a simple name
                if (depString.contains("libs.")) {
                     info.shortName = depString.substring(depString.lastIndexOf("libs.") + 5)
                                    .replace(".", " ").replace("-", " ");
                } else {
                    info.shortName = depString; 
                }
            }
            
            dependencies.add(info);
        }
        
        // Also match libs.xyz style (version catalog) e.g. implementation(libs.firebase.auth)
        // Regex logic: explicitly look for libs.something inside parens
        Pattern libsPattern = Pattern.compile("(implementation|api)[\\s(]+libs\\.([\\w\\.\\-]+)[\\s)]*");
        Matcher libsMatcher = libsPattern.matcher(gradleContent);
        while (libsMatcher.find()) {
            DependencyInfo info = new DependencyInfo();
            info.scope = libsMatcher.group(1);
            String rawLib = libsMatcher.group(2); // e.g. firebase.auth
            info.shortName = rawLib.replace(".", " ").replace("-", " "); 
            info.full = "libs." + rawLib;
            dependencies.add(info);
        }
    }
    
    private void categorizeDependencies() {
        for (DependencyInfo dep : dependencies) {
            String full = dep.full.toLowerCase();
            
            if (full.contains("firebase")) {
                firebaseDeps.add(dep.shortName);
            } else if (full.contains("retrofit") || full.contains("okhttp") || full.contains("ktor") || full.contains("gson") || full.contains("moshi")) {
                networkDeps.add(dep.shortName);
            } else if (full.contains("room") || full.contains("realm") || full.contains("sqlite")) {
                databaseDeps.add(dep.shortName);
            } else if (full.contains("hilt") || full.contains("dagger") || full.contains("koin")) {
                diDeps.add(dep.shortName);
            } else if (full.contains("compose") || full.contains("material") || full.contains("recyclerview") || 
                       full.contains("constraint") || full.contains("glide") || full.contains("coil") || full.contains("picasso")) {
                uiDeps.add(dep.shortName);
            } else if (full.contains("test") || full.contains("junit") || full.contains("espresso") || full.contains("mock")) {
                testDeps.add(dep.shortName);
            }
        }
    }
    
    private String extractFriendlyName(String artifact) {
        if (artifact == null) return "";
        // Convert artifact name like "firebase-auth" to "Firebase Auth"
        return artifact.replace("-", " ")
                       .replace("_", " ");
    }
    
    // Getters
    public boolean isParsed() { return parsed; }
    public String getApplicationId() { return applicationId; }
    public String getMinSdk() { return minSdk; }
    public String getTargetSdk() { return targetSdk; }
    public String getCompileSdk() { return compileSdk; }
    public String getVersionName() { return versionName; }
    public String getVersionCode() { return versionCode; }
    public boolean usesKotlin() { return usesKotlin; }
    public boolean usesCompose() { return usesCompose; }
    public boolean usesViewBinding() { return usesViewBinding; }
    public boolean usesDataBinding() { return usesDataBinding; }
    public String getKotlinVersion() { return kotlinVersion; }
    public String getJavaVersion() { return javaVersion; }
    
    public List<DependencyInfo> getDependencies() { return dependencies; }
    public List<String> getFirebaseDeps() { return firebaseDeps; }
    public List<String> getNetworkDeps() { return networkDeps; }
    public List<String> getDatabaseDeps() { return databaseDeps; }
    public List<String> getDiDeps() { return diDeps; }
    public List<String> getUiDeps() { return uiDeps; }
    public List<String> getTestDeps() { return testDeps; }
    
    /**
     * Gets the primary language.
     */
    public String getPrimaryLanguage() {
        return usesKotlin ? "Kotlin" : "Java";
    }
    
    /**
     * Gets a summary of the UI framework.
     */
    public String getUiFramework() {
        if (usesCompose) return "Jetpack Compose";
        if (usesViewBinding) return "XML Views (ViewBinding enabled)";
        if (usesDataBinding) return "XML Views (DataBinding enabled)";
        return "XML Views";
    }
    
    /**
     * Dependency information holder.
     */
    public static class DependencyInfo {
        public String scope = "";
        public String group = "";
        public String artifact = "";
        public String version = "";
        public String full = "";
        public String shortName = "";
        
        @Override
        public String toString() {
            return shortName;
        }
    }
}
