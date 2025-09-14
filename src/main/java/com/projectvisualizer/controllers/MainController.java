package com.projectvisualizer.controllers;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import com.projectvisualizer.services.ProjectAnalyzer;
import javafx.embed.swing.SwingFXUtils;
import com.projectvisualizer.visualization.GraphVisualizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
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

import javax.imageio.ImageIO;
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


    private ProjectAnalysisResult currentAnalysisResult;
    private ProjectAnalyzer projectAnalyzer;
    private GraphVisualizer graphVisualizer;
    private double currentZoomLevel = 1.0;

    private GraphVisualizer.LayoutType currentLayoutType = GraphVisualizer.LayoutType.HIERARCHICAL;
    private boolean showLabels = true;
    private boolean showGrid = false;

    @FXML
    public void initialize() {
        projectAnalyzer = new ProjectAnalyzer();
        graphVisualizer = new GraphVisualizer();
        setupProjectTree();
        setupStatusBar();
        setupComboBoxes();
        setupCheckBoxes();

        visualizationModeComboBox.getItems().addAll(
                "Technical Architecture", "User Journey", "Business Process",
                "Feature Overview", "Integration Map"
        );
        visualizationModeComboBox.setValue("Technical Architecture"); // Default to technical view

        visualizationModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVisualizationMode(newVal);
        });

        // Set up memory monitoring
        setupMemoryMonitoring();
    }

    private void updateVisualizationMode(String mode) {
        GraphVisualizer.VisualizationMode vizMode = mapStringToVisualizationMode(mode);
        graphVisualizer.setVisualizationMode(vizMode);
        refreshGraph();
    }


    private void setupComboBoxes() {
        // Setup layer filter
        layerFilterComboBox.getItems().addAll("All Layers", "UI", "Business Logic", "Data", "Other");
        layerFilterComboBox.setValue("All Layers");

        // Setup layout combo
        layoutComboBox.getItems().addAll("Hierarchical", "Force-Directed", "Circular", "Grid", "Layered");
        layoutComboBox.setValue("Hierarchical");

        // Add listener for layout changes
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
    }

    private void refreshGraph() {
        if (currentAnalysisResult != null) {
            // Update graph visualizer settings
            graphVisualizer.setLayoutType(currentLayoutType);
            graphVisualizer.setShowLabels(showLabels);
            graphVisualizer.setShowGrid(showGrid);

            // Refresh the diagram
            ScrollPane diagram = graphVisualizer.createGraphView(currentAnalysisResult);
            // Avoid nesting scroll panes; use the diagram content (shared canvas)
            diagramScrollPane.setContent(diagram.getContent());

            // Place legend into fixed overlay so it doesn't scroll
            if (legendOverlayPane != null) {
                legendOverlayPane.getChildren().clear();
                javafx.scene.layout.Region legend = graphVisualizer.getLegend();
                if (legend != null) {
                    legendOverlayPane.getChildren().add(legend);
                }
            }

            // Auto-fit by default after content is laid out
            javafx.application.Platform.runLater(() -> {
                graphVisualizer.fitToWindow(diagramScrollPane);
                currentZoomLevel = graphVisualizer.getCurrentZoom();
                updateZoom();
            });
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
    private void handleLayerFilter() {
        String selectedLayer = layerFilterComboBox.getValue();
        filterByLayer(selectedLayer);
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

        // Update statistics view
        updateStatisticsView(result);

        // Update metrics view
        updateMetricsView(result);

        // Update diagram
        ScrollPane diagram = graphVisualizer.createGraphView(result);
        // Avoid nesting scroll panes; use the diagram content (shared canvas)
        diagramScrollPane.setContent(diagram.getContent());
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
                    diMap.computeIfAbsent(key, k -> new java.util.ArrayList<>());
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
            graphVisualizer.exportToPlantUML(file);
            showInfoDialog("Export Successful", "PlantUML export complete",
                    "The diagram has been exported to PlantUML format: " + file.getAbsolutePath());
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
            graphVisualizer.exportToGraphviz(file);
            showInfoDialog("Export Successful", "Graphviz export complete",
                    "The diagram has been exported to Graphviz format: " + file.getAbsolutePath());
        }
    }
}