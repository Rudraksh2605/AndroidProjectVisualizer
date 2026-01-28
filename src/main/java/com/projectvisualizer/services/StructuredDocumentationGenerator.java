package com.projectvisualizer.services;

import com.projectvisualizer.model.*;
import com.projectvisualizer.ai.Phi2InferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates structured, professional documentation from parsed project data.
 * Uses ManifestDataExtractor plus GradleDataExtractor plus CodeComponent analysis.
 */
public class StructuredDocumentationGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredDocumentationGenerator.class);
    
    private final String projectPath;
    private final String projectName;
    private final AnalysisResult analysisResult;
    private final Phi2InferenceService aiService;
    
    private ManifestDataExtractor manifestExtractor;
    private GradleDataExtractor gradleExtractor;
    
    public StructuredDocumentationGenerator(String projectPath, String projectName, 
            AnalysisResult analysisResult, Phi2InferenceService aiService) {
        this.projectPath = projectPath;
        this.projectName = projectName;
        this.analysisResult = analysisResult;
        this.aiService = aiService;
        
        // Extract data from manifest and gradle
        if (projectPath != null && !projectPath.isEmpty()) {
            this.manifestExtractor = new ManifestDataExtractor(projectPath);
            this.gradleExtractor = new GradleDataExtractor(projectPath);
        }
    }
    
    /**
     * Generates complete structured documentation as a map of section name to content.
     */
    public Map<String, String> generateDocumentation() {
        Map<String, String> sections = new LinkedHashMap<>();
        
        sections.put("Project Overview", generateOverviewSection());
        sections.put("Technical Stack & Architecture", generateTechStackSection());
        sections.put("App Configuration", generateConfigSection());
        sections.put("Key Modules & Components", generateModulesSection());
        sections.put("Data Models", generateDataModelsSection());
        sections.put("Navigation Flow", generateNavigationSection());
        
        return sections;
    }
    
    /**
     * Section 1: Project Overview
     */
    private String generateOverviewSection() {
        StringBuilder sb = new StringBuilder();
        
        String appName = projectName != null ? projectName : "Android Application";
        String packageName = manifestExtractor != null && manifestExtractor.isParsed() && !manifestExtractor.getPackageName().isEmpty()
            ? manifestExtractor.getPackageName() : "Unknown";
            
        // Fallback to Gradle application ID if Manifest package is missing (common in modern AGP)
        if (packageName.equals("Unknown") && gradleExtractor != null && gradleExtractor.isParsed() && !gradleExtractor.getApplicationId().isEmpty()) {
            packageName = gradleExtractor.getApplicationId();
        }
        
        sb.append("**").append(appName).append("** is an Android application.\n\n");
        
        // AI-Enhanced Summary
        if (aiService != null && aiService.isReady()) {
            try {
                // We ask the AI purely for a high-level summary here based on what we know
                String context = "Project: " + appName + "\nPackage: " + packageName + "\nComponents: " + 
                                 (analysisResult != null ? analysisResult.getComponents().size() : 0) + " classes.";
                String prompt = "Write a clear, professional 1-paragraph summary for the documentation of this Android project. " +
                                "Describe it as a modern Android application." + 
                                (gradleExtractor != null ? " It uses " + gradleExtractor.getUiFramework() + "." : "");
                                
                // Note: In a real async flow we'd await this, but here we might be blocking or this method 
                // is called from a background thread. MainController seems to call this.
                // For safety/speed, we might need to rely on pre-computed insights, but let's try a direct simple call:
                // String aiSummary = aiService.analyzeCode(context, prompt); // This might block
                // For now, let's just add a placeholder if we haven't pre-computed it, 
                // BUT MainController has 'aiGeneratedSummary'.
                // actually, this class doesn't store the AI result from MainController.
                // We will skip direct AI call here to avoid blocking UI if running on FX thread.
                // Instead, we trust the caller to have enhanced it, OR we just provide better static text.
            } catch (Exception e) {
                // ignore
            }
        }

        sb.append("| Property | Value |\n");
        sb.append("|----------|-------|\n");
        sb.append("| **Package Name** | `").append(packageName).append("` |\n");
        
        if (gradleExtractor != null && gradleExtractor.isParsed()) {
            if (!gradleExtractor.getMinSdk().isEmpty()) {
                sb.append("| **Minimum SDK** | API ").append(gradleExtractor.getMinSdk())
                  .append(" (Android ").append(getAndroidVersion(gradleExtractor.getMinSdk())).append(") |\n");
            }
            if (!gradleExtractor.getTargetSdk().isEmpty()) {
                sb.append("| **Target SDK** | API ").append(gradleExtractor.getTargetSdk())
                  .append(" (Android ").append(getAndroidVersion(gradleExtractor.getTargetSdk())).append(") |\n");
            }
            sb.append("| **Language** | ").append(gradleExtractor.getPrimaryLanguage()).append(" |\n");
            sb.append("| **UI Framework** | ").append(gradleExtractor.getUiFramework()).append(" |\n");
            
            if (!gradleExtractor.getVersionName().isEmpty()) {
                sb.append("| **Version** | ").append(gradleExtractor.getVersionName()).append(" |\n");
            }
        }
        
        // Component count
        int componentCount = analysisResult != null && analysisResult.getComponents() != null 
            ? analysisResult.getComponents().size() : 0;
        sb.append("| **Components** | ").append(componentCount).append(" classes analyzed |\n");
        
        return sb.toString();
    }
    
    /**
     * Section 2: Technical Stack
     */
    private String generateTechStackSection() {
        StringBuilder sb = new StringBuilder();
        
        if (gradleExtractor == null || !gradleExtractor.isParsed()) {
            sb.append("*Build file not found - tech stack unavailable*\n");
            return sb.toString();
        }
        
        // Core Technologies
        sb.append("### Core Technologies\n\n");
        sb.append("| Technology | Details |\n");
        sb.append("|------------|--------|\n");
        sb.append("| **Language** | ").append(gradleExtractor.getPrimaryLanguage()).append(" |\n");
        sb.append("| **UI** | ").append(gradleExtractor.getUiFramework()).append(" |\n");
        sb.append("| **Build System** | Gradle (Kotlin DSL) |\n");
        
        // Firebase
        if (!gradleExtractor.getFirebaseDeps().isEmpty()) {
            sb.append("\n### Firebase Services\n\n");
            gradleExtractor.getFirebaseDeps().stream().sorted().distinct().forEach(dep -> 
                sb.append("- ").append(capitalize(dep)).append("\n")
            );
        }
        
        // Networking
        if (!gradleExtractor.getNetworkDeps().isEmpty()) {
            sb.append("\n### Networking\n\n");
            gradleExtractor.getNetworkDeps().stream().sorted().distinct().forEach(dep -> 
                sb.append("- ").append(capitalize(dep)).append("\n")
            );
        }
        
        // Database
        if (!gradleExtractor.getDatabaseDeps().isEmpty()) {
            sb.append("\n### Database\n\n");
            gradleExtractor.getDatabaseDeps().stream().sorted().distinct().forEach(dep -> 
                sb.append("- ").append(capitalize(dep)).append("\n")
            );
        }
        
        // Dependency Injection
        if (!gradleExtractor.getDiDeps().isEmpty()) {
            sb.append("\n### Dependency Injection\n\n");
            gradleExtractor.getDiDeps().stream().sorted().distinct().forEach(dep -> 
                sb.append("- ").append(capitalize(dep)).append("\n")
            );
        }
        
        // UI Libraries
        if (!gradleExtractor.getUiDeps().isEmpty()) {
            sb.append("\n### UI Components\n\n");
            gradleExtractor.getUiDeps().stream().sorted().distinct().forEach(dep -> 
                sb.append("- ").append(capitalize(dep)).append("\n")
            );
        }
        
        return sb.toString();
    }
    
    /**
     * Section 3: App Configuration (Manifest)
     */
    private String generateConfigSection() {
        StringBuilder sb = new StringBuilder();
        
        if (manifestExtractor == null || !manifestExtractor.isParsed()) {
            sb.append("*AndroidManifest.xml not found*\n");
            return sb.toString();
        }
        
        // Permissions
        List<String> perms = manifestExtractor.getPermissions();
        if (!perms.isEmpty()) {
            sb.append("### Permissions\n\n");
            sb.append("| Permission | Purpose |\n");
            sb.append("|------------|--------|\n");
            for (String perm : perms) {
                sb.append("| `").append(perm).append("` | ").append(getPermissionPurpose(perm)).append(" |\n");
            }
            sb.append("\n");
        }
        
        // Activities
        List<ManifestDataExtractor.ActivityInfo> activities = manifestExtractor.getActivities();
        if (!activities.isEmpty()) {
            sb.append("### Activities / Screens\n\n");
            sb.append("| Screen | Purpose |\n");
            sb.append("|--------|--------|\n");
            for (ManifestDataExtractor.ActivityInfo activity : activities) {
                String marker = activity.isLauncher ? " ⭐" : "";
                sb.append("| **").append(activity.shortName).append("**").append(marker)
                  .append(" | ").append(inferActivityPurpose(activity.shortName)).append(" |\n");
            }
            sb.append("\n*⭐ = Launcher/Entry point*\n");
        }
        
        // Services
        List<String> services = manifestExtractor.getServices();
        if (!services.isEmpty()) {
            sb.append("\n### Background Services\n\n");
            for (String service : services) {
                sb.append("- ").append(service).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Section 4: Key Modules & Components
     */
    private String generateModulesSection() {
        StringBuilder sb = new StringBuilder();
        
        if (analysisResult == null || analysisResult.getComponents() == null) {
            sb.append("*No components analyzed*\n");
            return sb.toString();
        }
        
        List<CodeComponent> components = analysisResult.getComponents();
        
        // Group by type
        Map<String, List<CodeComponent>> byType = new LinkedHashMap<>();
        for (CodeComponent comp : components) {
            String type = comp.getType() != null ? comp.getType() : "OTHER";
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(comp);
        }
        
        // Priority order
        String[] types = {"ACTIVITY", "FRAGMENT", "VIEWMODEL", "REPOSITORY", "SERVICE", "ADAPTER", "DAO", "API"};
        
        for (String type : types) {
            List<CodeComponent> comps = byType.get(type);
            if (comps == null || comps.isEmpty()) continue;
            
            sb.append("### ").append(capitalize(type)).append(" (").append(comps.size()).append(")\n\n");
            sb.append("| Component | Purpose |\n");
            sb.append("|-----------|--------|\n");
            
            for (CodeComponent comp : comps) {
                String purpose = inferComponentPurpose(comp);
                sb.append("| `").append(comp.getName()).append("` | ").append(purpose).append(" |\n");
            }
            sb.append("\n");
        }
        
        // Other types
        for (Map.Entry<String, List<CodeComponent>> entry : byType.entrySet()) {
            if (Arrays.asList(types).contains(entry.getKey())) continue;
            if (entry.getValue().size() > 0) {
                sb.append("### ").append(capitalize(entry.getKey())).append(" (")
                  .append(entry.getValue().size()).append(" components)\n\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Section 5: Data Models
     */
    private String generateDataModelsSection() {
        StringBuilder sb = new StringBuilder();
        
        if (analysisResult == null || analysisResult.getComponents() == null) {
            return "*No data models detected*\n";
        }
        
        // Find data classes (models)
        List<CodeComponent> models = analysisResult.getComponents().stream()
            .filter(c -> isDataModel(c))
            .collect(Collectors.toList());
        
        if (models.isEmpty()) {
            sb.append("*No data model classes detected*\n");
            return sb.toString();
        }
        
        sb.append("The following data classes were detected:\n\n");
        
        for (CodeComponent model : models) {
            sb.append("### ").append(model.getName()).append("\n\n");
            
            List<CodeField> fields = model.getFields();
            if (fields != null && !fields.isEmpty()) {
                sb.append("| Field | Type | Description |\n");
                sb.append("|-------|------|-------------|\n");
                for (CodeField field : fields) {
                    String desc = inferFieldDescription(field.getName());
                    sb.append("| `").append(field.getName()).append("` | ")
                      .append(field.getType()).append(" | ").append(desc).append(" |\n");
                }
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Section 6: Navigation Flow
     */
    private String generateNavigationSection() {
        StringBuilder sb = new StringBuilder();
        
        if (analysisResult == null || analysisResult.getNavigationFlows() == null 
            || analysisResult.getNavigationFlows().isEmpty()) {
            return "*No navigation flows detected*\n";
        }
        
        sb.append("```\n");
        for (NavigationFlow flow : analysisResult.getNavigationFlows()) {
            sb.append(flow.getSourceScreenId()).append(" → ").append(flow.getTargetScreenId());
            if (flow.getNavigationType() != null) {
                sb.append(" (").append(flow.getNavigationType().name()).append(")");
            }
            sb.append("\n");
        }
        sb.append("```\n");
        
        return sb.toString();
    }
    
    // Helper methods
    
    private String getAndroidVersion(String api) {
        return switch (api) {
            case "21" -> "5.0";
            case "23" -> "6.0";
            case "24" -> "7.0";
            case "26" -> "8.0";
            case "28" -> "9.0";
            case "29" -> "10";
            case "30" -> "11";
            case "31" -> "12";
            case "32" -> "12L";
            case "33" -> "13";
            case "34" -> "14";
            case "35" -> "15";
            default -> api;
        };
    }
    
    private String getPermissionPurpose(String perm) {
        return switch (perm.toUpperCase()) {
            case "INTERNET" -> "Network/API communication";
            case "ACCESS_NETWORK_STATE" -> "Check connectivity";
            case "ACCESS_WIFI_STATE" -> "Check Wi-Fi status";
            case "CAMERA" -> "Take photos/videos";
            case "READ_EXTERNAL_STORAGE" -> "Read files";
            case "WRITE_EXTERNAL_STORAGE" -> "Save files";
            case "ACCESS_FINE_LOCATION" -> "GPS location";
            case "ACCESS_COARSE_LOCATION" -> "Approximate location";
            case "RECORD_AUDIO" -> "Audio recording";
            case "READ_CONTACTS" -> "Access contacts";
            case "VIBRATE" -> "Vibration feedback";
            case "RECEIVE_BOOT_COMPLETED" -> "Auto-start on boot";
            case "POST_NOTIFICATIONS" -> "Show notifications";
            default -> "Required for app functionality";
        };
    }
    
    private String inferActivityPurpose(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("login")) return "User login screen";
        if (lower.contains("signup") || lower.contains("register")) return "User registration";
        if (lower.contains("home") || lower.contains("main")) return "Main dashboard";
        if (lower.contains("splash") || lower.contains("start")) return "App launch/splash";
        if (lower.contains("setting")) return "User settings";
        if (lower.contains("profile")) return "User profile";
        if (lower.contains("chat")) return "Messaging/Chat interface";
        if (lower.contains("detail")) return "Detailed view";
        if (lower.contains("list")) return "List view";
        if (lower.contains("search")) return "Search functionality";
        if (lower.contains("result")) return "Results display";
        if (lower.contains("course") || lower.contains("lesson")) return "Educational content";
        if (lower.contains("quiz") || lower.contains("question")) return "Quiz/Assessment";
        return "Application screen";
    }
    
    private String inferComponentPurpose(CodeComponent comp) {
        String name = comp.getName().toLowerCase();
        
        // Check annotations
        if (comp.getAnnotations() != null) {
            for (String ann : comp.getAnnotations()) {
                if (ann.contains("HiltViewModel")) return "Dependency-injected ViewModel";
                if (ann.contains("Entity")) return "Room database entity";
                if (ann.contains("Dao")) return "Database access object";
            }
        }
        
        // Infer from name
        if (name.contains("adapter")) return "RecyclerView data adapter";
        if (name.contains("repository")) return "Data repository layer";
        if (name.contains("viewmodel")) return "UI state holder";
        if (name.contains("service")) return "Background/API service";
        if (name.contains("util") || name.contains("helper")) return "Utility functions";
        if (name.contains("fragment")) return "UI fragment component";
        if (name.contains("activity")) return "Screen container";
        if (name.contains("api")) return "API interface";
        if (name.contains("dao")) return "Database access";
        
        return "Application component";
    }
    
    private boolean isDataModel(CodeComponent comp) {
        String name = comp.getName().toLowerCase();
        // Common model patterns
        if (name.contains("model") || name.contains("entity") || name.contains("dto") ||
            name.contains("response") || name.contains("request") || name.contains("data")) {
            return true;
        }
        // Has data class annotation
        if (comp.getAnnotations() != null) {
            for (String ann : comp.getAnnotations()) {
                if (ann.contains("Entity") || ann.contains("Parcelize") || ann.contains("Serializable")) {
                    return true;
                }
            }
        }
        // Is in model/data package
        if (comp.getFilePath() != null && 
            (comp.getFilePath().contains("/model/") || comp.getFilePath().contains("/data/") ||
             comp.getFilePath().contains("/entity/"))) {
            return true;
        }
        return false;
    }
    
    private String inferFieldDescription(String fieldName) {
        String lower = fieldName.toLowerCase();
        if (lower.contains("id")) return "Unique identifier";
        if (lower.contains("name")) return "Display name";
        if (lower.contains("email")) return "Email address";
        if (lower.contains("password")) return "User password";
        if (lower.contains("date") || lower.contains("time")) return "Timestamp";
        if (lower.contains("url") || lower.contains("link")) return "URL/Link";
        if (lower.contains("count") || lower.contains("total")) return "Count/Total value";
        if (lower.contains("status")) return "Status indicator";
        if (lower.contains("type")) return "Type classifier";
        if (lower.contains("image") || lower.contains("photo")) return "Image reference";
        if (lower.contains("description") || lower.contains("desc")) return "Description text";
        if (lower.contains("title")) return "Title text";
        if (lower.contains("message") || lower.contains("text")) return "Text content";
        if (lower.contains("price") || lower.contains("amount")) return "Monetary value";
        if (lower.contains("list") || lower.contains("items")) return "Collection of items";
        return "-";
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        // Convert snake_case or kebab-case to Title Case
        return Arrays.stream(str.replace("-", " ").replace("_", " ").split(" "))
            .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
}
