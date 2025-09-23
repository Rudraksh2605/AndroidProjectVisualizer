package com.projectvisualizer.visualization.builders;


import com.projectvisualizer.models.*;
import com.projectvisualizer.visualization.*;

import java.util.*;
import java.util.stream.Collectors;

public class HighLevelModuleBuilder extends AbstractVisualizationBuilder {

    public HighLevelModuleBuilder(ProjectAnalysisResult analysisResult, VisualizationConfig config) {
        super(analysisResult, config);
    }

    @Override
    public VisualizationGraph buildGraph() {
        VisualizationGraph graph = new VisualizationGraph(
                "high_level_modules",
                "High-Level Module View",
                AbstractionLevel.HIGH_LEVEL
        );

        // Group components by packages/modules
        Map<String, List<CodeComponent>> packageGroups = analysisResult.getComponents().stream()
                .collect(Collectors.groupingBy(this::getTopLevelPackage));

        // Create package nodes
        for (Map.Entry<String, List<CodeComponent>> entry : packageGroups.entrySet()) {
            String packageName = entry.getKey();
            List<CodeComponent> components = entry.getValue();

            VisualizationNode packageNode = VisualizationNode.createPackageNode(packageName);
            packageNode.setComponentCount(components.size());
            packageNode.setDisplayName(packageName + " (" + components.size() + " classes)");

            // Determine package role
            packageNode.setRole(inferPackageRole(packageName, components));

            graph.addNode(packageNode);
        }

        // Create package dependencies
        createPackageDependencies(graph, packageGroups);

        // Apply layout hints for high-level view
        graph.getLayoutHints().setDirection("LR"); // Left to Right for module dependencies
        graph.getLayoutHints().setEnableClustering(true);

        return graph;
    }

    private String getTopLevelPackage(CodeComponent component) {
        String packagePath = component.getId();
        String[] parts = packagePath.split("\\.");

        // Find the first meaningful package level (skip common roots like com.company.app)
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.matches("(ui|data|domain|network|feature|core|common|util)")) {
                return String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            }
        }

        // Default to first 3-4 levels
        int levels = Math.min(4, parts.length);
        return String.join(".", Arrays.copyOfRange(parts, 0, levels));
    }

    private VisualizationNode.NodeRole inferPackageRole(String packageName, List<CodeComponent> components) {
        String lowerName = packageName.toLowerCase();

        if (lowerName.contains("ui") || lowerName.contains("presentation")) {
            return VisualizationNode.NodeRole.VIEW;
        }
        if (lowerName.contains("data") || lowerName.contains("repository")) {
            return VisualizationNode.NodeRole.REPOSITORY;
        }
        if (lowerName.contains("network") || lowerName.contains("api")) {
            return VisualizationNode.NodeRole.NETWORK;
        }
        if (lowerName.contains("domain") || lowerName.contains("business")) {
            return VisualizationNode.NodeRole.BUSINESS_LOGIC;
        }

        // Analyze component types to infer role
        long uiComponents = components.stream()
                .filter(c -> c.getName().toLowerCase().matches(".*(activity|fragment|adapter|view).*"))
                .count();
        long dataComponents = components.stream()
                .filter(c -> c.getName().toLowerCase().matches(".*(repository|datasource|dao).*"))
                .count();

        if (uiComponents > dataComponents) {
            return VisualizationNode.NodeRole.VIEW;
        } else if (dataComponents > 0) {
            return VisualizationNode.NodeRole.REPOSITORY;
        }

        return VisualizationNode.NodeRole.BUSINESS_LOGIC;
    }

    private void createPackageDependencies(VisualizationGraph graph,
                                           Map<String, List<CodeComponent>> packageGroups) {
        Set<String> addedEdges = new HashSet<>();

        for (ComponentRelationship rel : analysisResult.getRelationships()) {
            String sourcePackage = getTopLevelPackageFromId(rel.getSourceId());
            String targetPackage = getTopLevelPackageFromId(rel.getTargetId());

            if (!sourcePackage.equals(targetPackage)) {
                String edgeKey = sourcePackage + "->" + targetPackage;
                if (!addedEdges.contains(edgeKey)) {
                    VisualizationEdge edge = VisualizationEdge.createDependencyEdge(
                            sourcePackage, targetPackage);
                    edge.setType(VisualizationEdge.EdgeType.PACKAGE_DEPENDENCY);
                    graph.addEdge(edge);
                    addedEdges.add(edgeKey);
                }
            }
        }
    }

    private String getTopLevelPackageFromId(String componentId) {
        return getTopLevelPackage(new CodeComponent() {{
            setId(componentId);
        }});
    }
}