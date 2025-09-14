// GraphExporter.java
package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GraphExporter {

    public String exportToPlantUML(ProjectAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam defaultFontName Arial\n");
        sb.append("skinparam roundCorner 15\n");
        sb.append("skinparam class {\n");
        sb.append("  BackgroundColor<<UI>> #3b82f6\n");
        sb.append("  BackgroundColor<<Business>> #10b981\n");
        sb.append("  BackgroundColor<<Data>> #ef4444\n");
        sb.append("  BackgroundColor<<Other>> #9ca3af\n");
        sb.append("  ArrowColor #6b7280\n");
        sb.append("  BorderColor #374151\n");
        sb.append("}\n\n");

        // Add components
        for (CodeComponent component : result.getComponents()) {
            String stereotype = getPlantUMLStereotype(component.getLayer());
            sb.append("class ").append(component.getName().replace(".", "_"))
                    .append(" ").append(stereotype).append(" {\n");

            // Add fields if needed
            if (!component.getFields().isEmpty()) {
                for (int i = 0; i < Math.min(3, component.getFields().size()); i++) {
                    sb.append("  ").append(component.getFields().get(i).getName()).append("\n");
                }
                if (component.getFields().size() > 3) {
                    sb.append("  ...\n");
                }
            }
            sb.append("}\n\n");
        }

        // Add relationships
        for (ComponentRelationship rel : result.getRelationships()) {
            String source = rel.getSourceId().contains(".") ?
                    rel.getSourceId().substring(rel.getSourceId().lastIndexOf(".") + 1) : rel.getSourceId();
            String target = rel.getTargetId().contains(".") ?
                    rel.getTargetId().substring(rel.getTargetId().lastIndexOf(".") + 1) : rel.getTargetId();

            String arrowType = getPlantUMLArrowType(rel.getType());
            sb.append(source.replace(".", "_")).append(" ").append(arrowType)
                    .append(" ").append(target.replace(".", "_")).append("\n");
        }

        sb.append("@enduml");
        return sb.toString();
    }

    public String exportToGraphviz(ProjectAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=record, fontname=Arial, fontsize=10];\n");
        sb.append("  edge [fontname=Arial, fontsize=9];\n\n");

        // Add nodes
        for (CodeComponent component : result.getComponents()) {
            String color = getGraphvizColor(component.getLayer());
            String label = component.getName();

            if (!component.getFields().isEmpty()) {
                label = "{ " + component.getName() + " | ";
                for (int i = 0; i < Math.min(2, component.getFields().size()); i++) {
                    label += component.getFields().get(i).getName() + "\\l";
                }
                if (component.getFields().size() > 2) {
                    label += "...\\l";
                }
                label += " }";
            }

            sb.append("  ").append(component.getName().replace(".", "_"))
                    .append(" [label=\"").append(label)
                    .append("\", style=filled, fillcolor=\"").append(color).append("\"];\n");
        }

        sb.append("\n");

        // Add edges
        for (ComponentRelationship rel : result.getRelationships()) {
            String source = rel.getSourceId().contains(".") ?
                    rel.getSourceId().substring(rel.getSourceId().lastIndexOf(".") + 1) : rel.getSourceId();
            String target = rel.getTargetId().contains(".") ?
                    rel.getTargetId().substring(rel.getTargetId().lastIndexOf(".") + 1) : rel.getTargetId();

            String style = getGraphvizEdgeStyle(rel.getType());
            sb.append("  ").append(source.replace(".", "_"))
                    .append(" -> ").append(target.replace(".", "_"))
                    .append(" [label=\"").append(rel.getType())
                    .append("\", style=").append(style).append("];\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private String getPlantUMLStereotype(String layer) {
        if (layer == null) return "<<Other>>";
        switch (layer) {
            case "UI": return "<<UI>>";
            case "Business Logic": return "<<Business>>";
            case "Data": return "<<Data>>";
            default: return "<<Other>>";
        }
    }

    private String getPlantUMLArrowType(String relationshipType) {
        if (relationshipType == null) return "-->";
        switch (relationshipType) {
            case "EXTENDS": return "--|>";
            case "IMPLEMENTS": return "..|>";
            case "USES": return "-->";
            case "DEPENDS": return "..>";
            case "STARTS": return "-->";
            case "TERMINATES": return "-->";
            default: return "-->";
        }
    }

    private String getGraphvizColor(String layer) {
        if (layer == null) return "lightgray";
        switch (layer) {
            case "UI": return "lightblue";
            case "Business Logic": return "lightgreen";
            case "Data": return "lightcoral";
            default: return "lightgray";
        }
    }

    private String getGraphvizEdgeStyle(String relationshipType) {
        if (relationshipType == null) return "solid";
        switch (relationshipType) {
            case "EXTENDS": return "bold";
            case "IMPLEMENTS": return "dashed";
            case "USES": return "solid";
            case "DEPENDS": return "dotted";
            default: return "solid";
        }
    }
}