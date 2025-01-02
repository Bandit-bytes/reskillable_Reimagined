package net.bandit.reskillable.client;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Keybind {
    public static final String RESKILLABLE_CATEGORY = "key.reskillable.category";
    public static final KeyMapping openKey = new KeyMapping("key.skills", KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1), RESKILLABLE_CATEGORY);

    @SubscribeEvent
    public static void keybind(RegisterKeyMappingsEvent event) {
        event.register(KeyBinding.SKILLS_KEY);
    }
}
