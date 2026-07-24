package com.example.pvphud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Вся отрисовка. Подписан на HudRenderCallback в PulsePvpClient.
 * Работает только с DrawContext (2D), никакого 3D/world-рендера —
 * это чисто информационный оверлей, не даёт механического преимущества.
 */
public final class PulseHudRenderer {

    private PulseHudRenderer() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.player == null || client.options.hudHidden) return;

        renderCritCooldownBar(ctx, client);

        LivingEntity target = CombatTracker.getTarget();
        if (target != null && target.isAlive()) {
            renderTargetPulseBar(ctx, client, target);
            renderTargetEffects(ctx, client, target);
        }
    }

    // ---------- 1. Индикатор крита / кулдауна атаки ----------
    private static void renderCritCooldownBar(DrawContext ctx, MinecraftClient client) {
        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();

        int barW = 60;
        int barH = 4;
        int x = screenW / 2 - barW / 2;
        int y = screenH / 2 + 18; // чуть ниже прицела

        float progress = CombatTracker.getAttackCooldownProgress(client);
        boolean ready = CombatTracker.isCritReady(client);

        // фон
        ctx.fill(x, y, x + barW, y + barH, 0x80000000);
        // заполнение
        int filled = (int) (barW * progress);
        int color = ready ? pulseColor(0xFFFFD54A, 0xFFFF7A00) // готово: жёлто-оранжевая пульсация
                           : 0xFF55C0FF;                        // восстанавливается: спокойный синий
        ctx.fill(x, y, x + filled, y + barH, color);

        if (ready) {
            // лёгкая пульсирующая рамка вокруг, когда крит готов
            int glow = (int) (CombatTracker.getPulseValue() * 255);
            int glowColor = (glow << 24) | 0xFFFFFF;
            ctx.drawBorder(x - 1, y - 1, barW + 2, barH + 2, glowColor);
        }
    }

    // ---------- 2. Пульс HP / брони цели ----------
    private static void renderTargetPulseBar(DrawContext ctx, MinecraftClient client, LivingEntity target) {
        int screenW = ctx.getScaledWindowWidth();

        int barW = 120;
        int barH = 6;
        int x = screenW / 2 - barW / 2;
        int y = 30;

        float hpRatio = MathHelper.clamp(target.getHealth() / (float) CombatTracker.getTargetMaxHealth(target), 0, 1);
        double armor = CombatTracker.getTargetArmor(target);

        // "Пульс" — сама полоса слегка масштабируется/светлеет в такт CombatTracker.getPulseValue(),
        // сильнее пульсирует, когда HP цели низкое (< 30%) — визуальный сигнал "добивай".
        float pulse = CombatTracker.getPulseValue();
        boolean lowHp = hpRatio < 0.3f;
        int alphaBoost = lowHp ? (int) (pulse * 90) : (int) (pulse * 30);

        // имя цели
        ctx.drawCenteredTextWithShadow(client.textRenderer, target.getName(), screenW / 2, y - 12, 0xFFFFFFFF);

        // фон бара
        ctx.fill(x, y, x + barW, y + barH, 0x80000000);
        // HP
        int hpFilled = (int) (barW * hpRatio);
        int hpColor = lowHp ? withAlpha(0xFFFF3B3B, 200 + alphaBoost) : withAlpha(0xFF4CD137, 220);
        ctx.fill(x, y, x + hpFilled, y + barH, hpColor);

        // тонкая полоска брони под HP-баром (ширина пропорциональна значению брони, максимум 20)
        int armorBarY = y + barH + 2;
        int armorW = (int) (barW * MathHelper.clamp((float) armor / 20f, 0, 1));
        ctx.fill(x, armorBarY, x + barW, armorBarY + 2, 0x60000000);
        ctx.fill(x, armorBarY, x + armorW, armorBarY + 2, withAlpha(0xFFB0B0B0, 220));

        ctx.drawBorder(x - 1, y - 1, barW + 2, barH + 2, withAlpha(0xFFFFFFFF, lowHp ? 120 + alphaBoost : 60));
    }

    // ---------- 3. Таймеры эффектов зелий у цели ----------
    private static void renderTargetEffects(DrawContext ctx, MinecraftClient client, LivingEntity target) {
        List<StatusEffectInstance> effects = target.getStatusEffects().stream().toList();
        if (effects.isEmpty()) return;

        int screenW = ctx.getScaledWindowWidth();
        int startX = screenW / 2 - (effects.size() * 22) / 2;
        int y = 44;

        int i = 0;
        for (StatusEffectInstance effect : effects) {
            int x = startX + i * 22;
            int secondsLeft = effect.getDuration() / 20;

            // цветной квадрат-плашка вместо текстуры эффекта (не зависит от ресурспака)
            int color = effectColor(effect);
            ctx.fill(x, y, x + 18, y + 18, withAlpha(color, 200));
            ctx.drawBorder(x, y, 18, 18, 0x80000000);

            String label = secondsLeft >= 60 ? (secondsLeft / 60) + "m" : secondsLeft + "s";
            ctx.drawCenteredTextWithShadow(client.textRenderer, label, x + 9, y + 20, 0xFFFFFFFF);
            i++;
        }
    }

    // ---------- утилиты ----------
    private static int pulseColor(int colorA, int colorB) {
        float t = CombatTracker.getPulseValue();
        int a1 = (colorA >> 24) & 0xFF, r1 = (colorA >> 16) & 0xFF, g1 = (colorA >> 8) & 0xFF, b1 = colorA & 0xFF;
        int a2 = (colorB >> 24) & 0xFF, r2 = (colorB >> 16) & 0xFF, g2 = (colorB >> 8) & 0xFF, b2 = colorB & 0xFF;
        int a = (int) MathHelper.lerp(t, a1, a2);
        int r = (int) MathHelper.lerp(t, r1, r2);
        int g = (int) MathHelper.lerp(t, g1, g2);
        int b = (int) MathHelper.lerp(t, b1, b2);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int argb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    private static int effectColor(StatusEffectInstance effect) {
        // Простая эвристика по категории эффекта (полезный/вредный/нейтральный)
        return switch (effect.getEffectType().value().getCategory()) {
            case BENEFICIAL -> 0xFF4CD137;
            case HARMFUL -> 0xFFFF3B3B;
            default -> 0xFFB0B0B0;
        };
    }
}
