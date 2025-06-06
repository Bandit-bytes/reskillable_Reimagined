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
            return;
        }

        var screen = event.getScreen();

        if (screen instanceof InventoryScreen && !(screen instanceof CreativeModeInventoryScreen) || screen instanceof SkillScreen) {
            boolean isSkillsOpen = screen instanceof SkillScreen;

            int guiLeft = (screen.width - 176) / 2; // Default GUI width
            int guiTop = (screen.height - 166) / 2;
            int buttonX = guiLeft - 28;
            int inventoryTabY = guiTop + 7;
            int skillsTabY = guiTop + 36;

            // Add Inventory tab
            event.addListener(new TabButton(buttonX, inventoryTabY, TabButton.TabType.INVENTORY, !isSkillsOpen) {
                @Override
                public void onPress() {
                    if (Minecraft.getInstance().player != null)
                        Minecraft.getInstance().setScreen(new InventoryScreen(Minecraft.getInstance().player));
                }
            });

            event.addListener(new TabButton(buttonX, skillsTabY, TabButton.TabType.SKILLS, isSkillsOpen) {
                @Override
                public void onPress() {
                    Minecraft.getInstance().setScreen(new SkillScreen());
                }
            });
        }
    }
}
