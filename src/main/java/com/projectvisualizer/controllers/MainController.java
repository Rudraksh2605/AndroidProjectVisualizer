package com.projectvisualizer.controllers;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import com.projectvisualizer.services.ProjectAnalyzer;
import javafx.embed.swing.SwingFXUtils;  // Added missing import
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
    @FXML private VBox componentDetailsContainer;
    @FXML private ScrollPane diagramScrollPane;
    @FXML private ScrollPane minimapScrollPane;

    private ProjectAnalysisResult currentAnalysisResult;
    private ProjectAnalyzer projectAnalyzer;
    private GraphVisualizer graphVisualizer;
    private double currentZoomLevel = 1.0;

    @FXML
    public void initialize() {
        projectAnalyzer = new ProjectAnalyzer();
        graphVisualizer = new GraphVisualizer();
        setupProjectTree();
        setupStatusBar();
        setupComboBoxes();
        setupCheckBoxes();

        // Set up memory monitoring
        setupMemoryMonitoring();
    }

    private void setupComboBoxes() {
        // Setup layer filter
        layerFilterComboBox.getItems().addAll("All Layers", "UI", "Business Logic", "Data", "Other");
        layerFilterComboBox.setValue("All Layers");

        // Setup layout combo
        layoutComboBox.getItems().addAll("Hierarchical", "Force-Directed", "Circular", "Grid", "Layered");
        layoutComboBox.setValue("Hierarchical");
    }

    private void setupCheckBoxes() {
        // Set up check box listeners
        showLabelsCheckBox.setSelected(true);
        showGridCheckBox.setSelected(false);
        showMinimapCheckBox.setSelected(false);
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
        try {
            // Create a snapshot of the diagram
            WritableImage image = diagramScrollPane.snapshot(null, null);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Diagram");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PNG", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
            );

            File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
            if (file != null) {
                String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), extension, file);
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
        currentZoomLevel *= 1.2;
        updateZoom();
    }

    @FXML
    private void handleZoomOut() {
        currentZoomLevel *= 0.8;
        updateZoom();
    }

    @FXML
    private void handleResetZoom() {
        currentZoomLevel = 1.0;
        updateZoom();
    }

    @FXML
    private void handleFitToWindow() {
        showInfoDialog("Fit to Window", "Fit to window functionality", "Fit to window feature will be implemented in future versions.");
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
    private void handleHierarchicalLayout() {
        showInfoDialog("Hierarchical Layout", "Hierarchical layout functionality", "Hierarchical layout feature will be implemented in future versions.");
    }

    @FXML
    private void handleCircularLayout() {
        showInfoDialog("Circular Layout", "Circular layout functionality", "Circular layout feature will be implemented in future versions.");
    }

    @FXML
    private void handleForceDirectedLayout() {
        showInfoDialog("Force-Directed Layout", "Force-directed layout functionality", "Force-directed layout feature will be implemented in future versions.");
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
        diagramScrollPane.setContent(diagram);
    }

    private void updateDependenciesView(ProjectAnalysisResult result) {
        dependenciesContainer.getChildren().clear();

        // Gradle Dependencies
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

        // Add other dependency types similarly...
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
}