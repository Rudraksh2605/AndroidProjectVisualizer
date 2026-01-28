package com.projectvisualizer.services;

import com.projectvisualizer.model.*;
import com.projectvisualizer.ai.Phi2InferenceService;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Generates dynamic documentation from analyzed Android project data.
 * Supports AI-enhanced documentation using Microsoft Phi-2 model.
 * Now reads ACTUAL source code for better AI understanding.
 */
public class ProjectDocumentationGenerator {

    private final AnalysisResult analysisResult;
    private final String projectName;
    private final String projectPath;
    private final Phi2InferenceService aiService;
    private final SourceCodeReader sourceCodeReader;
    private String aiGeneratedSummary;
    private String aiGeneratedUseCases;
    private String aiGeneratedArchitecture;

    public ProjectDocumentationGenerator(AnalysisResult analysisResult, String projectName) {
        this(analysisResult, projectName, null, null);
    }

    public ProjectDocumentationGenerator(AnalysisResult analysisResult, String projectName, Phi2InferenceService aiService) {
        this(analysisResult, projectName, null, aiService);
    }

    public ProjectDocumentationGenerator(AnalysisResult analysisResult, String projectName, String projectPath, Phi2InferenceService aiService) {
        this.analysisResult = analysisResult;
        this.projectName = projectName != null ? projectName : "Android Project";
        this.projectPath = projectPath;
        this.aiService = aiService;
        this.sourceCodeReader = projectPath != null ? new SourceCodeReader(projectPath) : null;
    }

    /**
     * Checks if AI is available for documentation generation.
     */
    public boolean isAIAvailable() {
        return aiService != null && aiService.isReady();
    }

    /**
     * Generates all documentation sections and returns them as VBox children.
     */
    public List<VBox> generateDocumentation() {
        List<VBox> sections = new ArrayList<>();

        sections.add(createHeroSection());
        
        // Add AI summary section if available
        if (isAIAvailable()) {
            sections.add(createAISummarySection());
        }
        
        sections.add(createAppOverviewSection());
        sections.add(createScreensSection());
        sections.add(createUseCasesSection());
        sections.add(createNavigationSection());
        sections.add(createArchitectureSection());
        sections.add(createTechSummarySection());

        return sections;
    }

    /**
     * Generates documentation with AI-enhanced content asynchronously.
     * Uses documentation-specific prompts for comprehensive AI analysis.
     * @param container The VBox container to populate with documentation
     * @param statusCallback Optional callback for status updates
     */
    /**
     * Generates documentation with STRUCTURED content from manifest/gradle data.
     * Uses StructuredDocumentationGenerator for professional formatted output.
     * AI is used only to enhance descriptions where available.
     * @param container The VBox container to populate with documentation
     * @param statusCallback Optional callback for status updates
     */
    public void generateDocumentationWithAI(VBox container, java.util.function.Consumer<String> statusCallback) {
        if (statusCallback != null) {
            statusCallback.accept("📊 Generating structured documentation...");
        }

        // Create structured documentation generator
        StructuredDocumentationGenerator structuredGen = new StructuredDocumentationGenerator(
            projectPath, projectName, analysisResult, aiService);
        
        // Generate structured documentation sections
        Map<String, String> sections = structuredGen.generateDocumentation();
        
        // Add Hero section first
        container.getChildren().add(createHeroSection());
        
        // Add each structured section as a formatted VBox
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            VBox sectionBox = createStructuredSection(entry.getKey(), entry.getValue());
            container.getChildren().add(sectionBox);
        }
        
        // If AI is available, add AI enhancement section at the end
        if (isAIAvailable()) {
            if (statusCallback != null) {
                statusCallback.accept("🤖 AI: Generating additional insights...");
            }
            
            // Add AI loading placeholder
            VBox aiLoadingSection = createAILoadingSection();
            container.getChildren().add(aiLoadingSection);
            
            String codeContext = buildRichCodeContext();
            
            CompletableFuture.runAsync(() -> {
                try {
                    aiGeneratedSummary = aiService.analyzeCode(codeContext, "doc_overview");
                    
                    Platform.runLater(() -> {
                        container.getChildren().remove(aiLoadingSection);
                        container.getChildren().add(createPureAIOverviewSection());
                        
                        if (statusCallback != null) {
                            statusCallback.accept("✨ Professional documentation generated");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        container.getChildren().remove(aiLoadingSection);
                        if (statusCallback != null) {
                            statusCallback.accept("📊 Documentation generated (AI enhancement unavailable)");
                        }
                    });
                }
            });
        } else {
            if (statusCallback != null) {
                statusCallback.accept("📊 Professional documentation generated");
            }
        }
    }
    
    /**
     * Creates a structured section with markdown-like rendering.
     */
    private VBox createStructuredSection(String title, String content) {
        VBox section = new VBox(8);
        section.getStyleClass().add("doc-section");
        section.setStyle("-fx-padding: 16; -fx-background-color: rgba(30, 30, 46, 0.6); -fx-background-radius: 8;");
        
        // Section header
        Label header = new Label("📄 " + title);
        header.getStyleClass().add("doc-section-header");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");
        
        // Content - parse markdown-style content into Labels
        VBox contentBox = new VBox(6);
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            Label lineLabel = createFormattedLabel(line);
            if (lineLabel != null) {
                contentBox.getChildren().add(lineLabel);
            }
        }
        
        section.getChildren().addAll(header, contentBox);
        return section;
    }
    
    /**
     * Creates a formatted label from markdown-style text.
     */
    private Label createFormattedLabel(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #bac2de;");
        
        // Handle different markdown elements
        if (line.startsWith("### ")) {
            label.setText(line.substring(4));
            label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #a6adc8; -fx-padding: 8 0 4 0;");
        } else if (line.startsWith("| ")) {
            // Table row - render as styled label
            label.setText(line.replace("|", "  ").trim());
            label.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-text-fill: #89b4fa;");
        } else if (line.startsWith("- ")) {
            label.setText("  • " + line.substring(2));
            label.setStyle("-fx-text-fill: #94e2d5;");
        } else if (line.startsWith("*") && line.endsWith("*")) {
            label.setText(line.replace("*", ""));
            label.setStyle("-fx-font-style: italic; -fx-text-fill: #9399b2;");
        } else if (line.startsWith("```")) {
            return null; // Skip code block markers
        } else {
            label.setText(line);
        }
        
        return label;
    }


    /**
     * Creates PURE AI overview section without pattern-based content.
     */
    private VBox createPureAIOverviewSection() {
        VBox section = createSection("📋 Project Overview",
                "AI-generated understanding of this Android application.");

        VBox card = new VBox(12);
        card.getStyleClass().add("doc-card");

        Label badge = new Label("🤖 Phi-2 AI Analysis");
        badge.getStyleClass().add("doc-badge");
        badge.setStyle("-fx-background-color: linear-gradient(135deg, #8b5cf6, #6366f1); -fx-text-fill: white; -fx-padding: 6 12;");

        String content = aiGeneratedSummary != null ? aiGeneratedSummary : 
                "Unable to generate overview. Please ensure AI model is loaded.";

        Label text = new Label(content);
        text.getStyleClass().add("doc-text");
        text.setWrapText(true);
        text.setStyle("-fx-line-spacing: 4;");

        card.getChildren().addAll(badge, text);
        section.getChildren().add(card);

        return section;
    }

    /**
     * Creates PURE AI features section without pattern-based content.
     */
    private VBox createPureAIFeaturesSection() {
        VBox section = createSection("🎯 Features & Capabilities",
                "AI-detected features from code analysis.");

        VBox card = new VBox(12);
        card.getStyleClass().add("doc-card");

        Label badge = new Label("🤖 AI Feature Detection");
        badge.getStyleClass().add("doc-badge");
        badge.setStyle("-fx-background-color: linear-gradient(135deg, #10b981, #059669); -fx-text-fill: white; -fx-padding: 6 12;");

        String content = aiGeneratedUseCases != null ? aiGeneratedUseCases : 
                "Unable to detect features. Please ensure AI model is loaded.";

        Label text = new Label(content);
        text.getStyleClass().add("doc-text");
        text.setWrapText(true);
        text.setStyle("-fx-line-spacing: 4;");

        card.getChildren().addAll(badge, text);
        section.getChildren().add(card);

        return section;
    }

    /**
     * Creates PURE AI architecture section without pattern-based content.
     */
    private VBox createPureAIArchitectureSection() {
        VBox section = createSection("🏗️ Architecture & Design",
                "AI-analyzed architecture patterns and structure.");

        VBox card = new VBox(12);
        card.getStyleClass().add("doc-card");

        Label badge = new Label("🤖 AI Architecture Analysis");
        badge.getStyleClass().add("doc-badge");
        badge.setStyle("-fx-background-color: linear-gradient(135deg, #f59e0b, #d97706); -fx-text-fill: white; -fx-padding: 6 12;");

        String content = aiGeneratedArchitecture != null ? aiGeneratedArchitecture : 
                "Unable to analyze architecture. Please ensure AI model is loaded.";

        Label text = new Label(content);
        text.getStyleClass().add("doc-text");
        text.setWrapText(true);
        text.setStyle("-fx-line-spacing: 4;");

        card.getChildren().addAll(badge, text);
        section.getChildren().add(card);

        return section;
    }

    /**
     * Builds a RICH code context string for AI analysis.
     * Builds RICH code context by reading ACTUAL source files.
     * Uses SourceCodeReader to get real code, not just metadata.
     */
    private String buildRichCodeContext() {
        List<CodeComponent> components = getComponents();
        
        // If we have SourceCodeReader, use it to get real source code
        if (sourceCodeReader != null) {
            return sourceCodeReader.buildFullCodeContext(components, projectName);
        }
        
        // Fallback: build context from metadata only (less useful for AI)
        StringBuilder context = new StringBuilder();
        context.append("=== PROJECT: ").append(projectName).append(" ===\n\n");
        context.append("NOTE: Reading from metadata only (project path not provided)\n\n");
        
        // Group components by type
        Map<String, List<CodeComponent>> byType = new LinkedHashMap<>();
        for (CodeComponent comp : components) {
            String type = comp.getType() != null ? comp.getType() : "OTHER";
            byType.computeIfAbsent(type, k -> new ArrayList<>()).add(comp);
        }

        // Describe each component type
        for (Map.Entry<String, List<CodeComponent>> entry : byType.entrySet()) {
            context.append("--- ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(") ---\n");
            for (CodeComponent comp : entry.getValue()) {
                context.append("\n[").append(comp.getName()).append("]");
                if (comp.getLanguage() != null) {
                    context.append(" (").append(comp.getLanguage()).append(")");
                }
                context.append("\n");
                
                if (comp.getFilePath() != null) {
                    context.append("File: ").append(comp.getFilePath()).append("\n");
                }
                
                if (comp.getAnnotations() != null && !comp.getAnnotations().isEmpty()) {
                    context.append("Annotations: ").append(String.join(", ", comp.getAnnotations())).append("\n");
                }
                
                List<CodeMethod> methods = comp.getMethods();
                if (methods != null && !methods.isEmpty()) {
                    context.append("Methods: ");
                    for (int i = 0; i < Math.min(methods.size(), 10); i++) {
                        context.append(methods.get(i).getName()).append("()");
                        if (i < Math.min(methods.size(), 10) - 1) context.append(", ");
                    }
                    if (methods.size() > 10) context.append("... (").append(methods.size() - 10).append(" more)");
                    context.append("\n");
                }
            }
            context.append("\n");
        }

        // Add navigation flows
        List<NavigationFlow> flows = analysisResult.getNavigationFlows();
        if (flows != null && !flows.isEmpty()) {
            context.append("\n--- NAVIGATION FLOWS ---\n");
            for (NavigationFlow flow : flows) {
                context.append(flow.getSourceScreenId()).append(" -> ").append(flow.getTargetScreenId()).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * Creates a loading indicator section for AI generation.
     */
    private VBox createAILoadingSection() {
        VBox section = new VBox(16);
        section.getStyleClass().add("doc-section");
        section.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(40, 40);

        Label loadingLabel = new Label("🤖 Phi-2 AI is analyzing your project...");
        loadingLabel.getStyleClass().add("doc-section-header");

        Label hintLabel = new Label("Generating intelligent documentation using Microsoft Phi-2 LLM");
        hintLabel.getStyleClass().add("doc-text");

        section.getChildren().addAll(progress, loadingLabel, hintLabel);
        return section;
    }

    /**
     * Creates an AI-generated summary section.
     */
    private VBox createAISummarySection() {
        VBox section = createSection("🤖 AI-Generated Summary",
                "Intelligent analysis by Microsoft Phi-2 LLM.");

        String summary = aiGeneratedSummary != null ? aiGeneratedSummary : 
                "AI summary not available. Load Phi-2 model to enable intelligent documentation.";

        VBox summaryCard = new VBox(8);
        summaryCard.getStyleClass().add("doc-card");

        Label badge = new Label("✨ AI Analysis");
        badge.getStyleClass().add("doc-badge");
        badge.setStyle("-fx-background-color: linear-gradient(135deg, #8b5cf6, #6366f1); -fx-text-fill: white;");

        Label summaryText = new Label(summary);
        summaryText.getStyleClass().add("doc-text");
        summaryText.setWrapText(true);

        summaryCard.getChildren().addAll(badge, summaryText);
        section.getChildren().add(summaryCard);

        return section;
    }

    /**
     * Creates an AI-enhanced use cases section.
     */
    private VBox createAIUseCasesSection() {
        VBox section = createSection("🎯 AI-Detected Use Cases",
                "Use cases extracted by AI analysis of code patterns and functionality.");

        if (aiGeneratedUseCases != null && !aiGeneratedUseCases.isEmpty()) {
            VBox useCaseCard = new VBox(8);
            useCaseCard.getStyleClass().add("doc-card");

            Label badge = new Label("🤖 Phi-2 Analysis");
            badge.getStyleClass().add("doc-badge");
            badge.setStyle("-fx-background-color: linear-gradient(135deg, #10b981, #059669); -fx-text-fill: white;");

            // Parse and display use cases
            String[] lines = aiGeneratedUseCases.split("\n");
            useCaseCard.getChildren().add(badge);
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    HBox item = createListItem(line.trim());
                    useCaseCard.getChildren().add(item);
                }
            }

            section.getChildren().add(useCaseCard);
        } else {
            section.getChildren().add(createNoDataLabel("AI use case analysis not available."));
        }

        // Also add pattern-based use cases
        VBox patternBased = createUseCasesSection();
        // Extract inner content from pattern-based section
        if (patternBased.getChildren().size() > 2) {
            Label separator = new Label("📋 Pattern-Based Detection");
            separator.getStyleClass().add("doc-section-header");
            separator.setStyle("-fx-font-size: 14px; -fx-padding: 16 0 8 0;");
            section.getChildren().add(separator);
            // Copy children after header and description
            for (int i = 2; i < patternBased.getChildren().size(); i++) {
                section.getChildren().add(patternBased.getChildren().get(i));
            }
        }

        return section;
    }

    /**
     * Creates an AI-enhanced architecture section.
     */
    private VBox createAIArchitectureSection() {
        VBox section = createSection("🏗️ AI Architecture Analysis",
                "Architecture patterns and component relationships detected by AI.");

        if (aiGeneratedArchitecture != null && !aiGeneratedArchitecture.isEmpty()) {
            VBox archCard = new VBox(8);
            archCard.getStyleClass().add("doc-card");

            Label badge = new Label("🤖 Phi-2 Analysis");
            badge.getStyleClass().add("doc-badge");
            badge.setStyle("-fx-background-color: linear-gradient(135deg, #f59e0b, #d97706); -fx-text-fill: white;");

            Label archText = new Label(aiGeneratedArchitecture);
            archText.getStyleClass().add("doc-text");
            archText.setWrapText(true);

            archCard.getChildren().addAll(badge, archText);
            section.getChildren().add(archCard);
        }

        // Add pattern-based architecture
        VBox patternArch = createArchitectureSection();
        if (patternArch.getChildren().size() > 2) {
            Label separator = new Label("📊 Component Distribution");
            separator.getStyleClass().add("doc-section-header");
            separator.setStyle("-fx-font-size: 14px; -fx-padding: 16 0 8 0;");
            section.getChildren().add(separator);
            for (int i = 2; i < patternArch.getChildren().size(); i++) {
                section.getChildren().add(patternArch.getChildren().get(i));
            }
        }

        return section;
    }

    /**
     * Builds a code context string for AI analysis.
     */
    private String buildCodeContext() {
        StringBuilder context = new StringBuilder();
        List<CodeComponent> components = getComponents();

        context.append("Project: ").append(projectName).append("\n\n");
        context.append("=== Components ("+ components.size() +") ===\n");

        // Add component summaries
        for (CodeComponent comp : components) {
            context.append("- ").append(comp.getName());
            if (comp.getType() != null) context.append(" (").append(comp.getType()).append(")");
            if (comp.getLayer() != null) context.append(" [Layer: ").append(comp.getLayer()).append("]");
            context.append("\n");

            // Add key methods
            List<CodeMethod> methods = comp.getMethods();
            if (methods != null && !methods.isEmpty()) {
                context.append("  Methods: ");
                for (int i = 0; i < Math.min(methods.size(), 5); i++) {
                    context.append(methods.get(i).getName());
                    if (i < Math.min(methods.size(), 5) - 1) context.append(", ");
                }
                context.append("\n");
            }
        }

        // Add navigation flows
        List<NavigationFlow> flows = analysisResult.getNavigationFlows();
        if (flows != null && !flows.isEmpty()) {
            context.append("\n=== Navigation Flows ===\n");
            for (NavigationFlow flow : flows) {
                context.append(flow.getSourceScreenId()).append(" -> ").append(flow.getTargetScreenId()).append("\n");
            }
        }

        // Add some code snippets
        context.append("\n=== Sample Code ===\n");
        int snippetCount = 0;
        for (CodeComponent comp : components) {
            if (comp.getCodeSnippet() != null && !comp.getCodeSnippet().isEmpty() && snippetCount < 3) {
                context.append("--- ").append(comp.getName()).append(" ---\n");
                String snippet = comp.getCodeSnippet();
                context.append(snippet.length() > 500 ? snippet.substring(0, 500) + "..." : snippet);
                context.append("\n\n");
                snippetCount++;
            }
        }

        return context.toString();
    }

    // ==================== HERO SECTION ====================
    private VBox createHeroSection() {
        VBox hero = new VBox(12);
        hero.getStyleClass().add("doc-hero");

        HBox heroHeader = new HBox(16);
        heroHeader.setAlignment(Pos.CENTER_LEFT);
        Label heroTitle = new Label("📋 " + projectName);
        heroTitle.getStyleClass().add("doc-hero-title");
        
        Label badge = new Label("Project Documentation");
        badge.getStyleClass().add("doc-hero-version");
        heroHeader.getChildren().addAll(heroTitle, badge);

        Label subtitle = new Label("Auto-generated documentation from analyzed project structure. " +
                "This document describes the application's screens, features, navigation, and architecture.");
        subtitle.getStyleClass().add("doc-hero-subtitle");
        subtitle.setWrapText(true);

        hero.getChildren().addAll(heroHeader, subtitle);
        return hero;
    }

    // ==================== APP OVERVIEW ====================
    private VBox createAppOverviewSection() {
        VBox section = createSection("📱 Application Overview",
                "High-level summary of the analyzed Android application.");

        List<CodeComponent> components = getComponents();
        Map<String, List<CodeComponent>> categorized = getCategorizedComponents();

        // Count components by type
        int activities = countByType(components, "Activity");
        int fragments = countByType(components, "Fragment");
        int viewModels = countByType(components, "ViewModel");
        int repositories = countByType(components, "Repository");
        int services = countByType(components, "Service");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(24);
        statsGrid.setVgap(12);
        statsGrid.getStyleClass().add("doc-tech-grid");

        statsGrid.add(createStatItem("Total Components", String.valueOf(components.size())), 0, 0);
        statsGrid.add(createStatItem("Activities", String.valueOf(activities)), 1, 0);
        statsGrid.add(createStatItem("Fragments", String.valueOf(fragments)), 2, 0);
        statsGrid.add(createStatItem("ViewModels", String.valueOf(viewModels)), 0, 1);
        statsGrid.add(createStatItem("Repositories", String.valueOf(repositories)), 1, 1);
        statsGrid.add(createStatItem("Services", String.valueOf(services)), 2, 1);

        section.getChildren().add(statsGrid);

        // Detect architecture pattern
        String pattern = detectArchitecturePattern(components);
        if (pattern != null) {
            Label patternLabel = new Label("🏗️ Detected Pattern: " + pattern);
            patternLabel.getStyleClass().add("doc-badge");
            section.getChildren().add(patternLabel);
        }

        return section;
    }

    // ==================== SCREENS SECTION ====================
    private VBox createScreensSection() {
        VBox section = createSection("🖥️ Screens & UI Components",
                "All Activities and Fragments that define the user interface.");

        List<CodeComponent> components = getComponents();
        List<CodeComponent> uiComponents = components.stream()
                .filter(c -> isUIComponent(c))
                .collect(Collectors.toList());

        if (uiComponents.isEmpty()) {
            section.getChildren().add(createNoDataLabel("No UI components detected."));
            return section;
        }

        VBox screenCards = new VBox(12);
        Map<String, String> layoutMap = analysisResult.getActivityLayoutMap();

        for (CodeComponent comp : uiComponents) {
            VBox card = createScreenCard(comp, layoutMap);
            screenCards.getChildren().add(card);
        }

        section.getChildren().add(screenCards);
        return section;
    }

    private VBox createScreenCard(CodeComponent comp, Map<String, String> layoutMap) {
        VBox card = new VBox(8);
        card.getStyleClass().add("doc-card");

        String icon = comp.getType().contains("Activity") ? "📱" : "🧩";
        Label title = new Label(icon + " " + comp.getName());
        title.getStyleClass().add("doc-card-title");

        // Description based on methods
        String description = inferScreenPurpose(comp);
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("doc-card-desc");
        descLabel.setWrapText(true);

        card.getChildren().addAll(title, descLabel);

        // Layout file if available
        String layout = layoutMap != null ? layoutMap.get(comp.getName()) : null;
        if (layout != null) {
            Label layoutLabel = new Label("📐 Layout: " + layout);
            layoutLabel.getStyleClass().add("doc-text");
            card.getChildren().add(layoutLabel);
        }

        // Key methods
        List<CodeMethod> methods = comp.getMethods();
        if (methods != null && !methods.isEmpty()) {
            List<String> keyMethods = extractKeyMethods(methods);
            if (!keyMethods.isEmpty()) {
                Label methodsLabel = new Label("⚙️ Key Actions: " + String.join(", ", keyMethods));
                methodsLabel.getStyleClass().add("doc-text");
                methodsLabel.setWrapText(true);
                card.getChildren().add(methodsLabel);
            }
        }

        return card;
    }

    // ==================== USE CASES SECTION ====================
    private VBox createUseCasesSection() {
        VBox section = createSection("🎯 Use Cases & Features",
                "Functionality extracted from business processes and method analysis.");

        List<BusinessProcessComponent> processes = analysisResult.getBusinessProcesses();
        List<CodeComponent> components = getComponents();

        VBox useCaseList = new VBox(8);

        // From business processes
        if (processes != null && !processes.isEmpty()) {
            for (BusinessProcessComponent process : processes) {
                VBox useCaseCard = createUseCaseCard(process);
                useCaseList.getChildren().add(useCaseCard);
            }
        }

        // Extract additional use cases from component methods
        Map<String, Set<String>> featuresByScreen = extractFeaturesFromComponents(components);
        for (Map.Entry<String, Set<String>> entry : featuresByScreen.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                VBox featureCard = new VBox(8);
                featureCard.getStyleClass().add("doc-card");
                
                Label screenLabel = new Label("📍 " + entry.getKey());
                screenLabel.getStyleClass().add("doc-card-title");
                featureCard.getChildren().add(screenLabel);

                for (String feature : entry.getValue()) {
                    HBox item = createListItem("• " + feature);
                    featureCard.getChildren().add(item);
                }
                useCaseList.getChildren().add(featureCard);
            }
        }

        if (useCaseList.getChildren().isEmpty()) {
            section.getChildren().add(createNoDataLabel("No use cases detected. Load a project to analyze."));
        } else {
            section.getChildren().add(useCaseList);
        }

        return section;
    }

    private VBox createUseCaseCard(BusinessProcessComponent process) {
        VBox card = new VBox(8);
        card.getStyleClass().add("doc-card");

        String icon = getProcessIcon(process.getProcessType());
        Label title = new Label(icon + " " + process.getProcessName());
        title.getStyleClass().add("doc-card-title");

        Label typeLabel = new Label("Type: " + process.getProcessType() + 
                " | Priority: " + process.getCriticalityLevel());
        typeLabel.getStyleClass().add("doc-text");

        card.getChildren().addAll(title, typeLabel);

        // Process steps
        List<ProcessStep> steps = process.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < Math.min(steps.size(), 5); i++) {
                ProcessStep step = steps.get(i);
                HBox stepItem = createListItem((i + 1) + ". " + step.getStepName());
                card.getChildren().add(stepItem);
            }
            if (steps.size() > 5) {
                Label moreLabel = new Label("   ... and " + (steps.size() - 5) + " more steps");
                moreLabel.getStyleClass().add("doc-text");
                card.getChildren().add(moreLabel);
            }
        }

        return card;
    }

    // ==================== NAVIGATION SECTION ====================
    private VBox createNavigationSection() {
        VBox section = createSection("🗺️ Navigation Map",
                "Screen-to-screen navigation flows detected in the application.");

        List<NavigationFlow> flows = analysisResult.getNavigationFlows();

        if (flows == null || flows.isEmpty()) {
            section.getChildren().add(createNoDataLabel("No navigation flows detected."));
            return section;
        }

        VBox flowList = new VBox(8);
        for (NavigationFlow flow : flows) {
            HBox flowItem = new HBox(8);
            flowItem.setAlignment(Pos.CENTER_LEFT);

            Label sourceLabel = new Label(flow.getSourceScreenId());
            sourceLabel.getStyleClass().add("doc-kbd");

            Label arrow = new Label("→");
            arrow.getStyleClass().add("doc-text");

            Label targetLabel = new Label(flow.getTargetScreenId());
            targetLabel.getStyleClass().add("doc-kbd");

            flowItem.getChildren().addAll(sourceLabel, arrow, targetLabel);
            flowList.getChildren().add(flowItem);
        }

        section.getChildren().add(flowList);
        return section;
    }

    // ==================== ARCHITECTURE SECTION ====================
    private VBox createArchitectureSection() {
        VBox section = createSection("🏗️ Architecture Analysis",
                "Component distribution across architectural layers.");

        Map<String, List<CodeComponent>> categorized = getCategorizedComponents();
        List<CodeComponent> components = getComponents();

        if (categorized == null || categorized.isEmpty()) {
            // Fallback to layer analysis
            Map<String, Integer> layerCounts = new HashMap<>();
            for (CodeComponent comp : components) {
                String layer = comp.getLayer() != null ? comp.getLayer() : "Unknown";
                layerCounts.merge(layer, 1, Integer::sum);
            }

            VBox layerList = new VBox(8);
            for (Map.Entry<String, Integer> entry : layerCounts.entrySet()) {
                HBox item = createListItem("📁 " + entry.getKey() + ": " + entry.getValue() + " components");
                layerList.getChildren().add(item);
            }
            section.getChildren().add(layerList);
        } else {
            VBox categoryList = new VBox(12);
            for (Map.Entry<String, List<CodeComponent>> entry : categorized.entrySet()) {
                VBox categoryCard = new VBox(4);
                categoryCard.getStyleClass().add("doc-card");

                Label categoryTitle = new Label("📦 " + entry.getKey() + " (" + entry.getValue().size() + ")");
                categoryTitle.getStyleClass().add("doc-card-title");
                categoryCard.getChildren().add(categoryTitle);

                // List first 5 components
                for (int i = 0; i < Math.min(entry.getValue().size(), 5); i++) {
                    CodeComponent comp = entry.getValue().get(i);
                    Label compLabel = new Label("   • " + comp.getName());
                    compLabel.getStyleClass().add("doc-text");
                    categoryCard.getChildren().add(compLabel);
                }
                if (entry.getValue().size() > 5) {
                    Label moreLabel = new Label("   ... and " + (entry.getValue().size() - 5) + " more");
                    moreLabel.getStyleClass().add("doc-text");
                    categoryCard.getChildren().add(moreLabel);
                }

                categoryList.getChildren().add(categoryCard);
            }
            section.getChildren().add(categoryList);
        }

        return section;
    }

    // ==================== TECH SUMMARY SECTION ====================
    private VBox createTechSummarySection() {
        VBox section = createSection("📊 Technical Summary",
                "Languages and technologies detected in the codebase.");

        List<CodeComponent> components = getComponents();

        // Count by language
        Map<String, Long> langCounts = components.stream()
                .filter(c -> c.getLanguage() != null)
                .collect(Collectors.groupingBy(CodeComponent::getLanguage, Collectors.counting()));

        GridPane techGrid = new GridPane();
        techGrid.setHgap(24);
        techGrid.setVgap(12);
        techGrid.getStyleClass().add("doc-tech-grid");

        int col = 0;
        int row = 0;
        for (Map.Entry<String, Long> entry : langCounts.entrySet()) {
            techGrid.add(createStatItem(entry.getKey(), entry.getValue() + " files"), col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }

        section.getChildren().add(techGrid);

        // Detect frameworks
        Set<String> frameworks = detectFrameworks(components);
        if (!frameworks.isEmpty()) {
            Label frameworksLabel = new Label("🔧 Frameworks: " + String.join(", ", frameworks));
            frameworksLabel.getStyleClass().add("doc-text");
            frameworksLabel.setWrapText(true);
            section.getChildren().add(frameworksLabel);
        }

        return section;
    }

    // ==================== HELPER METHODS ====================

    private VBox createSection(String title, String description) {
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

    private VBox createStatItem(String label, String value) {
        VBox item = new VBox(4);
        item.getStyleClass().add("doc-tech-item");

        Label labelNode = new Label(label.toUpperCase());
        labelNode.getStyleClass().add("doc-tech-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("doc-tech-value");

        item.getChildren().addAll(labelNode, valueNode);
        return item;
    }

    private HBox createListItem(String text) {
        HBox item = new HBox(8);
        item.getStyleClass().add("doc-list-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("doc-list-text");
        textLabel.setWrapText(true);

        item.getChildren().add(textLabel);
        return item;
    }

    private Label createNoDataLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("doc-text");
        label.setStyle("-fx-text-fill: #888;");
        return label;
    }

    private List<CodeComponent> getComponents() {
        return analysisResult.getComponents() != null ? 
                analysisResult.getComponents() : Collections.emptyList();
    }

    private Map<String, List<CodeComponent>> getCategorizedComponents() {
        return analysisResult.getCategorizedComponents() != null ?
                analysisResult.getCategorizedComponents() : Collections.emptyMap();
    }

    private int countByType(List<CodeComponent> components, String type) {
        return (int) components.stream()
                .filter(c -> c.getType() != null && c.getType().contains(type))
                .count();
    }

    private boolean isUIComponent(CodeComponent comp) {
        if (comp.getType() == null) return false;
        String type = comp.getType().toLowerCase();
        return type.contains("activity") || type.contains("fragment") || 
               type.contains("view") || type.contains("dialog");
    }

    private String inferScreenPurpose(CodeComponent comp) {
        String name = comp.getName().toLowerCase();
        
        if (name.contains("login")) return "Handles user authentication and login flow.";
        if (name.contains("register") || name.contains("signup")) return "Manages user registration and account creation.";
        if (name.contains("home") || name.contains("main")) return "Main entry point and dashboard for the application.";
        if (name.contains("profile")) return "Displays and manages user profile information.";
        if (name.contains("settings")) return "Application settings and preferences management.";
        if (name.contains("detail")) return "Shows detailed information for a selected item.";
        if (name.contains("list")) return "Displays a list of items for browsing.";
        if (name.contains("search")) return "Provides search functionality.";
        if (name.contains("splash")) return "Initial loading screen shown on app launch.";
        if (name.contains("onboard")) return "Guides new users through app features.";
        if (name.contains("payment") || name.contains("checkout")) return "Handles payment and checkout process.";
        if (name.contains("cart")) return "Shopping cart management.";
        
        return "Screen component for " + comp.getName().replaceAll("([A-Z])", " $1").trim() + ".";
    }

    private List<String> extractKeyMethods(List<CodeMethod> methods) {
        List<String> keyMethods = new ArrayList<>();
        for (CodeMethod method : methods) {
            String name = method.getName();
            if (name.startsWith("on") || name.startsWith("handle") || 
                name.startsWith("show") || name.startsWith("load") ||
                name.startsWith("submit") || name.startsWith("save") ||
                name.startsWith("delete") || name.startsWith("update")) {
                keyMethods.add(formatMethodName(name));
                if (keyMethods.size() >= 5) break;
            }
        }
        return keyMethods;
    }

    private String formatMethodName(String name) {
        return name.replaceAll("([A-Z])", " $1").trim();
    }

    private Map<String, Set<String>> extractFeaturesFromComponents(List<CodeComponent> components) {
        Map<String, Set<String>> features = new LinkedHashMap<>();
        
        for (CodeComponent comp : components) {
            if (!isUIComponent(comp)) continue;
            
            Set<String> screenFeatures = new LinkedHashSet<>();
            List<CodeMethod> methods = comp.getMethods();
            
            if (methods != null) {
                for (CodeMethod method : methods) {
                    String feature = methodToFeature(method.getName());
                    if (feature != null) {
                        screenFeatures.add(feature);
                    }
                }
            }
            
            if (!screenFeatures.isEmpty()) {
                features.put(comp.getName(), screenFeatures);
            }
        }
        
        return features;
    }

    private String methodToFeature(String methodName) {
        String name = methodName.toLowerCase();
        
        if (name.contains("login") || name.contains("signin")) return "User Login";
        if (name.contains("logout") || name.contains("signout")) return "User Logout";
        if (name.contains("register") || name.contains("signup")) return "User Registration";
        if (name.contains("search")) return "Search Functionality";
        if (name.contains("share")) return "Content Sharing";
        if (name.contains("upload")) return "File Upload";
        if (name.contains("download")) return "File Download";
        if (name.contains("refresh")) return "Data Refresh";
        if (name.contains("save")) return "Save Data";
        if (name.contains("delete") || name.contains("remove")) return "Delete Items";
        if (name.contains("edit") || name.contains("update")) return "Edit Content";
        if (name.contains("navigate") || name.contains("goto")) return "Navigation";
        if (name.contains("filter")) return "Filter Data";
        if (name.contains("sort")) return "Sort Data";
        if (name.contains("pay") || name.contains("checkout")) return "Payment Processing";
        if (name.contains("notification")) return "Notifications";
        
        return null;
    }

    private String getProcessIcon(BusinessProcessComponent.ProcessType type) {
        if (type == null) return "📌";
        switch (type) {
            case AUTHENTICATION: return "🔐";
            case USER_REGISTRATION: return "📝";
            case PAYMENT: return "💳";
            case SEARCH: return "🔍";
            case DATA_SYNC: return "🔄";
            case NOTIFICATION: return "🔔";
            default: return "📌";
        }
    }

    private String detectArchitecturePattern(List<CodeComponent> components) {
        boolean hasViewModels = components.stream().anyMatch(c -> 
            c.getType() != null && c.getType().toLowerCase().contains("viewmodel"));
        boolean hasRepositories = components.stream().anyMatch(c -> 
            c.getType() != null && c.getType().toLowerCase().contains("repository"));
        boolean hasPresenters = components.stream().anyMatch(c -> 
            c.getType() != null && c.getType().toLowerCase().contains("presenter"));
        boolean hasUseCases = components.stream().anyMatch(c -> 
            c.getType() != null && c.getType().toLowerCase().contains("usecase"));

        if (hasViewModels && hasRepositories) {
            if (hasUseCases) return "Clean Architecture with MVVM";
            return "MVVM (Model-View-ViewModel)";
        }
        if (hasPresenters) return "MVP (Model-View-Presenter)";
        if (hasRepositories) return "Repository Pattern";
        
        return null;
    }

    private Set<String> detectFrameworks(List<CodeComponent> components) {
        Set<String> frameworks = new LinkedHashSet<>();
        
        for (CodeComponent comp : components) {
            List<String> annotations = comp.getAnnotations();
            if (annotations != null) {
                for (String annotation : annotations) {
                    if (annotation.contains("Inject") || annotation.contains("Hilt")) {
                        frameworks.add("Hilt/Dagger");
                    }
                    if (annotation.contains("Room") || annotation.contains("Entity") || annotation.contains("Dao")) {
                        frameworks.add("Room Database");
                    }
                    if (annotation.contains("Composable")) {
                        frameworks.add("Jetpack Compose");
                    }
                    if (annotation.contains("Retrofit")) {
                        frameworks.add("Retrofit");
                    }
                }
            }
            
            if (comp.isCoroutineUsage()) {
                frameworks.add("Kotlin Coroutines");
            }
            if (comp.isViewBindingUsed()) {
                frameworks.add("ViewBinding");
            }
            if (comp.isDataBindingUsed()) {
                frameworks.add("DataBinding");
            }
        }
        
        return frameworks;
    }
}
