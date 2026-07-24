package com.example.pvphud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class PulsePvpClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Обновляем боевые данные каждый game tick (20 раз/сек)
        ClientTickEvents.END_CLIENT_TICK.register(CombatTracker::tick);

        // Рисуем HUD каждый кадр.
        // Примечание: HudRenderCallback в 1.21.1 помечен deprecated в пользу
        // HudElementRegistry/LayeredDrawer, но по-прежнему рабочий и самый простой
        // способ добавить оверлей поверх ванильного HUD.
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                PulseHudRenderer.render(drawContext, MinecraftClient.getInstance()));
    }
}
