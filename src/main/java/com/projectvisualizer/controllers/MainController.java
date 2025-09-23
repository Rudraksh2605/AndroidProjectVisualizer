package com.projectvisualizer.controllers;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import javax.imageio.ImageIO;
import java.nio.file.Files;

import com.projectvisualizer.visualization.*;
import javafx.embed.swing.SwingFXUtils;
import com.projectvisualizer.services.ProjectAnalyzer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.StatusBar;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane visualizationTabPane;
    @FXML private Label statusLabel;
    @FXML private StatusBar statusBar;
    @FXML private VBox mainContainer;
    @FXML private Label zoomLabel;
    @FXML private Label statusZoomLabel;
    @FXML private Label memoryLabel;
    @FXML private Label projectInfoLabel;
    @FXML private Label progressLabel;
    @FXML private HBox progressContainer;
    @FXML private ProgressIndicator analysisProgressIndicator;
    @FXML private Circle connectionStatusIndicator;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> layerFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> languageFilterComboBox;
    @FXML private ComboBox<String> layoutComboBox;
    @FXML private CheckBox showLabelsCheckBox;
    @FXML private CheckBox showGridCheckBox;
    @FXML private CheckBox showMinimapCheckBox;
    @FXML private ListView<String> componentsListView;
    @FXML private VBox dependenciesContainer;
    @FXML private VBox statisticsContainer;
    @FXML private VBox metricsContainer;
    @FXML private ScrollPane diagramScrollPane;
    @FXML private ScrollPane minimapScrollPane;
    @FXML private ComboBox<String> visualizationModeComboBox;
    @FXML private Pane legendOverlayPane;
    @FXML private TextArea plantUMLTextArea;
    @FXML private TextArea graphvizTextArea;
    @FXML private ImageView plantUMLImageView;
    @FXML private ImageView graphvizImageView;
    @FXML private Tab plantUMLImageTab;
    @FXML private Tab graphvizImageTab;

    @FXML private ComboBox<String> abstractionLevelComboBox;
    @FXML private CheckBox enableClusteringCheckBox;
    @FXML private CheckBox enableEdgeAggregationCheckBox;
    @FXML private CheckBox hideUtilityClassesCheckBox;
    @FXML private CheckBox hideTestClassesCheckBox;
    @FXML private Slider complexityThresholdSlider;
    @FXML private ComboBox<String> featureFilterComboBox;
    @FXML private Button expandAllButton;
    @FXML private Button collapseAllButton;

    private EnhancedGraphExporter enhancedExporter;
    private AbstractionLevel currentAbstractionLevel = AbstractionLevel.HIGH_LEVEL;
    private VisualizationConfig visualizationConfig = new VisualizationConfig();
    private ProjectAnalyzer projectAnalyzer;
    private GraphVisualizer graphVisualizer;
    private double currentZoomLevel = 1.0;
    private double plantUMLZoomFactor = 1.0;
    private double graphvizZoomFactor = 1.0;
    private BufferedImage plantUMLBufferedImage;
    private BufferedImage graphvizBufferedImage;

    private ProjectAnalysisResult currentAnalysisResult;


    private GraphVisualizer.LayoutType currentLayoutType = GraphVisualizer.LayoutType.HIERARCHICAL;
    private boolean showLabels = true;
    private boolean showGrid = false;

    @FXML
    public void initialize() {
        projectAnalyzer = new ProjectAnalyzer();
        graphVisualizer = new GraphVisualizer();
        enhancedExporter = new EnhancedGraphExporter();
        setupProjectTree();
        setupStatusBar();
        setupComboBoxes();
        setupCheckBoxes();
        setupImageTabs();

        setupEnhancedControls();



        visualizationModeComboBox.setValue("Technical Architecture");

        visualizationModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVisualizationMode(newVal);
        });

        // Set up memory monitoring
        setupMemoryMonitoring();
    }

    private void setupImageTabs() {
        // Add listeners to generate images when tabs are selected
        plantUMLImageTab.setOnSelectionChanged(event -> {
            if (plantUMLImageTab.isSelected() && currentAnalysisResult != null) {
                generatePlantUMLImage();
            }
        });

        graphvizImageTab.setOnSelectionChanged(event -> {
            if (graphvizImageTab.isSelected() && currentAnalysisResult != null) {
                generateGraphvizImage();
            }
        });
    }

    private void updateVisualizationMode(String mode) {
        GraphVisualizer.VisualizationMode vizMode = mapStringToVisualizationMode(mode);
        graphVisualizer.setVisualizationMode(vizMode);

        // Set appropriate abstraction level based on mode
        switch (vizMode) {
            case TECHNICAL_ARCHITECTURE:
                abstractionLevelComboBox.setValue(AbstractionLevel.HIGH_LEVEL.getDisplayName());
                currentAbstractionLevel = AbstractionLevel.HIGH_LEVEL;
                break;
            case USER_JOURNEY:
                // User journey view uses a different approach, not directly mapped to abstraction levels
                // Keep current abstraction level or set to a default
                break;
            case BUSINESS_PROCESS:
                abstractionLevelComboBox.setValue(AbstractionLevel.COMPONENT_FLOW.getDisplayName());
                currentAbstractionLevel = AbstractionLevel.COMPONENT_FLOW;
                break;
            case FEATURE_OVERVIEW:
                abstractionLevelComboBox.setValue(AbstractionLevel.FEATURE_BASED.getDisplayName());
                currentAbstractionLevel = AbstractionLevel.FEATURE_BASED;
                break;
            case INTEGRATION_MAP:
                abstractionLevelComboBox.setValue(AbstractionLevel.DETAILED.getDisplayName());
                currentAbstractionLevel = AbstractionLevel.DETAILED;
                break;
            default:
                abstractionLevelComboBox.setValue(AbstractionLevel.HIGH_LEVEL.getDisplayName());
                currentAbstractionLevel = AbstractionLevel.HIGH_LEVEL;
        }

        // Update the graph visualizer with the current abstraction level
        graphVisualizer.setAbstractionLevel(currentAbstractionLevel);

        refreshGraph();
    }


    private void setupComboBoxes() {
        // Setup layer filter
        layerFilterComboBox.getItems().addAll("All Layers", "UI", "Business Logic", "Data", "Other");
        layerFilterComboBox.setValue("All Layers");

        // Setup layout combo
        layoutComboBox.getItems().addAll("Hierarchical", "Force-Directed", "Circular", "Grid", "Layered");
        layoutComboBox.setValue("Grid");

        layoutComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "Hierarchical":
                    currentLayoutType = GraphVisualizer.LayoutType.HIERARCHICAL;
                    break;
                case "Force-Directed":
                    currentLayoutType = GraphVisualizer.LayoutType.FORCE_DIRECTED;
                    break;
                case "Circular":
                    currentLayoutType = GraphVisualizer.LayoutType.CIRCULAR;
                    break;
                case "Grid":
                    currentLayoutType = GraphVisualizer.LayoutType.GRID;
                    break;
                case "Layered":
                    currentLayoutType = GraphVisualizer.LayoutType.LAYERED;
                    break;
            }
            refreshGraph();
        });

        // Enhanced visualization mode combo
        visualizationModeComboBox.getItems().addAll(
                "Technical Architecture", "User Journey", "Business Process",
                "Feature Overview", "Integration Map"
        );
        visualizationModeComboBox.setValue("Technical Architecture");

        visualizationModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVisualizationMode(newVal);
        });
    }

    private void refreshGraph() {
        if (currentAnalysisResult != null) {
            // Update graph visualizer settings
            graphVisualizer.setLayoutType(currentLayoutType);
            graphVisualizer.setAbstractionLevel(currentAbstractionLevel);
            graphVisualizer.setVisualizationConfig(visualizationConfig);
            graphVisualizer.setShowLabels(showLabels);
            graphVisualizer.setShowGrid(showGrid);

            // Update feature filter options
            updateFeatureFilterOptions();

            // Refresh the diagram
            ScrollPane diagram = graphVisualizer.createGraphView(currentAnalysisResult);
            diagramScrollPane.setContent(diagram.getContent());

            // Place legend into fixed overlay
            if (legendOverlayPane != null) {
                legendOverlayPane.getChildren().clear();
                javafx.scene.layout.Region legend = graphVisualizer.getLegend();
                if (legend != null) {
                    legendOverlayPane.getChildren().add(legend);
                }
            }

            // Auto-fit after layout
            javafx.application.Platform.runLater(() -> {
                graphVisualizer.fitToWindow(diagramScrollPane);
                currentZoomLevel = graphVisualizer.getCurrentZoom();
                updateZoom();
            });

            // Update export text areas with enhanced content
            updateExportTextAreas();
        }
    }


    private void setupCheckBoxes() {
        // Set up check box listeners
        showLabelsCheckBox.setSelected(true);
        showGridCheckBox.setSelected(false);
        showMinimapCheckBox.setSelected(false);

        // Add listeners
        showLabelsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showLabels = newVal;
            refreshGraph();
        });

        showGridCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showGrid = newVal;
            refreshGraph();
        });
    }

    private void setupMemoryMonitoring() {
        // Update memory usage periodically
        javafx.animation.AnimationTimer memoryTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                memoryLabel.setText(String.format("Memory: %d MB", usedMemory / (1024 * 1024)));
            }
        };
        memoryTimer.start();
    }

    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");
        File selectedDirectory = directoryChooser.showDialog(mainContainer.getScene().getWindow());

        if (selectedDirectory != null) {
            analyzeProject(selectedDirectory);
        }
    }

    @FXML
    private void handleExportDiagram() {
        if (currentAnalysisResult == null) {
            showInfoDialog("Export", "No project loaded", "Please open a project first.");
            return;
        }

        // Create a custom dialog with export options
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Export Diagram");
        dialog.setHeaderText("Choose export format:");

        ButtonType pngButton = new ButtonType("PNG Image");
        ButtonType jpgButton = new ButtonType("JPEG Image");
        ButtonType plantUmlButton = new ButtonType("PlantUML");
        ButtonType graphvizButton = new ButtonType("Graphviz");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(pngButton, jpgButton, plantUmlButton, graphvizButton, cancelButton);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == pngButton) return "png";
            if (dialogButton == jpgButton) return "jpg";
            if (dialogButton == plantUmlButton) return "puml";
            if (dialogButton == graphvizButton) return "dot";
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(format -> {
            switch (format) {
                case "png":
                case "jpg":
                    exportImage(format);
                    break;
                case "puml":
                    handleExportToPlantUML();
                    break;
                case "dot":
                    handleExportToGraphviz();
                    break;
            }
        });
    }

    private void exportImage(String format) {
        try {
            WritableImage image = diagramScrollPane.snapshot(null, null);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Diagram as " + format.toUpperCase());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    format.toUpperCase() + " Image", "*." + format));

            File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
            if (file != null) {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), format, file);
                showInfoDialog("Export Successful", "Diagram exported successfully",
                        "The diagram has been exported to: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            showErrorDialog("Export Error", "Failed to export diagram", e.getMessage());
        }
    }

    @FXML
    private void handleExportReport() {
        showInfoDialog("Export Report", "Report functionality", "Report export feature will be implemented in future versions.");
    }

    @FXML
    private void handleRefreshAnalysis() {
        if (currentAnalysisResult != null) {
            analyzeProject(new File(currentAnalysisResult.getProjectPath()));
        } else {
            showInfoDialog("Refresh", "No project loaded", "Please open a project first.");
        }
    }


    @FXML
    private void handleZoomIn() {
        graphVisualizer.zoom(1.2, diagramScrollPane);
        currentZoomLevel *= 1.2;
        updateZoom();
    }

    @FXML
    private void handleZoomOut() {
        graphVisualizer.zoom(0.8, diagramScrollPane);
        currentZoomLevel *= 0.8;
        updateZoom();
    }

    @FXML
    private void handleResetZoom() {
        graphVisualizer.zoom(1.0/currentZoomLevel, diagramScrollPane);
        currentZoomLevel = 1.0;
        updateZoom();
    }
    @FXML
    private void handleFitToWindow() {
        if (diagramScrollPane != null && graphVisualizer != null) {
            javafx.application.Platform.runLater(() -> {
                graphVisualizer.fitToWindow(diagramScrollPane);
                currentZoomLevel = graphVisualizer.getCurrentZoom();
                updateZoom();
            });
        }
    }

    @FXML
    private void handleToggleGrid() {
        boolean showGrid = showGridCheckBox.isSelected();
        // Implementation to show/hide grid
    }

    @FXML
    private void handleToggleMinimap() {
        boolean showMinimap = showMinimapCheckBox.isSelected();
        minimapScrollPane.setVisible(showMinimap);
        minimapScrollPane.setManaged(showMinimap);
    }

    @FXML
    private void handleToggleLabels() {
        boolean showLabels = showLabelsCheckBox.isSelected();
        // Implementation to show/hide labels
    }

    @FXML
    private void handleToggleDarkMode() {
        showInfoDialog("Dark Mode", "Dark mode functionality", "Dark mode feature will be implemented in future versions.");
    }

    @FXML
    private void handleLayoutChange() {
        String selectedLayout = layoutComboBox.getValue();
        // Implementation to change layout
    }

    @FXML
    private void handleTypeFilter() {
        showInfoDialog("Type Filter", "Type filter functionality", "Type filter feature will be implemented in future versions.");
    }

    @FXML
    private void handleLanguageFilter() {
        showInfoDialog("Language Filter", "Language filter functionality", "Language filter feature will be implemented in future versions.");
    }

    @FXML
    private void handleRefreshTree() {
        showInfoDialog("Refresh Tree", "Refresh tree functionality", "Refresh tree feature will be implemented in future versions.");
    }

    @FXML
    private void handleTreeSettings() {
        showInfoDialog("Tree Settings", "Tree settings functionality", "Tree settings feature will be implemented in future versions.");
    }

    @FXML
    private void handleNavigateToSource() {
        showInfoDialog("Navigate to Source", "Navigate to source functionality", "Navigate to source feature will be implemented in future versions.");
    }

    @FXML
    private void handleShowComponentDependencies() {
        showInfoDialog("Show Dependencies", "Show dependencies functionality", "Show dependencies feature will be implemented in future versions.");
    }

    @FXML
    private void handleFindReferences() {
        showInfoDialog("Find References", "Find references functionality", "Find references feature will be implemented in future versions.");
    }

    @FXML
    private void handleGenerateDocumentation() {
        showInfoDialog("Generate Documentation", "Generate documentation functionality", "Generate documentation feature will be implemented in future versions.");
    }

    @FXML
    private void handleSettings() {
        showInfoDialog("Settings", "Settings functionality", "Settings feature will be implemented in future versions.");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleDeepAnalysis() {
        showInfoDialog("Deep Analysis", "Deep analysis functionality", "Deep analysis feature will be implemented in future versions.");
    }

    @FXML
    private void handleFindComponent() {
        showInfoDialog("Find Component", "Find component functionality", "Find component feature will be implemented in future versions.");
    }

    @FXML
    private void handleShowDependencies() {
        showInfoDialog("Show Dependencies", "Show dependencies functionality", "Show dependencies feature will be implemented in future versions.");
    }

    @FXML
    private void handleAutoLayout() {
        showInfoDialog("Auto Layout", "Auto layout functionality", "Auto layout feature will be implemented in future versions.");
    }



    @FXML
    private void handleUserGuide() {
        showInfoDialog("User Guide", "User guide functionality", "User guide feature will be implemented in future versions.");
    }

    @FXML
    private void handleKeyboardShortcuts() {
        showInfoDialog("Keyboard Shortcuts", "Keyboard shortcuts functionality", "Keyboard shortcuts feature will be implemented in future versions.");
    }

    @FXML
    private void handleCheckUpdates() {
        showInfoDialog("Check for Updates", "Check for updates functionality", "Check for updates feature will be implemented in future versions.");
    }

    @FXML
    private void handleAbout() {
        showInfoDialog("About CodeCartographer", "About functionality", "CodeCartographer v1.0 - Project Architecture Visualizer");
    }

    private void updateZoom() {
        zoomLabel.setText(String.format("%d%%", (int)(currentZoomLevel * 100)));
        statusZoomLabel.setText(String.format("%d%%", (int)(currentZoomLevel * 100)));
        // Implementation to actually zoom the diagram
    }

    private void analyzeProject(File projectDir) {
        statusLabel.setText("Analyzing project...");
        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        analysisProgressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressLabel.setText("Analyzing...");

        new Thread(() -> {
            try {
                ProjectAnalysisResult result = projectAnalyzer.analyze(projectDir,
                        (progress, message) -> Platform.runLater(() -> {
                            analysisProgressIndicator.setProgress(progress);
                            progressLabel.setText(message);
                        }));

                Platform.runLater(() -> {
                    currentAnalysisResult = result;
                    updateProjectTree(result);
                    updateVisualizationTabs(result);
                    statusLabel.setText("Analysis complete");
                    analysisProgressIndicator.setProgress(1.0);
                    progressContainer.setVisible(false);
                    progressContainer.setManaged(false);
                    projectInfoLabel.setText(result.getProjectName() + " - " + result.getComponents().size() + " components");
                    connectionStatusIndicator.setFill(Color.GREEN);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Analysis failed: " + e.getMessage());
                    analysisProgressIndicator.setProgress(0);
                    progressContainer.setVisible(false);
                    progressContainer.setManaged(false);
                    connectionStatusIndicator.setFill(Color.RED);
                    showErrorDialog("Analysis Error", "Failed to analyze project", e.getMessage());
                });
            }
        }).start();
    }

    private void setupProjectTree() {
        TreeItem<String> rootItem = new TreeItem<>("Projects");
        rootItem.setExpanded(true);
        projectTreeView.setRoot(rootItem);
        projectTreeView.setShowRoot(false);
    }

    private void setupStatusBar() {
        statusLabel.setText("Ready");
        projectInfoLabel.setText("");
        memoryLabel.setText("Memory: 0 MB");
        statusZoomLabel.setText("100%");
        connectionStatusIndicator.setFill(Color.GREEN);
    }

    private void updateProjectTree(ProjectAnalysisResult result) {
        TreeItem<String> root = projectTreeView.getRoot();
        root.getChildren().clear();

        TreeItem<String> projectNode = new TreeItem<>(result.getProjectName());
        root.getChildren().add(projectNode);

        // Add components to tree
        result.getComponents().forEach(component -> {
            TreeItem<String> componentNode = new TreeItem<>(component.getName() + " (" + component.getType() + ")");
            projectNode.getChildren().add(componentNode);
        });

        projectNode.setExpanded(true);
    }

    private void updateVisualizationTabs(ProjectAnalysisResult result) {
        // Set current settings before creating the graph
        graphVisualizer.setLayoutType(currentLayoutType);
        graphVisualizer.setShowLabels(showLabels);
        graphVisualizer.setShowGrid(showGrid);

        // Update components list
        componentsListView.getItems().clear();
        result.getComponents().forEach(component ->
                componentsListView.getItems().add(component.getName() + " - " + component.getType()));

        // Update dependencies view
        updateDependenciesView(result);

        GraphExporter exporter = new GraphExporter();
        plantUMLTextArea.setText(exporter.exportToPlantUML(result));
        graphvizTextArea.setText(exporter.exportToGraphviz(result));

        // Update statistics view
        updateStatisticsView(result);

        // Update metrics view
        updateMetricsView(result);

        // Update diagram
        ScrollPane diagram = graphVisualizer.createGraphView(result);
        // Avoid nesting scroll panes; use the diagram content (shared canvas)
        diagramScrollPane.setContent(diagram.getContent());

        if (plantUMLImageTab.isSelected()) {
            generatePlantUMLImage();
        }
        if (graphvizImageTab.isSelected()) {
            generateGraphvizImage();
        }
    }

    private void updateDependenciesView(ProjectAnalysisResult result) {
        dependenciesContainer.getChildren().clear();

        // Section: Dependency Injection Usages
        VBox diSection = new VBox(6);
        Label diTitle = new Label("Dependency Injection Usages");
        diTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        // Build map: dependency type -> list of classes where injected
        java.util.Map<String, java.util.List<String>> diMap = new java.util.HashMap<>();
        result.getComponents().forEach(component -> {
            if (component.getInjectedDependencies() != null) {
                for (String depType : component.getInjectedDependencies()) {
                    if (depType == null || depType.trim().isEmpty()) continue;
                    String key = depType.trim();
                    diMap.computeIfAbsent(key, k -> new java.util.ArrayList<String>());
                    String className = component.getId() != null ? component.getId() : component.getName();
                    if (className != null && !diMap.get(key).contains(className)) {
                        diMap.get(key).add(className);
                    }
                }
            }
        });

        if (diMap.isEmpty()) {
            Label none = new Label("No injected dependencies detected.");
            none.setTextFill(Color.GRAY);
            diSection.getChildren().add(none);
        } else {
            // Sort dependencies by name
            java.util.List<String> deps = new java.util.ArrayList<>(diMap.keySet());
            java.util.Collections.sort(deps, String.CASE_INSENSITIVE_ORDER);

            for (String dep : deps) {
                java.util.List<String> usages = diMap.get(dep);
                // Sort usages
                java.util.Collections.sort(usages, String.CASE_INSENSITIVE_ORDER);

                TitledPane pane = new TitledPane();
                pane.setText(dep + "  (" + usages.size() + ")");
                VBox content = new VBox(2);
                for (String cls : usages) {
                    Label lbl = new Label("• " + cls);
                    lbl.setStyle("-fx-text-fill: #374151;");
                    content.getChildren().add(lbl);
                }
                pane.setContent(content);
                pane.setExpanded(false);
                diSection.getChildren().add(pane);
            }
        }

        dependenciesContainer.getChildren().addAll(diTitle, diSection);

        // Section: Gradle Dependencies (existing)
        if (!result.getGradleDependencies().isEmpty()) {
            Label gradleLabel = new Label("Gradle Dependencies:");
            gradleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            VBox gradleBox = new VBox(5);
            result.getGradleDependencies().forEach(dep -> {
                HBox depRow = new HBox(10);
                Label nameLabel = new Label(dep.getName());
                Label versionLabel = new Label(dep.getVersion());
                versionLabel.setTextFill(Color.GRAY);
                depRow.getChildren().addAll(nameLabel, versionLabel);
                gradleBox.getChildren().add(depRow);
            });

            dependenciesContainer.getChildren().addAll(gradleLabel, gradleBox);
        }
    }

    private void updateStatisticsView(ProjectAnalysisResult result) {
        statisticsContainer.getChildren().clear();

        Label titleLabel = new Label("Project Statistics");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(15));
        statsGrid.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // Add statistics
        addStatistic(statsGrid, 0, "Total Components", String.valueOf(result.getComponents().size()), "#3b82f6");
        addStatistic(statsGrid, 1, "Total Relationships", String.valueOf(result.getRelationships().size()), "#10b981");

        statisticsContainer.getChildren().addAll(titleLabel, statsGrid);
    }

    private void updateMetricsView(ProjectAnalysisResult result) {
        metricsContainer.getChildren().clear();

        Label titleLabel = new Label("Code Quality Metrics");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        metricsContainer.getChildren().add(titleLabel);
        // Add metrics implementation here
    }

    private void addStatistic(GridPane grid, int row, String label, String value, String color) {
        Label statLabel = new Label(label + ":");
        statLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #4b5563;");

        Label statValue = new Label(value);
        statValue.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + color + ";");

        grid.add(statLabel, 0, row);
        grid.add(statValue, 1, row);
    }

    private void filterByLayer(String layer) {
        // Implementation to filter components by layer
        if (currentAnalysisResult != null) {
            // Filter logic here
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleHierarchicalLayout() {
        layoutComboBox.setValue("Hierarchical");
    }

    @FXML
    private void handleCircularLayout() {
        layoutComboBox.setValue("Circular");
    }

    @FXML
    private void handleForceDirectedLayout() {
        layoutComboBox.setValue("Force-Directed");
    }

    @FXML
    private void handleGridLayout() {
        layoutComboBox.setValue("Grid");
    }

    @FXML
    private void handleLayeredLayout() {
        layoutComboBox.setValue("Layered");
    }

    private GraphVisualizer.VisualizationMode mapStringToVisualizationMode(String mode) {
        switch (mode) {
            case "User Journey":
                return GraphVisualizer.VisualizationMode.USER_JOURNEY;
            case "Business Process":
                return GraphVisualizer.VisualizationMode.BUSINESS_PROCESS;
            case "Feature Overview":
                return GraphVisualizer.VisualizationMode.FEATURE_OVERVIEW;
            case "Integration Map":
                return GraphVisualizer.VisualizationMode.INTEGRATION_MAP;
            case "Technical Architecture":
            default:
                return GraphVisualizer.VisualizationMode.TECHNICAL_ARCHITECTURE;
        }
    }

    @FXML
    private void handleExportToPlantUML() {
        if (currentAnalysisResult == null) {
            showInfoDialog("Export", "No project loaded", "Please open a project first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to PlantUML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PlantUML Files", "*.puml"));
        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            try {
                String plantUMLContent = new GraphExporter().exportToPlantUML(currentAnalysisResult);
                Files.write(file.toPath(), plantUMLContent.getBytes());
                showInfoDialog("Export Successful", "PlantUML export complete",
                        "The diagram has been exported to PlantUML format: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export PlantUML", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportToGraphviz() {
        if (currentAnalysisResult == null) {
            showInfoDialog("Export", "No project loaded", "Please open a project first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to Graphviz");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphviz Files", "*.dot"));
        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            try {
                String graphvizContent = new GraphExporter().exportToGraphviz(currentAnalysisResult);
                Files.write(file.toPath(), graphvizContent.getBytes());
                showInfoDialog("Export Successful", "Graphviz export complete",
                        "The diagram has been exported to Graphviz format: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export Graphviz", e.getMessage());
            }
        }
    }


    @FXML
    private void handleCopyPlantUML() {
        if (plantUMLTextArea.getText() != null && !plantUMLTextArea.getText().isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(plantUMLTextArea.getText());
            clipboard.setContent(content);
            showInfoDialog("Copied", "PlantUML code copied to clipboard", "");
        }
    }

    @FXML
    private void handleCopyGraphviz() {
        if (graphvizTextArea.getText() != null && !graphvizTextArea.getText().isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(graphvizTextArea.getText());
            clipboard.setContent(content);
            showInfoDialog("Copied", "Graphviz code copied to clipboard", "");
        }
    }

    private void generatePlantUMLImage() {
        try {
            BufferedImage image = new GraphExporter().exportToPlantUMLImage(currentAnalysisResult);
            plantUMLBufferedImage = image; // Store the original image
            plantUMLImageView.setImage(GraphExporter.convertToFxImage(image));
            resetPlantUMLZoom(); // Reset zoom to 1.0 when generating new image
        } catch (Exception e) {
            showErrorDialog("Image Generation Error", "Failed to generate PlantUML image", e.getMessage());
        }
    }

    private void generateGraphvizImage() {
        try {
            BufferedImage image = new GraphExporter().exportToGraphvizImage(currentAnalysisResult);
            graphvizBufferedImage = image; // Store the original image
            graphvizImageView.setImage(GraphExporter.convertToFxImage(image));
            resetGraphvizZoom(); // Reset zoom to 1.0 when generating new image
        } catch (Exception e) {
            showErrorDialog("Image Generation Error",
                    "Failed to generate Graphviz image. Make sure Graphviz is installed.",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetPlantUMLZoom() {
        plantUMLZoomFactor = 1.0;
        updatePlantUMLImageZoom();
    }

    private void resetGraphvizZoom() {
        graphvizZoomFactor = 1.0;
        updateGraphvizImageZoom();
    }

    @FXML
    private void handleZoomInPlantUML() {
        plantUMLZoomFactor *= 1.2;
        updatePlantUMLImageZoom();
    }

    @FXML
    private void handleZoomOutPlantUML() {
        plantUMLZoomFactor /= 1.2;
        updatePlantUMLImageZoom();
    }

    @FXML
    private void handleResetZoomPlantUML() {
        plantUMLZoomFactor = 1.0;
        updatePlantUMLImageZoom();
    }

    private void updatePlantUMLImageZoom() {
        plantUMLImageView.setFitWidth(800 * plantUMLZoomFactor);
    }

    @FXML
    private void handleZoomInGraphviz() {
        graphvizZoomFactor *= 1.2;
        updateGraphvizImageZoom();
    }

    @FXML
    private void handleZoomOutGraphviz() {
        graphvizZoomFactor /= 1.2;
        updateGraphvizImageZoom();
    }

    @FXML
    private void handleResetZoomGraphviz() {
        graphvizZoomFactor = 1.0;
        updateGraphvizImageZoom();
    }

    private void updateGraphvizImageZoom() {
        graphvizImageView.setFitWidth(800 * graphvizZoomFactor);
    }

    @FXML
    private void handleExportPlantUMLImage() {
        if (plantUMLBufferedImage == null) {
            showInfoDialog("Export", "No PlantUML image", "Please generate the PlantUML image first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PlantUML Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                String format = file.getName().toLowerCase().endsWith(".jpg") ? "jpg" : "png";
                ImageIO.write(plantUMLBufferedImage, format, file);
                showInfoDialog("Export Successful", "PlantUML image exported successfully",
                        "The image has been exported to: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export PlantUML image", e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportGraphvizImage() {
        if (graphvizBufferedImage == null) {
            showInfoDialog("Export", "No Graphviz image", "Please generate the Graphviz image first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Graphviz Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg")
        );

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                String format = file.getName().toLowerCase().endsWith(".jpg") ? "jpg" : "png";
                ImageIO.write(graphvizBufferedImage, format, file);
                showInfoDialog("Export Successful", "Graphviz image exported successfully",
                        "The image has been exported to: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export Graphviz image", e.getMessage());
            }
        }
    }



    private void setupEnhancedControls() {
        // Setup abstraction level combo box
        if (abstractionLevelComboBox != null) {
            abstractionLevelComboBox.getItems().addAll(
                    AbstractionLevel.HIGH_LEVEL.getDisplayName(),
                    AbstractionLevel.COMPONENT_FLOW.getDisplayName(),
                    AbstractionLevel.LAYERED_ARCHITECTURE.getDisplayName(),
                    AbstractionLevel.FEATURE_BASED.getDisplayName(),
                    AbstractionLevel.DETAILED.getDisplayName()
            );
            abstractionLevelComboBox.setValue(AbstractionLevel.HIGH_LEVEL.getDisplayName());

            abstractionLevelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateAbstractionLevel(newVal);
            });
        }

        // Setup configuration checkboxes
        if (enableClusteringCheckBox != null) {
            enableClusteringCheckBox.setSelected(visualizationConfig.isEnableClustering());
            enableClusteringCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                visualizationConfig.setEnableClustering(newVal);
                refreshGraph();
            });
        }

        if (enableEdgeAggregationCheckBox != null) {
            enableEdgeAggregationCheckBox.setSelected(visualizationConfig.isEnableEdgeAggregation());
            enableEdgeAggregationCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                visualizationConfig.setEnableEdgeAggregation(newVal);
                refreshGraph();
            });
        }

        if (hideUtilityClassesCheckBox != null) {
            hideUtilityClassesCheckBox.setSelected(visualizationConfig.isHideUtilityClasses());
            hideUtilityClassesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                visualizationConfig.setHideUtilityClasses(newVal);
                refreshGraph();
            });
        }

        if (hideTestClassesCheckBox != null) {
            hideTestClassesCheckBox.setSelected(visualizationConfig.isHideTestClasses());
            hideTestClassesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                visualizationConfig.setHideTestClasses(newVal);
                refreshGraph();
            });
        }

        // Setup feature filter
        if (featureFilterComboBox != null) {
            featureFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if ("All Features".equals(newVal)) {
                    graphVisualizer.clearFilters();
                } else {
                    graphVisualizer.filterByFeature(newVal);
                }
            });
        }

        // Setup expand/collapse buttons
        if (expandAllButton != null) {
            expandAllButton.setOnAction(event -> expandAllNodes());
        }
        if (collapseAllButton != null) {
            collapseAllButton.setOnAction(event -> collapseAllNodes());
        }
    }

    private void updateAbstractionLevel(String levelDisplayName) {
        for (AbstractionLevel level : AbstractionLevel.values()) {
            if (level.getDisplayName().equals(levelDisplayName)) {
                currentAbstractionLevel = level;
                graphVisualizer.setAbstractionLevel(level);
                refreshGraph();
                break;
            }
        }
    }

    private void expandAllNodes() {
        VisualizationGraph graph = graphVisualizer.getCurrentGraph();
        if (graph != null) {
            for (VisualizationNode node : graph.getAllNodes()) {
                if (node.isExpandable()) {
                    graph.expandNode(node.getId());
                }
            }
            refreshGraph();
        }
    }

    private void collapseAllNodes() {
        VisualizationGraph graph = graphVisualizer.getCurrentGraph();
        if (graph != null) {
            for (VisualizationNode node : graph.getAllNodes()) {
                if (node.isExpandable()) {
                    graph.collapseNode(node.getId());
                }
            }
            refreshGraph();
        }
    }

    private void updateFeatureFilterOptions() {
        if (featureFilterComboBox != null && currentAnalysisResult != null) {
            featureFilterComboBox.getItems().clear();
            featureFilterComboBox.getItems().add("All Features");

            // Extract unique features from components
            currentAnalysisResult.getComponents().stream()
                    .map(this::inferFeatureFromComponent)
                    .distinct()
                    .sorted()
                    .forEach(featureFilterComboBox.getItems()::add);

            featureFilterComboBox.setValue("All Features");
        }
    }

    private String inferFeatureFromComponent(CodeComponent component) {
        String packagePath = component.getId();
        String[] parts = packagePath.split("\\.");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase();
            if (part.equals("feature") || part.equals("features")) {
                if (i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            if (part.matches("(login|auth|profile|dashboard|home|settings|payment|search|chat|notification)")) {
                return part;
            }
        }

        return "core";
    }

    private void updateExportTextAreas() {
        if (graphVisualizer.getCurrentGraph() != null) {
            // Use enhanced exporter for better output
            String plantUMLContent = enhancedExporter.exportToPlantUML(graphVisualizer.getCurrentGraph());
            String graphvizContent = enhancedExporter.exportToGraphviz(graphVisualizer.getCurrentGraph());

            plantUMLTextArea.setText(plantUMLContent);
            graphvizTextArea.setText(graphvizContent);
        }
    }

    @FXML
    private void handleNodeClick(String nodeId) {
        graphVisualizer.toggleNodeExpansion(nodeId);
    }


    @FXML
    private void handleExportEnhancedDiagram() {
        if (currentAnalysisResult == null) {
            showInfoDialog("Export", "No project loaded", "Please open a project first.");
            return;
        }

        VisualizationGraph graph = graphVisualizer.getCurrentGraph();
        if (graph == null) {
            showInfoDialog("Export", "No visualization available", "Please create a visualization first.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Export Enhanced Diagram");
        dialog.setHeaderText("Choose export format:");

        ButtonType plantUmlButton = new ButtonType("Enhanced PlantUML");
        ButtonType graphvizButton = new ButtonType("Enhanced Graphviz");
        ButtonType pngButton = new ButtonType("PNG Image");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(plantUmlButton, graphvizButton, pngButton, cancelButton);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == plantUmlButton) return "enhanced_puml";
            if (dialogButton == graphvizButton) return "enhanced_dot";
            if (dialogButton == pngButton) return "png";
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(format -> {
            switch (format) {
                case "enhanced_puml":
                    exportEnhancedPlantUML();
                    break;
                case "enhanced_dot":
                    exportEnhancedGraphviz();
                    break;
                case "png":
                    exportImage("png");
                    break;
            }
        });
    }

    private void exportEnhancedPlantUML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Enhanced PlantUML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PlantUML Files", "*.puml"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                String content = enhancedExporter.exportToPlantUML(graphVisualizer.getCurrentGraph());
                Files.write(file.toPath(), content.getBytes());
                showInfoDialog("Export Successful", "Enhanced PlantUML exported",
                        "File saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export enhanced PlantUML", e.getMessage());
            }
        }
    }

    private void exportEnhancedGraphviz() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Enhanced Graphviz");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Graphviz Files", "*.dot"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                String content = enhancedExporter.exportToGraphviz(graphVisualizer.getCurrentGraph());
                Files.write(file.toPath(), content.getBytes());
                showInfoDialog("Export Successful", "Enhanced Graphviz exported",
                        "File saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorDialog("Export Error", "Failed to export enhanced Graphviz", e.getMessage());
            }
        }
    }

    @FXML
    private void handleLayerFilter() {
        String selectedLayer = layerFilterComboBox.getValue();
        if (!"All Layers".equals(selectedLayer)) {
            graphVisualizer.filterByLayer(selectedLayer);
        } else {
            graphVisualizer.clearFilters();
        }
        refreshGraph();
    }

}