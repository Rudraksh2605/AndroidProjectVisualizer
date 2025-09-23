package com.projectvisualizer.visualization.builders;


import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.VisualizationConfig;
import com.projectvisualizer.visualization.VisualizationEdge;
import com.projectvisualizer.visualization.VisualizationGraph;
import com.projectvisualizer.visualization.VisualizationNode;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractVisualizationBuilder {
    protected ProjectAnalysisResult analysisResult;
    protected VisualizationConfig config;

    public AbstractVisualizationBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        this.analysisResult = analysisResult;
        this.config = config;
    }

    public abstract VisualizationGraph buildGraph();

    protected void aggregateEdges(VisualizationGraph graph) {
        if (!graph.getLayoutHints().isEnableEdgeAggregation()) return;

        Map<String, List<VisualizationEdge>> edgeGroups = graph.getEdges().stream()
                .collect(Collectors.groupingBy(edge -> edge.getSourceId() + "->" + edge.getTargetId()));

        List<VisualizationEdge> aggregatedEdges = new ArrayList<>();

        for (Map.Entry<String, List<VisualizationEdge>> entry : edgeGroups.entrySet()) {
            List<VisualizationEdge> edges = entry.getValue();
            if (edges.size() > 1) {
                VisualizationEdge firstEdge = edges.get(0);
                List<String> relationships = edges.stream()
                        .map(e -> e.getType().name())
                        .collect(Collectors.toList());

                VisualizationEdge aggregated = VisualizationEdge.createAggregatedEdge(
                        firstEdge.getSourceId(), firstEdge.getTargetId(), relationships);
                aggregatedEdges.add(aggregated);
            } else {
                aggregatedEdges.add(edges.get(0));
            }
        }

        graph.getEdges().clear();
        aggregatedEdges.forEach(graph::addEdge);
    }

    protected String inferFeatureFromComponent(CodeComponent component) {
        String packagePath = component.getId();
        String[] parts = packagePath.split("\\.");

        // Look for feature indicators in package path
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (part.equals("feature") || part.equals("features")) {
                if (i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            // Common feature names
            if (part.matches("(login|auth|profile|dashboard|home|settings|payment|search|chat|notification)")) {
                return part;
            }
        }

        // Fallback: use the deepest non-generic package
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].toLowerCase();
            if (!part.matches("(ui|data|domain|network|repository|service|activity|fragment)")) {
                return part;
            }
        }

        return "core";
    }

    protected VisualizationNode.NodeRole inferNodeRole(CodeComponent component) {
        String name = component.getName().toLowerCase();
        String type = component.getType().toLowerCase();
        String layer = component.getLayer();

        if (name.contains("activity") || name.contains("fragment") || "UI".equals(layer)) {
            return VisualizationNode.NodeRole.VIEW;
        }
        if (name.contains("viewmodel") || name.contains("presenter")) {
            return VisualizationNode.NodeRole.VIEWMODEL;
        }
        if (name.contains("repository")) {
            return VisualizationNode.NodeRole.REPOSITORY;
        }
        if (name.contains("datasource") || name.contains("dao")) {
            return VisualizationNode.NodeRole.DATASOURCE;
        }
        if (name.contains("api") || name.contains("service") || name.contains("client")) {
            return VisualizationNode.NodeRole.NETWORK;
        }
        if (name.contains("database") || name.contains("db")) {
            return VisualizationNode.NodeRole.DATABASE;
        }
        if ("Data".equals(layer)) {
            return VisualizationNode.NodeRole.MODEL;
        }

        return VisualizationNode.NodeRole.BUSINESS_LOGIC;
    }
}
