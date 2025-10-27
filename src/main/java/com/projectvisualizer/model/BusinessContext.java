
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class BusinessContext {
    private String id;
    private String businessGoal;
    private String userPersona;
    private List<String> businessRules;
    private String successMetric;

    public BusinessContext() {
        this.businessRules = new ArrayList<>();
    }

    public BusinessContext(String id, String businessGoal, String userPersona) {
        this();
        this.id = id;
        this.businessGoal = businessGoal;
        this.userPersona = userPersona;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusinessGoal() { return businessGoal; }
    public void setBusinessGoal(String businessGoal) { this.businessGoal = businessGoal; }

    public String getUserPersona() { return userPersona; }
    public void setUserPersona(String userPersona) { this.userPersona = userPersona; }

    public List<String> getBusinessRules() { return businessRules; }
    public void setBusinessRules(List<String> businessRules) { this.businessRules = businessRules; }

    public String getSuccessMetric() { return successMetric; }
    public void setSuccessMetric(String successMetric) { this.successMetric = successMetric; }

    // Helper methods
    public void addBusinessRule(String rule) {
        this.businessRules.add(rule);
    }
}