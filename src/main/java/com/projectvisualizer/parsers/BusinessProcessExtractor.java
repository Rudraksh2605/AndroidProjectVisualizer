package com.projectvisualizer.parsers;

import com.projectvisualizer.models.*;
import java.util.*;

public class BusinessProcessExtractor {

    public List<BusinessProcessComponent> extractBusinessProcesses(List<UserFlowComponent> userFlows) {
        List<BusinessProcessComponent> processes = new ArrayList<>();

        // Group flows by business context
        Map<String, List<UserFlowComponent>> processGroups = groupFlowsByBusinessGoal(userFlows);

        for (Map.Entry<String, List<UserFlowComponent>> entry : processGroups.entrySet()) {
            BusinessProcessComponent process = createBusinessProcess(entry.getKey(), entry.getValue());
            processes.add(process);
        }

        return processes;
    }

    private Map<String, List<UserFlowComponent>> groupFlowsByBusinessGoal(List<UserFlowComponent> userFlows) {
        Map<String, List<UserFlowComponent>> groups = new HashMap<>();

        for (UserFlowComponent flow : userFlows) {
            if (flow.getBusinessContext() != null) {
                String goal = flow.getBusinessContext().getBusinessGoal();
                groups.computeIfAbsent(goal, k -> new ArrayList<>()).add(flow);
            }
        }

        return groups;
    }

    private BusinessProcessComponent createBusinessProcess(String businessGoal, List<UserFlowComponent> flows) {
        BusinessProcessComponent process = new BusinessProcessComponent();
        process.setProcessId(UUID.randomUUID().toString());
        process.setProcessName(businessGoal);
        process.setProcessType(determineProcessType(businessGoal));
        process.setCriticalityLevel(determineCriticality(businessGoal));

        // Create process steps from user flows
        for (UserFlowComponent flow : flows) {
            ProcessStep step = createProcessStep(flow);
            process.addStep(step);
        }

        // Heuristically detect external integrations based on process type
        addExternalIntegrations(process);

        return process;
    }

    private BusinessProcessComponent.ProcessType determineProcessType(String businessGoal) {
        String goal = businessGoal.toLowerCase();

        if (goal.contains("authentication") || goal.contains("login")) {
            return BusinessProcessComponent.ProcessType.AUTHENTICATION;
        } else if (goal.contains("registration")) {
            return BusinessProcessComponent.ProcessType.USER_REGISTRATION;
        } else if (goal.contains("payment") || goal.contains("checkout")) {
            return BusinessProcessComponent.ProcessType.PAYMENT;
        } else if (goal.contains("search") || goal.contains("discovery")) {
            return BusinessProcessComponent.ProcessType.SEARCH;
        } else if (goal.contains("sync") || goal.contains("data")) {
            return BusinessProcessComponent.ProcessType.DATA_SYNC;
        } else if (goal.contains("notification")) {
            return BusinessProcessComponent.ProcessType.NOTIFICATION;
        } else {
            return BusinessProcessComponent.ProcessType.USER_REGISTRATION; // Default
        }
    }

    private BusinessProcessComponent.CriticalityLevel determineCriticality(String businessGoal) {
        String goal = businessGoal.toLowerCase();

        if (goal.contains("payment") || goal.contains("authentication") || goal.contains("security")) {
            return BusinessProcessComponent.CriticalityLevel.CRITICAL;
        } else if (goal.contains("registration") || goal.contains("sync") || goal.contains("data")) {
            return BusinessProcessComponent.CriticalityLevel.HIGH;
        } else if (goal.contains("search") || goal.contains("profile")) {
            return BusinessProcessComponent.CriticalityLevel.MEDIUM;
        } else {
            return BusinessProcessComponent.CriticalityLevel.LOW;
        }
    }

    private void addExternalIntegrations(BusinessProcessComponent process) {
        // Minimal heuristic-based population of external integrations to activate ExternalIntegration model usage
        BusinessProcessComponent.ProcessType type = process.getProcessType();
        List<ExternalIntegration> integrations = new ArrayList<>();
        if (type == BusinessProcessComponent.ProcessType.PAYMENT) {
            ExternalIntegration pg = new ExternalIntegration(UUID.randomUUID().toString(), "PaymentGateway", "https://api.payment.example.com");
            pg.setIntegrationType("REST_API");
            pg.setAuthType("OAUTH2");
            pg.addRequiredPermission("INTERNET");
            integrations.add(pg);
        } else if (type == BusinessProcessComponent.ProcessType.AUTHENTICATION) {
            ExternalIntegration auth = new ExternalIntegration(UUID.randomUUID().toString(), "AuthService", "https://auth.example.com");
            auth.setIntegrationType("REST_API");
            auth.setAuthType("TOKEN");
            integrations.add(auth);
        } else if (type == BusinessProcessComponent.ProcessType.DATA_SYNC) {
            ExternalIntegration sync = new ExternalIntegration(UUID.randomUUID().toString(), "SyncService", "wss://sync.example.com");
            sync.setIntegrationType("WEBSOCKET");
            sync.setAuthType("API_KEY");
            integrations.add(sync);
        }
        if (!integrations.isEmpty()) {
            for (ExternalIntegration ei : integrations) {
                process.addExternalSystem(ei);
            }
        }
    }

    private ProcessStep createProcessStep(UserFlowComponent flow) {
        ProcessStep step = new ProcessStep();
        step.setStepId(flow.getId());
        step.setStepName(flow.getScreenName());
        step.setDescription(flow.getBusinessContext() != null ?
                flow.getBusinessContext().getBusinessGoal() : "Screen interaction");

        // Add user actions as step details
        for (UserAction action : flow.getUserActions()) {
            step.addActionDescription(action.getActionName());
        }

        return step;
    }
}
