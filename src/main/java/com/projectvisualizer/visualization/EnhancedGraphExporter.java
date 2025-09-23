package com.projectvisualizer.visualization;

import java.util.*;
import java.util.stream.Collectors;

public class EnhancedGraphExporter {

    public String exportToPlantUML(VisualizationGraph graph) {
        StringBuilder sb = new StringBuilder();

        // Header with styling
        sb.append("@startuml\n");
        sb.append("!theme ").append(graph.getLayoutHints().getTheme()).append("\n");
        sb.append("skinparam defaultFontName \"Inter\"\n");
        sb.append("skinparam roundCorner 15\n");
        sb.append("skinparam backgroundColor #f8fafc\n\n");

        // Apply layout direction
        if ("LR".equals(graph.getLayoutHints().getDirection())) {
            sb.append("left to right direction\n");
        }

        // Define colors and styles
        addPlantUMLStyles(sb);

        // Export nodes with clustering
        exportPlantUMLNodes(sb, graph);

        // Export edges
        exportPlantUMLEdges(sb, graph);

        sb.append("\n@enduml\n");
        return sb.toString();
    }

    private void addPlantUMLStyles(StringBuilder sb) {
        sb.append("skinparam package {\n");
        sb.append("  BackgroundColor #ffffff\n");
        sb.append("  BorderColor #e2e8f0\n");
        sb.append("  FontColor #1a202c\n");
        sb.append("  BorderThickness 2\n");
        sb.append("}\n\n");

        sb.append("skinparam class {\n");
        sb.append("  BackgroundColor<<VIEW>> #3b82f6\n");
        sb.append("  BackgroundColor<<VIEWMODEL>> #8b5cf6\n");
        sb.append("  BackgroundColor<<REPOSITORY>> #10b981\n");
        sb.append("  BackgroundColor<<NETWORK>> #f59e0b\n");
        sb.append("  BackgroundColor<<DATABASE>> #ef4444\n");
        sb.append("  BackgroundColor<<BUSINESS>> #6b7280\n");
        sb.append("  BorderColor #374151\n");
        sb.append("  FontColor #ffffff\n");
        sb.append("  FontSize 12\n");
        sb.append("}\n\n");

        sb.append("skinparam arrow {\n");
        sb.append("  Color #6b7280\n");
        sb.append("  Thickness 2\n");
        sb.append("}\n\n");
    }

    private void exportPlantUMLNodes(StringBuilder sb, VisualizationGraph graph) {
        List<VisualizationNode> visibleNodes = graph.getVisibleNodes();

        // Group nodes by type for better organization
        Map<VisualizationNode.NodeType, List<VisualizationNode>> nodesByType =
                visibleNodes.stream().collect(Collectors.groupingBy(VisualizationNode::getType));

        // Export packages/modules first
        if (nodesByType.containsKey(VisualizationNode.NodeType.PACKAGE)) {
            for (VisualizationNode packageNode : nodesByType.get(VisualizationNode.NodeType.PACKAGE)) {
                exportPlantUMLPackage(sb, packageNode, graph);
            }
        }

        // Export features
        if (nodesByType.containsKey(VisualizationNode.NodeType.FEATURE)) {
            for (VisualizationNode featureNode : nodesByType.get(VisualizationNode.NodeType.FEATURE)) {
                exportPlantUMLFeature(sb, featureNode, graph);
            }
        }

        // Export layers
        if (nodesByType.containsKey(VisualizationNode.NodeType.LAYER)) {
            for (VisualizationNode layerNode : nodesByType.get(VisualizationNode.NodeType.LAYER)) {
                exportPlantUMLLayer(sb, layerNode, graph);
            }
        }

        // Export individual components
        List<VisualizationNode.NodeType> componentTypes = Arrays.asList(
                VisualizationNode.NodeType.ACTIVITY, VisualizationNode.NodeType.FRAGMENT,
                VisualizationNode.NodeType.VIEWMODEL, VisualizationNode.NodeType.REPOSITORY,
                VisualizationNode.NodeType.SERVICE, VisualizationNode.NodeType.CLASS
        );

        for (VisualizationNode.NodeType type : componentTypes) {
            if (nodesByType.containsKey(type)) {
                for (VisualizationNode node : nodesByType.get(type)) {
                    if (!hasVisibleParent(node, graph)) {
                        exportPlantUMLComponent(sb, node);
                    }
                }
            }
        }
    }

    private void exportPlantUMLPackage(StringBuilder sb, VisualizationNode packageNode, VisualizationGraph graph) {
        String packageId = sanitizeId(packageNode.getId());
        sb.append("package \"").append(packageNode.getDisplayName()).append("\" as ").append(packageId).append(" {\n");

        // Export children if expanded
        if (packageNode.isExpanded()) {
            for (VisualizationNode child : packageNode.getChildren()) {
                if (child.getType() == VisualizationNode.NodeType.CLUSTER) {
                    exportPlantUMLCluster(sb, child, "  ");
                } else {
                    sb.append("  ");
                    exportPlantUMLComponent(sb, child);
                }
            }
        } else {
            // Show collapsed state
            sb.append("  note as ").append(packageId).append("_note\n");
            sb.append("    ").append(packageNode.getComponentCount()).append(" components\n");
            sb.append("    Click to expand\n");
            sb.append("  end note\n");
        }

        sb.append("}\n\n");
    }

    private void exportPlantUMLFeature(StringBuilder sb, VisualizationNode featureNode, VisualizationGraph graph) {
        String featureId = sanitizeId(featureNode.getId());
        sb.append("rectangle \"").append(featureNode.getDisplayName()).append("\" as ").append(featureId).append(" {\n");

        if (featureNode.isExpanded()) {
            for (VisualizationNode child : featureNode.getChildren()) {
                sb.append("  ");
                if (child.getType() == VisualizationNode.NodeType.CLUSTER) {
                    exportPlantUMLCluster(sb, child, "  ");
                } else {
                    exportPlantUMLComponent(sb, child);
                }
            }
        }

        sb.append("}\n\n");
    }

    private void exportPlantUMLLayer(StringBuilder sb, VisualizationNode layerNode, VisualizationGraph graph) {
        String layerId = sanitizeId(layerNode.getId());
        String stereotype = getPlantUMLStereotype(layerNode.getRole());

        sb.append("frame \"").append(layerNode.getDisplayName()).append("\" as ").append(layerId).append(" ").append(stereotype).append(" {\n");

        if (layerNode.isExpanded()) {
            for (VisualizationNode child : layerNode.getChildren()) {
                sb.append("  ");
                if (child.getType() == VisualizationNode.NodeType.CLUSTER) {
                    exportPlantUMLCluster(sb, child, "  ");
                } else {
                    exportPlantUMLComponent(sb, child);
                }
            }
        }

        sb.append("}\n\n");
    }

    private void exportPlantUMLCluster(StringBuilder sb, VisualizationNode clusterNode, String indent) {
        String clusterId = sanitizeId(clusterNode.getId());
        sb.append(indent).append("package \"").append(clusterNode.getDisplayName()).append("\" as ").append(clusterId).append(" {\n");

        for (VisualizationNode child : clusterNode.getChildren()) {
            sb.append(indent).append("  ");
            exportPlantUMLComponent(sb, child);
        }

        sb.append(indent).append("}\n");
    }

    private void exportPlantUMLComponent(StringBuilder sb, VisualizationNode node) {
        String nodeId = sanitizeId(node.getId());
        String stereotype = getPlantUMLStereotype(node.getRole());
        String nodeType = getPlantUMLNodeType(node.getType());

        sb.append(nodeType).append(" ").append(nodeId).append(" as \"").append(node.getDisplayName()).append("\" ").append(stereotype).append("\n");
    }

    private void exportPlantUMLEdges(StringBuilder sb, VisualizationGraph graph) {
        sb.append("\n' Relationships\n");

        for (VisualizationEdge edge : graph.getEdges()) {
            String sourceId = sanitizeId(edge.getSourceId());
            String targetId = sanitizeId(edge.getTargetId());
            String arrow = getPlantUMLArrowType(edge.getType(), edge.getStyle());

            sb.append(sourceId).append(" ").append(arrow).append(" ").append(targetId);

            if (edge.getLabel() != null && !edge.getLabel().isEmpty()) {
                sb.append(" : ").append(edge.getLabel());
            }

            sb.append("\n");
        }
    }

    private String getPlantUMLStereotype(VisualizationNode.NodeRole role) {
        if (role == null) return "";
        switch (role) {
            case VIEW: return "<<VIEW>>";
            case VIEWMODEL: return "<<VIEWMODEL>>";
            case REPOSITORY: return "<<REPOSITORY>>";
            case NETWORK: return "<<NETWORK>>";
            case DATABASE: return "<<DATABASE>>";
            default: return "<<BUSINESS>>";
        }
    }

    private String getPlantUMLNodeType(VisualizationNode.NodeType type) {
        switch (type) {
            case ACTIVITY:
            case FRAGMENT:
            case VIEWMODEL:
            case REPOSITORY:
            case SERVICE:
                return "class";
            case INTERFACE:
                return "interface";
            case ENUM:
                return "enum";
            default:
                return "class";
        }
    }

    private String getPlantUMLArrowType(VisualizationEdge.EdgeType type, VisualizationEdge.EdgeStyle style) {
        String baseArrow;
        switch (type) {
            case INHERITANCE:
                baseArrow = "--|>";
                break;
            case IMPLEMENTATION:
                baseArrow = "..|>";
                break;
            case NAVIGATION:
                baseArrow = "==>";
                break;
            case INJECTION:
                baseArrow = "..>";
                break;
            case AGGREGATION:
                baseArrow = "-->";
                break;
            default:
                baseArrow = "-->";
        }

        // Modify based on style
        if (style == VisualizationEdge.EdgeStyle.BOLD) {
            baseArrow = "=" + baseArrow.substring(1);
        }

        return baseArrow;
    }

    public String exportToGraphviz(VisualizationGraph graph) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("digraph \"").append(graph.getTitle()).append("\" {\n");
        sb.append("  rankdir=").append(graph.getLayoutHints().getDirection()).append(";\n");
        sb.append("  ranksep=").append(graph.getLayoutHints().getRankSeparation()).append(";\n");
        sb.append("  nodesep=").append(graph.getLayoutHints().getNodeSeparation()).append(";\n");
        sb.append("  bgcolor=\"#f8fafc\";\n");
        sb.append("  fontname=\"Inter\";\n");
        sb.append("  fontsize=14;\n\n");

        // Global styles
        addGraphvizStyles(sb);

        // Export subgraphs and clusters
        exportGraphvizClusters(sb, graph);

        // Export individual nodes
        exportGraphvizNodes(sb, graph);

        // Export edges
        exportGraphvizEdges(sb, graph);

        sb.append("}\n");
        return sb.toString();
    }

    private void addGraphvizStyles(StringBuilder sb) {
        sb.append("  // Node styles\n");
        sb.append("  node [fontname=\"Inter\", fontsize=12, shape=box, style=\"rounded,filled\", margin=0.1];\n");
        sb.append("  edge [fontname=\"Inter\", fontsize=10, color=\"#6b7280\"];\n\n");
    }

    private void exportGraphvizClusters(StringBuilder sb, VisualizationGraph graph) {
        int clusterIndex = 0;
        List<VisualizationNode> clusterNodes = graph.getAllNodes().stream()
                .filter(node -> node.getType() == VisualizationNode.NodeType.PACKAGE ||
                        node.getType() == VisualizationNode.NodeType.FEATURE ||
                        node.getType() == VisualizationNode.NodeType.LAYER ||
                        node.getType() == VisualizationNode.NodeType.CLUSTER)
                .collect(Collectors.toList());

        for (VisualizationNode clusterNode : clusterNodes) {
            if (clusterNode.isExpanded() && !clusterNode.getChildren().isEmpty()) {
                sb.append("  subgraph cluster_").append(clusterIndex++).append(" {\n");
                sb.append("    label=\"").append(escapeGraphvizString(clusterNode.getDisplayName())).append("\";\n");
                sb.append("    style=filled;\n");
                sb.append("    fillcolor=\"").append(getClusterColor(clusterNode.getType())).append("\";\n");
                sb.append("    fontsize=14;\n");
                sb.append("    fontcolor=\"#1a202c\";\n\n");

                // Add children to cluster
                for (VisualizationNode child : clusterNode.getChildren()) {
                    if (child.getType() == VisualizationNode.NodeType.CLUSTER) {
                        // Nested cluster
                        exportGraphvizNestedCluster(sb, child, clusterIndex++, "    ");
                    } else {
                        String childId = sanitizeId(child.getId());
                        sb.append("    ").append(childId).append(";\n");
                    }
                }

                sb.append("  }\n\n");
            }
        }
    }

    private void exportGraphvizNestedCluster(StringBuilder sb, VisualizationNode clusterNode, int clusterIndex, String indent) {
        sb.append(indent).append("subgraph cluster_").append(clusterIndex).append(" {\n");
        sb.append(indent).append("  label=\"").append(escapeGraphvizString(clusterNode.getDisplayName())).append("\";\n");
        sb.append(indent).append("  style=filled;\n");
        sb.append(indent).append("  fillcolor=\"").append(getClusterColor(clusterNode.getType())).append("\";\n\n");

        for (VisualizationNode child : clusterNode.getChildren()) {
            String childId = sanitizeId(child.getId());
            sb.append(indent).append("  ").append(childId).append(";\n");
        }

        sb.append(indent).append("}\n\n");
    }

    private void exportGraphvizNodes(StringBuilder sb, VisualizationGraph graph) {
        sb.append("  // Individual nodes\n");

        for (VisualizationNode node : graph.getVisibleNodes()) {
            if (hasVisibleParent(node, graph) ||
                    node.getType() == VisualizationNode.NodeType.PACKAGE ||
                    node.getType() == VisualizationNode.NodeType.FEATURE ||
                    node.getType() == VisualizationNode.NodeType.LAYER) {
                continue; // Skip nodes that are in clusters or are cluster headers
            }

            String nodeId = sanitizeId(node.getId());
            String label = createGraphvizNodeLabel(node);
            String color = getNodeColor(node);
            String shape = getNodeShape(node.getType());

            sb.append("  ").append(nodeId).append(" [");
            sb.append("label=\"").append(escapeGraphvizString(label)).append("\", ");
            sb.append("fillcolor=\"").append(color).append("\", ");
            sb.append("shape=").append(shape);

            if (node.getType() == VisualizationNode.NodeType.CLUSTER) {
                sb.append(", style=\"rounded,filled,dashed\"");
            }

            sb.append("];\n");
        }
        sb.append("\n");
    }

    private void exportGraphvizEdges(StringBuilder sb, VisualizationGraph graph) {
        sb.append("  // Edges\n");

        for (VisualizationEdge edge : graph.getEdges()) {
            String sourceId = sanitizeId(edge.getSourceId());
            String targetId = sanitizeId(edge.getTargetId());

            sb.append("  ").append(sourceId).append(" -> ").append(targetId);
            sb.append(" [");

            // Edge styling
            sb.append("color=\"").append(getEdgeColor(edge.getType())).append("\"");

            if (edge.getStyle() == VisualizationEdge.EdgeStyle.DASHED) {
                sb.append(", style=dashed");
            } else if (edge.getStyle() == VisualizationEdge.EdgeStyle.DOTTED) {
                sb.append(", style=dotted");
            } else if (edge.getStyle() == VisualizationEdge.EdgeStyle.BOLD) {
                sb.append(", penwidth=3");
            }

            if (edge.getLabel() != null && !edge.getLabel().isEmpty()) {
                sb.append(", label=\"").append(escapeGraphvizString(edge.getLabel())).append("\"");
            }

            if (edge.getWeight() > 1) {
                sb.append(", penwidth=").append(Math.min(5, edge.getWeight()));
            }

            sb.append("];\n");
        }
    }

    private String createGraphvizNodeLabel(VisualizationNode node) {
        StringBuilder label = new StringBuilder();
        label.append(node.getDisplayName());

        if (node.getComponentCount() > 0) {
            label.append("\\n(").append(node.getComponentCount()).append(" components)");
        }

        Object methodCount = node.getMetadata("methodCount");
        if (methodCount instanceof Integer && (Integer) methodCount > 0) {
            label.append("\\n").append(methodCount).append(" methods");
        }

        return label.toString();
    }

    private String getClusterColor(VisualizationNode.NodeType type) {
        switch (type) {
            case PACKAGE: return "#e0f2fe";
            case FEATURE: return "#f3e5f5";
            case LAYER: return "#e8f5e8";
            case CLUSTER: return "#fff3e0";
            default: return "#f5f5f5";
        }
    }

    private String getNodeColor(VisualizationNode node) {
        if (node.getRole() != null) {
            switch (node.getRole()) {
                case VIEW: return "#3b82f6";
                case VIEWMODEL: return "#8b5cf6";
                case REPOSITORY: return "#10b981";
                case DATASOURCE: return "#06b6d4";
                case NETWORK: return "#f59e0b";
                case DATABASE: return "#ef4444";
                case BUSINESS_LOGIC: return "#6b7280";
                default: return "#9ca3af";
            }
        }

        switch (node.getType()) {
            case ACTIVITY: return "#3b82f6";
            case FRAGMENT: return "#8b5cf6";
            case SERVICE: return "#10b981";
            case REPOSITORY: return "#06b6d4";
            case VIEWMODEL: return "#8b5cf6";
            default: return "#9ca3af";
        }
    }

    private String getNodeShape(VisualizationNode.NodeType type) {
        switch (type) {
            case ACTIVITY:
            case FRAGMENT:
                return "rect";
            case SERVICE:
            case REPOSITORY:
                return "ellipse";
            case INTERFACE:
                return "diamond";
            case CLUSTER:
                return "folder";
            default:
                return "box";
        }
    }

    private String getEdgeColor(VisualizationEdge.EdgeType type) {
        switch (type) {
            case INHERITANCE: return "#3b82f6";
            case IMPLEMENTATION: return "#8b5cf6";
            case NAVIGATION: return "#10b981";
            case INJECTION: return "#f59e0b";
            case PACKAGE_DEPENDENCY: return "#06b6d4";
            case LAYER_DEPENDENCY: return "#ef4444";
            case FEATURE_DEPENDENCY: return "#ec4899";
            default: return "#6b7280";
        }
    }

    private boolean hasVisibleParent(VisualizationNode node, VisualizationGraph graph) {
        VisualizationNode parent = node.getParent();
        while (parent != null) {
            if (parent.isExpanded() &&
                    !Boolean.TRUE.equals(parent.getMetadata("hidden"))) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private String sanitizeId(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("^([0-9])", "_$1"); // Ensure doesn't start with number
    }

    private String escapeGraphvizString(String str) {
        return str.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\n", "\\n");
    }
}
