package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import com.example.client.setting.NumberSetting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Szablon przykładowego modułu.
 * Skopiuj tę klasę, zmień nazwę i zarejestruj w ModuleManager!
 */
public class ExampleModule extends Module {

    // 1. Definiujesz suwaki, przełączniki i tryby:
    public final BooleanSetting showParticles = new BooleanSetting("Cząsteczki", true);
    public final NumberSetting speed = new NumberSetting("Prędkość", 5.0, 1.0, 10.0, 0.5);
    public final ModeSetting mode = new ModeSetting("Tryb", "Normalny", "Normalny", "Szybki", "Niestandardowy");

    public ExampleModule() {
        // (Nazwa, Opis, Kategoria, Domyślny klawisz)
        super("WłasnyModuł", "Przykładowy szablon do tworzenia własnych modułów", Category.MISC, GLFW.GLFW_KEY_UNKNOWN);

        // 2. Dodajesz ustawienia do modułu:
        addSettings(showParticles, speed, mode);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§a[+] Włączono moduł: " + getName()), true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[-] Wyłączono moduł: " + getName()), true);
        }
    }

    @Override
    public void onTick() {
        // Kod wykonywany w każdym ticku gry (gdy moduł jest włączony)
    }
}
