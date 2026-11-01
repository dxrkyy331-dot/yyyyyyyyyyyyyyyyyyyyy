package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.gui.RenderUtils;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class WatermarkModule extends Module {
    public final BooleanSetting showFps = new BooleanSetting("Pokaż FPS", true);
    public final ModeSetting style = new ModeSetting("Styl", "Rockstar", "Rockstar", "Minimal");

    public WatermarkModule() {
        super("Watermark", "Wyświetla logo i nazwę klienta na ekranie", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(showFps, style);
        setToggled(true); // Domyślnie włączony
    }

    public void render2D(GuiGraphics graphics) {
        if (!isToggled() || mc.font == null) return;

        int fps = mc.getFps();
        String text = showFps.isEnabled() ? "★ ROCKSTAR CLIENT | FPS: " + fps : "★ ROCKSTAR CLIENT";

        int textW = mc.font.width(text);
        int boxW = textW + 16;
        int boxH = 18;

        // Tło w stylu Rockstar
        RenderUtils.drawRect(graphics, 6, 6, boxW, boxH, 0xDD12141C);
        RenderUtils.drawOutline(graphics, 6, 6, boxW, boxH, 1, 0xFF242A3C);
        RenderUtils.drawRect(graphics, 6, 6, 2, boxH, 0xFF00E5FF);

        graphics.drawString(mc.font, text, 12, 11, 0xFF00E5FF, false);
    }
}
