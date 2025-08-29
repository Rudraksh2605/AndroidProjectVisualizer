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
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.shape.Polygon;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GraphVisualizer {
    private static final double NODE_WIDTH = 120;
    private static final double NODE_HEIGHT = 60;
    private static final double HORIZONTAL_SPACING = 200;
    private static final double VERTICAL_SPACING = 150;

    private Map<String, Rectangle> nodeMap = new HashMap<>();
    private Map<String, Text> labelMap = new HashMap<>();
    private Map<String, List<ComponentRelationship>> relationshipMap = new HashMap<>();
    private Pane graphPane;

    public ScrollPane createGraphView(ProjectAnalysisResult result) {
        graphPane = new Pane();

        // Calculate required canvas size based on number of components
        int componentCount = result.getComponents().size();
        int rows = (int) Math.ceil(Math.sqrt(componentCount));
        int cols = (int) Math.ceil((double) componentCount / rows);

        double canvasWidth = cols * (NODE_WIDTH + HORIZONTAL_SPACING) + 100;
        double canvasHeight = rows * (NODE_HEIGHT + VERTICAL_SPACING) + 100;

        graphPane.setPrefSize(canvasWidth, canvasHeight);
        graphPane.setMinSize(canvasWidth, canvasHeight);

        // Build relationship map for quick lookup
        buildRelationshipMap(result);

        // Create nodes for each component using a grid layout
        createGridNodes(result, graphPane, rows, cols);

        // Create edges for relationships
        createEdges(result, graphPane);

        // Add tooltips and interactions
        addInteractions(result, graphPane);

        ScrollPane scrollPane = new ScrollPane(graphPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        return scrollPane;
    }

    private void buildRelationshipMap(ProjectAnalysisResult result) {
        relationshipMap.clear();
        for (ComponentRelationship relationship : result.getRelationships()) {
            relationshipMap
                    .computeIfAbsent(relationship.getSourceId(), k -> new ArrayList<>())
                    .add(relationship);
        }
    }

    private void createGridNodes(ProjectAnalysisResult result, Pane pane, int rows, int cols) {
        double x = 100;
        double y = 100;
        int count = 0;

        for (CodeComponent component : result.getComponents()) {
            int row = count / cols;
            int col = count % cols;

            double posX = x + col * (NODE_WIDTH + HORIZONTAL_SPACING);
            double posY = y + row * (NODE_HEIGHT + VERTICAL_SPACING);

            // Create node shape based on type
            Rectangle node = createNodeShape(component, posX, posY);

            // Create label
            Text label = new Text(posX + 10, posY + 30, getDisplayName(component));
            label.setWrappingWidth(NODE_WIDTH - 20);

            // Store references
            nodeMap.put(component.getId(), node);
            labelMap.put(component.getId(), label);

            // Add to pane
            pane.getChildren().addAll(node, label);

            count++;
        }
    }

    private Rectangle createNodeShape(CodeComponent component, double x, double y) {
        Rectangle node = new Rectangle(x, y, NODE_WIDTH, NODE_HEIGHT);
        node.setFill(getColorForType(component.getType()));
        node.setStroke(Color.BLACK);
        node.setStrokeWidth(2);
        node.setArcWidth(10);
        node.setArcHeight(10);

        // Add language indicator
        Text langIndicator = new Text(x + 5, y + 15, component.getLanguage().substring(0, 1).toUpperCase());
        langIndicator.setFill(Color.WHITE);
        graphPane.getChildren().add(langIndicator);

        return node;
    }

    private String getDisplayName(CodeComponent component) {
        String name = component.getName();
        if (name.length() > 20) {
            return name.substring(0, 17) + "...";
        }
        return name;
    }

    private void createEdges(ProjectAnalysisResult result, Pane pane) {
        for (ComponentRelationship relationship : result.getRelationships()) {
            Rectangle sourceNode = nodeMap.get(relationship.getSourceId());
            Rectangle targetNode = nodeMap.get(relationship.getTargetId());

            if (sourceNode != null && targetNode != null) {
                // Calculate center points of nodes
                double sourceCenterX = sourceNode.getX() + NODE_WIDTH / 2;
                double sourceCenterY = sourceNode.getY() + NODE_HEIGHT / 2;
                double targetCenterX = targetNode.getX() + NODE_WIDTH / 2;
                double targetCenterY = targetNode.getY() + NODE_HEIGHT / 2;

                Line edge = new Line(sourceCenterX, sourceCenterY, targetCenterX, targetCenterY);

                // Style based on relationship type
                switch (relationship.getType()) {
                    case "EXTENDS":
                        edge.setStroke(Color.BLUE);
                        edge.getStrokeDashArray().addAll(5d, 5d);
                        break;
                    case "IMPLEMENTS":
                        edge.setStroke(Color.GREEN);
                        edge.getStrokeDashArray().addAll(2d, 4d);
                        break;
                    case "DEPENDS_ON":
                        edge.setStroke(Color.RED);
                        break;
                    case "INJECTED":  // Add this case
                        edge.setStroke(Color.PURPLE);
                        edge.getStrokeDashArray().addAll(8d, 2d, 2d, 2d);
                        break;
                    default:
                        edge.setStroke(Color.GRAY);
                }

                edge.setStrokeWidth(2);

                // Add arrowhead
                addArrowhead(edge, sourceCenterX, sourceCenterY, targetCenterX, targetCenterY);

                pane.getChildren().add(0, edge); // Add edges behind nodes
            }
        }
    }

    private void addArrowhead(Line edge, double startX, double startY, double endX, double endY) {
        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowLength = 10;

        // Calculate arrowhead points
        double x1 = endX - arrowLength * Math.cos(angle - Math.PI / 6);
        double y1 = endY - arrowLength * Math.sin(angle - Math.PI / 6);
        double x2 = endX - arrowLength * Math.cos(angle + Math.PI / 6);
        double y2 = endY - arrowLength * Math.sin(angle + Math.PI / 6);

        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(endX, endY, x1, y1, x2, y2);
        arrowhead.setFill(edge.getStroke());

        graphPane.getChildren().add(arrowhead);
    }

    private void addInteractions(ProjectAnalysisResult result, Pane pane) {
        for (CodeComponent component : result.getComponents()) {
            Rectangle node = nodeMap.get(component.getId());
            Text label = labelMap.get(component.getId());

            if (node != null) {
                // Create tooltip with component details
                Tooltip tooltip = new Tooltip(createTooltipText(component));
                Tooltip.install(node, tooltip);
                Tooltip.install(label, tooltip);

                // Add click handler to highlight dependencies
                node.setOnMouseClicked(event -> highlightDependencies(component, pane, event, result));
                label.setOnMouseClicked(event -> highlightDependencies(component, pane, event, result));

                // Add hover effects
                node.setOnMouseEntered(event -> {
                    node.setStroke(Color.YELLOW);
                    node.setStrokeWidth(3);
                });

                node.setOnMouseExited(event -> {
                    node.setStroke(Color.BLACK);
                    node.setStrokeWidth(2);
                });
            }
        }
    }

    private void highlightDependencies(CodeComponent component, Pane pane, MouseEvent event, ProjectAnalysisResult result) {
        // Reset all nodes to default appearance
        for (Rectangle node : nodeMap.values()) {
            node.setStroke(Color.BLACK);
            node.setStrokeWidth(2);
        }

        for (Text label : labelMap.values()) {
            label.setFill(Color.BLACK);
        }

        // Highlight the selected node
        Rectangle selectedNode = nodeMap.get(component.getId());
        Text selectedLabel = labelMap.get(component.getId());

        if (selectedNode != null) {
            selectedNode.setStroke(Color.ORANGE);
            selectedNode.setStrokeWidth(4);
            selectedLabel.setFill(Color.BLUE);
        }

        // Highlight dependencies
        List<ComponentRelationship> relationships = relationshipMap.get(component.getId());
        if (relationships != null) {
            for (ComponentRelationship rel : relationships) {
                Rectangle targetNode = nodeMap.get(rel.getTargetId());
                if (targetNode != null) {
                    targetNode.setStroke(Color.PURPLE);
                    targetNode.setStrokeWidth(3);
                }
            }
        }

        // Highlight dependents
        for (ComponentRelationship rel : result.getRelationships()) {
            if (rel.getTargetId().equals(component.getId())) {
                Rectangle sourceNode = nodeMap.get(rel.getSourceId());
                if (sourceNode != null) {
                    sourceNode.setStroke(Color.CYAN);
                    sourceNode.setStrokeWidth(3);
                }
            }
        }
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

        sb.append("File: ").append(component.getFilePath());

        return sb.toString();
    }

    private Color getColorForType(String type) {
        switch (type.toLowerCase()) {
            case "class": return Color.LIGHTBLUE;
            case "interface": return Color.LIGHTGREEN;
            case "xml": return Color.LIGHTCORAL;
            case "composable": return Color.LIGHTYELLOW;
            case "widget": return Color.LIGHTPINK;
            case "component": return Color.LIGHTGRAY;
            case "dart": return Color.LIGHTSEAGREEN;
            case "javascript": return Color.LIGHTSALMON;
            case "typescript": return Color.LIGHTSTEELBLUE;
            case "kotlin": return Color.LIGHTCYAN;
            default: return Color.LIGHTGRAY;
        }
    }
}