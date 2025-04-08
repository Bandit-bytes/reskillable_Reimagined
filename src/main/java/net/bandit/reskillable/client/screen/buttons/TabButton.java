package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TabButton extends AbstractWidget {
    private final boolean selected;
    private final TabType type;

    public TabButton(int x, int y, TabType type, boolean selected) {
        super(x, y, 31, 28, Component.literal("Skill"));
        this.type = type;
        this.selected = selected;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        active = !(minecraft.screen instanceof InventoryScreen) || !((InventoryScreen) minecraft.screen).getRecipeBookComponent().isVisible();

        if (active) {
            if (minecraft.screen instanceof InventoryScreen inventoryScreen) {
                int guiLeft = inventoryScreen.getGuiLeft();
                int guiTop = inventoryScreen.getGuiTop();
                setPosition(guiLeft - 28, guiTop + (type == TabType.INVENTORY ? 7 : 36));
            }

            guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), selected ? 31 : 0, 166, width, height);
            guiGraphics.blit(SkillScreen.RESOURCES, getX() + (selected ? 8 : 10), getY() + 6, 240, 128 + type.iconIndex * 16, 16, 16);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && active) {
            onPress();
            return true;
        }
        return false;
    }

    public void onPress() {
        Minecraft minecraft = Minecraft.getInstance();

        switch (type) {
            case INVENTORY:
                if (minecraft.player != null) {
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                }
                break;

            case SKILLS:
                minecraft.setScreen(new SkillScreen());
                break;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public enum TabType {
        INVENTORY(0),
        SKILLS(1);

        public final int iconIndex;

        TabType(int index) {
            iconIndex = index;
        }
    }
}
