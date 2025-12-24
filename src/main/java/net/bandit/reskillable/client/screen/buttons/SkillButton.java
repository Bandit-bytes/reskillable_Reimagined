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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.gui.screens.Screen;


import java.util.ArrayList;
import java.util.List;

public class SkillButton extends Button {
    private final Skill skill;

    private boolean gateBlocked = false;
    private Component gateMissing = null;
    private List<Component> tooltipLines = List.of();

    public SkillButton(int x, int y, Skill skill) {
        super(new Builder(Component.literal(""), onPress -> RequestLevelUp.send(skill))
                .pos(x, y)
                .size(79, 32));
        this.skill = skill;
    }

    public Skill getSkill() {
        return this.skill;
    }

    public void setGateBlocked(boolean blocked, Component missingList) {
        this.gateBlocked = blocked;
        this.gateMissing = missingList;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer clientPlayer = mc.player;
        if (clientPlayer == null) return;

        SkillModel model = SkillModel.get(clientPlayer);
        if (model == null) return;

        Font font = mc.font;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

        int level = model.getSkillLevel(skill);
        int maxLevel = Configuration.getMaxLevel();

        int u = ((int) Math.ceil((double) level * 4 / maxLevel) - 1) * 16 + 176;
        int v = skill.index * 16 + 128;

        g.blit(SkillScreen.RESOURCES, getX(), getY(),
                176,
                (level == maxLevel ? 64 : 0) + (isMouseOver(mouseX, mouseY) ? 32 : 0),
                width, height);

        g.blit(SkillScreen.RESOURCES, getX() + 6, getY() + 8, u, v, 16, 16);
        if (!model.isPerkEnabled(skill) && SkillAttributeBonus.getBySkill(skill) != null) {
            g.drawString(font, "✖", getX() + width - 10, getY() + height - 10, 0xFF5555, false);
        }

        g.drawString(font, Component.translatable(skill.getDisplayName()), getX() + 25, getY() + 7, 0xFFFFFF, false);
        g.drawString(font, Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false);

        if (gateBlocked) {
            g.fill(getX(), getY(), getX() + width, getY() + height, 0x88000000);
            g.drawString(font, "🔒", getX() + width - 12, getY() + 3, 0xFFDCA64A, false);
        }

        // Cache tooltip lines when hovered (optional convenience)
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

        if (button == 1) {
            if (SkillAttributeBonus.getBySkill(skill) != null) {
                TogglePerk.send(skill);
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
            }
            return true;
        }

        if (button == 0) {

            if (gateBlocked) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                return true;
            }

            int level = model.getSkillLevel(skill);
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
        SkillModel model = SkillModel.get(player);
        if (model == null) return List.of();

        List<Component> lines = new ArrayList<>();

        int level = model.getSkillLevel(skill);
        int cost = Configuration.calculateCostForLevel(level);
        int playerXP = getPlayerTotalXP(player);

        Component xp = Component.literal(String.valueOf(playerXP))
                .withStyle(playerXP >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component costC = Component.literal(String.valueOf(cost));

        lines.add(Component.translatable("tooltip.rereskillable.skill_cost", xp, costC));
        if (!Configuration.isSkillLevelingEnabled()) {
            lines.add(Component.translatable("message.reskillable.leveling_disabled").withStyle(ChatFormatting.RED));
            return lines;
        }

        int max = Configuration.getMaxLevel();
        if (level >= max) {
            lines.add(Component.translatable("message.reskillable.max_level", max).withStyle(ChatFormatting.RED));
            return lines;
        }

        if (gateBlocked) {
            lines.add(
                    Component.literal("🔒 ")
                            .append(Component.translatable("message.reskillable.gate_blocked_short"))
                            .withStyle(ChatFormatting.RED)
            );

            if (!Screen.hasShiftDown()) {
                lines.add(
                        Component.literal("⇧ ")
                                .append(Component.translatable("message.reskillable.gate_hold_shift"))
                                .withStyle(ChatFormatting.DARK_GRAY)
                );
            } else if (gateMissing != null) {
                // Expanded requirements
                lines.add(Component.empty());

                lines.add(
                        Component.translatable("message.reskillable.gate_requirements")
                                .withStyle(ChatFormatting.GOLD)
                );

                for (Component part : splitRequirements(gateMissing)) {
                    lines.add(
                            Component.literal(" • ")
                                    .append(part)
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
            }

            lines.add(Component.literal("────────────────").withStyle(ChatFormatting.DARK_GRAY));
        }



        if (SkillAttributeBonus.getBySkill(skill) != null) {
            boolean enabled = model.isPerkEnabled(skill);
            lines.add(Component.literal("➤ ")
                    .append(Component.translatable("tooltip.rereskillable.right_click").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable(
                            enabled ? "tooltip.rereskillable.disable_perk" : "tooltip.rereskillable.enable_perk"
                    ).withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }
        lines.add(Component.literal("➤ ")
                .append(Component.translatable("tooltip.rereskillable.left_click").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("tooltip.rereskillable.level_up").withStyle(ChatFormatting.AQUA)));

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
    private List<Component> splitRequirements(Component combined) {
        String raw = combined.getString();
        String[] parts = raw.split(",\\s*");

        List<Component> out = new ArrayList<>();
        for (String p : parts) {
            out.add(Component.literal(p));
        }
        return out;
    }


    public List<Component> getCurrentTooltipLines() {
        return tooltipLines;
    }
}
