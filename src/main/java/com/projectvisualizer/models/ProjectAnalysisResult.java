package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisResult {
    private String projectName;
    private String projectPath;
    private List<CodeComponent> components = new ArrayList<>();
    private List<ComponentRelationship> relationships = new ArrayList<>();
    private List<Dependency> gradleDependencies = new ArrayList<>();
    private List<Dependency> flutterDependencies = new ArrayList<>();
    private List<Dependency> jsDependencies = new ArrayList<>();
    private List<UserFlowComponent> userFlows = new ArrayList<>();
    private List<BusinessProcessComponent> businessProcesses = new ArrayList<>();
    private List<FeatureGroup> featureGroups = new ArrayList<>();
    private List<NavigationFlow> navigationFlows = new ArrayList<>();


    // Getters and setters
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public List<CodeComponent> getComponents() { return components; }
    public void setComponents(List<CodeComponent> components) { this.components = components; }
    public void addComponent(CodeComponent component) { this.components.add(component); }
    public void addComponents(List<CodeComponent> components) { this.components.addAll(components); }

    public List<ComponentRelationship> getRelationships() { return relationships; }
    public void setRelationships(List<ComponentRelationship> relationships) { this.relationships = relationships; }
    public void addRelationship(ComponentRelationship relationship) { this.relationships.add(relationship); }

    public List<Dependency> getGradleDependencies() { return gradleDependencies; }
    public void setGradleDependencies(List<Dependency> gradleDependencies) { this.gradleDependencies = gradleDependencies; }
    public void addGradleDependencies(List<Dependency> dependencies) { this.gradleDependencies.addAll(dependencies); }

    public List<Dependency> getFlutterDependencies() { return flutterDependencies; }
    public void setFlutterDependencies(List<Dependency> flutterDependencies) { this.flutterDependencies = flutterDependencies; }
    public void addFlutterDependencies(List<Dependency> dependencies) { this.flutterDependencies.addAll(dependencies); }

    public List<Dependency> getJsDependencies() { return jsDependencies; }
    public void setJsDependencies(List<Dependency> jsDependencies) { this.jsDependencies = jsDependencies; }
    public void addJSDependencies(List<Dependency> dependencies) { this.jsDependencies.addAll(dependencies); }

    public List<UserFlowComponent> getUserFlows() {
        return userFlows;
    }

    public void setUserFlows(List<UserFlowComponent> userFlows) {
        this.userFlows = userFlows;
    }

    public List<BusinessProcessComponent> getBusinessProcesses() {
        return businessProcesses;
    }

    public void setBusinessProcesses(List<BusinessProcessComponent> businessProcesses) {
        this.businessProcesses = businessProcesses;
    }

    public CodeComponent findComponentById(String id) {
        for (CodeComponent comp : components) {
            if (comp.getId().equals(id)) {
                return comp;
            }
        }
        return null;
    }

    public void addUserFlow(UserFlowComponent flow) { this.userFlows.add(flow); }

    public List<NavigationFlow> getNavigationFlows() {
        return navigationFlows;
    }

    public void setNavigationFlows(List<NavigationFlow> navigationFlows) {
        this.navigationFlows = navigationFlows;
    }

    public void addNavigationFlow(NavigationFlow navigationFlow) {
        this.navigationFlows.add(navigationFlow);
    }

    // Add these missing getters/setters for feature groups
    public List<FeatureGroup> getFeatureGroups() {
        return featureGroups;
    }

    public void setFeatureGroups(List<FeatureGroup> featureGroups) {
        this.featureGroups = featureGroups;
    }

    public void addFeatureGroup(FeatureGroup featureGroup) {
        this.featureGroups.add(featureGroup);
    }

}
