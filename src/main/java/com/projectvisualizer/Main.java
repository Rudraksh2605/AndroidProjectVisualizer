package com.projectvisualizer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
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
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}