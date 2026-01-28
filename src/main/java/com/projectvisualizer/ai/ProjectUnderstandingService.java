package com.projectvisualizer.ai;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.CodeMethod;
import com.projectvisualizer.model.BusinessProcessComponent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level service for AI-powered project understanding.
 * Uses Phi-2 to analyze code components and extract meaningful insights
 * for improved UML diagram generation and project visualization.
 */
public class ProjectUnderstandingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectUnderstandingService.class);
    
    private final Phi2InferenceService inferenceService;
    private static ProjectUnderstandingService instance;
    
    public ProjectUnderstandingService(Phi2InferenceService inferenceService) {
        this.inferenceService = inferenceService;
    }
    
    /**
     * Gets the singleton instance of the service.
     */
    public static synchronized ProjectUnderstandingService getInstance() {
        if (instance == null) {
            instance = new ProjectUnderstandingService(new Phi2InferenceService());
        }
        return instance;
    }
    
    /**
     * Initializes the AI model asynchronously.
     * @param progressCallback Callback for progress updates
     * @return CompletableFuture that completes when ready
     */
    public CompletableFuture<Boolean> initializeAsync(Consumer<String> progressCallback) {
        return inferenceService.loadModelAsync(progressCallback);
    }
    
    /**
     * Checks if the AI service is ready for use.
     */
    public boolean isReady() {
        return inferenceService.isReady();
    }
    
    /**
     * Checks if AI model is currently loading.
     */
    public boolean isLoading() {
        return inferenceService.isLoading();
    }
    
    /**
     * Gets the underlying inference service.
     */
    public Phi2InferenceService getInferenceService() {
        return inferenceService;
    }
    
    /**
     * Checks if the model file is available.
     */
    public boolean isModelAvailable() {
        return inferenceService.getConfig().isModelAvailable();
    }
    
    /**
     * Gets the model download URL.
     */
    public String getModelDownloadUrl() {
        return inferenceService.getConfig().getModelDownloadUrl();
    }
    
    /**
     * Analyzes components to extract enhanced use cases using AI.
     * @param components List of code components to analyze
     * @return List of AI-extracted use case descriptions
     */
    public CompletableFuture<List<UseCaseInfo>> extractUseCasesAsync(List<CodeComponent> components) {
        return CompletableFuture.supplyAsync(() -> {
            if (!inferenceService.isReady()) {
                logger.warn("AI not ready, returning empty use cases");
                return Collections.emptyList();
            }
            
            List<UseCaseInfo> useCases = new ArrayList<>();
            
            // Group components by type for batch analysis
            Map<String, List<CodeComponent>> componentsByType = components.stream()
                .collect(Collectors.groupingBy(c -> c.getType() != null ? c.getType() : "UNKNOWN"));
            
            // Analyze Activities/Fragments for user-facing features
            List<CodeComponent> uiComponents = new ArrayList<>();
            uiComponents.addAll(componentsByType.getOrDefault("ACTIVITY", Collections.emptyList()));
            uiComponents.addAll(componentsByType.getOrDefault("FRAGMENT", Collections.emptyList()));
            
            if (!uiComponents.isEmpty()) {
                useCases.addAll(analyzeUIComponents(uiComponents));
            }
            
            // Analyze Services for background operations
            List<CodeComponent> services = componentsByType.getOrDefault("SERVICE", Collections.emptyList());
            if (!services.isEmpty()) {
                useCases.addAll(analyzeServiceComponents(services));
            }
            
            // Analyze Repositories/Data classes for data operations
            List<CodeComponent> dataComponents = new ArrayList<>();
            dataComponents.addAll(componentsByType.getOrDefault("REPOSITORY", Collections.emptyList()));
            dataComponents.addAll(componentsByType.getOrDefault("DAO", Collections.emptyList()));
            
            if (!dataComponents.isEmpty()) {
                useCases.addAll(analyzeDataComponents(dataComponents));
            }
            
            return useCases;
        });
    }
    
    /**
     * Analyzes UI components (Activities/Fragments) for user-facing features.
     */
    private List<UseCaseInfo> analyzeUIComponents(List<CodeComponent> components) {
        List<UseCaseInfo> useCases = new ArrayList<>();
        
        for (CodeComponent comp : components) {
            String codeSnippet = buildComponentSnippet(comp);
            
            String prompt = String.format("""
                Analyze this Android UI component and identify the main user actions.
                Return ONLY a numbered list of use cases in format: "User can [action] to [goal]"
                Keep each use case under 10 words. Maximum 5 use cases.
                
                Component: %s
                Type: %s
                Methods: %s
                
                Use cases:
                """, 
                comp.getName(),
                comp.getType(),
                getMethodNames(comp)
            );
            
            try {
                String response = inferenceService.generate(prompt);
                useCases.addAll(parseUseCaseResponse(response, comp.getName(), "User"));
            } catch (Exception e) {
                logger.error("Failed to analyze component: " + comp.getName(), e);
            }
        }
        
        return useCases;
    }
    
    /**
     * Analyzes Service components for background operations.
     */
    private List<UseCaseInfo> analyzeServiceComponents(List<CodeComponent> services) {
        List<UseCaseInfo> useCases = new ArrayList<>();
        
        for (CodeComponent service : services) {
            String prompt = String.format("""
                What background operations does this Android Service perform?
                Return ONLY a numbered list in format: "System [action] for [purpose]"
                Keep each under 10 words. Maximum 3 operations.
                
                Service: %s
                Methods: %s
                
                Operations:
                """,
                service.getName(),
                getMethodNames(service)
            );
            
            try {
                String response = inferenceService.generate(prompt);
                useCases.addAll(parseUseCaseResponse(response, service.getName(), "System"));
            } catch (Exception e) {
                logger.error("Failed to analyze service: " + service.getName(), e);
            }
        }
        
        return useCases;
    }
    
    /**
     * Analyzes Data components for database operations.
     */
    private List<UseCaseInfo> analyzeDataComponents(List<CodeComponent> dataComponents) {
        List<UseCaseInfo> useCases = new ArrayList<>();
        
        // Batch analyze data components
        StringBuilder componentsInfo = new StringBuilder();
        for (CodeComponent comp : dataComponents) {
            componentsInfo.append(comp.getName()).append(": ").append(getMethodNames(comp)).append("\n");
        }
        
        String prompt = String.format("""
            What data operations does this Android app support?
            Return ONLY a numbered list in format: "App [action] [data type]"
            Keep each under 8 words. Maximum 5 operations.
            
            Data components:
            %s
            
            Data operations:
            """,
            componentsInfo
        );
        
        try {
            String response = inferenceService.generate(prompt);
            useCases.addAll(parseUseCaseResponse(response, "DataLayer", "App"));
        } catch (Exception e) {
            logger.error("Failed to analyze data components", e);
        }
        
        return useCases;
    }
    
    /**
     * Generates a natural language description of a project.
     */
    public CompletableFuture<String> generateProjectSummaryAsync(List<CodeComponent> components) {
        return CompletableFuture.supplyAsync(() -> {
            if (!inferenceService.isReady()) {
                return "AI analysis not available. Please load the model first.";
            }
            
            String componentsSummary = components.stream()
                .limit(20)
                .map(c -> c.getName() + " (" + c.getType() + ")")
                .collect(Collectors.joining(", "));
            
            String prompt = String.format("""
                Based on these Android app components, write a 2-3 sentence summary of what this app does.
                Be specific about the app's purpose and main features.
                
                Components: %s
                
                Summary:
                """,
                componentsSummary
            );
            
            return inferenceService.generate(prompt);
        });
    }
    
    /**
     * Suggests improved names for use cases based on method analysis.
     */
    public String suggestUseCaseName(String methodName, String componentName) {
        if (!inferenceService.isReady()) {
            return formatMethodAsUseCase(methodName);
        }
        
        String prompt = String.format("""
            Convert this Android method name to a user-friendly action description.
            Return ONLY the action phrase (5-8 words max).
            
            Method: %s
            Component: %s
            
            User action:
            """,
            methodName,
            componentName
        );
        
        try {
            String result = inferenceService.generate(prompt);
            return result.trim().split("\n")[0]; // Take first line only
        } catch (Exception e) {
            return formatMethodAsUseCase(methodName);
        }
    }
    
    // Helper methods
    
    private String buildComponentSnippet(CodeComponent comp) {
        StringBuilder sb = new StringBuilder();
        sb.append("class ").append(comp.getName());
        if (comp.getMethods() != null) {
            for (CodeMethod m : comp.getMethods()) {
                sb.append("\n  ").append(m.getName()).append("()");
            }
        }
        return sb.toString();
    }
    
    private String getMethodNames(CodeComponent comp) {
        if (comp.getMethods() == null || comp.getMethods().isEmpty()) {
            return "none";
        }
        return comp.getMethods().stream()
            .map(CodeMethod::getName)
            .limit(10)
            .collect(Collectors.joining(", "));
    }
    
    private List<UseCaseInfo> parseUseCaseResponse(String response, String componentName, String defaultActor) {
        List<UseCaseInfo> useCases = new ArrayList<>();
        
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            // Remove numbering (1., 2., -, *)
            line = line.replaceFirst("^[0-9]+[.)\\s]+", "");
            line = line.replaceFirst("^[-*]\\s+", "");
            
            if (line.length() > 5 && line.length() < 100) {
                useCases.add(new UseCaseInfo(line, defaultActor, componentName));
            }
        }
        
        return useCases;
    }
    
    private String formatMethodAsUseCase(String methodName) {
        // Convert camelCase to readable format
        String readable = methodName
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .replaceAll("_", " ")
            .toLowerCase();
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }
    
    /**
     * Shuts down the AI service and releases resources.
     */
    public void shutdown() {
        if (inferenceService != null) {
            inferenceService.close();
        }
    }
    
    /**
     * Data class representing an AI-extracted use case.
     */
    public static class UseCaseInfo {
        private final String description;
        private final String actor;
        private final String sourceComponent;
        
        public UseCaseInfo(String description, String actor, String sourceComponent) {
            this.description = description;
            this.actor = actor;
            this.sourceComponent = sourceComponent;
        }
        
        public String getDescription() { return description; }
        public String getActor() { return actor; }
        public String getSourceComponent() { return sourceComponent; }
        
        @Override
        public String toString() {
            return actor + " -> " + description;
        }
    }
}
