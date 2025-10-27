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
import javafx.scene.shape.Circle;
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
                        renderPlantUml(true);
                    } else if (newTab == graphvizImageTab) {
                        renderGraphviz(true);
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

    private void handleAnalysisResult(AnalysisResult result) {
        statusLabel.setText("Project analysis complete");

        componentMap.clear();
        for (CodeComponent component : result.getComponents()) {
            // Add duplicate check here too for the main componentMap
            if (componentMap.containsKey(component.getId())) {
                System.err.println("Duplicate in main componentMap: " + component.getId());
                // Handle duplicate - you might want to append something to make it unique
                String uniqueId = component.getId() + "_" + System.currentTimeMillis();
                component.setId(uniqueId);
            }
            componentMap.put(component.getId(), component);
        }

        // RESOLVE DEPENDENCIES AFTER LOADING ALL COMPONENTS
        try {
            resolveDependencies(result.getComponents());
        } catch (Exception e) {
            System.err.println("Error resolving dependencies: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Warning: Some dependencies could not be resolved");
        }

        graphManager.clearGraph();

        int uiCount = filterUIComponents(result.getComponents()).size();
        int businessLogicCount = filterComponentsByLayer(result.getComponents(), "Business Logic").size();
        int dataCount = filterComponentsByLayer(result.getComponents(), "Data").size();

        statusLabel.setText(String.format(
                "Analysis complete - UI: %d, Business Logic: %d, Data: %d, Total: %d",
                uiCount, businessLogicCount, dataCount, result.getComponents().size()
        ));

        updateComponentList();
        updateProjectTreeWithRealData(result.getComponents());

        handleResetZoom();
        diagramScrollPane.setVvalue(0);
        diagramScrollPane.setHvalue(0);
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

    // View Menu Handlers
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
        statusLabel.setText("PlantUML code copied to clipboard");
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
        statusLabel.setText("Graphviz code copied to clipboard");
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

    // Component Selection and Filtering - FIXED TO ALLOW ALL COMPONENTS
    private void handleComponentSelection(TreeItem<String> selectedItem) {
        String itemValue = selectedItem.getValue();
        if (itemValue == null) return;

        // Try direct map by ID
        CodeComponent component = componentMap.get(itemValue);

        // If the item looks like a filename, try resolving by file path name
        if (component == null) {
            String filename = itemValue;
            String baseName = filename;
            String ext = null;
            int lastDot = filename.lastIndexOf('.');
            if (lastDot > 0 && lastDot < filename.length() - 1) {
                baseName = filename.substring(0, lastDot);
                ext = filename.substring(lastDot + 1).toLowerCase();
            }

            for (CodeComponent c : componentMap.values()) {
                String fp = c.getFilePath();
                if (fp != null) {
                    File f = new File(fp);
                    String fn = f.getName();
                    if (filename.equals(fn)) { // exact filename match with extension
                        component = c;
                        break;
                    }
                }
            }

            // Fallback: match by component name (without extension)
            if (component == null) {
                for (CodeComponent c : componentMap.values()) {
                    if (c.getName() != null && c.getName().equals(baseName)) {
                        // Optionally check language vs extension
                        if (ext == null || (ext.equals("kt") && "kotlin".equalsIgnoreCase(c.getLanguage()))
                                || (ext.equals("java") && c.getLanguage() != null && c.getLanguage().toLowerCase().startsWith("java"))
                                || (ext.equals("xml") && c.getFilePath() != null && c.getFilePath().toLowerCase().endsWith(".xml"))) {
                            component = c;
                            break;
                        }
                    }
                }
            }
        }

        if (component != null) {
            graphManager.addComponentToGraph(component);
            statusLabel.setText("Added " + component.getName() + " (" + component.getType() + " - " + component.getLayer() + ") to graph");
            autoScrollToNode(component.getId());
        } else {
            statusLabel.setText("Component not found: " + itemValue);
        }
    }

    private void autoScrollToNode(String componentId) {
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            diagramScrollPane.layout();
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
}