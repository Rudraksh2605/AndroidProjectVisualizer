// ComponentRelationship.java
package com.projectvisualizer.model;

public class ComponentRelationship {
    private String sourceComponentId;
    private String targetComponentId;
    private RelationshipType relationshipType;
    private String description;

    public enum RelationshipType {
        EXTENDS, IMPLEMENTS, DEPENDS_ON, USES, COMPOSES, AGGREGATES
    }

    public ComponentRelationship() {}

    public ComponentRelationship(String sourceComponentId, String targetComponentId, RelationshipType relationshipType) {
        this.sourceComponentId = sourceComponentId;
        this.targetComponentId = targetComponentId;
        this.relationshipType = relationshipType;
    }

    // Getters and Setters
    public String getSourceComponentId() { return sourceComponentId; }
    public void setSourceComponentId(String sourceComponentId) { this.sourceComponentId = sourceComponentId; }

    public String getTargetComponentId() { return targetComponentId; }
    public void setTargetComponentId(String targetComponentId) { this.targetComponentId = targetComponentId; }

    public RelationshipType getRelationshipType() { return relationshipType; }
    public void setRelationshipType(RelationshipType relationshipType) { this.relationshipType = relationshipType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}