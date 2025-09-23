package com.projectvisualizer.visualization;

import java.util.*;

public class VisualizationNode {
    private String id;
    private String name;
    private String displayName;
    private NodeType type;
    private NodeRole role;
    private String layer;
    private String feature;
    private String packagePath;
    private List<VisualizationNode> children = new ArrayList<>();
    private VisualizationNode parent;
    private Map<String, Object> metadata = new HashMap<>();
    private boolean isExpandable;
    private boolean isExpanded;
    private int componentCount; // For cluster nodes

    public enum NodeType {
        PACKAGE, MODULE, FEATURE, LAYER,
        ACTIVITY, FRAGMENT, SERVICE, RECEIVER, PROVIDER,
        VIEWMODEL, REPOSITORY, DATASOURCE, API, DATABASE,
        CLASS, INTERFACE, ENUM,
        CLUSTER, CONTAINER
    }

    public enum NodeRole {
        VIEW, VIEWMODEL, MODEL, REPOSITORY, DATASOURCE, NETWORK, DATABASE,
        NAVIGATION, BUSINESS_LOGIC, UTILITY, CONFIGURATION, TEST
    }

    // Constructors
    public VisualizationNode(String id, String name, NodeType type) {
        this.id = id;
        this.name = name;
        this.displayName = name;
        this.type = type;
        this.isExpandable = false;
        this.isExpanded = false;
    }

    // Factory methods for different node types
    public static VisualizationNode createPackageNode(String packagePath) {
        String packageName = packagePath.substring(packagePath.lastIndexOf('.') + 1);
        VisualizationNode node = new VisualizationNode(packagePath, packageName, NodeType.PACKAGE);
        node.setPackagePath(packagePath);
        node.setExpandable(true);
        return node;
    }

    public static VisualizationNode createFeatureNode(String featureName) {
        VisualizationNode node = new VisualizationNode("feature_" + featureName, featureName, NodeType.FEATURE);
        node.setFeature(featureName);
        node.setExpandable(true);
        return node;
    }

    public static VisualizationNode createLayerNode(String layerName) {
        VisualizationNode node = new VisualizationNode("layer_" + layerName, layerName, NodeType.LAYER);
        node.setLayer(layerName);
        node.setExpandable(true);
        return node;
    }

    public static VisualizationNode createClusterNode(String clusterId, String clusterName, NodeType baseType) {
        VisualizationNode node = new VisualizationNode(clusterId, clusterName, NodeType.CLUSTER);
        node.setDisplayName(clusterName);
        node.setExpandable(true);
        node.addMetadata("baseType", baseType);
        return node;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public NodeRole getRole() { return role; }
    public void setRole(NodeRole role) { this.role = role; }

    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }

    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }

    public String getPackagePath() { return packagePath; }
    public void setPackagePath(String packagePath) { this.packagePath = packagePath; }

    public List<VisualizationNode> getChildren() { return children; }
    public void addChild(VisualizationNode child) {
        children.add(child);
        child.setParent(this);
    }

    public VisualizationNode getParent() { return parent; }
    public void setParent(VisualizationNode parent) { this.parent = parent; }

    public boolean isExpandable() { return isExpandable; }
    public void setExpandable(boolean expandable) { this.isExpandable = expandable; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public int getComponentCount() { return componentCount; }
    public void setComponentCount(int componentCount) { this.componentCount = componentCount; }

    public void addMetadata(String key, Object value) { metadata.put(key, value); }
    public Object getMetadata(String key) { return metadata.get(key); }
    public Map<String, Object> getAllMetadata() { return metadata; }

    // Utility methods
    public boolean hasChildren() { return !children.isEmpty(); }

    public String getFullPath() {
        if (parent != null) {
            return parent.getFullPath() + "." + name;
        }
        return name;
    }

    public boolean isLeaf() { return children.isEmpty(); }

    public List<VisualizationNode> getAllDescendants() {
        List<VisualizationNode> descendants = new ArrayList<>();
        for (VisualizationNode child : children) {
            descendants.add(child);
            descendants.addAll(child.getAllDescendants());
        }
        return descendants;
    }
}