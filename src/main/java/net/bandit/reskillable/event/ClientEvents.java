package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.KeyMapping;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Screen screen = mc.screen;
        if (screen != null) {
            GuiEventListener focused = screen.getFocused();
            if (focused instanceof EditBox) return;
        }
        if (KeyMapping.openKey.consumeClick()) {
            mc.setScreen(new SkillScreen());
        }
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_page", new Overlay());
    }
}
