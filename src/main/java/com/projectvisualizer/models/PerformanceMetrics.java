package com.projectvisualizer.models;

/**
 * PerformanceMetrics class to store performance data for user flow components
 */
public class PerformanceMetrics {
    private long loadTimeMs;
    private int errorCount;
    private double averageResponseTime;
    private int userInteractions;
    private double memoryUsage;
    private double cpuUsage;
    private double networkUsage;
    private long timestamp;

    // Default constructor
    public PerformanceMetrics() {
        this.loadTimeMs = 0L;
        this.errorCount = 0;
        this.averageResponseTime = 0.0;
        this.userInteractions = 0;
        this.memoryUsage = 0.0;
        this.cpuUsage = 0.0;
        this.networkUsage = 0.0;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor with basic metrics
    public PerformanceMetrics(long loadTimeMs, int errorCount, double averageResponseTime) {
        this();
        this.loadTimeMs = loadTimeMs;
        this.errorCount = errorCount;
        this.averageResponseTime = averageResponseTime;
    }

    // Constructor with all metrics
    public PerformanceMetrics(long loadTimeMs, int errorCount, double averageResponseTime,
                              int userInteractions, double memoryUsage, double cpuUsage) {
        this(loadTimeMs, errorCount, averageResponseTime);
        this.userInteractions = userInteractions;
        this.memoryUsage = memoryUsage;
        this.cpuUsage = cpuUsage;
    }

    // Getters and Setters
    public long getLoadTimeMs() {
        return loadTimeMs;
    }

    public void setLoadTimeMs(long loadTimeMs) {
        this.loadTimeMs = loadTimeMs;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public int getUserInteractions() {
        return userInteractions;
    }

    public void setUserInteractions(int userInteractions) {
        this.userInteractions = userInteractions;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getNetworkUsage() {
        return networkUsage;
    }

    public void setNetworkUsage(double networkUsage) {
        this.networkUsage = networkUsage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods
    public void incrementErrorCount() {
        this.errorCount++;
    }

    public void incrementUserInteractions() {
        this.userInteractions++;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean isHighMemoryUsage() {
        return memoryUsage > 80.0; // Consider > 80% as high usage
    }

    public boolean isHighCpuUsage() {
        return cpuUsage > 70.0; // Consider > 70% as high usage
    }

    // Calculate performance score (0-100, higher is better)
    public double getPerformanceScore() {
        double score = 100.0;

        // Deduct points for high load time (>2 seconds is bad)
        if (loadTimeMs > 2000) {
            score -= Math.min(30, (loadTimeMs - 2000) / 100.0);
        }

        // Deduct points for errors
        score -= Math.min(25, errorCount * 5);

        // Deduct points for high resource usage
        if (isHighMemoryUsage()) {
            score -= 15;
        }
        if (isHighCpuUsage()) {
            score -= 15;
        }

        // Deduct points for slow response time (>1 second is bad)
        if (averageResponseTime > 1000) {
            score -= Math.min(15, (averageResponseTime - 1000) / 200.0);
        }

        return Math.max(0, score);
    }

    @Override
    public String toString() {
        return "PerformanceMetrics{" +
                "loadTimeMs=" + loadTimeMs +
                ", errorCount=" + errorCount +
                ", averageResponseTime=" + averageResponseTime +
                ", userInteractions=" + userInteractions +
                ", memoryUsage=" + memoryUsage +
                ", cpuUsage=" + cpuUsage +
                ", performanceScore=" + getPerformanceScore() +
                '}';
    }
}
