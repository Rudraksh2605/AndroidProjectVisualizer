package com.projectvisualizer.visualization.builders;

import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.*;

import java.util.*;
import java.util.stream.Collectors;

public class FeatureBasedBuilder extends AbstractVisualizationBuilder {

    public FeatureBasedBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        super(analysisResult, config);
    }

    @Override
    public VisualizationGraph buildGraph() {
        VisualizationGraph graph = new VisualizationGraph(
                "feature_based",
                "Feature-Based View",
                AbstractionLevel.FEATURE_BASED
        );

        // Group components by feature
        Map<String, List<CodeComponent>> featureGroups = analysisResult.getComponents().stream()
                .collect(Collectors.groupingBy(this::inferFeatureFromComponent));

        // Create feature container nodes
        for (Map.Entry<String, List<CodeComponent>> entry : featureGroups.entrySet()) {
            String featureName = entry.getKey();
            List<CodeComponent> components = entry.getValue();

            VisualizationNode featureNode = createFeatureContainer(featureName, components);
            graph.addNode(featureNode);
        }

        // Create inter-feature dependencies
        createInterFeatureDependencies(graph, featureGroups);

        // Apply layout hints for feature view
        graph.getLayoutHints().setDirection("LR");
        graph.getLayoutHints().setEnableClustering(true);

        aggregateEdges(graph);

        return graph;
    }

    private VisualizationNode createFeatureContainer(String featureName, List<CodeComponent> components) {
        VisualizationNode featureNode = VisualizationNode.createFeatureNode(featureName);
        featureNode.setComponentCount(components.size());
        featureNode.setDisplayName(featureName + " (" + components.size() + " classes)");

        // Find entry points (Activities/Fragments) for this feature
        List<CodeComponent> entryPoints = components.stream()
                .filter(c -> c.getName().toLowerCase().contains("activity") ||
                        c.getName().toLowerCase().contains("fragment"))
                .collect(Collectors.toList());

        // Create entry point cluster if multiple entry points exist
        if (entryPoints.size() > 1) {
            VisualizationNode entryCluster = VisualizationNode.createClusterNode(
                    featureName + "_entries",
                    "Entry Points (" + entryPoints.size() + ")",
                    VisualizationNode.NodeType.ACTIVITY
            );
            featureNode.addChild(entryCluster);

            for (CodeComponent entry : entryPoints) {
                VisualizationNode entryNode = new VisualizationNode(
                        entry.getId(), entry.getName(),
                        entry.getName().toLowerCase().contains("activity")
                                ? VisualizationNode.NodeType.ACTIVITY
                                : VisualizationNode.NodeType.FRAGMENT
                );
                entryCluster.addChild(entryNode);
            }
        } else if (entryPoints.size() == 1) {
            CodeComponent entry = entryPoints.get(0);
            VisualizationNode entryNode = new VisualizationNode(
                    entry.getId(), entry.getName(),
                    entry.getName().toLowerCase().contains("activity")
                            ? VisualizationNode.NodeType.ACTIVITY
                            : VisualizationNode.NodeType.FRAGMENT
            );
            featureNode.addChild(entryNode);
        }

        // Group remaining components by role
        List<CodeComponent> nonEntryComponents = components.stream()
                .filter(c -> !entryPoints.contains(c))
                .collect(Collectors.toList());

        Map<String, List<CodeComponent>> roleGroups = nonEntryComponents.stream()
                .collect(Collectors.groupingBy(c -> inferNodeRole(c).name()));

        for (Map.Entry<String, List<CodeComponent>> roleEntry : roleGroups.entrySet()) {
            String roleName = roleEntry.getKey();
            List<CodeComponent> roleComponents = roleEntry.getValue();

            if (roleComponents.size() > 1) {
                VisualizationNode roleCluster = VisualizationNode.createClusterNode(
                        featureName + "_" + roleName,
                        roleName + " (" + roleComponents.size() + ")",
                        VisualizationNode.NodeType.CLASS
                );
                featureNode.addChild(roleCluster);
            }
        }

        return featureNode;
    }

    private void createInterFeatureDependencies(VisualizationGraph graph,
                                                Map<String, List<CodeComponent>> featureGroups) {
        Set<String> addedEdges = new HashSet<>();

        // Check dependencies between different features
        for (ComponentRelationship rel : analysisResult.getRelationships()) {
            String sourceFeature = getFeatureFromComponentId(rel.getSourceId(), featureGroups);
            String targetFeature = getFeatureFromComponentId(rel.getTargetId(), featureGroups);

            if (!sourceFeature.equals(targetFeature)) {
                String edgeKey = sourceFeature + "->" + targetFeature;
                if (!addedEdges.contains(edgeKey)) {
                    VisualizationEdge edge = new VisualizationEdge(
                            "feature_" + sourceFeature,
                            "feature_" + targetFeature,
                            VisualizationEdge.EdgeType.FEATURE_DEPENDENCY
                    );
                    edge.setLabel("depends on");
                    graph.addEdge(edge);
                    addedEdges.add(edgeKey);
                }
            }
        }
    }

    private String getFeatureFromComponentId(String componentId, Map<String, List<CodeComponent>> featureGroups) {
        for (Map.Entry<String, List<CodeComponent>> entry : featureGroups.entrySet()) {
            if (entry.getValue().stream().anyMatch(c -> c.getId().equals(componentId))) {
                return entry.getKey();
            }
        }
        return "unknown";
    }
}
