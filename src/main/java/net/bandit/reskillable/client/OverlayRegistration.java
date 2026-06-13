package net.bandit.reskillable.client;

import net.bandit.reskillable.Reskillable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class OverlayRegistration {
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Overlay.ID, Overlay.INSTANCE::extractRenderState);
        NeoForge.EVENT_BUS.register(Overlay.INSTANCE);
    }
}
