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
        sections.put("Technical Stack", generateTechStackSection());
        sections.put("Class Roles & Responsibilities", generateClassRolesSection());
        sections.put("Dependency Injection Graph", generateDependencyGraphSection());
        sections.put("Navigation Flow", generateNavigationSection());
        sections.put("Data Flow Architecture", generateDataFlowSection());
        sections.put("API & Network Layer", generateApiSection());
        sections.put("App Configuration", generateConfigSection());
        sections.put("Data Models", generateDataModelsSection());
        
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
        
        if (aiService != null && aiService.isReady()) {
            try {
                String componentsSummary = analysisResult.getComponents().stream()
                    .limit(30)
                    .map(c -> c.getName())
                    .collect(Collectors.joining(", "));
                    
                String context = "Project: " + appName + "\nPackage: " + packageName + "\nComponents: " + componentsSummary;
                context += "\nLibraries: " + gradleExtractor.getDependencies().stream().map(Object::toString).collect(Collectors.joining(", "));
                
                String aiSummary = aiService.analyzeCode(context, "doc_overview");
                if (aiSummary != null && !aiSummary.isEmpty()) {
                    sb.append(aiSummary).append("\n\n");
                }
            } catch (Exception e) {
                logger.error("AI Overview generation failed", e);
            }
        } else {
             sb.append("**").append(appName).append("** is an Android application.\n\n");
        }

        sb.append("### Project Metadata\n");
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
        
        if (aiService != null && aiService.isReady()) {
             try {
                String dependencies = gradleExtractor.getDependencies().stream().map(Object::toString).collect(Collectors.joining("\n"));
                String aiTech = aiService.analyzeCode(dependencies, "doc_tech_stack");
                if (aiTech != null && !aiTech.isEmpty()) {
                    sb.append(aiTech).append("\n\n");
                    return sb.toString();
                }
            } catch (Exception e) {
                logger.error("AI Tech Stack generation failed", e);
            }
        }
        
        // Fallback to manual generation
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
     * Section: Class Roles & Responsibilities
     */
    private String generateClassRolesSection() {
        if (aiService == null || !aiService.isReady()) {
            // Fallback to old module section if AI not ready
             return generateModulesSection();
        }
        
        StringBuilder sb = new StringBuilder();
        try {
            List<CodeComponent> importantComponents = analysisResult.getComponents().stream()
                .filter(c -> !c.getName().contains("Test") && !c.getName().startsWith("BuildConfig"))
                .sorted((c1, c2) -> Integer.compare(c2.getLinesOfCode(), c1.getLinesOfCode())) // Sort by size
                .limit(10) // Limit to top 10 most complex classes for detailed analysis
                .collect(Collectors.toList());
            
            StringBuilder codeSummary = new StringBuilder();
            for (CodeComponent comp : importantComponents) {
                codeSummary.append("\nClass: ").append(comp.getName())
                    .append("\nMethods: ").append(comp.getMethods().stream().map(m -> m.getName()).limit(5).collect(Collectors.joining(", ")))
                    .append("\n");
            }
            
            String aiResponse = aiService.analyzeCode(codeSummary.toString(), "doc_class_details");
            if (aiResponse != null) {
                sb.append(aiResponse);
            }
        } catch (Exception e) {
            sb.append("*Error generating class roles: ").append(e.getMessage()).append("*\n");
        }
        return sb.toString();
    }
    
    /**
     * Section: Dependency Injection Graph
     */
    private String generateDependencyGraphSection() {
        if (aiService == null || !aiService.isReady()) {
            return "*AI model required for dependency graph generation*\n";
        }
        
        try {
            // Filter for DI related components (Modules, Components, etc)
            String diContext = analysisResult.getComponents().stream()
                .filter(c -> c.getAnnotations().stream().anyMatch(a -> a.contains("Module") || a.contains("Component") || a.contains("Inject") || a.contains("Provides")))
                .map(c -> "Class: " + c.getName() + " Annotations: " + c.getAnnotations())
                .limit(20)
                .collect(Collectors.joining("\n"));
                
             if (diContext.isEmpty()) {
                return "*No Dependency Injection components detected*\n";
             }
             
             return aiService.analyzeCode(diContext, "doc_di_graph");
        } catch (Exception e) {
            return "*Error analyzing DI graph: " + e.getMessage() + "*\n";
        }
    }

    /**
     * Section: Data Flow Architecture
     */
    private String generateDataFlowSection() {
         if (aiService == null || !aiService.isReady()) {
            return "*AI model required for data flow analysis*\n";
        }
        
        try {
            // Gather key components from different layers
            String context = analysisResult.getComponents().stream()
                .filter(c -> c.getName().endsWith("Repository") || c.getName().endsWith("ViewModel") || c.getName().endsWith("Activity"))
                .map(c -> c.getName())
                .limit(15)
                .collect(Collectors.joining(", "));
                
            return aiService.analyzeCode("Components: " + context, "doc_data_flow");
        } catch (Exception e) {
            return "*Error analyzing data flow: " + e.getMessage() + "*\n";
        }
    }
    
    /**
     * Section: API & Network Layer
     */
    private String generateApiSection() {
         if (aiService == null || !aiService.isReady()) {
            return "*AI model required for API analysis*\n";
        }
        
        try {
             String context = analysisResult.getComponents().stream()
                .filter(c -> c.getName().endsWith("Service") || c.getName().endsWith("Api") || c.getAnnotations().stream().anyMatch(a -> a.contains("GET") || a.contains("POST")))
                 // Get method signatures for APIs
                .map(c -> {
                    String methods = c.getMethods().stream()
                        .filter(m -> m.getAnnotations().stream().anyMatch(a -> a.contains("GET") || a.contains("POST") || a.contains("PUT")))
                        .map(m -> m.getName() + " " + m.getAnnotations())
                        .collect(Collectors.joining("; "));
                    return "Interface " + c.getName() + ": " + methods;
                })
                .collect(Collectors.joining("\n"));
                
            if (context.isEmpty()) {
                 return "*No explicit API interfaces detected via Retrofit annotations*\n";
            }
            
            return aiService.analyzeCode(context, "doc_apis");
        } catch (Exception e) {
            return "*Error analyzing APIs: " + e.getMessage() + "*\n";
        }
    }

    /**
     * Navigation Flow (Manual fallback / Existing)
     */
    private String generateNavigationSection() {
        StringBuilder sb = new StringBuilder();
        
        if (analysisResult == null || analysisResult.getNavigationFlows() == null 
            || analysisResult.getNavigationFlows().isEmpty()) {
            return "*No navigation flows detected*\n";
        }
        
        sb.append("```mermaid\ngraph TD\n");
        for (NavigationFlow flow : analysisResult.getNavigationFlows()) {
            sb.append("  ").append(flow.getSourceScreenId()).append(" --> ").append(flow.getTargetScreenId());
            if (flow.getNavigationType() != null) {
                sb.append(" : ").append(flow.getNavigationType().name());
            }
            sb.append("\n");
        }
        sb.append("```\n");
        sb.append("\n*Diagram shows navigation paths between screens*");
        
        return sb.toString();
    }
    
    // Helper methods
    
    private String getAndroidVersion(String api) {
        switch (api) {
            case "21": return "5.0";
            case "23": return "6.0";
            case "24": return "7.0";
            case "26": return "8.0";
            case "28": return "9.0";
            case "29": return "10";
            case "30": return "11";
            case "31": return "12";
            case "32": return "12L";
            case "33": return "13";
            case "34": return "14";
            case "35": return "15";
            default: return api;
        }
    }
    
    private String getPermissionPurpose(String perm) {
        switch (perm.toUpperCase()) {
            case "INTERNET": return "Network/API communication";
            case "ACCESS_NETWORK_STATE": return "Check connectivity";
            case "ACCESS_WIFI_STATE": return "Check Wi-Fi status";
            case "CAMERA": return "Take photos/videos";
            case "READ_EXTERNAL_STORAGE": return "Read files";
            case "WRITE_EXTERNAL_STORAGE": return "Save files";
            case "ACCESS_FINE_LOCATION": return "GPS location";
            case "ACCESS_COARSE_LOCATION": return "Approximate location";
            case "RECORD_AUDIO": return "Audio recording";
            case "READ_CONTACTS": return "Access contacts";
            case "VIBRATE": return "Vibration feedback";
            case "RECEIVE_BOOT_COMPLETED": return "Auto-start on boot";
            case "POST_NOTIFICATIONS": return "Show notifications";
            default: return "Required for app functionality";
        }
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
