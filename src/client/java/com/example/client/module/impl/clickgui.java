package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import com.example.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Moduł Click GUI – konfiguracja wyglądu i zachowania menu.
 * Ustawienia: Open Key, UI Scale, Liquid Glass, Background Blur,
 *             Background Dim, Interface Sounds, Toggle Sound.
 */
public final class clickgui extends Module {

    /** Klawisz otwierający GUI (domyślnie: Prawy Shift) */
    public static final int OPEN_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

    /** Skala UI (0.7x – 1.5x, krok 0.05) */
    public final NumberSetting scale = new NumberSetting("UI Scale", 1.0, 0.7, 1.5, 0.05);

    /** Efekt Liquid Glass – rozmycie tła przez szybę */
    public final BooleanSetting liquidGlass = new BooleanSetting("Liquid Glass", true);

    /** Siła rozmycia tła (0 – 6, krok 1) */
    public final NumberSetting blur = new NumberSetting("Background Blur", 2.0, 0.0, 6.0, 1.0);

    /** Przyciemnienie tła (0.0 – 0.9, krok 0.05) */
    public final NumberSetting dim = new NumberSetting("Background Dim", 0.4, 0.0, 0.9, 0.05);

    /** Dźwięki interfejsu przy kliknięciach */
    public final BooleanSetting sounds = new BooleanSetting("Interface Sounds", true);

    /** Tryb dźwięku przy włączaniu modułów: Blip, Chime, None */
    public final ModeSetting toggleSound = new ModeSetting(
            "Toggle Sound", "Blip", "Blip", "Chime", "None"
    );

    public clickgui() {
        super("Click GUI", "Ustawienia wyglądu i zachowania menu modułów", Category.MISC, OPEN_KEY);
        addSettings(scale, liquidGlass, blur, dim, sounds, toggleSound);
    }

    /**
     * Oblicza skalę UI proporcjonalnie do rozdzielczości okna.
     * @param framebufferHeight wysokość okna w pikselach
     * @return końcowa skala (min. 0.5)
     */
    public float getComputedScale(int framebufferHeight) {
        float base = Math.max(1.0f, framebufferHeight) / 540.0f;
        return Math.max(0.5f, base * (float) scale.getValue());
    }

    /** Czy efekt Liquid Glass jest włączony */
    public boolean isLiquidGlass() {
        return liquidGlass.isEnabled();
    }

    /** Siła rozmycia tła jako int (0–6) */
    public int getBlur() {
        return (int) blur.getValue();
    }

    /** Współczynnik przyciemnienia tła (0.0–0.9) */
    public float getDim() {
        return (float) dim.getValue();
    }

    /** Czy dźwięki interfejsu są włączone */
    public boolean hasSounds() {
        return sounds.isEnabled();
    }

    /** Czy wybrany tryb dźwięku to Chime */
    public boolean isChimeSound() {
        return toggleSound.is("Chime");
    }

    /** Czy wybrany tryb dźwięku to Blip */
    public boolean isBlipSound() {
        return toggleSound.is("Blip");
    }

    @Override
    public void onEnable() {
        // Moduł konfiguracyjny – brak akcji
    }

    @Override
    public void onDisable() {
        // Moduł konfiguracyjny – brak akcji
    }

    @Override
    public void onTick() {
        // Brak logiki tickowej
    }
}
