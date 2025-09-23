package com.projectvisualizer.visualization;

import java.util.*;
import java.util.stream.Collectors;

public class VisualizationGraph {
    private String id;
    private String title;
    private AbstractionLevel level;
    private Map<String, VisualizationNode> nodes = new HashMap<>();
    private List<VisualizationEdge> edges = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private LayoutHints layoutHints = new LayoutHints();

    public static class LayoutHints {
        private String direction = "TB"; // Top to Bottom
        private String rankSeparation = "1.0";
        private String nodeSeparation = "1.0";
        private boolean enableClustering = true;
        private boolean enableEdgeAggregation = true;
        private String theme = "modern";

        // Getters and setters
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public String getRankSeparation() { return rankSeparation; }
        public void setRankSeparation(String rankSeparation) { this.rankSeparation = rankSeparation; }
        public String getNodeSeparation() { return nodeSeparation; }
        public void setNodeSeparation(String nodeSeparation) { this.nodeSeparation = nodeSeparation; }
        public boolean isEnableClustering() { return enableClustering; }
        public void setEnableClustering(boolean enableClustering) { this.enableClustering = enableClustering; }
        public boolean isEnableEdgeAggregation() { return enableEdgeAggregation; }
        public void setEnableEdgeAggregation(boolean enableEdgeAggregation) { this.enableEdgeAggregation = enableEdgeAggregation; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
    }

    public VisualizationGraph(String id, String title, AbstractionLevel level) {
        this.id = id;
        this.title = title;
        this.level = level;
    }

    // Node management
    public void addNode(VisualizationNode node) {
        nodes.put(node.getId(), node);
    }

    public VisualizationNode getNode(String id) {
        return nodes.get(id);
    }

    public Collection<VisualizationNode> getAllNodes() {
        return nodes.values();
    }

    public List<VisualizationNode> getRootNodes() {
        return nodes.values().stream()
                .filter(node -> node.getParent() == null)
                .collect(Collectors.toList());
    }

    public List<VisualizationNode> getNodesByType(VisualizationNode.NodeType type) {
        return nodes.values().stream()
                .filter(node -> node.getType() == type)
                .collect(Collectors.toList());
    }

    // Edge management
    public void addEdge(VisualizationEdge edge) {
        edges.add(edge);
    }

    public List<VisualizationEdge> getEdges() {
        return edges;
    }

    public List<VisualizationEdge> getEdgesFromNode(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getSourceId().equals(nodeId))
                .collect(Collectors.toList());
    }

    public List<VisualizationEdge> getEdgesToNode(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getTargetId().equals(nodeId))
                .collect(Collectors.toList());
    }

    // Graph operations
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        edges.removeIf(edge -> edge.getSourceId().equals(nodeId) || edge.getTargetId().equals(nodeId));
    }

    public void collapseNode(String nodeId) {
        VisualizationNode node = nodes.get(nodeId);
        if (node != null && node.isExpandable()) {
            node.setExpanded(false);
            // Remove child nodes from visible nodes
            hideChildNodes(node);
        }
    }

    public void expandNode(String nodeId) {
        VisualizationNode node = nodes.get(nodeId);
        if (node != null && node.isExpandable()) {
            node.setExpanded(true);
            // Add child nodes to visible nodes
            showChildNodes(node);
        }
    }

    private void hideChildNodes(VisualizationNode parent) {
        for (VisualizationNode child : parent.getChildren()) {
            // Keep in data structure but mark as hidden
            child.addMetadata("hidden", true);
            hideChildNodes(child);
        }
    }

    private void showChildNodes(VisualizationNode parent) {
        for (VisualizationNode child : parent.getChildren()) {
            child.addMetadata("hidden", false);
            if (child.isExpanded()) {
                showChildNodes(child);
            }
        }
    }

    public List<VisualizationNode> getVisibleNodes() {
        return nodes.values().stream()
                .filter(node -> !Boolean.TRUE.equals(node.getMetadata("hidden")))
                .collect(Collectors.toList());
    }

    // Getters and setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public AbstractionLevel getLevel() { return level; }
    public LayoutHints getLayoutHints() { return layoutHints; }
    public void addMetadata(String key, Object value) { metadata.put(key, value); }
    public Object getMetadata(String key) { return metadata.get(key); }
}