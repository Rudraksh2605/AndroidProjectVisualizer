package com.projectvisualizer.models;

public class NavigationPath {
    private NavigationFlow navigationFlow;
    private String pathId;
    private boolean isConditional;
    private String pathType;
    private double pathWeight;

    // Constructors
    public NavigationPath() {
        this.isConditional = false;
        this.pathWeight = 1.0;
        this.pathType = "NAVIGATION";
    }

    public NavigationPath(NavigationFlow flow) {
        this();
        this.navigationFlow = flow;
        this.pathId = flow != null ? flow.getFlowId() : null;
        this.isConditional = flow != null && !flow.getConditions().isEmpty();
    }

    // Getters and setters
    public NavigationFlow getNavigationFlow() {
        return navigationFlow;
    }

    public void setNavigationFlow(NavigationFlow navigationFlow) {
        this.navigationFlow = navigationFlow;
    }

    public String getPathId() {
        return pathId;
    }

    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    public boolean isConditional() {
        return isConditional;
    }

    public void setConditional(boolean conditional) {
        this.isConditional = conditional;
    }

    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    public double getPathWeight() {
        return pathWeight;
    }

    public void setPathWeight(double pathWeight) {
        this.pathWeight = pathWeight;
    }
}
