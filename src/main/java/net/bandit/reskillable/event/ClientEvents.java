package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (KeyBinding.SKILLS_KEY.consumeClick()) {
            minecraft.setScreen(new SkillScreen());
        }
    }
}
