package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.network.payload.NotifyWarning;
import net.bandit.reskillable.common.network.payload.RequestGateStatus;
import net.bandit.reskillable.common.network.payload.RequestLevelUp;
import net.bandit.reskillable.common.network.payload.SyncGateStatus;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.bandit.reskillable.common.network.payload.TogglePerk;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkInit {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Reskillable.MOD_ID);

        registrar.playToClient(SyncToClient.TYPE, SyncToClient.STREAM_CODEC);
        registrar.playToClient(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC);
        registrar.playToClient(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC);
        registrar.playToClient(SyncGateStatus.TYPE, SyncGateStatus.STREAM_CODEC);

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
            if (context.player() instanceof ServerPlayer player) {
                RequestGateStatus.handle(payload, player);
            }
        });
    }
}
