package com.projectvisualizer.visualization;

public enum AbstractionLevel {
    HIGH_LEVEL("High Level", "Shows packages/modules and their dependencies"),
    COMPONENT_FLOW("Component Flow", "Shows Activities, Fragments, Services with navigation"),
    LAYERED_ARCHITECTURE("Layered Architecture", "Shows MVVM/MVC layers with inter-layer connections"),
    FEATURE_BASED("Feature Based", "Groups components by feature modules"),
    DETAILED("Detailed", "Shows all classes and their relationships");

    private final String displayName;
    private final String description;

    AbstractionLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}