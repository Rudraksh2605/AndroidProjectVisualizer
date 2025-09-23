package com.projectvisualizer.visualization.builders;

import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.*;

import java.util.*;

public class DetailedBuilder extends AbstractVisualizationBuilder {

    public DetailedBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        super(analysisResult, config);
    }

    @Override
    public VisualizationGraph buildGraph() {
        VisualizationGraph graph = new VisualizationGraph(
                "detailed_view",
                "Detailed Class View",
                AbstractionLevel.DETAILED
        );

        // Create all component nodes (similar to original implementation)
        for (CodeComponent component : analysisResult.getComponents()) {
            if (shouldIncludeComponent(component)) {
                VisualizationNode node = new VisualizationNode(
                        component.getId(),
                        component.getName(),
                        VisualizationNode.NodeType.CLASS
                );
                node.setRole(inferNodeRole(component));
                node.setLayer(component.getLayer());
                node.setFeature(inferFeatureFromComponent(component));

                // Add detailed metadata
                node.addMetadata("originalComponent", component);
                node.addMetadata("methodCount", component.getMethods().size());
                node.addMetadata("fieldCount", component.getFields().size());
                node.addMetadata("annotations", component.getAnnotations());

                graph.addNode(node);
            }
        }

        // Create all relationships
        for (ComponentRelationship rel : analysisResult.getRelationships()) {
            VisualizationNode sourceNode = graph.getNode(rel.getSourceId());
            VisualizationNode targetNode = graph.getNode(rel.getTargetId());

            if (sourceNode != null && targetNode != null) {
                VisualizationEdge.EdgeType edgeType = mapRelationshipType(rel.getType());
                VisualizationEdge edge = new VisualizationEdge(
                        rel.getSourceId(),
                        rel.getTargetId(),
                        edgeType
                );
                edge.setLabel(rel.getType());
                graph.addEdge(edge);
            }
        }

        // Apply clustering and aggregation if enabled
        if (config.isEnableClustering()) {
            applyClustering(graph);
        }

        if (config.isEnableEdgeAggregation()) {
            aggregateEdges(graph);
        }

        // Apply layout hints
        graph.getLayoutHints().setDirection("TB");
        graph.getLayoutHints().setEnableClustering(config.isEnableClustering());

        return graph;
    }

    private boolean shouldIncludeComponent(CodeComponent component) {
        if (config.isHideTestClasses() && isTestClass(component)) {
            return false;
        }
        if (config.isHideUtilityClasses() && isUtilityClass(component)) {
            return false;
        }
        return true;
    }

    private boolean isTestClass(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String path = component.getFilePath() != null ? component.getFilePath().toLowerCase() : "";
        return name.contains("test") || name.endsWith("tests") || path.contains("test");
    }

    private boolean isUtilityClass(CodeComponent component) {
        String name = component.getName().toLowerCase();
        return name.contains("util") || name.contains("helper") || name.contains("constant");
    }

    private VisualizationEdge.EdgeType mapRelationshipType(String relationshipType) {
        switch (relationshipType.toUpperCase()) {
            case "EXTENDS":
                return VisualizationEdge.EdgeType.INHERITANCE;
            case "IMPLEMENTS":
                return VisualizationEdge.EdgeType.IMPLEMENTATION;
            case "NAVIGATES_TO":
                return VisualizationEdge.EdgeType.NAVIGATION;
            case "INJECTED":
            case "AUTOWIRED":
                return VisualizationEdge.EdgeType.INJECTION;
            default:
                return VisualizationEdge.EdgeType.DEPENDENCY;
        }
    }

    private void applyClustering(VisualizationGraph graph) {
        // Group nodes by feature for clustering
        Map<String, List<VisualizationNode>> featureGroups = new HashMap<>();

        for (VisualizationNode node : graph.getAllNodes()) {
            String feature = node.getFeature();
            if (feature != null) {
                featureGroups.computeIfAbsent(feature, k -> new ArrayList<>()).add(node);
            }
        }

        // Create clusters for features with multiple components
        for (Map.Entry<String, List<VisualizationNode>> entry : featureGroups.entrySet()) {
            String featureName = entry.getKey();
            List<VisualizationNode> nodes = entry.getValue();

            if (nodes.size() >= config.getMinClusterSize()) {
                VisualizationNode clusterNode = VisualizationNode.createClusterNode(
                        "cluster_" + featureName,
                        featureName + " (" + nodes.size() + " classes)",
                        VisualizationNode.NodeType.CLASS
                );
                clusterNode.setFeature(featureName);
                clusterNode.setExpandable(true);
                clusterNode.setExpanded(false);

                // Add nodes as children of cluster
                for (VisualizationNode node : nodes) {
                    clusterNode.addChild(node);
                    node.setParent(clusterNode);
                    node.addMetadata("hidden", true); // Hide by default
                }

                graph.addNode(clusterNode);
            }
        }
    }
}
