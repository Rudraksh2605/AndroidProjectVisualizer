package com.projectvisualizer.model;

public class NavigationPath {
    private NavigationFlow navigationFlow;
    private String description;

    public NavigationPath() {}

    public NavigationPath(NavigationFlow navigationFlow) {
        this.navigationFlow = navigationFlow;
    }

    // Getters and Setters
    public NavigationFlow getNavigationFlow() { return navigationFlow; }
    public void setNavigationFlow(NavigationFlow navigationFlow) { this.navigationFlow = navigationFlow; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
