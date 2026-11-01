package com.example.client.category;

public enum Category {
    COMBAT("Combat", 0xFFEF5350),
    MOVEMENT("Movement", 0xFF66BB6A),
    RENDER("Render", 0xFFAB47BC),
    PLAYER("Player", 0xFFFFA726),
    MISC("Misc", 0xFF26C6DA);

    private final String displayName;
    private final int color;

    Category(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
