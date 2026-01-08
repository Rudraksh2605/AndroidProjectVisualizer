package com.projectvisualizer.controllers;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.CodeMethod;
import com.projectvisualizer.model.ComplexityInfo;
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
import com.projectvisualizer.services.ComponentCategorizer;
import com.projectvisualizer.visualization.UseCaseDiagramGenerator;

public class MainController implements Initializable {

    @FXML private VBox mainContainer;
    @FXML private TreeView<String> projectTreeView;
    @FXML private ScrollPane diagramScrollPane;
    @FXML private TabPane visualizationTabPane;
    @FXML private ListView<String> componentsListView;
    @FXML private TextArea plantUMLTextArea;
    @FXML private TextArea graphvizTextArea;
    @FXML private VBox statisticsContainer;

    @FXML private Tab plantUMLImageTab;
    @FXML private Tab graphvizImageTab;
    @FXML private ImageView plantUMLImageView;
    @FXML private ImageView graphvizImageView;

    @FXML private Label zoomLabel;

    @FXML private Label statusLabel;
    @FXML private Label projectInfoLabel;
    @FXML private Label memoryLabel;
    @FXML private Label statusZoomLabel;
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
    private String diagramViewMode = "ALL"; // used to filter diagram outputs
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
    private void handleCopyPlantUML() {
        if (plantUMLTextArea != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(plantUMLTextArea.getText() == null ? "" : plantUMLTextArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
            statusLabel.setText("PlantUML code copied to clipboard");
        }
    }

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
                // Clear canvas and show image, fit to canvas width (responsive)
                graphCanvas.getChildren().clear();
                ImageView iv = new ImageView(fxImage);
                iv.setPreserveRatio(true);
                iv.fitWidthProperty().bind(graphCanvas.widthProperty());
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
                iv.fitWidthProperty().bind(graphCanvas.widthProperty());
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
        if (plantUMLImageView == null) return;
        Image img = plantUMLImageView.getImage();
        if (img == null) return;
        double baseW = img.getWidth();
        double baseH = img.getHeight();
        if (baseW <= 0 || baseH <= 0) return;
        plantUMLImageView.setFitWidth(baseW * plantUmlZoom);
        plantUMLImageView.setFitHeight(baseH * plantUmlZoom);
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
        if (graphvizImageView == null) return;
        Image img = graphvizImageView.getImage();
        if (img == null) return;
        double baseW = img.getWidth();
        double baseH = img.getHeight();
        if (baseW <= 0 || baseH <= 0) return;
        graphvizImageView.setFitWidth(baseW * graphvizZoom);
        graphvizImageView.setFitHeight(baseH * graphvizZoom);
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
        // Ensure we clear any previous items to prevent duplicates on re-parse
        TreeItem<String> rootItem = projectTreeView.getRoot();
        if (rootItem == null) {
            rootItem = new TreeItem<>("Project Structure");
        } else {
            // Crucial: clear existing children before repopulating
            rootItem.getChildren().clear();
        }
        rootItem.setExpanded(true);

        // Group by file extensions as requested
        TreeItem<String> javaGroup = new TreeItem<>("Java Files (.java)");
        TreeItem<String> kotlinGroup = new TreeItem<>("Kotlin Files (.kt)");
        TreeItem<String> xmlGroup = new TreeItem<>("XML Files (.xml)");

        // Gatekeeper set to avoid adding the same file/component twice
        Set<String> addedKeys = new LinkedHashSet<>();

        for (CodeComponent component : components) {
            if (component == null) continue;
            String filePath = component.getFilePath();
            String language = component.getLanguage();

            // Build a stable key: prefer file path, then component id, then name+language
            String key;
            if (filePath != null && !filePath.isEmpty()) {
                key = filePath;
            } else if (component.getId() != null && !component.getId().isEmpty()) {
                key = "id:" + component.getId();
            } else {
                String base = component.getName() != null ? component.getName() : "Unknown";
                String lang = language != null ? language.toLowerCase() : "";
                key = "name:" + base + "|lang:" + lang;
            }

            // Skip duplicates
            if (!addedKeys.add(key)) {
                continue;
            }

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
                } else if (filePath != null && !filePath.isEmpty()) {
                    fileName = new File(filePath).getName();
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

    private void initializeViewModeMenu() {
        // Create menu items for different view modes
        MenuItem allItems = new MenuItem("Show All Components");
        MenuItem uiItems = new MenuItem("Show UI Components Only");
        MenuItem dataModelItems = new MenuItem("Show Data Models Only");
        MenuItem businessLogicItems = new MenuItem("Show Business Logic Only");
        MenuItem navigationItems = new MenuItem("Show Navigation Only");
        MenuItem useCaseItems = new MenuItem("Show Use Case Diagram");
        MenuItem relationshipsItems = new MenuItem("Show Component Relationships");

        allItems.setOnAction(e -> setViewMode("ALL"));
        uiItems.setOnAction(e -> setViewMode("UI"));
        dataModelItems.setOnAction(e -> setViewMode("DATA_MODEL"));
        businessLogicItems.setOnAction(e -> setViewMode("BUSINESS_LOGIC"));
        navigationItems.setOnAction(e -> setViewMode("NAVIGATION"));
        useCaseItems.setOnAction(e -> setViewMode("USE_CASE"));
        relationshipsItems.setOnAction(e -> showComponentRelationships());

        viewModeMenuButton.getItems().addAll(
                allItems, uiItems, dataModelItems, businessLogicItems,
                navigationItems, useCaseItems, new SeparatorMenuItem(), relationshipsItems
        );
    }

    private void setViewMode(String mode) {
        if (graphManager != null) {
            // Use the new method that only affects expanded nodes
            graphManager.setViewModeForExpandedNodes(mode);
            updateCategoryStats();
            statusLabel.setText("View mode: " + getViewModeDisplayName(mode) + " - applied to expanded nodes");
            // Also update diagram filtering mode and regenerate diagrams
            this.diagramViewMode = mode;
            if (currentAnalysisResult != null) {
                updateDiagrams(currentAnalysisResult);
            }
        }
    }

    private String getViewModeDisplayName(String mode) {
        switch (mode) {
            case "ALL": return "All Components";
            case "UI": return "UI Components";
            case "DATA_MODEL": return "Data Models";
            case "BUSINESS_LOGIC": return "Business Logic";
            case "NAVIGATION": return "Navigation/Intents";
            case "USE_CASE": return "Use Case Diagram";
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
        // Show dependencies for currently visible components only
        if (graphManager != null && graphManager.hasNodes()) {
            highlightDependencies();
            statusLabel.setText("Showing relationships for current selection");
        } else {
            statusLabel.setText("No components selected to show relationships");
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

        // Populate complexity statistics in Statistics tab
        populateStatisticsTab(result.getComponents());

        handleResetZoom();
        diagramScrollPane.setVvalue(0);
        diagramScrollPane.setHvalue(0);
    }

    /**
     * Populates the Statistics tab with complexity analysis for all components.
     */
    private void populateStatisticsTab(List<CodeComponent> components) {
        if (statisticsContainer == null) {
            System.err.println("Statistics container not bound");
            return;
        }

        statisticsContainer.getChildren().clear();

        // Summary header
        Label summaryLabel = new Label("📊 Complexity Analysis");
        summaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        statisticsContainer.getChildren().add(summaryLabel);

        // Collect stats
        int totalMethods = 0;
        int highComplexityMethods = 0;
        int mediumComplexityMethods = 0;
        int lowComplexityMethods = 0;

        for (CodeComponent component : components) {
            if (component.getMethods() == null) continue;
            for (CodeMethod method : component.getMethods()) {
                totalMethods++;
                ComplexityInfo info = method.getComplexityInfo();
                if (info != null) {
                    int severity = info.getSeverityLevel();
                    if (severity == 3) highComplexityMethods++;
                    else if (severity == 2) mediumComplexityMethods++;
                    else lowComplexityMethods++;
                } else {
                    lowComplexityMethods++;
                }
            }
        }

        // Summary stats
        HBox summaryBox = new HBox(20);
        summaryBox.setStyle("-fx-padding: 10; -fx-background-color: #f0f4f8; -fx-background-radius: 8;");
        summaryBox.getChildren().addAll(
            createStatCard("Total Methods", String.valueOf(totalMethods), "#3b82f6"),
            createStatCard("🟢 Low (O(1), O(log n))", String.valueOf(lowComplexityMethods), "#10b981"),
            createStatCard("🟡 Medium (O(n))", String.valueOf(mediumComplexityMethods), "#f59e0b"),
            createStatCard("🔴 High (O(n²)+)", String.valueOf(highComplexityMethods), "#ef4444")
        );
        statisticsContainer.getChildren().add(summaryBox);

        // Separator
        statisticsContainer.getChildren().add(new javafx.scene.control.Separator());

        // Per-component cards
        Label detailLabel = new Label("📄 Per-File Complexity Breakdown");
        detailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #555; -fx-padding: 10 0 5 0;");
        statisticsContainer.getChildren().add(detailLabel);

        for (CodeComponent component : components) {
            if (component.getMethods() == null || component.getMethods().isEmpty()) continue;

            // Create component card
            VBox componentCard = createComponentComplexityCard(component);
            statisticsContainer.getChildren().add(componentCard);
        }
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private VBox createComponentComplexityCard(CodeComponent component) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-margin: 5 0;");

        // Find worst complexity for header
        String worstComplexity = "O(1)";
        String emoji = "🟢";
        int maxSeverity = 1;

        for (CodeMethod method : component.getMethods()) {
            ComplexityInfo info = method.getComplexityInfo();
            if (info != null && info.getSeverityLevel() > maxSeverity) {
                maxSeverity = info.getSeverityLevel();
                worstComplexity = info.getTimeComplexity();
                emoji = info.getSeverityEmoji();
            }
        }

        // Header
        HBox header = new HBox(10);
        header.setStyle("-fx-alignment: center-left;");
        
        Label fileLabel = new Label("📄 " + component.getName());
        fileLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label complexityBadge = new Label(emoji + " " + worstComplexity);
        String badgeColor = maxSeverity == 3 ? "#fef2f2" : (maxSeverity == 2 ? "#fffbeb" : "#f0fdf4");
        String textColor = maxSeverity == 3 ? "#dc2626" : (maxSeverity == 2 ? "#d97706" : "#16a34a");
        complexityBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-padding: 4 8; " +
                                "-fx-background-radius: 4; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        
        header.getChildren().addAll(fileLabel, spacer, complexityBadge);
        card.getChildren().add(header);

        // Method list
        for (CodeMethod method : component.getMethods()) {
            ComplexityInfo info = method.getComplexityInfo();
            String time = info != null ? info.getTimeComplexity() : "O(1)";
            String space = info != null ? info.getSpaceComplexity() : "O(1)";
            String methodEmoji = info != null ? info.getSeverityEmoji() : "🟢";

            HBox methodRow = new HBox(10);
            methodRow.setStyle("-fx-padding: 4 0 4 20;");

            Label methodName = new Label("▸ " + method.getName() + "()");
            methodName.setStyle("-fx-font-size: 12px; -fx-text-fill: #444; -fx-min-width: 200;");

            Label timeLabel = new Label(methodEmoji + " Time: " + time);
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            Label spaceLabel = new Label("Space: " + space);
            spaceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

            methodRow.getChildren().addAll(methodName, timeLabel, spaceLabel);
            card.getChildren().add(methodRow);
        }

        return card;
    }

    private void updateDiagrams(AnalysisResult result) {
        try {
            // Check if Use Case Diagram mode is selected
            if ("USE_CASE".equals(diagramViewMode)) {
                if (plantUMLTextArea != null) {
                    UseCaseDiagramGenerator useCaseGenerator = new UseCaseDiagramGenerator();
                    // Pass both components and business processes for comprehensive use case extraction
                    String useCasePuml = useCaseGenerator.generatePlantUML(
                        result.getComponents(), 
                        result.getBusinessProcesses()
                    );
                    plantUMLTextArea.setText(useCasePuml);
                    renderPlantUml(false);
                }
                // Clear Graphviz area since Use Case is PlantUML-only
                if (graphvizTextArea != null) {
                    graphvizTextArea.setText("// Use Case diagrams are rendered in PlantUML only.\n// Switch to another view mode to see Graphviz diagrams.");
                }
                return;
            }
            
            // Default diagram generation for other modes
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
        // Layout improvements for wide screens and cleaner lines
        sb.append("left to right direction\n");
        sb.append("skinparam linetype polyline\n");
        sb.append("skinparam nodesep 60\n");
        sb.append("skinparam ranksep 100\n");
        sb.append("scale max 4096 width\n");
        sb.append("skinparam backgroundColor #ffffff\n");
        sb.append("skinparam class {\n");
        sb.append("  BackgroundColor<<UI>> #e0f2fe\n");
        sb.append("  BackgroundColor<<DataModel>> #fff4e5\n");
        sb.append("  BackgroundColor<<BusinessLogic>> #eaffea\n");
        sb.append("  BackgroundColor<<Navigation>> #ffe0e0\n");
        sb.append("  BackgroundColor<<Unknown>> #f2f2f2\n");
        sb.append("}\n\n");

        // 1) Filter by current diagram view mode
        List<CodeComponent> filtered = components.stream().filter(c -> {
            if (diagramViewMode == null || "ALL".equalsIgnoreCase(diagramViewMode)) return true;
            String cat = detectComponentCategoryForDiagrams(c);
            switch (diagramViewMode) {
                case "UI": return "UI".equals(cat);
                case "DATA_MODEL": return "DataModel".equals(cat);
                case "BUSINESS_LOGIC": return "BusinessLogic".equals(cat);
                case "NAVIGATION": return "Navigation".equals(cat);
                default: return true;
            }
        }).collect(Collectors.toList());

        // Precompute included IDs for relationship filtering
        Set<String> includedIds = filtered.stream()
                .map(CodeComponent::getId)
                .filter(Objects::nonNull)
                .map(this::sanitizeId)
                .collect(Collectors.toSet());

        // 2) Group by category using enhanced detection (on filtered set)
        Map<String, List<CodeComponent>> byCategory = filtered.stream()
                .collect(Collectors.groupingBy(this::detectComponentCategoryForDiagrams));

        for (Map.Entry<String, List<CodeComponent>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<CodeComponent> categoryComponents = entry.getValue();

            sb.append("' ").append(category).append(" Components\n");
            for (CodeComponent component : categoryComponents) {
                String name = component.getName() != null ? component.getName() : "Unknown";
                sb.append("class \"").append(name).append("\" as ")
                        .append(sanitizeId(component.getId()))
                        .append(" <<").append(category).append(">>\n");
            }
            sb.append("\n");
        }

        // 3) Add relationships only when both ends are present in filtered set
        sb.append("' Dependencies\n");
        for (CodeComponent component : filtered) {
            if (component.getDependencies() != null) {
                String fromId = sanitizeId(component.getId());
                if (!includedIds.contains(fromId)) continue; // should be present anyway
                for (CodeComponent dep : component.getDependencies()) {
                    String toId = sanitizeId(dep.getId());
                    if (!fromId.equals(toId) && includedIds.contains(toId)) {
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
        sb.append("  graph [bgcolor=white, splines=ortho, nodesep=1.0, ranksep=2.0];\n");
        sb.append("  node [shape=box, style=filled, fontname=Helvetica];\n\n");

        Map<String, List<CodeComponent>> byCategory = components.stream()
                .collect(Collectors.groupingBy(this::detectComponentCategoryForDiagrams));

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

    private String getCategoryColor(String category) {
        if (category == null) return "#cccccc";
        switch (category) {
            case "UI": return "#93c5fd";
            case "DataModel": return "#fdba74";
            case "BusinessLogic": return "#86efac";
            case "Navigation": return "#f87171";
            default: return "#cccccc";
        }
    }

    private String getCategoryFillColor(String category) {
        if (category == null) return "#f2f2f2";
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

    // In MainController.java - Replace detectComponentCategoryForDiagrams method with:
    private String detectComponentCategoryForDiagrams(CodeComponent component) {
        String category = ComponentCategorizer.detectCategory(component);
        // Convert to diagram format if needed
        if ("DATA_MODEL".equals(category)) return "DataModel";
        if ("BUSINESS_LOGIC".equals(category)) return "BusinessLogic";
        if ("NAVIGATION".equals(category)) return "Navigation";
        if ("UNKNOWN".equals(category)) return "Unknown";
        return category;
    }
}