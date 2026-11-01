package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.ModeSetting;
import com.example.client.setting.NumberSetting;
import javazoom.jl.player.Player;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Moduł HitSounds:
 * Odtwarza konfigurowalny dźwięk przy trafieniu (w tym własny fatality-hit-sound.mp3).
 */
public class HitSoundsModule extends Module {

    public final ModeSetting sound = new ModeSetting("Dźwięk", "Fatality",
            "Fatality", "Bell", "Orb", "Arrow", "Pling", "Chime", "Amethyst", "Anvil", "Pop", "Bit", "Mace", "Crit", "Bonk");

    public final ModeSetting triggerMode = new ModeSetting("Wyzwalacz", "Natychmiast",
            "Natychmiast", "Przy zranieniu", "Oba");

    public final NumberSetting volume = new NumberSetting("Głośność", 1.0, 0.1, 2.0, 0.1);
    public final NumberSetting pitch = new NumberSetting("Ton (Pitch)", 1.0, 0.5, 2.0, 0.05);
    public final BooleanSetting randomPitch = new BooleanSetting("Wariacja tonu", false);
    public final BooleanSetting onlyPlayers = new BooleanSetting("Tylko gracze", false);
    public final BooleanSetting cooldownCheck = new BooleanSetting("Tylko pełny cios", false);

    private final Random random = new Random();
    private final ExecutorService soundExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "HitSound-AudioThread");
        t.setDaemon(true);
        return t;
    });

    private byte[] customMp3Data = null;
    private int lastAttackedEntityId = -1;
    private long lastAttackTimeMs = 0;
    private long lastSoundPlayedMs = 0;

    public HitSoundsModule() {
        super("HitSounds", "Odtwarza satysfakcjonujący dźwięk (w tym Fatality) po trafieniu wroga", Category.COMBAT, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(sound, triggerMode, volume, pitch, randomPitch, onlyPlayers, cooldownCheck);
        setToggled(true);

        loadCustomSound();
    }

    private void loadCustomSound() {
        try (InputStream is = getClass().getResourceAsStream("/assets/modid/sounds/fatality-hit-sound.mp3")) {
            if (is != null) {
                this.customMp3Data = is.readAllBytes();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            playSound();
        }
    }

    public void onAttack(net.minecraft.world.entity.player.Player player, Entity target) {
        if (!isToggled() || player == null || target == null) return;
        if (player != mc.player) return;

        if (onlyPlayers.isEnabled() && !(target instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }

        if (cooldownCheck.isEnabled() && player.getAttackStrengthScale(0.5f) < 0.9f) {
            return;
        }

        this.lastAttackedEntityId = target.getId();
        this.lastAttackTimeMs = System.currentTimeMillis();

        String mode = triggerMode.getMode();
        if (mode.equalsIgnoreCase("Natychmiast") || mode.equalsIgnoreCase("Oba")) {
            playSound();
        }
    }

    public void onEntityHurt(LivingEntity entity) {
        if (!isToggled() || entity == null || mc.player == null) return;

        String mode = triggerMode.getMode();
        if (mode.equalsIgnoreCase("Natychmiast")) return;

        long now = System.currentTimeMillis();
        if (entity.getId() == lastAttackedEntityId && (now - lastAttackTimeMs) <= 750) {
            if (now - lastSoundPlayedMs > 70) {
                playSound();
            }
        }
    }

    public void playSound() {
        if (sound.getMode().equalsIgnoreCase("Fatality")) {
            playCustomMp3();
            lastSoundPlayedMs = System.currentTimeMillis();
            return;
        }

        SoundEvent soundEvent = getSelectedSound();
        if (soundEvent == null || mc.getSoundManager() == null) return;

        float basePitch = (float) pitch.getValue();
        if (randomPitch.isEnabled()) {
            float variance = (random.nextFloat() * 0.24f) - 0.12f;
            basePitch = Math.max(0.5f, Math.min(2.0f, basePitch + variance));
        }

        float soundVolume = (float) volume.getValue();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, basePitch, soundVolume));
        lastSoundPlayedMs = System.currentTimeMillis();
    }

    private void playCustomMp3() {
        if (customMp3Data == null || customMp3Data.length == 0) {
            loadCustomSound();
        }
        if (customMp3Data == null || customMp3Data.length == 0) {
            return;
        }

        soundExecutor.submit(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(customMp3Data);
                Player mp3Player = new Player(bais);
                mp3Player.play();
                mp3Player.close();
            } catch (Exception ignored) {
            }
        });
    }

    private SoundEvent getSelectedSound() {
        Object raw = switch (sound.getMode().toLowerCase()) {
            case "bell" -> SoundEvents.NOTE_BLOCK_BELL;
            case "orb" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "arrow" -> SoundEvents.ARROW_HIT_PLAYER;
            case "pling" -> SoundEvents.NOTE_BLOCK_PLING;
            case "chime" -> SoundEvents.NOTE_BLOCK_CHIME;
            case "amethyst" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "anvil" -> SoundEvents.ANVIL_LAND;
            case "pop" -> SoundEvents.ITEM_PICKUP;
            case "bit" -> SoundEvents.NOTE_BLOCK_BIT;
            case "mace" -> SoundEvents.MACE_SMASH_GROUND_HEAVY;
            case "crit" -> SoundEvents.PLAYER_ATTACK_CRIT;
            case "bonk" -> SoundEvents.NOTE_BLOCK_BASEDRUM;
            default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
        return asSoundEvent(raw);
    }

    private SoundEvent asSoundEvent(Object obj) {
        if (obj instanceof Holder<?> holder) {
            Object val = holder.value();
            if (val instanceof SoundEvent se) return se;
        } else if (obj instanceof SoundEvent se) {
            return se;
        }
        return SoundEvents.EXPERIENCE_ORB_PICKUP;
    }
}
