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
    private List<String> implementsList = new ArrayList<>();
    private List<CodeComponent> dependencies = new ArrayList<>();
    private List<CodeMethod> methods = new ArrayList<>();
    private List<CodeField> fields = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<String> injectedDependencies = new ArrayList<>();


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
}