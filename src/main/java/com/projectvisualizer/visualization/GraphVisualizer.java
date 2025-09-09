package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class GraphVisualizer {
    private static final double NODE_WIDTH = 140;
    private static final double NODE_HEIGHT = 70;
    private static final double SPECIAL_NODE_WIDTH = 100;
    private static final double SPECIAL_NODE_HEIGHT = 50;
    private static final double HORIZONTAL_SPACING = 250;
    private static final double VERTICAL_SPACING = 180;
    private static final double ARROW_SIZE = 12;

    private static final Color UI_LAYER_COLOR = Color.valueOf("#3b82f6"); // Blue
    private static final Color BUSINESS_LOGIC_COLOR = Color.valueOf("#10b981"); // Green
    private static final Color DATA_LAYER_COLOR = Color.valueOf("#ef4444"); // Red
    private static final Color OTHER_LAYER_COLOR = Color.valueOf("#6b7280"); // Gray

    // Special node IDs
    private static final String START_NODE_ID = "_START_NODE_";
    private static final String END_NODE_ID = "_END_NODE_";

    private Map<String, Rectangle> nodeMap = new HashMap<>();
    private Map<String, Ellipse> specialNodeMap = new HashMap<>();
    private Map<String, Text> labelMap = new HashMap<>();
    private Map<String, List<ComponentRelationship>> relationshipMap = new HashMap<>();
    private Map<Line, ComponentRelationship> edgeRelationshipMap = new HashMap<>();
    private Pane graphPane;
    private List<Line> edges = new ArrayList<>();
    private List<Polygon> arrowheads = new ArrayList<>();
    private double currentZoom = 1.0;

    public ScrollPane createGraphView(ProjectAnalysisResult result) {
        graphPane = new Pane();
        edges.clear();
        arrowheads.clear();

        // Enhance the result with Start/End nodes
        ProjectAnalysisResult enhancedResult = enhanceWithStartEndNodes(result);

        groupComponentsByLayer(result, graphPane);

        // Calculate required canvas size based on number of components (including special nodes)
        int componentCount = enhancedResult.getComponents().size();
        int rows = (int) Math.ceil(Math.sqrt(componentCount));
        int cols = (int) Math.ceil((double) componentCount / rows);

        double canvasWidth = cols * (NODE_WIDTH + HORIZONTAL_SPACING) + 200;
        double canvasHeight = rows * (NODE_HEIGHT + VERTICAL_SPACING) + 200;

        graphPane.setPrefSize(canvasWidth, canvasHeight);
        graphPane.setMinSize(canvasWidth, canvasHeight);

        // Build relationship map for quick lookup
        buildRelationshipMap(enhancedResult);

        // Create nodes for each component using a hierarchical layout
        createHierarchicalNodes(enhancedResult, graphPane);

        // Create edges for relationships
        createEdges(enhancedResult, graphPane);

        // Add tooltips and interactions
        addInteractions(enhancedResult, graphPane);

        // Create a scroll pane with zoom functionality
        ScrollPane scrollPane = new ScrollPane(graphPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        // Add zoom handlers
        setupZoomHandlers(scrollPane);

        return scrollPane;
    }

    private void groupComponentsByLayer(ProjectAnalysisResult result, Pane pane) {
        // Separate components by layer
        Map<String, List<CodeComponent>> componentsByLayer = new HashMap<>();
        componentsByLayer.put("UI", new ArrayList<>());
        componentsByLayer.put("Business Logic", new ArrayList<>());
        componentsByLayer.put("Data", new ArrayList<>());
        componentsByLayer.put("Other", new ArrayList<>());

        for (CodeComponent component : result.getComponents()) {
            String layer = component.getLayer();
            if (layer == null || !componentsByLayer.containsKey(layer)) {
                componentsByLayer.get("Other").add(component);
            } else {
                componentsByLayer.get(layer).add(component);
            }
        }

        // Position components by layer
        double startX = 150;
        double startY = 100;
        double layerSpacing = 300;

        // UI Layer
        positionComponentsInGrid(componentsByLayer.get("UI"), startX, startY, pane, UI_LAYER_COLOR);

        // Business Logic Layer
        positionComponentsInGrid(componentsByLayer.get("Business Logic"), startX + layerSpacing, startY, pane, BUSINESS_LOGIC_COLOR);

        // Data Layer
        positionComponentsInGrid(componentsByLayer.get("Data"), startX + layerSpacing * 2, startY, pane, DATA_LAYER_COLOR);

        // Other Components
        positionComponentsInGrid(componentsByLayer.get("Other"), startX + layerSpacing * 3, startY, pane, OTHER_LAYER_COLOR);

        // Add layer labels
        addLayerLabel("UI Layer", startX + NODE_WIDTH/2, startY - 40, pane, UI_LAYER_COLOR);
        addLayerLabel("Business Logic", startX + layerSpacing + NODE_WIDTH/2, startY - 40, pane, BUSINESS_LOGIC_COLOR);
        addLayerLabel("Data Layer", startX + layerSpacing * 2 + NODE_WIDTH/2, startY - 40, pane, DATA_LAYER_COLOR);
        addLayerLabel("Other", startX + layerSpacing * 3 + NODE_WIDTH/2, startY - 40, pane, OTHER_LAYER_COLOR);
    }

    private void positionComponentsInGrid(List<CodeComponent> components, double startX, double startY, Pane pane, Color layerColor) {
        if (components.isEmpty()) return;

        int cols = (int) Math.ceil(Math.sqrt(components.size()));
        int rows = (int) Math.ceil((double) components.size() / cols);

        for (int i = 0; i < components.size(); i++) {
            CodeComponent component = components.get(i);
            int row = i / cols;
            int col = i % cols;

            double posX = startX + col * (NODE_WIDTH + HORIZONTAL_SPACING/2);
            double posY = startY + row * (NODE_HEIGHT + VERTICAL_SPACING/2);

            createRegularNode(component, posX, posY, pane, layerColor);
        }
    }

    private void addLayerLabel(String text, double x, double y, Pane pane, Color color) {
        Text label = new Text(x, y, text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-fill: " + toHexColor(color) + ";");
        label.setTextOrigin(javafx.geometry.VPos.CENTER);
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Center the text
        label.setX(x - label.getBoundsInLocal().getWidth() / 2);

        pane.getChildren().add(label);
    }

    private String toHexColor(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private ProjectAnalysisResult enhanceWithStartEndNodes(ProjectAnalysisResult original) {
        // Create a copy of the original result
        ProjectAnalysisResult enhanced = new ProjectAnalysisResult();
        enhanced.setProjectName(original.getProjectName());
        enhanced.setProjectPath(original.getProjectPath());
        enhanced.setComponents(new ArrayList<>(original.getComponents()));
        enhanced.setRelationships(new ArrayList<>(original.getRelationships()));

        // Identify entry points and terminal nodes
        Set<String> entryPoints = identifyEntryPoints(original);
        Set<String> terminalNodes = identifyTerminalNodes(original);

        // Create Start node
        CodeComponent startNode = new CodeComponent();
        startNode.setId(START_NODE_ID);
        startNode.setName("Start");
        startNode.setType("start");
        startNode.setLanguage("system");
        enhanced.addComponent(startNode);

        // Create End node
        CodeComponent endNode = new CodeComponent();
        endNode.setId(END_NODE_ID);
        endNode.setName("End");
        endNode.setType("end");
        endNode.setLanguage("system");
        enhanced.addComponent(endNode);

        // Connect Start node to entry points
        for (String entryPointId : entryPoints) {
            ComponentRelationship startRelation = new ComponentRelationship();
            startRelation.setSourceId(START_NODE_ID);
            startRelation.setTargetId(entryPointId);
            startRelation.setType("STARTS");
            enhanced.addRelationship(startRelation);
        }

        // Connect terminal nodes to End node
        for (String terminalId : terminalNodes) {
            ComponentRelationship endRelation = new ComponentRelationship();
            endRelation.setSourceId(terminalId);
            endRelation.setTargetId(END_NODE_ID);
            endRelation.setType("TERMINATES");
            enhanced.addRelationship(endRelation);
        }

        return enhanced;
    }

    private Set<String> identifyEntryPoints(ProjectAnalysisResult result) {
        Set<String> entryPoints = new HashSet<>();

        for (CodeComponent component : result.getComponents()) {
            String name = component.getName().toLowerCase();
            String type = component.getType().toLowerCase();

            // Common entry point patterns
            if (name.contains("main") && (type.equals("class") || type.equals("interface"))) {
                entryPoints.add(component.getId());
            }
            // Android Activities
            else if (name.contains("mainactivity") || name.contains("launchactivity") ||
                    name.contains("splashactivity") || name.endsWith("activity") &&
                    component.getExtendsClass() != null &&
                    component.getExtendsClass().contains("Activity")) {
                entryPoints.add(component.getId());
            }
            // Flutter main widgets
            else if (name.contains("myapp") || name.contains("app") && type.equals("widget") ||
                    component.getExtendsClass() != null &&
                            component.getExtendsClass().contains("MaterialApp")) {
                entryPoints.add(component.getId());
            }
            // React/JavaScript entry components
            else if (name.contains("app") && (type.equals("component") || type.equals("class")) ||
                    name.contains("index") && type.equals("component")) {
                entryPoints.add(component.getId());
            }
            // Spring Boot main classes
            else if (component.getAnnotations().stream().anyMatch(ann ->
                    ann.contains("SpringBootApplication") || ann.contains("Configuration"))) {
                entryPoints.add(component.getId());
            }
            // Classes with main methods (detected by checking if they have main method)
            else if (component.getMethods().stream().anyMatch(method ->
                    method.getName().equals("main") && method.getVisibility().equals("public"))) {
                entryPoints.add(component.getId());
            }
        }

        // If no clear entry points found, find components that are not dependencies of others
        if (entryPoints.isEmpty()) {
            Set<String> allTargets = new HashSet<>();
            for (ComponentRelationship rel : result.getRelationships()) {
                allTargets.add(rel.getTargetId());
            }

            for (CodeComponent component : result.getComponents()) {
                if (!allTargets.contains(component.getId())) {
                    entryPoints.add(component.getId());
                }
            }
        }

        // If still empty, use the first component
        if (entryPoints.isEmpty() && !result.getComponents().isEmpty()) {
            entryPoints.add(result.getComponents().get(0).getId());
        }

        return entryPoints;
    }

    private Set<String> identifyTerminalNodes(ProjectAnalysisResult result) {
        Set<String> terminalNodes = new HashSet<>();
        Set<String> allSources = new HashSet<>();

        // Collect all source IDs
        for (ComponentRelationship rel : result.getRelationships()) {
            allSources.add(rel.getSourceId());
        }

        // Find components that don't point to anything else (leaf nodes)
        for (CodeComponent component : result.getComponents()) {
            String name = component.getName().toLowerCase();
            String type = component.getType().toLowerCase();

            // Skip if this component has outgoing relationships
            boolean hasOutgoing = result.getRelationships().stream()
                    .anyMatch(rel -> rel.getSourceId().equals(component.getId()));

            if (!hasOutgoing) {
                // Prefer certain types as terminal nodes
                if (name.contains("service") || name.contains("repository") ||
                        name.contains("dao") || name.contains("util") ||
                        type.equals("interface") && name.contains("repository")) {
                    terminalNodes.add(component.getId());
                }
                // Or any leaf node if no service-like components
                else {
                    terminalNodes.add(component.getId());
                }
            }
        }

        // If no terminal nodes found, use components that are only targets
        if (terminalNodes.isEmpty()) {
            Set<String> onlyTargets = new HashSet<>();
            for (ComponentRelationship rel : result.getRelationships()) {
                onlyTargets.add(rel.getTargetId());
            }
            onlyTargets.removeAll(allSources);
            terminalNodes.addAll(onlyTargets);
        }

        // If still empty, use the last component
        if (terminalNodes.isEmpty() && !result.getComponents().isEmpty()) {
            terminalNodes.add(result.getComponents().get(result.getComponents().size() - 1).getId());
        }

        return terminalNodes;
    }

    private void createHierarchicalNodes(ProjectAnalysisResult result, Pane pane) {
        List<CodeComponent> components = result.getComponents();

        // Separate special nodes from regular components
        CodeComponent startNode = null;
        CodeComponent endNode = null;
        List<CodeComponent> regularComponents = new ArrayList<>();

        for (CodeComponent component : components) {
            if (START_NODE_ID.equals(component.getId())) {
                startNode = component;
            } else if (END_NODE_ID.equals(component.getId())) {
                endNode = component;
            } else {
                regularComponents.add(component);
            }
        }

        // Calculate layout parameters
        int regularCount = regularComponents.size();
        int cols = (int) Math.ceil(Math.sqrt(regularCount));
        int rows = (int) Math.ceil((double) regularCount / cols);

        double startX = 150;
        double startY = 100;

        // Position Start node at the top center
        if (startNode != null) {
            double startNodeX = startX + (cols * (NODE_WIDTH + HORIZONTAL_SPACING)) / 2 - SPECIAL_NODE_WIDTH / 2;
            double startNodeY = startY - VERTICAL_SPACING;
            createSpecialNode(startNode, startNodeX, startNodeY, pane, Color.valueOf("#10b981"));
        }

        // Position regular components in a grid
        for (int i = 0; i < regularComponents.size(); i++) {
            CodeComponent component = regularComponents.get(i);
            int row = i / cols;
            int col = i % cols;

            double posX = startX + col * (NODE_WIDTH + HORIZONTAL_SPACING);
            double posY = startY + row * (NODE_HEIGHT + VERTICAL_SPACING);

            // Determine layer color for the component
            Color componentLayerColor = getLayerColorForComponent(component);
            createRegularNode(component, posX, posY, pane, componentLayerColor);
        }

        // Position End node at the bottom center
        if (endNode != null) {
            double endNodeX = startX + (cols * (NODE_WIDTH + HORIZONTAL_SPACING)) / 2 - SPECIAL_NODE_WIDTH / 2;
            double endNodeY = startY + rows * (NODE_HEIGHT + VERTICAL_SPACING) + VERTICAL_SPACING;
            createSpecialNode(endNode, endNodeX, endNodeY, pane, Color.valueOf("#ef4444"));
        }
    }

    private Color getLayerColorForComponent(CodeComponent component) {
        String layer = component.getLayer();
        if (layer != null) {
            switch (layer) {
                case "UI":
                    return UI_LAYER_COLOR;
                case "Business Logic":
                    return BUSINESS_LOGIC_COLOR;
                case "Data":
                    return DATA_LAYER_COLOR;
                default:
                    return OTHER_LAYER_COLOR;
            }
        }
        // If no layer is defined, use type-based coloring
        return getColorForType(component.getType());
    }

    private void createSpecialNode(CodeComponent component, double x, double y, Pane pane, Color color) {
        // Create elliptical shape for special nodes
        Ellipse node = new Ellipse(x + SPECIAL_NODE_WIDTH / 2, y + SPECIAL_NODE_HEIGHT / 2,
                SPECIAL_NODE_WIDTH / 2, SPECIAL_NODE_HEIGHT / 2);
        node.setFill(color);
        node.setStroke(Color.WHITE);
        node.setStrokeWidth(2);

        // Create label
        Text label = new Text(x + SPECIAL_NODE_WIDTH / 2, y + SPECIAL_NODE_HEIGHT / 2 + 5, component.getName());
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-fill: white;");
        label.setTextOrigin(javafx.geometry.VPos.CENTER);
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Center the text
        label.setX(x + SPECIAL_NODE_WIDTH / 2 - label.getBoundsInLocal().getWidth() / 2);

        // Store references
        specialNodeMap.put(component.getId(), node);
        labelMap.put(component.getId(), label);

        // Add to pane
        pane.getChildren().addAll(node, label);
    }

    private void createRegularNode(CodeComponent component, double x, double y, Pane pane, Color layerColor) {
        // Create node shape based on type
        Rectangle node = createNodeShape(component, x, y, layerColor);

        // Create label
        Text label = new Text(x + 10, y + 35, getDisplayName(component));
        label.setWrappingWidth(NODE_WIDTH - 20);
        label.setStyle("-fx-font-weight: 600; -fx-font-size: 12;");

        // Create language indicator
        Circle langIndicator = new Circle(x + NODE_WIDTH - 15, y + 15, 8);
        langIndicator.setFill(getColorForLanguage(component.getLanguage()));
        langIndicator.setStroke(Color.WHITE);
        langIndicator.setStrokeWidth(1.5);

        // Store references
        nodeMap.put(component.getId(), node);
        labelMap.put(component.getId(), label);

        // Fixed: Changed 'rect' to 'node'
        node.getProperties().put("componentId", component.getId());
        label.getProperties().put("componentId", component.getId());
        langIndicator.getProperties().put("componentId", component.getId());

        // Add to pane
        pane.getChildren().addAll(node, label, langIndicator);
    }

    private Rectangle createNodeShape(CodeComponent component, double x, double y, Color layerColor) {
        Rectangle node = new Rectangle(x, y, NODE_WIDTH, NODE_HEIGHT);

        // Use layer color if specified, otherwise fall back to type-based coloring
        if (layerColor != null) {
            node.setFill(layerColor);
        } else {
            node.setFill(getColorForType(component.getType()));
        }

        node.setStroke(Color.WHITE);
        node.setStrokeWidth(1.5);
        node.setArcWidth(12);
        node.setArcHeight(12);

        return node;
    }

    private void createEdges(ProjectAnalysisResult result, Pane pane) {
        for (ComponentRelationship relationship : result.getRelationships()) {
            createEdgeForRelationship(relationship, pane);
        }

        // Ensure all nodes are above edges
        for (Rectangle node : nodeMap.values()) {
            node.toFront();
        }

        for (Ellipse node : specialNodeMap.values()) {
            node.toFront();
        }

        // Ensure all labels are above nodes
        for (Text label : labelMap.values()) {
            label.toFront();
        }

        // Ensure all arrowheads are above everything
        for (Polygon arrowhead : arrowheads) {
            arrowhead.toFront();
        }
    }

    private void createEdgeForRelationship(ComponentRelationship relationship, Pane pane) {
        double[] sourcePoint = getNodeConnectionPoint(relationship.getSourceId(), relationship.getTargetId());
        double[] targetPoint = getNodeConnectionPoint(relationship.getTargetId(), relationship.getSourceId());

        if (sourcePoint != null && targetPoint != null) {
            // Create a curved path
            List<double[]> pathPoints = calculateCurvedPath(
                    sourcePoint[0], sourcePoint[1],
                    targetPoint[0], targetPoint[1]
            );

            // Create the path
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                double[] start = pathPoints.get(i);
                double[] end = pathPoints.get(i + 1);

                Line edge = new Line(start[0], start[1], end[0], end[1]);

                // Style based on relationship type
                styleEdge(edge, relationship.getType());

                // Store relationship for this edge
                edgeRelationshipMap.put(edge, relationship);

                // Add arrowhead to the last segment
                if (i == pathPoints.size() - 2) {
                    Polygon arrowhead = addArrowhead(edge, start[0], start[1], end[0], end[1]);
                    arrowheads.add(arrowhead);
                    pane.getChildren().add(arrowhead);
                }

                edges.add(edge);
                pane.getChildren().add(edge);
            }
        }
    }

    private double[] getNodeConnectionPoint(String nodeId, String otherNodeId) {
        // Handle special nodes
        if (specialNodeMap.containsKey(nodeId)) {
            Ellipse node = specialNodeMap.get(nodeId);
            return calculateEllipseConnectionPoint(node, getNodeCenter(otherNodeId));
        }

        // Handle regular nodes
        if (nodeMap.containsKey(nodeId)) {
            Rectangle node = nodeMap.get(nodeId);
            double[] otherCenter = getNodeCenter(otherNodeId);
            if (otherCenter != null) {
                return calculateConnectionPoint(node, otherCenter);
            }
        }

        return null;
    }

    private double[] getNodeCenter(String nodeId) {
        if (specialNodeMap.containsKey(nodeId)) {
            Ellipse node = specialNodeMap.get(nodeId);
            return new double[]{node.getCenterX(), node.getCenterY()};
        }

        if (nodeMap.containsKey(nodeId)) {
            Rectangle node = nodeMap.get(nodeId);
            return new double[]{node.getX() + NODE_WIDTH / 2, node.getY() + NODE_HEIGHT / 2};
        }

        return null;
    }

    private double[] calculateEllipseConnectionPoint(Ellipse ellipse, double[] targetCenter) {
        if (targetCenter == null) return null;

        double centerX = ellipse.getCenterX();
        double centerY = ellipse.getCenterY();
        double radiusX = ellipse.getRadiusX();
        double radiusY = ellipse.getRadiusY();

        // Calculate angle to target
        double dx = targetCenter[0] - centerX;
        double dy = targetCenter[1] - centerY;
        double angle = Math.atan2(dy, dx);

        // Calculate point on ellipse boundary
        double x = centerX + radiusX * Math.cos(angle);
        double y = centerY + radiusY * Math.sin(angle);

        return new double[]{x, y};
    }

    private double[] calculateConnectionPoint(Rectangle node, double[] targetCenter) {
        double centerX = node.getX() + NODE_WIDTH / 2;
        double centerY = node.getY() + NODE_HEIGHT / 2;

        // Calculate angle to target
        double dx = targetCenter[0] - centerX;
        double dy = targetCenter[1] - centerY;
        double angle = Math.atan2(dy, dx);

        // Calculate intersection with rectangle boundary
        double halfWidth = NODE_WIDTH / 2;
        double halfHeight = NODE_HEIGHT / 2;

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x, y;

        if (Math.abs(cos) > Math.abs(sin)) {
            // Horizontal intersection
            x = centerX + (cos > 0 ? halfWidth : -halfWidth);
            y = centerY + (x - centerX) * sin / cos;
        } else {
            // Vertical intersection
            y = centerY + (sin > 0 ? halfHeight : -halfHeight);
            x = centerX + (y - centerY) * cos / sin;
        }

        return new double[]{x, y};
    }

    private void styleEdge(Line edge, String relationshipType) {
        edge.setStrokeWidth(2);

        switch (relationshipType) {
            case "EXTENDS":
                edge.setStroke(Color.valueOf("#3b82f6"));
                edge.getStrokeDashArray().addAll(5d, 5d);
                break;
            case "IMPLEMENTS":
                edge.setStroke(Color.valueOf("#10b981"));
                edge.getStrokeDashArray().addAll(2d, 4d);
                break;
            case "DEPENDS_ON":
                edge.setStroke(Color.valueOf("#ef4444"));
                break;
            case "INJECTED":
                edge.setStroke(Color.valueOf("#8b5cf6"));
                edge.getStrokeDashArray().addAll(8d, 2d, 2d, 2d);
                break;
            case "STARTS":
                edge.setStroke(Color.valueOf("#10b981"));
                edge.setStrokeWidth(3);
                break;
            case "TERMINATES":
                edge.setStroke(Color.valueOf("#ef4444"));
                edge.setStrokeWidth(3);
                break;
            default:
                edge.setStroke(Color.GRAY);
        }
    }

    private void setupZoomHandlers(ScrollPane scrollPane) {
        // Mouse wheel zoom
        scrollPane.setOnScroll(event -> {
            if (event.isControlDown()) {
                double zoomFactor = 1.05;
                if (event.getDeltaY() < 0) {
                    zoomFactor = 0.95;
                }
                zoom(zoomFactor, scrollPane);
                event.consume();
            }
        });
    }

    public void zoom(double factor, ScrollPane scrollPane) {
        currentZoom *= factor;

        // Limit zoom range
        if (currentZoom < 0.1) currentZoom = 0.1;
        if (currentZoom > 5.0) currentZoom = 5.0;

        // Apply scaling
        graphPane.setScaleX(currentZoom);
        graphPane.setScaleY(currentZoom);

        // Adjust scroll pane to maintain center position
        double scrollH = scrollPane.getHvalue();
        double scrollV = scrollPane.getVvalue();

        scrollPane.setHvalue(scrollH);
        scrollPane.setVvalue(scrollV);
    }

    public void resetZoom(ScrollPane scrollPane) {
        currentZoom = 1.0;
        graphPane.setScaleX(currentZoom);
        graphPane.setScaleY(currentZoom);

        // Reset scroll position
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    private void buildRelationshipMap(ProjectAnalysisResult result) {
        relationshipMap.clear();
        for (ComponentRelationship relationship : result.getRelationships()) {
            relationshipMap
                    .computeIfAbsent(relationship.getSourceId(), k -> new ArrayList<>())
                    .add(relationship);
        }
    }

    private String getDisplayName(CodeComponent component) {
        String name = component.getName();
        if (name.length() > 20) {
            return name.substring(0, 17) + "...";
        }
        return name;
    }

    private List<double[]> calculateCurvedPath(double startX, double startY, double endX, double endY) {
        List<double[]> pathPoints = new ArrayList<>();

        // Calculate direction vector
        double dx = endX - startX;
        double dy = endY - startY;

        // Calculate distance
        double distance = Math.sqrt(dx * dx + dy * dy);

        // If distance is small, use a straight line
        if (distance < 200) {
            pathPoints.add(new double[]{startX, startY});
            pathPoints.add(new double[]{endX, endY});
            return pathPoints;
        }

        // Calculate control points for a curved path
        double controlX1, controlY1, controlX2, controlY2;

        // Determine curve direction based on angle
        double angle = Math.atan2(dy, dx);

        // Add some curvature to avoid overlapping with nodes
        double curveStrength = 80;

        if (Math.abs(dx) > Math.abs(dy)) {
            // More horizontal than vertical
            controlX1 = startX + dx * 0.4;
            controlY1 = startY + (dy > 0 ? curveStrength : -curveStrength);
            controlX2 = startX + dx * 0.6;
            controlY2 = endY + (dy > 0 ? -curveStrength : curveStrength);
        } else {
            // More vertical than horizontal
            controlX1 = startX + (dx > 0 ? curveStrength : -curveStrength);
            controlY1 = startY + dy * 0.4;
            controlX2 = endX + (dx > 0 ? -curveStrength : curveStrength);
            controlY2 = startY + dy * 0.6;
        }

        // Create a bezier curve with multiple line segments
        int segments = 20;
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double x = bezierPoint(startX, controlX1, controlX2, endX, t);
            double y = bezierPoint(startY, controlY1, controlY2, endY, t);
            pathPoints.add(new double[]{x, y});
        }

        return pathPoints;
    }

    private double bezierPoint(double p0, double p1, double p2, double p3, double t) {
        double oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * oneMinusT * p0 +
                3 * oneMinusT * oneMinusT * t * p1 +
                3 * oneMinusT * t * t * p2 +
                t * t * t * p3;
    }

    private Polygon addArrowhead(Line edge, double startX, double startY, double endX, double endY) {
        double angle = Math.atan2(endY - startY, endX - startX);

        // Calculate arrowhead points
        double x1 = endX - ARROW_SIZE * Math.cos(angle - Math.PI / 6);
        double y1 = endY - ARROW_SIZE * Math.sin(angle - Math.PI / 6);
        double x2 = endX - ARROW_SIZE * Math.cos(angle + Math.PI / 6);
        double y2 = endY - ARROW_SIZE * Math.sin(angle + Math.PI / 6);

        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(endX, endY, x1, y1, x2, y2);
        arrowhead.setFill(edge.getStroke());
        arrowhead.setStroke(edge.getStroke());
        arrowhead.setStrokeWidth(1);

        // Add click handler to highlight the path
        arrowhead.setOnMouseClicked(event -> {
            highlightEdgePath(edge);
            event.consume();
        });

        return arrowhead;
    }

    private void highlightEdgePath(Line edge) {
        // Reset all edges to normal
        for (Line e : edges) {
            e.setStrokeWidth(2);
            if (edgeRelationshipMap.get(e) != null) {
                ComponentRelationship rel = edgeRelationshipMap.get(e);
                styleEdge(e, rel.getType());
            }
        }

        // Highlight the clicked edge
        edge.setStrokeWidth(4);
        edge.setStroke(Color.valueOf("#f59e0b"));

        // Highlight the corresponding arrowheads
        for (Polygon arrowhead : arrowheads) {
            if (Math.abs(arrowhead.getPoints().get(0) - edge.getEndX()) < 1 &&
                    Math.abs(arrowhead.getPoints().get(1) - edge.getEndY()) < 1) {
                arrowhead.setStroke(Color.valueOf("#f59e0b"));
                arrowhead.setFill(Color.valueOf("#f59e0b"));
            }
        }

        // Highlight the source and target nodes
        ComponentRelationship relationship = edgeRelationshipMap.get(edge);
        if (relationship != null) {
            highlightNode(relationship.getSourceId(), Color.valueOf("#f59e0b"));
            highlightNode(relationship.getTargetId(), Color.valueOf("#f59e0b"));
        }
    }

    private void highlightNode(String nodeId, Color color) {
        if (nodeMap.containsKey(nodeId)) {
            Rectangle node = nodeMap.get(nodeId);
            node.setStroke(color);
            node.setStrokeWidth(3);
        } else if (specialNodeMap.containsKey(nodeId)) {
            Ellipse node = specialNodeMap.get(nodeId);
            node.setStroke(color);
            node.setStrokeWidth(3);
        }
    }

    private void addInteractions(ProjectAnalysisResult result, Pane pane) {
        for (CodeComponent component : result.getComponents()) {
            // Handle special nodes
            if (specialNodeMap.containsKey(component.getId())) {
                Ellipse node = specialNodeMap.get(component.getId());
                Text label = labelMap.get(component.getId());

                // Create tooltip for special nodes
                Tooltip tooltip = new Tooltip(createSpecialNodeTooltipText(component));
                Tooltip.install(node, tooltip);
                Tooltip.install(label, tooltip);

                // Add hover effects for special nodes
                node.setOnMouseEntered(event -> {
                    node.setStroke(Color.valueOf("#f59e0b"));
                    node.setStrokeWidth(3);
                    node.setEffect(new javafx.scene.effect.Glow(0.4));
                });

                node.setOnMouseExited(event -> {
                    node.setStroke(Color.WHITE);
                    node.setStrokeWidth(2);
                    node.setEffect(null);
                });
            }
            // Handle regular nodes
            else if (nodeMap.containsKey(component.getId())) {
                Rectangle node = nodeMap.get(component.getId());
                Text label = labelMap.get(component.getId());

                // Create tooltip with component details
                Tooltip tooltip = new Tooltip(createTooltipText(component));
                Tooltip.install(node, tooltip);
                Tooltip.install(label, tooltip);

                // Add click handler to highlight dependencies
                node.setOnMouseClicked(event -> highlightDependencies(component, pane, event, result));
                label.setOnMouseClicked(event -> highlightDependencies(component, pane, event, result));

                // Add hover effects
                node.setOnMouseEntered(event -> {
                    node.setStroke(Color.valueOf("#f59e0b"));
                    node.setStrokeWidth(2.5);
                    node.setEffect(new javafx.scene.effect.Glow(0.3));
                });

                node.setOnMouseExited(event -> {
                    node.setStroke(Color.WHITE);
                    node.setStrokeWidth(1.5);
                    node.setEffect(null);
                });
            }
        }

        // Add click handlers to edges
        for (Line edge : edges) {
            edge.setOnMouseClicked(event -> {
                highlightEdgePath(edge);
                event.consume();
            });
        }
    }

    private void highlightDependencies(CodeComponent component, Pane pane, MouseEvent event, ProjectAnalysisResult result) {
        // Reset all nodes to default appearance
        for (Rectangle node : nodeMap.values()) {
            node.setStroke(Color.WHITE);
            node.setStrokeWidth(1.5);
            node.setEffect(null);
        }

        for (Ellipse node : specialNodeMap.values()) {
            node.setStroke(Color.WHITE);
            node.setStrokeWidth(2);
            node.setEffect(null);
        }

        for (Text label : labelMap.values()) {
            label.setFill(Color.BLACK);
        }

        // Highlight the selected node
        highlightSelectedComponent(component);

        // Highlight dependencies
        List<ComponentRelationship> relationships = relationshipMap.get(component.getId());
        if (relationships != null) {
            for (ComponentRelationship rel : relationships) {
                highlightNode(rel.getTargetId(), Color.valueOf("#8b5cf6"));
            }
        }

        // Highlight dependents
        for (ComponentRelationship rel : result.getRelationships()) {
            if (rel.getTargetId().equals(component.getId())) {
                highlightNode(rel.getSourceId(), Color.valueOf("#10b981"));
            }
        }
    }

    private void highlightSelectedComponent(CodeComponent component) {
        if (nodeMap.containsKey(component.getId())) {
            Rectangle node = nodeMap.get(component.getId());
            Text label = labelMap.get(component.getId());

            node.setStroke(Color.valueOf("#f59e0b"));
            node.setStrokeWidth(3);
            node.setEffect(new javafx.scene.effect.Glow(0.4));
            label.setFill(Color.valueOf("#1e40af"));
        } else if (specialNodeMap.containsKey(component.getId())) {
            Ellipse node = specialNodeMap.get(component.getId());
            Text label = labelMap.get(component.getId());

            node.setStroke(Color.valueOf("#f59e0b"));
            node.setStrokeWidth(4);
            node.setEffect(new javafx.scene.effect.Glow(0.4));
            // Special nodes already have white text, so we don't change label color
        }
    }

    private String createSpecialNodeTooltipText(CodeComponent component) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node: ").append(component.getName()).append("\n");
        sb.append("Type: ").append(component.getType().toUpperCase()).append(" NODE\n");

        if (START_NODE_ID.equals(component.getId())) {
            sb.append("Purpose: Entry point of the application flow\n");
            sb.append("Description: This node represents where the application begins execution");
        } else if (END_NODE_ID.equals(component.getId())) {
            sb.append("Purpose: Terminal point of the application flow\n");
            sb.append("Description: This node represents where the application flow typically ends");
        }

        return sb.toString();
    }

    private String createTooltipText(CodeComponent component) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(component.getName()).append("\n");
        sb.append("Type: ").append(component.getType()).append("\n");
        sb.append("Language: ").append(component.getLanguage()).append("\n");

        if (component.getExtendsClass() != null && !component.getExtendsClass().isEmpty()) {
            sb.append("Extends: ").append(component.getExtendsClass()).append("\n");
        }

        if (!component.getImplementsList().isEmpty()) {
            sb.append("Implements: ").append(String.join(", ", component.getImplementsList())).append("\n");
        }

        if (!component.getDependencies().isEmpty()) {
            sb.append("Dependencies: ").append(component.getDependencies().size()).append("\n");
        }

        if (!component.getInjectedDependencies().isEmpty()) {
            sb.append("Injected Dependencies: ").append(component.getInjectedDependencies().size()).append("\n");
        }

        sb.append("File: ").append(component.getFilePath());

        return sb.toString();
    }

    private Color getColorForType(String type) {
        switch (type.toLowerCase()) {
            case "class":
                return Color.valueOf("#3b82f6");
            case "interface":
                return Color.valueOf("#10b981");
            case "layout":
                return Color.valueOf("#ef4444");
            case "widget":
                return Color.valueOf("#8b5cf6");
            case "component":
                return Color.valueOf("#f59e0b");
            case "bean":
                return Color.valueOf("#ec4899");
            case "start":
                return Color.valueOf("#10b981");
            case "end":
                return Color.valueOf("#ef4444");
            default:
                return Color.valueOf("#6b7280");
        }
    }

    private Color getColorForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return Color.valueOf("#ec4899");
            case "kotlin":
                return Color.valueOf("#7e22ce");
            case "dart":
                return Color.valueOf("#0369a1");
            case "javascript":
                return Color.valueOf("#eab308");
            case "typescript":
                return Color.valueOf("#0ea5e9");
            case "xml":
                return Color.valueOf("#84cc16");
            case "system":
                return Color.valueOf("#ffffff");
            default:
                return Color.valueOf("#6b7280");
        }
    }

    public Pane getGraphPane() {
        return graphPane;
    }
}
