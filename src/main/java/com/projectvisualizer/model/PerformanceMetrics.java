package com.projectvisualizer.model;

public class PerformanceMetrics {
    private long loadTimeMs;
    private int errorCount;
    private double averageResponseTime;
    private int memoryUsageKb;

    public PerformanceMetrics() {}

    // Getters and Setters
    public long getLoadTimeMs() { return loadTimeMs; }
    public void setLoadTimeMs(long loadTimeMs) { this.loadTimeMs = loadTimeMs; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }

    public int getMemoryUsageKb() { return memoryUsageKb; }
    public void setMemoryUsageKb(int memoryUsageKb) { this.memoryUsageKb = memoryUsageKb; }
}
