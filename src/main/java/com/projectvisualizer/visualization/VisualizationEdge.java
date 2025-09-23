package com.projectvisualizer.visualization;

import java.util.*;

public class VisualizationEdge {
    private String id;
    private String sourceId;
    private String targetId;
    private EdgeType type;
    private String label;
    private EdgeStyle style;
    private int weight; // For aggregated edges
    private List<String> aggregatedRelationships = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public enum EdgeType {
        DEPENDENCY, INHERITANCE, IMPLEMENTATION, COMPOSITION, AGGREGATION,
        NAVIGATION, DATA_FLOW, CONTROL_FLOW, INJECTION,
        PACKAGE_DEPENDENCY, MODULE_DEPENDENCY, LAYER_DEPENDENCY,
        FEATURE_DEPENDENCY, CLUSTER_DEPENDENCY
    }

    public enum EdgeStyle {
        SOLID, DASHED, DOTTED, BOLD, ARROW, DOUBLE_ARROW
    }

    public VisualizationEdge(String sourceId, String targetId, EdgeType type) {
        this.id = sourceId + "_to_" + targetId + "_" + type;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.style = EdgeStyle.SOLID;
        this.weight = 1;
    }

    // Factory methods for different edge types
    public static VisualizationEdge createDependencyEdge(String sourceId, String targetId) {
        VisualizationEdge edge = new VisualizationEdge(sourceId, targetId, EdgeType.DEPENDENCY);
        edge.setStyle(EdgeStyle.ARROW);
        return edge;
    }

    public static VisualizationEdge createNavigationEdge(String sourceId, String targetId) {
        VisualizationEdge edge = new VisualizationEdge(sourceId, targetId, EdgeType.NAVIGATION);
        edge.setStyle(EdgeStyle.BOLD);
        return edge;
    }

    public static VisualizationEdge createLayerEdge(String sourceLayer, String targetLayer) {
        VisualizationEdge edge = new VisualizationEdge(sourceLayer, targetLayer, EdgeType.LAYER_DEPENDENCY);
        edge.setStyle(EdgeStyle.DOUBLE_ARROW);
        return edge;
    }

    public static VisualizationEdge createAggregatedEdge(String sourceId, String targetId,
                                                         List<String> relationships) {
        VisualizationEdge edge = new VisualizationEdge(sourceId, targetId, EdgeType.AGGREGATION);
        edge.setAggregatedRelationships(relationships);
        edge.setWeight(relationships.size());
        edge.setLabel(relationships.size() + " connections");
        return edge;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public EdgeType getType() { return type; }
    public void setType(EdgeType type) { this.type = type; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public EdgeStyle getStyle() { return style; }
    public void setStyle(EdgeStyle style) { this.style = style; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public List<String> getAggregatedRelationships() { return aggregatedRelationships; }
    public void setAggregatedRelationships(List<String> relationships) {
        this.aggregatedRelationships = relationships;
    }
    public void addAggregatedRelationship(String relationship) {
        this.aggregatedRelationships.add(relationship);
        this.weight = this.aggregatedRelationships.size();
    }

    public void addMetadata(String key, Object value) { metadata.put(key, value); }
    public Object getMetadata(String key) { return metadata.get(key); }
}