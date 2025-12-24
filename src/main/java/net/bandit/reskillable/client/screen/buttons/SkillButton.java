package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.RequestLevelUp;
import net.bandit.reskillable.common.network.TogglePerkPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SkillButton extends Button {
    private final Skill skill;
    private boolean gateBlocked = false;
    private Component gateMissing = null;
    private List<Component> tooltipLines = null;


    public SkillButton(int x, int y, Skill skill) {
        super(new Button.Builder(Component.literal(""), onPress -> RequestLevelUp.send(skill))
                .pos(x, y)
                .size(79, 32));
        this.skill = skill;
    }

    public Skill getSkill() {
        return this.skill;
    }

    public void setGateBlocked(boolean blocked, Component missingList) {
        boolean changed = (this.gateBlocked != blocked) || !java.util.Objects.equals(this.gateMissing, missingList);
        this.gateBlocked = blocked;
        this.gateMissing = missingList;
        if (changed) {
            this.tooltipLines = null;
        }
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
        guiGraphics.drawString(font, Component.translatable(skill.getDisplayName()), getX() + 25, getY() + 7, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false);


        if (gateBlocked) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88000000);
            guiGraphics.drawString(font, "🔒", getX() + width - 12, getY() + 3, 0xFFDCA64A, false);
        }

        if (isMouseOver(mouseX, mouseY)) {
            int cost = Configuration.calculateCostForLevel(level);
            int playerTotalXP = getPlayerTotalXP(clientPlayer);

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
//            int tooltipHeight = 10 + tooltipLines.size() * 10; // estimated height
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.isMouseOver(mouseX, mouseY)) return false;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;

        SkillModel model = SkillModel.get(player);
        int level = model.getSkillLevel(skill);
        int max = Configuration.getMaxLevel();

        // RIGHT CLICK
        if (button == 1) {
            if (SkillAttributeBonus.getBySkill(skill) != null) {
                Reskillable.NETWORK.sendToServer(new TogglePerkPacket(skill));
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
                return true;
            }
            return false;
        }

        // LEFT CLICK
        if (button == 0) {
            if (!Configuration.isSkillLevelingEnabled()) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                return true;
            }

            if (level >= max) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                return true;
            }

            if (gateBlocked) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                return true;
            }

            int cost = Configuration.calculateCostForLevel(level);
            int playerXP = getPlayerTotalXP(player);
            if (!player.isCreative() && playerXP < cost) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                return true;
            }

            RequestLevelUp.send(skill);
            return true;
        }

        return false;
    }

    public List<Component> getTooltipLines(Player player) {
        List<Component> lines = new ArrayList<>();

        SkillModel model = SkillModel.get(player);
        if (model == null) return lines;

        int level = model.getSkillLevel(skill);

        int cost = Configuration.calculateCostForLevel(level);
        int playerXP = getPlayerTotalXP(player);

        Component xp = Component.literal(String.valueOf(playerXP))
                .withStyle(playerXP >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component costC = Component.literal(String.valueOf(cost));

        lines.add(Component.translatable("tooltip.rereskillable.skill_cost", xp, costC));

        if (SkillAttributeBonus.getBySkill(skill) != null) {
            boolean enabled = model.isPerkEnabled(skill);
            lines.add(Component.literal("➤ ")
                    .append(Component.translatable("tooltip.rereskillable.right_click").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable(enabled
                            ? "tooltip.rereskillable.disable_perk"
                            : "tooltip.rereskillable.enable_perk"
                    ).withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }

        if (!Configuration.isSkillLevelingEnabled()) {
            lines.add(Component.translatable("message.reskillable.leveling_disabled").withStyle(ChatFormatting.RED));
            return lines;
        }

        int maxLevel = Configuration.getMaxLevel();
        if (level >= maxLevel) {
            lines.add(Component.translatable("message.reskillable.max_level", maxLevel).withStyle(ChatFormatting.RED));
            return lines;
        }

        if (gateBlocked) {
            boolean shift = Screen.hasShiftDown();

            lines.add(
                    Component.literal("🔒 ")
                            .append(Component.translatable("tooltip.reskillable.locked"))
                            .withStyle(ChatFormatting.RED)
            );

            if (!shift) {
                lines.add(
                        Component.translatable("tooltip.reskillable.hold_shift")
                                .withStyle(ChatFormatting.DARK_GRAY)
                );
                return lines;
            }

            // Shift held → show detailed requirements
            if (gateMissing != null) {
                lines.add(Component.empty());

                for (Component c : gateMissing.getSiblings()) {
                    lines.add(
                            Component.literal("• ")
                                    .append(c.copy())
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
            }

            return lines;
        }


        lines.add(Component.literal("➤ ")
                .append(Component.translatable("tooltip.rereskillable.left_click").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("tooltip.rereskillable.level_up").withStyle(ChatFormatting.AQUA)));

        return lines;
    }

}
