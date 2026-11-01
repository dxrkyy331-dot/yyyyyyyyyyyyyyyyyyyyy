package com.example.client;

import com.example.client.gui.ClickGuiScreen;
import com.example.client.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {

    public static KeyMapping clickGuiKey;

    @Override
    public void onInitializeClient() {
        // Rejestracja klawisza do otwierania ClickGUI (Domyslnie: Prawy Shift)
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rockstar.clickgui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.rockstar"
        ));

        // Tick klienta: otwieranie menu i logika modulow
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (clickGuiKey.consumeClick()) {
                client.setScreen(new ClickGuiScreen());
            }

            ModuleManager.getInstance().onTick();
        });

        // HUD 2D (Watermark, TargetHUD)
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.options.hideGui) {
                ModuleManager.getInstance().render2D(guiGraphics);
            }
        });

        // Rendering 3D swiata (TargetESP - kontur wokol gracza)
        WorldRenderEvents.LAST.register(context -> {
            if (Minecraft.getInstance().player != null) {
                ModuleManager.getInstance().onWorldRender(context);
            }
        });
    }
}