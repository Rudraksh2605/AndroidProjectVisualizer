package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

/**
 * DecisionPoint class represents decision nodes in business processes
 */
public class DecisionPoint {
    private String decisionId;
    private String decisionName;
    private String description;
    private List<String> conditions;
    private List<String> outcomes;
    private String decisionType;
    private boolean isRequired;
    private String defaultOutcome;
    private int priority;
    private long createdTimestamp;

    // Decision type constants
    public static final String BINARY = "BINARY";
    public static final String MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String RULE_BASED = "RULE_BASED";

    // Default constructor
    public DecisionPoint() {
        this.conditions = new ArrayList<>();
        this.outcomes = new ArrayList<>();
        this.isRequired = true;
        this.decisionType = BINARY;
        this.priority = 1;
        this.createdTimestamp = System.currentTimeMillis();
    }

    // Constructor with basic fields
    public DecisionPoint(String decisionId, String decisionName, String description) {
        this();
        this.decisionId = decisionId;
        this.decisionName = decisionName;
        this.description = description;
    }

    // Constructor with type and requirement
    public DecisionPoint(String decisionId, String decisionName, String description,
                         String decisionType, boolean isRequired) {
        this(decisionId, decisionName, description);
        this.decisionType = decisionType;
        this.isRequired = isRequired;
    }

    // Getters and Setters
    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getDecisionName() {
        return decisionName;
    }

    public void setDecisionName(String decisionName) {
        this.decisionName = decisionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public List<String> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<String> outcomes) {
        this.outcomes = outcomes != null ? outcomes : new ArrayList<>();
    }

    public String getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        this.isRequired = required;
    }

    public String getDefaultOutcome() {
        return defaultOutcome;
    }

    public void setDefaultOutcome(String defaultOutcome) {
        this.defaultOutcome = defaultOutcome;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    // Utility methods for managing conditions
    public void addCondition(String condition) {
        if (condition != null && !condition.trim().isEmpty()) {
            if (!this.conditions.contains(condition)) {
                this.conditions.add(condition);
            }
        }
    }

    public void removeCondition(String condition) {
        this.conditions.remove(condition);
    }

    public boolean hasCondition(String condition) {
        return this.conditions.contains(condition);
    }

    public void clearConditions() {
        this.conditions.clear();
    }

    // Utility methods for managing outcomes
    public void addOutcome(String outcome) {
        if (outcome != null && !outcome.trim().isEmpty()) {
            if (!this.outcomes.contains(outcome)) {
                this.outcomes.add(outcome);
            }
        }
    }

    public void removeOutcome(String outcome) {
        this.outcomes.remove(outcome);
        // If removing the default outcome, clear it
        if (outcome.equals(defaultOutcome)) {
            this.defaultOutcome = null;
        }
    }

    public boolean hasOutcome(String outcome) {
        return this.outcomes.contains(outcome);
    }

    public void clearOutcomes() {
        this.outcomes.clear();
        this.defaultOutcome = null;
    }

    // Business logic methods
    public boolean isBinaryDecision() {
        return BINARY.equals(decisionType);
    }

    public boolean isMultipleChoice() {
        return MULTIPLE_CHOICE.equals(decisionType);
    }

    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }

    public boolean hasOutcomes() {
        return outcomes != null && !outcomes.isEmpty();
    }

    public boolean isValid() {
        return decisionId != null && !decisionId.trim().isEmpty() &&
                decisionName != null && !decisionName.trim().isEmpty() &&
                hasOutcomes();
    }

    public int getConditionCount() {
        return conditions != null ? conditions.size() : 0;
    }

    public int getOutcomeCount() {
        return outcomes != null ? outcomes.size() : 0;
    }

    @Override
    public String toString() {
        return "DecisionPoint{" +
                "decisionId='" + decisionId + '\'' +
                ", decisionName='" + decisionName + '\'' +
                ", decisionType='" + decisionType + '\'' +
                ", isRequired=" + isRequired +
                ", conditionsCount=" + getConditionCount() +
                ", outcomesCount=" + getOutcomeCount() +
                ", priority=" + priority +
                '}';
    }
}
