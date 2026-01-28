package com.projectvisualizer.ai;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for Microsoft Phi-2 LLM model settings.
 * Handles model file paths, GPU configuration, and inference parameters.
 */
public class Phi2Config {
    
    // Model settings
    private static final String DEFAULT_MODEL_NAME = "phi-2.Q4_K_M.gguf";
    private static final String MODELS_DIR = "models";
    
    // Inference parameters
    private int contextSize = 2048;
    private int maxTokens = 512;
    private float temperature = 0.7f;
    private float topP = 0.9f;
    private int topK = 40;
    private float repeatPenalty = 1.1f;
    
    // GPU Configuration
    private int gpuLayers = 35; // Number of layers to offload to GPU (0 = CPU only)
    private boolean useGpu = true;
    
    // Model path
    private String modelPath;
    
    public Phi2Config() {
        // Default model path relative to application directory
        this.modelPath = resolveModelPath();
    }
    
    /**
     * Resolves the path to the Phi-2 GGUF model file.
     * Searches in the following order:
     * 1. Application's models directory
     * 2. User's home directory/.projectvisualizer/models
     */
    private String resolveModelPath() {
        // Try application's models directory first
        Path appModelPath = Paths.get(System.getProperty("user.dir"), MODELS_DIR, DEFAULT_MODEL_NAME);
        if (appModelPath.toFile().exists()) {
            return appModelPath.toString();
        }
        
        // Try user's home directory
        Path userModelPath = Paths.get(System.getProperty("user.home"), ".projectvisualizer", MODELS_DIR, DEFAULT_MODEL_NAME);
        if (userModelPath.toFile().exists()) {
            return userModelPath.toString();
        }
        
        // Return default path (may need to be downloaded)
        return appModelPath.toString();
    }
    
    /**
     * Checks if the model file exists and is ready for use.
     */
    public boolean isModelAvailable() {
        File modelFile = new File(modelPath);
        return modelFile.exists() && modelFile.canRead() && modelFile.length() > 1_000_000; // > 1MB
    }
    
    /**
     * Gets the download URL for the Phi-2 GGUF model.
     */
    public String getModelDownloadUrl() {
        return "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf";
    }
    
    /**
     * Gets the expected model file size for progress tracking.
     */
    public long getExpectedModelSize() {
        return 1_620_000_000L; // ~1.62 GB for Q4_K_M quantization
    }
    
    // Getters and Setters
    
    public int getContextSize() {
        return contextSize;
    }
    
    public void setContextSize(int contextSize) {
        this.contextSize = Math.min(Math.max(contextSize, 512), 4096);
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
    
    @Override
    public String toString() {
        return String.format(
            "Phi2Config{modelPath='%s', gpuLayers=%d, contextSize=%d, maxTokens=%d, temp=%.2f}",
            modelPath, gpuLayers, contextSize, maxTokens, temperature
        );
    }
}
