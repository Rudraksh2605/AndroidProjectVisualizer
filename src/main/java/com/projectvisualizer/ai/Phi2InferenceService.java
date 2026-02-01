package com.projectvisualizer.ai;

import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaOutput;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core inference service for DeepSeek-Coder 6.7B Instruct LLM.
 * Handles model loading, GPU configuration, and text generation.
 * Optimized for code analysis, UML diagrams, and use case generation.
 */
public class Phi2InferenceService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Phi2InferenceService.class);

    static {
        try {
            String nativePath = java.nio.file.Paths.get("natives").toAbsolutePath().toString();
            System.setProperty("de.kherud.llama.lib.path", nativePath);
            System.out.println("DEBUG: Forcing de.kherud.llama.lib.path to: " + nativePath);

            // Also add to java.library.path just in case
            String currentPath = System.getProperty("java.library.path");
            System.setProperty("java.library.path", nativePath + java.io.File.pathSeparator + currentPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LlamaModel model;
    private final Phi2Config config;
    private final ExecutorService executor;
    private volatile boolean isLoaded = false;
    private volatile boolean isLoading = false;

    public Phi2InferenceService() {
        this(new Phi2Config());
    }

    public Phi2InferenceService(Phi2Config config) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DeepSeek-Coder-Inference-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Loads the Phi-2 model asynchronously.
     * 
     * @param progressCallback Optional callback for loading progress updates
     * @return CompletableFuture that completes when model is loaded
     */
    public CompletableFuture<Boolean> loadModelAsync(Consumer<String> progressCallback) {
        if (isLoaded) {
            return CompletableFuture.completedFuture(true);
        }

        if (isLoading) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                isLoading = true;

                if (progressCallback != null) {
                    progressCallback.accept("Checking model availability...");
                }

                if (!config.isModelAvailable()) {
                    String errorMsg = "DeepSeek-Coder model not found at: " + config.getModelPath() +
                            "\nPlease ensure the model exists at: C:\\Users\\rudra\\Downloads\\deepseek-coder-6.7b-instruct.Q4_K_M.gguf";
                    logger.error(errorMsg);
                    if (progressCallback != null) {
                        progressCallback.accept("ERROR: " + errorMsg);
                    }
                    return false;
                }

                if (progressCallback != null) {
                    progressCallback.accept("Loading DeepSeek-Coder 6.7B with GPU acceleration...");
                }

                // Configure model parameters
                ModelParameters modelParams = new ModelParameters()
                        .setModel(config.getModelPath())
                        .setGpuLayers(config.isUseGpu() ? config.getGpuLayers() : 0)
                        .setCtxSize(config.getContextSize())
                        .setMainGpu(config.getMainGpu())

                        .setLogVerbosity(1)
                        .setVerbose();

                // Lower thread priority to prevent UI freezing
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                logger.info("Loading model with config: {}", config);
                model = new LlamaModel(modelParams);
                isLoaded = true;

                if (progressCallback != null) {
                    progressCallback
                            .accept("DeepSeek-Coder 6.7B loaded successfully! GPU layers: " + config.getGpuLayers());
                }

                logger.info("DeepSeek-Coder 6.7B model loaded successfully");
                return true;

            } catch (Exception e) {
                logger.error("Failed to load DeepSeek-Coder model", e);
                if (progressCallback != null) {
                    progressCallback.accept("ERROR: Failed to load model - " + e.getMessage());
                }
                return false;
            } finally {
                isLoading = false;
            }
        }, executor);
    }

    /**
     * Generates text completion for the given prompt.
     * 
     * @param prompt The input prompt
     * @return Generated text response
     */
    public String generate(String prompt) {
        if (!isLoaded) {
            throw new IllegalStateException("Model not loaded. Call loadModelAsync() first.");
        }

        InferenceParameters inferParams = new InferenceParameters(prompt)
                .setTemperature(config.getTemperature())
                .setTopP(config.getTopP())
                .setTopK(config.getTopK())
                .setRepeatPenalty(config.getRepeatPenalty())
                .setNPredict(config.getMaxTokens());

        StringBuilder response = new StringBuilder();

        for (LlamaOutput output : model.generate(inferParams)) {
            response.append(output);
        }

        return response.toString().trim();
    }

    /**
     * Generates text completion asynchronously with streaming support.
     * 
     * @param prompt        The input prompt
     * @param tokenCallback Callback called for each generated token
     * @return CompletableFuture with the complete response
     */
    public CompletableFuture<String> generateAsync(String prompt, Consumer<String> tokenCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isLoaded) {
                throw new IllegalStateException("Model not loaded. Call loadModelAsync() first.");
            }

            InferenceParameters inferParams = new InferenceParameters(prompt)
                    .setTemperature(config.getTemperature())
                    .setTopP(config.getTopP())
                    .setTopK(config.getTopK())
                    .setRepeatPenalty(config.getRepeatPenalty())
                    .setNPredict(config.getMaxTokens());

            StringBuilder response = new StringBuilder();

            for (LlamaOutput output : model.generate(inferParams)) {
                String token = output.toString();
                response.append(token);
                if (tokenCallback != null) {
                    tokenCallback.accept(token);
                }
            }

            return response.toString().trim();
        }, executor);
    }

    /**
     * Analyzes code and generates insights using DeepSeek-Coder 6.7B.
     * Optimized for code understanding, UML diagrams, and use case generation.
     * 
     * @param codeContent  The code to analyze
     * @param analysisType Type of analysis (e.g., "use_cases", "architecture",
     *                     "summary")
     * @return Analysis result
     */
    public String analyzeCode(String codeContent, String analysisType) {
        String prompt = buildAnalysisPrompt(codeContent, analysisType);
        return generate(prompt);
    }

    /**
     * Builds a prompt for code analysis based on the analysis type.
     */
    private String buildAnalysisPrompt(String codeContent, String analysisType) {
        String systemContext = """
                You are DeepSeek-Coder, an expert AI assistant specialized in code analysis and software architecture.
                You are analyzing Android/Java/Kotlin code to generate UML diagrams and use case documentation.
                Analyze the following code and provide comprehensive, detailed insights.
                Be specific about what the application does based on the actual code.
                Focus on generating accurate PlantUML-compatible output when requested.
                """;

        String taskInstructions = switch (analysisType.toLowerCase()) {
            case "use_cases" -> """
                    Identify the main use cases from this Android application code.
                    List each use case as: "Actor -> Action -> Result"
                    Focus on user-facing functionality, not internal implementation details.
                    """;
            case "architecture" -> """
                    Describe the architecture pattern used in this code.
                    Identify the main components and their relationships.
                    List any design patterns you observe.
                    """;
            case "summary" -> """
                    Provide a brief summary of what this application does.
                    List the main features in bullet points.
                    """;
            case "dependencies" -> """
                    Identify the key dependencies and how components interact.
                    Focus on data flow and control flow between classes.
                    """;
            // New documentation-specific prompts - STRONGLY FORMATTED
            case "doc_overview" -> """
                    You must write a project overview in THIS EXACT FORMAT:

                    ## App Name
                    [Insert app name based on package/class names]

                    ## Purpose
                    [One paragraph explaining what this app does - be specific about the actual functionality you see]

                    ## Target Users
                    [Who uses this app]

                    ## Core Features
                    - Feature 1: [description]
                    - Feature 2: [description]
                    - Feature 3: [description]

                    IMPORTANT: Look at the actual class names, methods, and layouts.
                    If you see "Login", "SignUp" = authentication features.
                    If you see "Home", "Lessons" = education/learning features.
                    If you see "ChatAdapter" = messaging features.
                    BE SPECIFIC. Do not say "without more information" - use what you see!
                    """;
            case "doc_screens" -> """
                    List every Activity and Fragment you see in the code. Use THIS FORMAT:

                    ### [ScreenName]Activity or [ScreenName]Fragment
                    **Purpose:** [What users do here]
                    **UI Elements:** [Buttons, lists, inputs visible]
                    **Navigation:** Goes to -> [other screens]

                    List ALL Activities and Fragments. Do not skip any.
                    """;
            case "doc_features" -> """
                    You MUST list ALL features. Use THIS EXACT FORMAT:

                    ### Authentication
                    - Login: [how it works]
                    - Registration: [how it works]

                    ### Main Features
                    - [Feature from actual code]: [description]

                    ### Data Storage
                    - [Database/storage features]

                    ### Communication
                    - [Chat/messaging features if present]

                    IMPORTANT: Look at method names!
                    - Methods like "login()", "signUp()" = Authentication
                    - Methods like "sendMessage()" = Chat
                    - Methods like "loadLessons()" = Education
                    BE SPECIFIC about what you actually see in the code!
                    """;
            case "doc_architecture" -> """
                    Describe the architecture. Use THIS FORMAT:

                    ## Architecture Pattern
                    [MVVM/MVP/MVC - explain based on what classes you see]

                    ## Layers
                    **Presentation:** [Activities, Fragments, ViewModels]
                    **Business Logic:** [Repositories, Use Cases, Services]
                    **Data:** [DAOs, APIs, Database]

                    ## Key Components
                    | Component | Type | Purpose |
                    |-----------|------|---------|
                    | [Name] | [Activity/ViewModel/Repository] | [What it does] |

                    ## Data Flow
                    User -> [Component] -> [Component] -> [Data Source]

                    Use actual class names from the code!
                    """;
            case "doc_tech_stack" -> """
                    List ALL technologies detected. Use THIS FORMAT:

                    ## Language
                    - Primary: [Java/Kotlin]

                    ## Android Components
                    - [List Android SDK features used]

                    ## Libraries
                    - [List any libraries like Room, Retrofit, Hilt you detect]

                    ## UI
                    - [XML Views or Jetpack Compose]

                    Look at imports, annotations, and class names to detect technologies.
                    """;
            case "doc_data_flow" -> """
                    Analyze the data flow in this application. Use THIS EXACT FORMAT:

                    ## Data Flow Architecture
                    [Explain how data moves, e.g., API -> Repository -> ViewModel -> UI]

                    ## Key Data Paths
                    - **User Action**: [Action] -> [Component] -> [Result]
                    - **Data Loading**: [Source] -> [Repository] -> [UI]

                    ## State Management
                    [Explain how state is handled, e.g., LiveData, StateFlow]

                    Use actual class names where possible. Write in clear, human-readable English.
                    """;
            case "doc_apis" ->
                """
                        List all API endpoints and network operations. Use THIS EXACT FORMAT:

                        ## API Endpoints
                        - `[Method] [Path]` - [Description]

                        ## Network Clients
                        - [Client Class Name]: [Purpose]

                        ## Data Models
                        - [Model Class]: [What it represents]

                        If no APIs are explicitly defined (e.g., no Retrofit), describe how data is fetched or say "No external APIs detected".
                        Write in clear, human-readable English.
                        """;
            case "doc_class_details" -> """
                    Provide a functionality summary for these classes. Use THIS EXACT FORMAT:

                    ### [ClassName]
                    **Role**: [One sentence describing its role]
                    **Key Methods**:
                    - `[methodName]`: [What it does in plain English]

                    Do this for the most important classes listed. Write in clear, human-readable English.
                    """;
            case "doc_di_graph" -> """
                    Analyze the Dependency Injection (DI) structure. Use THIS EXACT FORMAT:

                    ## Dependency Injection Graph

                    ### Components & Modules
                    - [Module/Component Name]: [Purpose]

                    ### Injections
                    - **[Target Class]** receives:
                      - [Injected Dependency 1]
                      - [Injected Dependency 2]

                    Write in clear, human-readable English.
                    """;

            default -> "Analyze this code and provide relevant insights.";
        };

        return String.format("""
                %s

                TASK: %s

                CODE:
                ```
                %s
                ```

                ANALYSIS:
                """, systemContext, taskInstructions, truncateCode(codeContent));
    }

    /**
     * Truncates code to fit within context window.
     */
    private String truncateCode(String code) {
        int maxChars = config.getContextSize() * 3; // Approximate chars per token
        if (code.length() > maxChars) {
            return code.substring(0, maxChars) + "\n... [truncated]";
        }
        return code;
    }

    /**
     * Checks if the model is loaded and ready for inference.
     */
    public boolean isReady() {
        return isLoaded;
    }

    /**
     * Checks if the model is currently loading.
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Gets the current configuration.
     */
    public Phi2Config getConfig() {
        return config;
    }

    @Override
    public void close() {
        if (model != null) {
            try {
                model.close();
                logger.info("DeepSeek-Coder model unloaded");
            } catch (Exception e) {
                logger.error("Error closing model", e);
            }
        }
        executor.shutdown();
        isLoaded = false;
    }
}
