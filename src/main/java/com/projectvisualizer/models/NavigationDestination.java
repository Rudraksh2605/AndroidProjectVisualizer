package com.projectvisualizer.models;

public class NavigationDestination {
    private String destinationId;
    private String destinationName;
    private String destinationType; // fragment, activity, etc.
    private String actionId;

    public NavigationDestination(String destinationId, String destinationName, String destinationType, String actionId) {
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.destinationType = destinationType;
        this.actionId = actionId;
    }

    // Getters and setters
    public String getDestinationId() { return destinationId; }
    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public String getDestinationType() { return destinationType; }
    public void setDestinationType(String destinationType) { this.destinationType = destinationType; }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
}