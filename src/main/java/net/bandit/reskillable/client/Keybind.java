package net.bandit.reskillable.client;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class Keybind {
    @SubscribeEvent
    public static void keybind(RegisterKeyMappingsEvent event) {
        event.register(KeyBinding.SKILLS_KEY);
    }
}
