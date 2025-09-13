package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class NavigationFlow extends ComponentRelationship {
    private String flowId;
    private String sourceScreenId;  // ADD THIS FIELD IF MISSING
    private String targetScreenId;  // ADD THIS FIELD IF MISSING
    private NavigationType navigationType;
    private List<NavigationCondition> conditions = new ArrayList<>();
    private TransitionAnimation animation;
    private boolean isBackStackPreserved;

    public enum NavigationType {
        FORWARD, BACKWARD, REPLACE, POPUP, DEEP_LINK, EXTERNAL
    }

    public static class NavigationCondition {
        private String conditionType;
        private String conditionValue;
        private boolean isRequired;

        public NavigationCondition(String type, String value, boolean required) {
            this.conditionType = type;
            this.conditionValue = value;
            this.isRequired = required;
        }

        public String getConditionType() { return conditionType; }
        public String getConditionValue() { return conditionValue; }
        public boolean isRequired() { return isRequired; }
    }

    public static class TransitionAnimation {
        private String animationType;
        private int duration;
        private String interpolator;

        public TransitionAnimation(String type, int duration, String interpolator) {
            this.animationType = type;
            this.duration = duration;
            this.interpolator = interpolator;
        }

        public String getAnimationType() { return animationType; }
        public int getDuration() { return duration; }
        public String getInterpolator() { return interpolator; }
    }

    // FIXED: Add all missing getters and setters
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
    public void addCondition(NavigationCondition condition) { this.conditions.add(condition); }

    public TransitionAnimation getAnimation() { return animation; }
    public void setAnimation(TransitionAnimation animation) { this.animation = animation; }

    public boolean isBackStackPreserved() { return isBackStackPreserved; }
    public void setBackStackPreserved(boolean backStackPreserved) { this.isBackStackPreserved = backStackPreserved; }
}
