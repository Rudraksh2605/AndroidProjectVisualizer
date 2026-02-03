package com.projectvisualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set path for custom native libraries (GPU support)
        String nativesPath = java.nio.file.Paths.get("natives").toAbsolutePath().toString();
        System.setProperty("de.kherud.llama.lib.path", nativesPath);

        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/fxml/main.fxml")));

        primaryStage.setTitle("Android Project Architecture Visualizer");
        // Try to load an application icon if available
        java.net.URL iconUrl = getClass().getResource("/icon.png");
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toString()));
        }

        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Ensure all background processes are terminated when the window is closed
        primaryStage.setOnCloseRequest(event -> {
            shutdownAllServices();
            Platform.exit();
        });

        // Register JVM shutdown hook as a fallback
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered - cleaning up resources...");
            shutdownAllServices();
        }));

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application stopping - cleaning up resources...");
        shutdownAllServices();
        super.stop();
    }

    /**
     * Shuts down all background services and releases resources.
     * This includes the AI inference service, model resources, and any background threads.
     */
    private void shutdownAllServices() {
        try {
            // Shutdown the AI service and its underlying model
            com.projectvisualizer.ai.ProjectUnderstandingService aiService = 
                    com.projectvisualizer.ai.ProjectUnderstandingService.getInstance();
            if (aiService != null) {
                aiService.shutdown();
                System.out.println("AI service shutdown completed");
            }
        } catch (Exception e) {
            System.err.println("Error during AI service shutdown: " + e.getMessage());
        }

        // Force exit to ensure all native threads are terminated
        System.out.println("All services shutdown - exiting application");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
