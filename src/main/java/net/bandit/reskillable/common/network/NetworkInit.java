package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class NetworkInit {
    private NetworkInit() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestLevelUp.TYPE, RequestLevelUp.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePerk.TYPE, TogglePerk.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestGateStatus.TYPE, RequestGateStatus.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(SyncToClient.TYPE, SyncToClient.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGateStatus.TYPE, SyncGateStatus.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestLevelUp.TYPE, (payload, context) ->
                context.server().execute(() -> RequestLevelUp.handle(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(TogglePerk.TYPE, (payload, context) ->
                context.server().execute(() -> TogglePerk.handle(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(RequestGateStatus.TYPE, (payload, context) ->
                context.server().execute(() -> RequestGateStatus.handle(payload, context.player())));
    }
}
