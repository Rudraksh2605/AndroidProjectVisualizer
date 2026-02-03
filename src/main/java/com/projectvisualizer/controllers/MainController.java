package com.projectvisualizer.controllers;

import com.projectvisualizer.model.CodeComponent;
import com.projectvisualizer.model.CodeMethod;
import com.projectvisualizer.model.ComplexityInfo;
import com.projectvisualizer.model.AnalysisResult;
import com.projectvisualizer.services.ProjectAnalysisService;
import com.projectvisualizer.visualization.GraphManager;
import com.projectvisualizer.visualization.GraphNode;
import javafx.concurrent.Task;
import java.util.concurrent.CompletableFuture;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
import java.io.FileOutputStream;
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
import com.projectvisualizer.visualization.ActivityDiagramGenerator;
import javax.imageio.ImageIO;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.element.Text;

public class MainController implements Initializable {

    // Static block to ensure PlantUML property is set before any PlantUML classes are loaded
    static {
        // Increase PlantUML rendering limit to prevent clipping of large diagrams
        // 81920 is a very safe upper limit (approx 8x default 4096) to accommodate huge diagrams
        System.setProperty("PLANTUML_LIMIT_SIZE", "81920");
        // Also set Graphviz dot path if needed/custom, but usually auto-detected
    }

    @FXML
    private VBox mainContainer;
    @FXML
    private TreeView<String> projectTreeView;
    @FXML
    private ScrollPane diagramScrollPane;
    @FXML
    private TabPane visualizationTabPane;
    @FXML
    private ListView<String> componentsListView;
    @FXML
    private TextArea plantUMLTextArea;
    @FXML
    private TextArea graphvizTextArea;
    @FXML
    private VBox statisticsContainer;

    @FXML
    private Tab plantUMLImageTab;
    @FXML
    private Tab graphvizImageTab;
    @FXML
    private Tab documentationTab;
    @FXML
    private Tab projectDocsTab;
    @FXML
    private ImageView plantUMLImageView;
    @FXML
    private ImageView graphvizImageView;
    @FXML
    private ScrollPane plantUMLScrollPane;
    @FXML
    private ScrollPane graphvizScrollPane;
    @FXML
    private StackPane plantUMLImageContainer;
    @FXML
    private StackPane graphvizImageContainer;
    @FXML
    private VBox documentationContainer;
    @FXML
    private VBox projectDocsContainer;
    @FXML
    private Label projectDocsStatusLabel;

    // Navigation Flow tab elements
    @FXML
    private Tab navigationFlowTab;
    @FXML
    private ImageView navFlowImageView;
    @FXML
    private ScrollPane navFlowScrollPane;
    @FXML
    private StackPane navFlowImageContainer;
    @FXML
    private ComboBox<String> navFlowProcessFilter;

    @FXML
    private Label zoomLabel;

    @FXML
    private Label statusLabel;
    @FXML
    private Label projectInfoLabel;
    @FXML
    private Label memoryLabel;
    @FXML
    private Label statusZoomLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private HBox progressContainer;

    @FXML
    private MenuButton viewModeMenuButton;
    @FXML
    private Label categoryStatsLabel;

    private GraphManager graphManager;
    private Pane graphCanvas;
    private Map<String, CodeComponent> componentMap;
    private double currentZoom = 1.0;
    private double plantUmlZoom = 1.0;
    private double graphvizZoom = 1.0;
    private String diagramViewMode = "ALL"; // used to filter diagram outputs
    private AnalysisResult currentAnalysisResult;
    private String currentProjectName;
    private String currentProjectPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Increase PlantUML rendering limit to prevent clipping of large diagrams
        // PlantUML limit is now set in static block to ensure it applies globally and early

        initializeGraphCanvas();
        initializeTreeView();
        initializeEventHandlers();
        initializeStatusBar();
        initializeViewModeMenu();
        initializeDocumentation();
        loadSampleData();
        // initializeAI(); // MOVED: Auto-load disabled to prevent startup freeze.
        // Triggered manually or lazily.

        // Delay AI loading to ensure UI is responsive first
        javafx.application.Platform.runLater(() -> {
            // Further delay to let other UI components settle
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2 second delay
                    javafx.application.Platform.runLater(this::initializeAI);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }).start();
        });
    }

    /**
     * Initializes and auto-loads the Phi-2 AI model on startup.
     */
    private void initializeAI() {
        com.projectvisualizer.ai.ProjectUnderstandingService aiService = com.projectvisualizer.ai.ProjectUnderstandingService
                .getInstance();

        if (aiService.isModelAvailable() && !aiService.isReady() && !aiService.isLoading()) {
            statusLabel.setText("🤖 Loading Phi-2 AI model...");

            aiService.initializeAsync(progress -> {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(progress);
                });
            }).thenAccept(success -> {
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("🤖 Phi-2 AI model loaded successfully");
                    } else {
                        statusLabel.setText("AI model not loaded (download from Hugging Face)");
                    }
                });
            });
        } else if (!aiService.isModelAvailable()) {
            statusLabel.setText("AI model not found. Download Phi-2 GGML to enable AI features.");
        }
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
                        Math.max(2000, viewportHeight));
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
                new TreeItem<>("UserRepository.java"));
        kotlinGroup.getChildren().addAll(
                new TreeItem<>("LoginFragment.kt"),
                new TreeItem<>("UserViewModel.kt"));
        xmlGroup.getChildren().addAll(
                new TreeItem<>("activity_main.xml"),
                new TreeItem<>("fragment_login.xml"));

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
                });

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

    private void initializeDocumentation() {
        if (documentationContainer == null)
            return;
        documentationContainer.getChildren().clear();
        documentationContainer.setSpacing(24);

        // ===== HERO SECTION =====
        VBox hero = new VBox(12);
        hero.getStyleClass().add("doc-hero");

        HBox heroHeader = new HBox(16);
        heroHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label heroTitle = new Label("\uD83D\uDCF1 Android Project Visualizer");
        heroTitle.getStyleClass().add("doc-hero-title");
        Label versionBadge = new Label("v1.0.0");
        versionBadge.getStyleClass().add("doc-hero-version");
        heroHeader.getChildren().addAll(heroTitle, versionBadge);

        Label heroSubtitle = new Label(
                "A powerful tool for visualizing and understanding Android project architectures. " +
                        "Analyze code structure, generate UML diagrams, detect patterns, and gain insights into your codebase.");
        heroSubtitle.getStyleClass().add("doc-hero-subtitle");
        heroSubtitle.setWrapText(true);

        hero.getChildren().addAll(heroHeader, heroSubtitle);
        documentationContainer.getChildren().add(hero);

        // ===== GETTING STARTED SECTION =====
        VBox gettingStarted = createDocSection("\uD83D\uDE80 Getting Started",
                "Open any Android project folder to begin analysis. The tool will automatically parse Java, Kotlin, and XML files, "
                        +
                        "extract component relationships, and generate interactive architecture visualizations.");

        VBox stepsList = new VBox(4);
        stepsList.getChildren().addAll(
                createDocListItem("1. Go to File → Open Project or press Ctrl+O"),
                createDocListItem("2. Select your Android project's root directory"),
                createDocListItem("3. Wait for analysis to complete (progress shown in status bar)"),
                createDocListItem("4. Explore the generated diagrams and statistics"));
        gettingStarted.getChildren().add(stepsList);
        documentationContainer.getChildren().add(gettingStarted);

        // ===== SCREENS & TABS SECTION =====
        VBox screensSection = createDocSection("\uD83D\uDCCB Screens & Tabs",
                "The application provides multiple views for different aspects of your project analysis:");

        VBox tabCards = new VBox(12);
        tabCards.getChildren().addAll(
                createDocCard("\uD83C\uDFDB\uFE0F Architecture",
                        "Interactive graph visualization of your project's component structure. Click components in the tree to add them to the canvas. Use zoom controls to navigate large diagrams."),
                createDocCard("\uD83D\uDD17 Dependencies",
                        "Detailed view of component dependencies and relationships. Identify coupling issues and understand how components interconnect."),
                createDocCard("\uD83D\uDCCA Statistics",
                        "Project metrics including total files, lines of code, component counts by type, and code complexity analysis per method."),
                createDocCard("\uD83C\uDF31 PlantUML",
                        "Generated PlantUML diagram code. Copy or save to use in other tools. Supports class diagrams and use case diagrams."),
                createDocCard("\uD83D\uDD37 Graphviz",
                        "Generated Graphviz DOT notation for alternative diagram rendering. Compatible with GraphViz tools."),
                createDocCard("\uD83D\uDDBC\uFE0F PlantUML Preview",
                        "Rendered PlantUML diagram as an image. Zoom in/out and export as PNG for documentation."),
                createDocCard("\uD83D\uDDBC\uFE0F Graphviz Preview",
                        "Rendered Graphviz diagram as an image with export capabilities."));
        screensSection.getChildren().add(tabCards);
        documentationContainer.getChildren().add(screensSection);

        // ===== KEYBOARD SHORTCUTS =====
        VBox shortcutsSection = createDocSection("⌨\uFE0F Keyboard Shortcuts",
                "Quick access to common actions:");

        javafx.scene.layout.GridPane shortcutsGrid = new javafx.scene.layout.GridPane();
        shortcutsGrid.setHgap(24);
        shortcutsGrid.setVgap(12);
        shortcutsGrid.add(createKbdLabel("Ctrl+O"), 0, 0);
        shortcutsGrid.add(new Label("Open Project"), 1, 0);
        shortcutsGrid.add(createKbdLabel("Ctrl+R"), 0, 1);
        shortcutsGrid.add(new Label("Export Report"), 1, 1);
        shortcutsGrid.add(createKbdLabel("Ctrl++"), 0, 2);
        shortcutsGrid.add(new Label("Zoom In"), 1, 2);
        shortcutsGrid.add(createKbdLabel("Ctrl+-"), 0, 3);
        shortcutsGrid.add(new Label("Zoom Out"), 1, 3);
        shortcutsGrid.add(createKbdLabel("Ctrl+0"), 0, 4);
        shortcutsGrid.add(new Label("Reset Zoom"), 1, 4);
        shortcutsGrid.add(createKbdLabel("F1"), 0, 5);
        shortcutsGrid.add(new Label("Documentation"), 1, 5);
        shortcutsSection.getChildren().add(shortcutsGrid);
        documentationContainer.getChildren().add(shortcutsSection);

        // ===== AI FEATURES SECTION =====
        VBox aiSection = createDocSection("\uD83E\uDD16 AI-Enhanced Analysis",
                "Optional AI-powered features using Microsoft Phi-2 for deeper code understanding:");

        VBox aiFeatures = new VBox(8);
        aiFeatures.getChildren().addAll(
                createDocListItem("• Intelligent use case extraction from method names and patterns"),
                createDocListItem("• Natural language descriptions of component functionality"),
                createDocListItem("• Automatic actor detection for use case diagrams"),
                createDocListItem("• GPU-accelerated inference for fast analysis"));

        VBox aiNote = new VBox(8);
        aiNote.setStyle("-fx-background-color: rgba(251, 191, 36, 0.1); -fx-background-radius: 10; -fx-padding: 16;");
        Label aiNoteLabel = new Label(
                "⚠\uFE0F AI features require downloading the Phi-2 model (~1.6GB). See AI_SETUP.md for instructions.");
        aiNoteLabel.setWrapText(true);
        aiNoteLabel.getStyleClass().add("doc-text");
        aiNote.getChildren().add(aiNoteLabel);

        aiSection.getChildren().addAll(aiFeatures, aiNote);
        documentationContainer.getChildren().add(aiSection);

        // ===== TECH STACK SECTION =====
        VBox techSection = createDocSection("\uD83D\uDEE0\uFE0F Technical Details",
                "Built with modern technologies for robust performance:");

        javafx.scene.layout.GridPane techGrid = new javafx.scene.layout.GridPane();
        techGrid.setHgap(16);
        techGrid.setVgap(12);
        techGrid.getStyleClass().add("doc-tech-grid");
        techGrid.add(createTechItem("Language", "Java 17"), 0, 0);
        techGrid.add(createTechItem("UI Framework", "JavaFX 17"), 1, 0);
        techGrid.add(createTechItem("Build Tool", "Gradle (Kotlin DSL)"), 2, 0);
        techGrid.add(createTechItem("Java Parsing", "JavaParser 3.x"), 0, 1);
        techGrid.add(createTechItem("Kotlin Parsing", "Kotlin Compiler"), 1, 1);
        techGrid.add(createTechItem("Diagrams", "PlantUML + Graphviz"), 2, 1);
        techGrid.add(createTechItem("AI Engine", "llama.java (Phi-2)"), 0, 2);
        techGrid.add(createTechItem("UI Library", "ControlsFX"), 1, 2);
        techSection.getChildren().add(techGrid);
        documentationContainer.getChildren().add(techSection);

        // ===== PARSERS SECTION =====
        VBox parsersSection = createDocSection("\uD83D\uDD0D Code Parsers",
                "12 specialized parsers for comprehensive Android project analysis:");

        VBox parsersList = new VBox(4);
        parsersList.getChildren().addAll(
                createDocListItem("• JavaFileParser - Parses Java classes, interfaces, methods, and annotations"),
                createDocListItem("• KotlinParser - Full Kotlin support including data classes and extensions"),
                createDocListItem("• XmlParser - Android resource files (layouts, strings, styles)"),
                createDocListItem("• AndroidManifestParser - Activities, services, permissions extraction"),
                createDocListItem("• NavigationGraphParser - Jetpack Navigation component support"),
                createDocListItem("• IntentAnalyzer - Intent-based navigation detection"),
                createDocListItem("• ComplexityAnalyzer - Cyclomatic complexity metrics"));
        parsersSection.getChildren().add(parsersList);
        documentationContainer.getChildren().add(parsersSection);
    }

    private VBox createDocSection(String title, String description) {
        VBox section = new VBox(12);
        section.getStyleClass().add("doc-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("doc-section-header");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("doc-text");
        descLabel.setWrapText(true);

        section.getChildren().addAll(titleLabel, descLabel);
        return section;
    }

    private VBox createDocCard(String title, String description) {
        VBox card = new VBox(8);
        card.getStyleClass().add("doc-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("doc-card-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("doc-card-desc");
        descLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, descLabel);
        return card;
    }

    private HBox createDocListItem(String text) {
        HBox item = new HBox(8);
        item.getStyleClass().add("doc-list-item");
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("doc-list-text");
        textLabel.setWrapText(true);

        item.getChildren().add(textLabel);
        return item;
    }

    private Label createKbdLabel(String shortcut) {
        Label kbd = new Label(shortcut);
        kbd.getStyleClass().add("doc-kbd");
        return kbd;
    }

    private VBox createTechItem(String label, String value) {
        VBox item = new VBox(4);
        item.getStyleClass().add("doc-tech-item");

        Label labelNode = new Label(label.toUpperCase());
        labelNode.getStyleClass().add("doc-tech-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("doc-tech-value");

        item.getChildren().addAll(labelNode, valueNode);
        return item;
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
            currentProjectName = selectedDirectory.getName();
            currentProjectPath = selectedDirectory.getAbsolutePath();
            statusLabel.setText("Loading project: " + currentProjectName);
            projectInfoLabel.setText("Project: " + currentProjectName);

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
                new FileChooser.ExtensionFilter("SVG Files", "*.svg"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                String fileName = file.getName().toLowerCase();
                String extension = "";
                if (fileName.endsWith(".png")) extension = "png";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) extension = "jpg";
                else if (fileName.endsWith(".svg")) extension = "svg";

                // Determine source based on active tab
                Tab selectedTab = visualizationTabPane != null
                        ? visualizationTabPane.getSelectionModel().getSelectedItem()
                        : null;

                if (extension.equals("svg")) {
                    handleSvgExport(file, selectedTab);
                } else {
                    handleImageExport(file, selectedTab, extension);
                }
            } catch (Exception ex) {
                statusLabel.setText("Export failed: " + ex.getMessage());
                showErrorAlert("Export Failed", "Could not save image: " + ex.getMessage());
            }
        }
    }

    private void handleSvgExport(File file, Tab selectedTab) throws IOException {
        if (selectedTab == plantUMLImageTab) {
            if (plantUMLTextArea == null || plantUMLTextArea.getText().isEmpty()) {
                throw new IOException("No PlantUML source available to export.");
            }
            String puml = plantUMLTextArea.getText();
            if (!puml.contains("@startuml")) {
                puml = "@startuml\n" + puml + "\n@enduml";
            }
            SourceStringReader reader = new SourceStringReader(puml);
            try (FileOutputStream os = new FileOutputStream(file)) {
                reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
            }
            statusLabel.setText("Exported PlantUML SVG to: " + file.getName());

        } else if (selectedTab == graphvizImageTab) {
            if (graphvizTextArea == null || graphvizTextArea.getText().isEmpty()) {
                throw new IOException("No Graphviz source available to export.");
            }
            String dot = graphvizTextArea.getText();
            String low = dot.toLowerCase();
            if (!low.contains("digraph") && !low.contains("graph") && (dot.contains("->") || dot.contains("--"))) {
                dot = "digraph G {\n" + dot + "\n}";
            }
            Graphviz.fromString(dot).render(Format.SVG).toFile(file);
            statusLabel.setText("Exported Graphviz SVG to: " + file.getName());

        } else {
            // Fallback for canvas (not supported) or other tabs
            showErrorAlert("Export Format Warning",
                    "SVG export is only supported for PlantUML and Graphviz diagrams. " +
                            "Please choose PNG or JPEG for this view.");
        }
    }

    private void handleImageExport(File file, Tab selectedTab, String format) throws IOException {
        Image imageToSave = null;

        if (selectedTab == plantUMLImageTab && plantUMLImageView != null) {
            imageToSave = plantUMLImageView.getImage();
        } else if (selectedTab == graphvizImageTab && graphvizImageView != null) {
            imageToSave = graphvizImageView.getImage();
        } else {
            // Default to capturing the graph canvas
            if (graphCanvas != null) {
                WritableImage snapshot = graphCanvas.snapshot(new SnapshotParameters(), null);
                imageToSave = snapshot;
            }
        }

        if (imageToSave != null) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imageToSave, null);

            if (format.equals("jpg")) {
                // Convert transparent background to white for JPG
                BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                        bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, java.awt.Color.WHITE, null);
                bufferedImage = newBufferedImage;
            }

            ImageIO.write(bufferedImage, format, file);
            statusLabel.setText("Exported diagram to: " + file.getName());
        } else {
            statusLabel.setText("Nothing to export (Image is null)");
            throw new IOException("No image data found to export.");
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
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try {
                if (currentAnalysisResult == null) {
                    statusLabel.setText("No analysis data to export");
                    return;
                }

                // Generate documentation content
                com.projectvisualizer.ai.ProjectUnderstandingService aiService = com.projectvisualizer.ai.ProjectUnderstandingService
                        .getInstance();

                com.projectvisualizer.services.StructuredDocumentationGenerator generator = new com.projectvisualizer.services.StructuredDocumentationGenerator(
                        currentProjectPath,
                        currentProjectName,
                        currentAnalysisResult,
                        aiService.isReady() ? aiService.getInferenceService() : null);

                Map<String, String> docSections = generator.generateDocumentation();
                StringBuilder reportContent = new StringBuilder();

                boolean isHtml = file.getName().toLowerCase().endsWith(".html");
                boolean isJson = file.getName().toLowerCase().endsWith(".json");
                boolean isPdf = file.getName().toLowerCase().endsWith(".pdf");

                if (isPdf) {
                    exportToPdf(file, docSections, currentProjectName);
                } else if (isJson) {
                    // Simple JSON construction
                    reportContent.append("{\n");
                    reportContent.append("  \"project\": \"").append(currentProjectName).append("\",\n");
                    reportContent.append("  \"sections\": {\n");
                    int i = 0;
                    for (Map.Entry<String, String> entry : docSections.entrySet()) {
                        reportContent.append("    \"").append(entry.getKey()).append("\": \"")
                                .append(entry.getValue().replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
                        if (i < docSections.size() - 1)
                            reportContent.append(",");
                        reportContent.append("\n");
                        i++;
                    }
                    reportContent.append("  }\n");
                    reportContent.append("}");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write(reportContent.toString());
                    }
                } else if (isHtml) {
                    reportContent.append("<!DOCTYPE html>\n<html>\n<head>\n");
                    reportContent.append("<title>Project Documentation - ").append(currentProjectName)
                            .append("</title>\n");
                    reportContent.append("<style>\n");
                    reportContent.append(
                            "body { font-family: sans-serif; line-height: 1.6; max-width: 800px; margin: 0 auto; padding: 20px; color: #333; }\n");
                    reportContent
                            .append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }\n");
                    reportContent.append("h2 { color: #34495e; margin-top: 30px; }\n");
                    reportContent.append("h3 { color: #16a085; }\n");
                    reportContent.append(
                            "pre { background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }\n");
                    reportContent.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }\n");
                    reportContent.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
                    reportContent.append("th { background-color: #f2f2f2; }\n");
                    reportContent.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
                    reportContent.append("</style>\n</head>\n<body>\n");
                    reportContent.append("<h1>").append(currentProjectName).append(" Documentation</h1>\n");

                    for (Map.Entry<String, String> entry : docSections.entrySet()) {
                        reportContent.append("<h2>").append(entry.getKey()).append("</h2>\n");
                        // Basic Markdown-to-HTML conversion
                        String content = entry.getValue()
                                .replace("\n", "<br/>\n")
                                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
                                .replaceAll("`(.*?)`", "<code>$1</code>")
                                .replaceAll("### (.*?)<br/>", "<h3>$1</h3>")
                                .replaceAll("\\| (.*?) \\|", "<tr><td>$1</td></tr>") // Very naive table
                                .replaceAll("<tr><td>- (.*?)</td></tr>", "<li>$1</li>"); // List items

                        // Fix tables (naive fix)
                        if (content.contains("<tr>")) {
                            content = content.replace("<br/>", ""); // Remove breaks in tables
                            content = "<table>" + content + "</table>";
                        }

                        reportContent.append("<div>").append(content).append("</div>\n");
                    }
                    reportContent.append("</body>\n</html>");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write(reportContent.toString());
                    }
                } else {
                    // Default Markdown/Text
                    reportContent.append("# Project Documentation: ").append(currentProjectName).append("\n\n");
                    for (Map.Entry<String, String> entry : docSections.entrySet()) {
                        reportContent.append("## ").append(entry.getKey()).append("\n\n");
                        reportContent.append(entry.getValue()).append("\n\n");
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write(reportContent.toString());
                    }
                }

                statusLabel.setText("Exported report to: " + file.getName());
            } catch (IOException ex) {
                statusLabel.setText("Export failed: " + ex.getMessage());
                showErrorAlert("Export Failed", "Could not save report: " + ex.getMessage());
            }
        }
    }

    private void exportToPdf(File file, Map<String, String> sections, String projectName) throws IOException {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        Paragraph title = new Paragraph(projectName + " Documentation")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(24)
                .setBold();
        document.add(title);

        // Sections
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            // Header
            document.add(new Paragraph(entry.getKey())
                    .setFontSize(18)
                    .setBold()
                    .setMarginTop(20));

            // Content (Simple rendering)
            String content = entry.getValue();
            Paragraph p = new Paragraph(content)
                    .setFontSize(12);

            // If content looks like a table or code, use monospace
            if (content.contains("|") || content.contains("```")) {
                try {
                    p.setFont(PdfFontFactory.createFont(StandardFonts.COURIER));
                } catch (Exception e) {
                    // Ignore font error
                }
            }

            document.add(p);
        }

        document.close();
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
    private void handleShowDocumentation() {
        if (visualizationTabPane != null && documentationTab != null) {
            visualizationTabPane.getSelectionModel().select(documentationTab);
            statusLabel.setText("Viewing documentation");
        }
    }

    @FXML
    private void handleShowAbout() {
        Alert aboutDialog = new Alert(Alert.AlertType.INFORMATION);
        aboutDialog.setTitle("About Android Project Visualizer");
        aboutDialog.setHeaderText("Android Project Architecture Visualizer");
        aboutDialog.setContentText(
                "Version: 1.0.0\n\n" +
                        "A powerful tool for visualizing and understanding\n" +
                        "Android project architectures.\n\n" +
                        "Features:\n" +
                        "• Interactive architecture diagrams\n" +
                        "• PlantUML & Graphviz export\n" +
                        "• AI-enhanced use case detection\n" +
                        "• Code complexity analysis\n\n" +
                        "Built with JavaFX 17");
        aboutDialog.showAndWait();
    }

    @FXML
    private void handleViewProjectDocs() {
        if (visualizationTabPane != null && projectDocsTab != null) {
            visualizationTabPane.getSelectionModel().select(projectDocsTab);
            statusLabel.setText("Viewing project documentation");
        }
    }

    private void populateProjectDocumentation(AnalysisResult result, String projectName) {
        if (projectDocsContainer == null)
            return;

        projectDocsContainer.getChildren().clear();

        if (result == null || result.getComponents() == null || result.getComponents().isEmpty()) {
            if (projectDocsStatusLabel != null) {
                projectDocsStatusLabel.setText("No project loaded");
            }
            return;
        }

        if (projectDocsStatusLabel != null) {
            projectDocsStatusLabel.setText("🤖 Generating documentation in background...");
        }
        statusLabel.setText("Generating documentation...");

        try {
            // Get AI service from ProjectUnderstandingService singleton
            com.projectvisualizer.ai.ProjectUnderstandingService aiService = com.projectvisualizer.ai.ProjectUnderstandingService
                    .getInstance();

            com.projectvisualizer.ai.Phi2InferenceService phi2Service = aiService.isReady()
                    ? aiService.getInferenceService()
                    : null;

            com.projectvisualizer.services.ProjectDocumentationGenerator generator = new com.projectvisualizer.services.ProjectDocumentationGenerator(
                    result, projectName, currentProjectPath, phi2Service);

            if (aiService.isReady()) {
                // generateDocumentationWithAI now handles its own async work internally
                generator.generateDocumentationWithAI(projectDocsContainer, status -> {
                    if (projectDocsStatusLabel != null) {
                        projectDocsStatusLabel.setText(status);
                    }
                    statusLabel.setText(status);
                });
            } else {
                // Fallback: Run non-AI generation in background
                CompletableFuture.runAsync(() -> {
                    java.util.List<javafx.scene.layout.VBox> sections = generator.generateDocumentation();

                    javafx.application.Platform.runLater(() -> {
                        projectDocsContainer.getChildren().addAll(sections);

                        String aiStatus = aiService.isModelAvailable() ? " (AI model available but not loaded)"
                                : " (AI model not available)";
                        if (projectDocsStatusLabel != null) {
                            projectDocsStatusLabel.setText("Documentation generated for " + projectName + aiStatus);
                        }
                        statusLabel.setText("Generated project documentation" + aiStatus);
                    });
                });
            }
        } catch (Exception e) {
            statusLabel.setText("Error generating documentation: " + e.getMessage());
            if (projectDocsStatusLabel != null) {
                projectDocsStatusLabel.setText("Error generating documentation");
            }
        }
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
        if (plantUMLTextArea == null || plantUMLImageView == null)
            return;
        String src = plantUMLTextArea.getText();
        if (src == null || src.trim().isEmpty()) {
            statusLabel.setText("No PlantUML source to render");
            return;
        }

        statusLabel.setText("Wait... Rendering PlantUML...");

        CompletableFuture.supplyAsync(() -> {
            try {
                // Ensure PlantUML has start/end wrappers
                String puml = src.trim();
                if (!puml.contains("@startuml")) {
                    puml = "@startuml\n" + puml + "\n@enduml";
                }

                // Add scale directive for higher resolution if not present
                if (!puml.contains("scale ")) {
                    // Start with scale 2 to make the base diagram 2x larger (higher resolution)
                    puml = puml.replace("@startuml", "@startuml\nscale 2");
                }

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                SourceStringReader reader = new SourceStringReader(puml);
                // Heavy operation happening here - render at higher quality
                reader.generateImage(os, new FileFormatOption(FileFormat.PNG));
                return os.toByteArray();
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(bytes -> {
            javafx.application.Platform.runLater(() -> {
                if (bytes == null || bytes.length == 0) {
                    statusLabel.setText("PlantUML rendering failed or empty");
                    return;
                }

                try {
                    Image fxImage = new Image(new ByteArrayInputStream(bytes));
                    plantUMLImageView.setImage(fxImage);

                    // Reset zoom to 100% and apply - let user scroll to see full diagram
                    plantUmlZoom = 1.0;
                    applyPlantUmlZoom();

                    statusLabel.setText("Rendered PlantUML diagram (" + (int) fxImage.getWidth() + "x"
                            + (int) fxImage.getHeight() + "px) - scroll to view full diagram");

                    if (showOnCanvas && graphCanvas != null) {
                        // Clear canvas and show image, fit to canvas width (responsive)
                        graphCanvas.getChildren().clear();
                        ImageView iv = new ImageView(fxImage);
                        iv.setPreserveRatio(true);
                        iv.fitWidthProperty().bind(graphCanvas.widthProperty());
                        graphCanvas.getChildren().add(iv);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error displaying PlantUML: " + e.getMessage());
                }
            });
        });
    }

    @FXML
    private void handleExportToPlantUML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PlantUML Diagram");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PlantUML Files", "*.puml"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(plantUMLTextArea.getText());
                statusLabel.setText("Exported PlantUML to: " + file.getName());
            } catch (IOException ex) {
                statusLabel.setText("Export failed: " + ex.getMessage());
                showErrorAlert("Export Failed", "Could not save PlantUML file: " + ex.getMessage());
            }
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
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(graphvizTextArea.getText());
                statusLabel.setText("Exported Graphviz to: " + file.getName());
            } catch (IOException ex) {
                statusLabel.setText("Export failed: " + ex.getMessage());
                showErrorAlert("Export Failed", "Could not save Graphviz file: " + ex.getMessage());
            }
        }
    }

    private void renderGraphviz(boolean showOnCanvas) {
        if (graphvizTextArea == null || graphvizImageView == null)
            return;
        String dot = graphvizTextArea.getText();
        if (dot == null || dot.trim().isEmpty()) {
            statusLabel.setText("No Graphviz (DOT) source to render");
            return;
        }

        statusLabel.setText("Wait... Rendering Graphviz...");

        CompletableFuture.supplyAsync(() -> {
            try {
                String src = dot;
                String low = dot.toLowerCase();
                if (!low.contains("digraph") && !low.contains("graph") && (dot.contains("->") || dot.contains("--"))) {
                    src = "digraph G {\n" + dot + "\n}";
                }
                return Graphviz.fromString(src).render(Format.PNG).toImage();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAccept(bi -> {
            javafx.application.Platform.runLater(() -> {
                if (bi == null) {
                    statusLabel.setText("Graphviz rendering failed");
                    return;
                }
                try {
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
                    statusLabel.setText("Error displaying Graphviz: " + e.getMessage());
                }
            });
        });
    }

    @FXML
    private void handleZoomInPlantUML() {
        plantUmlZoom = Math.min(plantUmlZoom * 1.25, 5.0);
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom: " + (int) (plantUmlZoom * 100) + "%");
    }

    @FXML
    private void handleZoomOutPlantUML() {
        plantUmlZoom = Math.max(plantUmlZoom / 1.25, 0.2);
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom: " + (int) (plantUmlZoom * 100) + "%");
    }

    @FXML
    private void handleResetZoomPlantUML() {
        plantUmlZoom = 1.0;
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML zoom reset");
    }

    private void applyPlantUmlZoom() {
        if (plantUMLImageView == null)
            return;
        Image img = plantUMLImageView.getImage();
        if (img == null)
            return;
        double baseW = img.getWidth();
        double baseH = img.getHeight();
        if (baseW <= 0 || baseH <= 0)
            return;
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
        statusLabel.setText("Graphviz zoom: " + (int) (graphvizZoom * 100) + "%");
    }

    @FXML
    private void handleZoomOutGraphviz() {
        graphvizZoom = Math.max(graphvizZoom / 1.25, 0.2);
        applyGraphvizZoom();
        statusLabel.setText("Graphviz zoom: " + (int) (graphvizZoom * 100) + "%");
    }

    @FXML
    private void handleResetZoomGraphviz() {
        graphvizZoom = 1.0;
        applyGraphvizZoom();
        statusLabel.setText("Graphviz zoom reset");
    }

    private void applyGraphvizZoom() {
        if (graphvizImageView == null)
            return;
        Image img = graphvizImageView.getImage();
        if (img == null)
            return;
        double baseW = img.getWidth();
        double baseH = img.getHeight();
        if (baseW <= 0 || baseH <= 0)
            return;
        graphvizImageView.setFitWidth(baseW * graphvizZoom);
        graphvizImageView.setFitHeight(baseH * graphvizZoom);
    }

    @FXML
    private void handleExportGraphvizImage() {
        handleExportEnhancedDiagram();
    }

    @FXML
    private void handleFitPlantUMLToView() {
        if (plantUMLImageView == null || plantUMLScrollPane == null)
            return;
        Image img = plantUMLImageView.getImage();
        if (img == null)
            return;

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0)
            return;

        // Get the visible viewport size
        double viewportWidth = plantUMLScrollPane.getViewportBounds().getWidth();
        double viewportHeight = plantUMLScrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewportWidth = plantUMLScrollPane.getWidth() - 20;
            viewportHeight = plantUMLScrollPane.getHeight() - 20;
        }

        // Calculate scale to fit whole image in viewport
        double scaleX = viewportWidth / imgWidth;
        double scaleY = viewportHeight / imgHeight;
        double fitScale = Math.min(scaleX, scaleY);

        // Clamp the scale to reasonable bounds
        fitScale = Math.max(0.1, Math.min(fitScale, 2.0));

        plantUmlZoom = fitScale;
        applyPlantUmlZoom();
        statusLabel.setText("PlantUML fitted to view: " + (int) (plantUmlZoom * 100) + "%");
    }

    @FXML
    private void handleFitGraphvizToView() {
        if (graphvizImageView == null || graphvizScrollPane == null)
            return;
        Image img = graphvizImageView.getImage();
        if (img == null)
            return;

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0)
            return;

        // Get the visible viewport size
        double viewportWidth = graphvizScrollPane.getViewportBounds().getWidth();
        double viewportHeight = graphvizScrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewportWidth = graphvizScrollPane.getWidth() - 20;
            viewportHeight = graphvizScrollPane.getHeight() - 20;
        }

        // Calculate scale to fit whole image in viewport
        double scaleX = viewportWidth / imgWidth;
        double scaleY = viewportHeight / imgHeight;
        double fitScale = Math.min(scaleX, scaleY);

        // Clamp the scale to reasonable bounds
        fitScale = Math.max(0.1, Math.min(fitScale, 2.0));

        graphvizZoom = fitScale;
        applyGraphvizZoom();
        statusLabel.setText("Graphviz fitted to view: " + (int) (graphvizZoom * 100) + "%");
    }

    // Navigation Flow tab zoom state
    private double navFlowZoom = 1.0;

    @FXML
    private void handleZoomInNavFlow() {
        navFlowZoom = Math.min(navFlowZoom * 1.25, 5.0);
        applyNavFlowZoom();
        statusLabel.setText("Navigation Flow zoom: " + (int) (navFlowZoom * 100) + "%");
    }

    @FXML
    private void handleZoomOutNavFlow() {
        navFlowZoom = Math.max(navFlowZoom / 1.25, 0.2);
        applyNavFlowZoom();
        statusLabel.setText("Navigation Flow zoom: " + (int) (navFlowZoom * 100) + "%");
    }

    @FXML
    private void handleResetZoomNavFlow() {
        navFlowZoom = 1.0;
        applyNavFlowZoom();
        statusLabel.setText("Navigation Flow zoom reset");
    }

    @FXML
    private void handleFitNavFlowToView() {
        if (navFlowImageView == null || navFlowScrollPane == null)
            return;
        Image img = navFlowImageView.getImage();
        if (img == null)
            return;

        double imgWidth = img.getWidth();
        double imgHeight = img.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0)
            return;

        double viewportWidth = navFlowScrollPane.getViewportBounds().getWidth();
        double viewportHeight = navFlowScrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            viewportWidth = navFlowScrollPane.getWidth() - 20;
            viewportHeight = navFlowScrollPane.getHeight() - 20;
        }

        double scaleX = viewportWidth / imgWidth;
        double scaleY = viewportHeight / imgHeight;
        double fitScale = Math.min(scaleX, scaleY);
        fitScale = Math.max(0.1, Math.min(fitScale, 2.0));

        navFlowZoom = fitScale;
        applyNavFlowZoom();
        statusLabel.setText("Navigation Flow fitted to view: " + (int) (navFlowZoom * 100) + "%");
    }

    private void applyNavFlowZoom() {
        if (navFlowImageView == null)
            return;
        Image img = navFlowImageView.getImage();
        if (img == null)
            return;
        double baseW = img.getWidth();
        double baseH = img.getHeight();
        if (baseW <= 0 || baseH <= 0)
            return;
        navFlowImageView.setFitWidth(baseW * navFlowZoom);
        navFlowImageView.setFitHeight(baseH * navFlowZoom);
    }

    @FXML
    private void handleExportNavFlowImage() {
        handleExportEnhancedDiagram();
    }

    private void handleComponentSelection(TreeItem<String> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null)
            return;

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

                    if (fileName.contains(compName)
                            || compName.contains(fileName.replace(".java", "").replace(".kt", ""))) {
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
            statusLabel.setText("Added " + component.getName() + " (" + component.getType() + " - "
                    + component.getLayer() + ") to graph");

            // Auto-scroll to the new node after a short delay
            autoScrollToNode(component.getId());
        } else {
            System.out.println("Component not found for: " + itemValue);
            statusLabel.setText("Component not found: " + itemValue);

        }
    }

    private void autoScrollToNode(String componentId) {
        // Use PauseTransition instead of Thread.sleep to avoid blocking UI
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(event -> {
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
        });
        pause.play();
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
            if (component == null)
                continue;
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
        if (allComponents == null || allComponents.isEmpty())
            return;

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
                        }));

        // Build simple-name fallback map
        Map<String, CodeComponent> bySimpleName = new HashMap<>();
        for (CodeComponent c : allComponents) {
            if (c == null)
                continue;
            String id = c.getId();
            String simple = null;
            if (id != null && id.contains(".")) {
                simple = id.substring(id.lastIndexOf('.') + 1);
            } else if (c.getName() != null) {
                simple = c.getName();
            }
            if (simple != null)
                bySimpleName.putIfAbsent(simple, c);
        }

        for (CodeComponent component : allComponents) {
            if (component == null)
                continue;
            List<CodeComponent> originalDeps = component.getDependencies();
            if (originalDeps == null || originalDeps.isEmpty()) {
                component.setDependencies(Collections.emptyList());
                continue;
            }

            List<CodeComponent> resolvedDependencies = new ArrayList<>();

            for (CodeComponent dependency : originalDeps) {
                if (dependency == null)
                    continue;
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
        if (component.getName() == null)
            return;

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
                navigationItems, useCaseItems, new SeparatorMenuItem(), relationshipsItems);
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
            case "ALL":
                return "All Components";
            case "UI":
                return "UI Components";
            case "DATA_MODEL":
                return "Data Models";
            case "BUSINESS_LOGIC":
                return "Business Logic";
            case "NAVIGATION":
                return "Navigation/Intents";
            case "USE_CASE":
                return "Use Case Diagram";
            default:
                return "All Components";
        }
    }

    private void updateCategoryStats() {
        if (graphManager != null) {
            Map<String, Integer> stats = graphManager.getCategoryStats();
            StringBuilder statsText = new StringBuilder("Categories: ");
            stats.forEach((category, count) -> statsText.append(category).append(": ").append(count).append("  "));
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
        statusLabel.setText("Processing analysis results...");

        // Perform heavy processing in background
        CompletableFuture.runAsync(() -> {
            // Thread-safe map update
            synchronized (componentMap) {
                componentMap.clear();
                for (CodeComponent component : result.getComponents()) {
                    if (componentMap.containsKey(component.getId())) {
                        String uniqueId = component.getId() + "_" + System.currentTimeMillis();
                        component.setId(uniqueId);
                    }
                    componentMap.put(component.getId(), component);
                }
            }

            try {
                resolveDependencies(result.getComponents());
            } catch (Exception e) {
                System.err.println("Error resolving dependencies: " + e.getMessage());
            }

            // Categorize components (CPU-bound)
            graphManager.categorizeComponents(result.getComponents());
        }).thenRun(() -> {
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Updating UI...");
                updateCategoryStats();
                setViewMode("ALL");
                updateComponentList();
                updateProjectTreeWithRealData(result.getComponents());

                // Update diagrams (already async internally)
                updateDiagrams(result);

                // Populate complexity statistics in Statistics tab
                populateStatisticsTab(result.getComponents());

                // Generate project documentation
                populateProjectDocumentation(result, currentProjectName);

                handleResetZoom();
                diagramScrollPane.setVvalue(0);
                diagramScrollPane.setHvalue(0);
                statusLabel.setText("Project analysis complete");
            });
        }).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Analysis processing failed: " + ex.getMessage());
            });
            return null;
        });
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
            if (component.getMethods() == null)
                continue;
            for (CodeMethod method : component.getMethods()) {
                totalMethods++;
                ComplexityInfo info = method.getComplexityInfo();
                if (info != null) {
                    int severity = info.getSeverityLevel();
                    if (severity == 3)
                        highComplexityMethods++;
                    else if (severity == 2)
                        mediumComplexityMethods++;
                    else
                        lowComplexityMethods++;
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
                createStatCard("🔴 High (O(n²)+)", String.valueOf(highComplexityMethods), "#ef4444"));
        statisticsContainer.getChildren().add(summaryBox);

        // Separator
        statisticsContainer.getChildren().add(new javafx.scene.control.Separator());

        // Per-component cards
        Label detailLabel = new Label("📄 Per-File Complexity Breakdown");
        detailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #555; -fx-padding: 10 0 5 0;");
        statisticsContainer.getChildren().add(detailLabel);

        for (CodeComponent component : components) {
            if (component.getMethods() == null || component.getMethods().isEmpty())
                continue;

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
                    // Enable AI for enhanced use case analysis
                    UseCaseDiagramGenerator useCaseGenerator = new UseCaseDiagramGenerator(true);

                    statusLabel.setText(useCaseGenerator.getAIStatusMessage());
                    statusLabel.setText("Wait... Generating AI Use Case Diagram...");

                    CompletableFuture.supplyAsync(() -> {
                        return useCaseGenerator.generatePlantUMLWithAI(
                                result.getComponents(),
                                result.getBusinessProcesses());
                    }).thenAccept(useCasePuml -> {
                        javafx.application.Platform.runLater(() -> {
                            plantUMLTextArea.setText(useCasePuml);
                            renderPlantUml(false);
                            statusLabel.setText("Use Case diagram generated with AI analysis");
                        });
                    }).exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText("AI Generation Failed: " + ex.getMessage());
                        });
                        return null;
                    });
                }
                // Clear Graphviz area since Use Case is PlantUML-only
                if (graphvizTextArea != null) {
                    graphvizTextArea.setText(
                            "// Use Case diagrams are rendered in PlantUML only.\n// Switch to another view mode to see Graphviz diagrams.");
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

            // Generate Navigation Flow Diagram
            generateNavigationFlowDiagram(result);
        } catch (Exception e) {
            statusLabel.setText("Failed to generate diagrams: " + e.getMessage());
        }
    }

    /**
     * Generates and renders the Navigation Flow (Activity) diagram.
     */
    private void generateNavigationFlowDiagram(AnalysisResult result) {
        if (navFlowImageView == null) {
            System.err.println("Navigation Flow ImageView not bound");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            ActivityDiagramGenerator generator = new ActivityDiagramGenerator();
            return generator.generatePlantUML(result.getComponents(), result.getNavigationFlows());
        }).thenAccept(plantUmlCode -> {
            javafx.application.Platform.runLater(() -> {
                renderNavigationFlowDiagram(plantUmlCode);
            });
        }).exceptionally(ex -> {
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText("Navigation Flow diagram failed: " + ex.getMessage());
            });
            return null;
        });
    }

    /**
     * Renders a PlantUML Activity Diagram to the Navigation Flow tab's ImageView.
     */
    private void renderNavigationFlowDiagram(String plantUmlCode) {
        if (plantUmlCode == null || plantUmlCode.isEmpty()) {
            statusLabel.setText("No navigation flow data to render");
            return;
        }

        try {
            SourceStringReader reader = new SourceStringReader(plantUmlCode);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            reader.outputImage(baos, new FileFormatOption(FileFormat.PNG));
            baos.close();

            byte[] imageData = baos.toByteArray();
            if (imageData.length > 0) {
                Image image = new Image(new ByteArrayInputStream(imageData));
                navFlowImageView.setImage(image);
                navFlowImageView.setPreserveRatio(true);
                navFlowZoom = 1.0;
                applyNavFlowZoom();
                statusLabel.setText("Navigation Flow diagram rendered");
            } else {
                statusLabel.setText("Navigation Flow diagram is empty");
            }
        } catch (Exception e) {
            statusLabel.setText("Navigation Flow render error: " + e.getMessage());
            e.printStackTrace();
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
        sb.append("scale max 16384 width\n");
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
            if (diagramViewMode == null || "ALL".equalsIgnoreCase(diagramViewMode))
                return true;
            String cat = detectComponentCategoryForDiagrams(c);
            switch (diagramViewMode) {
                case "UI":
                    return "UI".equals(cat);
                case "DATA_MODEL":
                    return "DataModel".equals(cat);
                case "BUSINESS_LOGIC":
                    return "BusinessLogic".equals(cat);
                case "NAVIGATION":
                    return "Navigation".equals(cat);
                default:
                    return true;
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
                if (!includedIds.contains(fromId))
                    continue; // should be present anyway
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
        if (category == null)
            return "#cccccc";
        switch (category) {
            case "UI":
                return "#93c5fd";
            case "DataModel":
                return "#fdba74";
            case "BusinessLogic":
                return "#86efac";
            case "Navigation":
                return "#f87171";
            default:
                return "#cccccc";
        }
    }

    private String getCategoryFillColor(String category) {
        if (category == null)
            return "#f2f2f2";
        switch (category) {
            case "UI":
                return "#e0f2fe";
            case "DataModel":
                return "#fff4e5";
            case "BusinessLogic":
                return "#eaffea";
            case "Navigation":
                return "#ffe0e0";
            default:
                return "#f2f2f2";
        }
    }

    private String sanitizeId(String id) {
        if (id == null)
            return "id_" + UUID.randomUUID().toString().replace('-', '_');
        return id.replaceAll("[^A-Za-z0-9_]", "_");
    }

    // In MainController.java - Replace detectComponentCategoryForDiagrams method
    // with:
    private String detectComponentCategoryForDiagrams(CodeComponent component) {
        String category = ComponentCategorizer.detectCategory(component);
        // Convert to diagram format if needed
        if ("DATA_MODEL".equals(category))
            return "DataModel";
        if ("BUSINESS_LOGIC".equals(category))
            return "BusinessLogic";
        if ("NAVIGATION".equals(category))
            return "Navigation";
        if ("UNKNOWN".equals(category))
            return "Unknown";
        return category;
    }
}