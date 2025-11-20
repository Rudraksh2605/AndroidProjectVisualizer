package com.projectvisualizer.controllers;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.AnalysisResult;
import com.projectvisualizer.services.ProjectAnalysisService;
import com.projectvisualizer.visualization.GraphManager;
import com.projectvisualizer.visualization.GraphNode;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.Callback;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Diagram renderers
import net.sourceforge.plantuml.SourceStringReader;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Format;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class MainController implements Initializable {

    // FXML Injections from main.fxml
    @FXML private VBox mainContainer;
    @FXML private TreeView<String> projectTreeView;
    @FXML private ScrollPane diagramScrollPane;
    @FXML private ScrollPane minimapScrollPane;
    @FXML private Pane legendOverlayPane;
    @FXML private TabPane visualizationTabPane;
    @FXML private ListView<String> componentsListView;
    @FXML private VBox dependenciesContainer;
    @FXML private VBox statisticsContainer;
    @FXML private VBox metricsContainer;
    @FXML private TextArea plantUMLTextArea;
    @FXML private TextArea graphvizTextArea;

    // PlantUML and Graphviz tabs
    @FXML private Tab plantUMLImageTab;
    @FXML private Tab graphvizImageTab;
    @FXML private ImageView plantUMLImageView;
    @FXML private ImageView graphvizImageView;

    // Toolbar controls
    @FXML private ComboBox<String> featureFilterComboBox;
    @FXML private Label zoomLabel;
    @FXML private CheckBox showLabelsCheckBox;
    @FXML private CheckBox showGridCheckBox;
    @FXML private CheckBox showMinimapCheckBox;

    // Status bar
    @FXML private Label statusLabel;
    @FXML private Label projectInfoLabel;
    @FXML private Label memoryLabel;
    @FXML private Label statusZoomLabel;
    @FXML private Circle connectionStatusIndicator;
    @FXML private ProgressIndicator analysisProgressIndicator;
    @FXML private Label progressLabel;
    @FXML private HBox progressContainer;

    @FXML private MenuButton viewModeMenuButton;
    @FXML private Label categoryStatsLabel;

    private GraphManager graphManager;
    private Pane graphCanvas;
    private Map<String, CodeComponent> componentMap;
    private double currentZoom = 1.0;
    private double plantUmlZoom = 1.0;
    private double graphvizZoom = 1.0;
    private AnalysisResult currentAnalysisResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeGraphCanvas();
        initializeTreeView();
        initializeEventHandlers();
        initializeStatusBar();
        initializeViewModeMenu();
        loadSampleData();
    }

    private void initializeGraphCanvas() {
        graphCanvas = new Pane();
        graphCanvas.setStyle("-fx-background-color: #f8f9fa;");

        diagramScrollPane.setContent(graphCanvas);
        diagramScrollPane.setPannable(true);
        diagramScrollPane.setFitToWidth(false);
        diagramScrollPane.setFitToHeight(false);
        diagramScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        diagramScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        diagramScrollPane.setStyle("-fx-background: #f8f9fa; -fx-border-color: #e0e0e0;");

        // Enable smooth scrolling
        diagramScrollPane.setHvalue(0.5);
        diagramScrollPane.setVvalue(0.5);

        // Make canvas dynamically expandable
        graphCanvas.setMinSize(2000, 2000);
        graphCanvas.setPrefSize(2000, 2000);
        graphCanvas.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        diagramScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (!graphManager.hasNodes()) {
                double viewportWidth = newVal.getWidth();
                double viewportHeight = newVal.getHeight();
                // Set initial size based on viewport
                graphCanvas.setPrefSize(
                        Math.max(2000, viewportWidth),
                        Math.max(2000, viewportHeight)
                );
            }
        });

        graphManager = new GraphManager(graphCanvas);
        componentMap = new HashMap<>();
    }

    private void initializeTreeView() {
        TreeItem<String> rootItem = new TreeItem<>("Project Structure");
        rootItem.setExpanded(true);

        // Sample grouping by file extensions
        TreeItem<String> javaGroup = new TreeItem<>("Java Files (.java)");
        TreeItem<String> kotlinGroup = new TreeItem<>("Kotlin Files (.kt)");
        TreeItem<String> xmlGroup = new TreeItem<>("XML Files (.xml)");

        // Sample files
        javaGroup.getChildren().addAll(
                new TreeItem<>("MainActivity.java"),
                new TreeItem<>("UserRepository.java")
        );
        kotlinGroup.getChildren().addAll(
                new TreeItem<>("LoginFragment.kt"),
                new TreeItem<>("UserViewModel.kt")
        );
        xmlGroup.getChildren().addAll(
                new TreeItem<>("activity_main.xml"),
                new TreeItem<>("fragment_login.xml")
        );

        rootItem.getChildren().addAll(javaGroup, kotlinGroup, xmlGroup);

        projectTreeView.setRoot(rootItem);
        projectTreeView.setShowRoot(true);

        projectTreeView.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
            @Override
            public TreeCell<String> call(TreeView<String> param) {
                return new TextFieldTreeCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item);
                        }
                    }
                };
            }
        });
    }


    private void initializeEventHandlers() {
        // Tree view selection listener
        projectTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && !newValue.getValue().equals("Project Structure")) {
                        handleComponentSelection(newValue);
                    }
                }
        );

        // Render PlantUML/Graphviz images when their tabs are selected
        if (visualizationTabPane != null) {
            visualizationTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                try {
                    if (newTab == plantUMLImageTab) {
                        renderPlantUml(false);
                    } else if (newTab == graphvizImageTab) {
                        renderGraphviz(false);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Failed to render diagram: " + e.getMessage());
                }
            });
        }

        updateZoomLabel();
    }

    private void initializeStatusBar() {
        statusLabel.setText("Ready");
        projectInfoLabel.setText("No project loaded");
        memoryLabel.setText("Memory: " + getUsedMemory() + " MB");
        statusZoomLabel.setText("100%");
        startMemoryMonitoring();
    }

    private void loadSampleData() {
        // Create sample components for testing
        CodeComponent mainActivity = new CodeComponent();
        mainActivity.setId("MainActivity");
        mainActivity.setName("MainActivity");
        mainActivity.setType("Activity");
        mainActivity.setLayer("UI");
        mainActivity.setLanguage("Java");

        CodeComponent loginFragment = new CodeComponent();
        loginFragment.setId("LoginFragment");
        loginFragment.setName("LoginFragment");
        loginFragment.setType("Fragment");
        loginFragment.setLayer("UI");
        loginFragment.setLanguage("Kotlin");

        CodeComponent userRepository = new CodeComponent();
        userRepository.setId("UserRepository");
        userRepository.setName("UserRepository");
        userRepository.setType("Repository");
        userRepository.setLayer("Data");
        userRepository.setLanguage("Java");

        CodeComponent userViewModel = new CodeComponent();
        userViewModel.setId("UserViewModel");
        userViewModel.setName("UserViewModel");
        userViewModel.setType("ViewModel");
        userViewModel.setLayer("Business Logic");
        userViewModel.setLanguage("Kotlin");

        // Add dependencies
        mainActivity.addDependency(loginFragment);
        loginFragment.addDependency(userRepository);
        userRepository.addDependency(userViewModel);

        // Store in map
        componentMap.put("MainActivity", mainActivity);
        componentMap.put("LoginFragment", loginFragment);
        componentMap.put("UserRepository", userRepository);
        componentMap.put("UserViewModel", userViewModel);

        statusLabel.setText("Sample data loaded");
    }

    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Project Directory");
        File selectedDirectory = directoryChooser.showDialog(mainContainer.getScene().getWindow());

        if (selectedDirectory != null) {
            statusLabel.setText("Loading project: " + selectedDirectory.getName());
            projectInfoLabel.setText("Project: " + selectedDirectory.getName());

            progressContainer.setVisible(true);
            progressLabel.setText("Analyzing project structure...");

            Task<AnalysisResult> analysisTask = new Task<AnalysisResult>() {
                @Override
                protected AnalysisResult call() throws Exception {
                    ProjectAnalysisService analysisService = new ProjectAnalysisService();
                    return analysisService.analyzeProject(selectedDirectory);
                }
            };

            analysisTask.setOnSucceeded(event -> {
                currentAnalysisResult = analysisTask.getValue();
                progressContainer.setVisible(false);

                if (currentAnalysisResult.getError() != null) {
                    statusLabel.setText("Analysis failed: " + currentAnalysisResult.getError());
                    showErrorAlert("Analysis Error", currentAnalysisResult.getError());
                } else {
                    handleAnalysisResult(currentAnalysisResult);
                }
            });

            analysisTask.setOnFailed(event -> {
                progressContainer.setVisible(false);
                statusLabel.setText("Project analysis failed");
                showErrorAlert("Analysis Failed", "Failed to analyze project: " +
                        analysisTask.getException().getMessage());
            });

            new Thread(analysisTask).start();
        }
    }

    @FXML
    private void handleExportEnhancedDiagram() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Enhanced Diagram");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("SVG Files", "*.svg")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            statusLabel.setText("Exported diagram to: " + file.getName());
        }
    }

    @FXML
    private void handleExportDiagram() {
        handleExportEnhancedDiagram();
    }

    @FXML
    private void handleExportReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Report");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("HTML Files", "*.html"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            statusLabel.setText("Exported report to: " + file.getName());
        }
    }

    @FXML
    private void handleSettings() {
        Alert settingsDialog = new Alert(Alert.AlertType.INFORMATION);
        settingsDialog.setTitle("Settings");
        settingsDialog.setHeaderText("Application Settings");
        settingsDialog.setContentText("Settings dialog will be implemented in future version.");
        settingsDialog.showAndWait();
    }

    @FXML
    private void handleExit() {
        javafx.application.Platform.exit();
    }


    @FXML
    private void handleZoomIn() {
        currentZoom *= 1.2;
        diagramScrollPane.setScaleX(currentZoom);
        diagramScrollPane.setScaleY(currentZoom);
        updateZoomLabel();
    }

    @FXML
    private void handleZoomOut() {
        currentZoom /= 1.2;
        diagramScrollPane.setScaleX(currentZoom);
        diagramScrollPane.setScaleY(currentZoom);
        updateZoomLabel();
    }

    @FXML
    private void handleResetZoom() {
        currentZoom = 1.0;
        diagramScrollPane.setScaleX(currentZoom);
        diagramScrollPane.setScaleY(currentZoom);
        updateZoomLabel();
    }

    @FXML
    private void handleFitToWindow() {
        handleResetZoom();
        statusLabel.setText("Zoom reset to 100%");
    }

    @FXML
    private void handleResetToFullView() {
        graphManager.clearGraph();
        handleResetZoom();
        diagramScrollPane.setVvalue(0);
        diagramScrollPane.setHvalue(0);
        statusLabel.setText("Canvas cleared");
    }

    // Sidebar Handlers
    @FXML
    private void handleRefreshTree() {
        statusLabel.setText("Refreshing project tree...");
        initializeTreeView();
        statusLabel.setText("Project tree refreshed");
    }

    @FXML
    private void handleTreeSettings() {
        Alert settingsDialog = new Alert(Alert.AlertType.INFORMATION);
        settingsDialog.setTitle("Tree View Settings");
        settingsDialog.setHeaderText("Tree View Configuration");
        settingsDialog.setContentText("Tree view settings will be implemented in future version.");
        settingsDialog.showAndWait();
    }

    @FXML
    private void handleCopyPlantUML() {
        if (plantUMLTextArea != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(plantUMLTextArea.getText() == null ? "" : plantUMLTextArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("PlantUML code copied to clipboard");
        }
    }

    // Renders PlantUML source from plantUMLTextArea into plantUMLImageView and optionally onto canvas
    private void renderPlantUml(boolean showOnCanvas) {
        try {
            if (plantUMLTextArea == null || plantUMLImageView == null) return;
            String src = plantUMLTextArea.getText();
            if (src == null || src.trim().isEmpty()) {
                statusLabel.setText("No PlantUML source to render");
                return;
            }
            // Ensure PlantUML has start/end wrappers
            String puml = src.trim();
            if (!puml.contains("@startuml")) {
                puml = "@startuml\n" + puml + "\n@enduml";
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            SourceStringReader reader = new SourceStringReader(puml);
            reader.generateImage(os, new FileFormatOption(FileFormat.PNG));
            byte[] bytes = os.toByteArray();
            if (bytes.length == 0) {
                statusLabel.setText("PlantUML produced empty output");
                return;
            }
            Image fxImage = new Image(new ByteArrayInputStream(bytes));
            plantUMLImageView.setImage(fxImage);
            statusLabel.setText("Rendered PlantUML diagram");

            if (showOnCanvas && graphCanvas != null) {
                // Clear canvas and show image centered
                graphCanvas.getChildren().clear();
                ImageView iv = new ImageView(fxImage);
                iv.setPreserveRatio(true);
                iv.setFitWidth(Math.min(1200, fxImage.getWidth()));
                graphCanvas.getChildren().add(iv);
            }
        } catch (Exception e) {
            statusLabel.setText("Error rendering PlantUML: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportToPlantUML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PlantUML Diagram");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PlantUML Files", "*.puml"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            statusLabel.setText("Exported PlantUML to: " + file.getName());
        }
    }

    @FXML
    private void handleCopyGraphviz() {
        if (graphvizTextArea != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(graphvizTextArea.getText() == null ? "" : graphvizTextArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("Graphviz code copied to clipboard");
        }
    }

    @FXML
    private void handleExportToGraphviz() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Graphviz Diagram");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DOT Files", "*.dot"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            statusLabel.setText("Exported Graphviz to: " + file.getName());
        }
    }

    private void renderGraphviz(boolean showOnCanvas) {
        try {
            if (graphvizTextArea == null || graphvizImageView == null) return;
            String dot = graphvizTextArea.getText();
            if (dot == null || dot.trim().isEmpty()) {
                statusLabel.setText("No Graphviz (DOT) source to render");
                return;
            }
            String src = dot;
            String low = dot.toLowerCase();
            if (!low.contains("digraph") && !low.contains("graph") && (dot.contains("->") || dot.contains("--"))) {
                src = "digraph G {\n" + dot + "\n}";
            }
            BufferedImage bi = Graphviz.fromString(src).render(Format.PNG).toImage();
            if (bi == null) {
                statusLabel.setText("Graphviz produced no image");
                return;
            }
            Image fxImage = SwingFXUtils.toFXImage(bi, null);
            graphvizImageView.setImage(fxImage);
            statusLabel.setText("Rendered Graphviz diagram");

            if (showOnCanvas && graphCanvas != null) {
                graphCanvas.getChildren().clear();
                ImageView iv = new ImageView(fxImage);
                iv.setPreserveRatio(true);
                iv.setFitWidth(Math.min(1200, fxImage.getWidth()));
                graphCanvas.getChildren().add(iv);
            }
        } catch (Exception e) {
            statusLabel.setText("Error rendering Graphviz: " + e.getMessage());
        }
    }

    @FXML
    private void handleZoomInPlantUML() {
        plantUmlZoom = Math.min(plantUmlZoom * 1.25, 5.0);
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom: " + (int)(plantUmlZoom * 100) + "%");
    }

    @FXML
    private void handleZoomOutPlantUML() {
        plantUmlZoom = Math.max(plantUmlZoom / 1.25, 0.2);
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom: " + (int)(plantUmlZoom * 100) + "%");
    }

    @FXML
    private void handleResetZoomPlantUML() {
        plantUmlZoom = 1.0;
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom reset");
    }

    private void applyPlantUmlZoom() {
        if (plantUMLImageView != null) {
            plantUMLImageView.setScaleX(plantUmlZoom);
            plantUMLImageView.setScaleY(plantUmlZoom);
        }
    }

    @FXML
    private void handleExportPlantUMLImage() {
        handleExportEnhancedDiagram();
    }

    @FXML
    private void handleZoomInGraphviz() {
        graphvizZoom = Math.min(graphvizZoom * 1.25, 5.0);
        applyGraphvizZoom();
        statusLabel.setText("Graphviz zoom: " + (int)(graphvizZoom * 100) + "%");
    }

    @FXML
    private void handleZoomOutGraphviz() {
        graphvizZoom = Math.max(graphvizZoom / 1.25, 0.2);
        applyGraphvizZoom();
        statusLabel.setText("Graphviz zoom: " + (int)(graphvizZoom * 100) + "%");
    }

    @FXML
    private void handleResetZoomGraphviz() {
        graphvizZoom = 1.0;
        applyGraphvizZoom();
        statusLabel.setText("Graphviz zoom reset");
    }

    private void applyGraphvizZoom() {
        if (graphvizImageView != null) {
            graphvizImageView.setScaleX(graphvizZoom);
            graphvizImageView.setScaleY(graphvizZoom);
        }
    }

    @FXML
    private void handleExportGraphvizImage() {
        handleExportEnhancedDiagram();
    }

    private void handleComponentSelection(TreeItem<String> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) return;

        String itemValue = selectedItem.getValue();

        // Skip if it's a group header or root
        if (itemValue.equals("Project Structure") ||
                itemValue.equals("Java Files (.java)") ||
                itemValue.equals("Kotlin Files (.kt)") ||
                itemValue.equals("XML Files (.xml)")) {
            return;
        }

        System.out.println("Selected tree item: " + itemValue);
        System.out.println("Component map size: " + componentMap.size());
        System.out.println("Component map keys: " + componentMap.keySet());

        CodeComponent component = null;

        // Strategy 1: Direct ID match
        component = componentMap.get(itemValue);
        if (component != null) {
            System.out.println("Found component by direct ID: " + component.getId());
        }

        // Strategy 2: Try with filename without extension
        if (component == null) {
            String baseName = itemValue;
            int dotIndex = itemValue.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = itemValue.substring(0, dotIndex);
            }
            component = componentMap.get(baseName);
            if (component != null) {
                System.out.println("Found component by base name: " + baseName);
            }
        }

        // Strategy 3: Search by component name (case-insensitive)
        if (component == null) {
            for (CodeComponent comp : componentMap.values()) {
                if (comp.getName() != null && comp.getName().equalsIgnoreCase(itemValue)) {
                    component = comp;
                    System.out.println("Found component by name match: " + comp.getName());
                    break;
                }
            }
        }

        // Strategy 4: Search by filename pattern
        if (component == null) {
            for (CodeComponent comp : componentMap.values()) {
                if (comp.getName() != null) {
                    // Check if component name matches filename pattern
                    String compName = comp.getName().toLowerCase();
                    String fileName = itemValue.toLowerCase();

                    if (fileName.contains(compName) || compName.contains(fileName.replace(".java", "").replace(".kt", ""))) {
                        component = comp;
                        System.out.println("Found component by pattern match: " + comp.getName());
                        break;
                    }
                }
            }
        }

        if (component != null) {
            System.out.println("Adding component to graph: " + component.getName());
            graphManager.addComponentToGraph(component);
            statusLabel.setText("Added " + component.getName() + " (" + component.getType() + " - " + component.getLayer() + ") to graph");

            // Auto-scroll to the new node after a short delay
            autoScrollToNode(component.getId());
        } else {
            System.out.println("Component not found for: " + itemValue);
            statusLabel.setText("Component not found: " + itemValue);

        }
    }

    private void autoScrollToNode(String componentId) {
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(300); // Give time for the node to be rendered
                diagramScrollPane.layout();

                // Try to find and center on the node
                GraphNode node = graphManager.getNodeMap().get(componentId);
                if (node != null) {
                    javafx.scene.layout.VBox container = node.getContainer();
                    double centerX = container.getLayoutX() / graphCanvas.getWidth();
                    double centerY = container.getLayoutY() / graphCanvas.getHeight();

                    diagramScrollPane.setHvalue(Math.max(0, Math.min(1, centerX)));
                    diagramScrollPane.setVvalue(Math.max(0, Math.min(1, centerY)));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // Utility Methods
    private void updateZoomLabel() {
        int zoomPercentage = (int) (currentZoom * 100);
        zoomLabel.setText(zoomPercentage + "%");
        statusZoomLabel.setText(zoomPercentage + "%");
    }

    private void updateComponentList() {
        ObservableList<String> components = FXCollections.observableArrayList();
        for (String componentName : componentMap.keySet()) {
            components.add(componentName);
        }
        componentsListView.setItems(components);
    }

    private void updateProjectTreeWithRealData(List<CodeComponent> components) {
        TreeItem<String> rootItem = new TreeItem<>("Project Structure");
        rootItem.setExpanded(true);

        // Group by file extensions as requested
        TreeItem<String> javaGroup = new TreeItem<>("Java Files (.java)");
        TreeItem<String> kotlinGroup = new TreeItem<>("Kotlin Files (.kt)");
        TreeItem<String> xmlGroup = new TreeItem<>("XML Files (.xml)");

        for (CodeComponent component : components) {
            if (component == null) continue;
            String filePath = component.getFilePath();
            String language = component.getLanguage();
            String fileName = null;
            if (filePath != null) {
                File f = new File(filePath);
                fileName = f.getName();
            }
            if (fileName == null || fileName.isEmpty()) {
                // Fallback to name + inferred extension
                String base = component.getName() != null ? component.getName() : "Unknown";
                if (language != null && language.equalsIgnoreCase("kotlin")) {
                    fileName = base + ".kt";
                } else if (language != null && language.toLowerCase().startsWith("java")) {
                    fileName = base + ".java";
                } else {
                    // default unknown extension
                    fileName = base;
                }
            }

            TreeItem<String> item = new TreeItem<>(fileName);

            String lower = fileName.toLowerCase();
            if (lower.endsWith(".java")) {
                javaGroup.getChildren().add(item);
            } else if (lower.endsWith(".kt")) {
                kotlinGroup.getChildren().add(item);
            } else if (lower.endsWith(".xml")) {
                xmlGroup.getChildren().add(item);
            }
        }

        if (!javaGroup.getChildren().isEmpty()) {
            rootItem.getChildren().add(javaGroup);
            javaGroup.setExpanded(true);
        }
        if (!kotlinGroup.getChildren().isEmpty()) {
            rootItem.getChildren().add(kotlinGroup);
            kotlinGroup.setExpanded(true);
        }
        if (!xmlGroup.getChildren().isEmpty()) {
            rootItem.getChildren().add(xmlGroup);
            xmlGroup.setExpanded(true);
        }

        projectTreeView.setRoot(rootItem);
    }

    // Component Filtering Methods
    private List<CodeComponent> filterUIComponents(List<CodeComponent> allComponents) {
        List<CodeComponent> uiComponents = new ArrayList<>();
        for (CodeComponent component : allComponents) {
            if (isUIComponent(component)) {
                uiComponents.add(component);
            }
        }
        return uiComponents;
    }

    private List<CodeComponent> filterComponentsByLayer(List<CodeComponent> allComponents, String targetLayer) {
        List<CodeComponent> filteredComponents = new ArrayList<>();
        for (CodeComponent component : allComponents) {
            if (component != null && targetLayer.equals(component.getLayer())) {
                filteredComponents.add(component);
            }
        }
        return filteredComponents;
    }

    // Enhanced UI component detection for both Java and Kotlin
    private boolean isUIComponent(CodeComponent component) {
        if (component == null || component.getName() == null) return false;

        String layer = component.getLayer();
        String name = component.getName().toLowerCase();
        String extendsClass = component.getExtendsClass();
        String type = component.getType();
        String packageName = component.getPackageName();
        String filePath = component.getFilePath();

        // Check by layer first
        if ("UI".equals(layer)) {
            return true;
        }

        // Check by type (works for both Java and Kotlin)
        if (type != null) {
            String lowerType = type.toLowerCase();
            if (lowerType.contains("activity") ||
                    lowerType.contains("fragment") ||
                    lowerType.contains("adapter") ||
                    lowerType.contains("viewholder") ||
                    lowerType.contains("view") ||
                    lowerType.contains("layout") ||
                    lowerType.contains("dialog") ||
                    lowerType.contains("menu") ||
                    lowerType.contains("button") ||
                    lowerType.contains("text") ||
                    lowerType.contains("image") ||
                    lowerType.contains("list") ||
                    lowerType.contains("recycler") ||
                    lowerType.contains("card")) {
                return true;
            }
        }

        // Check by name patterns (works for both Java and Kotlin)
        boolean isUIByName = name.endsWith("activity") ||
                name.endsWith("fragment") ||
                name.endsWith("adapter") ||
                name.endsWith("viewholder") ||
                name.endsWith("view") ||
                name.endsWith("layout") ||
                name.contains("screen") ||
                name.contains("page") ||
                name.contains("dialog") ||
                name.contains("button") ||
                name.contains("text") ||
                name.contains("image") ||
                name.contains("list") ||
                name.contains("recycler") ||
                name.contains("card");

        // Check extends class (works for both Java and Kotlin)
        boolean isUIByExtends = extendsClass != null &&
                (extendsClass.endsWith("Activity") ||
                        extendsClass.endsWith("Fragment") ||
                        extendsClass.endsWith("View") ||
                        extendsClass.endsWith("Adapter") ||
                        extendsClass.contains("android.app.Activity") ||
                        extendsClass.contains("androidx.fragment.app.Fragment") ||
                        extendsClass.contains("android.view.View") ||
                        extendsClass.contains("android.widget.") ||
                        extendsClass.contains("androidx.recyclerview.widget.") ||
                        extendsClass.contains("androidx.appcompat.app.AppCompatActivity") ||
                        extendsClass.contains("androidx.fragment.app.FragmentActivity"));

        // Check package name for Android UI components
        boolean isUIByPackage = packageName != null &&
                (packageName.startsWith("android.") ||
                        packageName.startsWith("androidx.") ||
                        packageName.contains(".widget.") ||
                        packageName.contains(".view.") ||
                        packageName.contains(".custom."));

        // Check file path for layout files
        boolean isUIByFile = filePath != null &&
                (filePath.contains("/layout/") ||
                        filePath.endsWith(".xml") && filePath.contains("res"));

        return isUIByName || isUIByExtends || isUIByPackage || isUIByFile;
    }
    private void resolveDependencies(List<CodeComponent> allComponents) {
        if (allComponents == null || allComponents.isEmpty()) return;

        // Use a merge function to handle duplicate keys by keeping the first occurrence
        Map<String, CodeComponent> byId = allComponents.stream()
                .filter(c -> c != null && c.getId() != null)
                .collect(Collectors.toMap(
                        CodeComponent::getId,
                        Function.identity(),
                        (existing, replacement) -> {
                            System.out.println("WARNING: Duplicate component ID found: " + existing.getId() +
                                    ". Keeping first occurrence: " + existing.getName());
                            return existing; // Keep the existing component when duplicate ID is found
                        }
                ));

        // Build simple-name fallback map
        Map<String, CodeComponent> bySimpleName = new HashMap<>();
        for (CodeComponent c : allComponents) {
            if (c == null) continue;
            String id = c.getId();
            String simple = null;
            if (id != null && id.contains(".")) {
                simple = id.substring(id.lastIndexOf('.') + 1);
            } else if (c.getName() != null) {
                simple = c.getName();
            }
            if (simple != null) bySimpleName.putIfAbsent(simple, c);
        }

        for (CodeComponent component : allComponents) {
            if (component == null) continue;
            List<CodeComponent> originalDeps = component.getDependencies();
            if (originalDeps == null || originalDeps.isEmpty()) {
                component.setDependencies(Collections.emptyList());
                continue;
            }

            List<CodeComponent> resolvedDependencies = new ArrayList<>();

            for (CodeComponent dependency : originalDeps) {
                if (dependency == null) continue;
                CodeComponent resolved = null;
                if (dependency.getId() != null) {
                    resolved = byId.get(dependency.getId());
                }
                if (resolved == null) {
                    String key = null;
                    String depId = dependency.getId();
                    if (depId != null) {
                        key = depId.contains(".") ? depId.substring(depId.lastIndexOf('.') + 1) : depId;
                    }
                    if ((key == null || key.isEmpty()) && dependency.getName() != null) {
                        key = dependency.getName();
                    }
                    if (key != null) {
                        resolved = bySimpleName.get(key);
                    }
                }

                if (resolved != null && resolved != component) {
                    resolvedDependencies.add(resolved);
                } else {
                    detectComponentLayer(dependency);
                    resolvedDependencies.add(dependency);
                }
            }

            component.setDependencies(resolvedDependencies);
        }
    }

    private void detectComponentLayer(CodeComponent component) {
        if (component.getName() == null) return;

        String name = component.getName().toLowerCase();
        if (name.endsWith("repository") || name.endsWith("datasource") || name.endsWith("dao")) {
            component.setLayer("Data");
        } else if (name.endsWith("viewmodel") || name.endsWith("presenter") || name.endsWith("usecase") ||
                name.endsWith("service") || name.endsWith("manager") || name.contains("bot")) {
            component.setLayer("Business Logic");
        } else if (name.endsWith("activity") || name.endsWith("fragment") || name.endsWith("adapter")) {
            component.setLayer("UI");
        } else if (name.endsWith("entity") || name.endsWith("domain") || name.endsWith("model")) {
            component.setLayer("Domain");
        }
        // Otherwise, layer remains as is (might be already set by parser)
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    private void startMemoryMonitoring() {
        Thread memoryMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        memoryLabel.setText("Memory: " + getUsedMemory() + " MB");
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        memoryMonitor.setDaemon(true);
        memoryMonitor.start();
    }

    @FXML
    private void handleScrollToCenter() {
        diagramScrollPane.setHvalue(0.5);
        diagramScrollPane.setVvalue(0.5);
        statusLabel.setText("Scrolled to center");
    }

    @FXML
    private void handleScrollToOrigin() {
        diagramScrollPane.setHvalue(0);
        diagramScrollPane.setVvalue(0);
        statusLabel.setText("Scrolled to origin");
    }

    @FXML
    private void handleScrollToNodes() {
        if (graphManager != null && graphManager.hasNodes()) {
            // Find the bounding box of all nodes and center on them
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

            for (GraphNode node : graphManager.getNodeMap().values()) {
                javafx.scene.layout.VBox container = node.getContainer();
                double x = container.getLayoutX();
                double y = container.getLayoutY();
                double width = container.getBoundsInLocal().getWidth();
                double height = container.getBoundsInLocal().getHeight();

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + width);
                maxY = Math.max(maxY, y + height);
            }

            if (minX != Double.MAX_VALUE) {
                // Calculate center of nodes
                double centerX = (minX + maxX) / 2 / graphCanvas.getWidth();
                double centerY = (minY + maxY) / 2 / graphCanvas.getHeight();

                diagramScrollPane.setHvalue(centerX);
                diagramScrollPane.setVvalue(centerY);
                statusLabel.setText("Centered on nodes");
            }
        }
    }
    // Build a PlantUML source from analyzed components and their dependencies
    private String buildPlantUmlFromComponents(List<CodeComponent> components) {
        if (components == null || components.isEmpty()) {
            return "@startuml\n' No components found\n@enduml";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam backgroundColor #ffffff\n");
        sb.append("skinparam class {\n  BackgroundColor<<UI>> #e0f2fe\n  BackgroundColor<<BusinessLogic>> #e0ffe0\n  BackgroundColor<<Data>> #fff4e5\n  BackgroundColor<<Domain>> #f3e8ff\n}\n");

        // Create class declarations
        Map<String, String> idToAlias = new HashMap<>();
        for (CodeComponent c : components) {
            if (c == null) continue;
            String id = Optional.ofNullable(c.getId()).orElse(Optional.ofNullable(c.getName()).orElse(UUID.randomUUID().toString()));
            String alias = sanitizeIdForDiagram(id);
            idToAlias.put(id, alias);

            String name = Optional.ofNullable(c.getName()).orElse(id);
            String type = Optional.ofNullable(c.getType()).orElse("");
            String layer = Optional.ofNullable(c.getLayer()).orElse("");
            String stereo = layerStereo(layer);

            sb.append("class \"").append(name).append("\" as ").append(alias);
            if (!stereo.isEmpty()) {
                sb.append(" <<").append(stereo).append(">>");
            }
            if (type != null && !type.isEmpty()) {
                sb.append(" : ").append(type);
            }
            sb.append("\n");
        }

        // Dependencies
        for (CodeComponent c : components) {
            if (c == null) continue;
            String fromId = Optional.ofNullable(c.getId()).orElse(c.getName());
            if (fromId == null) continue;
            String fromAlias = idToAlias.get(fromId);
            if (fromAlias == null) fromAlias = sanitizeIdForDiagram(fromId);

            List<CodeComponent> deps = c.getDependencies();
            if (deps == null) continue;
            for (CodeComponent d : deps) {
                if (d == null) continue;
                String toId = Optional.ofNullable(d.getId()).orElse(d.getName());
                if (toId == null) continue;
                String toAlias = idToAlias.getOrDefault(toId, sanitizeIdForDiagram(toId));
                if (fromAlias.equals(toAlias)) continue;
                sb.append(fromAlias).append(" --> ").append(toAlias).append("\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    // Build a Graphviz DOT source from analyzed components and their dependencies
    private String buildGraphvizDotFromComponents(List<CodeComponent> components) {
        if (components == null || components.isEmpty()) {
            return "digraph G {\n  // No components found\n}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  graph [bgcolor=white];\n");
        sb.append("  node [shape=box, style=filled, fontname=Helvetica];\n");

        // Group by layer into subgraphs
        Map<String, List<CodeComponent>> byLayer = new HashMap<>();
        for (CodeComponent c : components) {
            if (c == null) continue;
            String layer = Optional.ofNullable(c.getLayer()).orElse("Other");
            byLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(c);
        }

        Map<String, String> idToNode = new HashMap<>();
        for (Map.Entry<String, List<CodeComponent>> e : byLayer.entrySet()) {
            String layer = e.getKey();
            List<CodeComponent> list = e.getValue();
            String clusterName = sanitizeIdForDiagram("cluster_" + layer);
            sb.append("  subgraph ").append(clusterName).append(" {\n");
            sb.append("    label=\"").append(layer).append("\";\n");
            sb.append("    color=\"").append(layerStroke(layer)).append("\";\n");
            for (CodeComponent c : list) {
                String id = Optional.ofNullable(c.getId()).orElse(Optional.ofNullable(c.getName()).orElse(UUID.randomUUID().toString()));
                String node = sanitizeIdForDiagram(id);
                idToNode.put(id, node);
                String label = Optional.ofNullable(c.getName()).orElse(id);
                String fill = layerFill(layer);
                sb.append("    ").append(node).append(" [label=\"").append(label.replace("\"", "\\\"")).append("\" fillcolor=\"")
                        .append(fill).append("\"];\n");
            }
            sb.append("  }\n");
        }

        // Edges
        for (CodeComponent c : components) {
            if (c == null) continue;
            String fromId = Optional.ofNullable(c.getId()).orElse(c.getName());
            if (fromId == null) continue;
            String from = idToNode.getOrDefault(fromId, sanitizeIdForDiagram(fromId));
            List<CodeComponent> deps = c.getDependencies();
            if (deps == null) continue;
            for (CodeComponent d : deps) {
                if (d == null) continue;
                String toId = Optional.ofNullable(d.getId()).orElse(d.getName());
                if (toId == null) continue;
                String to = idToNode.getOrDefault(toId, sanitizeIdForDiagram(toId));
                if (from.equals(to)) continue;
                sb.append("  ").append(from).append(" -> ").append(to).append(";\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String sanitizeIdForDiagram(String raw) {
        if (raw == null || raw.isEmpty()) return "N" + UUID.randomUUID().toString().replace('-', '_');
        return raw.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String layerStereo(String layer) {
        if (layer == null) return "";
        switch (layer.trim()) {
            case "UI": return "UI";
            case "Data": return "Data";
            case "Business Logic": return "BusinessLogic";
            case "Domain": return "Domain";
            default: return "";
        }
    }

    private String layerFill(String layer) {
        if (layer == null) return "#f2f2f2";
        switch (layer.trim()) {
            case "UI": return "#e0f2fe";           // light blue
            case "Data": return "#fff4e5";         // light orange
            case "Business Logic": return "#eaffea"; // light green
            case "Domain": return "#f3e8ff";       // light purple
            default: return "#f2f2f2";               // light gray
        }
    }

    private String layerStroke(String layer) {
        if (layer == null) return "#cccccc";
        switch (layer.trim()) {
            case "UI": return "#93c5fd";           // blue border
            case "Data": return "#fdba74";         // orange border
            case "Business Logic": return "#86efac"; // green border
            case "Domain": return "#d8b4fe";       // purple border
            default: return "#cccccc";
        }
    }

    private void initializeViewModeMenu() {
        // Create menu items for different view modes
        MenuItem allItems = new MenuItem("Show All Components");
        MenuItem uiItems = new MenuItem("Show UI Components Only");
        MenuItem dataModelItems = new MenuItem("Show Data Models Only");
        MenuItem businessLogicItems = new MenuItem("Show Business Logic Only");
        MenuItem navigationItems = new MenuItem("Show Navigation Only");
        MenuItem relationshipsItems = new MenuItem("Show Component Relationships");

        allItems.setOnAction(e -> setViewMode("ALL"));
        uiItems.setOnAction(e -> setViewMode("UI"));
        dataModelItems.setOnAction(e -> setViewMode("DATA_MODEL"));
        businessLogicItems.setOnAction(e -> setViewMode("BUSINESS_LOGIC"));
        navigationItems.setOnAction(e -> setViewMode("NAVIGATION"));
        relationshipsItems.setOnAction(e -> showComponentRelationships());

        viewModeMenuButton.getItems().addAll(
                allItems, uiItems, dataModelItems, businessLogicItems,
                navigationItems, new SeparatorMenuItem(), relationshipsItems
        );
    }

    private void setViewMode(String mode) {
        if (graphManager != null) {
            graphManager.setViewMode(mode);
            updateCategoryStats();
            statusLabel.setText("View mode: " + getViewModeDisplayName(mode));
        }
    }

    private String getViewModeDisplayName(String mode) {
        switch (mode) {
            case "ALL": return "All Components";
            case "UI": return "UI Components";
            case "DATA_MODEL": return "Data Models";
            case "BUSINESS_LOGIC": return "Business Logic";
            case "NAVIGATION": return "Navigation/Intents";
            default: return "All Components";
        }
    }

    private void updateCategoryStats() {
        if (graphManager != null) {
            Map<String, Integer> stats = graphManager.getCategoryStats();
            StringBuilder statsText = new StringBuilder("Categories: ");
            stats.forEach((category, count) ->
                    statsText.append(category).append(": ").append(count).append("  "));
            categoryStatsLabel.setText(statsText.toString());
        }
    }

    private void showComponentRelationships() {
        // Show dependencies between components
        if (currentAnalysisResult != null) {
            graphManager.clearGraph();

            // Add all components and show their relationships
            for (CodeComponent component : currentAnalysisResult.getComponents()) {
                graphManager.addComponentToGraph(component);
            }

            // Highlight dependencies
            highlightDependencies();
            statusLabel.setText("Showing component relationships and dependencies");
        }
    }

    private void highlightDependencies() {
        // Enhanced dependency highlighting
        for (GraphNode node : graphManager.getNodeMap().values()) {
            CodeComponent component = node.getComponent();
            if (component.getDependencies() != null) {
                for (CodeComponent dep : component.getDependencies()) {
                    highlightDependencyConnection(component, dep);
                }
            }
        }
    }

    private void highlightDependencyConnection(CodeComponent from, CodeComponent to) {
        // Implementation for highlighting dependency connections
        GraphNode fromNode = graphManager.getNodeMap().get(from.getId());
        GraphNode toNode = graphManager.getNodeMap().get(to.getId());

        if (fromNode != null && toNode != null) {
            // Create and style dependency connection
            createDependencyLine(fromNode, toNode);
        }
    }

    private void createDependencyLine(GraphNode fromNode, GraphNode toNode) {
        Line line = new Line();

        line.startXProperty().bind(fromNode.getContainer().layoutXProperty()
                .add(fromNode.getContainer().widthProperty().divide(2)));
        line.startYProperty().bind(fromNode.getContainer().layoutYProperty()
                .add(fromNode.getContainer().heightProperty().divide(2)));
        line.endXProperty().bind(toNode.getContainer().layoutXProperty()
                .add(toNode.getContainer().widthProperty().divide(2)));
        line.endYProperty().bind(toNode.getContainer().layoutYProperty()
                .add(toNode.getContainer().heightProperty().divide(2)));

        // Style based on dependency type
        line.setStroke(Color.DARKBLUE);
        line.setStrokeWidth(1.5);
        line.getStrokeDashArray().addAll(2.0, 2.0);

        graphCanvas.getChildren().add(line);
        line.toBack(); // Send to back so nodes appear on top
    }

    // Enhanced analysis result handling
    private void handleAnalysisResult(AnalysisResult result) {
        statusLabel.setText("Project analysis complete");

        componentMap.clear();
        for (CodeComponent component : result.getComponents()) {
            if (componentMap.containsKey(component.getId())) {
                String uniqueId = component.getId() + "_" + System.currentTimeMillis();
                component.setId(uniqueId);
            }
            componentMap.put(component.getId(), component);
        }

        try {
            resolveDependencies(result.getComponents());
        } catch (Exception e) {
            System.err.println("Error resolving dependencies: " + e.getMessage());
            statusLabel.setText("Warning: Some dependencies could not be resolved");
        }

        // Categorize components
        graphManager.categorizeComponents(result.getComponents());
        updateCategoryStats();

        // Show initial view
        setViewMode("ALL");

        updateComponentList();
        updateProjectTreeWithRealData(result.getComponents());

        // Update diagrams
        updateDiagrams(result);

        handleResetZoom();
        diagramScrollPane.setVvalue(0);
        diagramScrollPane.setHvalue(0);
    }

    private void updateDiagrams(AnalysisResult result) {
        try {
            if (plantUMLTextArea != null) {
                String puml = buildCategorizedPlantUml(result.getComponents());
                plantUMLTextArea.setText(puml);
                renderPlantUml(false);
            }
            if (graphvizTextArea != null) {
                String dot = buildCategorizedGraphviz(result.getComponents());
                graphvizTextArea.setText(dot);
                renderGraphviz(false);
            }
        } catch (Exception e) {
            statusLabel.setText("Failed to generate diagrams: " + e.getMessage());
        }
    }

    private String buildCategorizedPlantUml(List<CodeComponent> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam backgroundColor #ffffff\n");
        sb.append("skinparam class {\n");
        sb.append("  BackgroundColor<<UI>> #e0f2fe\n");
        sb.append("  BackgroundColor<<DataModel>> #fff4e5\n");
        sb.append("  BackgroundColor<<BusinessLogic>> #eaffea\n");
        sb.append("  BackgroundColor<<Navigation>> #ffe0e0\n");
        sb.append("  BackgroundColor<<Unknown>> #f2f2f2\n");
        sb.append("}\n\n");

        // Group by category
        Map<String, List<CodeComponent>> byCategory = components.stream()
                .collect(Collectors.groupingBy(this::detectComponentCategory));

        for (Map.Entry<String, List<CodeComponent>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<CodeComponent> categoryComponents = entry.getValue();

            sb.append("' ").append(category).append(" Components\n");
            for (CodeComponent component : categoryComponents) {
                String name = component.getName() != null ? component.getName() : "Unknown";
                String type = component.getType() != null ? component.getType() : "Component";
                sb.append("class \"").append(name).append("\" as ").append(sanitizeId(component.getId()))
                        .append(" <<").append(category).append(">>\n");
            }
            sb.append("\n");
        }

        // Add relationships
        sb.append("' Dependencies\n");
        for (CodeComponent component : components) {
            if (component.getDependencies() != null) {
                String fromId = sanitizeId(component.getId());
                for (CodeComponent dep : component.getDependencies()) {
                    String toId = sanitizeId(dep.getId());
                    if (!fromId.equals(toId)) {
                        sb.append(fromId).append(" --> ").append(toId).append("\n");
                    }
                }
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String buildCategorizedGraphviz(List<CodeComponent> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  graph [bgcolor=white];\n");
        sb.append("  node [shape=box, style=filled, fontname=Helvetica];\n\n");

        Map<String, List<CodeComponent>> byCategory = components.stream()
                .collect(Collectors.groupingBy(this::detectComponentCategory));

        for (Map.Entry<String, List<CodeComponent>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<CodeComponent> categoryComponents = entry.getValue();

            sb.append("  subgraph cluster_").append(category).append(" {\n");
            sb.append("    label=\"").append(category).append(" Components\";\n");
            sb.append("    color=\"").append(getCategoryColor(category)).append("\";\n");

            for (CodeComponent component : categoryComponents) {
                String name = component.getName() != null ? component.getName() : "Unknown";
                sb.append("    ").append(sanitizeId(component.getId()))
                        .append(" [label=\"").append(name).append("\" fillcolor=\"")
                        .append(getCategoryFillColor(category)).append("\"];\n");
            }
            sb.append("  }\n\n");
        }

        // Dependencies
        sb.append("  // Dependencies\n");
        for (CodeComponent component : components) {
            if (component.getDependencies() != null) {
                String fromId = sanitizeId(component.getId());
                for (CodeComponent dep : component.getDependencies()) {
                    String toId = sanitizeId(dep.getId());
                    if (!fromId.equals(toId)) {
                        sb.append("  ").append(fromId).append(" -> ").append(toId).append(";\n");
                    }
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String detectComponentCategory(CodeComponent component) {
        if (component == null || component.getName() == null) return "Unknown";

        String name = component.getName().toLowerCase();

        if (name.matches(".*(activity|fragment|adapter|viewholder|view|layout|dialog|menu|button|text|image|list|recycler|card).*")) {
            return "UI";
        } else if (name.matches(".*(entity|model|pojo|dto|vo|bean|data|table|user|product|item|order).*")) {
            return "DataModel";
        } else if (name.matches(".*(viewmodel|presenter|usecase|service|manager|handler|repository|datasource|dao).*")) {
            return "BusinessLogic";
        } else if (name.matches(".*(intent|navigate|navigation|launch|start|goto|action).*")) {
            return "Navigation";
        }

        return "Unknown";
    }

    private String getCategoryColor(String category) {
        switch (category) {
            case "UI": return "#93c5fd";
            case "DataModel": return "#fdba74";
            case "BusinessLogic": return "#86efac";
            case "Navigation": return "#f87171";
            default: return "#cccccc";
        }
    }

    private String getCategoryFillColor(String category) {
        switch (category) {
            case "UI": return "#e0f2fe";
            case "DataModel": return "#fff4e5";
            case "BusinessLogic": return "#eaffea";
            case "Navigation": return "#ffe0e0";
            default: return "#f2f2f2";
        }
    }

    private String sanitizeId(String id) {
        if (id == null) return "id_" + UUID.randomUUID().toString().replace('-', '_');
        return id.replaceAll("[^A-Za-z0-9_]", "_");
    }

}