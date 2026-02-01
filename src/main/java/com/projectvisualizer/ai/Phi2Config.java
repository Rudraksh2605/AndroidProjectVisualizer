package com.projectvisualizer.ai;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for DeepSeek-Coder 6.7B Instruct LLM model settings.
 * Handles model file paths, GPU configuration, and inference parameters.
 */
public class Phi2Config {

    // Model settings - DeepSeek-Coder 6.7B Instruct
    private static final String DEFAULT_MODEL_NAME = "deepseek-coder-6.7b-instruct.Q4_K_M.gguf";
    private static final String DEFAULT_EXTERNAL_MODEL_PATH = "C:\\Users\\rudra\\Downloads\\deepseek-coder-6.7b-instruct.Q4_K_M.gguf";
    private static final String MODELS_DIR = "models";

    // Inference parameters - optimized for DeepSeek-Coder
    private int contextSize = 2048; // Reduced to 2048 to fit in 6GB VRAM (KV cache consumes ~1GB)
    private int maxTokens = 1024; // Allow longer responses for detailed diagrams
    private float temperature = 0.3f; // Lower temperature for more deterministic code/diagram generation
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;

    // GPU Configuration - optimized for RTX 3060 with 12GB VRAM
    private int gpuLayers = 50; // Offload more layers to GPU for 6.7B model
    private boolean useGpu = true;
    private int mainGpu = 0; // User specified GPU index
    private int nThreads = 4; // Default to 4 threads for CPU inference

    // Model path
    private String modelPath;

    public Phi2Config() {
        // Default model path relative to application directory
        this.modelPath = resolveModelPath();
    }

    /**
     * Resolves the path to the DeepSeek-Coder GGUF model file.
     * Searches in the following order:
     * 1. User's Downloads folder (external model path)
     * 2. Application's models directory
     * 3. User's home directory/.projectvisualizer/models
     */
    private String resolveModelPath() {
        // Try external model path first (user's Downloads folder)
        File externalModel = new File(DEFAULT_EXTERNAL_MODEL_PATH);
        if (externalModel.exists()) {
            return DEFAULT_EXTERNAL_MODEL_PATH;
        }

        // Try application's models directory
        Path appModelPath = Paths.get(System.getProperty("user.dir"), MODELS_DIR, DEFAULT_MODEL_NAME);
        if (appModelPath.toFile().exists()) {
            return appModelPath.toString();
        }

        // Try user's home directory
        Path userModelPath = Paths.get(System.getProperty("user.home"), ".projectvisualizer", MODELS_DIR,
                DEFAULT_MODEL_NAME);
        if (userModelPath.toFile().exists()) {
            return userModelPath.toString();
        }

        // Return external path as default (where user has the model)
        return DEFAULT_EXTERNAL_MODEL_PATH;
    }

    /**
     * Checks if the model file exists and is ready for use.
     */
    public boolean isModelAvailable() {
        File modelFile = new File(modelPath);
        return modelFile.exists() && modelFile.canRead() && modelFile.length() > 1_000_000; // > 1MB
    }

    /**
     * Gets the download URL for the DeepSeek-Coder GGUF model.
     */
    public String getModelDownloadUrl() {
        return "https://huggingface.co/TheBloke/deepseek-coder-6.7B-instruct-GGUF/resolve/main/deepseek-coder-6.7b-instruct.Q4_K_M.gguf";
    }

    /**
     * Gets the expected model file size for progress tracking.
     */
    public long getExpectedModelSize() {
        return 3_800_000_000L; // ~3.8 GB for DeepSeek-Coder 6.7B Q4_K_M quantization
    }

    // Getters and Setters

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = Math.min(Math.max(contextSize, 512), 16384); // DeepSeek supports up to 16K context
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = Math.min(Math.max(maxTokens, 32), 2048);
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = Math.min(Math.max(temperature, 0.0f), 2.0f);
    }

    public float getTopP() {
        return topP;
    }

    public void setTopP(float topP) {
        this.topP = Math.min(Math.max(topP, 0.0f), 1.0f);
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = Math.min(Math.max(topK, 1), 100);
    }

    public float getRepeatPenalty() {
        return repeatPenalty;
    }

    public void setRepeatPenalty(float repeatPenalty) {
        this.repeatPenalty = Math.min(Math.max(repeatPenalty, 1.0f), 2.0f);
    }

    public int getGpuLayers() {
        return gpuLayers;
    }

    public void setGpuLayers(int gpuLayers) {
        this.gpuLayers = Math.max(gpuLayers, 0);
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean useGpu) {
        this.useGpu = useGpu;
        if (!useGpu) {
            this.gpuLayers = 0;
        }
    }

    public int getMainGpu() {
        return mainGpu;
    }

    public void setMainGpu(int mainGpu) {
        this.mainGpu = mainGpu;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * Gets the models directory path.
     */
    public String getModelsDirectory() {
        return Paths.get(System.getProperty("user.dir"), MODELS_DIR).toString();
    }

    public int getNThreads() {
        return nThreads;
    }

    public void setNThreads(int nThreads) {
        this.nThreads = Math.max(1, Math.min(nThreads, Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public String toString() {
        return String.format(
                "DeepSeekCoderConfig{modelPath='%s', gpuLayers=%d, contextSize=%d, maxTokens=%d, temp=%.2f, threads=%d}",
                modelPath, gpuLayers, contextSize, maxTokens, temperature, nThreads);
    }
}
