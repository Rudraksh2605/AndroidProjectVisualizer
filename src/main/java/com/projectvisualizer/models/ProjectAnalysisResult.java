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
}