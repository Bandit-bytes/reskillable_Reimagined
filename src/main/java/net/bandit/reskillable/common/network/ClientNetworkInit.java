package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworkInit {
    private ClientNetworkInit() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SyncToClient.TYPE, (payload, context) ->
                context.client().execute(() -> ClientHandlers.handleSyncToClient(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NotifyWarning.TYPE, (payload, context) ->
                context.client().execute(() -> ClientHandlers.handleNotifyWarning(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncSkillConfig.TYPE, (payload, context) ->
                context.client().execute(() -> ClientHandlers.handleSyncSkillConfig(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SyncGateStatus.TYPE, (payload, context) ->
                context.client().execute(() -> SyncGateStatus.handleClient(payload)));
    }

    public static void sendLevelUp(String skillId) {
        ClientPlayNetworking.send(new RequestLevelUp(skillId == null ? "" : skillId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public static void sendTogglePerk(String skillId) {
        ClientPlayNetworking.send(new TogglePerk(skillId == null ? "" : skillId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public static void requestGateStatus() {
        ClientPlayNetworking.send(new RequestGateStatus());
    }
}
