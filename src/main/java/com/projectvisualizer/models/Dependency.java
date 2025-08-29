package com.projectvisualizer.models;

public class Dependency {
    private String name;
    private String version;
    private String type; // GRADLE, FLUTTER, JS, etc.

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}