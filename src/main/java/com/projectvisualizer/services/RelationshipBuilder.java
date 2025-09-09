package com.projectvisualizer.services;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class RelationshipBuilder {

    // Special node IDs (should match those in GraphVisualizer)
    private static final String START_NODE_ID = "_START_NODE_";
    private static final String END_NODE_ID = "_END_NODE_";

    public void buildRelationships(ProjectAnalysisResult result) {
        List<ComponentRelationship> relationships = new ArrayList<>();

        // Build regular component relationships
        buildRegularRelationships(result, relationships);

        // Add Start and End node relationships if they don't already exist
        enhanceWithStartEndRelationships(result, relationships);

        result.setRelationships(relationships);
    }

    private void buildRegularRelationships(ProjectAnalysisResult result, List<ComponentRelationship> relationships) {
        for (CodeComponent component : result.getComponents()) {
            // Skip special nodes
            if (isSpecialNode(component)) {
                continue;
            }

            // Handle extends relationship
            if (component.getExtendsClass() != null && !component.getExtendsClass().isEmpty()) {
                String targetId = findComponentIdByName(result, component.getExtendsClass());
                if (targetId != null) {
                    ComponentRelationship extendsRel = new ComponentRelationship();
                    extendsRel.setSourceId(component.getId());
                    extendsRel.setTargetId(targetId);
                    extendsRel.setType("EXTENDS");
                    relationships.add(extendsRel);
                }
            }

            // Handle implements relationships
            for (String interfaceName : component.getImplementsList()) {
                String targetId = findComponentIdByName(result, interfaceName);
                if (targetId != null) {
                    ComponentRelationship implementsRel = new ComponentRelationship();
                    implementsRel.setSourceId(component.getId());
                    implementsRel.setTargetId(targetId);
                    implementsRel.setType("IMPLEMENTS");
                    relationships.add(implementsRel);
                }
            }

            // Handle field dependencies
            for (CodeComponent dependency : component.getDependencies()) {
                if (!isSpecialNode(dependency)) {
                    ComponentRelationship dependsRel = new ComponentRelationship();
                    dependsRel.setSourceId(component.getId());
                    dependsRel.setTargetId(dependency.getId());
                    dependsRel.setType("DEPENDS_ON");
                    relationships.add(dependsRel);
                }
            }

            // Handle injected dependencies
            for (String injectedDep : component.getInjectedDependencies()) {
                ComponentRelationship injectionRel = new ComponentRelationship();
                injectionRel.setSourceId(component.getId());

                // Special handling for autowiring where we don't know the exact type
                if ("AUTOWIRED".equals(injectedDep)) {
                    injectionRel.setTargetId("AUTOWIRED_DEPENDENCY");
                    injectionRel.setType("AUTOWIRED");
                } else {
                    String targetId = findComponentIdByName(result, injectedDep);
                    if (targetId != null) {
                        injectionRel.setTargetId(targetId);
                        injectionRel.setType("INJECTED");
                    } else {
                        // Create a placeholder for external dependencies
                        injectionRel.setTargetId(injectedDep);
                        injectionRel.setType("INJECTED");
                    }
                }

                relationships.add(injectionRel);
            }
        }
    }

    private void enhanceWithStartEndRelationships(ProjectAnalysisResult result, List<ComponentRelationship> relationships) {
        // Check if Start and End nodes already exist in relationships
        boolean hasStartRelationships = relationships.stream()
                .anyMatch(rel -> START_NODE_ID.equals(rel.getSourceId()));
        boolean hasEndRelationships = relationships.stream()
                .anyMatch(rel -> END_NODE_ID.equals(rel.getTargetId()));

        // Add Start node relationships if they don't exist
        if (!hasStartRelationships) {
            Set<String> entryPoints = identifyEntryPoints(result, relationships);
            for (String entryPointId : entryPoints) {
                ComponentRelationship startRelation = new ComponentRelationship();
                startRelation.setSourceId(START_NODE_ID);
                startRelation.setTargetId(entryPointId);
                startRelation.setType("STARTS");
                relationships.add(startRelation);
            }
        }

        // Add End node relationships if they don't exist
        if (!hasEndRelationships) {
            Set<String> terminalNodes = identifyTerminalNodes(result, relationships);
            for (String terminalId : terminalNodes) {
                ComponentRelationship endRelation = new ComponentRelationship();
                endRelation.setSourceId(terminalId);
                endRelation.setTargetId(END_NODE_ID);
                endRelation.setType("TERMINATES");
                relationships.add(endRelation);
            }
        }
    }

    private Set<String> identifyEntryPoints(ProjectAnalysisResult result, List<ComponentRelationship> relationships) {
        Set<String> entryPoints = new HashSet<>();
        Set<String> allTargets = new HashSet<>();

        // Collect all target IDs from existing relationships
        for (ComponentRelationship rel : relationships) {
            allTargets.add(rel.getTargetId());
        }

        for (CodeComponent component : result.getComponents()) {
            if (isSpecialNode(component)) {
                continue;
            }

            String name = component.getName().toLowerCase();
            String type = component.getType().toLowerCase();
            String filePath = component.getFilePath() != null ? component.getFilePath().toLowerCase() : "";

            // Priority-based entry point detection
            boolean isHighPriorityEntry = false;
            boolean isMediumPriorityEntry = false;
            boolean isLowPriorityEntry = false;

            // High priority: Main classes and methods
            if (name.equals("main") || name.contains("main") && type.equals("class")) {
                isHighPriorityEntry = true;
            }
            // Check for main method in methods list
            else if (component.getMethods() != null &&
                    component.getMethods().stream().anyMatch(method ->
                            "main".equals(method.getName()) &&
                                    "public".equals(method.getVisibility()) &&
                                    "static".equals(method.getReturnType()))) {
                isHighPriorityEntry = true;
            }

            // Medium priority: Application entry points
            if (!isHighPriorityEntry) {
                // Android Activities
                if ((name.contains("mainactivity") || name.contains("launchactivity") ||
                        name.contains("splashactivity")) &&
                        (component.getExtendsClass() != null &&
                                component.getExtendsClass().toLowerCase().contains("activity"))) {
                    isMediumPriorityEntry = true;
                }
                // Flutter main app widgets
                else if ((name.contains("myapp") || name.equals("app")) &&
                        type.equals("widget") &&
                        (component.getExtendsClass() != null &&
                                component.getExtendsClass().contains("MaterialApp"))) {
                    isMediumPriorityEntry = true;
                }
                // React/JS app components
                else if ((name.contains("app") || name.contains("index")) &&
                        type.equals("component")) {
                    isMediumPriorityEntry = true;
                }
                // Spring Boot applications
                else if (component.getAnnotations() != null &&
                        component.getAnnotations().stream().anyMatch(ann ->
                                ann.contains("SpringBootApplication") ||
                                        ann.contains("EnableAutoConfiguration"))) {
                    isMediumPriorityEntry = true;
                }
            }

            // Low priority: Components not referenced by others
            if (!isHighPriorityEntry && !isMediumPriorityEntry) {
                if (!allTargets.contains(component.getId())) {
                    isLowPriorityEntry = true;
                }
            }

            // Add to entry points based on priority
            if (isHighPriorityEntry) {
                entryPoints.clear(); // Clear lower priority entries
                entryPoints.add(component.getId());
                break; // High priority found, stop searching
            } else if (isMediumPriorityEntry && entryPoints.isEmpty()) {
                entryPoints.add(component.getId());
            } else if (isLowPriorityEntry && entryPoints.isEmpty()) {
                entryPoints.add(component.getId());
            }
        }

        // If no entry points found, use the first component
        if (entryPoints.isEmpty() && !result.getComponents().isEmpty()) {
            for (CodeComponent component : result.getComponents()) {
                if (!isSpecialNode(component)) {
                    entryPoints.add(component.getId());
                    break;
                }
            }
        }

        return entryPoints;
    }

    private Set<String> identifyTerminalNodes(ProjectAnalysisResult result, List<ComponentRelationship> relationships) {
        Set<String> terminalNodes = new HashSet<>();
        Set<String> allSources = new HashSet<>();

        // Collect all source IDs from existing relationships
        for (ComponentRelationship rel : relationships) {
            allSources.add(rel.getSourceId());
        }

        // Find components that are leaf nodes (no outgoing relationships)
        Set<String> leafNodes = new HashSet<>();
        for (CodeComponent component : result.getComponents()) {
            if (isSpecialNode(component)) {
                continue;
            }

            boolean hasOutgoing = relationships.stream()
                    .anyMatch(rel -> rel.getSourceId().equals(component.getId()));

            if (!hasOutgoing) {
                leafNodes.add(component.getId());
            }
        }

        // Priority-based terminal node detection
        for (CodeComponent component : result.getComponents()) {
            if (isSpecialNode(component) || !leafNodes.contains(component.getId())) {
                continue;
            }

            String name = component.getName().toLowerCase();
            String type = component.getType().toLowerCase();

            // High priority: Service layer components, DAOs, Repositories
            if (name.contains("service") || name.contains("repository") ||
                    name.contains("dao") || type.contains("repository") ||
                    (type.equals("interface") && (name.contains("repository") || name.contains("dao")))) {
                terminalNodes.add(component.getId());
            }
            // Medium priority: Utility classes, helpers
            else if (name.contains("util") || name.contains("helper") ||
                    name.contains("manager") || name.contains("handler")) {
                if (terminalNodes.isEmpty()) {
                    terminalNodes.add(component.getId());
                }
            }
        }

        // If no specific terminal nodes found, use any leaf nodes
        if (terminalNodes.isEmpty()) {
            terminalNodes.addAll(leafNodes);
        }

        // If still no terminal nodes, use components that are only targets
        if (terminalNodes.isEmpty()) {
            Set<String> allTargets = new HashSet<>();
            for (ComponentRelationship rel : relationships) {
                allTargets.add(rel.getTargetId());
            }
            allTargets.removeAll(allSources);

            for (String targetId : allTargets) {
                if (result.getComponents().stream()
                        .anyMatch(comp -> comp.getId().equals(targetId) && !isSpecialNode(comp))) {
                    terminalNodes.add(targetId);
                }
            }
        }

        // If still empty, use the last regular component
        if (terminalNodes.isEmpty() && !result.getComponents().isEmpty()) {
            for (int i = result.getComponents().size() - 1; i >= 0; i--) {
                CodeComponent component = result.getComponents().get(i);
                if (!isSpecialNode(component)) {
                    terminalNodes.add(component.getId());
                    break;
                }
            }
        }

        return terminalNodes;
    }

    private boolean isSpecialNode(CodeComponent component) {
        return START_NODE_ID.equals(component.getId()) || END_NODE_ID.equals(component.getId()) ||
                "start".equals(component.getType()) || "end".equals(component.getType());
    }

    private String findComponentIdByName(ProjectAnalysisResult result, String name) {
        // First, try exact match
        for (CodeComponent component : result.getComponents()) {
            if (name.equals(component.getName()) && !isSpecialNode(component)) {
                return component.getId();
            }
        }

        // Then try case-insensitive match
        for (CodeComponent component : result.getComponents()) {
            if (name.equalsIgnoreCase(component.getName()) && !isSpecialNode(component)) {
                return component.getId();
            }
        }

        // Finally, try partial match for qualified names
        for (CodeComponent component : result.getComponents()) {
            if ((component.getId().endsWith("." + name) ||
                    component.getName().endsWith(name)) && !isSpecialNode(component)) {
                return component.getId();
            }
        }

        // Return the name as ID if not found (for external dependencies)
        return name;
    }
}