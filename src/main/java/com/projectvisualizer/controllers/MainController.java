package com.projectvisualizer.controllers;

import com.projectvisualizer.models.CodeComponent;
import com.projectvisualizer.models.ComponentRelationship;
import com.projectvisualizer.models.ProjectAnalysisResult;
import com.projectvisualizer.services.ProjectAnalyzer;
import com.projectvisualizer.visualization.GraphVisualizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.StatusBar;

import java.io.File;

public class MainController {

    @FXML private TreeView<String> projectTreeView;
    @FXML private TabPane visualizationTabPane;
    @FXML private ProgressBar analysisProgressBar;
    @FXML private Label statusLabel;
    @FXML private StatusBar statusBar;
    @FXML private VBox mainContainer;

    private ProjectAnalyzer projectAnalyzer;
    private GraphVisualizer graphVisualizer;

    @FXML
    public void initialize() {
        projectAnalyzer = new ProjectAnalyzer();
        graphVisualizer = new GraphVisualizer();
        setupProjectTree();
        setupStatusBar();
    }

    @FXML
    private void handleOpenProject() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");
        File selectedDirectory = directoryChooser.showDialog(projectTreeView.getScene().getWindow());

        if (selectedDirectory != null) {
            analyzeProject(selectedDirectory);
        }
    }

    @FXML
    private void handleExportDiagram() {
        // Implementation for exporting diagrams
        showInfoDialog("Export", "Export functionality", "Export feature will be implemented in future versions.");
    }

    @FXML
    private void handleSettings() {
        // Implementation for settings dialog
        showInfoDialog("Settings", "Settings functionality", "Settings feature will be implemented in future versions.");
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    private void analyzeProject(File projectDir) {
        statusLabel.setText("Analyzing project...");
        analysisProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        new Thread(() -> {
            try {
                ProjectAnalysisResult result = projectAnalyzer.analyze(projectDir,
                        (progress, message) -> Platform.runLater(() -> {
                            analysisProgressBar.setProgress(progress);
                            statusBar.setText(message);
                        }));

                Platform.runLater(() -> {
                    updateProjectTree(result);
                    createVisualizationTabs(result);
                    statusLabel.setText("Analysis complete");
                    analysisProgressBar.setProgress(1.0);
                    statusBar.setText("Ready");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Analysis failed: " + e.getMessage());
                    analysisProgressBar.setProgress(0);
                    statusBar.setText("Analysis failed");
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
        statusBar.setText("Ready");
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

    private void createVisualizationTabs(ProjectAnalysisResult result) {
        visualizationTabPane.getTabs().clear();

        // Architecture Diagram Tab
        Tab diagramTab = new Tab("Architecture Diagram");
        diagramTab.setContent(graphVisualizer.createGraphView(result));
        diagramTab.setClosable(false);

        // Components Tab
        Tab componentsTab = new Tab("Components");
        componentsTab.setContent(createComponentsView(result));
        componentsTab.setClosable(false);

        // Dependencies Tab
        Tab dependenciesTab = new Tab("Dependencies");
        dependenciesTab.setContent(createDependenciesView(result));
        dependenciesTab.setClosable(false);

        visualizationTabPane.getTabs().addAll(diagramTab, componentsTab, dependenciesTab);
    }

    private ScrollPane createComponentsView(ProjectAnalysisResult result) {
        ListView<String> componentsList = new ListView<>();
        result.getComponents().forEach(component ->
                componentsList.getItems().add(component.getName() + " - " + component.getType()));

        return new ScrollPane(componentsList);
    }

    private ScrollPane createDependenciesView(ProjectAnalysisResult result) {
        VBox dependenciesBox = new VBox(10);
        dependenciesBox.setPadding(new Insets(10));

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

            dependenciesBox.getChildren().addAll(gradleLabel, gradleBox);
        }

        // Flutter Dependencies
        if (!result.getFlutterDependencies().isEmpty()) {
            Label flutterLabel = new Label("Flutter Dependencies:");
            flutterLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            VBox flutterBox = new VBox(5);
            result.getFlutterDependencies().forEach(dep -> {
                HBox depRow = new HBox(10);
                Label nameLabel = new Label(dep.getName());
                Label versionLabel = new Label(dep.getVersion());
                versionLabel.setTextFill(Color.GRAY);
                depRow.getChildren().addAll(nameLabel, versionLabel);
                flutterBox.getChildren().add(depRow);
            });

            dependenciesBox.getChildren().addAll(flutterLabel, flutterBox);
        }

        // JavaScript Dependencies
        if (!result.getJsDependencies().isEmpty()) {
            Label jsLabel = new Label("JavaScript Dependencies:");
            jsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            VBox jsBox = new VBox(5);
            result.getJsDependencies().forEach(dep -> {
                HBox depRow = new HBox(10);
                Label nameLabel = new Label(dep.getName());
                Label versionLabel = new Label(dep.getVersion());
                versionLabel.setTextFill(Color.GRAY);
                depRow.getChildren().addAll(nameLabel, versionLabel);
                jsBox.getChildren().add(depRow);
            });

            dependenciesBox.getChildren().addAll(jsLabel, jsBox);
        }

        // Component Dependencies
        Label compLabel = new Label("Component Relationships:");
        compLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        dependenciesBox.getChildren().add(compLabel);

        VBox relBox = new VBox(5);
        for (ComponentRelationship rel : result.getRelationships()) {
            HBox relRow = new HBox(10);
            Label sourceLabel = new Label(findComponentName(result, rel.getSourceId()));
            Label arrowLabel = new Label("→");
            arrowLabel.setTextFill(Color.GRAY);
            Label targetLabel = new Label(findComponentName(result, rel.getTargetId()));
            Label typeLabel = new Label("(" + rel.getType() + ")");
            typeLabel.setTextFill(Color.DARKGRAY);

            relRow.getChildren().addAll(sourceLabel, arrowLabel, targetLabel, typeLabel);
            relBox.getChildren().add(relRow);
        }

        dependenciesBox.getChildren().add(relBox);

        ScrollPane scrollPane = new ScrollPane(dependenciesBox);
        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private String findComponentName(ProjectAnalysisResult result, String id) {
        for (CodeComponent comp : result.getComponents()) {
            if (comp.getId().equals(id)) {
                return comp.getName();
            }
        }
        return id; // Return the ID if component not found
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