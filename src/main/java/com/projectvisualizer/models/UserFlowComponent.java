package com.projectvisualizer.models;

import java.util.ArrayList;
import java.util.List;

public class UserFlowComponent {
    private String id;
    private String screenName;
    private String activityName;
    private FlowType flowType;
    private List<UserAction> userActions = new ArrayList<>();
    private List<NavigationPath> outgoingPaths = new ArrayList<>();
    private List<NavigationPath> incomingPaths = new ArrayList<>();
    private BusinessContext businessContext;
    private PerformanceMetrics performanceMetrics;

    public enum FlowType {
        ENTRY_POINT, MAIN_FLOW, DECISION_POINT, EXIT_POINT, ERROR_HANDLING
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public FlowType getFlowType() { return flowType; }
    public void setFlowType(FlowType flowType) { this.flowType = flowType; }

    public List<UserAction> getUserActions() { return userActions; }
    public void setUserActions(List<UserAction> userActions) { this.userActions = userActions; }


    public BusinessContext getBusinessContext() { return businessContext; }
    public void setBusinessContext(BusinessContext businessContext) { this.businessContext = businessContext; }
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public List<NavigationPath> getIncomingPaths() {
        return incomingPaths;
    }


    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    // Add these getters/setters if missing
    public List<NavigationPath> getOutgoingPaths() {
        return outgoingPaths;
    }

    public void setOutgoingPaths(List<NavigationPath> outgoingPaths) {
        this.outgoingPaths = outgoingPaths;
    }

    public void addOutgoingPath(NavigationPath path) {
        if (this.outgoingPaths == null) {
            this.outgoingPaths = new ArrayList<>();
        }
        this.outgoingPaths.add(path);
    }


}
