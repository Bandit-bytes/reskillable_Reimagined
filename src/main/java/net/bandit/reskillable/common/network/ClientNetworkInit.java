package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.network.payload.*;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "reskillable", bus = EventBusSubscriber.Bus.MOD)
public class ClientNetworkInit {

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("reskillable");

        registrar.playToClient(SyncToClient.TYPE, SyncToClient.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> ClientHandlers.handleSyncToClient(payload));
        });

        registrar.playToClient(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> ClientHandlers.handleNotifyWarning(payload));
        });

        registrar.playToClient(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> ClientHandlers.handleSyncSkillConfig(payload));
        });
        registrar.playToClient(
                SyncGateStatus.TYPE,
                SyncGateStatus.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> SyncGateStatus.handleClient(payload));
                }
        );

    }
}
