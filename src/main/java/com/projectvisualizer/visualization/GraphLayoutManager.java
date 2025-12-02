package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import java.util.*;

public class GraphLayoutManager {

    // Layer Y-Coordinates (Swimlanes)
    private static final double Y_UI_LAYER = 100;
    private static final double Y_LOGIC_LAYER = 400;
    private static final double Y_DATA_LAYER = 700;
    private static final double Y_UNKNOWN_LAYER = 1000;

    // Spacing
    private static final double NODE_SPACING_X = 220;
    private static final double NODE_SPACING_Y = 150;
    private static final double INITIAL_X = 100;

    // Grid tracking to prevent overlap
    private Map<String, Integer> layerCounters = new HashMap<>();

    public GraphLayoutManager() {
        resetCounters();
    }

    private void resetCounters() {
        layerCounters.put("UI", 0);
        layerCounters.put("Business Logic", 0);
        layerCounters.put("Data", 0);
        layerCounters.put("Unknown", 0);
    }

    /**
     * Positions a new node based on its architectural layer (Semantic Layout).
     * This replaces random/spiral positioning with a structured approach.
     */
    public void positionNewNode(GraphNode newNode, Collection<GraphNode> existingNodes) {
        CodeComponent component = newNode.getComponent();
        String layer = component.getLayer() != null ? component.getLayer() : "Unknown";

        // Map category to simplified layer keys if needed
        if (layer.equalsIgnoreCase("DATA_MODEL")) layer = "Data";

        double yPos;
        String counterKey;

        if (isUILayer(layer)) {
            yPos = Y_UI_LAYER;
            counterKey = "UI";
        } else if (isLogicLayer(layer)) {
            yPos = Y_LOGIC_LAYER;
            counterKey = "Business Logic";
        } else if (isDataLayer(layer)) {
            yPos = Y_DATA_LAYER;
            counterKey = "Data";
        } else {
            yPos = Y_UNKNOWN_LAYER;
            counterKey = "Unknown";
        }

        // Get index for this layer
        int index = layerCounters.getOrDefault(counterKey, 0);
        layerCounters.put(counterKey, index + 1);

        // Calculate X position (Grid layout)
        // We wrap every 5 nodes to a new "sub-row" to keep width manageable
        int rowWidth = 8;
        double xPos = INITIAL_X + (index % rowWidth) * NODE_SPACING_X;
        double yOffset = (index / rowWidth) * NODE_SPACING_Y;

        newNode.getContainer().setLayoutX(xPos);
        newNode.getContainer().setLayoutY(yPos + yOffset);
    }

    /**
     * Rearranges all nodes into a clean Hierarchical Layout.
     * Time Complexity: O(N)
     */
    public void rearrangeNodes(Collection<GraphNode> nodes) {
        resetCounters();

        // Sort nodes by package name to keep related classes together visually
        List<GraphNode> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.sort(Comparator.comparing(node -> {
            String pkg = node.getComponent().getPackageName();
            return pkg != null ? pkg : "";
        }));

        for (GraphNode node : sortedNodes) {
            positionNewNode(node, Collections.emptyList());
        }
    }

    private boolean isUILayer(String layer) {
        return layer != null && (layer.equalsIgnoreCase("UI") || layer.contains("Presentation"));
    }

    private boolean isLogicLayer(String layer) {
        return layer != null && (layer.equalsIgnoreCase("Business Logic") || layer.contains("Domain") || layer.contains("Service"));
    }

    private boolean isDataLayer(String layer) {
        return layer != null && (layer.equalsIgnoreCase("Data") || layer.contains("Repository") || layer.contains("Model"));
    }
}