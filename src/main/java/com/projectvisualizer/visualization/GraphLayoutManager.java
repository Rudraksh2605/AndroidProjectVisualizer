package com.projectvisualizer.visualization;

import java.util.Collection;
import java.util.Random;

public class GraphLayoutManager {
    private Random random = new Random();
    private static final double INITIAL_PADDING = 100;
    private static final double NODE_SPACING = 180;
    private static final double MIN_NODE_DISTANCE = 120; // Minimum distance between nodes
    private double currentCenterX = 400;
    private double currentCenterY = 300;

    public void positionNewNode(GraphNode newNode, Collection<GraphNode> existingNodes) {
        if (existingNodes.isEmpty()) {
            // First node - position near center of initial viewport
            newNode.getContainer().setLayoutX(currentCenterX);
            newNode.getContainer().setLayoutY(currentCenterY);
        } else if (existingNodes.size() == 1) {
            // Second node - position to the right with collision check
            GraphNode firstNode = existingNodes.iterator().next();
            double x = firstNode.getContainer().getLayoutX() + NODE_SPACING;
            double y = firstNode.getContainer().getLayoutY();

            // Ensure no overlap
            if (isOverlapping(newNode, x, y, existingNodes)) {
                // Try alternative positions
                Position alternative = findNonOverlappingPosition(newNode, x, y, existingNodes);
                x = alternative.x;
                y = alternative.y;
            }

            newNode.getContainer().setLayoutX(x);
            newNode.getContainer().setLayoutY(y);
        } else {
            // Position in expanding spiral pattern with collision avoidance
            positionWithCollisionAvoidance(newNode, existingNodes);
        }
    }

    private void positionWithCollisionAvoidance(GraphNode newNode, Collection<GraphNode> existingNodes) {
        int nodeCount = existingNodes.size();
        int maxAttempts = 50; // Maximum attempts to find a non-overlapping position

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Spiral positioning - creates an organic, expanding layout
            double angle = nodeCount * 0.8 + (attempt * 0.5); // Vary angle with attempts
            double radius = 120 + (nodeCount * 40) + (attempt * 20); // Expanding radius

            // Calculate position based on spiral
            double x = currentCenterX + radius * Math.cos(angle);
            double y = currentCenterY + radius * Math.sin(angle);

            // Check if this position overlaps with existing nodes
            if (!isOverlapping(newNode, x, y, existingNodes)) {
                newNode.getContainer().setLayoutX(x);
                newNode.getContainer().setLayoutY(y);
                return;
            }
        }

        // If all attempts fail, use force-directed approach
        positionWithForceDirected(newNode, existingNodes);
    }

    private void positionWithForceDirected(GraphNode newNode, Collection<GraphNode> existingNodes) {
        double x = currentCenterX;
        double y = currentCenterY;
        double repulsionForce = 200.0;
        int iterations = 20;

        for (int i = 0; i < iterations; i++) {
            double forceX = 0;
            double forceY = 0;

            for (GraphNode existingNode : existingNodes) {
                double dx = x - existingNode.getContainer().getLayoutX();
                double dy = y - existingNode.getContainer().getLayoutY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < MIN_NODE_DISTANCE) {
                    double force = repulsionForce / (distance + 1);
                    forceX += (dx / distance) * force;
                    forceY += (dy / distance) * force;
                }
            }

            // Apply forces with damping
            x += forceX * 0.1;
            y += forceY * 0.1;

            // Keep within reasonable bounds of large canvas
            x = Math.max(100, Math.min(1900, x));
            y = Math.max(100, Math.min(1900, y));
        }

        newNode.getContainer().setLayoutX(x);
        newNode.getContainer().setLayoutY(y);
    }

    private boolean isOverlapping(GraphNode newNode, double x, double y, Collection<GraphNode> existingNodes) {
        double newNodeWidth = newNode.getContainer().getBoundsInLocal().getWidth();
        double newNodeHeight = newNode.getContainer().getBoundsInLocal().getHeight();

        for (GraphNode existingNode : existingNodes) {
            double existingX = existingNode.getContainer().getLayoutX();
            double existingY = existingNode.getContainer().getLayoutY();
            double existingWidth = existingNode.getContainer().getBoundsInLocal().getWidth();
            double existingHeight = existingNode.getContainer().getBoundsInLocal().getHeight();

            // Calculate distance between node centers
            double centerX1 = x + newNodeWidth / 2;
            double centerY1 = y + newNodeHeight / 2;
            double centerX2 = existingX + existingWidth / 2;
            double centerY2 = existingY + existingHeight / 2;

            double distance = Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));

            // Consider nodes overlapping if distance is less than minimum allowed
            if (distance < MIN_NODE_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private Position findNonOverlappingPosition(GraphNode newNode, double preferredX, double preferredY,
                                                Collection<GraphNode> existingNodes) {
        // Try positions in a circle around the preferred position
        int attempts = 8; // Try 8 directions
        double radius = MIN_NODE_DISTANCE;

        for (int i = 0; i < attempts; i++) {
            double angle = 2 * Math.PI * i / attempts;
            double x = preferredX + radius * Math.cos(angle);
            double y = preferredY + radius * Math.sin(angle);

            if (!isOverlapping(newNode, x, y, existingNodes)) {
                return new Position(x, y);
            }

            // Increase radius for next attempt
            radius += 20;
        }

        // Fallback: random position
        return findRandomNonOverlappingPosition(newNode, existingNodes);
    }

    private Position findRandomNonOverlappingPosition(GraphNode newNode, Collection<GraphNode> existingNodes) {
        int maxAttempts = 100;

        for (int i = 0; i < maxAttempts; i++) {
            double x = currentCenterX + (random.nextDouble() - 0.5) * 400;
            double y = currentCenterY + (random.nextDouble() - 0.5) * 300;

            if (!isOverlapping(newNode, x, y, existingNodes)) {
                return new Position(x, y);
            }
        }

        // Last resort: far away from center
        return new Position(currentCenterX + 500, currentCenterY + 400);
    }

    public void rearrangeNodes(Collection<GraphNode> nodes) {
        int nodeCount = nodes.size();
        if (nodeCount == 0) return;

        // Circular arrangement around current center with collision avoidance
        int index = 0;
        double radius = Math.max(150, nodeCount * 30);

        // First pass: arrange in circle
        for (GraphNode node : nodes) {
            double angle = 2 * Math.PI * index / nodeCount;
            double x = currentCenterX + radius * Math.cos(angle);
            double y = currentCenterY + radius * Math.sin(angle);

            node.getContainer().setLayoutX(x);
            node.getContainer().setLayoutY(y);
            index++;
        }

        // Second pass: resolve overlaps with force-directed approach
        resolveAllOverlaps(nodes);
    }

    private void resolveAllOverlaps(Collection<GraphNode> nodes) {
        double repulsionForce = 150.0;
        int iterations = 30;
        boolean hasOverlaps = true;

        for (int iter = 0; iter < iterations && hasOverlaps; iter++) {
            hasOverlaps = false;

            for (GraphNode node1 : nodes) {
                double forceX = 0;
                double forceY = 0;

                for (GraphNode node2 : nodes) {
                    if (node1 == node2) continue;

                    double x1 = node1.getContainer().getLayoutX();
                    double y1 = node1.getContainer().getLayoutY();
                    double x2 = node2.getContainer().getLayoutX();
                    double y2 = node2.getContainer().getLayoutY();

                    double dx = x1 - x2;
                    double dy = y1 - y2;
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (distance < MIN_NODE_DISTANCE) {
                        hasOverlaps = true;
                        double force = repulsionForce / (distance + 1);
                        forceX += (dx / distance) * force;
                        forceY += (dy / distance) * force;
                    }
                }

                // Apply forces with damping
                if (forceX != 0 || forceY != 0) {
                    double newX = node1.getContainer().getLayoutX() + forceX * 0.1;
                    double newY = node1.getContainer().getLayoutY() + forceY * 0.1;

                    // Keep within reasonable bounds
                    newX = Math.max(50, Math.min(750, newX));
                    newY = Math.max(50, Math.min(550, newY));

                    node1.getContainer().setLayoutX(newX);
                    node1.getContainer().setLayoutY(newY);
                }
            }
        }
    }

    public void setCenter(double x, double y) {
        this.currentCenterX = x;
        this.currentCenterY = y;
    }

    // Helper class for position
    private static class Position {
        double x, y;
        Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}