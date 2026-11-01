package com.example.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public class RenderUtils {

    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int color) {
        graphics.fill(x, y, x + width, y + thickness, color); // Top
        graphics.fill(x, y + height - thickness, x + width, y + height, color); // Bottom
        graphics.fill(x, y + thickness, x + thickness, y + height - thickness, color); // Left
        graphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color); // Right
    }

    public static void drawGradient(GuiGraphics graphics, int x, int y, int width, int height, int colorStart, int colorEnd) {
        graphics.fillGradient(x, y, x + width, y + height, colorStart, colorEnd);
    }

    public static void drawGradientHorizontal(GuiGraphics graphics, int x, int y, int width, int height, int colorStart, int colorEnd) {
        if (width <= 0 || height <= 0) return;
        int steps = Math.min(width, 32);
        float stepWidth = (float) width / steps;
        for (int i = 0; i < steps; i++) {
            float t1 = (float) i / steps;
            float t2 = (float) (i + 1) / steps;
            int c1 = interpolateColor(colorStart, colorEnd, (t1 + t2) * 0.5f);
            int sx = x + (int) (i * stepWidth);
            int ex = (i == steps - 1) ? (x + width) : (x + (int) ((i + 1) * stepWidth));
            graphics.fill(sx, y, ex, y + height, c1);
        }
    }

    public static void drawGlow(GuiGraphics graphics, int x, int y, int width, int height, int glowColor, int layers) {
        int baseAlpha = (glowColor >> 24) & 0xFF;
        if (baseAlpha == 0) baseAlpha = 255;
        int rgb = glowColor & 0x00FFFFFF;

        for (int i = 1; i <= layers; i++) {
            int alpha = Math.max(2, (int) (baseAlpha * (1.0f - (float) i / (layers + 1)) * 0.25f));
            int layerColor = (alpha << 24) | rgb;
            drawOutline(graphics, x - i, y - i, width + (i * 2), height + (i * 2), 1, layerColor);
        }
    }

    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width < 4 || height < 4) {
            drawRect(graphics, x, y, width, height, color);
            return;
        }
        // Środek
        graphics.fill(x + 1, y, x + width - 1, y + height, color);
        // Boki bez rogów
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static int interpolateColor(int color1, int color2, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
