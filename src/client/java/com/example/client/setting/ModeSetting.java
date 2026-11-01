package com.example.client.setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.index = Math.max(0, this.modes.indexOf(defaultMode));
    }

    public String getMode() {
        if (modes.isEmpty()) return "";
        return modes.get(index);
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.index = index;
        }
    }

    public void cycle() {
        if (!modes.isEmpty()) {
            index = (index + 1) % modes.size();
        }
    }

    public boolean is(String mode) {
        return getMode().equalsIgnoreCase(mode);
    }
}
