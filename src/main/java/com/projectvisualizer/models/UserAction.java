package com.projectvisualizer.models;

public class UserAction {
    private String actionId;
    private String actionName;
    private ActionType actionType;
    private String targetElement;
    private String expectedOutcome;
    private boolean isOptional;

    public enum ActionType {
        TAP, SWIPE, LONG_PRESS, DOUBLE_TAP, PINCH, TYPE_TEXT,
        VOICE_INPUT, GESTURE, SCROLL, DRAG_DROP
    }

    public UserAction(String actionId, String actionName, ActionType actionType) {
        this.actionId = actionId;
        this.actionName = actionName;
        this.actionType = actionType;
    }

    // Getters and setters
    public String getActionId() { return actionId; }
    public String getActionName() { return actionName; }
    public ActionType getActionType() { return actionType; }
    public String getTargetElement() { return targetElement; }
    public void setTargetElement(String targetElement) { this.targetElement = targetElement; }
}