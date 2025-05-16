package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "reskillable", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientNetworkInit {

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("reskillable");

        registrar.playToClient(SyncToClient.TYPE, SyncToClient.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    SyncToClient.handle(payload, player);
                }
            });
        });

        registrar.playToClient(NotifyWarning.TYPE, NotifyWarning.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    NotifyWarning.handle(payload, player);
                }
            });
        });

        registrar.playToClient(SyncSkillConfig.TYPE, SyncSkillConfig.STREAM_CODEC, (payload, context) -> {
            Minecraft.getInstance().execute(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    SyncSkillConfig.handle(payload, player);
                }
            });
        });
    }
}
