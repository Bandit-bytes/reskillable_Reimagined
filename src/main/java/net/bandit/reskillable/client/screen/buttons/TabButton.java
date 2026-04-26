package net.bandit.reskillable.client.screen.buttons;

import net.bandit.reskillable.client.screen.SkillScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TabButton extends AbstractWidget {
    private final TabType type;

    public TabButton(int x, int y, TabType type) {
        super(x, y, 31, 28, Component.literal("Skill"));
        this.type = type;
    }

    private boolean isSelected() {
        Minecraft mc = Minecraft.getInstance();
        return (type == TabType.SKILLS) && (mc.screen instanceof SkillScreen);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();

        active = !(minecraft.screen instanceof InventoryScreen)
                || !((InventoryScreen) minecraft.screen).getRecipeBookComponent().isVisible();

        if (!active) return;

        if (minecraft.screen instanceof InventoryScreen inventoryScreen) {
            int guiLeft = inventoryScreen.getGuiLeft();
            int guiTop = inventoryScreen.getGuiTop();
            setPosition(guiLeft - 28, guiTop + 7);
        } else if (minecraft.screen instanceof SkillScreen skillScreen) {
            int guiLeft = (skillScreen.width - 176) / 2;
            int guiTop = (skillScreen.height - 166) / 2;
            setPosition(guiLeft - 28, guiTop + 7);
        }

        boolean selected = isSelected();

        guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), selected ? 31 : 0, 166, width, height);
        guiGraphics.blit(
                SkillScreen.RESOURCES,
                getX() + (selected ? 8 : 10),
                getY() + 6,
                240,
                128 + type.iconIndex * 16,
                16,
                16
        );
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
        Minecraft mc = Minecraft.getInstance();

        if (type == TabType.SKILLS) {
            if (mc.screen instanceof SkillScreen) {
                if (mc.player != null) mc.setScreen(new InventoryScreen(mc.player));
            } else {
                mc.setScreen(new SkillScreen());
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public enum TabType {
        SKILLS(1);

        public final int iconIndex;
        TabType(int index) { iconIndex = index; }
    }

}
