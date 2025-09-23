package com.projectvisualizer.visualization.builders;

import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.*;

import java.util.*;
import java.util.stream.Collectors;

public class ComponentFlowBuilder extends AbstractVisualizationBuilder {

    public ComponentFlowBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        super(analysisResult, config);
    }

    @Override
    public VisualizationGraph buildGraph() {
        VisualizationGraph graph = new VisualizationGraph(
                "component_flow",
                "Component Flow View",
                AbstractionLevel.COMPONENT_FLOW
        );

        // Filter for significant components only
        List<CodeComponent> significantComponents = analysisResult.getComponents().stream()
                .filter(this::isSignificantComponent)
                .collect(Collectors.toList());

        // Create nodes for significant components
        for (CodeComponent component : significantComponents) {
            VisualizationNode node = createComponentNode(component);
            graph.addNode(node);
        }

        // Create navigation and service connections
        createNavigationEdges(graph);
        createServiceEdges(graph, significantComponents);

        // Apply layout hints for component flow
        graph.getLayoutHints().setDirection("TB");
        graph.getLayoutHints().setEnableClustering(true);

        aggregateEdges(graph);

        return graph;
    }

    private boolean isSignificantComponent(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String type = component.getType().toLowerCase();

        return name.contains("activity") ||
                name.contains("fragment") ||
                name.contains("service") ||
                name.contains("receiver") ||
                name.contains("provider") ||
                name.contains("viewmodel") ||
                name.contains("repository") ||
                type.equals("service");
    }

    private VisualizationNode createComponentNode(CodeComponent component) {
        VisualizationNode.NodeType nodeType = mapToNodeType(component);
        VisualizationNode node = new VisualizationNode(component.getId(), component.getName(), nodeType);

        node.setRole(inferNodeRole(component));
        node.setLayer(component.getLayer());
        node.setFeature(inferFeatureFromComponent(component));

        // Add metadata
        node.addMetadata("originalComponent", component);
        node.addMetadata("methodCount", component.getMethods().size());
        node.addMetadata("fieldCount", component.getFields().size());

        return node;
    }

    private VisualizationNode.NodeType mapToNodeType(CodeComponent component) {
        String name = component.getName().toLowerCase();

        if (name.contains("activity")) return VisualizationNode.NodeType.ACTIVITY;
        if (name.contains("fragment")) return VisualizationNode.NodeType.FRAGMENT;
        if (name.contains("service")) return VisualizationNode.NodeType.SERVICE;
        if (name.contains("receiver")) return VisualizationNode.NodeType.RECEIVER;
        if (name.contains("provider")) return VisualizationNode.NodeType.PROVIDER;
        if (name.contains("viewmodel")) return VisualizationNode.NodeType.VIEWMODEL;
        if (name.contains("repository")) return VisualizationNode.NodeType.REPOSITORY;
        if (name.contains("datasource")) return VisualizationNode.NodeType.DATASOURCE;

        return VisualizationNode.NodeType.CLASS;
    }

    private void createNavigationEdges(VisualizationGraph graph) {
        // Use navigation flows if available
        for (NavigationFlow navFlow : analysisResult.getNavigationFlows()) {
            VisualizationNode sourceNode = graph.getNode(navFlow.getSourceScreenId());
            VisualizationNode targetNode = graph.getNode(navFlow.getTargetScreenId());

            if (sourceNode != null && targetNode != null) {
                VisualizationEdge edge = VisualizationEdge.createNavigationEdge(
                        sourceNode.getId(), targetNode.getId());
                edge.setLabel("navigates to");
                graph.addEdge(edge);
            }
        }
    }

    private void createServiceEdges(VisualizationGraph graph, List<CodeComponent> components) {
        // Create edges for service dependencies and injections
        for (CodeComponent component : components) {
            for (String injectedDep : component.getInjectedDependencies()) {
                // Find the injected dependency in our component list
                Optional<CodeComponent> depComponent = components.stream()
                        .filter(c -> c.getName().equals(injectedDep) || c.getId().endsWith("." + injectedDep))
                        .findFirst();

                if (depComponent.isPresent()) {
                    VisualizationEdge edge = new VisualizationEdge(
                            component.getId(),
                            depComponent.get().getId(),
                            VisualizationEdge.EdgeType.INJECTION
                    );
                    edge.setLabel("injects");
                    edge.setStyle(VisualizationEdge.EdgeStyle.DASHED);
                    graph.addEdge(edge);
                }
            }
        }
    }
}

