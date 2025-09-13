package com.projectvisualizer.visualization.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class LegendFactory {
    private LegendFactory() {}

    public static VBox createTechnicalArchitectureLegend(Color ui, Color business, Color data, Color other) {
        VBox legend = baseLegend();
        legend.getChildren().add(makeLegendItem(ui, "UI Layer"));
        legend.getChildren().add(makeLegendItem(business, "Business Logic"));
        legend.getChildren().add(makeLegendItem(data, "Data Layer"));
        legend.getChildren().add(makeLegendItem(other, "Other / Utility"));
        return legend;
    }

    public static VBox createUserJourneyLegend(Color entry, Color main, Color decision, Color exit) {
        VBox legend = baseLegend();
        legend.getChildren().add(makeLegendItem(entry, "Entry Point"));
        legend.getChildren().add(makeLegendItem(main, "Main Flow"));
        legend.getChildren().add(makeLegendItem(decision, "Decision"));
        legend.getChildren().add(makeLegendItem(exit, "Exit"));
        return legend;
    }

    public static VBox createBusinessProcessLegend(Color processColor, Color flowColor) {
        VBox legend = baseLegend();
        legend.getChildren().add(makeLegendItem(processColor, "Process Step"));
        legend.getChildren().add(makeLegendItem(flowColor, "Process Flow (edge)"));
        return legend;
    }

    public static VBox createFeatureOverviewLegend(Color groupColor, Color linkColor) {
        VBox legend = baseLegend();
        legend.getChildren().add(makeLegendItem(groupColor, "Feature Group"));
        legend.getChildren().add(makeLegendItem(linkColor, "Feature Link (edge)"));
        return legend;
    }

    public static VBox createIntegrationMapLegend(Color internalColor, Color externalColor) {
        VBox legend = baseLegend();
        legend.getChildren().add(makeLegendItem(internalColor, "Internal Component"));
        legend.getChildren().add(makeLegendItem(externalColor, "External Integration"));
        return legend;
    }

    public static HBox makeLegendItem(Color color, String text) {
        HBox row = new HBox(8);
        javafx.scene.shape.Rectangle swatch = new javafx.scene.shape.Rectangle(16, 12, color);
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #374151; -fx-font-size: 12;");
        row.getChildren().addAll(swatch, label);
        return row;
    }

    private static VBox baseLegend() {
        VBox legend = new VBox(6);
        legend.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        Label title = new Label("Legend");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-padding: 0 0 4 0;");
        legend.getChildren().add(title);
        legend.setLayoutX(20);
        legend.setLayoutY(20);
        return legend;
    }
}
