// BusinessProcessComponent.java
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class BusinessProcessComponent {
    private String processId;
    private String processName;
    private ProcessType processType;
    private CriticalityLevel criticalityLevel;
    private List<ProcessStep> steps;
    private List<ExternalIntegration> externalSystems;

    public enum ProcessType {
        AUTHENTICATION, USER_REGISTRATION, PAYMENT, SEARCH, DATA_SYNC, NOTIFICATION
    }

    public enum CriticalityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public BusinessProcessComponent() {
        this.steps = new ArrayList<>();
        this.externalSystems = new ArrayList<>();
    }

    // Getters and Setters
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public ProcessType getProcessType() { return processType; }
    public void setProcessType(ProcessType processType) { this.processType = processType; }

    public CriticalityLevel getCriticalityLevel() { return criticalityLevel; }
    public void setCriticalityLevel(CriticalityLevel criticalityLevel) { this.criticalityLevel = criticalityLevel; }

    public List<ProcessStep> getSteps() { return steps; }
    public void setSteps(List<ProcessStep> steps) { this.steps = steps; }

    public List<ExternalIntegration> getExternalSystems() { return externalSystems; }
    public void setExternalSystems(List<ExternalIntegration> externalSystems) { this.externalSystems = externalSystems; }

    // Helper methods
    public void addStep(ProcessStep step) {
        this.steps.add(step);
    }

    public void addExternalSystem(ExternalIntegration system) {
        this.externalSystems.add(system);
    }
}