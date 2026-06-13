package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.network.payload.NotifyWarning;
import net.bandit.reskillable.common.network.payload.SyncGateStatus;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class ClientNetworkInit {

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SyncToClient.TYPE, (payload, context) -> {
            ClientHandlers.handleSyncToClient(payload);
        });

        event.register(NotifyWarning.TYPE, (payload, context) -> {
            ClientHandlers.handleNotifyWarning(payload);
        });

        event.register(SyncSkillConfig.TYPE, (payload, context) -> {
            ClientHandlers.handleSyncSkillConfig(payload);
        });

        event.register(SyncGateStatus.TYPE, (payload, context) -> {
            SyncGateStatus.handleClient(payload);
        });
    }
}
