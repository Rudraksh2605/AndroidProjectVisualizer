// UserAction.java
package com.projectvisualizer.model;

public class UserAction {
    private String methodName;
    private String actionName;
    private ActionType actionType;

    public enum ActionType {
        TAP, SWIPE, LONG_PRESS, TYPE_TEXT, SCROLL, PINCH_ZOOM
    }

    public UserAction() {}

    public UserAction(String methodName, String actionName, ActionType actionType) {
        this.methodName = methodName;
        this.actionName = actionName;
        this.actionType = actionType;
    }

    // Getters and Setters
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }

    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
}