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
        if (!Configuration.shouldShowTabButtons()) {
            return; // Do nothing if tab buttons are disabled
        }

        var screen = event.getScreen();

        // Check if the current screen is Inventory, Creative, or Skill screen
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen || screen instanceof SkillScreen) {
            boolean isCreative = screen instanceof CreativeModeInventoryScreen;
            boolean isSkillsOpen = screen instanceof SkillScreen;

            // Calculate positions for the tabs
            int x = (screen.width - (isCreative ? 195 : 176)) / 2 - 28; // Left of the GUI
            int y = (screen.height - (isCreative ? 136 : 166)) / 2;    // Center vertically

            // Add Inventory tab
            event.addListener(new TabButton(x, y + 7, TabButton.TabType.INVENTORY, !isSkillsOpen) {
                @Override
                public void onPress() {
                    Minecraft.getInstance().setScreen(new InventoryScreen(Minecraft.getInstance().player));
                }
            });

            // Add Skills tab
            event.addListener(new TabButton(x, y + 36, TabButton.TabType.SKILLS, isSkillsOpen) {
                @Override
                public void onPress() {
                    Minecraft.getInstance().setScreen(new SkillScreen());
                }
            });
        }
    }
}
