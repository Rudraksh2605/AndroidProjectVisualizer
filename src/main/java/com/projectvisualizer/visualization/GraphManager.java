package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import javafx.scene.layout.Pane;
import java.util.HashMap;
import java.util.Map;

public class GraphManager {
    private Pane canvas;
    private Map<String, GraphNode> nodeMap;
    private GraphLayoutManager layoutManager;
    private double viewportWidth;
    private double viewportHeight;
    private double expansionThreshold = 100; // Increased threshold for better scrolling
    private double minCanvasWidth = 2000;
    private double minCanvasHeight = 2000;

    public GraphManager(Pane canvas) {
        this.canvas = canvas;
        this.nodeMap = new HashMap<>();
        this.layoutManager = new GraphLayoutManager();

        // Set canvas to be large and scrollable
        canvas.setMinSize(minCanvasWidth, minCanvasHeight);
        canvas.setPrefSize(minCanvasWidth, minCanvasHeight);
        canvas.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Listen for canvas size changes to update viewport
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            viewportWidth = newVal.doubleValue();
            checkCanvasExpansion();
        });

        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            viewportHeight = newVal.doubleValue();
            checkCanvasExpansion();
        });
    }

    public void addComponentToGraph(CodeComponent component) {
        if (nodeMap.containsKey(component.getId())) {
            highlightNode(component.getId());
            return;
        }

        GraphNode newNode = new GraphNode(component, canvas);
        nodeMap.put(component.getId(), newNode);

        // Position the new node using layout manager
        layoutManager.positionNewNode(newNode, nodeMap.values());

        canvas.getChildren().add(newNode.getContainer());

        // Check if we need to expand the canvas
        checkNodePositionForExpansion(newNode);

        // Animate the appearance of the new node
        animateNodeAppearance(newNode);
    }

    private void checkNodePositionForExpansion(GraphNode node) {
        javafx.scene.layout.VBox container = node.getContainer();
        double nodeRight = container.getLayoutX() + container.getBoundsInLocal().getWidth();
        double nodeBottom = container.getLayoutY() + container.getBoundsInLocal().getHeight();
        double nodeLeft = container.getLayoutX();
        double nodeTop = container.getLayoutY();

        double currentWidth = canvas.getWidth();
        double currentHeight = canvas.getHeight();

        boolean needsExpansion = false;
        double newWidth = currentWidth;
        double newHeight = currentHeight;

        // Check if node is too close to right edge
        if (nodeRight > currentWidth - expansionThreshold) {
            newWidth = Math.max(currentWidth * 1.5, nodeRight + expansionThreshold);
            needsExpansion = true;
        }

        // Check if node is too close to bottom edge
        if (nodeBottom > currentHeight - expansionThreshold) {
            newHeight = Math.max(currentHeight * 1.5, nodeBottom + expansionThreshold);
            needsExpansion = true;
        }

        // Check if node is too close to left edge (negative space)
        if (nodeLeft < expansionThreshold) {
            double shiftAmount = expansionThreshold - nodeLeft;
            shiftAllNodesRight(shiftAmount);
            newWidth = currentWidth + shiftAmount;
            needsExpansion = true;
        }

        // Check if node is too close to top edge (negative space)
        if (nodeTop < expansionThreshold) {
            double shiftAmount = expansionThreshold - nodeTop;
            shiftAllNodesDown(shiftAmount);
            newHeight = currentHeight + shiftAmount;
            needsExpansion = true;
        }

        if (needsExpansion) {
            // Ensure minimum size
            newWidth = Math.max(minCanvasWidth, newWidth);
            newHeight = Math.max(minCanvasHeight, newHeight);

            canvas.setPrefSize(newWidth, newHeight);
            animateCanvasExpansion();
        }
    }

    private void shiftAllNodesRight(double amount) {
        for (GraphNode node : nodeMap.values()) {
            javafx.scene.layout.VBox container = node.getContainer();
            container.setLayoutX(container.getLayoutX() + amount);
        }
    }

    private void shiftAllNodesDown(double amount) {
        for (GraphNode node : nodeMap.values()) {
            javafx.scene.layout.VBox container = node.getContainer();
            container.setLayoutY(container.getLayoutY() + amount);
        }
    }

    private void checkCanvasExpansion() {
        // Check all nodes to see if any are near the edges
        for (GraphNode node : nodeMap.values()) {
            checkNodePositionForExpansion(node);
        }
    }

    private void animateCanvasExpansion() {
        // Smooth expansion effect
        javafx.animation.FadeTransition fadeTransition =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), canvas);
        fadeTransition.setFromValue(0.95);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    public void removeComponentFromGraph(String componentId) {
        GraphNode node = nodeMap.remove(componentId);
        if (node != null) {
            node.removeFromCanvas();
            canvas.getChildren().remove(node.getContainer());
            layoutManager.rearrangeNodes(nodeMap.values());
        }
    }

    public void clearGraph() {
        for (GraphNode node : nodeMap.values()) {
            node.removeFromCanvas();
            canvas.getChildren().remove(node.getContainer());
        }
        nodeMap.clear();

        // Reset to initial size but maintain scrollability
        canvas.setPrefSize(minCanvasWidth, minCanvasHeight);
    }

    private void highlightNode(String componentId) {
        GraphNode node = nodeMap.get(componentId);
        if (node != null) {
            animateNodeHighlight(node);
        }
    }

    private void animateNodeAppearance(GraphNode node) {
        javafx.animation.ScaleTransition scaleTransition =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(500), node.getContainer());
        scaleTransition.setFromX(0.1);
        scaleTransition.setFromY(0.1);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();

        javafx.animation.FadeTransition fadeTransition =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), node.getContainer());
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    private void animateNodeHighlight(GraphNode node) {
        javafx.animation.ScaleTransition scaleTransition =
                new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), node.getContainer());
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.2);
        scaleTransition.setToY(1.2);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(4);
        scaleTransition.play();
    }

    public Map<String, GraphNode> getNodeMap() {
        return nodeMap;
    }

    public boolean hasNodes() {
        return !nodeMap.isEmpty();
    }

    public boolean containsNode(String componentId) {
        return nodeMap.containsKey(componentId);
    }

    public int getNodeCount() {
        return nodeMap.size();
    }
}