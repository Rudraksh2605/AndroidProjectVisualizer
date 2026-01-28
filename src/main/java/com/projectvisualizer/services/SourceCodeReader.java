package com.projectvisualizer.services;

import com.projectvisualizer.model.CodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for reading actual source code from files.
 * Provides real code content to AI for meaningful documentation generation.
 */
public class SourceCodeReader {
    
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeReader.class);
    
    // Cache for file contents to avoid re-reading
    private final Map<String, String> fileCache = new HashMap<>();
    
    // Maximum characters to read per file (to fit in AI context)
    private static final int MAX_FILE_SIZE = 8000;
    
    // Maximum total context size
    private static final int MAX_TOTAL_CONTEXT = 32000;
    
    private String projectPath;
    
    public SourceCodeReader(String projectPath) {
        this.projectPath = projectPath;
    }
    
    /**
     * Reads the content of a source file.
     * @param filePath Path to the source file
     * @return File content or empty string if error
     */
    public String readSourceFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // Check cache first
        if (fileCache.containsKey(filePath)) {
            return fileCache.get(filePath);
        }
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("File not found: {}", filePath);
                return "";
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            
            // Truncate if too large
            if (content.length() > MAX_FILE_SIZE) {
                content = content.substring(0, MAX_FILE_SIZE) + "\n... [truncated]";
            }
            
            fileCache.put(filePath, content);
            return content;
            
        } catch (IOException e) {
            logger.error("Failed to read file: {}", filePath, e);
            return "";
        }
    }
    
    /**
     * Reads build.gradle or build.gradle.kts to detect tech stack.
     * @return Build file content or empty string
     */
    public String readBuildGradle() {
        if (projectPath == null) return "";
        
        // Try different build file locations
        String[] possiblePaths = {
            projectPath + "/app/build.gradle.kts",
            projectPath + "/app/build.gradle",
            projectPath + "/build.gradle.kts",
            projectPath + "/build.gradle"
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    logger.warn("Could not read build file: {}", path);
                }
            }
        }
        
        return "";
    }
    
    /**
     * Detects tech stack from build.gradle contents.
     * @return Human-readable tech stack description
     */
    public String detectTechStack() {
        String buildContent = readBuildGradle().toLowerCase();
        if (buildContent.isEmpty()) {
            return "Build file not found - tech stack unknown";
        }
        
        StringBuilder stack = new StringBuilder();
        stack.append("Technology Stack Detected:\n\n");
        
        // Language detection
        if (buildContent.contains("kotlin")) {
            stack.append("- Language: Kotlin\n");
        }
        if (buildContent.contains("java")) {
            stack.append("- Language: Java\n");
        }
        
        // Architecture Components
        if (buildContent.contains("lifecycle-viewmodel") || buildContent.contains("viewmodel")) {
            stack.append("- Architecture: MVVM with ViewModel\n");
        }
        if (buildContent.contains("livedata")) {
            stack.append("- State: LiveData\n");
        }
        if (buildContent.contains("flow") || buildContent.contains("coroutines")) {
            stack.append("- Async: Kotlin Coroutines & Flow\n");
        }
        
        // Dependency Injection
        if (buildContent.contains("hilt")) {
            stack.append("- DI: Hilt\n");
        } else if (buildContent.contains("dagger")) {
            stack.append("- DI: Dagger\n");
        } else if (buildContent.contains("koin")) {
            stack.append("- DI: Koin\n");
        }
        
        // Database
        if (buildContent.contains("room")) {
            stack.append("- Database: Room\n");
        }
        if (buildContent.contains("realm")) {
            stack.append("- Database: Realm\n");
        }
        
        // Networking
        if (buildContent.contains("retrofit")) {
            stack.append("- Networking: Retrofit\n");
        }
        if (buildContent.contains("okhttp")) {
            stack.append("- HTTP: OkHttp\n");
        }
        if (buildContent.contains("ktor")) {
            stack.append("- Networking: Ktor\n");
        }
        
        // UI
        if (buildContent.contains("compose")) {
            stack.append("- UI: Jetpack Compose\n");
        }
        if (buildContent.contains("navigation")) {
            stack.append("- Navigation: Navigation Component\n");
        }
        if (buildContent.contains("recyclerview")) {
            stack.append("- Lists: RecyclerView\n");
        }
        
        // Image Loading
        if (buildContent.contains("glide")) {
            stack.append("- Images: Glide\n");
        }
        if (buildContent.contains("coil")) {
            stack.append("- Images: Coil\n");
        }
        if (buildContent.contains("picasso")) {
            stack.append("- Images: Picasso\n");
        }
        
        // Firebase
        if (buildContent.contains("firebase")) {
            stack.append("- Backend: Firebase\n");
            if (buildContent.contains("firestore")) {
                stack.append("  - Firestore Database\n");
            }
            if (buildContent.contains("auth")) {
                stack.append("  - Firebase Auth\n");
            }
        }
        
        // Testing
        if (buildContent.contains("junit")) {
            stack.append("- Testing: JUnit\n");
        }
        if (buildContent.contains("espresso")) {
            stack.append("- UI Testing: Espresso\n");
        }
        if (buildContent.contains("mockk") || buildContent.contains("mockito")) {
            stack.append("- Mocking: Mockito/MockK\n");
        }
        
        return stack.toString();
    }
    
    /**
     * Builds comprehensive code context for AI from components.
     * Reads actual source files and combines with metadata.
     * @param components List of code components to analyze
     * @return Full code context string for AI
     */
    public String buildFullCodeContext(List<CodeComponent> components, String projectName) {
        StringBuilder context = new StringBuilder();
        int currentSize = 0;
        
        // Header
        context.append("=== ANDROID PROJECT: ").append(projectName).append(" ===\n\n");
        
        // Tech stack from build.gradle
        String techStack = detectTechStack();
        context.append(techStack).append("\n");
        currentSize = context.length();
        
        // Group components by type
        Map<String, List<CodeComponent>> byType = new LinkedHashMap<>();
        for (CodeComponent comp : components) {
            String type = comp.getType() != null ? comp.getType() : "OTHER";
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(comp);
        }
        
        // Priority order: Activities first, then Fragments, ViewModels, Repositories, etc.
        String[] priority = {"ACTIVITY", "FRAGMENT", "VIEWMODEL", "REPOSITORY", "SERVICE", "ADAPTER", "DAO"};
        
        for (String type : priority) {
            List<CodeComponent> comps = byType.get(type);
            if (comps == null) continue;
            
            context.append("\n=== ").append(type).append(" COMPONENTS (").append(comps.size()).append(") ===\n");
            
            for (CodeComponent comp : comps) {
                if (currentSize >= MAX_TOTAL_CONTEXT) {
                    context.append("\n[Context limit reached - remaining files omitted]\n");
                    break;
                }
                
                context.append("\n--- ").append(comp.getName());
                if (comp.getLanguage() != null) {
                    context.append(" (").append(comp.getLanguage()).append(")");
                }
                context.append(" ---\n");
                
                // File path
                if (comp.getFilePath() != null) {
                    context.append("File: ").append(comp.getFilePath()).append("\n");
                }
                
                // Annotations (important for understanding purpose)
                if (comp.getAnnotations() != null && !comp.getAnnotations().isEmpty()) {
                    context.append("Annotations: ").append(String.join(", ", comp.getAnnotations())).append("\n");
                }
                
                // Extends/Implements
                if (comp.getExtendsClass() != null) {
                    context.append("Extends: ").append(comp.getExtendsClass()).append("\n");
                }
                if (comp.getImplementsList() != null && !comp.getImplementsList().isEmpty()) {
                    context.append("Implements: ").append(String.join(", ", comp.getImplementsList())).append("\n");
                }
                
                // READ ACTUAL SOURCE CODE
                String sourceCode = "";
                if (comp.getFilePath() != null) {
                    sourceCode = readSourceFile(comp.getFilePath());
                }
                
                if (!sourceCode.isEmpty()) {
                    context.append("\nSOURCE CODE:\n```\n");
                    context.append(sourceCode);
                    context.append("\n```\n");
                    currentSize = context.length();
                } else {
                    // Fallback to method signatures if file not readable
                    if (comp.getMethods() != null && !comp.getMethods().isEmpty()) {
                        context.append("\nMethods:\n");
                        comp.getMethods().forEach(m -> 
                            context.append("  - ").append(m.getName()).append("()\n")
                        );
                    }
                }
            }
            
            if (currentSize >= MAX_TOTAL_CONTEXT) break;
        }
        
        // Add remaining types briefly
        for (Map.Entry<String, List<CodeComponent>> entry : byType.entrySet()) {
            if (Arrays.asList(priority).contains(entry.getKey())) continue;
            if (currentSize >= MAX_TOTAL_CONTEXT) break;
            
            context.append("\n=== ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(") ===\n");
            for (CodeComponent comp : entry.getValue()) {
                context.append("- ").append(comp.getName()).append("\n");
            }
        }
        
        return context.toString();
    }
    
    /**
     * Clears the file cache.
     */
    public void clearCache() {
        fileCache.clear();
    }
}
