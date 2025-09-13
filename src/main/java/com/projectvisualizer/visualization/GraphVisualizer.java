package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import javafx.animation.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import com.projectvisualizer.visualization.render.EdgeRenderer;
import com.projectvisualizer.visualization.ui.LegendFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.projectvisualizer.models.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

public class GraphVisualizer {
    // Constants for node sizing and spacing
    private static final double NODE_WIDTH = 200;
    private static final double NODE_HEIGHT = 100;
    private static final double SPECIAL_NODE_SIZE = 70;
    private static final double MIN_NODE_DISTANCE = 250; // Minimum distance between nodes (approx 5cm at 96 DPI)
    private static final double LAYER_SPACING = 350;
    private static final double ARROW_SIZE = 14;
    private static final double CURVE_STRENGTH = 100;

    // Color constants
    private static final Color UI_COLOR = Color.web("#3b82f6");
    private static final Color BUSINESS_COLOR = Color.web("#10b981");
    private static final Color DATA_COLOR = Color.web("#ef4444");
    private static final Color OTHER_COLOR = Color.web("#9ca3af");
    private static final Color PRIMARY_500 = Color.web("#3b82f6");
    private static final Color PRIMARY_600 = Color.web("#2563eb");
    private static final Color SUCCESS = Color.web("#10b981");
    private static final Color WARNING = Color.web("#f59e0b");
    private static final Color ERROR = Color.web("#ef4444");
    private static final Color PURPLE = Color.web("#8b5cf6");
    private static final Color INFO = Color.web("#0ea5e9");
    private static final Color GRAY_400 = Color.web("#9ca3af");

    // Special node identifiers
    private static final String START_NODE_ID = "_START_NODE_";
    private static final String END_NODE_ID = "_END_NODE_";

    // Data structures for graph elements
    private Map<String, Shape> nodeMap = new HashMap<>();
    private Map<String, Text> labelMap = new HashMap<>();
    private Map<String, Circle> indicatorMap = new HashMap<>();
    private Map<ComponentRelationship, Path> edgeMap = new HashMap<>();
    private Pane graphPane;
    private ScrollPane sharedScrollPane;
    private List<Path> edges = new ArrayList<>();
    private List<Polygon> arrowheads = new ArrayList<>();
    private double currentZoom = 1.0;
    private Set<String> selectedNodes = new HashSet<>();
    // Holds the last created legend so controllers can place it in a fixed overlay
    private VBox lastLegend;

    // Add new layout types
    public enum LayoutType {
        HIERARCHICAL, FORCE_DIRECTED, CIRCULAR, GRID, LAYERED,
        USER_FLOW,           // NEW: Show user journey flows
        BUSINESS_PROCESS,    // NEW: Show business processes
        FEATURE_BASED,       // NEW: Group by features
        HYBRID              // NEW: Combine multiple views
    }

    // Add new visualization modes
    public enum VisualizationMode {
        TECHNICAL_ARCHITECTURE,  // Current implementation
        USER_JOURNEY,           // NEW: Focus on user flows
        BUSINESS_PROCESS,       // NEW: Focus on business logic
        FEATURE_OVERVIEW,       // NEW: Feature-based grouping
        INTEGRATION_MAP         // NEW: External integrations
    }

    private VisualizationMode currentMode = VisualizationMode.TECHNICAL_ARCHITECTURE;

    private LayoutType currentLayout = LayoutType.HIERARCHICAL;
    private boolean showGrid = false;
    private boolean showLabels = true;

    public ScrollPane createGraphView(ProjectAnalysisResult result) {
        if (graphPane == null) {
            graphPane = new Pane();
            graphPane.getStyleClass().add("graph-container");
        } else {
            graphPane.getChildren().clear();
        }
        edges.clear();
        arrowheads.clear();
        nodeMap.clear();
        labelMap.clear();
        indicatorMap.clear();

        // Set up the graph pane with modern styling
        graphPane.getStyleClass().add("graph-container");

        // NEW: Create multi-mode visualization
        switch (currentMode) {
            case USER_JOURNEY:
                return createUserJourneyView(result);
            case BUSINESS_PROCESS:
                return createBusinessProcessView(result);
            case FEATURE_OVERVIEW:
                return createFeatureBasedView(result);
            case INTEGRATION_MAP:
                return createIntegrationMapView(result);
            case TECHNICAL_ARCHITECTURE:
            default:
                return createTechnicalArchitectureView(result);
        }

        // REMOVE ALL THE CODE BELOW - IT'S UNREACHABLE!
        // DELETE FROM HERE DOWN TO THE END OF THE METHOD
    }


    private ScrollPane createTechnicalArchitectureView(ProjectAnalysisResult result) {
        // This should contain your EXISTING createGraphView logic
        ProjectAnalysisResult enhancedResult = enhanceWithStartEndNodes(result);
        calculateDynamicCanvasSize(enhancedResult);

        if (showGrid) {
            createBackgroundGrid();
        }

        createModernNodes(enhancedResult);

        switch (currentLayout) {
            case FORCE_DIRECTED:
                applyForceDirectedLayout(enhancedResult, 300);
                break;
            case HIERARCHICAL:
            default:
                applyHierarchicalLayout(enhancedResult);
                break;
            case CIRCULAR:
                applyCircularLayout(enhancedResult);
                break;
            case GRID:
                applyGridLayout(enhancedResult);
                break;
            case LAYERED:
                applyLayeredLayout(enhancedResult);
                break;
        }

        // Final pass to prevent any overlaps regardless of layout
        resolveOverlapsForCurrentNodes();

        createEnhancedEdges(enhancedResult);
        addModernInteractions(enhancedResult);

        // Add legend for technical architecture
        addTechnicalArchitectureLegend();

        return createEnhancedScrollPane();
    }

    private ScrollPane createUserJourneyView(ProjectAnalysisResult result) {
        // Ensure canvas is properly sized and optional grid is shown
        calculateDynamicCanvasSize(result);
        if (showGrid) {
            createBackgroundGrid();
        }

        // Extract user flows from Android components
        List<UserFlowComponent> userFlows = extractUserFlows(result);

        // Augment with synthetic Start/End and required navigation links
        augmentUserFlowsWithStartEnd(userFlows);

        // Create user flow nodes
        createUserFlowNodes(userFlows);

        // Apply user flow specific layout before edges so geometry is correct
        applyUserFlowLayout(userFlows);

        // Prevent overlaps before creating edges
        resolveOverlapsForCurrentNodes();

        // Create navigation flow edges after nodes are positioned
        createNavigationFlowEdges(userFlows);

        // Add a simple legend to help first-time readers
        addUserJourneyLegend();

        // Add progressive disclosure interactions
        addProgressiveDisclosureInteractions(userFlows);

        return createEnhancedScrollPane();
    }

    private List<UserFlowComponent> extractUserFlows(ProjectAnalysisResult result) {
        List<UserFlowComponent> userFlows = new ArrayList<>();

        for (CodeComponent component : result.getComponents()) {
            if (isUserInterfaceComponent(component)) {
                UserFlowComponent userFlow = createUserFlowFromComponent(component);

                // FIXED: Handle navigation destinations correctly
                if (component.getNavigationDestinations() != null) {
                    for (NavigationDestination dest : component.getNavigationDestinations()) {
                        NavigationFlow navFlow = new NavigationFlow();
                        navFlow.setFlowId(component.getId() + "_to_" + dest.getDestinationId());
                        navFlow.setSourceScreenId(component.getId());
                        navFlow.setTargetScreenId(dest.getDestinationId());
                        navFlow.setNavigationType(NavigationFlow.NavigationType.FORWARD);

                        // Create NavigationPath and add to user flow
                        NavigationPath navPath = new NavigationPath(navFlow);
                        userFlow.addOutgoingPath(navPath);
                    }
                }

                userFlows.add(userFlow);
            }
        }

        // Categorize flow types
        categorizeUserFlowTypes(userFlows, result);
        return userFlows;
    }


    private boolean isUserInterfaceComponent(CodeComponent component) {
        if (component.getType() == null) return false;

        String type = component.getType().toLowerCase();
        String name = component.getName().toLowerCase();
        String extendsClass = component.getExtendsClass();

        return type.contains("activity") ||
                type.contains("fragment") ||
                name.contains("activity") ||
                name.contains("fragment") ||
                (extendsClass != null && (
                        extendsClass.contains("Activity") ||
                                extendsClass.contains("Fragment") ||
                                extendsClass.contains("DialogFragment")
                ));
    }

    private UserFlowComponent createUserFlowFromComponent(CodeComponent component) {
        UserFlowComponent userFlow = new UserFlowComponent();
        userFlow.setId(component.getId());
        userFlow.setScreenName(component.getName());
        userFlow.setActivityName(component.getName());

        // Analyze business context
        BusinessContext context = analyzeBusinessContext(component);
        userFlow.setBusinessContext(context);

        // Extract user actions from component
        List<UserAction> actions = extractUserActions(component);
        userFlow.setUserActions(actions);

        return userFlow;
    }

    private BusinessContext analyzeBusinessContext(CodeComponent component) {
        String name = component.getName().toLowerCase();

        // Determine business goal based on component name/type
        String businessGoal = "Unknown";
        String userPersona = "General User";

        if (name.contains("login") || name.contains("auth")) {
            businessGoal = "User Authentication";
            userPersona = "New/Returning User";
        } else if (name.contains("main") || name.contains("home")) {
            businessGoal = "App Entry Point";
            userPersona = "Authenticated User";
        } else if (name.contains("profile") || name.contains("account")) {
            businessGoal = "User Profile Management";
            userPersona = "Registered User";
        } else if (name.contains("payment") || name.contains("checkout")) {
            businessGoal = "Transaction Processing";
            userPersona = "Purchasing User";
        } else if (name.contains("search")) {
            businessGoal = "Content Discovery";
            userPersona = "Content Consumer";
        }

        BusinessContext context = new BusinessContext(
                component.getId() + "_context",
                businessGoal,
                userPersona
        );

        return context;
    }

    private List<UserAction> extractUserActions(CodeComponent component) {
        List<UserAction> actions = new ArrayList<>();

        // Analyze methods to infer user actions
        for (CodeMethod method : component.getMethods()) {
            String methodName = method.getName().toLowerCase();

            if (methodName.contains("onclick") || methodName.contains("ontouch")) {
                actions.add(new UserAction(
                        method.getName(),
                        "Tap " + extractTargetFromMethodName(methodName),
                        UserAction.ActionType.TAP
                ));
            } else if (methodName.contains("onswipe")) {
                actions.add(new UserAction(
                        method.getName(),
                        "Swipe " + extractTargetFromMethodName(methodName),
                        UserAction.ActionType.SWIPE
                ));
            } else if (methodName.contains("ontext") || methodName.contains("oninput")) {
                actions.add(new UserAction(
                        method.getName(),
                        "Enter Text",
                        UserAction.ActionType.TYPE_TEXT
                ));
            }
        }

        return actions;
    }

    private void augmentUserFlowsWithStartEnd(List<UserFlowComponent> userFlows) {
        // Build quick lookup by id for existing flows
        Map<String, UserFlowComponent> byId = new HashMap<>();
        for (UserFlowComponent f : userFlows) {
            byId.put(f.getId(), f);
        }

        // Compute incoming counts for each existing node
        Map<String, Integer> incoming = new HashMap<>();
        for (UserFlowComponent f : userFlows) {
            incoming.putIfAbsent(f.getId(), 0);
        }
        for (UserFlowComponent f : userFlows) {
            if (f.getOutgoingPaths() != null) {
                for (NavigationPath p : f.getOutgoingPaths()) {
                    NavigationFlow nf = p.getNavigationFlow();
                    if (nf != null && nf.getTargetScreenId() != null) {
                        incoming.put(nf.getTargetScreenId(), incoming.getOrDefault(nf.getTargetScreenId(), 0) + 1);
                    }
                }
            }
        }

        // Identify entries (no incoming) and exits (no outgoing)
        List<UserFlowComponent> entries = new ArrayList<>();
        List<UserFlowComponent> exits = new ArrayList<>();
        for (UserFlowComponent f : userFlows) {
            if (START_NODE_ID.equals(f.getId()) || END_NODE_ID.equals(f.getId())) continue;
            int in = incoming.getOrDefault(f.getId(), 0);
            boolean hasOutgoing = f.getOutgoingPaths() != null && !f.getOutgoingPaths().isEmpty();
            if (in == 0) entries.add(f);
            if (!hasOutgoing) exits.add(f);
        }

        // Create Start node if missing
        UserFlowComponent start = byId.get(START_NODE_ID);
        if (start == null) {
            start = new UserFlowComponent();
            start.setId(START_NODE_ID);
            start.setScreenName("Start");
            start.setActivityName("Start");
            start.setFlowType(UserFlowComponent.FlowType.ENTRY_POINT);
            userFlows.add(0, start);
            byId.put(START_NODE_ID, start);
        }

        // Create End node if missing
        UserFlowComponent end = byId.get(END_NODE_ID);
        if (end == null) {
            end = new UserFlowComponent();
            end.setId(END_NODE_ID);
            end.setScreenName("End");
            end.setActivityName("End");
            end.setFlowType(UserFlowComponent.FlowType.EXIT_POINT);
            userFlows.add(end);
            byId.put(END_NODE_ID, end);
        }

        // Ensure Start has outgoing to each entry; if none entries and there are normal nodes, pick first normal
        if (start.getOutgoingPaths() == null) start.setOutgoingPaths(new ArrayList<>());
        if (entries.isEmpty()) {
            // Fallback: if there are any normal nodes, connect to the first one
            for (UserFlowComponent f : userFlows) {
                if (!START_NODE_ID.equals(f.getId()) && !END_NODE_ID.equals(f.getId())) {
                    entries.add(f);
                    break;
                }
            }
        }
        for (UserFlowComponent e : entries) {
            boolean alreadyLinked = start.getOutgoingPaths().stream()
                    .map(NavigationPath::getNavigationFlow)
                    .filter(Objects::nonNull)
                    .anyMatch(nf -> e.getId().equals(nf.getTargetScreenId()));
            if (!alreadyLinked) {
                NavigationFlow nf = new NavigationFlow();
                nf.setFlowId(START_NODE_ID + "_to_" + e.getId());
                nf.setSourceScreenId(START_NODE_ID);
                nf.setTargetScreenId(e.getId());
                nf.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                start.addOutgoingPath(new NavigationPath(nf));
            }
        }

        // Ensure each exit connects to End
        for (UserFlowComponent x : exits) {
            if (x.getOutgoingPaths() == null) x.setOutgoingPaths(new ArrayList<>());
            boolean linkedToEnd = x.getOutgoingPaths().stream()
                    .map(NavigationPath::getNavigationFlow)
                    .filter(Objects::nonNull)
                    .anyMatch(nf -> END_NODE_ID.equals(nf.getTargetScreenId()));
            if (!linkedToEnd) {
                NavigationFlow nf = new NavigationFlow();
                nf.setFlowId(x.getId() + "_to_" + END_NODE_ID);
                nf.setSourceScreenId(x.getId());
                nf.setTargetScreenId(END_NODE_ID);
                nf.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                x.addOutgoingPath(new NavigationPath(nf));
            }
        }

        // Reorder list to guarantee Start first and End last
        userFlows.removeIf(f -> START_NODE_ID.equals(f.getId()) || END_NODE_ID.equals(f.getId()));
        userFlows.add(0, start);
        userFlows.add(end);
    }

    private void createUserFlowNodes(List<UserFlowComponent> userFlows) {
        for (UserFlowComponent flow : userFlows) {
            Shape node = createUserFlowNode(flow);
            nodeMap.put(flow.getId(), node);
            graphPane.getChildren().add(node);

            // Create a direct Text label and register it, honoring showLabels
            if (showLabels) {
                Text label = new Text(flow.getScreenName());
                label.setFont(Font.font("System", FontWeight.BOLD, 14));
                label.setFill(Color.BLACK);
                labelMap.put(flow.getId(), label);
                graphPane.getChildren().add(label);
            }
        }
    }

    private Shape createUserFlowNode(UserFlowComponent flow) {
        Rectangle rect = new Rectangle(NODE_WIDTH + 40, NODE_HEIGHT + 30);
        rect.setArcWidth(20);
        rect.setArcHeight(20);

        // Color based on flow type
        Color fillColor = getColorForFlowType(flow.getFlowType());
        rect.setFill(fillColor);

        // Enhanced styling for user flow nodes
        rect.setStroke(Color.web("#2d3748"));
        rect.setStrokeWidth(2);

        // Add glow effect for important flows
        if (flow.getFlowType() == UserFlowComponent.FlowType.ENTRY_POINT ||
                flow.getFlowType() == UserFlowComponent.FlowType.DECISION_POINT) {

            DropShadow glow = new DropShadow();
            glow.setRadius(15);
            glow.setColor(fillColor);
            rect.setEffect(glow);
        }

        return rect;
    }

    private Color getColorForFlowType(UserFlowComponent.FlowType flowType) {
        switch (flowType) {
            case ENTRY_POINT: return Color.web("#10b981"); // Green
            case MAIN_FLOW: return Color.web("#3b82f6");   // Blue
            case DECISION_POINT: return Color.web("#f59e0b"); // Yellow
            case EXIT_POINT: return Color.web("#ef4444");  // Red
            case ERROR_HANDLING: return Color.web("#f97316"); // Orange
            default: return Color.web("#6b7280"); // Gray
        }
    }

    // NEW: Create enhanced label with business context
    private VBox createEnhancedUserFlowLabel(UserFlowComponent flow) {
        VBox container = new VBox(5);
        container.setAlignment(javafx.geometry.Pos.CENTER);

        // Screen name
        Text screenLabel = new Text(flow.getScreenName());
        screenLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        screenLabel.setFill(Color.BLACK);

        // Business context
        if (flow.getBusinessContext() != null) {
            Text contextLabel = new Text(flow.getBusinessContext().getBusinessGoal());
            contextLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
            contextLabel.setFill(Color.web("#6b7280"));
            container.getChildren().addAll(screenLabel, contextLabel);
        } else {
            container.getChildren().add(screenLabel);
        }

        return container;
    }

    private ScrollPane createBusinessProcessView(ProjectAnalysisResult result) {
        // Ensure canvas is properly sized and optional grid is shown
        calculateDynamicCanvasSize(result);
        if (showGrid) {
            createBackgroundGrid();
        }

        List<BusinessProcessComponent> processes = extractBusinessProcesses(result);

        // Create process flow visualization
        createBusinessProcessNodes(processes);
        // Layout nodes before creating edges so edge geometry is correct
        applyBusinessProcessLayout(processes);

        // Prevent overlaps before creating edges
        resolveOverlapsForCurrentNodes();

        createBusinessProcessEdges(processes);

        // Add legend for business process view
        addBusinessProcessLegend();

        return createEnhancedScrollPane();
    }

    // NEW: Create feature-based grouping visualization
    private ScrollPane createFeatureBasedView(ProjectAnalysisResult result) {
        // Ensure canvas is properly sized and optional grid is shown
        calculateDynamicCanvasSize(result);
        if (showGrid) {
            createBackgroundGrid();
        }

        List<FeatureGroup> featureGroups = groupComponentsByFeature(result);

        // Create feature group visualization
        createFeatureGroupNodes(featureGroups);
        // Layout nodes before creating edges so edge geometry is correct
        applyFeatureGroupLayout(featureGroups);

        // Prevent overlaps before creating edges
        resolveOverlapsForCurrentNodes();

        createFeatureGroupEdges(featureGroups);

        // Add legend for feature overview
        addFeatureOverviewLegend();

        return createEnhancedScrollPane();
    }

    private void addProgressiveDisclosureInteractions(List<UserFlowComponent> userFlows) {
        for (UserFlowComponent flow : userFlows) {
            Shape node = nodeMap.get(flow.getId());
            if (node != null) {
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        // Double-click to drill down
                        showDetailedView(flow);
                    } else {
                        // Single-click to show context
                        showContextualInfo(flow);
                    }
                });
            }
        }
    }

    // NEW: Show detailed view of a flow
    private void showDetailedView(UserFlowComponent flow) {
        // Create popup or side panel with detailed information
        showFlowDetailsDialog(flow);
    }

    // NEW: Show contextual information
    private void showContextualInfo(UserFlowComponent flow) {
        // Update context panel with flow information
        displayFlowContext(flow);
    }

    // Setters for new modes
    public void setVisualizationMode(VisualizationMode mode) {
        this.currentMode = mode;
    }


    private void calculateDynamicCanvasSize(ProjectAnalysisResult result) {
        int componentCount = result.getComponents().size();

        // Calculate required dimensions based on component count and layout
        double width, height;

        switch (currentLayout) {
            case HIERARCHICAL:
            case LAYERED:
                // For hierarchical layouts, we need width for layers and height for nodes in each layer
                Map<String, List<CodeComponent>> layers = groupByLayers(result);
                width = layers.size() * LAYER_SPACING + 400;
                int maxNodesInLayer = layers.values().stream()
                        .mapToInt(List::size)
                        .max().orElse(1);
                height = maxNodesInLayer * MIN_NODE_DISTANCE + 300;
                break;
            case GRID:
                // For grid layout, calculate based on square root of components
                int cols = (int) Math.ceil(Math.sqrt(componentCount));
                int rows = (int) Math.ceil((double) componentCount / cols);
                width = cols * MIN_NODE_DISTANCE + 400;
                height = rows * MIN_NODE_DISTANCE + 400;
                break;
            case CIRCULAR:
                // For circular layout, calculate based on circumference
                double radius = Math.max(300, componentCount * 25);
                width = height = radius * 2 + 400;
                break;
            case FORCE_DIRECTED:
            default:
                // For force-directed, use a generous size
                width = Math.max(1600, componentCount * 60);
                height = Math.max(1200, componentCount * 50);
        }

        // Add extra space for start and end nodes
        height += 200;

        graphPane.setPrefSize(width, height);
        graphPane.setMinSize(width, height);
    }

    private void createBackgroundGrid() {
        double spacing = 50;
        double width = graphPane.getPrefWidth();
        double height = graphPane.getPrefHeight();

        // Create grid lines
        for (double x = 0; x <= width; x += spacing) {
            Line line = new Line(x, 0, x, height);
            line.setStroke(Color.web("#f3f4f6"));
            line.setStrokeWidth(0.5);
            line.getStyleClass().add("grid-line");
            graphPane.getChildren().add(line);
        }

        for (double y = 0; y <= height; y += spacing) {
            Line line = new Line(0, y, width, y);
            line.setStroke(Color.web("#f3f4f6"));
            line.setStrokeWidth(0.5);
            line.getStyleClass().add("grid-line");
            graphPane.getChildren().add(line);
        }
    }

    private Map<String, List<CodeComponent>> groupByLayers(ProjectAnalysisResult result) {
        Map<String, List<CodeComponent>> layers = new LinkedHashMap<>();
        layers.put("UI", new ArrayList<>());
        layers.put("Business Logic", new ArrayList<>());
        layers.put("Data", new ArrayList<>());
        layers.put("Other", new ArrayList<>());

        for (CodeComponent component : result.getComponents()) {
            String layer = component.getLayer();
            if (layer == null || !layers.containsKey(layer)) {
                layers.get("Other").add(component);
            } else {
                layers.get(layer).add(component);
            }
        }

        return layers;
    }

    private ProjectAnalysisResult enhanceWithStartEndNodes(ProjectAnalysisResult original) {
        ProjectAnalysisResult enhanced = new ProjectAnalysisResult();
        enhanced.setProjectName(original.getProjectName());
        enhanced.setProjectPath(original.getProjectPath());
        enhanced.setComponents(new ArrayList<>(original.getComponents()));
        enhanced.setRelationships(new ArrayList<>(original.getRelationships()));
        enhanced.setGradleDependencies(original.getGradleDependencies());
        enhanced.setFlutterDependencies(original.getFlutterDependencies());
        enhanced.setJsDependencies(original.getJsDependencies());

        // Create enhanced start and end nodes
        CodeComponent startNode = new CodeComponent();
        startNode.setId(START_NODE_ID);
        startNode.setName("Start");
        startNode.setType("start");
        startNode.setLanguage("system");
        enhanced.addComponent(startNode);

        CodeComponent endNode = new CodeComponent();
        endNode.setId(END_NODE_ID);
        endNode.setName("End");
        endNode.setType("end");
        endNode.setLanguage("system");
        enhanced.addComponent(endNode);

        // Add intelligent start/end connections
        addIntelligentConnections(enhanced);

        return enhanced;
    }

    private Set<String> findEntryPoints(ProjectAnalysisResult result) {
        Set<String> entryPoints = new HashSet<>();
        Set<String> hasIncoming = new HashSet<>();

        // Find components that have no incoming relationships
        for (ComponentRelationship rel : result.getRelationships()) {
            hasIncoming.add(rel.getTargetId());
        }

        for (CodeComponent comp : result.getComponents()) {
            if (!hasIncoming.contains(comp.getId()) && !comp.getId().equals(START_NODE_ID) && !comp.getId().equals(END_NODE_ID)) {
                entryPoints.add(comp.getId());
            }
        }

        return entryPoints;
    }

    private Set<String> findExitPoints(ProjectAnalysisResult result) {
        Set<String> exitPoints = new HashSet<>();
        Set<String> hasOutgoing = new HashSet<>();

        // Find components that have no outgoing relationships
        for (ComponentRelationship rel : result.getRelationships()) {
            hasOutgoing.add(rel.getSourceId());
        }

        for (CodeComponent comp : result.getComponents()) {
            if (!hasOutgoing.contains(comp.getId()) && !comp.getId().equals(START_NODE_ID) && !comp.getId().equals(END_NODE_ID)) {
                exitPoints.add(comp.getId());
            }
        }

        return exitPoints;
    }

    private void createModernNodes(ProjectAnalysisResult result) {
        for (CodeComponent component : result.getComponents()) {
            Shape node;

            // Create nodes but don't position them yet (positioning happens in layout methods)
            if (isSpecialNode(component)) {
                // Create special nodes (start/end) as circles
                Ellipse ellipse = new Ellipse(SPECIAL_NODE_SIZE / 2, SPECIAL_NODE_SIZE / 2);

                if (component.getId().equals(START_NODE_ID)) {
                    ellipse.setFill(SUCCESS);
                } else {
                    ellipse.setFill(ERROR);
                }

                node = ellipse;
            } else {
                // Create regular nodes as rounded rectangles
                Rectangle rect = new Rectangle(NODE_WIDTH, NODE_HEIGHT);
                rect.setArcWidth(15);
                rect.setArcHeight(15);

                // Apply solid color based on layer
                Color layerColor = getColorForLayer(component.getLayer());
                rect.setFill(layerColor);

                node = rect;
            }

            // Add 2D styling
            node.setStroke(Color.web("#374151")); // Dark gray border
            node.setStrokeWidth(1.5);
            node.setEffect(null); // Remove any effects for 2D look

            // Store node reference
            nodeMap.put(component.getId(), node);

            // Add the node to the graph first
            graphPane.getChildren().add(node);

            // Create label
            if (showLabels) {
                Text label = createNodeLabel(component);
                labelMap.put(component.getId(), label);
                graphPane.getChildren().add(label); // Add label after node
            }

            // Create language indicator
            Circle indicator = createLanguageIndicator(component);
            indicatorMap.put(component.getId(), indicator);
            graphPane.getChildren().add(indicator); // Add indicator after node
        }
    }

    private Color getColorForLayer(String layer) {
        if (layer == null) return OTHER_COLOR;

        switch (layer) {
            case "UI": return UI_COLOR;
            case "Business Logic": return BUSINESS_COLOR;
            case "Data": return DATA_COLOR;
            default: return OTHER_COLOR;
        }
    }

    private Text createNodeLabel(CodeComponent component) {
        // Use the component name (class name) directly
        Text label = new Text(component.getName());
        label.setFont(Font.font("System", FontWeight.BOLD, 12)); // Smaller, bold font
        label.setFill(Color.BLACK); // Change to black for better visibility
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrappingWidth(NODE_WIDTH - 20); // Allow text wrapping with more padding

        return label;
    }

    private Circle createLanguageIndicator(CodeComponent component) {
        Circle indicator = new Circle(6);

        // Set color based on language
        Color languageColor = getColorForLanguage(component.getLanguage());
        indicator.setFill(languageColor);
        indicator.setStroke(Color.WHITE);
        indicator.setStrokeWidth(1.5);

        return indicator;
    }

    private Color getColorForLanguage(String language) {
        if (language == null) return GRAY_400;

        switch (language.toLowerCase()) {
            case "java": return PRIMARY_500;
            case "kotlin": return PURPLE;
            case "dart": return INFO;
            case "javascript": return WARNING;
            case "typescript": return PRIMARY_600;
            case "python": return Color.web("#3776ab");
            case "c++": return Color.web("#f34b7d");
            case "c#": return Color.web("#178600");
            default: return GRAY_400;
        }
    }

    private void createEnhancedEdges(ProjectAnalysisResult result) {
        for (ComponentRelationship relationship : result.getRelationships()) {
            Shape sourceNode = nodeMap.get(relationship.getSourceId());
            Shape targetNode = nodeMap.get(relationship.getTargetId());

            if (sourceNode != null && targetNode != null) {
                Path edge = createCurvedEdge(sourceNode, targetNode, relationship.getType());
                edges.add(edge);
                edgeMap.put(relationship, edge); // Store relationship → path mapping
                graphPane.getChildren().add(edge);

                Polygon arrowhead = createArrowhead(edge);
                arrowheads.add(arrowhead);
                graphPane.getChildren().add(arrowhead);
            }
        }
    }

    private Path createCurvedEdge(Shape source, Shape target, String relationshipType) {
        // Delegated to EdgeRenderer for cleaner separation of concerns
        return EdgeRenderer.createCurvedEdge(source, target, relationshipType, CURVE_STRENGTH);
    }

    private Color getColorForRelationship(String relationshipType) {
        // Delegate to EdgeRenderer for consistent coloring
        return EdgeRenderer.getColorForRelationship(relationshipType);
    }

    private Polygon createArrowhead(Path edge) {
        // Delegated to EdgeRenderer for consistent arrow styling
        return EdgeRenderer.createArrowhead(edge, ARROW_SIZE);
    }

    private void addModernInteractions(ProjectAnalysisResult result) {
        for (Map.Entry<String, Shape> entry : nodeMap.entrySet()) {
            String nodeId = entry.getKey();
            Shape node = entry.getValue();

            // Find the component
            CodeComponent component = null;
            for (CodeComponent comp : result.getComponents()) {
                if (comp.getId().equals(nodeId)) {
                    component = comp;
                    break;
                }
            }

            if (component == null) continue;

            final CodeComponent finalComponent = component;

            // Add click handlers
            node.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                    handleNodeSelection(finalComponent, result);
                } else if (event.getClickCount() == 2) {
                    handleNodeDoubleClick(finalComponent);
                }
            });

            // Add tooltip
            Tooltip tooltip = new Tooltip(createTooltipText(finalComponent));
            Tooltip.install(node, tooltip);
        }

        // Add edge interactions
        for (Path edge : edges) {
            edge.setOnMouseEntered(event -> highlightEdgePath(edge));
            edge.setOnMouseExited(event -> resetEdgeStyle(edge));
        }
    }

    private String createTooltipText(CodeComponent component) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(component.getName()).append("\n");
        sb.append("Type: ").append(component.getType()).append("\n");
        sb.append("Language: ").append(component.getLanguage()).append("\n");

        if (component.getLayer() != null) {
            sb.append("Layer: ").append(component.getLayer()).append("\n");
        }

        if (component.getFilePath() != null) {
            sb.append("Path: ").append(component.getFilePath());
        }

        return sb.toString();
    }

    private void handleNodeSelection(CodeComponent component, ProjectAnalysisResult result) {
        // Clear previous selections
        clearSelections();

        // Add to selected nodes
        selectedNodes.add(component.getId());

        // Highlight selected node
        highlightNode(component.getId(), Color.web("#f59e0b"), true);

        // Highlight related nodes
        highlightRelatedNodes(component, result);

        // Trigger selection event (would be handled by MainController)
        fireNodeSelectionEvent(component);
    }

    private void handleNodeDoubleClick(CodeComponent component) {
        // Trigger navigation to source (would be handled by MainController)
        fireNodeNavigationEvent(component);
    }

    private void clearSelections() {
        selectedNodes.clear();

        // Reset all node styles
        for (Map.Entry<String, Shape> entry : nodeMap.entrySet()) {
            Shape node = entry.getValue();
            resetNodeStyle(node, entry.getKey());
        }

        // Reset all edge styles
        for (Path edge : edges) {
            resetEdgeStyle(edge);
        }
    }

    private void highlightNode(String nodeId, Color highlightColor, boolean selected) {
        Shape node = nodeMap.get(nodeId);
        if (node == null) return;

        if (node instanceof Rectangle) {
            Rectangle rect = (Rectangle) node;
            rect.setStroke(highlightColor);
            rect.setStrokeWidth(selected ? 3 : 2.5);

        } else if (node instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) node;
            ellipse.setStroke(highlightColor);
            ellipse.setStrokeWidth(selected ? 4 : 3);
        }
    }

    private void highlightRelatedNodes(CodeComponent component, ProjectAnalysisResult result) {
        // Highlight dependencies (outgoing)
        for (ComponentRelationship rel : result.getRelationships()) {
            if (rel.getSourceId().equals(component.getId())) {
                highlightNode(rel.getTargetId(), SUCCESS, false);
                highlightEdgeForRelationship(rel, SUCCESS);
            }
            // Highlight dependents (incoming)
            else if (rel.getTargetId().equals(component.getId())) {
                highlightNode(rel.getSourceId(), PURPLE, false);
                highlightEdgeForRelationship(rel, PURPLE);
            }
        }
    }

    private void highlightEdgeForRelationship(ComponentRelationship rel, Color color) {
        Path edge = edgeMap.get(rel);
        if (edge != null) {
            edge.setStroke(color);
            edge.setOpacity(1.0);
            edge.setStrokeWidth(3.5);
        }
    }

    private void highlightEdgePath(Path edge) {
        // Reset all other edges
        for (Path e : edges) {
            resetEdgeStyle(e);
        }

        // Highlight selected edge
        edge.setStroke(WARNING);
        edge.setStrokeWidth(4);
        edge.setOpacity(1.0);

        // Find and highlight corresponding arrowhead
        for (Polygon arrowhead : arrowheads) {
            arrowhead.setFill(WARNING);
            arrowhead.setStroke(WARNING);
        }
    }

    private void resetNodeStyle(Shape node, String nodeId) {
        if (node instanceof Rectangle) {
            Rectangle rect = (Rectangle) node;
            rect.setStroke(Color.WHITE);
            rect.setStrokeWidth(2);

            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(8);
            dropShadow.setOffsetY(3);
            dropShadow.setColor(Color.color(0, 0, 0, 0.15));
            rect.setEffect(dropShadow);
        } else if (node instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) node;
            ellipse.setStroke(Color.WHITE);
            ellipse.setStrokeWidth(3);

            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(12);
            dropShadow.setOffsetY(4);
            dropShadow.setColor(Color.color(0, 0, 0, 0.25));
            ellipse.setEffect(dropShadow);
        }
    }

    private void resetEdgeStyle(Path edge) {
        // Find relationship that owns this edge
        ComponentRelationship relationship = edgeMap.entrySet().stream()
                .filter(e -> e.getValue() == edge)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (relationship != null) {
            edge.setOpacity(0.8);
            edge.setStrokeWidth(2.5);
            edge.setStroke(getColorForRelationship(relationship.getType()));
        }
    }

    private ScrollPane createEnhancedScrollPane() {
        if (sharedScrollPane == null) {
            sharedScrollPane = new ScrollPane();
            sharedScrollPane.setFitToWidth(false);
            sharedScrollPane.setFitToHeight(false);
            sharedScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sharedScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sharedScrollPane.setPannable(true);
            sharedScrollPane.getStyleClass().add("graph-scroll-pane");

            // Enhanced zoom with mouse wheel (register once)
            sharedScrollPane.setOnScroll(event -> {
                if (event.isControlDown()) {
                    double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                    zoom(zoomFactor, sharedScrollPane);
                    event.consume();
                }
            });
        }
        // Always point to current graphPane
        sharedScrollPane.setContent(graphPane);
        return sharedScrollPane;
    }

    public void zoom(double factor, ScrollPane scrollPane) {
        currentZoom *= factor;

        // Limit zoom range
        currentZoom = Math.max(0.1, Math.min(5.0, currentZoom));

        // Apply scaling with smooth animation
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), graphPane);
        scaleTransition.setToX(currentZoom);
        scaleTransition.setToY(currentZoom);
        scaleTransition.setInterpolator(Interpolator.EASE_OUT);
        scaleTransition.play();
    }

    public void fitToWindow(ScrollPane scrollPane) {
        if (graphPane == null || scrollPane == null) return;

        // Ensure we are using layout bounds (unscaled) for accurate sizing
        javafx.geometry.Bounds bounds = graphPane.getLayoutBounds();
        double contentWidth = bounds.getWidth();
        double contentHeight = bounds.getHeight();

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        if (contentWidth <= 0 || contentHeight <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            return; // Nothing to fit yet
        }

        double padding = 40.0; // small margin so content doesn't touch edges
        double scaleX = (viewportWidth - padding) / contentWidth;
        double scaleY = (viewportHeight - padding) / contentHeight;
        double scale = Math.min(scaleX, scaleY);

        // Clamp to allowed range
        scale = Math.max(0.1, Math.min(5.0, scale));
        currentZoom = scale;

        graphPane.setScaleX(scale);
        graphPane.setScaleY(scale);

        // Center the content
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    private boolean isSpecialNode(CodeComponent component) {
        return START_NODE_ID.equals(component.getId()) ||
                END_NODE_ID.equals(component.getId()) ||
                "start".equals(component.getType()) ||
                "end".equals(component.getType());
    }

    private void fireNodeSelectionEvent(CodeComponent component) {
        // This would fire a custom event that MainController would listen to
        System.out.println("Node selected: " + component.getName());
    }

    private void fireNodeNavigationEvent(CodeComponent component) {
        // This would fire a navigation event
        System.out.println("Navigate to: " + component.getFilePath());
    }

    // Layout methods
    private void applyHierarchicalLayout(ProjectAnalysisResult result) {
        // Place start node at top center
        Shape startNode = nodeMap.get(START_NODE_ID);
        if (startNode != null) {
            if (startNode instanceof Ellipse) {
                ((Ellipse) startNode).setCenterX(graphPane.getPrefWidth() / 2);
                ((Ellipse) startNode).setCenterY(100);
            }

            // Position label for start node
            Text startLabel = labelMap.get(START_NODE_ID);
            if (startLabel != null) {
                double textWidth = startLabel.getLayoutBounds().getWidth();
                startLabel.setX(graphPane.getPrefWidth() / 2 - textWidth / 2);
                startLabel.setY(100 + 40);
            }

            // Position indicator for start node
            Circle startIndicator = indicatorMap.get(START_NODE_ID);
            if (startIndicator != null) {
                startIndicator.setCenterX(graphPane.getPrefWidth() / 2 + 25);
                startIndicator.setCenterY(100 - 25);
            }
        }

        // Place end node at bottom center
        Shape endNode = nodeMap.get(END_NODE_ID);
        if (endNode != null) {
            if (endNode instanceof Ellipse) {
                ((Ellipse) endNode).setCenterX(graphPane.getPrefWidth() / 2);
                ((Ellipse) endNode).setCenterY(graphPane.getPrefHeight() - 100);
            }

            // Position label for end node
            Text endLabel = labelMap.get(END_NODE_ID);
            if (endLabel != null) {
                double textWidth = endLabel.getLayoutBounds().getWidth();
                endLabel.setX(graphPane.getPrefWidth() / 2 - textWidth / 2);
                endLabel.setY(graphPane.getPrefHeight() - 100 + 40);
            }

            // Position indicator for end node
            Circle endIndicator = indicatorMap.get(END_NODE_ID);
            if (endIndicator != null) {
                endIndicator.setCenterX(graphPane.getPrefWidth() / 2 + 25);
                endIndicator.setCenterY(graphPane.getPrefHeight() - 100 - 25);
            }
        }

        // Group components by layer for hierarchical placement
        Map<String, List<CodeComponent>> layers = groupByLayers(result);
        double layerWidth = graphPane.getPrefWidth() / (layers.size() + 1);
        int layerIndex = 1;

        for (Map.Entry<String, List<CodeComponent>> entry : layers.entrySet()) {
            List<CodeComponent> components = entry.getValue();
            double x = layerIndex * layerWidth;
            double layerHeight = graphPane.getPrefHeight() - 250; // Reserve space for start/end
            double ySpacing = layerHeight / (components.size() + 1);

            for (int i = 0; i < components.size(); i++) {
                CodeComponent comp = components.get(i);
                if (isSpecialNode(comp)) continue;

                Shape node = nodeMap.get(comp.getId());
                if (node instanceof Rectangle) {
                    double y = 150 + (i + 1) * ySpacing;

                    // Position the node
                    ((Rectangle) node).setX(x - NODE_WIDTH / 2);
                    ((Rectangle) node).setY(y - NODE_HEIGHT / 2);

                    // Position the label
                    Text label = labelMap.get(comp.getId());
                    if (label != null) {
                        double textWidth = label.getLayoutBounds().getWidth();
                        label.setX(x - textWidth / 2);
                        label.setY(y + NODE_HEIGHT / 2 + 20);
                    }

                    // Position the indicator
                    Circle indicator = indicatorMap.get(comp.getId());
                    if (indicator != null) {
                        indicator.setCenterX(x + NODE_WIDTH / 2 - 15);
                        indicator.setCenterY(y - NODE_HEIGHT / 2 + 15);
                    }
                }
            }
            layerIndex++;
        }
    }

    private void applyCircularLayout(ProjectAnalysisResult result) {
        List<CodeComponent> normalComponents = new ArrayList<>();
        Shape startNode = null;
        Shape endNode = null;

        // Separate special nodes from normal components
        for (CodeComponent comp : result.getComponents()) {
            if (isSpecialNode(comp)) {
                if (comp.getId().equals(START_NODE_ID)) {
                    startNode = nodeMap.get(START_NODE_ID);
                } else if (comp.getId().equals(END_NODE_ID)) {
                    endNode = nodeMap.get(END_NODE_ID);
                }
            } else {
                normalComponents.add(comp);
            }
        }

        // Place start node at top center
        if (startNode instanceof Ellipse) {
            ((Ellipse) startNode).setCenterX(graphPane.getPrefWidth() / 2);
            ((Ellipse) startNode).setCenterY(100);
        }

        // Place end node at bottom center
        if (endNode instanceof Ellipse) {
            ((Ellipse) endNode).setCenterX(graphPane.getPrefWidth() / 2);
            ((Ellipse) endNode).setCenterY(graphPane.getPrefHeight() - 100);
        }

        // Arrange normal components in a circle
        double centerX = graphPane.getPrefWidth() / 2;
        double centerY = graphPane.getPrefHeight() / 2;
        double radius = Math.min(graphPane.getPrefWidth(), graphPane.getPrefHeight()) / 3;

        for (int i = 0; i < normalComponents.size(); i++) {
            CodeComponent comp = normalComponents.get(i);
            Shape node = nodeMap.get(comp.getId());

            if (node instanceof Rectangle) {
                double angle = 2 * Math.PI * i / normalComponents.size();
                double x = centerX + radius * Math.cos(angle) - NODE_WIDTH / 2;
                double y = centerY + radius * Math.sin(angle) - NODE_HEIGHT / 2;

                ((Rectangle) node).setX(x);
                ((Rectangle) node).setY(y);

                // Update label position
                Text label = labelMap.get(comp.getId());
                if (label != null) {
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(centerX + radius * Math.cos(angle) - textWidth / 2);
                    label.setY(centerY + radius * Math.sin(angle) + NODE_HEIGHT / 2 + 20);
                }

                // Update indicator position
                Circle indicator = indicatorMap.get(comp.getId());
                if (indicator != null) {
                    indicator.setCenterX(x + NODE_WIDTH - 15);
                    indicator.setCenterY(y + 15);
                }
            }
        }
    }

    private void applyGridLayout(ProjectAnalysisResult result) {
        List<CodeComponent> normalComponents = new ArrayList<>();
        Shape startNode = null;
        Shape endNode = null;

        // Separate special nodes from normal components
        for (CodeComponent comp : result.getComponents()) {
            if (isSpecialNode(comp)) {
                if (comp.getId().equals(START_NODE_ID)) {
                    startNode = nodeMap.get(START_NODE_ID);
                } else if (comp.getId().equals(END_NODE_ID)) {
                    endNode = nodeMap.get(END_NODE_ID);
                }
            } else {
                normalComponents.add(comp);
            }
        }

        // Place start node at top center
        if (startNode instanceof Ellipse) {
            ((Ellipse) startNode).setCenterX(graphPane.getPrefWidth() / 2);
            ((Ellipse) startNode).setCenterY(100);
        }

        // Place end node at bottom center
        if (endNode instanceof Ellipse) {
            ((Ellipse) endNode).setCenterX(graphPane.getPrefWidth() / 2);
            ((Ellipse) endNode).setCenterY(graphPane.getPrefHeight() - 100);
        }

        // Arrange normal components in a grid
        int cols = (int) Math.ceil(Math.sqrt(normalComponents.size()));
        int rows = (int) Math.ceil((double) normalComponents.size() / cols);

        double gridWidth = graphPane.getPrefWidth() - 200;
        double gridHeight = graphPane.getPrefHeight() - 300;
        double cellWidth = gridWidth / cols;
        double cellHeight = gridHeight / rows;

        for (int i = 0; i < normalComponents.size(); i++) {
            CodeComponent comp = normalComponents.get(i);
            Shape node = nodeMap.get(comp.getId());

            if (node instanceof Rectangle) {
                int row = i / cols;
                int col = i % cols;

                double x = 100 + col * cellWidth + (cellWidth - NODE_WIDTH) / 2;
                double y = 150 + row * cellHeight + (cellHeight - NODE_HEIGHT) / 2;

                ((Rectangle) node).setX(x);
                ((Rectangle) node).setY(y);

                // Update label position
                Text label = labelMap.get(comp.getId());
                if (label != null) {
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(x + (NODE_WIDTH - textWidth) / 2);
                    label.setY(y + NODE_HEIGHT + 20);
                }

                // Update indicator position
                Circle indicator = indicatorMap.get(comp.getId());
                if (indicator != null) {
                    indicator.setCenterX(x + NODE_WIDTH - 15);
                    indicator.setCenterY(y + 15);
                }
            }
        }
    }

    private void applyLayeredLayout(ProjectAnalysisResult result) {
        // Place start node at top center
        Shape startNode = nodeMap.get(START_NODE_ID);
        if (startNode != null) {
            if (startNode instanceof Ellipse) {
                ((Ellipse) startNode).setCenterX(graphPane.getPrefWidth() / 2);
                ((Ellipse) startNode).setCenterY(100);
            }
        }

        // Place end node at bottom center
        Shape endNode = nodeMap.get(END_NODE_ID);
        if (endNode != null) {
            if (endNode instanceof Ellipse) {
                ((Ellipse) endNode).setCenterX(graphPane.getPrefWidth() / 2);
                ((Ellipse) endNode).setCenterY(graphPane.getPrefHeight() - 100);
            }
        }

        // Group by layers
        Map<String, List<CodeComponent>> layers = groupByLayers(result);
        double layerWidth = graphPane.getPrefWidth() / (layers.size() + 1);
        int layerIndex = 1;

        for (Map.Entry<String, List<CodeComponent>> entry : layers.entrySet()) {
            List<CodeComponent> components = entry.getValue();
            double x = layerIndex * layerWidth;
            double layerHeight = graphPane.getPrefHeight() - 250; // Reserve space for start/end
            double ySpacing = layerHeight / (components.size() + 1);

            for (int i = 0; i < components.size(); i++) {
                CodeComponent comp = components.get(i);
                if (isSpecialNode(comp)) continue;

                Shape node = nodeMap.get(comp.getId());
                if (node instanceof Rectangle) {
                    double y = 150 + (i + 1) * ySpacing;

                    ((Rectangle) node).setX(x - NODE_WIDTH / 2);
                    ((Rectangle) node).setY(y - NODE_HEIGHT / 2);

                    // Update label position
                    Text label = labelMap.get(comp.getId());
                    if (label != null) {
                        double textWidth = label.getLayoutBounds().getWidth();
                        label.setX(x - textWidth / 2);
                        label.setY(y + NODE_HEIGHT / 2 + 20);
                    }

                    // Update indicator position
                    Circle indicator = indicatorMap.get(comp.getId());
                    if (indicator != null) {
                        indicator.setCenterX(x + NODE_WIDTH / 2 - 15);
                        indicator.setCenterY(y - NODE_HEIGHT / 2 + 15);
                    }
                }
            }
            layerIndex++;
        }
    }

    private void applyForceDirectedLayout(ProjectAnalysisResult result, int iterations) {
        Random rand = new Random();
        Map<String, double[]> positions = new HashMap<>();
        Map<String, double[]> velocities = new HashMap<>();
        Map<String, Boolean> fixedNodes = new HashMap<>();

        double width = graphPane.getPrefWidth();
        double height = graphPane.getPrefHeight();

        // Fixed positions for start and end nodes
        positions.put(START_NODE_ID, new double[]{width / 2, 80});
        positions.put(END_NODE_ID, new double[]{width / 2, height - 120});
        fixedNodes.put(START_NODE_ID, true);
        fixedNodes.put(END_NODE_ID, true);
        velocities.put(START_NODE_ID, new double[]{0, 0});
        velocities.put(END_NODE_ID, new double[]{0, 0});

        // Initialize positions and velocities for other nodes
        for (CodeComponent comp : result.getComponents()) {
            if (isSpecialNode(comp)) continue;

            positions.put(comp.getId(), new double[]{
                    200 + rand.nextDouble() * (width - 400),
                    200 + rand.nextDouble() * (height - 400)
            });
            velocities.put(comp.getId(), new double[]{0, 0});
            fixedNodes.put(comp.getId(), false);
        }

        // Force-directed parameters
        int n = Math.max(1, result.getComponents().size());
        double k = Math.sqrt(width * height / n);
        double repulsion = k * k;
        double attraction = k;
        double damping = 0.85; // slightly stronger damping for stability
        double maxForce = 15.0;
        double epsilon = 0.0001; // avoid division by zero
        double padding = 80; // keep away from hard edges

        // Adapt iterations to graph size if caller passed a small value
        int iters = Math.max(iterations, Math.min(800, 200 + n * 4));

        for (int iter = 0; iter < iters; iter++) {
            Map<String, double[]> forces = new HashMap<>();

            // Initialize forces to zero
            for (String nodeId : positions.keySet()) {
                forces.put(nodeId, new double[]{0, 0});
            }

            // Calculate repulsive forces between all pairs of nodes
            List<String> nodeIds = new ArrayList<>(positions.keySet());
            for (int i = 0; i < nodeIds.size(); i++) {
                for (int j = i + 1; j < nodeIds.size(); j++) {
                    String node1 = nodeIds.get(i);
                    String node2 = nodeIds.get(j);

                    double[] pos1 = positions.get(node1);
                    double[] pos2 = positions.get(node2);
                    double dx = pos1[0] - pos2[0];
                    double dy = pos1[1] - pos2[1];
                    double distance = Math.max(epsilon, Math.sqrt(dx * dx + dy * dy));

                    // Repulsive force (Fruchterman-Reingold)
                    double force = repulsion / (distance * distance);
                    double fx = force * dx / distance;
                    double fy = force * dy / distance;

                    double[] f1 = forces.get(node1);
                    f1[0] += fx;
                    f1[1] += fy;
                    double[] f2 = forces.get(node2);
                    f2[0] -= fx;
                    f2[1] -= fy;
                }
            }

            // Calculate attractive forces along edges
            for (ComponentRelationship rel : result.getRelationships()) {
                String sourceId = rel.getSourceId();
                String targetId = rel.getTargetId();

                if (!positions.containsKey(sourceId) || !positions.containsKey(targetId)) continue;

                double[] pos1 = positions.get(sourceId);
                double[] pos2 = positions.get(targetId);
                double dx = pos1[0] - pos2[0];
                double dy = pos1[1] - pos2[1];
                double distance = Math.max(epsilon, Math.sqrt(dx * dx + dy * dy));

                // Attractive force
                double force = (distance * distance) / k; // standard FR attractive term
                double fx = force * dx / distance;
                double fy = force * dy / distance;

                if (!Boolean.TRUE.equals(fixedNodes.get(sourceId))) {
                    double[] f1 = forces.get(sourceId);
                    f1[0] -= fx;
                    f1[1] -= fy;
                }
                if (!Boolean.TRUE.equals(fixedNodes.get(targetId))) {
                    double[] f2 = forces.get(targetId);
                    f2[0] += fx;
                    f2[1] += fy;
                }
            }

            // Apply forces and update positions
            double maxDisp = 0;
            for (String nodeId : positions.keySet()) {
                if (Boolean.TRUE.equals(fixedNodes.get(nodeId))) continue;

                double[] force = forces.get(nodeId);
                double forceMagnitude = Math.sqrt(force[0] * force[0] + force[1] * force[1]);

                // Limit maximum force for stability
                if (forceMagnitude > maxForce) {
                    force[0] = force[0] * maxForce / forceMagnitude;
                    force[1] = force[1] * maxForce / forceMagnitude;
                }

                // Update velocity
                double[] velocity = velocities.get(nodeId);
                velocity[0] = (velocity[0] + force[0]) * damping;
                velocity[1] = (velocity[1] + force[1]) * damping;

                // Update position
                double[] position = positions.get(nodeId);
                position[0] += velocity[0];
                position[1] += velocity[1];

                maxDisp = Math.max(maxDisp, Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]));

                // Keep within bounds with padding
                position[0] = Math.max(padding, Math.min(width - padding, position[0]));
                position[1] = Math.max(padding, Math.min(height - padding, position[1]));
            }

            // Early exit if motion is very small
            if (maxDisp < 0.05) {
                break;
            }
        }

        // Collision resolution pass to ensure non-overlap and respect MIN_NODE_DISTANCE
        resolveOverlapsForCurrentNodesUsingPositions(positions, fixedNodes, width, height);

        // Apply final positions to JavaFX nodes
        for (CodeComponent comp : result.getComponents()) {
            double[] pos = positions.get(comp.getId());
            if (pos == null) continue;

            Shape node = nodeMap.get(comp.getId());
            if (node instanceof Rectangle) {
                ((Rectangle) node).setX(pos[0] - NODE_WIDTH / 2);
                ((Rectangle) node).setY(pos[1] - NODE_HEIGHT / 2);

                // Update label position
                Text label = labelMap.get(comp.getId());
                if (label != null) {
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(pos[0] - textWidth / 2);
                    label.setY(pos[1] + NODE_HEIGHT / 2 + 20);
                }

                // Update indicator position
                Circle indicator = indicatorMap.get(comp.getId());
                if (indicator != null) {
                    indicator.setCenterX(pos[0] + NODE_WIDTH / 2 - 15);
                    indicator.setCenterY(pos[1] - NODE_HEIGHT / 2 + 15);
                }
            } else if (node instanceof Ellipse) {
                ((Ellipse) node).setCenterX(pos[0]);
                ((Ellipse) node).setCenterY(pos[1]);

                // Update label position for special nodes
                Text label = labelMap.get(comp.getId());
                if (label != null) {
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(pos[0] - textWidth / 2);
                    label.setY(pos[1] + 40);
                }

                // Update indicator position for special nodes
                Circle indicator = indicatorMap.get(comp.getId());
                if (indicator != null) {
                    indicator.setCenterX(pos[0] + 25);
                    indicator.setCenterY(pos[1] - 25);
                }
            }
        }
    }

    // Utility: resolve overlaps by adjusting positions map (used by force-directed)
    private void resolveOverlapsForCurrentNodesUsingPositions(Map<String, double[]> positions,
                                                              Map<String, Boolean> fixedNodes,
                                                              double width,
                                                              double height) {
        double minDist = MIN_NODE_DISTANCE;
        double padding = 80;
        List<String> ids = new ArrayList<>(positions.keySet());

        for (int pass = 0; pass < 12; pass++) {
            boolean anyMoved = false;
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    String a = ids.get(i);
                    String b = ids.get(j);
                    // Skip if either is not a rectangle-backed node
                    Shape na = nodeMap.get(a);
                    Shape nb = nodeMap.get(b);
                    if (!(na instanceof Rectangle) || !(nb instanceof Rectangle)) continue;

                    double[] pa = positions.get(a);
                    double[] pb = positions.get(b);
                    if (pa == null || pb == null) continue;

                    double dx = pb[0] - pa[0];
                    double dy = pb[1] - pa[1];
                    double dist = Math.max(0.0001, Math.sqrt(dx * dx + dy * dy));

                    if (dist < minDist) {
                        double overlap = (minDist - dist);
                        double ux = dx / dist;
                        double uy = dy / dist;
                        double moveX = ux * overlap / 2.0;
                        double moveY = uy * overlap / 2.0;

                        if (!Boolean.TRUE.equals(fixedNodes.get(a))) {
                            pa[0] -= moveX;
                            pa[1] -= moveY;
                            pa[0] = Math.max(padding, Math.min(width - padding, pa[0]));
                            pa[1] = Math.max(padding, Math.min(height - padding, pa[1]));
                            anyMoved = true;
                        }
                        if (!Boolean.TRUE.equals(fixedNodes.get(b))) {
                            pb[0] += moveX;
                            pb[1] += moveY;
                            pb[0] = Math.max(padding, Math.min(width - padding, pb[0]));
                            pb[1] = Math.max(padding, Math.min(height - padding, pb[1]));
                            anyMoved = true;
                        }
                    }
                }
            }
            if (!anyMoved) break;
        }
    }

    // Generic de-overlap for any already-positioned rectangle nodes on the pane
    private void resolveOverlapsForCurrentNodes() {
        // Collect rectangle nodes
        List<Map.Entry<String, Shape>> items = new ArrayList<>(nodeMap.entrySet());
        double minDist = MIN_NODE_DISTANCE;
        double padding = 80;

        for (int pass = 0; pass < 10; pass++) {
            boolean moved = false;
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    String idA = items.get(i).getKey();
                    String idB = items.get(j).getKey();
                    Shape sa = items.get(i).getValue();
                    Shape sb = items.get(j).getValue();
                    if (!(sa instanceof Rectangle) || !(sb instanceof Rectangle)) continue;

                    Rectangle ra = (Rectangle) sa;
                    Rectangle rb = (Rectangle) sb;
                    double ax = ra.getX() + NODE_WIDTH / 2.0;
                    double ay = ra.getY() + NODE_HEIGHT / 2.0;
                    double bx = rb.getX() + NODE_WIDTH / 2.0;
                    double by = rb.getY() + NODE_HEIGHT / 2.0;

                    double dx = bx - ax;
                    double dy = by - ay;
                    double dist = Math.max(0.0001, Math.sqrt(dx * dx + dy * dy));

                    if (dist < minDist) {
                        double overlap = (minDist - dist);
                        double ux = dx / dist;
                        double uy = dy / dist;
                        double moveX = ux * overlap / 2.0;
                        double moveY = uy * overlap / 2.0;

                        // Move rectangles apart
                        ra.setX(Math.max(padding, Math.min(graphPane.getPrefWidth() - padding - NODE_WIDTH, ra.getX() - moveX)));
                        ra.setY(Math.max(padding, Math.min(graphPane.getPrefHeight() - padding - NODE_HEIGHT, ra.getY() - moveY)));
                        rb.setX(Math.max(padding, Math.min(graphPane.getPrefWidth() - padding - NODE_WIDTH, rb.getX() + moveX)));
                        rb.setY(Math.max(padding, Math.min(graphPane.getPrefHeight() - padding - NODE_HEIGHT, rb.getY() + moveY)));

                        // Update labels/indicators for these two
                        updateLabelAndIndicatorForNode(idA, ra.getX(), ra.getY());
                        updateLabelAndIndicatorForNode(idB, rb.getX(), rb.getY());
                        moved = true;
                    }
                }
            }
            if (!moved) break;
        }
    }

    private void updateLabelAndIndicatorForNode(String nodeId, double rectX, double rectY) {
        Text label = labelMap.get(nodeId);
        if (label != null) {
            double textWidth = label.getLayoutBounds().getWidth();
            label.setX(rectX + NODE_WIDTH / 2.0 - textWidth / 2.0);
            label.setY(rectY + NODE_HEIGHT + 20);
        }
        Circle indicator = indicatorMap.get(nodeId);
        if (indicator != null) {
            indicator.setCenterX(rectX + NODE_WIDTH - 15);
            indicator.setCenterY(rectY + 15);
        }
    }

    public void setLayoutType(LayoutType layoutType) {
        this.currentLayout = layoutType;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    //

    private void applyUserFlowLayout(List<UserFlowComponent> userFlows) {
        // Clearer, level-based left-to-right layout derived from navigation paths (BFS from Start)
        double leftMargin = 120;
        double topMargin = 140;
        double spacingX = 320;
        double spacingY = 220;

        // Index flows by id
        Map<String, UserFlowComponent> byId = new HashMap<>();
        for (UserFlowComponent f : userFlows) {
            byId.put(f.getId(), f);
        }

        // Build adjacency from outgoing paths
        Map<String, List<String>> adj = new HashMap<>();
        for (UserFlowComponent f : userFlows) {
            List<String> outs = new ArrayList<>();
            if (f.getOutgoingPaths() != null) {
                for (NavigationPath p : f.getOutgoingPaths()) {
                    NavigationFlow nf = p.getNavigationFlow();
                    if (nf != null && nf.getTargetScreenId() != null) {
                        outs.add(nf.getTargetScreenId());
                    }
                }
            }
            adj.put(f.getId(), outs);
        }

        // Compute levels with BFS from Start node (fallback to first element)
        String startId = byId.containsKey(START_NODE_ID) ? START_NODE_ID : (userFlows.isEmpty() ? null : userFlows.get(0).getId());
        Map<String, Integer> level = new HashMap<>();
        if (startId != null) {
            Deque<String> dq = new ArrayDeque<>();
            dq.add(startId);
            level.put(startId, 0);
            while (!dq.isEmpty()) {
                String u = dq.removeFirst();
                int lu = level.get(u);
                List<String> outs = adj.getOrDefault(u, Collections.emptyList());
                for (String v : outs) {
                    if (!level.containsKey(v)) {
                        level.put(v, lu + 1);
                        dq.addLast(v);
                    } else {
                        // keep the smallest level for clarity
                        level.put(v, Math.min(level.get(v), lu + 1));
                    }
                }
            }
        }

        // Any unvisited nodes: assign them after max level
        int maxLevel = level.values().stream().mapToInt(i -> i).max().orElse(0);
        for (UserFlowComponent f : userFlows) {
            level.putIfAbsent(f.getId(), maxLevel + 1);
        }

        // Ensure End is at the last level
        if (byId.containsKey(END_NODE_ID)) {
            int newMax = level.values().stream().mapToInt(i -> i).max().orElse(0);
            level.put(END_NODE_ID, newMax);
        }

        // Group by level
        Map<Integer, List<UserFlowComponent>> byLevel = new TreeMap<>();
        for (UserFlowComponent f : userFlows) {
            int lv = level.getOrDefault(f.getId(), 0);
            byLevel.computeIfAbsent(lv, k -> new ArrayList<>()).add(f);
        }

        // Layout nodes column by column
        for (Map.Entry<Integer, List<UserFlowComponent>> entry : byLevel.entrySet()) {
            int lv = entry.getKey();
            List<UserFlowComponent> col = entry.getValue();

            // Sort for stability: Entry/Decision/Main/Exit
            col.sort(Comparator.comparing((UserFlowComponent f) -> f.getFlowType() == UserFlowComponent.FlowType.ENTRY_POINT ? 0 :
                    f.getFlowType() == UserFlowComponent.FlowType.DECISION_POINT ? 1 :
                    f.getFlowType() == UserFlowComponent.FlowType.MAIN_FLOW ? 2 : 3)
                    .thenComparing(UserFlowComponent::getScreenName, Comparator.nullsLast(String::compareToIgnoreCase)));

            for (int i = 0; i < col.size(); i++) {
                UserFlowComponent f = col.get(i);
                Shape node = nodeMap.get(f.getId());
                if (!(node instanceof Rectangle)) continue;

                double x = leftMargin + lv * spacingX;
                double y = topMargin + i * spacingY;
                ((Rectangle) node).setX(x);
                ((Rectangle) node).setY(y);

                // Label position under node
                Text label = labelMap.get(f.getId());
                if (label != null) {
                    double nodeWidth = NODE_WIDTH + 40;
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(x + nodeWidth / 2 - textWidth / 2);
                    label.setY(y + (NODE_HEIGHT + 30) + 20);
                }
            }
        }
    }

    private void categorizeUserFlowTypes(List<UserFlowComponent> userFlows, ProjectAnalysisResult result) {
        // Simple categorization based on naming patterns
        for (UserFlowComponent flow : userFlows) {
            String name = flow.getScreenName().toLowerCase();

            if (name.contains("main") || name.contains("home") || name.contains("splash")) {
                flow.setFlowType(UserFlowComponent.FlowType.ENTRY_POINT);
            } else if (name.contains("error") || name.contains("exception")) {
                flow.setFlowType(UserFlowComponent.FlowType.ERROR_HANDLING);
            } else if (flow.getOutgoingPaths().size() > 1) {
                flow.setFlowType(UserFlowComponent.FlowType.DECISION_POINT);
            } else if (flow.getOutgoingPaths().isEmpty()) {
                flow.setFlowType(UserFlowComponent.FlowType.EXIT_POINT);
            } else {
                flow.setFlowType(UserFlowComponent.FlowType.MAIN_FLOW);
            }
        }
    }

    // 3. BUSINESS PROCESS METHODS (Simplified implementations)
    private List<BusinessProcessComponent> extractBusinessProcesses(ProjectAnalysisResult result) {
        List<BusinessProcessComponent> processes = new ArrayList<>();

        // Create a sample business process for now
        BusinessProcessComponent authProcess = new BusinessProcessComponent();
        authProcess.setProcessId("auth_process");
        authProcess.setProcessName("User Authentication");
        authProcess.setProcessType(BusinessProcessComponent.ProcessType.AUTHENTICATION);
        authProcess.setCriticalityLevel(BusinessProcessComponent.CriticalityLevel.CRITICAL);

        processes.add(authProcess);
        return processes;
    }

    private void createBusinessProcessNodes(List<BusinessProcessComponent> processes) {
        for (BusinessProcessComponent process : processes) {
            Rectangle node = new Rectangle(NODE_WIDTH, NODE_HEIGHT);
            node.setFill(Color.web("#f59e0b")); // Yellow for processes
            node.setArcWidth(15);
            node.setArcHeight(15);

            nodeMap.put(process.getProcessId(), node);
            graphPane.getChildren().add(node);

            // Label
            if (showLabels) {
                Text label = new Text(process.getProcessName());
                label.setFont(Font.font("System", FontWeight.BOLD, 13));
                label.setFill(Color.BLACK);
                labelMap.put(process.getProcessId(), label);
                graphPane.getChildren().add(label);
            }
        }
    }

    private void createBusinessProcessEdges(List<BusinessProcessComponent> processes) {
        // Connect processes sequentially for visualization
        for (int i = 0; i < processes.size() - 1; i++) {
            BusinessProcessComponent a = processes.get(i);
            BusinessProcessComponent b = processes.get(i + 1);
            Shape sourceNode = nodeMap.get(a.getProcessId());
            Shape targetNode = nodeMap.get(b.getProcessId());
            if (sourceNode != null && targetNode != null) {
                Path edge = createCurvedEdge(sourceNode, targetNode, "PROCESS_FLOW");
                edges.add(edge);
                graphPane.getChildren().add(edge);
                Polygon arrowhead = createArrowhead(edge);
                arrowheads.add(arrowhead);
                graphPane.getChildren().add(arrowhead);
            }
        }
    }

    private void applyBusinessProcessLayout(List<BusinessProcessComponent> processes) {
        // Simple vertical layout
        double startX = 200;
        double startY = 100;
        double spacingY = 200;

        for (int i = 0; i < processes.size(); i++) {
            BusinessProcessComponent process = processes.get(i);
            Shape node = nodeMap.get(process.getProcessId());

            if (node instanceof Rectangle) {
                double x = startX;
                double y = startY + (i * spacingY);
                ((Rectangle) node).setX(x);
                ((Rectangle) node).setY(y);

                // Label position
                Text label = labelMap.get(process.getProcessId());
                if (label != null) {
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(x + NODE_WIDTH / 2 - textWidth / 2);
                    label.setY(y + NODE_HEIGHT + 18);
                }
            }
        }
    }

    // 4. FEATURE GROUP METHODS (Simplified implementations)
    private List<FeatureGroup> groupComponentsByFeature(ProjectAnalysisResult result) {
        List<FeatureGroup> groups = new ArrayList<>();

        // Create sample feature groups
        FeatureGroup authFeature = new FeatureGroup();
        authFeature.setGroupId("auth_feature");
        authFeature.setFeatureName("Authentication");
        groups.add(authFeature);

        return groups;
    }

    private void createFeatureGroupNodes(List<FeatureGroup> groups) {
        for (FeatureGroup group : groups) {
            Rectangle node = new Rectangle(NODE_WIDTH + 50, NODE_HEIGHT + 50);
            node.setFill(Color.web("#8b5cf6")); // Purple for feature groups
            node.setArcWidth(20);
            node.setArcHeight(20);

            nodeMap.put(group.getGroupId(), node);
            graphPane.getChildren().add(node);

            // Label
            if (showLabels) {
                String name = group.getFeatureName() != null ? group.getFeatureName() : group.getGroupId();
                Text label = new Text(name);
                label.setFont(Font.font("System", FontWeight.BOLD, 13));
                label.setFill(Color.BLACK);
                labelMap.put(group.getGroupId(), label);
                graphPane.getChildren().add(label);
            }
        }
    }

    private void createFeatureGroupEdges(List<FeatureGroup> groups) {
        // Connect groups sequentially to visualize relationships
        for (int i = 0; i < groups.size() - 1; i++) {
            FeatureGroup a = groups.get(i);
            FeatureGroup b = groups.get(i + 1);
            Shape sourceNode = nodeMap.get(a.getGroupId());
            Shape targetNode = nodeMap.get(b.getGroupId());
            if (sourceNode != null && targetNode != null) {
                Path edge = createCurvedEdge(sourceNode, targetNode, "FEATURE_LINK");
                edges.add(edge);
                graphPane.getChildren().add(edge);
                Polygon arrowhead = createArrowhead(edge);
                arrowheads.add(arrowhead);
                graphPane.getChildren().add(arrowhead);
            }
        }
    }

    private void applyFeatureGroupLayout(List<FeatureGroup> groups) {
        // Simple grid layout
        int cols = 3;
        double startX = 150;
        double startY = 150;
        double spacingX = 300;
        double spacingY = 250;

        for (int i = 0; i < groups.size(); i++) {
            FeatureGroup group = groups.get(i);
            Shape node = nodeMap.get(group.getGroupId());

            if (node instanceof Rectangle) {
                int row = i / cols;
                int col = i % cols;
                double x = startX + (col * spacingX);
                double y = startY + (row * spacingY);
                ((Rectangle) node).setX(x);
                ((Rectangle) node).setY(y);

                // Label position
                Text label = labelMap.get(group.getGroupId());
                if (label != null) {
                    double nodeWidth = NODE_WIDTH + 50;
                    double textWidth = label.getLayoutBounds().getWidth();
                    label.setX(x + nodeWidth / 2 - textWidth / 2);
                    label.setY(y + (NODE_HEIGHT + 50) + 18);
                }
            }
        }
    }

    // 5. INTEGRATION MAP METHOD (Simplified)
    private ScrollPane createIntegrationMapView(ProjectAnalysisResult result) {
        // Ensure canvas is properly sized and optional grid is shown
        calculateDynamicCanvasSize(result);
        if (showGrid) {
            createBackgroundGrid();
        }
        // Fallback to technical architecture view for now
        return createTechnicalArchitectureView(result);
    }

    // 6. DIALOG AND CONTEXT METHODS
    private void showFlowDetailsDialog(UserFlowComponent flow) {
        // Simple console output for now
        System.out.println("Flow Details: " + flow.getScreenName());
        System.out.println("Business Goal: " +
                (flow.getBusinessContext() != null ? flow.getBusinessContext().getBusinessGoal() : "Unknown"));
    }

    private void displayFlowContext(UserFlowComponent flow) {
        // Simple console output for now
        System.out.println("Flow Context: " + flow.getScreenName());
    }

    // 7. UTILITY METHOD
    private String extractTargetFromMethodName(String methodName) {
        // Simple extraction - remove common prefixes
        return methodName.replace("onclick", "").replace("ontouch", "").replace("on", "");
    }

    private void createNavigationFlowEdges(List<UserFlowComponent> userFlows) {
        for (UserFlowComponent flow : userFlows) {
            if (flow.getOutgoingPaths() != null) {
                for (NavigationPath path : flow.getOutgoingPaths()) {
                    NavigationFlow navFlow = path.getNavigationFlow();
                    if (navFlow != null) {
                        Shape sourceNode = nodeMap.get(navFlow.getSourceScreenId());
                        Shape targetNode = nodeMap.get(navFlow.getTargetScreenId());

                        if (sourceNode != null && targetNode != null) {
                            Path edge = createCurvedEdge(sourceNode, targetNode, "NAVIGATION");
                            edges.add(edge);
                            graphPane.getChildren().add(edge);

                            Polygon arrowhead = createArrowhead(edge);
                            arrowheads.add(arrowhead);
                            graphPane.getChildren().add(arrowhead);
                        }
                    }
                }
            }
        }
    }

    private void addNavigationFlowConnections(ProjectAnalysisResult result, List<NavigationFlow> navigationFlows) {
        // Handle navigation flow connections separately
        for (NavigationFlow navFlow : navigationFlows) {
            // Don't add these to ComponentRelationship list
            // Handle them separately in the user journey view
        }
    }

    private void addIntelligentConnections(ProjectAnalysisResult result) {
        Set<String> entryPoints = findEntryPoints(result);
        Set<String> exitPoints = findExitPoints(result);

        // Connect start to entry points - ONLY ComponentRelationship
        for (String entryId : entryPoints) {
            ComponentRelationship rel = new ComponentRelationship();
            rel.setSourceId(START_NODE_ID);
            rel.setTargetId(entryId);
            rel.setType("STARTS");
            result.addRelationship(rel);  // This expects ComponentRelationship
        }

        // Connect exit points to end - ONLY ComponentRelationship
        for (String exitId : exitPoints) {
            ComponentRelationship rel = new ComponentRelationship();
            rel.setSourceId(exitId);
            rel.setTargetId(END_NODE_ID);
            rel.setType("TERMINATES");
            result.addRelationship(rel);  // This expects ComponentRelationship
        }
    }

    // Adds a simple legend panel for the User Journey view to improve readability for new readers
    private void addUserJourneyLegend() {
        try {
            VBox legend = LegendFactory.createUserJourneyLegend(
                    Color.web("#10b981"), // Entry
                    Color.web("#3b82f6"), // Main
                    Color.web("#f59e0b"), // Decision
                    Color.web("#ef4444")  // Exit
            );
            legend.setMouseTransparent(true);
            legend.setPickOnBounds(false);
            this.lastLegend = legend;
        } catch (Exception ignored) {
        }
    }

    // Adds a legend for the Technical Architecture view mapping node colors to layers
    private void addTechnicalArchitectureLegend() {
        try {
            VBox legend = LegendFactory.createTechnicalArchitectureLegend(UI_COLOR, BUSINESS_COLOR, DATA_COLOR, OTHER_COLOR);
            legend.setMouseTransparent(true);
            legend.setPickOnBounds(false);
            this.lastLegend = legend;
        } catch (Exception ignored) {
        }
    }

    // Adds a legend for the Business Process view
    private void addBusinessProcessLegend() {
        try {
            VBox legend = LegendFactory.createBusinessProcessLegend(WARNING, INFO);
            legend.setMouseTransparent(true);
            legend.setPickOnBounds(false);
            this.lastLegend = legend;
        } catch (Exception ignored) {
        }
    }

    // Adds a legend for the Feature Overview view
    private void addFeatureOverviewLegend() {
        try {
            VBox legend = LegendFactory.createFeatureOverviewLegend(PURPLE, INFO);
            legend.setMouseTransparent(true);
            legend.setPickOnBounds(false);
            this.lastLegend = legend;
        } catch (Exception ignored) {
        }
    }

    public VBox getLegend() {
        return lastLegend;
    }
}
