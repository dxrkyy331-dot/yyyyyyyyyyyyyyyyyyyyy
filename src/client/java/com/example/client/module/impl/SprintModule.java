package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;

public class SprintModule extends Module {
    public final BooleanSetting omniSprint = new BooleanSetting("We wszystkich kierunkach", false);

    public SprintModule() {
        super("Sprint", "Automatyczny sprint bez konieczności trzymania klawisza", Category.MOVEMENT);
        addSettings(omniSprint);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.options.keyUp.isDown()) {
            mc.player.setSprinting(true);
        }
    }
}
