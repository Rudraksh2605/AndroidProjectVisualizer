// ProjectAnalysisResult.java
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisResult {
    private List<CodeComponent> components;
    private List<NavigationFlow> navigationFlows;
    private List<UserFlowComponent> userFlows;
    private List<BusinessProcessComponent> businessProcesses;
    private List<ComponentRelationship> relationships;

    public ProjectAnalysisResult() {
        this.components = new ArrayList<>();
        this.navigationFlows = new ArrayList<>();
        this.userFlows = new ArrayList<>();
        this.businessProcesses = new ArrayList<>();
        this.relationships = new ArrayList<>();
    }

    // Getters and Setters
    public List<CodeComponent> getComponents() { return components; }
    public void setComponents(List<CodeComponent> components) { this.components = components; }

    public List<NavigationFlow> getNavigationFlows() { return navigationFlows; }
    public void setNavigationFlows(List<NavigationFlow> navigationFlows) { this.navigationFlows = navigationFlows; }

    public List<UserFlowComponent> getUserFlows() { return userFlows; }
    public void setUserFlows(List<UserFlowComponent> userFlows) { this.userFlows = userFlows; }

    public List<BusinessProcessComponent> getBusinessProcesses() { return businessProcesses; }
    public void setBusinessProcesses(List<BusinessProcessComponent> businessProcesses) { this.businessProcesses = businessProcesses; }

    public List<ComponentRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<ComponentRelationship> relationships) { this.relationships = relationships; }

    // Helper methods
    public void addComponent(CodeComponent component) {
        this.components.add(component);
    }

    public void addNavigationFlow(NavigationFlow flow) {
        this.navigationFlows.add(flow);
    }

    public void addUserFlow(UserFlowComponent userFlow) {
        this.userFlows.add(userFlow);
    }

    public void addBusinessProcess(BusinessProcessComponent process) {
        this.businessProcesses.add(process);
    }

    public void addRelationship(ComponentRelationship relationship) {
        this.relationships.add(relationship);
    }

    public List<CodeComponent> getUIComponents() {
        return filterUIComponents(this.components);
    }

    public List<CodeComponent> getBusinessLogicComponents() {
        return filterComponentsByLayer(this.components, "Business Logic");
    }

    public List<CodeComponent> getDataComponents() {
        return filterComponentsByLayer(this.components, "Data");
    }

    private List<CodeComponent> filterUIComponents(List<CodeComponent> allComponents) {
        List<CodeComponent> uiComponents = new ArrayList<>();

        for (CodeComponent component : allComponents) {
            if (isUIComponent(component)) {
                uiComponents.add(component);
            }
        }

        return uiComponents;
    }

    private List<CodeComponent> filterComponentsByLayer(List<CodeComponent> allComponents, String targetLayer) {
        List<CodeComponent> filteredComponents = new ArrayList<>();

        for (CodeComponent component : allComponents) {
            if (component != null && targetLayer.equals(component.getLayer())) {
                filteredComponents.add(component);
            }
        }

        return filteredComponents;
    }

    private boolean isUIComponent(CodeComponent component) {
        if (component == null || component.getName() == null) return false;

        String layer = component.getLayer();
        String name = component.getName().toLowerCase();
        String extendsClass = component.getExtendsClass();

        // Explicit UI layer
        if ("UI".equals(layer)) {
            return true;
        }

        // Check for UI patterns in name and inheritance
        return name.endsWith("activity") ||
                name.endsWith("fragment") ||
                name.endsWith("adapter") ||
                name.endsWith("viewholder") ||
                name.contains("screen") ||
                name.contains("page") ||
                name.contains("dialog") ||
                (extendsClass != null &&
                        (extendsClass.endsWith("Activity") ||
                                extendsClass.endsWith("Fragment") ||
                                extendsClass.contains("android.app.Activity") ||
                                extendsClass.contains("androidx.fragment.app.Fragment")));
    }

}