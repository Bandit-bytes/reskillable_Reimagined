package net.bandit.reskillable.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class Keybind {
    public static final String RESKILLABLE_CATEGORY = "key.reskillable.category";
    public static final KeyMapping openKey = new KeyMapping("key.skills", KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1), RESKILLABLE_CATEGORY);

    @SubscribeEvent
    public static void keybind(RegisterKeyMappingsEvent event) {
        event.register(KeyBinding.SKILLS_KEY);
    }

}
