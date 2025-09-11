package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import javafx.animation.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphVisualizer {
    // Enhanced Constants with Modern Design
    private static final double NODE_WIDTH = 160;
    private static final double NODE_HEIGHT = 80;
    private static final double SPECIAL_NODE_SIZE = 60;
    private static final double HORIZONTAL_SPACING = 280;
    private static final double VERTICAL_SPACING = 200;
    private static final double ARROW_SIZE = 14;
    private static final double CURVE_STRENGTH = 100;

    // Modern Color Palette
    private static final Color PRIMARY_500 = Color.web("#3b82f6");
    private static final Color PRIMARY_600 = Color.web("#2563eb");
    private static final Color SUCCESS = Color.web("#10b981");
    private static final Color WARNING = Color.web("#f59e0b");
    private static final Color ERROR = Color.web("#ef4444");
    private static final Color PURPLE = Color.web("#8b5cf6");
    private static final Color INFO = Color.web("#0ea5e9");
    private static final Color GRAY_400 = Color.web("#9ca3af");
    private static final Color GRAY_600 = Color.web("#4b5563");

    // Layer Colors with Gradients
    private static final LinearGradient UI_GRADIENT = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, PRIMARY_500),
            new Stop(1, PRIMARY_600)
    );

    private static final LinearGradient BUSINESS_GRADIENT = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, SUCCESS),
            new Stop(1, Color.web("#059669"))
    );

    private static final LinearGradient DATA_GRADIENT = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, ERROR),
            new Stop(1, Color.web("#dc2626"))
    );

    private static final LinearGradient OTHER_GRADIENT = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, GRAY_400),
            new Stop(1, GRAY_600)
    );

    // Special node IDs
    private static final String START_NODE_ID = "_START_NODE_";
    private static final String END_NODE_ID = "_END_NODE_";

    // Instance variables
    private Map<String, Shape> nodeMap = new HashMap<>();
    private Map<String, Text> labelMap = new HashMap<>();
    private Map<String, Circle> indicatorMap = new HashMap<>();
    private Pane graphPane;
    private List<Path> edges = new ArrayList<>();
    private List<Polygon> arrowheads = new ArrayList<>();
    private double currentZoom = 1.0;
    private Timeline animationTimeline;
    private Set<String> selectedNodes = new HashSet<>();
    private Map<String, Set<String>> layerGroups = new HashMap<>();

    // Layout options
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

        // Calculate canvas size based on layout and components
        calculateCanvasSize(result);

        // Create background grid if enabled
        if (showGrid) {
            createBackgroundGrid();
        }

        // Enhance the result with Start/End nodes
        ProjectAnalysisResult enhancedResult = enhanceWithStartEndNodes(result);

        // Create nodes with modern styling
        createModernNodes(enhancedResult);

        // Create enhanced edges
        createEnhancedEdges(enhancedResult);

        // Add interactions and animations
        addModernInteractions(enhancedResult);

        // Create scroll pane with enhanced features
        ScrollPane scrollPane = createEnhancedScrollPane();

        return scrollPane;
    }

    private void calculateCanvasSize(ProjectAnalysisResult result) {
        int componentCount = result.getComponents().size() + 2; // +2 for start/end nodes

        double width, height;

        switch (currentLayout) {
            case CIRCULAR:
                double radius = Math.max(300, componentCount * 25);
                width = height = radius * 2 + 400;
                break;
            case GRID:
                int cols = (int) Math.ceil(Math.sqrt(componentCount));
                int rows = (int) Math.ceil((double) componentCount / cols);
                width = cols * (NODE_WIDTH + HORIZONTAL_SPACING) + 200;
                height = rows * (NODE_HEIGHT + VERTICAL_SPACING) + 200;
                break;
            case LAYERED:
                // Group by layers
                Map<String, List<CodeComponent>> layers = groupByLayers(result);
                width = layers.size() * 400 + 200;
                height = layers.values().stream()
                        .mapToInt(List::size)
                        .max().orElse(1) * (NODE_HEIGHT + VERTICAL_SPACING) + 300;
                break;
            default: // HIERARCHICAL, FORCE_DIRECTED
                width = Math.max(1200, componentCount * 40);
                height = Math.max(800, componentCount * 30);
        }

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
        Map<String, List<CodeComponent>> layers = new HashMap<>();
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
        AtomicInteger index = new AtomicInteger(0);
        int totalComponents = result.getComponents().size();

        for (CodeComponent component : result.getComponents()) {
            Shape node;
            double x, y;

            // Calculate position based on layout
            switch (currentLayout) {
                case CIRCULAR:
                    double angle = 2 * Math.PI * index.get() / totalComponents;
                    double radius = Math.min(graphPane.getPrefWidth(), graphPane.getPrefHeight()) / 3;
                    x = graphPane.getPrefWidth() / 2 + radius * Math.cos(angle);
                    y = graphPane.getPrefHeight() / 2 + radius * Math.sin(angle);
                    break;
                case GRID:
                    int cols = (int) Math.ceil(Math.sqrt(totalComponents));
                    int row = index.get() / cols;
                    int col = index.get() % cols;
                    x = col * (NODE_WIDTH + HORIZONTAL_SPACING) + 100;
                    y = row * (NODE_HEIGHT + VERTICAL_SPACING) + 100;
                    break;
                case LAYERED:
                    Map<String, List<CodeComponent>> layers = groupByLayers(result);
                    int layerIndex = 0;
                    int positionInLayer = 0;

                    for (Map.Entry<String, List<CodeComponent>> entry : layers.entrySet()) {
                        if (entry.getValue().contains(component)) {
                            x = layerIndex * 400 + 200;
                            positionInLayer = entry.getValue().indexOf(component);
                            y = positionInLayer * (NODE_HEIGHT + VERTICAL_SPACING) + 150;
                            break;
                        }
                        layerIndex++;
                    }
                    x = 200; // Default if not found
                    y = index.get() * (NODE_HEIGHT + VERTICAL_SPACING) + 150;
                    break;
                default: // HIERARCHICAL, FORCE_DIRECTED
                    x = Math.random() * (graphPane.getPrefWidth() - NODE_WIDTH - 200) + 100;
                    y = Math.random() * (graphPane.getPrefHeight() - NODE_HEIGHT - 200) + 100;
            }

            if (isSpecialNode(component)) {
                // Create special nodes (start/end) as circles
                Ellipse ellipse = new Ellipse(SPECIAL_NODE_SIZE / 2, SPECIAL_NODE_SIZE / 2);
                ellipse.setCenterX(x);
                ellipse.setCenterY(y);

                if (component.getId().equals(START_NODE_ID)) {
                    ellipse.setFill(SUCCESS);
                } else {
                    ellipse.setFill(ERROR);
                }

                node = ellipse;
            } else {
                // Create regular nodes as rounded rectangles
                Rectangle rect = new Rectangle(x, y, NODE_WIDTH, NODE_HEIGHT);
                rect.setArcWidth(15);
                rect.setArcHeight(15);

                // Apply gradient based on layer
                LinearGradient gradient = getGradientForLayer(component.getLayer());
                rect.setFill(gradient);

                node = rect;
            }

            // Add modern styling
            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(8);
            dropShadow.setOffsetY(3);
            dropShadow.setColor(Color.color(0, 0, 0, 0.15));
            node.setEffect(dropShadow);
            node.setStroke(Color.WHITE);
            node.setStrokeWidth(2);

            // Store node reference
            nodeMap.put(component.getId(), node);

            // Create label
            if (showLabels) {
                Text label = createNodeLabel(component, x, y);
                labelMap.put(component.getId(), label);
                graphPane.getChildren().add(label);
            }

            // Create language indicator
            Circle indicator = createLanguageIndicator(component, x, y);
            indicatorMap.put(component.getId(), indicator);
            graphPane.getChildren().add(indicator);

            graphPane.getChildren().add(node);
            index.incrementAndGet();
        }
    }

    private LinearGradient getGradientForLayer(String layer) {
        if (layer == null) return OTHER_GRADIENT;

        switch (layer) {
            case "UI": return UI_GRADIENT;
            case "Business Logic": return BUSINESS_GRADIENT;
            case "Data": return DATA_GRADIENT;
            default: return OTHER_GRADIENT;
        }
    }

    private Text createNodeLabel(CodeComponent component, double x, double y) {
        Text label = new Text(getDisplayName(component));
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setFill(Color.WHITE);
        label.setTextAlignment(TextAlignment.CENTER);

        // Center the label
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();

        if (isSpecialNode(component)) {
            label.setX(x - textWidth / 2);
            label.setY(y + textHeight / 4);
        } else {
            label.setX(x + (NODE_WIDTH - textWidth) / 2);
            label.setY(y + NODE_HEIGHT / 2 + textHeight / 4);
        }

        return label;
    }

    private Circle createLanguageIndicator(CodeComponent component, double x, double y) {
        Circle indicator = new Circle(6);

        if (isSpecialNode(component)) {
            indicator.setCenterX(x + SPECIAL_NODE_SIZE / 2 - 10);
            indicator.setCenterY(y - SPECIAL_NODE_SIZE / 2 + 10);
        } else {
            indicator.setCenterX(x + NODE_WIDTH - 15);
            indicator.setCenterY(y + 15);
        }

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
                graphPane.getChildren().add(edge);

                // Add arrowhead
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

            // Add hover effects
            node.setOnMouseEntered(event -> animateNodeHover(node, true));
            node.setOnMouseExited(event -> animateNodeHover(node, false));

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

    private void animateNodeHover(javafx.scene.Node node, boolean entering) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), node);

        if (entering) {
            scaleTransition.setToX(1.05);
            scaleTransition.setToY(1.05);

            // Add glow effect
            Glow glow = new Glow();
            glow.setLevel(0.4);
            node.setEffect(glow);
        } else {
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);

            // Remove glow effect
            if (node instanceof Rectangle) {
                DropShadow dropShadow = new DropShadow();
                dropShadow.setRadius(8);
                dropShadow.setOffsetY(3);
                dropShadow.setColor(Color.color(0, 0, 0, 0.15));
                node.setEffect(dropShadow);
            }
        }

        scaleTransition.setInterpolator(Interpolator.EASE_OUT);
        scaleTransition.play();
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

            if (selected) {
                Glow glow = new Glow();
                glow.setLevel(0.6);
                rect.setEffect(glow);
            }
        } else if (node instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) node;
            ellipse.setStroke(highlightColor);
            ellipse.setStrokeWidth(selected ? 4 : 3);

            if (selected) {
                Glow glow = new Glow();
                glow.setLevel(0.6);
                ellipse.setEffect(glow);
            }
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
        // Find and highlight the edge for this relationship
        for (Path edge : edges) {
            // This would need additional tracking to match edges to relationships
            // For now, we'll implement a basic version
            edge.setStroke(color);
            edge.setOpacity(1.0);
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
        // This would need to restore original edge styling based on relationship type
        // For now, implement basic reset
        edge.setOpacity(0.8);
        edge.setStrokeWidth(2.5);
        edge.setStroke(GRAY_400);
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

    // Public interface methods
    public void setLayout(LayoutType layout) {
        this.currentLayout = layout;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
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

    public void resetZoom(ScrollPane scrollPane) {
        currentZoom = 1.0;

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), graphPane);
        scaleTransition.setToX(currentZoom);
        scaleTransition.setToY(currentZoom);
        scaleTransition.setInterpolator(Interpolator.EASE_OUT);

        // Also reset scroll position
        scaleTransition.setOnFinished(e -> {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        });

        scaleTransition.play();
    }

    public void fitToWindow(ScrollPane scrollPane) {
        double scrollPaneWidth = scrollPane.getViewportBounds().getWidth();
        double scrollPaneHeight = scrollPane.getViewportBounds().getHeight();
        double contentWidth = graphPane.getPrefWidth();
        double contentHeight = graphPane.getPrefHeight();

        double scaleX = scrollPaneWidth / contentWidth;
        double scaleY = scrollPaneHeight / contentHeight;
        double optimalZoom = Math.min(scaleX, scaleY) * 0.9; // 90% for padding

        currentZoom = optimalZoom;

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(400), graphPane);
        scaleTransition.setToX(currentZoom);
        scaleTransition.setToY(currentZoom);
        scaleTransition.setInterpolator(Interpolator.EASE_OUT);

        scaleTransition.setOnFinished(e -> {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        });

        scaleTransition.play();
    }

    public double getCurrentZoom() {
        return currentZoom;
    }

    public void selectNode(String nodeId) {
        clearSelections();
        selectedNodes.add(nodeId);
        highlightNode(nodeId, WARNING, true);
    }

    public void filterByLayer(String layer) {
        for (Map.Entry<String, Shape> entry : nodeMap.entrySet()) {
            String nodeId = entry.getKey();
            Shape node = entry.getValue();

            // This would need component lookup to check layer
            // For now, show/hide based on simple logic
            boolean visible = "All Layers".equals(layer) ||
                    shouldShowNodeForLayer(nodeId, layer);

            node.setVisible(visible);

            Text label = labelMap.get(nodeId);
            if (label != null) {
                label.setVisible(visible);
            }

            Circle indicator = indicatorMap.get(nodeId);
            if (indicator != null) {
                indicator.setVisible(visible);
            }
        }

        // Update edge visibility based on node visibility
        updateEdgeVisibility();
    }

    private boolean shouldShowNodeForLayer(String nodeId, String layer) {
        // This would be implemented with actual component data
        return true; // Placeholder
    }

    private void updateEdgeVisibility() {
        for (Path edge : edges) {
            // Check if both connected nodes are visible
            boolean visible = true; // Placeholder logic
            edge.setVisible(visible);
        }

        for (Polygon arrowhead : arrowheads) {
            arrowhead.setVisible(true); // Placeholder
        }
    }

    // Utility methods
    private boolean isSpecialNode(CodeComponent component) {
        return START_NODE_ID.equals(component.getId()) ||
                END_NODE_ID.equals(component.getId()) ||
                "start".equals(component.getType()) ||
                "end".equals(component.getType());
    }

    private String getDisplayName(CodeComponent component) {
        String name = component.getName();
        if (name.length() > 18) {
            return name.substring(0, 15) + "...";
        }
        return name;
    }

    // Event firing methods (would be connected to MainController)
    private void fireNodeSelectionEvent(CodeComponent component) {
        // This would fire a custom event that MainController would listen to
        System.out.println("Node selected: " + component.getName());
    }

    private void fireNodeNavigationEvent(CodeComponent component) {
        // This would fire a navigation event
        System.out.println("Navigate to: " + component.getFilePath());
    }

    // Animation and effect methods
    public void animateLayoutChange(LayoutType newLayout, ProjectAnalysisResult result) {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }

        // Fade out current layout
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), graphPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3);

        fadeOut.setOnFinished(e -> {
            // Clear current layout
            graphPane.getChildren().clear();
            nodeMap.clear();
            labelMap.clear();
            indicatorMap.clear();
            edges.clear();
            arrowheads.clear();

            // Apply new layout
            this.currentLayout = newLayout;
            createModernNodes(result);
            createEnhancedEdges(result);
            addModernInteractions(result);

            // Fade in new layout
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), graphPane);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    public void highlightPath(List<String> componentIds) {
        clearSelections();

        for (int i = 0; i < componentIds.size(); i++) {
            String nodeId = componentIds.get(i);
            Color pathColor = Color.hsb(240 + i * 30, 0.8, 0.9); // Different colors for path progression
            highlightNode(nodeId, pathColor, i == componentIds.size() - 1);
        }
    }

    public void showComponentFlow(String startComponentId) {
        // Animate a flow visualization starting from the given component
        Shape startNode = nodeMap.get(startComponentId);
        if (startNode == null) return;

        // Create ripple effect
        Circle ripple = new Circle();
        ripple.setCenterX(startNode.getBoundsInParent().getCenterX());
        ripple.setCenterY(startNode.getBoundsInParent().getCenterY());
        ripple.setRadius(0);
        ripple.setFill(Color.TRANSPARENT);
        ripple.setStroke(PRIMARY_500);
        ripple.setStrokeWidth(3);
        ripple.setOpacity(0.8);

        graphPane.getChildren().add(ripple);

        // Animate ripple expansion
        ScaleTransition rippleAnimation = new ScaleTransition(Duration.millis(1500), ripple);
        rippleAnimation.setFromX(0);
        rippleAnimation.setFromY(0);
        rippleAnimation.setToX(10);
        rippleAnimation.setToY(10);

        FadeTransition rippleFade = new FadeTransition(Duration.millis(1500), ripple);
        rippleFade.setFromValue(0.8);
        rippleFade.setToValue(0);

        ParallelTransition rippleEffect = new ParallelTransition(rippleAnimation, rippleFade);
        rippleEffect.setOnFinished(e -> graphPane.getChildren().remove(ripple));
        rippleEffect.play();
    }

    // Getters for external access
    public Pane getGraphPane() {
        return graphPane;
    }

    public Map<String, Shape> getNodeMap() {
        return new HashMap<>(nodeMap);
    }

    public Set<String> getSelectedNodes() {
        return new HashSet<>(selectedNodes);
    }

    public LayoutType getCurrentLayout() {
        return currentLayout;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public boolean isShowLabels() {
        return showLabels;
    }
}