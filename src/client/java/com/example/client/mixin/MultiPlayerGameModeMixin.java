package com.example.client.mixin;

import com.example.client.module.ModuleManager;
import com.example.client.module.impl.HitSoundsModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        HitSoundsModule hitSounds = ModuleManager.getInstance().getModule(HitSoundsModule.class);
        if (hitSounds != null) {
            hitSounds.onAttack(player, target);
        }

        com.example.client.module.impl.TargetHudModule targetHud = ModuleManager.getInstance().getModule(com.example.client.module.impl.TargetHudModule.class);
        if (targetHud != null) {
            targetHud.onAttackTarget(target);
        }

        com.example.client.module.impl.FakePlayerModule fakePlayerMod = ModuleManager.getInstance().getModule(com.example.client.module.impl.FakePlayerModule.class);
        if (fakePlayerMod != null) {
            fakePlayerMod.onAttack(target);
        }
    }
}
