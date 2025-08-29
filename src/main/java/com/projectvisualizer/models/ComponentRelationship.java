package com.projectvisualizer.models;

public class ComponentRelationship {
    private String sourceId;
    private String targetId;
    private String type; // EXTENDS, IMPLEMENTS, DEPENDS_ON, etc.

    // Getters and setters
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}