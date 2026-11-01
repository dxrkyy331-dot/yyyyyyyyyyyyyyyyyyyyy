package com.example.client.module.impl;

import com.example.client.category.Category;
import com.example.client.module.Module;
import com.example.client.setting.BooleanSetting;
import com.example.client.setting.NumberSetting;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Moduł FakePlayer:
 * Tworzy w świecie gracza o nazwie "krux visual" do testów (TargetESP, TargetHUD, HitSounds).
 * Po zabiciu odradza się automatycznie po 3 sekundach.
 */
public class FakePlayerModule extends Module {

    public final NumberSetting respawnDelay = new NumberSetting("Czas odrodzenia", 3.0, 1.0, 10.0, 0.5);
    public final NumberSetting playerHp = new NumberSetting("Punkty HP", 20.0, 1.0, 100.0, 1.0);
    public final BooleanSetting copyGear = new BooleanSetting("Kopiuj ekwipunek", true);

    private RemotePlayer fakePlayer = null;
    private Vec3 spawnPos = null;
    private float spawnYaw = 0f;
    private long deathTimeMs = -1;

    public FakePlayerModule() {
        super("FakePlayer", "Tworzy gracza krux visual, ktory odradza sie po 3s", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(respawnDelay, playerHp, copyGear);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) return;
        deathTimeMs = -1;
        spawnFakePlayer();
    }

    @Override
    public void onDisable() {
        removeFakePlayer();
        deathTimeMs = -1;
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        // Jeśli fake player zginął, odliczamy czas do odrodzenia (domyślnie 3 sekundy)
        if (fakePlayer == null && deathTimeMs > 0) {
            long delayMs = (long) (respawnDelay.getValue() * 1000.0);
            if (System.currentTimeMillis() - deathTimeMs >= delayMs) {
                spawnFakePlayer();
                deathTimeMs = -1;
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§a[+] krux visual odrodził się!"), true);
                }
            }
        }
    }

    /**
     * Spawnuje fake playera "krux visual" 2.5 bloku przed graczem.
     */
    private void spawnFakePlayer() {
        if (mc.player == null || mc.level == null) return;

        removeFakePlayer();

        // Wyznaczenie pozycji przed graczem
        if (spawnPos == null) {
            Vec3 look = mc.player.getLookAngle();
            double sx = mc.player.getX() + look.x * 2.5;
            double sy = mc.player.getY();
            double sz = mc.player.getZ() + look.z * 2.5;
            spawnPos = new Vec3(sx, sy, sz);
            spawnYaw = mc.player.getYRot() + 180f;
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), "krux visual");
        fakePlayer = new RemotePlayer(mc.level, profile);

        fakePlayer.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        fakePlayer.setYRot(spawnYaw);
        fakePlayer.setXRot(0f);
        fakePlayer.setYHeadRot(spawnYaw);
        fakePlayer.setYBodyRot(spawnYaw);

        float maxHealth = (float) playerHp.getValue();
        fakePlayer.setHealth(maxHealth);

        // Opcjonalne skopiowanie zbroi od gracza dla lepszego wyglądu
        if (copyGear.isEnabled()) {
            fakePlayer.setItemSlot(EquipmentSlot.HEAD, mc.player.getItemBySlot(EquipmentSlot.HEAD).copy());
            fakePlayer.setItemSlot(EquipmentSlot.CHEST, mc.player.getItemBySlot(EquipmentSlot.CHEST).copy());
            fakePlayer.setItemSlot(EquipmentSlot.LEGS, mc.player.getItemBySlot(EquipmentSlot.LEGS).copy());
            fakePlayer.setItemSlot(EquipmentSlot.FEET, mc.player.getItemBySlot(EquipmentSlot.FEET).copy());
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, mc.player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        }

        mc.level.addEntity(fakePlayer);

        // Cząsteczki pojawienia się
        for (int i = 0; i < 8; i++) {
            mc.level.addParticle(ParticleTypes.CLOUD, spawnPos.x, spawnPos.y + 1.0, spawnPos.z, 0, 0.05, 0);
        }
    }

    /**
     * Obsługa ataku na fake playera.
     */
    public void onAttack(Entity target) {
        if (!isToggled() || fakePlayer == null || target != fakePlayer) return;
        if (mc.player == null || mc.level == null) return;

        // Obliczenie obrażeń (standardowo 5-7 HP lub bazowane na broni)
        float damage = 5.0f;
        float currentHp = fakePlayer.getHealth();
        float newHp = Math.max(0f, currentHp - damage);

        fakePlayer.setHealth(newHp);
        fakePlayer.animateHurt(0f);

        // Dźwięk zranienia
        mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Jeśli HP spadło do 0 – fake player ginie
        if (newHp <= 0f) {
            handleDeath();
        }
    }

    private void handleDeath() {
        if (fakePlayer == null || mc.level == null) return;

        // Dźwięk śmierci i cząsteczki
        mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1.0f, 1.0f);

        for (int i = 0; i < 15; i++) {
            mc.level.addParticle(ParticleTypes.POOF, fakePlayer.getX(), fakePlayer.getY() + 1.0, fakePlayer.getZ(),
                    (Math.random() - 0.5) * 0.2, Math.random() * 0.15, (Math.random() - 0.5) * 0.2);
        }

        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[-] Zabito krux visual! Odrodzenie za 3 sekundy..."), true);
        }

        removeFakePlayer();
        this.deathTimeMs = System.currentTimeMillis();
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.level != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }
    }

    public RemotePlayer getFakePlayer() {
        return fakePlayer;
    }
}
