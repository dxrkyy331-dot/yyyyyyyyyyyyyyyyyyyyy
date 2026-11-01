package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.NumberSetting;

public class CoordinatesModule extends Module {
    public final BooleanSetting showBiome = new BooleanSetting("Pokaż Biom", true);
    public final NumberSetting scale = new NumberSetting("Rozmiar", 1.0, 0.5, 2.0, 0.1);

    public CoordinatesModule() {
        super("Coordinates", "Wyświetla aktualne koordynaty gracza X, Y, Z", Category.RENDER);
        addSettings(showBiome, scale);
    }
}
