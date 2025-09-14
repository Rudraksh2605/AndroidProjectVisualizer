package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class CodeComponent {
    private String id;
    private String name;
    private String type;
    private String filePath;
    private String language;
    private String extendsClass;
    private String layer; // NEW: UI, Business Logic, Data, etc.
    private List<String> implementsList = new ArrayList<>();
    private List<CodeComponent> dependencies = new ArrayList<>();
    private List<CodeMethod> methods = new ArrayList<>();
    private List<CodeField> fields = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<String> injectedDependencies = new ArrayList<>();
    private List<String> layoutFiles = new ArrayList<>(); // NEW: Associated layout files
    private List<NavigationDestination> navigationDestinations = new ArrayList<>(); // NEW: Navigation info

    public String getDaggerComponentType() {
        return daggerComponentType;
    }

    public void setDaggerComponentType(String daggerComponentType) {
        this.daggerComponentType = daggerComponentType;
    }

    private String daggerComponentType;

    public boolean isHiltComponent() {
        return hiltComponent;
    }

    public void setHiltComponent(boolean hiltComponent) {
        this.hiltComponent = hiltComponent;
    }

    private boolean hiltComponent;

    public String getHiltComponentType() {
        return hiltComponentType;
    }

    public void setHiltComponentType(String hiltComponentType) {
        this.hiltComponentType = hiltComponentType;
    }

    private String hiltComponentType;
    private List<String> daggerDependencies = new ArrayList<>();
    private boolean hasDaggerInjection;

    public List<String> getDaggerDependencies() {
        return daggerDependencies;
    }

    public void setDaggerDependencies(List<String> daggerDependencies) {
        this.daggerDependencies = daggerDependencies;
    }



    public List<String> getDaggerInjectedDependencies() {
        return daggerInjectedDependencies;
    }

    public void setDaggerInjectedDependencies(List<String> daggerInjectedDependencies) {
        this.daggerInjectedDependencies = daggerInjectedDependencies;
    }

    private List<String> daggerInjectedDependencies = new ArrayList<>();

    public boolean isHasDaggerInjection() {
        return hasDaggerInjection;
    }

    public void setHasDaggerInjection(boolean hasDaggerInjection) {
        this.hasDaggerInjection = hasDaggerInjection;
    }


    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getExtendsClass() { return extendsClass; }
    public void setExtendsClass(String extendsClass) { this.extendsClass = extendsClass; }

    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }

    public List<String> getImplementsList() { return implementsList; }
    public void setImplementsList(List<String> implementsList) { this.implementsList = implementsList; }

    public List<CodeComponent> getDependencies() { return dependencies; }
    public void setDependencies(List<CodeComponent> dependencies) { this.dependencies = dependencies; }

    public List<CodeMethod> getMethods() { return methods; }
    public void setMethods(List<CodeMethod> methods) { this.methods = methods; }

    public List<CodeField> getFields() { return fields; }
    public void setFields(List<CodeField> fields) { this.fields = fields; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public List<String> getInjectedDependencies() { return injectedDependencies; }
    public void setInjectedDependencies(List<String> injectedDependencies) { this.injectedDependencies = injectedDependencies; }

    public List<String> getLayoutFiles() { return layoutFiles; }
    public void setLayoutFiles(List<String> layoutFiles) { this.layoutFiles = layoutFiles; }
    public void addLayoutFile(String layoutFile) { this.layoutFiles.add(layoutFile); }

    public List<NavigationDestination> getNavigationDestinations() { return navigationDestinations; }
    public void setNavigationDestinations(List<NavigationDestination> navigationDestinations) { this.navigationDestinations = navigationDestinations; }
    public void addNavigationDestination(NavigationDestination destination) { this.navigationDestinations.add(destination); }
}