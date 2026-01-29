package net.bandit.reskillable.client.screen;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.TabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "reskillable", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class InventoryTabs {

    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (!Configuration.shouldShowTabButtons()) return;

        var screen = event.getScreen();

        if ((screen instanceof InventoryScreen && !(screen instanceof CreativeModeInventoryScreen))
                || screen instanceof SkillScreen) {

            int guiLeft = (screen.width - 176) / 2;
            int guiTop = (screen.height - 166) / 2;

            int buttonX = guiLeft - 28;
            int tabY = guiTop + 7;

            event.addListener(new TabButton(buttonX, tabY, TabButton.TabType.SKILLS));
        }
    }
}
