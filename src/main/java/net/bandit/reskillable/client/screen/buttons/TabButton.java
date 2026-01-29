package net.bandit.reskillable.client.screen.buttons;

import net.bandit.reskillable.client.screen.InventoryTabs;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TabButton extends AbstractWidget {

    private final TabType type;

    private int relX;
    private int relY;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public TabButton(int relX, int relY) {
        super(0, 0, 31, 28, Component.literal("Tab"));
        this.type = TabType.SKILLS;
        this.relX = relX;
        this.relY = relY;
    }

    private boolean isSelected() {
        return Minecraft.getInstance().screen instanceof SkillScreen;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();

        active = !(minecraft.screen instanceof InventoryScreen inv)
                || !inv.getRecipeBookComponent().isVisible();

        if (!active) return;

        if (minecraft.screen != null) {
            int guiLeft;
            int guiTop;

            if (minecraft.screen instanceof InventoryScreen inv) {
                guiLeft = inv.getGuiLeft();
                guiTop = inv.getGuiTop();
            } else {
                guiLeft = InventoryTabs.getGuiLeft(minecraft.screen);
                guiTop = InventoryTabs.getGuiTop(minecraft.screen);
            }

            setPosition(guiLeft + relX, guiTop + relY);
        }

        boolean selected = isSelected();

        guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), selected ? 31 : 0, 166, width, height);
        guiGraphics.blit(SkillScreen.RESOURCES, getX() + (selected ? 8 : 10), getY() + 6,
                240, 128 + type.iconIndex * 16, 16, 16);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !isMouseOver(mouseX, mouseY) || button != 0) return false;

        if (Screen.hasShiftDown()) {
            dragging = true;
            dragOffsetX = (int) mouseX - getX();
            dragOffsetY = (int) mouseY - getY();
            return true;
        }

        onPress();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging || button != 0) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) return false;

        int guiLeft;
        int guiTop;

        if (minecraft.screen instanceof InventoryScreen inv) {
            guiLeft = inv.getGuiLeft();
            guiTop = inv.getGuiTop();
        } else {
            guiLeft = InventoryTabs.getGuiLeft(minecraft.screen);
            guiTop = InventoryTabs.getGuiTop(minecraft.screen);
        }

        int newAbsX = (int) mouseX - dragOffsetX;
        int newAbsY = (int) mouseY - dragOffsetY;

        relX = newAbsX - guiLeft;
        relY = newAbsY - guiTop;

        relX = clamp(relX, -80, InventoryTabs.GUI_W + 80);
        relY = clamp(relY, -80, InventoryTabs.GUI_H + 80);

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;

            InventoryTabs.setPosition(relX, relY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void onPress() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen instanceof SkillScreen) {
            if (mc.player != null) {
                mc.setScreen(new InventoryScreen(mc.player));
            }
        } else {
            mc.setScreen(new SkillScreen());
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public enum TabType {
        SKILLS(1);

        public final int iconIndex;
        TabType(int index) { this.iconIndex = index; }
    }
}
