package net.bandit.reskillable.client;

import net.bandit.reskillable.Reskillable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class OverlayRegistration {
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Overlay.ID, Overlay.INSTANCE);
    }
}
