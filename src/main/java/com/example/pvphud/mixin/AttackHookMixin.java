package com.example.pvphud.mixin;

import com.example.pvphud.CombatTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает клиентский вызов "игрок атаковал сущность" (левый клик по мобу/игроку),
 * чтобы CombatTracker мгновенно зафиксировал цель, а не ждал следующего кадра прицела.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class AttackHookMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void pulsepvp$onAttackEntity(net.minecraft.entity.player.PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof LivingEntity living) {
            MinecraftClient client = MinecraftClient.getInstance();
            long time = client.world != null ? client.world.getTime() : 0L;
            CombatTracker.onPlayerHit(living, time);
        }
    }
}
