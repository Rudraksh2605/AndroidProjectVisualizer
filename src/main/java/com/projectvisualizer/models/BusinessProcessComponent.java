package com.projectvisualizer.models;


import java.util.ArrayList;
import java.util.List;

public class BusinessProcessComponent {
    private String processId;
    private String processName;
    private ProcessType processType;
    private List<ProcessStep> steps = new ArrayList<>();
    private List<DecisionPoint> decisionPoints = new ArrayList<>();
    private List<ExternalIntegration> externalSystems = new ArrayList<>();
    private CriticalityLevel criticalityLevel;

    public enum ProcessType {
        USER_REGISTRATION, AUTHENTICATION, PAYMENT, DATA_SYNC,
        CONTENT_CREATION, SEARCH, NOTIFICATION, BACKUP
    }

    public enum CriticalityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Getters and setters
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public ProcessType getProcessType() { return processType; }
    public void setProcessType(ProcessType processType) { this.processType = processType; }

    public List<ProcessStep> getSteps() { return steps; }
    public void addStep(ProcessStep step) { this.steps.add(step); }

    public List<DecisionPoint> getDecisionPoints() { return decisionPoints; }
    public void setDecisionPoints(List<DecisionPoint> decisionPoints) { this.decisionPoints = decisionPoints; }
    public void addDecisionPoint(DecisionPoint decisionPoint) { this.decisionPoints.add(decisionPoint); }

    public List<ExternalIntegration> getExternalSystems() { return externalSystems; }
    public void setExternalSystems(List<ExternalIntegration> externalSystems) { this.externalSystems = externalSystems; }
    public void addExternalSystem(ExternalIntegration integration) { this.externalSystems.add(integration); }

    public CriticalityLevel getCriticalityLevel() { return criticalityLevel; }
    public void setCriticalityLevel(CriticalityLevel criticalityLevel) { this.criticalityLevel = criticalityLevel; }
}
