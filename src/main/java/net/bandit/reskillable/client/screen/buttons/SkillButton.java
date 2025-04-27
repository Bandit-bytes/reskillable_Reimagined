package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.RequestLevelUp;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SkillButton extends Button {
    private final Skill skill;
    private List<Component> tooltipLines = null;



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
        if (minecraft == null || clientPlayer == null) return;
        Font font = minecraft.font;

        RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

        SkillModel skillModel = SkillModel.get(clientPlayer);
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

        PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        font.drawInBatch(Component.translatable(skill.getDisplayName()), getX() + 25, getY() + 7, 0xFFFFFF, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        font.drawInBatch(Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

        bufferSource.endBatch();

        if (isMouseOver(mouseX, mouseY)) {
            int cost = Configuration.calculateCostForLevel(level);
            int playerTotalXP = getPlayerTotalXP(clientPlayer);

            // Create components for the tooltip
            Component xpComponent;
            if (playerTotalXP >= cost) {
                xpComponent = Component.literal(String.valueOf(playerTotalXP)).withStyle(ChatFormatting.GREEN);
            } else {
                xpComponent = Component.literal(String.valueOf(playerTotalXP)).withStyle(ChatFormatting.RED);
            }

            Component costComponent = Component.literal(String.valueOf(cost));
            Component tooltip = Component.translatable("tooltip.rereskillable.skill_cost", xpComponent, costComponent);

            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(tooltip);
//            if (SkillAttributeBonus.getBySkill(skill) != null) {
//                boolean enabled = skillModel.isPerkEnabled(skill);
//                tooltipLines.add(Component.literal("➤ ")
//                        .append(Component.literal("Right-click: ").withStyle(ChatFormatting.GOLD))
//                        .append(Component.literal(enabled ? "Disable skill perk" : "Enable skill perk").withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)));
//            }
//
//            tooltipLines.add(Component.literal("➤ ")
//                    .append(Component.literal("Left-click: ").withStyle(ChatFormatting.GOLD))
//                    .append(Component.literal("Level up this skill").withStyle(ChatFormatting.AQUA)));
//
//
//            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
//            int tooltipHeight = 10 + tooltipLines.size() * 10; // estimated height (adjust if you have custom fonts)
//            int tooltipY = mouseY;
//            if (mouseY + tooltipHeight > screenHeight) {
//                tooltipY = mouseY - tooltipHeight - 4;
//            }
//
//            guiGraphics.renderTooltip(
//                    Minecraft.getInstance().font,
//                    tooltipLines.stream().map(Component::getVisualOrderText).toList(),
//                    mouseX,
//                    tooltipY
//            );

        }
    }

        private int getPlayerTotalXP (Player player){
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
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !this.isMouseOver(mouseX, mouseY)) return false;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;

        SkillModel model = SkillModel.get(player);

        if (button == 1) { // Right-click
            if (SkillAttributeBonus.getBySkill(skill) != null) {
                model.togglePerk(skill);
                model.updateSkillAttributeBonuses(player);
            }
        } else if (button == 0) { // Left-click
            RequestLevelUp.send(skill);
        }

        return true;

}
    public List<Component> getTooltipLines(Player player) {
        List<Component> lines = new ArrayList<>();
        SkillModel model = SkillModel.get(player);
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

}
