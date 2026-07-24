package com.example.pvphud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Собирает "сырые" боевые данные каждый тик:
 * - текущую цель (сущность под прицелом или последняя атакованная)
 * - прогресс восстановления атаки (0..1) для крит/кулдаун-индикатора
 * - активна ли пульсация (гладкая анимация 0..1, как "сердцебиение")
 */
public final class CombatTracker {

    private static LivingEntity lockedTarget;
    private static long lastHitTick = -1000;
    private static float pulsePhase = 0f;

    private CombatTracker() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        // 1. Обновляем цель: приоритет — то, во что игрок только что ударил,
        //    иначе — то, что сейчас под прицелом (в пределах реальной дистанции атаки).
        LivingEntity crosshairTarget = getEntityUnderCrosshair(client);
        if (crosshairTarget != null) {
            lockedTarget = crosshairTarget;
        } else if (client.world != null && client.world.getTime() - lastHitTick > 60) {
            // цель "протухла" — сбрасываем через 3 сек без апдейта
            lockedTarget = null;
        }

        // 2. Анимация пульса — синусоида, ускоряется когда кулдаун атаки почти заполнен
        float cooldown = getAttackCooldownProgress(client);
        float speed = 0.05f + cooldown * 0.15f; // чем ближе к 100% готовности — тем чаще "пульс"
        pulsePhase = (pulsePhase + speed) % (float) (Math.PI * 2);
    }

    /** Вызывать из миксина/колбэка при успешном попадании игрока по существу. */
    public static void onPlayerHit(LivingEntity victim, long worldTime) {
        lockedTarget = victim;
        lastHitTick = worldTime;
    }

    private static LivingEntity getEntityUnderCrosshair(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public static LivingEntity getTarget() {
        return lockedTarget;
    }

    /** 0.0 (только что ударили) .. 1.0 (атака полностью восстановлена / готов крит) */
    public static float getAttackCooldownProgress(MinecraftClient client) {
        if (client.player == null) return 1f;
        // getAttackCooldownProgress(0) — ванильный метод игрока, тот же что красит хотбар меча
        return client.player.getAttackCooldownProgress(0f);
    }

    public static boolean isCritReady(MinecraftClient client) {
        return getAttackCooldownProgress(client) >= 0.99f;
    }

    /** Значение пульсации 0..1, используйте для альфы/масштаба индикаторов. */
    public static float getPulseValue() {
        return (float) (Math.sin(pulsePhase) * 0.5 + 0.5);
    }

    public static double getTargetMaxHealth(LivingEntity target) {
        var attr = target.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        return attr != null ? attr.getValue() : target.getMaxHealth();
    }

    public static double getTargetArmor(LivingEntity target) {
        var attr = target.getAttributeInstance(EntityAttributes.ARMOR);
        return attr != null ? attr.getValue() : 0;
    }

    public static double distanceToTarget(MinecraftClient client, LivingEntity target) {
        if (client.player == null) return -1;
        Vec3d a = client.player.getEntityPos();
        Vec3d b = target.getEntityPos();
        return a.distanceTo(b);
    }
}
