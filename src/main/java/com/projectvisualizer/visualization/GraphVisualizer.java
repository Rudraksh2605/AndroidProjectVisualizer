package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

public class GraphVisualizer {
    private static final double NODE_RADIUS = 30;
    private static final double HORIZONTAL_SPACING = 150;
    private static final double VERTICAL_SPACING = 100;

    private Map<String, Circle> nodeMap = new HashMap<>();
    private Map<String, Text> labelMap = new HashMap<>();

    public ScrollPane createGraphView(ProjectAnalysisResult result) {
        Pane pane = new Pane();
        pane.setPrefSize(10000, 10000); // Large canvas for the graph

        // Create nodes for each component
        createNodes(result, pane);

        // Create edges for relationships
        createEdges(result, pane);

        // Add tooltips and interactions
        addInteractions(result, pane);

        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        return scrollPane;
    }

    private void createNodes(ProjectAnalysisResult result, Pane pane) {
        double x = 100;
        double y = 100;
        int rowCount = 0;

        for (CodeComponent component : result.getComponents()) {
            // Create node
            Circle node = new Circle(x, y, NODE_RADIUS);
            node.setFill(getColorForType(component.getType()));
            node.setStroke(Color.BLACK);

            // Create label
            Text label = new Text(x - 25, y + 40, component.getName());
            label.setWrappingWidth(60);

            // Store references
            nodeMap.put(component.getId(), node);
            labelMap.put(component.getId(), label);

            // Add to pane
            pane.getChildren().addAll(node, label);

            // Position next node
            x += HORIZONTAL_SPACING;
            rowCount++;

            // Move to next row after 5 nodes
            if (rowCount >= 5) {
                x = 100;
                y += VERTICAL_SPACING;
                rowCount = 0;
            }
        }
    }

    private void createEdges(ProjectAnalysisResult result, Pane pane) {
        for (ComponentRelationship relationship : result.getRelationships()) {
            Circle sourceNode = nodeMap.get(relationship.getSourceId());
            Circle targetNode = nodeMap.get(relationship.getTargetId());

            if (sourceNode != null && targetNode != null) {
                Line edge = new Line(
                        sourceNode.getCenterX(), sourceNode.getCenterY(),
                        targetNode.getCenterX(), targetNode.getCenterY()
                );

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
                    default:
                        edge.setStroke(Color.GRAY);
                }

                edge.setStrokeWidth(2);
                pane.getChildren().add(0, edge); // Add edges behind nodes
            }
        }
    }

    private void addInteractions(ProjectAnalysisResult result, Pane pane) {
        for (CodeComponent component : result.getComponents()) {
            Circle node = nodeMap.get(component.getId());
            Text label = labelMap.get(component.getId());

            if (node != null) {
                // Create tooltip with component details
                Tooltip tooltip = new Tooltip(createTooltipText(component));
                Tooltip.install(node, tooltip);
                Tooltip.install(label, tooltip);

                // Add click handler to highlight dependencies
                node.setOnMouseClicked(event -> highlightDependencies(component, pane, event));
                label.setOnMouseClicked(event -> highlightDependencies(component, pane, event));
            }
        }
    }

    private void highlightDependencies(CodeComponent component, Pane pane, MouseEvent event) {
        // Reset all nodes to default appearance
        for (Circle node : nodeMap.values()) {
            node.setStroke(Color.BLACK);
            node.setStrokeWidth(1);
        }

        for (Text label : labelMap.values()) {
            label.setFill(Color.BLACK);
        }

        // Highlight the selected node
        Circle selectedNode = nodeMap.get(component.getId());
        Text selectedLabel = labelMap.get(component.getId());

        if (selectedNode != null) {
            selectedNode.setStroke(Color.YELLOW);
            selectedNode.setStrokeWidth(3);
            selectedLabel.setFill(Color.BLUE);
        }

        // Highlight dependencies (both incoming and outgoing)
        for (Node node : pane.getChildren()) {
            if (node instanceof Line) {
                Line edge = (Line) node;
                // This is a simplified approach - in a real implementation,
                // you'd need to track which edge corresponds to which relationship
                edge.setStrokeWidth(1);
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
            default: return Color.LIGHTGRAY;
        }
    }
}