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
import java.util.List;
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

        // --- KEY FIX: Explicitly handle Intent Node Logic ---
        if (isIntentNode()) {
            System.out.println("GraphNode [" + getDisplayName() + "]: Detected as Intent Node. Attempting to extract target...");

            String targetName = extractTargetActivityFromIntent();
            System.out.println("GraphNode [" + getDisplayName() + "]: Extracted target name: " + targetName);

            if (targetName != null && !targetName.isEmpty()) {
                CodeComponent targetComp = findOrCreateTargetComponent(targetName);
                if (targetComp != null) {
                    System.out.println("GraphNode [" + getDisplayName() + "]: Found/Created target component: " + targetComp.getName());

                    // Avoid duplicates
                    boolean exists = childComponents.stream()
                            .anyMatch(c -> c.getName() != null && c.getName().equals(targetComp.getName()));
                    if (!exists) {
                        childComponents.add(targetComp);
                    }
                } else {
                    System.out.println("GraphNode [" + getDisplayName() + "]: Failed to find/create target component for " + targetName);
                }
            } else {
                // FALLBACK: Aggressive Scan of Dependencies for ANY Activity/Fragment
                System.out.println("GraphNode [" + getDisplayName() + "]: No name extracted. Scanning dependencies for UI components...");
                if (component.getDependencies() != null) {
                    for (CodeComponent dep : component.getDependencies()) {
                        System.out.println("  - Checking dep: " + dep.getName() + " (" + dep.getType() + ")");
                        if (isTargetComponent(dep)) {
                            System.out.println("  -> MATCH! Adding " + dep.getName() + " as target.");
                            childComponents.add(dep);
                        }
                    }
                }
            }
        }

        // --- DEBUG LOGGING ---
        if (childComponents.isEmpty()) {
            System.out.println("GraphNode [" + getDisplayName() + "]: No further nodes to be shown (null/empty)");
        } else {
            System.out.println("GraphNode [" + getDisplayName() + "]: Showing " + childComponents.size() + " further nodes:");
            for (CodeComponent child : childComponents) {
                System.out.println("  -> " + (child.getName() != null ? child.getName() : child.getId()));
            }
        }
        // ---------------------

        if (childComponents.isEmpty()) return;

        // Visual layout settings
        double angleStep = 180.0 / Math.max(1, childComponents.size() - 1);
        double radius = 200;

        double startAngle = 0;
        if ("UI".equals(component.getLayer())) startAngle = 0;
        else if ("Data".equals(component.getLayer())) startAngle = 180;

        int index = 0;
        for (CodeComponent childComp : childComponents) {
            // Retrieve REAL component from Registry
            CodeComponent realChild = childComp;
            if (canvas.getUserData() instanceof GraphManager) {
                GraphManager gm = (GraphManager) canvas.getUserData();
                CodeComponent lookup = gm.getCanonicalComponent(childComp.getId());
                if (lookup != null) realChild = lookup;
            }

            GraphNode childNode = new GraphNode(realChild, canvas);
            childNode.setViewMode(this.currentViewMode);
            childNode.setExpansionMode(this.expansionMode);

            // Calculate position
            double angle = Math.toRadians(startAngle + (index * angleStep));
            double childX = nodeContainer.getLayoutX() + radius * Math.cos(angle);
            double childY = nodeContainer.getLayoutY() + radius * Math.sin(angle) + 80;

            childNode.getContainer().setLayoutX(childX);
            childNode.getContainer().setLayoutY(childY);

            // Create Semantic Connection
            Line connectionLine = createSemanticConnectionLine(nodeContainer, childNode.getContainer(), childComp);

            connectionLines.add(connectionLine);
            canvas.getChildren().add(connectionLine);
            connectionLine.toBack();

            canvas.getChildren().add(childNode.getContainer());
            children.add(childNode);
            index++;
        }
    }

    private Line createSemanticConnectionLine(VBox parent, VBox child, CodeComponent childComp) {
        Line line = new Line();
        line.startXProperty().bind(parent.layoutXProperty().add(parent.widthProperty().divide(2)));
        line.startYProperty().bind(parent.layoutYProperty().add(parent.heightProperty().divide(2)));
        line.endXProperty().bind(child.layoutXProperty().add(child.widthProperty().divide(2)));
        line.endYProperty().bind(child.layoutYProperty().add(child.heightProperty().divide(2)));
        line.setStrokeWidth(2);

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

        return list.stream()
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

        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (!isUIComponent(dep)) classDependencies.add(dep);
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

    private void setupEventHandlers() {
        nodeCircle.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && isIntentNode()) {
                handleIntentNodeClick();
            } else if (e.getClickCount() == 2) {
                toggleExpansion();
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

        if (isIntentNode()) {
            String target = extractTargetActivityFromIntent();
            if (target != null) return "Nav to\n" + target;
        }
        return component.getName();
    }

    private String createTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(component.getType() != null ? component.getType() : "N/A").append("\n");
        sb.append("Layer: ").append(component.getLayer() != null ? component.getLayer() : "N/A");
        if (isIntentNode()) {
            String target = extractTargetActivityFromIntent();
            if (target != null) sb.append("\nTarget: ").append(target);
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

    // --- UPDATED: More Robust Intent Target Extraction ---
    private String extractTargetActivityFromIntent() {
        String name = component.getName();
        if (name == null) return null;

        // 1. Regex to find Class Name before .class or ::class, handling potential spaces
        // Matches "SecondActivity" in "SecondActivity.class" or "SecondActivity :: class.java"
        // Also matches "TargetActivity" in "Intent(context, TargetActivity.class)"
        Pattern pattern = Pattern.compile("([A-Z][a-zA-Z0-9_]*)\\s*(?=\\.class|::\\s*class)");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            System.out.println("DEBUG: Regex 1 matched: " + matcher.group(1));
            return matcher.group(1);
        }

        // 2. Regex to find string literals in navigate("Destination")
        Pattern stringPattern = Pattern.compile("\"([A-Z][a-zA-Z0-9_]*)\"");
        Matcher stringMatcher = stringPattern.matcher(name);
        if (stringMatcher.find()) {
            System.out.println("DEBUG: Regex 2 matched: " + stringMatcher.group(1));
            return stringMatcher.group(1);
        }

        // 3. Regex for generic intent creation: Intent(context, TargetActivity.class)
        // This is useful if the node name is the full code line
        Pattern intentCreationPattern = Pattern.compile("Intent\\s*\\([^,]+,\\s*([A-Z][a-zA-Z0-9_]*)(\\.class|::class\\.java)\\s*\\)");
        Matcher intentMatcher = intentCreationPattern.matcher(name);
        if (intentMatcher.find()) {
            System.out.println("DEBUG: Regex 3 matched: " + intentMatcher.group(1));
            return intentMatcher.group(1);
        }

        // 4. Fallback: Check Dependencies for Targets
        // If the Intent declaration (e.g., "new Intent(...)") was parsed as a node,
        // the target class often appears as a dependency of that node.
        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (isTargetComponent(dep)) {
                    System.out.println("DEBUG: Dependency fallback matched: " + dep.getName());
                    return dep.getName();
                }
            }
        }

        return null;
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
        String target = extractTargetActivityFromIntent();
        if (target != null) {
            CodeComponent targetComp = findOrCreateTargetComponent(target);
            if (targetComp != null) {
                createTargetActivityNode(targetComp);
                expanded = true;
            }
        } else {
            toggleExpansion();
        }
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
}