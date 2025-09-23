package com.projectvisualizer.visualization.builders;

import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.*;

import java.util.*;
import java.util.stream.Collectors;

public class LayeredArchitectureBuilder extends AbstractVisualizationBuilder {

    private static final List<String> LAYER_ORDER = Arrays.asList(
            "Presentation", "Business", "Data", "Network"
    );

    public LayeredArchitectureBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        super(analysisResult, config);
    }

    @Override
    public VisualizationGraph buildGraph() {
        VisualizationGraph graph = new VisualizationGraph(
                "layered_architecture",
                "Layered Architecture View",
                AbstractionLevel.LAYERED_ARCHITECTURE
        );

        // Group components by architectural layer
        Map<String, List<CodeComponent>> layerGroups = groupComponentsByArchitecturalLayer();

        // Create layer container nodes
        for (String layerName : LAYER_ORDER) {
            if (layerGroups.containsKey(layerName)) {
                VisualizationNode layerNode = createLayerContainer(layerName, layerGroups.get(layerName));
                graph.addNode(layerNode);
            }
        }

        // Create inter-layer connections
        createInterLayerConnections(graph, layerGroups);

        // Apply layout hints for layered view
        graph.getLayoutHints().setDirection("TB");
        graph.getLayoutHints().setRankSeparation("2.0");
        graph.getLayoutHints().setEnableClustering(true);

        return graph;
    }

    private Map<String, List<CodeComponent>> groupComponentsByArchitecturalLayer() {
        Map<String, List<CodeComponent>> layers = new HashMap<>();

        for (CodeComponent component : analysisResult.getComponents()) {
            String architecturalLayer = mapToArchitecturalLayer(component);
            layers.computeIfAbsent(architecturalLayer, k -> new ArrayList<>()).add(component);
        }

        return layers;
    }

    private String mapToArchitecturalLayer(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String layer = component.getLayer();
        VisualizationNode.NodeRole role = inferNodeRole(component);

        // Map based on role and naming conventions
        switch (role) {
            case VIEW:
            case VIEWMODEL:
                return "Presentation";
            case REPOSITORY:
            case DATASOURCE:
            case DATABASE:
                return "Data";
            case NETWORK:
                return "Network";
            default:
                return "Business";
        }
    }

    private VisualizationNode createLayerContainer(String layerName, List<CodeComponent> components) {
        VisualizationNode layerNode = VisualizationNode.createLayerNode(layerName);
        layerNode.setComponentCount(components.size());
        layerNode.setDisplayName(layerName + " Layer (" + components.size() + " components)");

        // Group similar components within the layer
        Map<String, List<CodeComponent>> componentGroups = groupSimilarComponents(components);

        for (Map.Entry<String, List<CodeComponent>> entry : componentGroups.entrySet()) {
            String groupName = entry.getKey();
            List<CodeComponent> groupComponents = entry.getValue();

            if (groupComponents.size() > 1) {
                // Create cluster node for multiple similar components
                VisualizationNode clusterNode = VisualizationNode.createClusterNode(
                        layerName + "_" + groupName,
                        groupName + "s (" + groupComponents.size() + ")",
                        VisualizationNode.NodeType.CLASS
                );
                layerNode.addChild(clusterNode);

                // Add individual components as children of cluster
                for (CodeComponent comp : groupComponents) {
                    VisualizationNode compNode = new VisualizationNode(
                            comp.getId(), comp.getName(), VisualizationNode.NodeType.CLASS);
                    clusterNode.addChild(compNode);
                }
            } else {
                // Single component - add directly to layer
                CodeComponent comp = groupComponents.get(0);
                VisualizationNode compNode = new VisualizationNode(
                        comp.getId(), comp.getName(), VisualizationNode.NodeType.CLASS);
                layerNode.addChild(compNode);
            }
        }

        return layerNode;
    }

    private Map<String, List<CodeComponent>> groupSimilarComponents(List<CodeComponent> components) {
        return components.stream()
                .collect(Collectors.groupingBy(this::getComponentGroup));
    }

    private String getComponentGroup(CodeComponent component) {
        String name = component.getName().toLowerCase();

        if (name.contains("activity")) return "Activity";
        if (name.contains("fragment")) return "Fragment";
        if (name.contains("viewmodel")) return "ViewModel";
        if (name.contains("repository")) return "Repository";
        if (name.contains("datasource")) return "DataSource";
        if (name.contains("service")) return "Service";
        if (name.contains("adapter")) return "Adapter";
        if (name.contains("dao")) return "Dao";

        return "Other";
    }

    private void createInterLayerConnections(VisualizationGraph graph,
                                             Map<String, List<CodeComponent>> layerGroups) {
        // Create connections between layers based on dependencies
        for (int i = 0; i < LAYER_ORDER.size() - 1; i++) {
            String currentLayer = LAYER_ORDER.get(i);
            String nextLayer = LAYER_ORDER.get(i + 1);

            if (hasConnectionsBetweenLayers(currentLayer, nextLayer, layerGroups)) {
                VisualizationEdge layerEdge = VisualizationEdge.createLayerEdge(
                        "layer_" + currentLayer, "layer_" + nextLayer);
                layerEdge.setLabel("depends on");
                graph.addEdge(layerEdge);
            }
        }
    }

    private boolean hasConnectionsBetweenLayers(String layer1, String layer2,
                                                Map<String, List<CodeComponent>> layerGroups) {
        List<CodeComponent> layer1Components = layerGroups.getOrDefault(layer1, Collections.emptyList());
        List<CodeComponent> layer2Components = layerGroups.getOrDefault(layer2, Collections.emptyList());

        Set<String> layer2Ids = layer2Components.stream()
                .map(CodeComponent::getId)
                .collect(Collectors.toSet());

        // Check if any component in layer1 depends on any component in layer2
        for (CodeComponent comp1 : layer1Components) {
            for (String injectedDep : comp1.getInjectedDependencies()) {
                if (layer2Ids.contains(injectedDep)) {
                    return true;
                }
            }
            for (CodeComponent dep : comp1.getDependencies()) {
                if (layer2Ids.contains(dep.getId())) {
                    return true;
                }
            }
        }

        return false;
    }
}

