package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.NumberSetting;

public class FullbrightModule extends Module {
    public final NumberSetting brightness = new NumberSetting("Jasność", 10.0, 1.0, 15.0, 0.5);

    public FullbrightModule() {
        super("Fullbright", "Maksymalizuje oświetlenie w jaskiniach i nocy", Category.RENDER);
        addSettings(brightness);
    }
}
