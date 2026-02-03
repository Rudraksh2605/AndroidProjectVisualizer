package com.projectvisualizer.visualization;

import com.projectvisualizer.model.*;
import com.projectvisualizer.parsers.BusinessProcessExtractor;
import com.projectvisualizer.parsers.ScreenFlowDetector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates PlantUML Activity Diagrams from navigation flow data.
 * Visualizes screen-to-screen navigation with swimlanes for business processes.
 * Uses pattern-based analysis without AI.
 */
public class ActivityDiagramGenerator {

    private final ScreenFlowDetector screenFlowDetector;
    private final BusinessProcessExtractor businessProcessExtractor;

    public ActivityDiagramGenerator() {
        this.screenFlowDetector = new ScreenFlowDetector();
        this.businessProcessExtractor = new BusinessProcessExtractor();
    }

    /**
     * Generates a PlantUML Activity Diagram from code components and navigation flows.
     *
     * @param components      List of code components (Activities, Fragments)
     * @param navigationFlows List of navigation flows between screens
     * @return PlantUML string for the activity diagram
     */
    public String generatePlantUML(List<CodeComponent> components, List<NavigationFlow> navigationFlows) {
        if (components == null || components.isEmpty()) {
            return generateEmptyDiagram();
        }

        // Step 1: Detect user flows from components
        List<UserFlowComponent> userFlows = screenFlowDetector.detectUserFlows(components, navigationFlows);

        if (userFlows.isEmpty()) {
            return generateEmptyDiagram();
        }

        // Step 2: Extract business processes
        List<BusinessProcessComponent> processes = businessProcessExtractor.extractBusinessProcesses(userFlows);

        // Step 3: Generate PlantUML
        return buildPlantUML(userFlows, navigationFlows, processes);
    }

    /**
     * Builds the PlantUML activity diagram string.
     */
    private String buildPlantUML(List<UserFlowComponent> userFlows,
                                  List<NavigationFlow> navigationFlows,
                                  List<BusinessProcessComponent> processes) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("@startuml\n");
        sb.append("!theme cerulean\n");
        sb.append("skinparam ActivityBackgroundColor #e8eef7\n");
        sb.append("skinparam ActivityBorderColor #3b82f6\n");
        sb.append("skinparam ActivityFontColor #1e3a5f\n");
        sb.append("skinparam ArrowColor #64748b\n");
        sb.append("skinparam PartitionBackgroundColor #f8fafc\n");
        sb.append("skinparam PartitionBorderColor #cbd5e1\n");
        sb.append("skinparam shadowing false\n\n");

        // Title
        sb.append("title <b>Application Navigation Flow</b>\n\n");

        // Group flows by business process
        Map<String, List<UserFlowComponent>> flowsByProcess = groupFlowsByProcess(userFlows, processes);

        // Build navigation map for transitions
        Map<String, List<NavigationFlow>> outgoingFlows = buildNavigationMap(navigationFlows);

        // Find entry point
        UserFlowComponent entryPoint = findEntryPoint(userFlows);

        if (flowsByProcess.isEmpty() || flowsByProcess.size() == 1) {
            // No business processes detected - render as simple flow
            sb.append("start\n\n");
            renderSimpleFlow(sb, userFlows, outgoingFlows, entryPoint);
            sb.append("\nstop\n");
        } else {
            // Define swimlanes FIRST (before start)
            String[] colors = {"#AntiqueWhite", "#LightBlue", "#LightGreen", "#LightPink", "#LightYellow"};
            int colorIndex = 0;
            for (String processName : flowsByProcess.keySet()) {
                sb.append("|").append(colors[colorIndex % colors.length])
                  .append("|").append(sanitizeLabel(processName)).append("|\n");
                colorIndex++;
            }
            sb.append("\n");
            
            // Now add start
            sb.append("start\n\n");
            
            // Render with swimlane partitions
            renderWithSwimlanes(sb, flowsByProcess, outgoingFlows, entryPoint);
            sb.append("\nstop\n");
        }

        sb.append("\n@enduml");
        return sb.toString();
    }

    /**
     * Groups user flows by their business process.
     */
    private Map<String, List<UserFlowComponent>> groupFlowsByProcess(
            List<UserFlowComponent> userFlows,
            List<BusinessProcessComponent> processes) {

        Map<String, List<UserFlowComponent>> grouped = new LinkedHashMap<>();

        // Create a map from flow ID to process name
        Map<String, String> flowToProcess = new HashMap<>();
        for (BusinessProcessComponent process : processes) {
            for (ProcessStep step : process.getSteps()) {
                flowToProcess.put(step.getStepId(), process.getProcessName());
            }
        }

        // Group flows
        for (UserFlowComponent flow : userFlows) {
            String processName = flowToProcess.getOrDefault(flow.getId(), "Main Flow");
            grouped.computeIfAbsent(processName, k -> new ArrayList<>()).add(flow);
        }

        return grouped;
    }

    /**
     * Builds a map of source screen ID to outgoing navigation flows.
     */
    private Map<String, List<NavigationFlow>> buildNavigationMap(List<NavigationFlow> navigationFlows) {
        Map<String, List<NavigationFlow>> map = new HashMap<>();
        if (navigationFlows != null) {
            for (NavigationFlow flow : navigationFlows) {
                map.computeIfAbsent(flow.getSourceScreenId(), k -> new ArrayList<>()).add(flow);
            }
        }
        return map;
    }

    /**
     * Finds the entry point (launcher activity/splash screen).
     */
    private UserFlowComponent findEntryPoint(List<UserFlowComponent> userFlows) {
        // First, look for explicit entry points
        for (UserFlowComponent flow : userFlows) {
            if (flow.getFlowType() == UserFlowComponent.FlowType.ENTRY_POINT) {
                return flow;
            }
        }

        // Look for common entry point names
        for (UserFlowComponent flow : userFlows) {
            String name = flow.getScreenName() != null ? flow.getScreenName().toLowerCase() : "";
            if (name.contains("splash") || name.contains("launcher") || name.contains("main")) {
                return flow;
            }
        }

        // Return first flow as fallback
        return userFlows.isEmpty() ? null : userFlows.get(0);
    }

    /**
     * Renders a simple flow without swimlanes.
     */
    private void renderSimpleFlow(StringBuilder sb,
                                   List<UserFlowComponent> userFlows,
                                   Map<String, List<NavigationFlow>> outgoingFlows,
                                   UserFlowComponent entryPoint) {
        Set<String> rendered = new HashSet<>();

        if (entryPoint != null) {
            renderFlowRecursive(sb, entryPoint, userFlows, outgoingFlows, rendered, 0);
        } else {
            // Render all flows linearly
            for (UserFlowComponent flow : userFlows) {
                if (!rendered.contains(flow.getId())) {
                    renderActivityState(sb, flow);
                    rendered.add(flow.getId());
                }
            }
        }
    }

    /**
     * Recursively renders flows following navigation paths.
     */
    private void renderFlowRecursive(StringBuilder sb,
                                      UserFlowComponent current,
                                      List<UserFlowComponent> allFlows,
                                      Map<String, List<NavigationFlow>> outgoingFlows,
                                      Set<String> rendered,
                                      int depth) {
        if (current == null || rendered.contains(current.getId()) || depth > 20) {
            return;
        }

        rendered.add(current.getId());
        renderActivityState(sb, current);

        // Get outgoing navigation
        List<NavigationFlow> outgoing = outgoingFlows.get(current.getId());
        if (outgoing == null || outgoing.isEmpty()) {
            // Check for outgoing paths in the component
            if (current.getOutgoingPaths() != null && !current.getOutgoingPaths().isEmpty()) {
                for (NavigationPath path : current.getOutgoingPaths()) {
                    NavigationFlow navFlow = path.getNavigationFlow();
                    if (navFlow != null) {
                        UserFlowComponent target = findFlowById(allFlows, navFlow.getTargetScreenId());
                        if (target != null && !rendered.contains(target.getId())) {
                            sb.append("--> ");
                            renderFlowRecursive(sb, target, allFlows, outgoingFlows, rendered, depth + 1);
                        }
                    }
                }
            }
            return;
        }

        // Handle branching (decision points)
        if (outgoing.size() > 1 && current.getFlowType() == UserFlowComponent.FlowType.DECISION_POINT) {
            renderDecisionPoint(sb, current, outgoing, allFlows, outgoingFlows, rendered, depth);
        } else {
            // Single path
            for (NavigationFlow nav : outgoing) {
                UserFlowComponent target = findFlowById(allFlows, nav.getTargetScreenId());
                if (target != null && !rendered.contains(target.getId())) {
                    String label = getTransitionLabel(nav);
                    if (label != null) {
                        sb.append("--> ").append(label).append("\n");
                    } else {
                        sb.append("--> ");
                    }
                    renderFlowRecursive(sb, target, allFlows, outgoingFlows, rendered, depth + 1);
                }
            }
        }
    }

    /**
     * Renders a decision point with branches.
     */
    private void renderDecisionPoint(StringBuilder sb,
                                      UserFlowComponent current,
                                      List<NavigationFlow> outgoing,
                                      List<UserFlowComponent> allFlows,
                                      Map<String, List<NavigationFlow>> outgoingFlows,
                                      Set<String> rendered,
                                      int depth) {
        sb.append("if (").append(sanitizeLabel(current.getScreenName())).append("?) then\n");

        boolean first = true;
        for (NavigationFlow nav : outgoing) {
            UserFlowComponent target = findFlowById(allFlows, nav.getTargetScreenId());
            if (target != null) {
                if (!first) {
                    sb.append("else\n");
                }
                String condition = getConditionLabel(nav);
                if (condition != null && first) {
                    sb.append("  (").append(condition).append(")\n");
                }
                renderFlowRecursive(sb, target, allFlows, outgoingFlows, rendered, depth + 1);
                first = false;
            }
        }
        sb.append("endif\n");
    }

    /**
     * Renders flows with swimlane partitions for each business process.
     * Swimlanes must already be declared before 'start'.
     */
    private void renderWithSwimlanes(StringBuilder sb,
                                      Map<String, List<UserFlowComponent>> flowsByProcess,
                                      Map<String, List<NavigationFlow>> outgoingFlows,
                                      UserFlowComponent entryPoint) {
        Set<String> rendered = new HashSet<>();

        for (Map.Entry<String, List<UserFlowComponent>> entry : flowsByProcess.entrySet()) {
            String processName = entry.getKey();
            List<UserFlowComponent> flows = entry.getValue();

            // Switch to the swimlane (already declared before start)
            sb.append("|").append(sanitizeLabel(processName)).append("|\n");

            for (UserFlowComponent flow : flows) {
                if (!rendered.contains(flow.getId())) {
                    renderActivityState(sb, flow);
                    rendered.add(flow.getId());

                    // Add transitions
                    List<NavigationFlow> outgoing = outgoingFlows.get(flow.getId());
                    if (outgoing != null) {
                        for (NavigationFlow nav : outgoing) {
                            String label = getTransitionLabel(nav);
                            if (label != null) {
                                sb.append("--> ").append(label).append("\n");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a single activity state.
     */
    private void renderActivityState(StringBuilder sb, UserFlowComponent flow) {
        String screenName = flow.getScreenName() != null ? flow.getScreenName() : flow.getActivityName();
        if (screenName == null) {
            screenName = "Unknown Screen";
        }

        String sanitized = sanitizeLabel(screenName);

        // Add icon based on flow type
        String icon = getFlowTypeIcon(flow.getFlowType());

        sb.append(":").append(icon).append(sanitized).append(";\n");

        // Add user actions as notes if present
        if (flow.getUserActions() != null && !flow.getUserActions().isEmpty()) {
            sb.append("note right\n");
            int actionCount = 0;
            for (UserAction action : flow.getUserActions()) {
                if (actionCount >= 3) {
                    sb.append("  ...\n");
                    break;
                }
                sb.append("  • ").append(sanitizeLabel(action.getActionName())).append("\n");
                actionCount++;
            }
            sb.append("end note\n");
        }
    }

    /**
     * Gets an icon for the flow type.
     */
    private String getFlowTypeIcon(UserFlowComponent.FlowType flowType) {
        if (flowType == null) return "";
        switch (flowType) {
            case ENTRY_POINT: return "🚀 ";
            case EXIT_POINT: return "🚪 ";
            case DECISION_POINT: return "🔀 ";
            case ERROR_HANDLING: return "⚠️ ";
            default: return "";
        }
    }

    /**
     * Gets a label for a transition.
     */
    private String getTransitionLabel(NavigationFlow nav) {
        if (nav.getConditions() != null && !nav.getConditions().isEmpty()) {
            NavigationFlow.NavigationCondition cond = nav.getConditions().get(0);
            return "[" + sanitizeLabel(cond.getCondition()) + "]";
        }
        return null;
    }

    /**
     * Gets a condition label from navigation.
     */
    private String getConditionLabel(NavigationFlow nav) {
        if (nav.getConditions() != null && !nav.getConditions().isEmpty()) {
            return sanitizeLabel(nav.getConditions().get(0).getCondition());
        }
        return null;
    }

    /**
     * Finds a UserFlowComponent by ID.
     */
    private UserFlowComponent findFlowById(List<UserFlowComponent> flows, String id) {
        if (id == null) return null;
        for (UserFlowComponent flow : flows) {
            if (id.equals(flow.getId())) {
                return flow;
            }
        }
        return null;
    }

    /**
     * Sanitizes a label for PlantUML.
     */
    private String sanitizeLabel(String label) {
        if (label == null) return "Unknown";
        return label
                .replace("Activity", "")
                .replace("Fragment", "")
                .replace("Screen", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .trim();
    }

    /**
     * Generates an empty diagram when no data is available.
     */
    private String generateEmptyDiagram() {
        return "@startuml\n" +
               "!theme cerulean\n" +
               "title No Navigation Data Available\n\n" +
               "start\n" +
               ":No Activities or Fragments detected;\n" +
               "note right\n" +
               "  Load an Android project to visualize\n" +
               "  the navigation flow between screens.\n" +
               "end note\n" +
               "stop\n" +
               "@enduml";
    }
}
