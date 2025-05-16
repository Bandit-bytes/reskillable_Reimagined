package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkInit {

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("reskillable");

        // Server-side handlers only
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
    }
}
