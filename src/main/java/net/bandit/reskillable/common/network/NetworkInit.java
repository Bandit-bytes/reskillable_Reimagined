package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkInit {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("reskillable");

        registrar.playToClient(SyncToClient.TYPE, SyncToClient.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.client.player.LocalPlayer player) {
                SyncToClient.handle(payload, player);
            }
        });

        registrar.playToClient(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.client.player.LocalPlayer player) {
                NotifyWarning.handle(payload, player);
            }
        });

        registrar.playToClient(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.client.player.LocalPlayer player) {
                SyncSkillConfig.handle(payload, player);
            }
        });

        registrar.playToServer(RequestLevelUp.TYPE, RequestLevelUp.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                RequestLevelUp.handle(payload, player);
            }
        });

        registrar.playToServer(TogglePerk.TYPE, TogglePerk.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                TogglePerk.handle(payload, player);
            }
        });
    }
}
