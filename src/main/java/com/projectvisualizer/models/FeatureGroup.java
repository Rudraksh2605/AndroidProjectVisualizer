package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class FeatureGroup {
    private String groupId;
    private String featureName;
    private String description;
    private List<CodeComponent> components = new ArrayList<>();
    private List<UserFlowComponent> userFlows = new ArrayList<>();
    private List<BusinessProcessComponent> businessProcesses = new ArrayList<>();
    private FeatureComplexity complexity;
    private UserImpact userImpact;

    public enum FeatureComplexity {
        SIMPLE, MODERATE, COMPLEX, VERY_COMPLEX
    }

    public enum UserImpact {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Getters and setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public List<CodeComponent> getComponents() { return components; }
    public void addComponent(CodeComponent component) { this.components.add(component); }

    public List<UserFlowComponent> getUserFlows() { return userFlows; }
    public void addUserFlow(UserFlowComponent flow) { this.userFlows.add(flow); }

    public FeatureComplexity getComplexity() { return complexity; }
    public void setComplexity(FeatureComplexity complexity) { this.complexity = complexity; }
}
