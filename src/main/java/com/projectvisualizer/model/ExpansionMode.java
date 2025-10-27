package com.projectvisualizer.model;

public enum ExpansionMode {
    UI_COMPONENTS("UI Components"),
    CLASS_DEPENDENCIES("Class Dependencies");

    private final String displayName;

    ExpansionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
