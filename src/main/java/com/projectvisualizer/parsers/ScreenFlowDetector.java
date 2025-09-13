package com.projectvisualizer.parsers;

import com.projectvisualizer.models.*;
import java.util.*;

public class ScreenFlowDetector {

    public List<UserFlowComponent> detectUserFlows(List<CodeComponent> components,
                                                   List<NavigationFlow> navigationFlows) {
        List<UserFlowComponent> userFlows = new ArrayList<>();

        // Convert UI components to user flow components
        for (CodeComponent component : components) {
            if (isUserInterfaceComponent(component)) {
                UserFlowComponent userFlow = convertToUserFlow(component, navigationFlows);
                userFlows.add(userFlow);
            }
        }

        // Analyze flow patterns and categorize
        categorizeFlowTypes(userFlows, navigationFlows);

        return userFlows;
    }

    private boolean isUserInterfaceComponent(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String type = component.getType() != null ? component.getType().toLowerCase() : "";
        String extendsClass = component.getExtendsClass();

        return name.contains("activity") ||
                name.contains("fragment") ||
                name.contains("dialog") ||
                type.contains("activity") ||
                type.contains("fragment") ||
                (extendsClass != null && (
                        extendsClass.contains("Activity") ||
                                extendsClass.contains("Fragment") ||
                                extendsClass.contains("Dialog")
                ));
    }

    private UserFlowComponent convertToUserFlow(CodeComponent component, List<NavigationFlow> flows) {
        UserFlowComponent userFlow = new UserFlowComponent();
        userFlow.setId(component.getId());
        userFlow.setScreenName(component.getName());
        userFlow.setActivityName(component.getName());

        // Find outgoing navigation flows
        for (NavigationFlow flow : flows) {
            if (flow.getSourceScreenId().equals(component.getId())) {
                userFlow.addOutgoingPath(new NavigationPath(flow));
            }
        }

        // Extract user actions from methods
        List<UserAction> actions = extractUserActionsFromMethods(component);
        userFlow.setUserActions(actions);

        // Determine business context
        BusinessContext context = determineBusinessContext(component);
        userFlow.setBusinessContext(context);

        // Populate simple performance metrics heuristically
        PerformanceMetrics metrics = new PerformanceMetrics();
        int methodCount = component.getMethods() != null ? component.getMethods().size() : 0;
        int actionCount = actions != null ? actions.size() : 0;
        long estimatedLoadTime = 100 + (long) (methodCount * 8L + actionCount * 12L);
        metrics.setLoadTimeMs(estimatedLoadTime);
        metrics.setErrorCount(0);
        metrics.setAverageResponseTime(200.0 + methodCount * 5.0);
        userFlow.setPerformanceMetrics(metrics);

        return userFlow;
    }

    private void categorizeFlowTypes(List<UserFlowComponent> userFlows, List<NavigationFlow> navigationFlows) {
        // Build flow graph
        Map<String, List<String>> incomingFlows = new HashMap<>();
        Map<String, List<String>> outgoingFlows = new HashMap<>();

        for (NavigationFlow flow : navigationFlows) {
            incomingFlows.computeIfAbsent(flow.getTargetScreenId(), k -> new ArrayList<>())
                    .add(flow.getSourceScreenId());
            outgoingFlows.computeIfAbsent(flow.getSourceScreenId(), k -> new ArrayList<>())
                    .add(flow.getTargetScreenId());
        }

        for (UserFlowComponent flow : userFlows) {
            String flowId = flow.getId();

            // Entry points - no incoming flows or launched from external
            if (!incomingFlows.containsKey(flowId) ||
                    isLauncherActivity(flow)) {
                flow.setFlowType(UserFlowComponent.FlowType.ENTRY_POINT);
            }

            // Exit points - no outgoing flows
            else if (!outgoingFlows.containsKey(flowId)) {
                flow.setFlowType(UserFlowComponent.FlowType.EXIT_POINT);
            }

            // Decision points - multiple outgoing flows with conditions
            else if (outgoingFlows.get(flowId).size() > 1) {
                flow.setFlowType(UserFlowComponent.FlowType.DECISION_POINT);
            }

            // Error handling - name contains error/exception patterns
            else if (isErrorHandlingScreen(flow)) {
                flow.setFlowType(UserFlowComponent.FlowType.ERROR_HANDLING);
            }

            // Main flow - regular flow screens
            else {
                flow.setFlowType(UserFlowComponent.FlowType.MAIN_FLOW);
            }
        }
    }

    private boolean isLauncherActivity(UserFlowComponent flow) {
        String name = flow.getScreenName().toLowerCase();
        return name.contains("main") ||
                name.contains("launch") ||
                name.contains("splash") ||
                name.contains("home");
    }

    private boolean isErrorHandlingScreen(UserFlowComponent flow) {
        String name = flow.getScreenName().toLowerCase();
        return name.contains("error") ||
                name.contains("exception") ||
                name.contains("crash") ||
                name.contains("failure");
    }

    private List<UserAction> extractUserActionsFromMethods(CodeComponent component) {
        List<UserAction> actions = new ArrayList<>();

        for (CodeMethod method : component.getMethods()) {
            String methodName = method.getName().toLowerCase();

            // Button click handlers
            if (methodName.contains("onclick") || methodName.contains("ontouch")) {
                UserAction action = new UserAction(
                        method.getName(),
                        "Tap " + extractButtonName(methodName),
                        UserAction.ActionType.TAP
                );
                actions.add(action);
            }

            // Text input handlers
            else if (methodName.contains("ontextchanged") || methodName.contains("aftertext")) {
                UserAction action = new UserAction(
                        method.getName(),
                        "Enter Text",
                        UserAction.ActionType.TYPE_TEXT
                );
                actions.add(action);
            }

            // Gesture handlers
            else if (methodName.contains("onswipe") || methodName.contains("onfling")) {
                UserAction action = new UserAction(
                        method.getName(),
                        "Swipe Gesture",
                        UserAction.ActionType.SWIPE
                );
                actions.add(action);
            }

            // Long press handlers
            else if (methodName.contains("onlongclick") || methodName.contains("onlongpress")) {
                UserAction action = new UserAction(
                        method.getName(),
                        "Long Press",
                        UserAction.ActionType.LONG_PRESS
                );
                actions.add(action);
            }
        }

        return actions;
    }

    private String extractButtonName(String methodName) {
        // Extract button name from onClick method names
        // e.g., "onLoginButtonClick" -> "Login Button"
        String name = methodName.replace("onclick", "").replace("ontouch", "");
        return name.replaceAll("([A-Z])", " $1").trim();
    }

    private BusinessContext determineBusinessContext(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String businessGoal;
        String userPersona;

        // Determine business context based on component name and type
        if (name.contains("login") || name.contains("signin") || name.contains("auth")) {
            businessGoal = "User Authentication";
            userPersona = "Unauthenticated User";
        } else if (name.contains("register") || name.contains("signup")) {
            businessGoal = "User Registration";
            userPersona = "New User";
        } else if (name.contains("main") || name.contains("home") || name.contains("dashboard")) {
            businessGoal = "Main Application Hub";
            userPersona = "Authenticated User";
        } else if (name.contains("profile") || name.contains("account") || name.contains("settings")) {
            businessGoal = "User Profile Management";
            userPersona = "Registered User";
        } else if (name.contains("payment") || name.contains("checkout") || name.contains("billing")) {
            businessGoal = "Payment Processing";
            userPersona = "Purchasing User";
        } else if (name.contains("search") || name.contains("browse")) {
            businessGoal = "Content Discovery";
            userPersona = "Content Consumer";
        } else if (name.contains("chat") || name.contains("message")) {
            businessGoal = "Communication";
            userPersona = "Active User";
        } else {
            businessGoal = "Application Feature";
            userPersona = "General User";
        }

        BusinessContext context = new BusinessContext(
                component.getId() + "_context",
                businessGoal,
                userPersona
        );

        // Add common business rules based on context
        addBusinessRules(context, name);

        return context;
    }

    private void addBusinessRules(BusinessContext context, String componentName) {
        String goal = context.getBusinessGoal();

        switch (goal) {
            case "User Authentication":
                context.addBusinessRule("User must provide valid credentials");
                context.addBusinessRule("Failed attempts should be limited");
                context.addBusinessRule("Secure password requirements");
                context.setSuccessMetric("Successful login rate");
                break;

            case "Payment Processing":
                context.addBusinessRule("Payment information must be validated");
                context.addBusinessRule("Secure payment processing required");
                context.addBusinessRule("Transaction confirmation needed");
                context.setSuccessMetric("Payment completion rate");
                break;

            case "User Registration":
                context.addBusinessRule("Email verification required");
                context.addBusinessRule("Unique username/email constraint");
                context.addBusinessRule("Terms and conditions acceptance");
                context.setSuccessMetric("Registration completion rate");
                break;

            default:
                context.addBusinessRule("User-friendly error handling");
                context.setSuccessMetric("User engagement time");
                break;
        }
    }
}
