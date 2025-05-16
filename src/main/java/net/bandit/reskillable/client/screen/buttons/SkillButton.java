package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.payload.RequestLevelUp;
import net.bandit.reskillable.common.network.payload.TogglePerk;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkillButton extends Button {
    private final Skill skill;
    private List<Component> tooltipLines = List.of();

    public SkillButton(int x, int y, Skill skill) {
        super(new Builder(Component.literal(""), onPress -> RequestLevelUp.send(skill))
                .pos(x, y)
                .size(79, 32));
        this.skill = skill;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) return;

        LocalPlayer clientPlayer = minecraft.player;
        Font font = minecraft.font;

        RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

        SkillModel skillModel = SkillModel.get(clientPlayer);
        if (skillModel == null) return;

        int level = skillModel.getSkillLevel(skill);
        int maxLevel = Configuration.getMaxLevel();

        int u = ((int) Math.ceil((double) level * 4 / maxLevel) - 1) * 16 + 176;
        int v = skill.index * 16 + 128;

        guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), 176, (level == maxLevel ? 64 : 0) + (isMouseOver(mouseX, mouseY) ? 32 : 0), width, height);
        guiGraphics.blit(SkillScreen.RESOURCES, getX() + 6, getY() + 8, u, v, 16, 16);

        if (!skillModel.isPerkEnabled(skill) && SkillAttributeBonus.getBySkill(skill) != null) {
            int iconX = getX() + width - 10;
            int iconY = getY() + height - 10;
            guiGraphics.drawString(font, "✖", iconX, iconY, 0xFF5555, false);
        }

        guiGraphics.drawString(font, Component.translatable(skill.getDisplayName()), getX() + 25, getY() + 7, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false);

        if (isMouseOver(mouseX, mouseY)) {
            this.tooltipLines = getTooltipLines(clientPlayer);
        } else {
            this.tooltipLines = List.of();
        }
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !this.isMouseOver(mouseX, mouseY)) return false;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;

        SkillModel model = SkillModel.get(player);
        if (model == null) return false;

        if (button == 1) { // Right-click
            if (SkillAttributeBonus.getBySkill(skill) != null) {
                TogglePerk.send(skill);
                player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
            }
        } else if (button == 0) { // Left-click
            RequestLevelUp.send(skill);
        }

        return true;
    }

    public List<Component> getTooltipLines(Player player) {
        SkillModel model = SkillModel.get(player);
        if (model == null) return List.of();

        List<Component> lines = new ArrayList<>();
        int level = model.getSkillLevel(skill);
        int cost = Configuration.calculateCostForLevel(level);
        int playerXP = getPlayerTotalXP(player);

        Component xp = Component.literal(String.valueOf(playerXP)).withStyle(playerXP >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component costC = Component.literal(String.valueOf(cost));
        lines.add(Component.translatable("tooltip.rereskillable.skill_cost", xp, costC));

        if (SkillAttributeBonus.getBySkill(skill) != null) {
            boolean enabled = model.isPerkEnabled(skill);
            lines.add(Component.literal("➤ ")
                    .append(Component.literal("Right-click: ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(enabled ? "Disable skill perk" : "Enable skill perk").withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }

        lines.add(Component.literal("➤ ")
                .append(Component.literal("Left-click: ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("Level up this skill").withStyle(ChatFormatting.AQUA)));

        return lines;
    }

    private int getPlayerTotalXP(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        if (level <= 16) {
            return (level * level) + (6 * level) + Math.round(progress * (2 * level + 7));
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360) + Math.round(progress * (5 * level - 38));
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220) + Math.round(progress * (9 * level - 158));
        }
    }

    public List<Component> getCurrentTooltipLines() {
        return tooltipLines;
    }
}
