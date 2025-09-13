package com.projectvisualizer.visualization.render;

import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class EdgeRenderer {
    private EdgeRenderer() {}

    public static Path createCurvedEdge(Shape source, Shape target, String relationshipType, double curveStrength) {
        Path path = new Path();

        double startX, startY, endX, endY;

        if (source instanceof Rectangle) {
            Rectangle rect = (Rectangle) source;
            startX = rect.getX() + rect.getWidth() / 2;
            startY = rect.getY() + rect.getHeight() / 2;
        } else if (source instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) source;
            startX = ellipse.getCenterX();
            startY = ellipse.getCenterY();
        } else {
            startX = source.getBoundsInParent().getMinX();
            startY = source.getBoundsInParent().getMinY();
        }

        if (target instanceof Rectangle) {
            Rectangle rect = (Rectangle) target;
            endX = rect.getX() + rect.getWidth() / 2;
            endY = rect.getY() + rect.getHeight() / 2;
        } else if (target instanceof Ellipse) {
            Ellipse ellipse = (Ellipse) target;
            endX = ellipse.getCenterX();
            endY = ellipse.getCenterY();
        } else {
            endX = target.getBoundsInParent().getMinX();
            endY = target.getBoundsInParent().getMinY();
        }

        double controlX = (startX + endX) / 2;
        double controlY = (startY + endY) / 2 - curveStrength;

        path.getElements().add(new MoveTo(startX, startY));
        path.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));

        path.setStroke(getColorForRelationship(relationshipType));
        path.setStrokeWidth(2.5);
        path.setOpacity(0.8);
        path.setStrokeLineCap(StrokeLineCap.ROUND);

        if ("DEPENDS".equals(relationshipType)) {
            path.getStrokeDashArray().addAll(5d, 5d);
        }

        return path;
    }

    public static Polygon createArrowhead(Path edge, double arrowSize) {
        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(
                0.0, 0.0,
                -arrowSize, -arrowSize / 2,
                -arrowSize, arrowSize / 2
        );

        // Position at the end of the path
        if (!edge.getElements().isEmpty()) {
            PathElement lastElement = edge.getElements().get(edge.getElements().size() - 1);
            if (lastElement instanceof QuadCurveTo) {
                QuadCurveTo curve = (QuadCurveTo) lastElement;
                arrowhead.setTranslateX(curve.getX());
                arrowhead.setTranslateY(curve.getY());
            }

            // Rotate to align with direction from previous element
            if (edge.getElements().size() >= 2) {
                PathElement prev = edge.getElements().get(edge.getElements().size() - 2);
                if (prev instanceof MoveTo && lastElement instanceof QuadCurveTo) {
                    MoveTo move = (MoveTo) prev;
                    QuadCurveTo curve = (QuadCurveTo) lastElement;
                    double dx = curve.getX() - move.getX();
                    double dy = curve.getY() - move.getY();
                    double angle = Math.toDegrees(Math.atan2(dy, dx));
                    arrowhead.setRotate(angle);
                }
            }
        }

        arrowhead.setFill(((Shape) edge).getStroke());
        arrowhead.setStroke(((Shape) edge).getStroke());
        arrowhead.setStrokeWidth(1);
        return arrowhead;
    }

    public static Color getColorForRelationship(String relationshipType) {
        if (relationshipType == null) return Color.web("#9ca3af"); // GRAY_400 default
        switch (relationshipType) {
            case "EXTENDS": return Color.web("#3b82f6"); // PRIMARY_500
            case "IMPLEMENTS": return Color.web("#8b5cf6"); // PURPLE
            case "USES": return Color.web("#0ea5e9"); // INFO
            case "DEPENDS": return Color.web("#f59e0b"); // WARNING
            case "STARTS": return Color.web("#10b981"); // SUCCESS
            case "TERMINATES": return Color.web("#ef4444"); // ERROR
            default: return Color.web("#9ca3af");
        }
    }
}
