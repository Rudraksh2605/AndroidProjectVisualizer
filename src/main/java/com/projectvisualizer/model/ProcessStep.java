// ProcessStep.java
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class ProcessStep {
    private String stepId;
    private String stepName;
    private String description;
    private List<String> actionDescriptions;

    public ProcessStep() {
        this.actionDescriptions = new ArrayList<>();
    }

    // Getters and Setters
    public String getStepId() { return stepId; }
    public void setStepId(String stepId) { this.stepId = stepId; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getActionDescriptions() { return actionDescriptions; }
    public void setActionDescriptions(List<String> actionDescriptions) { this.actionDescriptions = actionDescriptions; }

    // Helper methods
    public void addActionDescription(String action) {
        this.actionDescriptions.add(action);
    }
}