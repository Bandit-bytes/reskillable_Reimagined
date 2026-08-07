package net.bandit.reskillable.client.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.InventoryTabs;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.function.Consumer;

/**
 * EMI integration for Reskillable's movable Inventory/Skills tab.
 * Keeps EMI's sidebars from covering the tab and teaches EMI the bounds of
 * the custom SkillScreen so its sidebars can lay out around the GUI.
 */
public final class ReskillableEmiPlugin implements EmiPlugin {
    private static final int TAB_WIDTH = 31;
    private static final int TAB_HEIGHT = 28;

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(InventoryScreen.class, this::addTabExclusion);
        registry.addExclusionArea(SkillScreen.class, this::addTabExclusion);

        registry.addScreenBoundsProvider(SkillScreen.class, screen -> new Bounds(
                InventoryTabs.getGuiLeft(screen),
                InventoryTabs.getGuiTop(screen),
                InventoryTabs.GUI_W,
                InventoryTabs.GUI_H
        ));
    }

    private <T extends Screen> void addTabExclusion(T screen, Consumer<Bounds> consumer) {
        if (!Configuration.shouldShowTabButtons()) return;
        if (screen instanceof InventoryScreen inventoryScreen
                && inventoryScreen.getRecipeBookComponent().isVisible()) return;

        InventoryTabs.Pos pos = InventoryTabs.getPosition();
        consumer.accept(new Bounds(
                InventoryTabs.getGuiLeft(screen) + pos.x(),
                InventoryTabs.getGuiTop(screen) + pos.y(),
                TAB_WIDTH,
                TAB_HEIGHT
        ));
    }
}
