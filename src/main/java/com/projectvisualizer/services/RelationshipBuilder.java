package com.projectvisualizer.services;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;

import java.util.ArrayList;
import java.util.List;

public class RelationshipBuilder {
    public void buildRelationships(ProjectAnalysisResult result) {
        List<ComponentRelationship> relationships = new ArrayList<>();

        for (CodeComponent component : result.getComponents()) {
            // Handle extends relationship
            if (component.getExtendsClass() != null && !component.getExtendsClass().isEmpty()) {
                ComponentRelationship extendsRel = new ComponentRelationship();
                extendsRel.setSourceId(component.getId());
                extendsRel.setTargetId(findComponentIdByName(result, component.getExtendsClass()));
                extendsRel.setType("EXTENDS");
                relationships.add(extendsRel);
            }

            // Handle implements relationships
            for (String interfaceName : component.getImplementsList()) {
                ComponentRelationship implementsRel = new ComponentRelationship();
                implementsRel.setSourceId(component.getId());
                implementsRel.setTargetId(findComponentIdByName(result, interfaceName));
                implementsRel.setType("IMPLEMENTS");
                relationships.add(implementsRel);
            }

            // Handle field dependencies
            for (CodeComponent dependency : component.getDependencies()) {
                ComponentRelationship dependsRel = new ComponentRelationship();
                dependsRel.setSourceId(component.getId());
                dependsRel.setTargetId(dependency.getId());
                dependsRel.setType("DEPENDS_ON");
                relationships.add(dependsRel);
            }

            for (String injectedDep : component.getInjectedDependencies()) {
                ComponentRelationship injectionRel = new ComponentRelationship();
                injectionRel.setSourceId(component.getId());

                // Special handling for autowiring where we don't know the exact type
                if ("AUTOWIRED".equals(injectedDep)) {
                    injectionRel.setTargetId("AUTOWIRED_DEPENDENCY");
                    injectionRel.setType("AUTOWIRED");
                } else {
                    injectionRel.setTargetId(findComponentIdByName(result, injectedDep));
                    injectionRel.setType("INJECTED");
                }

                relationships.add(injectionRel);
            }
        }

        result.setRelationships(relationships);
    }

    private String findComponentIdByName(ProjectAnalysisResult result, String name) {
        for (CodeComponent component : result.getComponents()) {
            if (name.equals(component.getName())) {
                return component.getId();
            }
        }
        return name; // Return the name as ID if not found (for external dependencies)
    }
}