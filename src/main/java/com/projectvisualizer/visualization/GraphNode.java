package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.ExpansionMode;
import com.projectvisualizer.services.ComponentCategorizer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GraphNode {
    private CodeComponent component;
    private Circle nodeCircle;
    private Text nodeLabel;
    private VBox nodeContainer;
    private List<GraphNode> children;
    private List<Line> connectionLines;
    private boolean expanded = false;
    private Pane canvas;
    private String currentViewMode = "ALL";
    private ExpansionMode expansionMode = ExpansionMode.CLASS_DEPENDENCIES;

    public GraphNode(CodeComponent component, Pane canvas) {
        this.component = component;
        this.canvas = canvas;
        this.children = new ArrayList<>();
        this.connectionLines = new ArrayList<>();
        initializeNode();
        setupContextMenu();
        setupEventHandlers();
    }

    private void initializeNode() {
        nodeCircle = new Circle(25);
        nodeCircle.setFill(getColorForLayer(component.getLayer()));
        nodeCircle.setStroke(Color.BLACK);
        nodeCircle.setStrokeWidth(1.5);

        nodeLabel = new Text(getDisplayName());
        nodeLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-alignment: center;");
        nodeLabel.setWrappingWidth(80);

        nodeContainer = new VBox(3);
        nodeContainer.getChildren().addAll(nodeCircle, nodeLabel);
        nodeContainer.setAlignment(Pos.TOP_CENTER);

        Tooltip tooltip = new Tooltip(createTooltipText());
        Tooltip.install(nodeContainer, tooltip);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // 1. Toggle Expansion Action
        MenuItem expandItem = new MenuItem("Toggle Expansion");
        expandItem.setOnAction(e -> toggleExpansion());

        // 2. Expansion Mode Selection
        Menu modeMenu = new Menu("Expansion Mode");
        ToggleGroup modeGroup = new ToggleGroup();

        RadioMenuItem uiModeItem = new RadioMenuItem("UI Components (Screens, Intents)");
        uiModeItem.setToggleGroup(modeGroup);
        uiModeItem.setSelected(expansionMode == ExpansionMode.UI_COMPONENTS);
        uiModeItem.setOnAction(e -> {
            expansionMode = ExpansionMode.UI_COMPONENTS;
            if (expanded) refreshExpansion();
        });

        RadioMenuItem classModeItem = new RadioMenuItem("Class Dependencies (Structure)");
        classModeItem.setToggleGroup(modeGroup);
        classModeItem.setSelected(expansionMode == ExpansionMode.CLASS_DEPENDENCIES);
        classModeItem.setOnAction(e -> {
            expansionMode = ExpansionMode.CLASS_DEPENDENCIES;
            if (expanded) refreshExpansion();
        });

        modeMenu.getItems().addAll(uiModeItem, classModeItem);
        contextMenu.getItems().addAll(expandItem, new SeparatorMenuItem(), modeMenu);

        nodeContainer.setOnContextMenuRequested(e -> contextMenu.show(nodeContainer, e.getScreenX(), e.getScreenY()));
    }

    public void expand() {
        if (expanded) return;
        expanded = true;
        createChildNodes();
        animateChildAppearance();
    }

    public void collapse() {
        if (!expanded) return;
        expanded = false;
        removeChildNodes();
    }

    public void toggleExpansion() {
        if (expanded) collapse(); else expand();
    }

    public void refreshExpansion() {
        if (expanded) {
            collapse();
            expand();
        }
    }


    private void createChildNodes() {
        List<CodeComponent> childComponents = getChildComponents();

        if (isIntentNode()) {
            // Intent node: build a list of target components, then place radially
            List<String> targets = extractTargetActivitiesFromIntent();
            if (targets == null || targets.isEmpty()) { toggleExpansion(); return; }

            List<CodeComponent> toRender = new ArrayList<>();
            for (String targetName : targets) {
                CodeComponent targetComp = findOrCreateTargetComponent(targetName);
                if (targetComp == null) {
                    // Build a placeholder if necessary
                    CodeComponent placeholder = new CodeComponent();
                    placeholder.setId(targetName);
                    placeholder.setName(targetName);
                    placeholder.setType("Activity");
                    placeholder.setLayer("UI");
                    targetComp = placeholder;
                }
                final String nameToCheck = targetComp.getName();
                boolean exists = childComponents.stream()
                        .anyMatch(c -> c.getName() != null && c.getName().equals(nameToCheck));
                if (!exists) toRender.add(targetComp);
            }

            int n = toRender.size();
            if (n == 0) { expanded = true; triggerGlobalLayoutRefresh(); return; }
            double radius = 200 + n * 20;

            for (int i = 0; i < n; i++) {
                CodeComponent cc = toRender.get(i);
                GraphNode childNode = new GraphNode(cc, canvas);

                double angle = (2 * Math.PI * i) / Math.max(n, 1);
                double childX = nodeContainer.getLayoutX() + Math.cos(angle) * radius;
                double childY = nodeContainer.getLayoutY() + Math.sin(angle) * radius;

                childNode.getContainer().setLayoutX(childX);
                childNode.getContainer().setLayoutY(childY);

                Line connectionLine = createSemanticConnectionLine(nodeContainer, childNode.getContainer(), cc);
                connectionLines.add(connectionLine);
                canvas.getChildren().add(connectionLine);
                connectionLine.toBack();

                canvas.getChildren().add(childNode.getContainer());
                children.add(childNode);
            }
            expanded = true;
            triggerGlobalLayoutRefresh();
            return;
        }

        // Non-intent nodes: radial layout of children
        int n = childComponents.size();
        if (n == 0) return;
        double radius = 200 + n * 20;

        for (int i = 0; i < n; i++) {
            CodeComponent childComp = childComponents.get(i);
            GraphNode childNode = new GraphNode(childComp, canvas);

            double angle = (2 * Math.PI * i) / n;
            double childX = nodeContainer.getLayoutX() + Math.cos(angle) * radius;
            double childY = nodeContainer.getLayoutY() + Math.sin(angle) * radius;

            childNode.getContainer().setLayoutX(childX);
            childNode.getContainer().setLayoutY(childY);

            Line connectionLine = createSemanticConnectionLine(nodeContainer, childNode.getContainer(), childComp);
            connectionLines.add(connectionLine);
            canvas.getChildren().add(connectionLine);
            connectionLine.toBack();

            canvas.getChildren().add(childNode.getContainer());
            children.add(childNode);
        }

        triggerGlobalLayoutRefresh();
    }

    private void triggerGlobalLayoutRefresh() {
        if (canvas.getUserData() instanceof GraphManager) {
            GraphManager gm = (GraphManager) canvas.getUserData();
            gm.refreshLayout();
        }
    }


    private CodeComponent findCanonicalComponent(String simpleName) {
        if (canvas.getUserData() instanceof GraphManager) {
            GraphManager gm = (GraphManager) canvas.getUserData();
            // Try specific lookup
            CodeComponent exact = gm.getCanonicalComponent(simpleName);
            if (exact != null) return exact;

            // Try fuzzy lookup for simple class names
            if (!simpleName.contains(":")) {
                for (CodeComponent c : gm.getNodeMap().values().stream().map(GraphNode::getComponent).collect(Collectors.toList())) {
                    if (c.getName().equals(simpleName)) return c;
                }
            }
        }
        return null;
    }

    private Line createSemanticConnectionLine(VBox parent, VBox child, CodeComponent childComp) {
        Line line = new Line();
        line.startXProperty().bind(parent.layoutXProperty().add(parent.widthProperty().divide(2)));
        line.startYProperty().bind(parent.layoutYProperty().add(parent.heightProperty().divide(2)));
        line.endXProperty().bind(child.layoutXProperty().add(child.widthProperty().divide(2)));
        line.endYProperty().bind(child.layoutYProperty().add(child.heightProperty().divide(2)));
        line.setStrokeWidth(2);

        if (childComp.getId().startsWith("action:")) {
            line.setStroke(Color.ORANGE);
            line.setStrokeWidth(2);
            line.getStrokeDashArray().addAll(10d, 5d);
        } else if (component.getNavigationTargets().contains(childComp.getName()) ||
                component.getNavigationTargets().contains(childComp.getId())) {
            line.setStroke(Color.GREEN);
            line.setStrokeWidth(3);
            line.getStrokeDashArray().addAll(5d, 5d);
        } else if (checkInheritance(childComp)) {
            line.setStroke(Color.BLUE);
        }

        if (checkInheritance(childComp)) {
            line.setStroke(Color.BLUE);
            line.setStrokeWidth(3);
        } else if (checkInjection(childComp)) {
            line.setStroke(Color.PURPLE);
        } else if ((childComp.getLayer() != null && "UI".equals(childComp.getLayer())) || isIntentNode()) {
            line.setStroke(Color.FORESTGREEN);
            line.getStrokeDashArray().addAll(5d, 5d);
        } else {
            line.setStroke(Color.GRAY);
            line.setStrokeWidth(1.5);
        }

        return line;
    }

    private List<CodeComponent> getChildComponents() {
        List<CodeComponent> list = new ArrayList<>();

        if (expansionMode == ExpansionMode.UI_COMPONENTS) {
            list.addAll(getUIComponents());
        } else {
            list.addAll(getClassDependencies());
        }

        // Deduplicate by name - show each unique component name only once
        java.util.Map<String, CodeComponent> uniqueByName = new java.util.LinkedHashMap<>();
        for (CodeComponent c : list) {
            String key = c.getName();
            if (key == null || key.isEmpty()) {
                key = c.getId();
            }
            if (!uniqueByName.containsKey(key)) {
                uniqueByName.put(key, c);
            }
        }

        return uniqueByName.values().stream()
                .filter(c -> shouldShowInViewMode(c, currentViewMode))
                .limit(12)
                .collect(Collectors.toList());
    }

    private List<CodeComponent> getUIComponents() {
        List<CodeComponent> uiComponents = new ArrayList<>();

        if (component.getIntents() != null) uiComponents.addAll(component.getIntents());

        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (isUIComponent(dep)) uiComponents.add(dep);
            }
        }

        if (component.getLayoutFiles() != null) {
            for (String layout : component.getLayoutFiles()) {
                CodeComponent layoutComp = new CodeComponent();
                layoutComp.setId("layout:" + layout);
                layoutComp.setName(layout);
                layoutComp.setType("Layout");
                layoutComp.setLayer("UI");
                uiComponents.add(layoutComp);
            }
        }

        if (component.getResourcesUsed() != null) {
            for (String res : component.getResourcesUsed()) {
                CodeComponent resComp = new CodeComponent();
                resComp.setId("res:" + res);
                resComp.setName(res);
                resComp.setType("Resource");
                resComp.setLayer("UI");
                uiComponents.add(resComp);
            }
        }
        return uiComponents;
    }

    private List<CodeComponent> getClassDependencies() {
        List<CodeComponent> classDependencies = new ArrayList<>();

        // First, add dependencies from fields with their field names for better identification
        if (component.getFields() != null) {
            for (com.projectvisualizer.model.CodeField field : component.getFields()) {
                if (field.getType() != null && field.getName() != null) {
                    CodeComponent fieldComp = new CodeComponent();
                    String fieldName = field.getName();
                    String fieldType = field.getType();
                    // Use "fieldName: Type" format for better identification
                    fieldComp.setId("field:" + fieldName + ":" + fieldType);
                    fieldComp.setName(fieldName + ": " + fieldType);
                    fieldComp.setType(fieldType);
                    fieldComp.setLayer(isUIType(fieldType) ? "UI" : null);
                    classDependencies.add(fieldComp);
                }
            }
        }

        // Then add dependencies that don't match any field
        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (!isUIComponent(dep)) {
                    // Check if this dependency already exists from fields
                    String depName = dep.getName();
                    boolean existsInFields = classDependencies.stream()
                            .anyMatch(c -> c.getName() != null && c.getName().contains(depName));
                    if (!existsInFields) {
                        classDependencies.add(dep);
                    }
                }
            }
        }

        if (component.getExtendsClass() != null) {
            CodeComponent parent = new CodeComponent();
            parent.setId("extends:" + component.getExtendsClass());
            parent.setName(component.getExtendsClass());
            parent.setType("Parent Class");
            parent.setExtendsClass(component.getExtendsClass());
            classDependencies.add(parent);
        }

        if (component.getImplementsList() != null) {
            for (String iface : component.getImplementsList()) {
                CodeComponent interfaceComp = new CodeComponent();
                interfaceComp.setId("interface:" + iface);
                interfaceComp.setName(iface);
                interfaceComp.setType("Interface");
                classDependencies.add(interfaceComp);
            }
        }
        return classDependencies;
    }

    private boolean isUIType(String type) {
        if (type == null) return false;
        String lowerType = type.toLowerCase();
        return lowerType.contains("view") || lowerType.contains("text") || 
               lowerType.contains("button") || lowerType.contains("image") ||
               lowerType.contains("layout") || lowerType.contains("recycler") ||
               lowerType.contains("adapter") || lowerType.contains("fragment");
    }

    private boolean checkInheritance(CodeComponent child) {
        String childName = child.getName();
        return component.getExtendsClass() != null && component.getExtendsClass().contains(childName);
    }

    private boolean checkInjection(CodeComponent child) {
        return component.getInjectedDependencies().stream()
                .anyMatch(d -> d.contains(child.getName()));
    }

    private boolean isUIComponent(CodeComponent comp) {
        if (comp == null || comp.getName() == null) return false;
        String name = comp.getName().toLowerCase();
        String type = comp.getType() != null ? comp.getType().toLowerCase() : "";
        String layer = comp.getLayer();

        if ("UI".equals(layer)) return true;
        if (type.contains("activity") || type.contains("fragment") || type.contains("layout")) return true;
        return name.endsWith("activity") || name.endsWith("fragment") || name.endsWith("adapter");
    }

    private void removeChildNodes() {
        for (Line line : connectionLines) canvas.getChildren().remove(line);
        connectionLines.clear();
        for (GraphNode child : children) {
            child.removeFromCanvas();
            canvas.getChildren().remove(child.getContainer());
        }
        children.clear();
    }

    public void removeFromCanvas() {
        removeChildNodes();
    }

    public void animateHighlight() {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), nodeContainer);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.2); st.setToY(1.2);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    public boolean isExpanded() { return expanded; }
    public VBox getContainer() { return nodeContainer; }
    public CodeComponent getComponent() { return component; }
    public void setViewMode(String mode) { this.currentViewMode = mode; }
    public void setExpansionMode(ExpansionMode mode) { this.expansionMode = mode; }

    // Expose children for layout purposes (read-only copy)
    public List<GraphNode> getChildrenNodes() {
        return new ArrayList<>(children);
    }

    private void setupEventHandlers() {
        nodeCircle.setOnMouseClicked(e -> {
            if (isIntentNode()) {
                // Intent node: lazy-load expansion on single click
                if (e.getClickCount() == 1) {
                    handleIntentNodeClick();
                }
            } else {
                // Other nodes: expand/collapse on double-click
                if (e.getClickCount() == 2) {
                    toggleExpansion();
                }
            }
        });
        nodeContainer.setOnMousePressed(e -> nodeContainer.toFront());
        nodeContainer.setOnMouseDragged(e -> {
            nodeContainer.setLayoutX(e.getSceneX() - nodeContainer.getWidth()/2);
            nodeContainer.setLayoutY(e.getSceneY() - nodeContainer.getHeight()/2);
        });
    }

    private Color getColorForLayer(String layer) {
        if (layer == null) return Color.LIGHTGRAY;
        switch (layer.toLowerCase()) {
            case "ui": return Color.LIGHTSKYBLUE;
            case "business logic": return Color.LIGHTGREEN;
            case "data": return Color.LIGHTCORAL;
            default: return Color.LIGHTGRAY;
        }
    }

    private String getDisplayName() {
        String name = component.getName();
        if (name == null) return "Unknown";

        // Do not trigger expansions here to preserve lazy-load behavior
        return component.getName();
    }


    private String createTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(component.getType() != null ? component.getType() : "N/A").append("\n");
        sb.append("Layer: ").append(component.getLayer() != null ? component.getLayer() : "N/A");
        if (isIntentNode()) {
            List<String> targets = extractTargetActivitiesFromIntent();
            if (targets != null && !targets.isEmpty()) sb.append("\nTarget(s): ").append(String.join(", ", targets));
        }
        return sb.toString();
    }


    private void animateChildAppearance() {
        for (int i=0; i<children.size(); i++) {
            FadeTransition ft = new FadeTransition(Duration.millis(300), children.get(i).getContainer());
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 50));
            ft.play();
        }
    }

    public boolean shouldShowInViewMode(CodeComponent component, String currentViewMode) {
        if ("ALL".equals(currentViewMode)) return true;
        String category = ComponentCategorizer.detectCategory(component);
        return currentViewMode.equals(category);
    }

    private boolean isIntentNode() {
        String type = component.getType();
        String name = component.getName();
        if (type != null && (type.contains("Intent") || type.contains("Navigation"))) return true;
        if (name != null && (name.contains("Intent") || name.startsWith("nav_") || name.contains("navigate"))) return true;
        return false;
    }

    //
//    private List<String> extractTargetActivitiesFromIntent() {
//        List<String> results = new ArrayList<>();
//        String source = null;
//
//        try {
//            if (component.getCode() != null && !component.getCode().isEmpty()) {
//                source = component.getCode();
//            } else {
//                source = component.getName();
//            }
//        } catch (Exception ex) {
//            source = component.getName();
//        }
//
//        if (source == null) return results;
//
//        System.out.println("GraphNode: Analyzing Intent code/snippet: " + source);
//
//        List<Pattern> patterns = new ArrayList<>();
//
//        // 1) Generic explicit intent (Kotlin or Java) capturing fully-qualified or simple class
//        patterns.add(Pattern.compile("Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)(::class\\.java|\\.class)"));
//        // 2) new Intent(context, TargetActivity.class) (Java)
//        patterns.add(Pattern.compile("new\\s+Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class"));
//        // 3) Kotlin startActivity<TargetActivity>()
//        patterns.add(Pattern.compile("startActivity\\s*<\\s*([A-Z][a-zA-Z0-9_.$]+)\\s*>"));
//        // 4) setClass(this, Target.class)
//        patterns.add(Pattern.compile("setClass\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class\\)"));
//        // 5) setClassName("com.pkg","com.pkg.TargetActivity") or setClassName("com.pkg.TargetActivity")
//        patterns.add(Pattern.compile("setClassName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
//        patterns.add(Pattern.compile("setClassName\\s*\\(\\s*\"([^\"]+)\"\\s*\\)")); // single-arg
//        // 6) setComponent(new ComponentName("com.pkg","com.pkg.TargetActivity"))
//        patterns.add(Pattern.compile("setComponent\\s*\\(\\s*new\\s+ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
//        // 7) ComponentName("com.pkg","com.pkg.TargetActivity")
//        patterns.add(Pattern.compile("ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
//        // 8) NavController.navigate(R.id.action_x_to_y) or navigate(R.id.dest)
//        patterns.add(Pattern.compile("navigate\\s*\\(\\s*R\\.id\\.([a-zA-Z0-9_]+)\\s*\\)"));
//
//        Set<String> found = new LinkedHashSet<>();
//        for (Pattern p : patterns) {
//            Matcher m = p.matcher(source);
//            while (m.find()) {
//                String g = null;
//                for (int i = 1; i <= m.groupCount(); i++) {
//                    try {
//                        if (m.group(i) != null && !m.group(i).isEmpty()) {
//                            g = m.group(i);
//                            break;
//                        }
//                    } catch (Exception ex) { /* ignore */ }
//                }
//                if (g == null) continue;
//
//                if (g.matches("[a-z0-9_]+") || g.startsWith("action_") || g.startsWith("nav_") || g.startsWith("dest_")) {
//                    try {
//                        String conv = convertNavIdToName(g);
//                        if (conv != null && !conv.isEmpty()) {
//                            found.add(conv);
//                            continue;
//                        }
//                    } catch (Exception ex) { }
//                    found.add(g);
//                } else {
//                    String last = g;
//                    if (g.contains(".")) {
//                        last = g.substring(g.lastIndexOf('.') + 1);
//                    }
//                    if (last.endsWith("$")) last = last.replace("$", "");
//                    found.add(last);
//                }
//            }
//        }
//
//        if (found.isEmpty() && component.getDependencies() != null) {
//            for (CodeComponent dep : component.getDependencies()) {
//                if (isTargetComponent(dep)) {
//                    System.out.println("DEBUG: Dependency fallback matched: " + dep.getName());
//                    found.add(dep.getName());
//                }
//            }
//        }
//
//        results.addAll(found);
//        return results;
//    }

    private List<String> extractTargetActivitiesFromIntent() {
        List<String> results = new ArrayList<>();
        String source = null;

        // Prefer a full code snippet field if available
        if (component.getCodeSnippet() != null && !component.getCodeSnippet().isEmpty()) {
            source = component.getCodeSnippet();
        }
        else {
            // fallback to name, but this is weak
            source = component.getName();
        }
        if (source == null) return results;

        System.out.println("GraphNode: Analyzing Intent code/snippet: " + source);

        List<Pattern> patterns = new ArrayList<>();

        // 1) Generic explicit intent (Kotlin or Java) capturing fully-qualified or simple class
        patterns.add(Pattern.compile("Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)(::class\\.java|\\.class)"));
        // 2) new Intent(context, TargetActivity.class) (Java)
        patterns.add(Pattern.compile("new\\s+Intent\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class"));
        // 3) Kotlin startActivity<TargetActivity>()
        patterns.add(Pattern.compile("startActivity\\s*<\\s*([A-Z][a-zA-Z0-9_.$]+)\\s*>"));
        // 4) setClass(this, Target.class)
        patterns.add(Pattern.compile("setClass\\s*\\([^,]+,\\s*([a-zA-Z0-9_.$]+)\\.class\\)"));
        // 5) setClassName("com.pkg","com.pkg.TargetActivity") or setClassName("com.pkg.TargetActivity")
        patterns.add(Pattern.compile("setClassName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
        patterns.add(Pattern.compile("setClassName\\s*\\(\\s*\"([^\"]+)\"\\s*\\)")); // single-arg
        // 6) setComponent(new ComponentName("com.pkg","com.pkg.TargetActivity"))
        patterns.add(Pattern.compile("setComponent\\s*\\(\\s*new\\s+ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
        // 7) setComponent(ComponentName) style with fully-qualified string inside
        patterns.add(Pattern.compile("ComponentName\\s*\\(\\s*\"[^\"]+\"\\s*,\\s*\"([^\"]+)\"\\s*\\)"));
        // 8) NavController.navigate(R.id.action_x_to_y) or navController.navigate(R.id.dest)
        patterns.add(Pattern.compile("navigate\\s*\\(\\s*R\\.id\\.([a-zA-Z0-9_]+)\\s*\\)"));

        Set<String> found = new LinkedHashSet<>();
        for (Pattern p : patterns) {
            Matcher m = p.matcher(source);
            while (m.find()) {
                String g = null;
                // find a plausible group
                for (int i = 1; i <= m.groupCount(); i++) {
                    if (m.group(i) != null) {
                        g = m.group(i);
                        break;
                    }
                }
                if (g == null) continue;
                // If it's a nav id, convert
                if (g.startsWith("action_") || g.startsWith("nav_") || g.startsWith("dest_") || g.matches("[a-z0-9_]+")) {
                    // try convertNavIdToName (exists in class)
                    try {
                        String conv = convertNavIdToName(g);
                        if (conv != null && !conv.isEmpty()) found.add(conv);
                    } catch (Exception ex) {
                        // fallback to raw id
                        found.add(g);
                    }
                } else {
                    // If fully-qualified, keep last simple name for UI
                    if (g.contains(".")) {
                        String last = g.substring(g.lastIndexOf('.') + 1);
                        if (last.endsWith("$")) last = last.replace("$", "");
                        found.add(last);
                    } else {
                        found.add(g);
                    }
                }
            }
        }

        // 9) Fallback: check dependencies attached to component (some parsers attach resolved dependency components)
        if (found.isEmpty() && component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (isTargetComponent(dep)) {
                    found.add(dep.getName());
                }
            }
        }

        results.addAll(found);
        return results;
    }


    private String convertNavIdToName(String id) {
        // Example: "action_home_to_details" -> "Details"
        String[] parts = id.split("_");
        String lastPart = parts[parts.length - 1];

        // Capitalize first letter
        if (lastPart != null && !lastPart.isEmpty()) {
            return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
        }
        return id;
    }

    private boolean isTargetComponent(CodeComponent c) {
        if (c == null) return false;
        String name = c.getName();
        if (name == null) return false;

        // Match common Android component suffixes
        if (name.endsWith("Activity") || name.endsWith("Fragment") || name.endsWith("Screen")) return true;

        // Also check type field if available
        String type = c.getType() != null ? c.getType() : "";
        return type.equalsIgnoreCase("Activity") || type.equalsIgnoreCase("Fragment");
    }


    private void handleIntentNodeClick() {
        // Delegate to standard expand/collapse flow so createChildNodes handles intent targets
        toggleExpansion();
    }


    private CodeComponent findOrCreateTargetComponent(String targetName) {
        if (canvas.getUserData() instanceof GraphManager) {
            GraphManager gm = (GraphManager) canvas.getUserData();
            CodeComponent real = gm.getCanonicalComponent(targetName);
            if (real != null) return real;
        }
        // Fallback stub
        CodeComponent stub = new CodeComponent();
        stub.setId(targetName);
        stub.setName(targetName);
        stub.setType("Activity");
        stub.setLayer("UI");
        return stub;
    }

    private void createTargetActivityNode(CodeComponent targetComp) {
        // Prevent duplicates
        for(GraphNode child : children) {
            if (child.getComponent().getName().equals(targetComp.getName())) return;
        }

        GraphNode targetNode = new GraphNode(targetComp, canvas);
        targetNode.setViewMode(currentViewMode);
        targetNode.setExpansionMode(expansionMode); // Inherit mode!

        // Position
        double childX = nodeContainer.getLayoutX() + 220;
        double childY = nodeContainer.getLayoutY();

        targetNode.getContainer().setLayoutX(childX);
        targetNode.getContainer().setLayoutY(childY);

        Line line = createSemanticConnectionLine(nodeContainer, targetNode.getContainer(), targetComp);

        connectionLines.add(line);
        canvas.getChildren().add(line);
        line.toBack();

        canvas.getChildren().add(targetNode.getContainer());
        children.add(targetNode);

        // Animate
        FadeTransition ft = new FadeTransition(Duration.millis(300), targetNode.getContainer());
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }
    private void createTargetPlaceholderNode(String name) {
        // Create a lightweight placeholder CodeComponent and reuse createTargetActivityNode logic
        CodeComponent placeholder = new CodeComponent();
        try {
            placeholder.setName(name);
            placeholder.setType("Activity");
        } catch (Exception ex) {
            // if setters are not available, ignore
        }

        // Prevent duplicates
        for (GraphNode child : children) {
            if (child.getComponent() != null && child.getComponent().getName() != null
                    && child.getComponent().getName().equals(name)) return;
        }

        try {
            createTargetActivityNode(placeholder);
        } catch (Exception ex) {
            System.out.println("Failed to create placeholder node for: " + name);
        }
    }

}