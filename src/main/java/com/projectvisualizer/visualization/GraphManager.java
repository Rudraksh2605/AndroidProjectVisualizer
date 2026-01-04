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
    private DependencyGraph dependencyGraph; // NEW: Robust Graph Structure

    private double viewportWidth;
    private double viewportHeight;
    private double minCanvasWidth = 2000;
    private double minCanvasHeight = 2000;

    private Map<String, List<CodeComponent>> categorizedComponents;
    private String currentViewMode = "ALL";

    public GraphManager(Pane canvas) {
        this.canvas = canvas;
        this.nodeMap = new HashMap<>();
        this.layoutManager = new GraphLayoutManager();
        this.dependencyGraph = new DependencyGraph(); // Initialize graph
        this.categorizedComponents = new HashMap<>();

        categorizedComponents.put("UI", new ArrayList<>());
        categorizedComponents.put("DATA_MODEL", new ArrayList<>());
        categorizedComponents.put("BUSINESS_LOGIC", new ArrayList<>());
        categorizedComponents.put("NAVIGATION", new ArrayList<>());
        categorizedComponents.put("UNKNOWN", new ArrayList<>());

        canvas.setMinSize(minCanvasWidth, minCanvasHeight);
        setCanvasUserData();
    }

    /**
     * Smart lookup for components to avoid duplicates.
     */
    public CodeComponent getCanonicalComponent(String identifier) {
        // 1. Try exact match from registry
        CodeComponent c = dependencyGraph.getComponent(identifier);
        if (c != null) return c;

        // 2. Try handling "activity:Name" format
        String cleanIdentifier = identifier;
        if (identifier.contains(":")) {
            cleanIdentifier = identifier.substring(identifier.indexOf(":") + 1);
        }

        // 3. Search by simple name in registry
        for (CodeComponent comp : dependencyGraph.getAllComponents()) {
            if (comp.getName() != null && comp.getName().equalsIgnoreCase(cleanIdentifier)) {
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

        // Add to data structure
        dependencyGraph.addComponent(component);

        GraphNode newNode = new GraphNode(component, canvas);
        nodeMap.put(component.getId(), newNode);

        // Position using Layered Layout (O(1) calculation)
        layoutManager.positionNewNode(newNode, nodeMap.values());

        canvas.getChildren().add(newNode.getContainer());
        animateNodeAppearance(newNode);
    }

    public void categorizeComponents(List<CodeComponent> components) {
        categorizedComponents.forEach((k, v) -> v.clear());
        dependencyGraph.clear();

        for (CodeComponent component : components) {
            String category = ComponentCategorizer.detectCategory(component);
            categorizedComponents.get(category).add(component);

            // Populate the robust dependency graph
            dependencyGraph.addComponent(component);
        }

        // Build graph edges for O(1) traversal later
        for (CodeComponent component : components) {
            if (component.getDependencies() != null) {
                for (CodeComponent dep : component.getDependencies()) {
                    dependencyGraph.addEdge(component.getId(), dep.getId());
                }
            }
        }
    }

    public void removeComponentFromGraph(String componentId) {
        GraphNode node = nodeMap.remove(componentId);
        if (node != null) {
            node.removeFromCanvas();
            canvas.getChildren().remove(node.getContainer());
            // No need to rearrange heavily, just leave gap or shift lightly
        }
    }

    public void clearGraph() {
        for (GraphNode node : nodeMap.values()) {
            node.removeFromCanvas();
            canvas.getChildren().remove(node.getContainer());
        }
        nodeMap.clear();
        layoutManager = new GraphLayoutManager(); // Reset layout counters
    }

    private void highlightNode(String componentId) {
        GraphNode node = nodeMap.get(componentId);
        if (node != null) {
            node.animateHighlight();
        }
    }

    private void animateNodeAppearance(GraphNode node) {
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), node.getContainer());
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
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

    public void setCanvasUserData() {
        canvas.setUserData(this);
    }

    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        categorizedComponents.forEach((category, list) ->
                stats.put(ComponentCategorizer.getDisplayName(category), list.size()));
        return stats;
    }

    public void setViewModeForExpandedNodes(String viewMode) {
        this.currentViewMode = viewMode;
        for (GraphNode node : nodeMap.values()) {
            if (node.isExpanded()) {
                node.refreshExpansion(); // Let the node handle its children visibility
            }
        }
    }

    /**
     * Public hook to re-run overlap resolution across all currently visible nodes.
     * Invoked by nodes after dynamic expansion.
     */
    public void refreshLayout() {
        if (layoutManager != null && nodeMap != null && !nodeMap.isEmpty()) {
            layoutManager.resolveOverlaps(nodeMap.values());
        }
    }
}