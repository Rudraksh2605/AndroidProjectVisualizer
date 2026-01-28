# Phi-2 AI Integration Setup

This document explains how to set up the Microsoft Phi-2 AI model for enhanced project analysis in Android Project Visualizer.

## Prerequisites

1. **NVIDIA GPU with CUDA support** (recommended for GPU acceleration)
2. **CUDA Toolkit 11.x or 12.x** installed
3. **At least 4GB VRAM** (for full model loading)

## Model Download

The Phi-2 model is **not bundled** with the application due to its size (~1.6GB). You need to download it manually:

### Option 1: Direct Download
Download the quantized GGUF model:
```
https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf
```

### Option 2: Using Hugging Face CLI
```bash
huggingface-cli download TheBloke/phi-2-GGUF phi-2.Q4_K_M.gguf --local-dir ./models
```

## Installation

1. Create a `models` directory in the application root:
   ```
   AndroidProjectVisualizer/
   └── models/
       └── phi-2.Q4_K_M.gguf  <-- Place the model here
   ```

2. Run the application. The AI will automatically detect and load the model.

## Verification

When the model is loaded correctly, you should see:
- "AI Enhancement: Ready (GPU accelerated)" in the status bar
- The "AI Analysis" option enabled in the Use Case diagram generator

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Model not found" | Ensure the .gguf file is in the `models/` directory |
| "CUDA not available" | Install CUDA Toolkit and update GPU drivers |
| Slow inference | Increase `gpuLayers` in Phi2Config for more GPU offloading |
| Out of memory | Use a smaller quantized model (Q2_K or Q3_K) |

## GPU vs CPU

- **GPU Mode**: Uses CUDA for fast inference (~1-2 seconds per analysis)
- **CPU Mode**: Falls back automatically if no GPU available (~10-30 seconds)

To force CPU mode, set `useGpu = false` in `Phi2Config.java`.
