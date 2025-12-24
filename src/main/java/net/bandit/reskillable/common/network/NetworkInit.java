package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkInit {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("reskillable");

        registrar.playToServer(RequestLevelUp.TYPE, RequestLevelUp.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                RequestLevelUp.handle(payload, player);
            }
        });

        registrar.playToServer(TogglePerk.TYPE, TogglePerk.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                TogglePerk.handle(payload, player);
            }
        });
        registrar.playToServer(RequestGateStatus.TYPE, RequestGateStatus.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer sp) {
                    RequestGateStatus.handle(payload, sp);
                }
            });
        });

//
//        // Client-bound channels must still be registered on the server
//        registrar.playToClient(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC, (payload, context) -> {});
//        registrar.playToClient(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC, (payload, context) -> {});
//        registrar.playToClient(SyncToClient.TYPE, SyncToClient.STREAM_CODEC, (payload, context) -> {});
    }
}

