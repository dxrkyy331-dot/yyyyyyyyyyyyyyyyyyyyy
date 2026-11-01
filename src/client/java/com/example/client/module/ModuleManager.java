package com.example.client.module;

import com.example.client.category.Category;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private ModuleManager() {
        initModules();
    }

    /**
     * TUTAJ MOŻESZ DODAWAĆ SWOJE WŁASNE MODUŁY!
     * Wystarczy wpisać: register(new TwojNowyModul());
     */
    public void initModules() {
        register(new com.example.client.module.impl.HitSoundsModule());
        register(new com.example.client.module.impl.TargetESPModule());
        register(new com.example.client.module.impl.TargetHudModule());
        register(new com.example.client.module.impl.WatermarkModule());
        register(new com.example.client.module.impl.CoordinatesModule());
        register(new com.example.client.module.impl.FullbrightModule());
        register(new com.example.client.module.impl.SprintModule());
        register(new com.example.client.module.impl.FakePlayerModule());
        register(new com.example.client.module.impl.ExampleModule());
        register(new com.example.client.module.impl.clickgui());
    }

    public void register(Module module) {
        this.modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) {
                return (T) m;
            }
        }
        return null;
    }

    public void onTick() {
        for (Module module : modules) {
            if (module.isToggled()) {
                try {
                    module.onTick();
                } catch (Exception e) {
                    // zabezpieczenie przed crashami z modułów
                }
            }
        }
    }

    public void onKeyPress(int key) {
        if (key <= 0) return;
        for (Module module : modules) {
            if (module.getKey() == key) {
                module.toggle();
            }
        }
    }

    public void render2D(net.minecraft.client.gui.GuiGraphics graphics) {
        for (Module module : modules) {
            if (module.isToggled()) {
                try {
                    module.render2D(graphics);
                } catch (Exception e) {
                    // zabezpieczenie
                }
            }
        }
    }

    public void onWorldRender(WorldRenderContext context) {
        for (Module module : modules) {
            if (module.isToggled()) {
                try {
                    module.onWorldRender(context);
                } catch (Exception e) {
                    // zabezpieczenie
                }
            }
        }
    }
}
