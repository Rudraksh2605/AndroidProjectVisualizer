package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.services.ComponentCategorizer;
import javafx.scene.layout.Pane;

import java.util.*;
import java.util.stream.Collectors;

public class GraphManager {
    private Pane canvas;
    private Map<String, GraphNode> nodeMap;
    private GraphLayoutManager layoutManager;
    private double viewportWidth;
    private double viewportHeight;
    private double expansionThreshold = 100;
    private double minCanvasWidth = 2000;
    private double minCanvasHeight = 2000;
    private Map<String, List<CodeComponent>> categorizedComponents;
    private String currentViewMode = "ALL";

    // NEW: Central Registry to store the "Real" data
    private Map<String, CodeComponent> componentRegistry;

    private Set<CodeComponent> activeComponents;

    public GraphManager(Pane canvas) {
        this.canvas = canvas;
        this.nodeMap = new HashMap<>();
        this.layoutManager = new GraphLayoutManager();
        this.categorizedComponents = new HashMap<>();
        this.componentRegistry = new HashMap<>(); // Initialize registry
        this.activeComponents = new HashSet<>();

        categorizedComponents.put("UI", new ArrayList<>());
        categorizedComponents.put("DATA_MODEL", new ArrayList<>());
        categorizedComponents.put("BUSINESS_LOGIC", new ArrayList<>());
        categorizedComponents.put("NAVIGATION", new ArrayList<>());
        categorizedComponents.put("UNKNOWN", new ArrayList<>());

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

        setCanvasUserData();
    }

    /**
     * NEW: Looks up the "Real" component containing full dependencies.
     * Handles fuzzy matching (e.g., "MainActivity" -> "com.example.MainActivity")
     */
    public CodeComponent getCanonicalComponent(String identifier) {
        if (identifier == null) return null;

        // 1. Try exact ID match
        if (componentRegistry.containsKey(identifier)) {
            return componentRegistry.get(identifier);
        }

        // 2. Try handling "activity:Name" format often used in GraphNode
        String cleanIdentifier = identifier;
        if (identifier.contains(":")) {
            cleanIdentifier = identifier.substring(identifier.indexOf(":") + 1);
        }

        // 3. Search by simple name (case-insensitive)
        for (CodeComponent comp : componentRegistry.values()) {
            // Check Name
            if (comp.getName() != null && comp.getName().equalsIgnoreCase(cleanIdentifier)) {
                return comp;
            }
            // Check ID ending (e.g., id is com.example.MainActivity, identifier is MainActivity)
            if (comp.getId() != null && comp.getId().endsWith("." + cleanIdentifier)) {
                return comp;
            }
        }

        return null;
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

    public void setCanvasUserData() {
        canvas.setUserData(this);
    }

    public void categorizeComponents(List<CodeComponent> components) {
        categorizedComponents.forEach((k, v) -> v.clear());
        componentRegistry.clear(); // Clear registry

        for (CodeComponent component : components) {
            String category = detectComponentCategory(component);
            categorizedComponents.get(category).add(component);

            // NEW: Populate the registry with the FULL component data
            componentRegistry.put(component.getId(), component);
            if (component.getName() != null) {
                // Also map simple name for easier lookup
                componentRegistry.putIfAbsent(component.getName(), component);
            }
        }
    }

    private String detectComponentCategory(CodeComponent component) {
        return ComponentCategorizer.detectCategory(component);
    }

    public void setViewMode(String viewMode) {
        this.currentViewMode = viewMode;
        updateNodeVisibility();
    }

    private void updateNodeVisibility() {
        for (GraphNode node : nodeMap.values()) {
            CodeComponent component = node.getComponent();
            boolean shouldBeVisible = shouldShowComponent(component);

            node.getContainer().setVisible(shouldBeVisible);
            node.getContainer().setManaged(shouldBeVisible);
        }
    }

    private boolean shouldShowComponent(CodeComponent component) {
        if ("ALL".equals(currentViewMode)) {
            return true;
        }

        String category = detectComponentCategory(component);
        return currentViewMode.equals(category);
    }

    private void refreshGraph() {
        clearGraph();

        List<CodeComponent> componentsToShow;
        if ("ALL".equals(currentViewMode)) {
            componentsToShow = categorizedComponents.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } else {
            componentsToShow = categorizedComponents.get(currentViewMode);
        }

        for (CodeComponent component : componentsToShow) {
            addComponentToGraph(component);
        }
    }

    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        categorizedComponents.forEach((category, list) ->
                stats.put(ComponentCategorizer.getDisplayName(category), list.size()));
        return stats;
    }

    public void setViewModeForExpandedNodes(String viewMode) {
        this.currentViewMode = viewMode;

        // Update all currently expanded nodes
        for (GraphNode node : nodeMap.values()) {
            if (node.isExpanded()) {
                updateExpandedNodeChildren(node, viewMode);
            }
        }
    }

    private void updateExpandedNodeChildren(GraphNode parentNode, String viewMode) {
        if (!parentNode.isExpanded()) return;

        List<GraphNode> children = parentNode.getChildren();

        for (GraphNode child : children) {
            boolean shouldBeVisible = child.shouldShowInViewMode(child.getComponent(), viewMode);

            child.setVisible(shouldBeVisible);

            // Recursively update if this child is also expanded
            if (child.isExpanded()) {
                updateExpandedNodeChildren(child, viewMode);
            }
        }
    }

    public void expandNodeWithChildren(String componentId) {
        GraphNode node = nodeMap.get(componentId);
        if (node != null && !node.isExpanded()) {
            node.expand();
            expandChildNodesRecursively(node, 2);
        }
    }

    private void expandChildNodesRecursively(GraphNode parentNode, int maxDepth) {
        if (maxDepth <= 0) return;

        for (GraphNode childNode : parentNode.getChildren()) {
            if (!childNode.isExpanded() && shouldAutoExpand(childNode)) {
                childNode.expand();
                expandChildNodesRecursively(childNode, maxDepth - 1);
            }
        }
    }

    private boolean shouldAutoExpand(GraphNode node) {
        CodeComponent component = node.getComponent();
        return (component.getDependencies() != null && !component.getDependencies().isEmpty()) ||
                "Activity".equals(component.getType()) ||
                "Fragment".equals(component.getType()) ||
                "ViewModel".equals(component.getType());
    }
}