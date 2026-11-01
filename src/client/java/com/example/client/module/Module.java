package com.example.client.module;

import com.example.client.category.Category;
import com.example.client.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bazowa klasa dla wszystkich modułów.
 * Aby dodać nowy moduł, wystarczy stworzyć nową klasę dziedziczącą po Module!
 */
public abstract class Module {
    protected final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private int key;
    private boolean toggled;
    private boolean expandedInGui = false;

    private final List<Setting> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
    }

    public Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = defaultKey;
        this.toggled = false;
    }

    public void addSettings(Setting... newSettings) {
        Collections.addAll(this.settings, newSettings);
    }

    public void toggle() {
        setToggled(!this.toggled);
    }

    public void setToggled(boolean toggled) {
        if (this.toggled != toggled) {
            this.toggled = toggled;
            if (this.toggled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    /**
     * Wywoływane gdy moduł zostanie włączony.
     */
    public void onEnable() {}

    /**
     * Wywoływane gdy moduł zostanie wyłączony.
     */
    public void onDisable() {}

    /**
     * Wywoływane w każdym ticku klienta, gdy moduł jest włączony.
     */
    public void onTick() {}

    /**
     * Wywoływane podczas renderowania HUD (2D), gdy moduł jest włączony.
     */
    public void render2D(net.minecraft.client.gui.GuiGraphics graphics) {}

    /**
     * Wywoływane podczas renderowania swiata 3D (WorldRenderEvents.LAST).
     */
    public void onWorldRender(WorldRenderContext context) {}

    // Gettery i Settery

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isToggled() {
        return toggled;
    }

    public boolean isExpandedInGui() {
        return expandedInGui;
    }

    public void setExpandedInGui(boolean expandedInGui) {
        this.expandedInGui = expandedInGui;
    }

    public void toggleExpandedInGui() {
        this.expandedInGui = !this.expandedInGui;
    }

    public List<Setting> getSettings() {
        return settings;
    }
}
