package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.network.RequestLevelUp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class SkillButton extends Button {
    private final Skill skill;

    public SkillButton(int x, int y, Skill skill) {
        super(new Button.Builder(Component.literal(""), onPress -> RequestLevelUp.send(skill))
                .pos(x, y)
                .size(79, 32));
        this.skill = skill;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayer = minecraft.player;

        // Ensure client environment and valid player instance
        if (minecraft == null || clientPlayer == null) return;

        RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

        SkillModel skillModel = SkillModel.get(clientPlayer);
        int level = skillModel.getSkillLevel(skill);
        int maxLevel = Configuration.getMaxLevel();

        int u = ((int) Math.ceil((double) level * 4 / maxLevel) - 1) * 16 + 176;
        int v = skill.index * 16 + 128;

        // Render button background and skill icon
        guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), 176, (level == maxLevel ? 64 : 0) + (isMouseOver(mouseX, mouseY) ? 32 : 0), width, height);
        guiGraphics.blit(SkillScreen.RESOURCES, getX() + 6, getY() + 8, u, v, 16, 16);

        // Render skill text
        PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        Font font = minecraft.font;

        font.drawInBatch(Component.translatable(skill.getDisplayName()), getX() + 25, getY() + 7, 0xFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        font.drawInBatch(Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

        bufferSource.endBatch();

//        // Render XP cost if applicable
//        if (isMouseOver(mouseX, mouseY) && level < maxLevel) {
//            int cost = Configuration.calculateCostForLevel(level);
//            int playerTotalXP = getPlayerTotalXP(clientPlayer);
//
//            int color = playerTotalXP >= cost ? 0x7EFC20 : 0xFC5454;
//            String text = Integer.toString(cost);
//
//            int digitOffset = switch (text.length()) {
//                case 1, 2 -> 4;
//                case 3 -> 2;
//                default -> 0;
//            };
//
//            int textX = getX() + 73 - font.width(text) + 7 - digitOffset;
//            if (textX < getX() + 25) {
//                textX = getX() + 25;
//            }
//
//            poseStack.pushPose();
//            float scale = 0.9f;
//            poseStack.translate(textX, getY() + 18, 0);
//            poseStack.scale(scale, scale, 1.0f);
//            guiGraphics.drawString(font, text, 0, 0, color, false);
//            poseStack.popPose();
//        }
//    }
        // Render Tooltip Only
        if (isMouseOver(mouseX, mouseY)) {
            int cost = Configuration.calculateCostForLevel(level);
            int playerTotalXP = getPlayerTotalXP(clientPlayer);

            // Determine tooltip text based on player's XP
            String tooltipText;
            if (playerTotalXP >= cost) {
                tooltipText = Component.translatable("tooltip.rereskillable.skill_cost_available", cost).getString();
            } else {
                tooltipText = Component.translatable("tooltip.rereskillable.skill_cost_unavailable", cost).getString();
            }

            // Render the tooltip
            guiGraphics.renderTooltip(minecraft.font, Component.literal(tooltipText), mouseX, mouseY);
        }
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

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
