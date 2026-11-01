package com.example.client.mixin;

import com.example.client.module.ModuleManager;
import com.example.client.module.impl.HitSoundsModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "animateHurt", at = @At("HEAD"))
    private void onAnimateHurt(float yaw, CallbackInfo ci) {
        HitSoundsModule hitSounds = ModuleManager.getInstance().getModule(HitSoundsModule.class);
        if (hitSounds != null) {
            hitSounds.onEntityHurt((LivingEntity) (Object) this);
        }
    }
}
