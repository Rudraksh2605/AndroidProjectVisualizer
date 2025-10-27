package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class NavigationFlow {
    private String flowId;
    private String sourceScreenId;
    private String targetScreenId;
    private NavigationType navigationType;
    private List<NavigationCondition> conditions;

    public enum NavigationType {
        FORWARD, BACK, UP, CUSTOM
    }

    public static class NavigationCondition {
        private String type;
        private String condition;
        private boolean isBlocking;

        public NavigationCondition() {}

        public NavigationCondition(String type, String condition, boolean isBlocking) {
            this.type = type;
            this.condition = condition;
            this.isBlocking = isBlocking;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public boolean isBlocking() { return isBlocking; }
        public void setBlocking(boolean blocking) { isBlocking = blocking; }
    }

    public NavigationFlow() {
        this.conditions = new ArrayList<>();
        this.navigationType = NavigationType.FORWARD;
    }

    // Getters and Setters
    public String getFlowId() { return flowId; }
    public void setFlowId(String flowId) { this.flowId = flowId; }

    public String getSourceScreenId() { return sourceScreenId; }
    public void setSourceScreenId(String sourceScreenId) { this.sourceScreenId = sourceScreenId; }

    public String getTargetScreenId() { return targetScreenId; }
    public void setTargetScreenId(String targetScreenId) { this.targetScreenId = targetScreenId; }

    public NavigationType getNavigationType() { return navigationType; }
    public void setNavigationType(NavigationType navigationType) { this.navigationType = navigationType; }

    public List<NavigationCondition> getConditions() { return conditions; }
    public void setConditions(List<NavigationCondition> conditions) { this.conditions = conditions; }

    // Helper methods
    public void addCondition(NavigationCondition condition) {
        this.conditions.add(condition);
    }
}