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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private List<Path> edges = new ArrayList<>();
    private List<Polygon> arrowheads = new ArrayList<>();
    private double currentZoom = 1.0;
    private Set<String> selectedNodes = new HashSet<>();

    public enum LayoutType {
        HIERARCHICAL, FORCE_DIRECTED, CIRCULAR, GRID, LAYERED
    }

    private LayoutType currentLayout = LayoutType.HIERARCHICAL;
    private boolean showGrid = false;
    private boolean showLabels = true;

    public ScrollPane createGraphView(ProjectAnalysisResult result) {
        graphPane = new Pane();
        edges.clear();
        arrowheads.clear();
        nodeMap.clear();
        labelMap.clear();
        indicatorMap.clear();

        // Set up the graph pane with modern styling
        graphPane.getStyleClass().add("graph-container");

        // Enhance the result with Start/End nodes
        ProjectAnalysisResult enhancedResult = enhanceWithStartEndNodes(result);

        // Calculate dynamic canvas size based on components
        calculateDynamicCanvasSize(enhancedResult);

        // Create background grid if enabled
        if (showGrid) {
            createBackgroundGrid();
        }

        // Create nodes with modern styling
        createModernNodes(enhancedResult);

        // Apply layout based on selected type
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

        // Create enhanced edges
        createEnhancedEdges(enhancedResult);

        // Add interactions and animations
        addModernInteractions(enhancedResult);

        // Create scroll pane with enhanced features
        ScrollPane scrollPane = createEnhancedScrollPane();

        return scrollPane;
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

    private void addIntelligentConnections(ProjectAnalysisResult result) {
        Set<String> entryPoints = findEntryPoints(result);
        Set<String> exitPoints = findExitPoints(result);

        // Connect start to entry points
        for (String entryId : entryPoints) {
            ComponentRelationship rel = new ComponentRelationship();
            rel.setSourceId(START_NODE_ID);
            rel.setTargetId(entryId);
            rel.setType("STARTS");
            result.addRelationship(rel);
        }

        // Connect exit points to end
        for (String exitId : exitPoints) {
            ComponentRelationship rel = new ComponentRelationship();
            rel.setSourceId(exitId);
            rel.setTargetId(END_NODE_ID);
            rel.setType("TERMINATES");
            result.addRelationship(rel);
        }
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
        Path path = new Path();

        double startX, startY, endX, endY;

        if (source instanceof Rectangle) {
            Rectangle rect = (Rectangle) source;
            startX = rect.getX() + rect.getWidth() / 2;
            startY = rect.getY() + rect.getHeight() / 2;
        } else if (source instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) source;
            startX = ellipse.getCenterX();
            startY = ellipse.getCenterY();
        } else {
            startX = source.getBoundsInParent().getMinX();
            startY = source.getBoundsInParent().getMinY();
        }

        if (target instanceof Rectangle) {
            Rectangle rect = (Rectangle) target;
            endX = rect.getX() + rect.getWidth() / 2;
            endY = rect.getY() + rect.getHeight() / 2;
        } else if (target instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) target;
            endX = ellipse.getCenterX();
            endY = ellipse.getCenterY();
        } else {
            endX = target.getBoundsInParent().getMinX();
            endY = target.getBoundsInParent().getMinY();
        }

        // Create a curved path
        double controlX = (startX + endX) / 2;
        double controlY = (startY + endY) / 2 - CURVE_STRENGTH;

        path.getElements().add(new MoveTo(startX, startY));
        path.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));

        // Style based on relationship type
        path.setStroke(getColorForRelationship(relationshipType));
        path.setStrokeWidth(2.5);
        path.setOpacity(0.8);
        path.setStrokeLineCap(StrokeLineCap.ROUND);

        // Add dash pattern for certain relationship types
        if ("DEPENDS".equals(relationshipType)) {
            path.getStrokeDashArray().addAll(5d, 5d);
        }

        return path;
    }

    private Color getColorForRelationship(String relationshipType) {
        if (relationshipType == null) return GRAY_400;

        switch (relationshipType) {
            case "EXTENDS": return PRIMARY_500;
            case "IMPLEMENTS": return PURPLE;
            case "USES": return INFO;
            case "DEPENDS": return WARNING;
            case "STARTS": return SUCCESS;
            case "TERMINATES": return ERROR;
            default: return GRAY_400;
        }
    }

    private Polygon createArrowhead(Path edge) {
        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(
                0.0, 0.0,
                -ARROW_SIZE, -ARROW_SIZE / 2,
                -ARROW_SIZE, ARROW_SIZE / 2
        );

        // Position at the end of the path
        PathElement lastElement = edge.getElements().get(edge.getElements().size() - 1);
        if (lastElement instanceof QuadCurveTo) {
            QuadCurveTo curve = (QuadCurveTo) lastElement;
            arrowhead.setTranslateX(curve.getX());
            arrowhead.setTranslateY(curve.getY());
        }

        // Rotate to point in the direction of the curve
        PathElement firstElement = edge.getElements().get(edge.getElements().size() - 2);
        if (firstElement instanceof MoveTo && lastElement instanceof QuadCurveTo) {
            MoveTo move = (MoveTo) firstElement;
            QuadCurveTo curve = (QuadCurveTo) lastElement;

            double dx = curve.getX() - move.getX();
            double dy = curve.getY() - move.getY();
            double angle = Math.toDegrees(Math.atan2(dy, dx));

            arrowhead.setRotate(angle);
        }

        arrowhead.setFill(edge.getStroke());
        arrowhead.setStroke(edge.getStroke());
        arrowhead.setStrokeWidth(1);

        return arrowhead;
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
        ScrollPane scrollPane = new ScrollPane(graphPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("graph-scroll-pane");

        // Enhanced zoom with mouse wheel
        scrollPane.setOnScroll(event -> {
            if (event.isControlDown()) {
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                zoom(zoomFactor, scrollPane);
                event.consume();
            }
        });

        return scrollPane;
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
        double k = Math.sqrt(width * height / result.getComponents().size());
        double repulsion = k * k;
        double attraction = k;
        double damping = 0.8;
        double maxForce = 10.0;
        double minDistance = MIN_NODE_DISTANCE; // Use the minimum distance constant

        for (int iter = 0; iter < iterations; iter++) {
            Map<String, double[]> forces = new HashMap<>();

            // Initialize forces to zero
            for (String nodeId : positions.keySet()) {
                forces.put(nodeId, new double[]{0, 0});
            }

            // Calculate repulsive forces between all pairs of nodes
            for (String node1 : positions.keySet()) {
                for (String node2 : positions.keySet()) {
                    if (node1.equals(node2)) continue;

                    double[] pos1 = positions.get(node1);
                    double[] pos2 = positions.get(node2);
                    double dx = pos1[0] - pos2[0];
                    double dy = pos1[1] - pos2[1];
                    double distance = Math.max(minDistance, Math.sqrt(dx * dx + dy * dy));

                    if (distance > 0) {
                        double force = repulsion / (distance * distance);
                        double fx = force * dx / distance;
                        double fy = force * dy / distance;

                        double[] force1 = forces.get(node1);
                        force1[0] += fx;
                        force1[1] += fy;

                        double[] force2 = forces.get(node2);
                        force2[0] -= fx;
                        force2[1] -= fy;
                    }
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
                double distance = Math.max(minDistance, Math.sqrt(dx * dx + dy * dy));

                if (distance > 0) {
                    double force = attraction * Math.log(distance / k);
                    double fx = force * dx / distance;
                    double fy = force * dy / distance;

                    if (!fixedNodes.get(sourceId)) {
                        double[] force1 = forces.get(sourceId);
                        force1[0] -= fx;
                        force1[1] -= fy;
                    }

                    if (!fixedNodes.get(targetId)) {
                        double[] force2 = forces.get(targetId);
                        force2[0] += fx;
                        force2[1] += fy;
                    }
                }
            }

            // Apply forces and update positions
            for (String nodeId : positions.keySet()) {
                if (fixedNodes.get(nodeId)) continue;

                double[] force = forces.get(nodeId);
                double forceMagnitude = Math.sqrt(force[0] * force[0] + force[1] * force[1]);

                // Limit maximum force
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

                // Keep within bounds
                position[0] = Math.max(100, Math.min(width - 100, position[0]));
                position[1] = Math.max(100, Math.min(height - 100, position[1]));
            }
        }

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


    public void setLayoutType(LayoutType layoutType) {
        this.currentLayout = layoutType;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }
}