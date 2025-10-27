
package com.projectvisualizer.model;

import java.util.ArrayList;
import java.util.List;

public class UserFlowComponent {
    private String id;
    private String screenName;
    private String activityName;
    private FlowType flowType;
    private List<UserAction> userActions;
    private List<NavigationPath> outgoingPaths;
    private BusinessContext businessContext;
    private PerformanceMetrics performanceMetrics;

    public enum FlowType {
        ENTRY_POINT, EXIT_POINT, DECISION_POINT, ERROR_HANDLING, MAIN_FLOW
    }

    public UserFlowComponent() {
        this.userActions = new ArrayList<>();
        this.outgoingPaths = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public FlowType getFlowType() { return flowType; }
    public void setFlowType(FlowType flowType) { this.flowType = flowType; }

    public List<UserAction> getUserActions() { return userActions; }
    public void setUserActions(List<UserAction> userActions) { this.userActions = userActions; }

    public List<NavigationPath> getOutgoingPaths() { return outgoingPaths; }
    public void setOutgoingPaths(List<NavigationPath> outgoingPaths) { this.outgoingPaths = outgoingPaths; }

    public BusinessContext getBusinessContext() { return businessContext; }
    public void setBusinessContext(BusinessContext businessContext) { this.businessContext = businessContext; }

    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }

    // Helper methods
    public void addUserAction(UserAction action) {
        this.userActions.add(action);
    }

    public void addOutgoingPath(NavigationPath path) {
        this.outgoingPaths.add(path);
    }
}