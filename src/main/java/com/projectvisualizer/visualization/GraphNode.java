package com.projectvisualizer.visualization;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.ExpansionMode;
import com.projectvisualizer.services.ComponentCategorizer;
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
import java.util.function.Predicate;
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

        // Use clean display name instead of raw component name
        nodeLabel = new Text(getDisplayName());
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

    public void expand() {
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

    public void collapse() {
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
            System.out.println("No child components found for " + component.getName() + " in mode: " + expansionMode);
            return;
        }

        double angleStep = 360.0 / Math.max(1, childComponents.size());
        double radius = 150;
        int index = 0;

        for (CodeComponent childComponent : childComponents) {
            // UPDATED: Check for the REAL object in registry to ensure children are expandable too
            CodeComponent realChild = childComponent;
            if (canvas.getUserData() instanceof GraphManager) {
                GraphManager gm = (GraphManager) canvas.getUserData();
                CodeComponent lookup = gm.getCanonicalComponent(childComponent.getId());
                if (lookup != null) {
                    realChild = lookup;
                }
            }

            GraphNode childNode = new GraphNode(realChild, canvas);

            // IMPORTANT: Copy expansion mode and view mode from parent to child
            childNode.setExpansionMode(this.expansionMode);
            childNode.setViewMode(this.currentViewMode);

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

    public void setExpansionMode(ExpansionMode expansionMode) {
        this.expansionMode = expansionMode;
        if (expanded) {
            refreshExpansion();
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

        // Filter by current view mode
        return childComponents.stream()
                .filter(component -> shouldShowInViewMode(component, currentViewMode))
                .collect(Collectors.toList());
    }

    private List<CodeComponent> getUIComponents() {
        List<CodeComponent> uiComponents = new ArrayList<>();

        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (isUIComponent(dep) || isAndroidUIComponent(dep)) {
                    uiComponents.add(dep);
                }
            }
        }

        if (component.getIntents() != null) {
            uiComponents.addAll(component.getIntents());
        }

        if (component.getResourcesUsed() != null) {
            for (String resource : component.getResourcesUsed()) {
                if (isUIResource(resource)) {
                    CodeComponent resourceComponent = new CodeComponent();
                    resourceComponent.setId("resource:" + resource);
                    resourceComponent.setName(resource);
                    resourceComponent.setType("Resource");
                    resourceComponent.setLayer("UI");
                    uiComponents.add(resourceComponent);
                }
            }
        }

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

        if (component.isViewBindingUsed()) {
            CodeComponent bindingComponent = new CodeComponent();
            bindingComponent.setId("viewbinding:" + component.getId());
            bindingComponent.setName("ViewBinding");
            bindingComponent.setType("Binding");
            bindingComponent.setLayer("UI");
            uiComponents.add(bindingComponent);
        }

        if (component.isDataBindingUsed()) {
            CodeComponent dataBindingComponent = new CodeComponent();
            dataBindingComponent.setId("databinding:" + component.getId());
            dataBindingComponent.setName("DataBinding");
            dataBindingComponent.setType("Binding");
            dataBindingComponent.setLayer("UI");
            uiComponents.add(dataBindingComponent);
        }

        if (isUIComponent(component) && component.getFilePath() != null) {
            String filePath = component.getFilePath();
            if (filePath.endsWith(".xml") && filePath.contains("layout")) {
                CodeComponent layoutComponent = new CodeComponent();
                layoutComponent.setId("layout:" + component.getId());
                layoutComponent.setName(filePath.substring(filePath.lastIndexOf("/") + 1));
                layoutComponent.setType("Layout");
                layoutComponent.setLayer("UI");
                uiComponents.add(layoutComponent);
            }
        }

        return uiComponents;
    }

    public boolean shouldShowInViewMode(CodeComponent component, String currentViewMode) {
        if ("ALL".equals(currentViewMode)) return true;
        String category = detectComponentCategory(component);
        return currentViewMode.equals(category);
    }

    private boolean isUIResource(String resource) {
        if (resource == null) return false;
        return resource.startsWith("@string/") ||
                resource.startsWith("@dimen/") ||
                resource.startsWith("@color/") ||
                resource.startsWith("@drawable/") ||
                resource.startsWith("@mipmap/") ||
                resource.startsWith("@layout/") ||
                resource.startsWith("@menu/") ||
                resource.startsWith("@style/");
    }

    private List<CodeComponent> getClassDependencies() {
        List<CodeComponent> classDependencies = new ArrayList<>();

        if (component.getDependencies() != null) {
            for (CodeComponent dep : component.getDependencies()) {
                if (!isUIComponent(dep) && !isAndroidUIComponent(dep)) {
                    classDependencies.add(dep);
                }
            }
        }

        if (component.getExtendsClass() != null && !component.getExtendsClass().isEmpty()) {
            if (!isAndroidUIComponentByName(component.getExtendsClass())) {
                CodeComponent extendsComponent = new CodeComponent();
                extendsComponent.setId("extends:" + component.getExtendsClass());
                extendsComponent.setName(component.getExtendsClass());
                extendsComponent.setType("Parent Class");
                extendsComponent.setLayer("Inheritance");
                classDependencies.add(extendsComponent);
            }
        }

        if (component.getImplementsList() != null) {
            for (String interfaceName : component.getImplementsList()) {
                if (!isAndroidUIComponentByName(interfaceName)) {
                    CodeComponent interfaceComponent = new CodeComponent();
                    interfaceComponent.setId("implements:" + interfaceName);
                    interfaceComponent.setName(interfaceName);
                    interfaceComponent.setType("Interface");
                    interfaceComponent.setLayer("Inheritance");
                    classDependencies.add(interfaceComponent);
                }
            }
        }

        return classDependencies;
    }

    private boolean isAndroidUIComponentByName(String className) {
        if (className == null) return false;
        String lowerName = className.toLowerCase();

        return lowerName.contains("button") ||
                lowerName.contains("textview") ||
                lowerName.contains("edittext") ||
                lowerName.contains("imageview") ||
                lowerName.contains("recyclerview") ||
                lowerName.contains("listview") ||
                lowerName.contains("cardview") ||
                lowerName.contains("constraintlayout") ||
                lowerName.contains("linearlayout") ||
                lowerName.contains("relativelayout") ||
                lowerName.contains("framelayout") ||
                lowerName.contains("scrollview") ||
                lowerName.contains("viewpager") ||
                lowerName.contains("tablayout") ||
                lowerName.contains("navigationview") ||
                lowerName.contains("drawerlayout") ||
                lowerName.contains("coordinatorlayout") ||
                lowerName.contains("appbarlayout") ||
                lowerName.contains("floatingactionbutton") ||
                lowerName.contains("snackbar") ||
                lowerName.contains("bottomnavigationview") ||
                lowerName.contains("toolbar") ||
                lowerName.contains("actionbar") ||
                lowerName.contains("menu") ||
                lowerName.contains("menuitem") ||
                lowerName.contains("dialog") ||
                lowerName.contains("alertdialog") ||
                lowerName.contains("progressbar") ||
                lowerName.contains("seekbar") ||
                lowerName.contains("switch") ||
                lowerName.contains("checkbox") ||
                lowerName.contains("radiobutton") ||
                lowerName.contains("spinner") ||
                lowerName.contains("webview") ||
                lowerName.contains("mapview") ||
                lowerName.contains("surfaceview") ||
                lowerName.contains("textureview") ||
                lowerName.contains("calendarview") ||
                lowerName.contains("datepicker") ||
                lowerName.contains("timepicker") ||
                lowerName.contains("numberpicker") ||
                lowerName.contains("ratingbar") ||
                lowerName.contains("searchview") ||
                lowerName.contains("videoview");
    }

    private boolean isAndroidUIComponent(CodeComponent component) {
        if (component == null) return false;

        if (isAndroidUIComponentByName(component.getName())) {
            return true;
        }

        String packageName = component.getPackageName();
        if (packageName != null) {
            return packageName.startsWith("android.") ||
                    packageName.startsWith("androidx.") ||
                    packageName.contains(".widget.") ||
                    packageName.contains(".view.") ||
                    packageName.contains(".custom.");
        }

        String filePath = component.getFilePath();
        if (filePath != null) {
            return filePath.contains("/res/") ||
                    filePath.contains("/layout/") ||
                    filePath.contains("/drawable/") ||
                    filePath.contains("/menu/") ||
                    filePath.contains("/values/");
        }

        return false;
    }

    private boolean isUIComponent(CodeComponent component) {
        if (component == null || component.getName() == null) return false;

        String layer = component.getLayer();
        String name = component.getName().toLowerCase();
        String extendsClass = component.getExtendsClass();
        String type = component.getType();

        if ("UI".equals(layer)) {
            return true;
        }

        if (type != null) {
            String lowerType = type.toLowerCase();
            if (lowerType.contains("activity") ||
                    lowerType.contains("fragment") ||
                    lowerType.contains("adapter") ||
                    lowerType.contains("viewholder") ||
                    lowerType.contains("view") ||
                    lowerType.contains("layout") ||
                    lowerType.contains("dialog") ||
                    lowerType.contains("menu")) {
                return true;
            }
        }

        boolean isUIByName = name.endsWith("activity") ||
                name.endsWith("fragment") ||
                name.endsWith("adapter") ||
                name.endsWith("viewholder") ||
                name.contains("screen") ||
                name.contains("page") ||
                name.contains("dialog") ||
                name.contains("view") ||
                name.contains("layout") ||
                name.contains("button") ||
                name.contains("text") ||
                name.contains("image") ||
                name.contains("list") ||
                name.contains("recycler") ||
                name.contains("card");

        boolean isUIByExtends = extendsClass != null &&
                (extendsClass.endsWith("Activity") ||
                        extendsClass.endsWith("Fragment") ||
                        extendsClass.contains("android.app.Activity") ||
                        extendsClass.contains("androidx.fragment.app.Fragment") ||
                        extendsClass.contains("android.view.View") ||
                        extendsClass.contains("android.widget.") ||
                        extendsClass.contains("androidx.recyclerview.widget."));

        return isUIByName || isUIByExtends;
    }

    private void removeChildNodes() {
        for (Line line : connectionLines) {
            canvas.getChildren().remove(line);
        }
        connectionLines.clear();

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

        line.startXProperty().bind(parent.layoutXProperty().add(parent.widthProperty().divide(2)));
        line.startYProperty().bind(parent.layoutYProperty().add(parent.heightProperty().divide(2)));
        line.endXProperty().bind(child.layoutXProperty().add(child.widthProperty().divide(2)));
        line.endYProperty().bind(child.layoutYProperty().add(child.heightProperty().divide(2)));

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

    private String getDisplayName() {
        if (component == null) return "Unknown";

        String name = component.getName();
        if (name == null) {
            name = component.getId();
            if (name == null) return "Unknown";
        }

        if (isIntentNode()) {
            String cleanName = cleanDisplayName(name);
            String target = extractTargetActivityFromIntent();
            String navType = getNavigationType();

            if (target != null && !target.isEmpty()) {
                return navType + " to " + cleanDisplayName(target);
            } else {
                return createDescriptiveNavigationName(cleanName, navType);
            }
        }

        return cleanDisplayName(name);
    }

    private String cleanDisplayName(String name) {
        if (name == null) return "Unknown";

        String cleaned = name.trim();
        cleaned = cleaned.replaceAll("(?i)(action_|r\\.id\\.|navigate\\(|startactivity\\(|new\\s+)", "");
        cleaned = cleaned.replaceAll("\\.(java|kt|xml|class)$", "");
        cleaned = cleaned.replaceAll("::class\\.java", "");
        cleaned = cleaned.replaceAll("\\(\\)", "");
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s_]", " ");
        cleaned = cleaned.replaceAll("_", " ");
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = capitalizeWords(cleaned);
        cleaned = fixCommonAbbreviations(cleaned);

        return cleaned.trim();
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.split("\\s");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    private String fixCommonAbbreviations(String text) {
        if (text == null) return text;

        return text.replaceAll("\\bFrag\\b", "Fragment")
                .replaceAll("\\bAct\\b", "Activity")
                .replaceAll("\\bActv\\b", "Activity")
                .replaceAll("\\bFrg\\b", "Fragment")
                .replaceAll("\\bVm\\b", "ViewModel")
                .replaceAll("\\bRepo\\b", "Repository")
                .replaceAll("\\bAdapter\\b", "Adapter")
                .replaceAll("\\bImpl\\b", "Implementation")
                .replaceAll("\\bInt\\b", "Intent")
                .replaceAll("\\bNav\\b", "Navigation");
    }

    private String createTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getDisplayName()).append("\n");

        String badge = component.getComponentType() != null ? component.getComponentType() : component.getType();
        sb.append("Type: ").append(badge != null ? cleanDisplayName(badge) : "Unknown").append("\n");

        if (isIntentNode()) {
            String targetActivity = extractTargetActivityFromIntent();
            String navType = getNavigationType();

            sb.append("\n[NAVIGATION - ").append(navType).append("]");
            if (targetActivity != null && !targetActivity.isEmpty()) {
                sb.append("\nTarget: ").append(cleanDisplayName(targetActivity));
            } else {
                sb.append("\nTarget: Unknown");
            }

            String methodDetails = getNavigationMethodDetails();
            if (methodDetails != null) {
                sb.append("\nMethod: ").append(methodDetails);
            }

            sb.append("\nClick to navigate to target");
        }

        if (component.getPackageName() != null && !component.getPackageName().isEmpty()) {
            sb.append("Package: ").append(component.getPackageName()).append("\n");
        }

        sb.append("Layer: ").append(component.getLayer() != null ? component.getLayer() : "Unknown").append("\n");

        return sb.toString();
    }

    private String getNavigationType() {
        String name = component.getName().toLowerCase();

        if (name.contains("fragmenttransaction") || name.contains("replace") || name.contains("addtobackstack")) {
            return "Fragment Transaction";
        } else if (name.contains("navcontroller") || name.contains("findnavcontroller") || name.contains("action_")) {
            return "Navigation Component";
        } else if (name.contains("pendingintent")) {
            return "Pending Intent";
        } else if (name.contains("deeplink") || name.contains("action_view")) {
            return "Deep Link";
        } else if (name.contains("activityresult") || name.contains("registerforactivityresult")) {
            return "Activity Result";
        } else if (name.contains("compose") || name.contains("composable")) {
            return "Compose Navigation";
        } else if (name.contains("intent")) {
            return "Intent";
        } else {
            return "Navigation";
        }
    }

    private String getNavigationMethodDetails() {
        String name = component.getName().toLowerCase();

        if (name.contains("startactivity")) return "startActivity()";
        if (name.contains("findnavcontroller")) return "NavController.navigate()";
        if (name.contains("fragmenttransaction")) return "FragmentTransaction.replace()";
        if (name.contains("pendingintent")) return "PendingIntent.getActivity()";
        if (name.contains("activityresult")) return "Activity Result API";
        if (name.contains("deeplink")) return "Deep Link (ACTION_VIEW)";
        if (name.contains("compose")) return "Compose Navigation";

        return "Intent Navigation";
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

    private void setupEventHandlers() {
        nodeCircle.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && isIntentNode()) {
                handleIntentNodeClick();
                event.consume();
            } else if (event.getClickCount() == 2) {
                toggleExpansion();
            }
        });

        nodeContainer.setOnMousePressed(event -> {
            nodeContainer.toFront();
            for (Line line : connectionLines) {
                line.toFront();
            }
        });

        nodeContainer.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                double newX = event.getSceneX() - nodeContainer.getWidth() / 2;
                double newY = event.getSceneY() - nodeContainer.getHeight() / 2;

                if (!wouldOverlapWithOtherNodes(newX, newY)) {
                    nodeContainer.setLayoutX(newX);
                    nodeContainer.setLayoutY(newY);
                }
            }
        });

        Text expandArrow = new Text("▼");
        expandArrow.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-fill: #666;");
        expandArrow.setOnMouseClicked(event -> toggleExpansion());

        Tooltip arrowTooltip = new Tooltip("Double-click to expand\nCurrent mode: " + expansionMode.getDisplayName());
        Tooltip.install(expandArrow, arrowTooltip);

        nodeContainer.getChildren().add(expandArrow);

        updateIntentTooltip();
    }

    private boolean wouldOverlapWithOtherNodes(double x, double y) {
        if (canvas == null) return false;

        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof VBox && node != nodeContainer) {
                double otherX = ((VBox) node).getLayoutX();
                double otherY = ((VBox) node).getLayoutY();
                double otherWidth = node.getBoundsInLocal().getWidth();
                double otherHeight = node.getBoundsInLocal().getHeight();

                double thisWidth = nodeContainer.getBoundsInLocal().getWidth();
                double thisHeight = nodeContainer.getBoundsInLocal().getHeight();

                double centerX1 = x + thisWidth / 2;
                double centerY1 = y + thisHeight / 2;
                double centerX2 = otherX + otherWidth / 2;
                double centerY2 = otherY + otherHeight / 2;

                double distance = Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));

                if (distance < 80) {
                    return true;
                }
            }
        }
        return false;
    }


    private void handleIntentNodeClick() {
        if (isIntentNode()) {
            String targetActivity = extractTargetActivityFromIntent();
            if (targetActivity != null && !targetActivity.isEmpty()) {
                CodeComponent targetComponent = findOrCreateTargetComponent(targetActivity);
                if (targetComponent != null) {
                    createTargetActivityNode(targetComponent);
                }
            }
        }
    }

    private boolean isIntentNode() {
        if (component == null) return false;

        String type = component.getType();
        String name = component.getName();

        if (type == null || name == null) return false;

        boolean isIntentType = type.equalsIgnoreCase("Intent") ||
                type.toLowerCase().contains("intent") ||
                type.equalsIgnoreCase("Navigation") ||
                type.equalsIgnoreCase("FragmentTransaction") ||
                type.equalsIgnoreCase("ActivityResult") ||
                type.equalsIgnoreCase("PendingIntent") ||
                type.equalsIgnoreCase("DeepLink") ||
                type.equalsIgnoreCase("NavController") ||
                type.equalsIgnoreCase("ComposeNavigation");

        boolean isIntentName = name.toLowerCase().contains("intent") ||
                name.toLowerCase().contains("navigate") ||
                name.toLowerCase().contains("navigation") ||
                name.toLowerCase().contains("launch") ||
                name.toLowerCase().contains("open") ||
                name.toLowerCase().contains("start") ||
                name.toLowerCase().contains("goto") ||
                name.toLowerCase().contains("goTo") ||
                name.toLowerCase().contains("findnavcontroller") ||
                name.toLowerCase().contains("navcontroller") ||
                name.toLowerCase().contains("fragmenttransaction") ||
                name.toLowerCase().contains("replace") ||
                name.toLowerCase().contains("addtobackstack") ||
                name.toLowerCase().contains("pendingintent") ||
                name.toLowerCase().contains("deeplink") ||
                name.toLowerCase().contains("activityresult") ||
                name.toLowerCase().contains("registerforactivityresult") ||
                name.toLowerCase().contains("composenavigation") ||
                (name.toLowerCase().contains("action_") && name.toLowerCase().contains("to_")) ||
                name.toLowerCase().contains("r.id.action_");

        return isIntentType || isIntentName;
    }

    private String extractTargetActivityFromIntent() {
        if (component == null || component.getName() == null) return null;

        String name = component.getName();
        String originalName = name;

        if (component.getDependencies() != null && !component.getDependencies().isEmpty()) {
            for (CodeComponent dep : component.getDependencies()) {
                if (dep != null && isTargetComponent(dep)) {
                    String targetName = extractMeaningfulName(dep.getName());
                    if (isValidTargetName(targetName)) {
                        return targetName;
                    }
                }
            }
        }

        String navTarget = extractNavigationComponentTarget(name);
        if (isValidTargetName(navTarget)) return navTarget;

        String fragmentTarget = extractFragmentTransactionTarget(name);
        if (isValidTargetName(fragmentTarget)) return fragmentTarget;

        String activityTarget = extractActivityIntentTarget(name);
        if (isValidTargetName(activityTarget)) return activityTarget;

        String composeTarget = extractComposeNavigationTarget(name);
        if (isValidTargetName(composeTarget)) return composeTarget;

        String patternTarget = extractFromCommonPatterns(name);
        if (isValidTargetName(patternTarget)) return patternTarget;

        String propertyTarget = getComponentProperty("targetActivity");
        if (isValidTargetName(propertyTarget)) return propertyTarget;

        String fallbackTarget = extractAnyMeaningfulName(originalName);
        if (fallbackTarget != null && !fallbackTarget.equals("TargetScreen")) {
            return fallbackTarget;
        }

        return null;
    }

    private String extractFromCommonPatterns(String name) {
        if (name == null) return null;

        String[] prefixes = {"navigateTo", "goTo", "open", "launch", "start", "show"};
        for (String prefix : prefixes) {
            if (name.toLowerCase().contains(prefix.toLowerCase())) {
                int index = name.toLowerCase().indexOf(prefix.toLowerCase());
                String afterPrefix = name.substring(index + prefix.length());
                String target = extractFirstMeaningfulWord(afterPrefix);
                if (target != null) {
                    return formatTargetName(target);
                }
            }
        }

        String[] suffixes = {"Intent", "Navigation", "Navigate", "Nav"};
        for (String suffix : suffixes) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                String beforeSuffix = name.substring(0, name.length() - suffix.length());
                String target = extractFirstMeaningfulWord(beforeSuffix);
                if (target != null) {
                    return formatTargetName(target);
                }
            }
        }

        if (name.contains("action_") && name.contains("_to_")) {
            String[] parts = name.split("_to_");
            if (parts.length > 1) {
                String targetPart = parts[1];
                targetPart = targetPart.split("[_\\[\\]()]")[0];
                return formatTargetName(targetPart);
            }
        }

        if (name.contains("R.id.action_") && name.contains("_to_")) {
            String targetPart = name.replaceAll(".*_to_", "");
            targetPart = targetPart.split("[^a-zA-Z]")[0];
            return formatTargetName(targetPart);
        }

        return null;
    }

    private boolean isTargetComponent(CodeComponent component) {
        if (component == null) return false;

        String type = component.getType();
        String name = component.getName();

        if (type == null || name == null) return false;

        return type.equals("Activity") ||
                type.equals("Fragment") ||
                type.equals("Composable") ||
                type.equals("Screen") ||
                type.equals("Destination") ||
                name.toLowerCase().contains("activity") ||
                name.toLowerCase().contains("fragment") ||
                name.toLowerCase().contains("composable") ||
                name.toLowerCase().contains("screen");
    }

    private String extractNavigationComponentTarget(String cleanName) {
        if (cleanName.toLowerCase().contains("action_")) {
            String[] parts = cleanName.split("_");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equalsIgnoreCase("to") && i + 1 < parts.length) {
                    return capitalizeWords(parts[i + 1]) + "Fragment";
                }
            }
        }

        if (cleanName.toLowerCase().contains("r.id.action_")) {
            String[] parts = cleanName.split("_");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equalsIgnoreCase("to") && i + 1 < parts.length) {
                    return capitalizeWords(parts[i + 1]);
                }
            }
        }

        if (cleanName.toLowerCase().contains("navigate(r.id.")) {
            String target = cleanName.replaceAll(".*navigate\\(r\\.id\\.(action_)?", "")
                    .replaceAll("[^a-zA-Z].*", "");
            if (!target.isEmpty()) {
                return extractTargetFromActionId(target);
            }
        }

        return null;
    }

    private String extractFragmentTransactionTarget(String cleanName) {
        if (cleanName.toLowerCase().contains("replace") &&
                cleanName.toLowerCase().contains("new ")) {
            String target = cleanName.replaceAll(".*new\\s+", "")
                    .replaceAll("\\(.*", "")
                    .replaceAll("[^a-zA-Z].*", "");
            if (!target.isEmpty() && target.toLowerCase().contains("fragment")) {
                return target;
            }
        }

        if (cleanName.toLowerCase().contains("fragment") &&
                cleanName.contains("(") && cleanName.contains(")")) {
            String target = cleanName.replaceAll(".*\\b([A-Z][a-zA-Z]*Fragment)\\(.*", "$1");
            if (!target.equals(cleanName) && target.toLowerCase().contains("fragment")) {
                return target;
            }
        }

        return null;
    }

    private String extractActivityIntentTarget(String cleanName) {
        if (cleanName.toLowerCase().contains("intent") &&
                cleanName.contains(",")) {
            String target = cleanName.replaceAll(".*,\\s*", "")
                    .replaceAll("\\.class.*", "")
                    .replaceAll("::class.*", "")
                    .trim();
            if (!target.isEmpty() && !target.equals(cleanName)) {
                return target;
            }
        }

        String[] patterns = {
                "To", "NavigateTo", "Open", "Launch", "Start", "Goto", "GoTo",
                "Action", "Destination", "Screen", "Route"
        };

        for (String pattern : patterns) {
            if (cleanName.contains(pattern)) {
                String[] parts = cleanName.split(pattern);
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    String target = parts[1].trim();
                    target = target.replaceAll("[^a-zA-Z0-9].*", "");
                    if (!target.isEmpty()) {
                        if (!target.toLowerCase().contains("activity") &&
                                !target.toLowerCase().contains("fragment") &&
                                !target.toLowerCase().contains("composable")) {
                            target += "Activity";
                        }
                        return target;
                    }
                }
            }
        }

        return null;
    }

    private String extractComposeNavigationTarget(String cleanName) {
        if (cleanName.toLowerCase().contains("navigate(\"")) {
            String target = cleanName.replaceAll(".*navigate\\(\"", "")
                    .replaceAll("\".*", "");
            if (!target.isEmpty()) {
                return capitalizeWords(target) + "Screen";
            }
        }

        if (cleanName.toLowerCase().contains("composable(\"")) {
            String target = cleanName.replaceAll(".*composable\\(\"", "")
                    .replaceAll("\".*", "");
            if (!target.isEmpty()) {
                return capitalizeWords(target) + "Screen";
            }
        }

        return null;
    }

    private String extractTargetFromActionId(String actionId) {
        if (actionId == null) return null;

        String cleanAction = actionId.replace("action_", "");
        String[] parts = cleanAction.split("_");

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("to") && i + 1 < parts.length) {
                return capitalizeWords(parts[i + 1]);
            }
        }

        if (parts.length > 0) {
            return capitalizeWords(parts[parts.length - 1]);
        }

        return null;
    }

    private CodeComponent findOrCreateTargetComponent(String targetActivity) {
        // UPDATED: Use the central registry in GraphManager to find the REAL component
        if (canvas.getUserData() instanceof GraphManager) {
            GraphManager graphManager = (GraphManager) canvas.getUserData();

            // Try to find by canonical ID or simple name using new smart lookup
            CodeComponent realComponent = graphManager.getCanonicalComponent(targetActivity);

            if (realComponent != null) {
                return realComponent;
            }
        }

        // Fallback: Create a stub only if we absolutely cannot find the real code
        CodeComponent targetComponent = new CodeComponent();
        targetComponent.setId("activity:" + targetActivity);
        targetComponent.setName(targetActivity);
        targetComponent.setType("Activity");
        targetComponent.setLayer("UI");

        if (targetActivity.toLowerCase().contains("activity")) {
            targetComponent.setLanguage("Java");
            targetComponent.setComponentType("Activity");
        } else if (targetActivity.toLowerCase().contains("fragment")) {
            targetComponent.setComponentType("Fragment");
        }

        return targetComponent;
    }

    private void createTargetActivityNode(CodeComponent targetComponent) {
        GraphNode targetNode = new GraphNode(targetComponent, canvas);

        double intentX = nodeContainer.getLayoutX();
        double intentY = nodeContainer.getLayoutY();

        double targetX = intentX + 200;
        double targetY = intentY;

        targetNode.getContainer().setLayoutX(targetX);
        targetNode.getContainer().setLayoutY(targetY);

        Line intentConnection = createIntentConnectionLine(nodeContainer, targetNode.getContainer());
        connectionLines.add(intentConnection);
        canvas.getChildren().add(intentConnection);

        canvas.getChildren().add(targetNode.getContainer());
        children.add(targetNode);

        animateTargetNodeAppearance(targetNode);
    }

    private Line createIntentConnectionLine(VBox source, VBox target) {
        Line line = new Line();

        line.startXProperty().bind(source.layoutXProperty().add(source.widthProperty().divide(2)));
        line.startYProperty().bind(source.layoutYProperty().add(source.heightProperty().divide(2)));
        line.endXProperty().bind(target.layoutXProperty().add(target.widthProperty().divide(2)));
        line.endYProperty().bind(target.layoutYProperty().add(target.heightProperty().divide(2)));

        line.setStroke(Color.DARKORANGE);
        line.setStrokeWidth(3);
        line.getStrokeDashArray().addAll(5.0, 5.0);

        line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        return line;
    }

    private void animateTargetNodeAppearance(GraphNode targetNode) {
        VBox targetContainer = targetNode.getContainer();

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(500), targetContainer);
        scaleTransition.setFromX(0.1);
        scaleTransition.setFromY(0.1);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), targetContainer);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);

        ScaleTransition pulseTransition = new ScaleTransition(Duration.millis(300), targetContainer);
        pulseTransition.setFromX(1.0);
        pulseTransition.setFromY(1.0);
        pulseTransition.setToX(1.2);
        pulseTransition.setToY(1.2);
        pulseTransition.setAutoReverse(true);
        pulseTransition.setCycleCount(2);

        javafx.animation.SequentialTransition sequentialTransition =
                new javafx.animation.SequentialTransition(scaleTransition, pulseTransition);
        javafx.animation.ParallelTransition parallelTransition =
                new javafx.animation.ParallelTransition(sequentialTransition, fadeTransition);
        parallelTransition.play();
    }

    private void updateIntentTooltip() {
        if (isIntentNode()) {
            String targetActivity = extractTargetActivityFromIntent();
            if (targetActivity != null) {
                Tooltip tooltip = new Tooltip("Intent Navigation\nTarget: " + targetActivity +
                        "\nClick to navigate to target activity");
                Tooltip.install(nodeContainer, tooltip);
            }
        }
    }


    private String getComponentProperty(String propertyName) {
        if (component == null) return null;

        String name = component.getName();
        String type = component.getType();

        if (name == null) return null;

        switch (propertyName) {
            case "targetActivity":
                if (name.contains("To") || name.contains("Navigate")) {
                    String[] parts = name.split("To|Navigate");
                    if (parts.length > 1) {
                        return parts[1].replace("Intent", "")
                                .replace("Navigation", "")
                                .trim();
                    }
                }
                break;

            case "navigationDestination":
                if (name.contains("action_")) {
                    String[] parts = name.split("_");
                    for (int i = 0; i < parts.length; i++) {
                        if ("to".equals(parts[i]) && i + 1 < parts.length) {
                            return parts[i + 1];
                        }
                    }
                }
                break;
        }

        return null;
    }

    private String extractFirstMeaningfulWord(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        text = text.replaceAll("^[^a-zA-Z]*", "");

        String[] words = text.split("[^a-zA-Z0-9]");
        if (words.length > 0 && !words[0].isEmpty()) {
            String word = words[0];
            if (word.length() > 2 && !isCommonWord(word)) {
                return word;
            }
        }

        return null;
    }

    private boolean isCommonWord(String word) {
        if (word == null) return true;
        String lower = word.toLowerCase();
        return lower.equals("the") || lower.equals("and") || lower.equals("or") ||
                lower.equals("to") || lower.equals("from") || lower.equals("with") ||
                lower.equals("for") || lower.equals("this") || lower.equals("that");
    }

    private String formatTargetName(String baseName) {
        if (baseName == null || baseName.isEmpty()) return null;

        String formatted = baseName.substring(0, 1).toUpperCase() +
                baseName.substring(1).toLowerCase();

        if (!formatted.toLowerCase().endsWith("activity") &&
                !formatted.toLowerCase().endsWith("fragment") &&
                !formatted.toLowerCase().endsWith("screen") &&
                !formatted.toLowerCase().endsWith("composable")) {

            formatted += "Activity";
        }

        return formatted;
    }

    private String extractAnyMeaningfulName(String name) {
        if (name == null) return null;

        String[] words = name.split("(?<=[a-z])(?=[A-Z])|[_\\s]+");

        for (String word : words) {
            if (word.length() > 3 && !isCommonWord(word) &&
                    !word.equalsIgnoreCase("intent") &&
                    !word.equalsIgnoreCase("navigation") &&
                    !word.equalsIgnoreCase("navigate") &&
                    !word.equalsIgnoreCase("open") &&
                    !word.equalsIgnoreCase("launch")) {

                String candidate = formatTargetName(word);
                if (isValidTargetName(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private String extractMeaningfulName(String name) {
        if (name == null) return null;

        name = name.replaceAll("^(R\\.id\\.|R\\.string\\.|@string/|@id/|action_|navigate|goTo|open|launch)", "")
                .replaceAll("\\.(java|kt|class|xml)$", "")
                .replaceAll("::class.*", "");

        String[] parts = name.split("[./_]");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            if (isValidTargetName(lastPart)) {
                return formatTargetName(lastPart);
            }
        }

        return formatTargetName(name);
    }

    private boolean isValidTargetName(String name) {
        if (name == null || name.isEmpty() || name.equals("TargetScreen")) {
            return false;
        }

        if (name.length() < 3) return false;

        String lower = name.toLowerCase();
        return !lower.contains("intent") &&
                !lower.contains("navigation") &&
                !lower.contains("navigate") &&
                !lower.contains("open") &&
                !lower.contains("launch") &&
                !lower.contains("start") &&
                !lower.contains("goto");
    }

    private String createDescriptiveNavigationName(String cleanName, String navType) {
        String meaningfulPart = extractAnyMeaningfulName(cleanName);
        if (meaningfulPart != null && !meaningfulPart.equals("TargetScreen")) {
            return navType + ": " + meaningfulPart;
        }

        String descriptiveName = cleanName.replace("Intent", "")
                .replace("intent", "")
                .replace("Navigation", "")
                .replace("navigation", "")
                .replace("navigate", "")
                .replace("launch", "")
                .replace("start", "")
                .trim();

        if (!descriptiveName.isEmpty()) {
            return navType + ": " + descriptiveName;
        }

        return navType;
    }

    public void setVisible(boolean visible) {
        nodeContainer.setVisible(visible);
        nodeContainer.setManaged(visible);
    }

    public boolean isVisible() {
        return nodeContainer.isVisible();
    }

    public void setViewMode(String viewMode) {
        this.currentViewMode = viewMode;
        if (expanded) {
            refreshExpansion();
        }
    }


    public void updateChildrenVisibility(Predicate<CodeComponent> visibilityFilter) {
        for (GraphNode child : children) {
            CodeComponent component = child.getComponent();
            boolean shouldBeVisible = visibilityFilter.test(component);

            child.setVisible(shouldBeVisible);
            child.setConnectionLinesVisible(shouldBeVisible);

            if (child.isExpanded()) {
                child.updateChildrenVisibility(visibilityFilter);
            }
        }
    }

    public void setConnectionLinesVisible(boolean visible) {
        for (Line line : connectionLines) {
            line.setVisible(visible);
            line.setManaged(visible);
        }
    }

    private String detectComponentCategory(CodeComponent component) {
        return ComponentCategorizer.detectCategory(component);
    }

    public List<GraphNode> getChildren() {
        return new ArrayList<>(children);
    }

}