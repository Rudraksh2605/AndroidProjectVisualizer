// GraphExporter.java
package com.projectvisualizer.visualization;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;

import net.sourceforge.plantuml.SourceStringReader;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.HashSet;
import java.util.Set;

public class GraphExporter {
    // PlantUML reserved keywords
    private static final Set<String> PLANTUML_RESERVED_KEYWORDS = new HashSet<>();
    static {
        PLANTUML_RESERVED_KEYWORDS.add("abstract");
        PLANTUML_RESERVED_KEYWORDS.add("actor");
        PLANTUML_RESERVED_KEYWORDS.add("agent");
        PLANTUML_RESERVED_KEYWORDS.add("and");
        PLANTUML_RESERVED_KEYWORDS.add("as");
        PLANTUML_RESERVED_KEYWORDS.add("break");
        PLANTUML_RESERVED_KEYWORDS.add("card");
        PLANTUML_RESERVED_KEYWORDS.add("case");
        PLANTUML_RESERVED_KEYWORDS.add("class");
        PLANTUML_RESERVED_KEYWORDS.add("component");
        PLANTUML_RESERVED_KEYWORDS.add("database");
        PLANTUML_RESERVED_KEYWORDS.add("default");
        PLANTUML_RESERVED_KEYWORDS.add("define");
        PLANTUML_RESERVED_KEYWORDS.add("else");
        PLANTUML_RESERVED_KEYWORDS.add("end");
        PLANTUML_RESERVED_KEYWORDS.add("endif");
        PLANTUML_RESERVED_KEYWORDS.add("endwhile");
        PLANTUML_RESERVED_KEYWORDS.add("entity");
        PLANTUML_RESERVED_KEYWORDS.add("enum");
        PLANTUML_RESERVED_KEYWORDS.add("false");
        PLANTUML_RESERVED_KEYWORDS.add("file");
        PLANTUML_RESERVED_KEYWORDS.add("folder");
        PLANTUML_RESERVED_KEYWORDS.add("frame");
        PLANTUML_RESERVED_KEYWORDS.add("if");
        PLANTUML_RESERVED_KEYWORDS.add("interface");
        PLANTUML_RESERVED_KEYWORDS.add("is");
        PLANTUML_RESERVED_KEYWORDS.add("loop");
        PLANTUML_RESERVED_KEYWORDS.add("namespace");
        PLANTUML_RESERVED_KEYWORDS.add("node");
        PLANTUML_RESERVED_KEYWORDS.add("not");
        PLANTUML_RESERVED_KEYWORDS.add("note");
        PLANTUML_RESERVED_KEYWORDS.add("or");
        PLANTUML_RESERVED_KEYWORDS.add("package");
        PLANTUML_RESERVED_KEYWORDS.add("participant");
        PLANTUML_RESERVED_KEYWORDS.add("repeat");
        PLANTUML_RESERVED_KEYWORDS.add("return");
        PLANTUML_RESERVED_KEYWORDS.add("self");
        PLANTUML_RESERVED_KEYWORDS.add("start");
        PLANTUML_RESERVED_KEYWORDS.add("state");
        PLANTUML_RESERVED_KEYWORDS.add("stop");
        PLANTUML_RESERVED_KEYWORDS.add("switch");
        PLANTUML_RESERVED_KEYWORDS.add("true");
        PLANTUML_RESERVED_KEYWORDS.add("until");
        PLANTUML_RESERVED_KEYWORDS.add("usecase");
        PLANTUML_RESERVED_KEYWORDS.add("while");
        PLANTUML_RESERVED_KEYWORDS.add("xor");
    }

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

        // Create components with sanitized names
        for (CodeComponent component : result.getComponents()) {
            String sanitizedName = sanitizeForPlantUML(component.getName());
            String stereotype = getPlantUMLStereotype(component.getLayer());
            sb.append("class \"").append(sanitizedName).append("\" ")
                    .append(stereotype).append(" {\n");

            if (!component.getFields().isEmpty()) {
                for (int i = 0; i < Math.min(3, component.getFields().size()); i++) {
                    String fieldName = sanitizeForPlantUML(component.getFields().get(i).getName());
                    sb.append("  ").append(fieldName).append("\n");
                }
                if (component.getFields().size() > 3) {
                    sb.append("  ...\n");
                }
            }
            sb.append("}\n\n");
        }

        // Create relationships with sanitized names
        for (ComponentRelationship rel : result.getRelationships()) {
            String source = sanitizeForPlantUML(rel.getSourceId());
            String target = sanitizeForPlantUML(rel.getTargetId());

            // Skip invalid relationships
            if (source.isEmpty() || target.isEmpty()) {
                continue;
            }

            String arrowType = getPlantUMLArrowType(rel.getType());
            sb.append("\"").append(source).append("\" ").append(arrowType)
                    .append(" \"").append(target).append("\"\n");
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

        for (CodeComponent component : result.getComponents()) {
            String color = getGraphvizColor(component.getLayer());
            String sanitizedName = sanitizeForGraphviz(component.getName());
            String label = "\"" + sanitizedName + "\"";

            if (!component.getFields().isEmpty()) {
                label = "\"{ " + sanitizedName + " | ";
                for (int i = 0; i < Math.min(2, component.getFields().size()); i++) {
                    String fieldName = sanitizeForGraphviz(component.getFields().get(i).getName());
                    label += fieldName + "\\l";
                }
                if (component.getFields().size() > 2) {
                    label += "...\\l";
                }
                label += " }\"";
            }

            sb.append("  ").append(sanitizedName.replace(".", "_"))
                    .append(" [label=").append(label)
                    .append(", style=filled, fillcolor=\"").append(color).append("\"];\n");
        }

        sb.append("\n");

        for (ComponentRelationship rel : result.getRelationships()) {
            String source = sanitizeForGraphviz(rel.getSourceId());
            String target = sanitizeForGraphviz(rel.getTargetId());

            // Skip invalid relationships
            if (source.isEmpty() || target.isEmpty()) {
                continue;
            }

            String style = getGraphvizEdgeStyle(rel.getType());
            sb.append("  ").append(source.replace(".", "_"))
                    .append(" -> ").append(target.replace(".", "_"))
                    .append(" [label=\"").append(rel.getType())
                    .append("\", style=").append(style).append("];\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private String sanitizeForPlantUML(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // Remove any characters that might cause syntax issues
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");

        // Handle empty result after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }

        // Check if it's a reserved keyword and add prefix if needed
        if (PLANTUML_RESERVED_KEYWORDS.contains(sanitized.toLowerCase())) {
            sanitized = "class_" + sanitized;
        }

        return sanitized;
    }

    private String sanitizeForGraphviz(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // Remove any characters that might cause syntax issues
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");

        // Handle empty result after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }

        return sanitized;
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

    public BufferedImage exportToPlantUMLImage(ProjectAnalysisResult result) throws IOException {
        String plantUMLCode = exportToPlantUML(result);
        SourceStringReader reader = new SourceStringReader(plantUMLCode);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        reader.outputImage(os);
        os.close();

        byte[] imageData = os.toByteArray();
        return ImageIO.read(new ByteArrayInputStream(imageData));
    }

    public BufferedImage exportToGraphvizImage(ProjectAnalysisResult result) throws IOException {
        String graphvizCode = exportToGraphviz(result);

        try {
            // Try fast native engine first
            Graphviz.useEngine(new GraphvizCmdLineEngine());
            return Graphviz.fromString(graphvizCode).render(Format.PNG).toImage();
        } catch (Exception ex) {
            // Fallback to pure Java engine if 'dot' is not found
            Graphviz.useEngine(new GraphvizJdkEngine());
            return Graphviz.fromString(graphvizCode).render(Format.PNG).toImage();
        }
    }

    public static javafx.scene.image.Image convertToFxImage(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
            return new javafx.scene.image.Image(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert image", e);
        }
    }
}