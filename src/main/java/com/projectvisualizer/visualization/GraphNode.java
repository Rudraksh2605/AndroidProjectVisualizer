// GraphNode.java
package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    private CodeComponent component;
    private Circle nodeCircle;
    private Text nodeLabel;
    private VBox nodeContainer;
    private List<GraphNode> children;
    private List<Line> connectionLines;
    private boolean expanded = false;
    private Pane canvas;

    // Expansion mode: UI_COMPONENTS or CLASS_DEPENDENCIES
    private ExpansionMode expansionMode = ExpansionMode.CLASS_DEPENDENCIES;

    public enum ExpansionMode {
        UI_COMPONENTS("UI Components"),
        CLASS_DEPENDENCIES("Class Dependencies");

        private final String displayName;

        ExpansionMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public GraphNode(CodeComponent component, Pane canvas) {
        this.component = component;
        this.canvas = canvas;
        this.children = new ArrayList<>();
        this.connectionLines = new ArrayList<>();
        initializeNode();
    }

    private void initializeNode() {
        nodeCircle = new Circle(25);
        nodeCircle.setFill(getColorForLayer(component.getLayer()));
        nodeCircle.setStroke(Color.BLACK);
        nodeCircle.setStrokeWidth(1.5);

        nodeLabel = new Text(component.getName());
        nodeLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-alignment: center;");
        nodeLabel.setWrappingWidth(50);

        nodeContainer = new VBox(3);
        nodeContainer.getChildren().addAll(nodeCircle, nodeLabel);
        nodeContainer.setAlignment(Pos.TOP_CENTER);

        nodeContainer.setLayoutX(0);
        nodeContainer.setLayoutY(0);

        setupEventHandlers();
        setupContextMenu();

        Tooltip tooltip = new Tooltip(createTooltipText());
        Tooltip.install(nodeContainer, tooltip);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // Expansion mode selection
        MenuItem expansionModeHeader = new MenuItem("Expansion Mode:");
        expansionModeHeader.setDisable(true);

        ToggleGroup expansionModeGroup = new ToggleGroup();

        RadioMenuItem uiComponentsItem = new RadioMenuItem("Show UI Components");
        uiComponentsItem.setToggleGroup(expansionModeGroup);
        uiComponentsItem.setSelected(expansionMode == ExpansionMode.UI_COMPONENTS);
        uiComponentsItem.setOnAction(e -> {
            expansionMode = ExpansionMode.UI_COMPONENTS;
            // Refresh if currently expanded
            if (expanded) {
                refreshExpansion();
            }
        });

        RadioMenuItem classDependenciesItem = new RadioMenuItem("Show Class Dependencies");
        classDependenciesItem.setToggleGroup(expansionModeGroup);
        classDependenciesItem.setSelected(expansionMode == ExpansionMode.CLASS_DEPENDENCIES);
        classDependenciesItem.setOnAction(e -> {
            expansionMode = ExpansionMode.CLASS_DEPENDENCIES;
            // Refresh if currently expanded
            if (expanded) {
                refreshExpansion();
            }
        });

        contextMenu.getItems().addAll(
                expansionModeHeader,
                uiComponentsItem,
                classDependenciesItem
        );

        nodeContainer.setOnContextMenuRequested(event -> {
            contextMenu.show(nodeContainer, event.getScreenX(), event.getScreenY());
        });
    }

    private void refreshExpansion() {
        if (expanded) {
            collapse();
            expand();
        }
    }

    private void toggleExpansion() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    private void expand() {
        if (expanded) return;

        expanded = true;

        // Animation for expansion
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), nodeContainer);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setToY(1.1);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        scaleTransition.play();

        // Create child nodes based on selected mode
        createChildNodes();

        // Animate the appearance of child nodes
        animateChildAppearance();
    }

    private void collapse() {
        if (!expanded) return;

        expanded = false;

        // Animation for collapse
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), nodeContainer);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.9);
        scaleTransition.setToY(0.9);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        scaleTransition.play();

        // Remove child nodes and connections
        removeChildNodes();
    }

    private void createChildNodes() {
        List<CodeComponent> childComponents = getChildComponents();

        if (childComponents.isEmpty()) {
            return;
        }

        double angleStep = 360.0 / Math.max(1, childComponents.size());
        double radius = 150;
        int index = 0;

        for (CodeComponent childComponent : childComponents) {
            GraphNode childNode = new GraphNode(childComponent, canvas);

            // Position children in a circle around parent
            double angle = Math.toRadians(angleStep * index);
            double childX = nodeContainer.getLayoutX() + radius * Math.cos(angle);
            double childY = nodeContainer.getLayoutY() + radius * Math.sin(angle);

            childNode.getContainer().setLayoutX(childX);
            childNode.getContainer().setLayoutY(childY);

            // Create connection line
            Line connectionLine = createConnectionLine(nodeContainer, childNode.getContainer());
            connectionLines.add(connectionLine);
            canvas.getChildren().add(connectionLine);

            canvas.getChildren().add(childNode.getContainer());
            children.add(childNode);
            index++;
        }
    }

    private List<CodeComponent> getChildComponents() {
        List<CodeComponent> childComponents = new ArrayList<>();

        switch (expansionMode) {
            case UI_COMPONENTS:
                childComponents.addAll(getUIComponents());
                break;
            case CLASS_DEPENDENCIES:
                childComponents.addAll(getClassDependencies());
                break;
        }

        return childComponents;
    }

    private List<CodeComponent> getUIComponents() {
        List<CodeComponent> uiComponents = new ArrayList<>();

        // Add UI resources like strings, dimensions, etc.
        if (component.getResourcesUsed() != null) {
            for (String resource : component.getResourcesUsed()) {
                CodeComponent resourceComponent = new CodeComponent();
                resourceComponent.setId("resource:" + resource);
                resourceComponent.setName(resource);
                resourceComponent.setType("Resource");
                resourceComponent.setLayer("UI");
                uiComponents.add(resourceComponent);
            }
        }

        // Add composables if available
        if (component.getComposablesUsed() != null) {
            for (String composable : component.getComposablesUsed()) {
                CodeComponent composableComponent = new CodeComponent();
                composableComponent.setId("composable:" + composable);
                composableComponent.setName(composable);
                composableComponent.setType("Composable");
                composableComponent.setLayer("UI");
                uiComponents.add(composableComponent);
            }
        }

        // Add view bindings
        if (component.isViewBindingUsed()) {
            CodeComponent bindingComponent = new CodeComponent();
            bindingComponent.setId("viewbinding:" + component.getId());
            bindingComponent.setName("ViewBinding");
            bindingComponent.setType("Binding");
            bindingComponent.setLayer("UI");
            uiComponents.add(bindingComponent);
        }

        // Add data bindings
        if (component.isDataBindingUsed()) {
            CodeComponent dataBindingComponent = new CodeComponent();
            dataBindingComponent.setId("databinding:" + component.getId());
            dataBindingComponent.setName("DataBinding");
            dataBindingComponent.setType("Binding");
            dataBindingComponent.setLayer("UI");
            uiComponents.add(dataBindingComponent);
        }

        return uiComponents;
    }

    private List<CodeComponent> getClassDependencies() {
        List<CodeComponent> classDependencies = new ArrayList<>();

        // Add regular dependencies
        if (component.getDependencies() != null) {
            classDependencies.addAll(component.getDependencies());
        }

        // Add extended class if available
        if (component.getExtendsClass() != null && !component.getExtendsClass().isEmpty()) {
            CodeComponent extendsComponent = new CodeComponent();
            extendsComponent.setId("extends:" + component.getExtendsClass());
            extendsComponent.setName(component.getExtendsClass());
            extendsComponent.setType("Parent Class");
            extendsComponent.setLayer("Inheritance");
            classDependencies.add(extendsComponent);
        }

        // Add implemented interfaces if available
        if (component.getImplementsList() != null) {
            for (String interfaceName : component.getImplementsList()) {
                CodeComponent interfaceComponent = new CodeComponent();
                interfaceComponent.setId("implements:" + interfaceName);
                interfaceComponent.setName(interfaceName);
                interfaceComponent.setType("Interface");
                interfaceComponent.setLayer("Inheritance");
                classDependencies.add(interfaceComponent);
            }
        }

        return classDependencies;
    }

    private void removeChildNodes() {
        // Remove connection lines
        for (Line line : connectionLines) {
            canvas.getChildren().remove(line);
        }
        connectionLines.clear();

        // Remove child nodes recursively
        for (GraphNode child : children) {
            child.removeFromCanvas();
            canvas.getChildren().remove(child.getContainer());
        }
        children.clear();
    }

    void removeFromCanvas() {
        removeChildNodes();
    }

    private Line createConnectionLine(VBox parent, VBox child) {
        Line line = new Line();

        // Bind line to parent and child positions
        line.startXProperty().bind(parent.layoutXProperty().add(parent.widthProperty().divide(2)));
        line.startYProperty().bind(parent.layoutYProperty().add(parent.heightProperty().divide(2)));
        line.endXProperty().bind(child.layoutXProperty().add(child.widthProperty().divide(2)));
        line.endYProperty().bind(child.layoutYProperty().add(child.heightProperty().divide(2)));

        // Style based on expansion mode
        if (expansionMode == ExpansionMode.UI_COMPONENTS) {
            line.setStroke(Color.LIGHTBLUE);
            line.getStrokeDashArray().addAll(3.0, 3.0);
        } else {
            line.setStroke(Color.GRAY);
        }

        line.setStrokeWidth(2);
        return line;
    }

    private void animateChildAppearance() {
        for (int i = 0; i < children.size(); i++) {
            GraphNode child = children.get(i);
            VBox childContainer = child.getContainer();

            FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), childContainer);
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);
            fadeTransition.setDelay(Duration.millis(i * 100));
            fadeTransition.play();

            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(400), childContainer);
            scaleTransition.setFromX(0.1);
            scaleTransition.setFromY(0.1);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.setDelay(Duration.millis(i * 100));
            scaleTransition.play();
        }
    }

    private Color getColorForLayer(String layer) {
        if (layer == null) return Color.LIGHTGRAY;

        switch (layer.toLowerCase()) {
            case "ui":
                return Color.LIGHTSKYBLUE;
            case "business logic":
                return Color.LIGHTGREEN;
            case "data":
                return Color.LIGHTCORAL;
            case "domain":
                return Color.LIGHTYELLOW;
            case "inheritance":
                return Color.PLUM;
            default:
                return Color.LIGHTGRAY;
        }
    }

    private String createTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(component.getName() != null ? component.getName() : "Unknown").append("\n");
        String badge = component.getComponentType() != null ? component.getComponentType() : component.getType();
        sb.append("Type: ").append(badge != null ? badge : "Unknown").append("\n");
        if (component.getPackageName() != null && !component.getPackageName().isEmpty()) {
            sb.append("Package: ").append(component.getPackageName()).append("\n");
        }
        if (component.getModuleName() != null && !component.getModuleName().isEmpty()) {
            sb.append("Module: ").append(component.getModuleName()).append("\n");
        }
        if (component.getFileExtension() != null && !component.getFileExtension().isEmpty()) {
            sb.append("File: .").append(component.getFileExtension()).append("\n");
        }
        sb.append("Layer: ").append(component.getLayer() != null ? component.getLayer() : "Unknown").append("\n");
        if (component.getLanguage() != null) {
            sb.append("Language: ").append(component.getLanguage()).append("\n");
        }
        sb.append("Expansion Mode: ").append(expansionMode.getDisplayName()).append("\n");
        sb.append("Dependencies: ").append(component.getDependencies() != null ? component.getDependencies().size() : 0).append("\n");

        java.util.List<String> hints = new java.util.ArrayList<>();
        if (component.isViewBindingUsed()) hints.add("ViewBinding");
        if (component.isDataBindingUsed()) hints.add("DataBinding");
        if (component.isCoroutineUsage()) hints.add("Coroutines");
        if (component.getApiClients() != null && !component.getApiClients().isEmpty()) hints.add("API: " + String.join(",", component.getApiClients()));
        if (component.getDbDaos() != null && !component.getDbDaos().isEmpty()) hints.add("DB: " + String.join(",", component.getDbDaos()));
        if (component.getComposablesUsed() != null && !component.getComposablesUsed().isEmpty()) hints.add("Composables: " + component.getComposablesUsed().size());
        if (component.getResourcesUsed() != null && !component.getResourcesUsed().isEmpty()) hints.add("Resources: " + component.getResourcesUsed().size());
        if (!hints.isEmpty()) {
            sb.append("\nHints: ").append(String.join(" | ", hints)).append("\n");
        }
        sb.append("\n").append(isUIComponent(component) ? "UI COMPONENT" : "Non-UI Component");
        return sb.toString();
    }

    public VBox getContainer() {
        return nodeContainer;
    }

    public CodeComponent getComponent() {
        return component;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public ExpansionMode getExpansionMode() {
        return expansionMode;
    }

    public void setExpansionMode(ExpansionMode expansionMode) {
        this.expansionMode = expansionMode;
        if (expanded) {
            refreshExpansion();
        }
    }

    private void setupEventHandlers() {
        nodeCircle.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleExpansion();
            }
        });

        // Add drag functionality to prevent overlapping during manual movement
        nodeContainer.setOnMousePressed(event -> {
            // Bring to front when clicked
            nodeContainer.toFront();
            for (Line line : connectionLines) {
                line.toFront();
            }
        });

        nodeContainer.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                double newX = event.getSceneX() - nodeContainer.getWidth() / 2;
                double newY = event.getSceneY() - nodeContainer.getHeight() / 2;

                // Check for collisions with other nodes
                if (!wouldOverlapWithOtherNodes(newX, newY)) {
                    nodeContainer.setLayoutX(newX);
                    nodeContainer.setLayoutY(newY);
                }
            }
        });

        // Update expand arrow to show current mode
        Text expandArrow = new Text("▼");
        expandArrow.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-fill: #666;");
        expandArrow.setOnMouseClicked(event -> toggleExpansion());

        // Add tooltip to expand arrow to show current mode
        Tooltip arrowTooltip = new Tooltip("Double-click to expand\nCurrent mode: " + expansionMode.getDisplayName());
        Tooltip.install(expandArrow, arrowTooltip);

        nodeContainer.getChildren().add(expandArrow);
    }

    // Add this method to check for overlaps during drag
    private boolean wouldOverlapWithOtherNodes(double x, double y) {
        if (canvas == null) return false;

        // Get all nodes except this one
        java.util.List<GraphNode> otherNodes = new ArrayList<>();
        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof VBox && node != nodeContainer) {
                // This is a simplified check - in a real implementation,
                // you'd want to maintain a reference to other GraphNode objects
                double otherX = ((VBox) node).getLayoutX();
                double otherY = ((VBox) node).getLayoutY();
                double otherWidth = node.getBoundsInLocal().getWidth();
                double otherHeight = node.getBoundsInLocal().getHeight();

                double thisWidth = nodeContainer.getBoundsInLocal().getWidth();
                double thisHeight = nodeContainer.getBoundsInLocal().getHeight();

                // Calculate distance between centers
                double centerX1 = x + thisWidth / 2;
                double centerY1 = y + thisHeight / 2;
                double centerX2 = otherX + otherWidth / 2;
                double centerY2 = otherY + otherHeight / 2;

                double distance = Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));

                if (distance < 80) { // Minimum drag distance
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUIComponent(CodeComponent component) {
        if (component == null || component.getName() == null) return false;

        String layer = component.getLayer();
        String name = component.getName().toLowerCase();
        String extendsClass = component.getExtendsClass();

        return "UI".equals(layer) ||
                name.endsWith("activity") ||
                name.endsWith("fragment") ||
                name.endsWith("adapter") ||
                name.endsWith("viewholder") ||
                name.contains("screen") ||
                name.contains("page") ||
                name.contains("dialog") ||
                (extendsClass != null &&
                        (extendsClass.endsWith("Activity") ||
                                extendsClass.endsWith("Fragment") ||
                                extendsClass.contains("android.app.Activity") ||
                                extendsClass.contains("androidx.fragment.app.Fragment")));
    }
}