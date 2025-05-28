package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.KeyMapping;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (KeyMapping.openKey.consumeClick()) {
            minecraft.setScreen(new SkillScreen());
        }
    }


    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_page", new Overlay());
    }
}
