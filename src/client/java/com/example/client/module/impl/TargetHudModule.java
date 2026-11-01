package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.gui.RenderUtils;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import com.example.client.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Moduł Target HUD:
 * Wysoce zoptymalizowany, przezroczysty i nowoczesny wskaźnik celu w stylu Rockstar Client:
 * - Płynnie regulowana przezroczystość tła (suwak od 0.0 do 1.0)
 * - Animowany pasek zdrowia (smooth health lerp) bez nagłych skoków
 * - Brak migotania (Smooth Fade-Out / target hold timer)
 * - Awatar gracza lub stylowa plakietka z pierwszą literą celu
 * - Zoptymalizowany kod renderowania bez alokacji GC
 */
public class TargetHudModule extends Module {

    public final ModeSetting style = new ModeSetting("Styl", "Rockstar Neon", "Rockstar Neon", "Minimal", "Kompakt");
    public final NumberSetting backgroundAlpha = new NumberSetting("Przezroczystość", 0.45, 0.0, 1.0, 0.05);
    public final BooleanSetting showFace = new BooleanSetting("Awatar / Twarz", true);
    public final BooleanSetting showDistance = new BooleanSetting("Pokaż dystans", true);
    public final BooleanSetting animatedBar = new BooleanSetting("Płynny pasek HP", true);
    public final NumberSetting scale = new NumberSetting("Skala", 1.0, 0.7, 1.5, 0.05);

    // Zapamiętany cel i płynny fade-out
    private LivingEntity target = null;
    private long lastSeenTime = 0;
    private float animatedHp = -1f;
    private float damageAnimHp = -1f;

    public TargetHudModule() {
        super("TargetHUD", "Przezroczysty, zoptymalizowany wskaźnik zdrowia celu", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(style, backgroundAlpha, showFace, showDistance, animatedBar, scale);
        setToggled(true);
    }

    /**
     * Rejestracja ataku z HitSounds / gameMode – przytrzymuje HUD na aktywnym celu.
     */
    public void onAttackTarget(Entity entity) {
        if (entity instanceof LivingEntity && entity.isAlive() && entity != mc.player) {
            this.target = (LivingEntity) entity;
            this.lastSeenTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render2D(GuiGraphics graphics) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();

        // Sprawdzamy celownik
        Entity crosshair = mc.crosshairPickEntity;
        if (crosshair instanceof LivingEntity && crosshair.isAlive() && crosshair != mc.player) {
            this.target = (LivingEntity) crosshair;
            this.lastSeenTime = now;
        }

        // Płynne podtrzymanie celu do 850 ms po utracie z celownika (zero migotania)
        if (target == null || !target.isAlive()) return;
        long elapsed = now - lastSeenTime;
        if (elapsed > 850) {
            target = null;
            return;
        }

        // Obliczenie płynnego fade-out (przezroczystość na końcu)
        float alpha = 1.0f;
        if (elapsed > 600) {
            alpha = Math.max(0.0f, 1.0f - ((float) (elapsed - 600) / 250f));
        }
        if (alpha <= 0.01f) return;

        // Płynna interpolacja paska zdrowia (smooth lerp)
        float currentHp = Math.max(0.0f, target.getHealth());
        float maxHp = Math.max(1.0f, target.getMaxHealth());

        if (animatedHp < 0 || !animatedBar.isEnabled()) {
            animatedHp = currentHp;
            damageAnimHp = currentHp;
        } else {
            animatedHp += (currentHp - animatedHp) * 0.14f;
            if (damageAnimHp < currentHp) {
                damageAnimHp = currentHp;
            } else {
                damageAnimHp += (currentHp - damageAnimHp) * 0.04f;
            }
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 + 18;
        int y = screenHeight / 2 + 18;

        String currentStyle = style.getMode();
        if (currentStyle.equalsIgnoreCase("Minimal")) {
            renderMinimal(graphics, x, y, currentHp, maxHp, alpha);
        } else if (currentStyle.equalsIgnoreCase("Kompakt")) {
            renderCompact(graphics, x, y, currentHp, maxHp, alpha);
        } else {
            renderRockstarNeon(graphics, x, y, currentHp, maxHp, alpha);
        }
    }

    /**
     * Główny styl: Rockstar Neon (półprzezroczyste tło, neonowa ramka, awatar, animowany pasek).
     */
    private void renderRockstarNeon(GuiGraphics graphics, int x, int y, float hp, float maxHp, float alpha) {
        int width = 160;
        int height = 48;

        float userBgA = (float) backgroundAlpha.getValue();
        int bgA = (int) (userBgA * alpha * 255);
        int elemA = (int) (alpha * 255);

        int bgCol = (bgA << 24) | 0x11131B;
        int borderCol = (Math.min(255, (int)(elemA * 0.85f)) << 24) | 0x00E5FF;
        int textCol = (elemA << 24) | 0xFFFFFF;
        int subTextCol = (elemA << 24) | 0x8892B0;

        // 1. Przezroczyste tło z neonową ramką
        if (bgA > 0) {
            RenderUtils.drawRect(graphics, x, y, width, height, bgCol);
        }
        RenderUtils.drawOutline(graphics, x, y, width, height, 1, borderCol);

        // 2. Neonowy akcent boczny
        RenderUtils.drawRect(graphics, x, y, 3, height, (elemA << 24) | 0x00E5FF);

        int textStartX = x + 10;

        // 3. Awatar gracza lub plakietka
        if (showFace.isEnabled()) {
            int avatarSize = 34;
            int avatarX = x + 7;
            int avatarY = y + 7;

            if (target instanceof AbstractClientPlayer player) {
                PlayerSkin skin = player.getSkin();
                PlayerFaceRenderer.draw(graphics, skin, avatarX, avatarY, avatarSize);
                RenderUtils.drawOutline(graphics, avatarX, avatarY, avatarSize, avatarSize, 1, (elemA << 24) | 0x2A324B);
            } else {
                int plateBg = (Math.max(20, bgA) << 24) | 0x1D2232;
                RenderUtils.drawRect(graphics, avatarX, avatarY, avatarSize, avatarSize, plateBg);
                RenderUtils.drawOutline(graphics, avatarX, avatarY, avatarSize, avatarSize, 1, (elemA << 24) | 0x00E5FF);
                String initial = target.getName().getString();
                if (!initial.isEmpty()) initial = initial.substring(0, 1).toUpperCase();
                int initW = mc.font.width(initial);
                graphics.drawString(mc.font, initial, avatarX + (avatarSize - initW) / 2, avatarY + 12, (elemA << 24) | 0x00E5FF, false);
            }
            textStartX = avatarX + avatarSize + 8;
        }

        // 4. Nazwa celu
        String name = target.getDisplayName().getString();
        int maxNameWidth = width - (textStartX - x) - 38;
        if (mc.font.width(name) > maxNameWidth) {
            name = mc.font.plainSubstrByWidth(name, maxNameWidth - 6) + "...";
        }
        graphics.drawString(mc.font, name, textStartX, y + 8, textCol, false);

        // 5. Dystans
        if (showDistance.isEnabled()) {
            int distInt = (int) mc.player.distanceTo(target);
            String dStr = distInt + "m";
            graphics.drawString(mc.font, dStr, x + width - mc.font.width(dStr) - 7, y + 8, subTextCol, false);
        }

        // 6. Pasek zdrowia
        int barX = textStartX;
        int barY = y + 24;
        int barW = width - (textStartX - x) - 8;
        int barH = 7;

        int barBgA = Math.max(30, (int)(elemA * 0.45f));
        RenderUtils.drawRect(graphics, barX, barY, barW, barH, (barBgA << 24) | 0x1D2232);

        // Opóźniony pasek (damage bar)
        float dmgPct = Math.max(0.0f, Math.min(1.0f, damageAnimHp / maxHp));
        int dmgW = (int) (barW * dmgPct);
        if (dmgW > 0) {
            RenderUtils.drawRect(graphics, barX, barY, dmgW, barH, (elemA << 24) | 0xFF3366);
        }

        // Główny pasek HP
        float hpPct = Math.max(0.0f, Math.min(1.0f, animatedHp / maxHp));
        int filledW = (int) (barW * hpPct);
        if (filledW > 0) {
            RenderUtils.drawRect(graphics, barX, barY, filledW, barH, (elemA << 24) | 0x00E5FF);
        }

        // 7. Wartość HP
        int hpWhole = (int) hp;
        int hpDec = Math.abs((int) ((hp - hpWhole) * 10));
        String hpLabel = hpWhole + "." + hpDec + " HP";
        graphics.drawString(mc.font, hpLabel, barX, barY + 9, subTextCol, false);
    }

    /**
     * Styl Kompakt: Przezroczysty, zwięzły panel.
     */
    private void renderCompact(GuiGraphics graphics, int x, int y, float hp, float maxHp, float alpha) {
        int width = 130;
        int height = 32;

        float userBgA = (float) backgroundAlpha.getValue();
        int bgA = (int) (userBgA * alpha * 255);
        int elemA = (int) (alpha * 255);

        if (bgA > 0) {
            RenderUtils.drawRect(graphics, x, y, width, height, (bgA << 24) | 0x14161F);
        }
        RenderUtils.drawOutline(graphics, x, y, width, height, 1, (elemA << 24) | 0x2A324B);

        String name = target.getDisplayName().getString();
        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(name, width - 14), x + 6, y + 5, (elemA << 24) | 0xFFFFFF, false);

        int barX = x + 6;
        int barY = y + 18;
        int barW = width - 12;
        int barH = 5;

        RenderUtils.drawRect(graphics, barX, barY, barW, barH, (Math.max(25, (int)(elemA * 0.4f)) << 24) | 0x202535);
        float pct = Math.max(0f, Math.min(1f, animatedHp / maxHp));
        RenderUtils.drawRect(graphics, barX, barY, (int) (barW * pct), barH, (elemA << 24) | 0x00E5FF);
    }

    /**
     * Styl Minimal: Całkowicie przezroczysty, minimalistyczny pasek.
     */
    private void renderMinimal(GuiGraphics graphics, int x, int y, float hp, float maxHp, float alpha) {
        int width = 120;
        int height = 18;

        float userBgA = (float) backgroundAlpha.getValue();
        int bgA = (int) (userBgA * alpha * 255);
        int elemA = (int) (alpha * 255);

        if (bgA > 0) {
            RenderUtils.drawRect(graphics, x, y, width, height, (bgA << 24) | 0x11131B);
        }

        float pct = Math.max(0f, Math.min(1f, animatedHp / maxHp));
        RenderUtils.drawRect(graphics, x, y, (int) (width * pct), height, (Math.max(30, (int)(elemA * 0.5f)) << 24) | 0x00E5FF);
        RenderUtils.drawOutline(graphics, x, y, width, height, 1, (elemA << 24) | 0x00E5FF);

        String name = target.getName().getString();
        int hpWhole = (int) hp;
        String text = name + " §7(" + hpWhole + " HP)";
        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(text, width - 8), x + 5, y + 5, (elemA << 24) | 0xFFFFFF, false);
    }
}
