package com.projectvisualizer.visualization;

public class VisualizationConfig {
    private boolean enableClustering = true;
    private boolean enableEdgeAggregation = true;
    private int maxNodesPerCluster = 10;
    private int minClusterSize = 3;
    private boolean showComponentCounts = true;
    private boolean showMethodCounts = false;
    private String colorScheme = "modern";
    private double edgeThreshold = 0.1;
    private boolean hideUtilityClasses = true;
    private boolean hideTestClasses = true;

    // Getters and setters
    public boolean isEnableClustering() { return enableClustering; }
    public void setEnableClustering(boolean enableClustering) { this.enableClustering = enableClustering; }

    public boolean isEnableEdgeAggregation() { return enableEdgeAggregation; }
    public void setEnableEdgeAggregation(boolean enableEdgeAggregation) { this.enableEdgeAggregation = enableEdgeAggregation; }

    public int getMaxNodesPerCluster() { return maxNodesPerCluster; }
    public void setMaxNodesPerCluster(int maxNodesPerCluster) { this.maxNodesPerCluster = maxNodesPerCluster; }

    public int getMinClusterSize() { return minClusterSize; }
    public void setMinClusterSize(int minClusterSize) { this.minClusterSize = minClusterSize; }

    public boolean isShowComponentCounts() { return showComponentCounts; }
    public void setShowComponentCounts(boolean showComponentCounts) { this.showComponentCounts = showComponentCounts; }

    public boolean isShowMethodCounts() { return showMethodCounts; }
    public void setShowMethodCounts(boolean showMethodCounts) { this.showMethodCounts = showMethodCounts; }

    public String getColorScheme() { return colorScheme; }
    public void setColorScheme(String colorScheme) { this.colorScheme = colorScheme; }

    public double getEdgeThreshold() { return edgeThreshold; }
    public void setEdgeThreshold(double edgeThreshold) { this.edgeThreshold = edgeThreshold; }

    public boolean isHideUtilityClasses() { return hideUtilityClasses; }
    public void setHideUtilityClasses(boolean hideUtilityClasses) { this.hideUtilityClasses = hideUtilityClasses; }

    public boolean isHideTestClasses() { return hideTestClasses; }
    public void setHideTestClasses(boolean hideTestClasses) { this.hideTestClasses = hideTestClasses; }
}
