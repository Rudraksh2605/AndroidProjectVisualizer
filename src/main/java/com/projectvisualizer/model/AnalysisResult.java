package com.projectvisualizer.model;

import java.util.*;

public class AnalysisResult {

    private List<CodeComponent> components;
    private List<NavigationFlow> navigationFlows;
    private List<UserFlowComponent> userFlows;
    private Map<String, String> activityLayoutMap;
    private String error;
    private Map<String, List<CodeComponent>> categorizedComponents;

    public Map<String, List<CodeComponent>> getCategorizedComponents() {
        return categorizedComponents;
    }

    public void setCategorizedComponents(Map<String, List<CodeComponent>> categorizedComponents) {
        this.categorizedComponents = categorizedComponents;
    }

    public List<CodeComponent> getComponents() {
        return components;
    }

    public void setComponents(List<CodeComponent> components) {
        this.components = components;
    }

    public List<NavigationFlow> getNavigationFlows() {
        return navigationFlows;
    }

    public void setNavigationFlows(List<NavigationFlow> navigationFlows) {
        this.navigationFlows = navigationFlows;
    }

    public List<UserFlowComponent> getUserFlows() {
        return userFlows;
    }

    public void setUserFlows(List<UserFlowComponent> userFlows) {
        this.userFlows = userFlows;
    }

    public Map<String, String> getActivityLayoutMap() {
        return activityLayoutMap;
    }

    public void setActivityLayoutMap(Map<String, String> activityLayoutMap) {
        this.activityLayoutMap = activityLayoutMap;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
