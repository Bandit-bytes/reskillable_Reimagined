package net.bandit.reskillable.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.bandit.reskillable.Reskillable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyMapping {
    public static final String RESKILLABLE_CATEGORY = "key." + Reskillable.MOD_ID + ".category";
    public static final net.minecraft.client.KeyMapping openKey = new net.minecraft.client.KeyMapping("key." + Reskillable.MOD_ID + ".open_skills", KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1), RESKILLABLE_CATEGORY);

    @SubscribeEvent
    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(openKey);
    }
}
