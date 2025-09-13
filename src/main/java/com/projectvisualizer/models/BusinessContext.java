package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class BusinessContext {
    private String contextId;
    private String businessGoal;
    private String userPersona;
    private List<String> businessRules = new ArrayList<>();
    private List<String> constraints = new ArrayList<>();
    private String successMetric;
    private String failureScenario;

    // Constructor
    public BusinessContext(String contextId, String businessGoal, String userPersona) {
        this.contextId = contextId;
        this.businessGoal = businessGoal;
        this.userPersona = userPersona;
    }

    // Getters and setters
    public String getContextId() { return contextId; }
    public String getBusinessGoal() { return businessGoal; }
    public String getUserPersona() { return userPersona; }
    public List<String> getBusinessRules() { return businessRules; }
    public void addBusinessRule(String rule) { this.businessRules.add(rule); }
    public String getSuccessMetric() { return successMetric; }
    public void setSuccessMetric(String successMetric) { this.successMetric = successMetric; }
}
