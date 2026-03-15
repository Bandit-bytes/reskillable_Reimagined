package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.RequestLevelUp;
import net.bandit.reskillable.common.network.payload.TogglePerk;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SkillButton extends Button {
    private static final ResourceLocation LOCK_ICON =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/lock_icon.png");

    private final String skillId;
    private boolean gateBlocked = false;
    private Component gateMissing = null;
    private List<Component> tooltipLines = null;

    public SkillButton(int x, int y, String skillId) {
        super(new Button.Builder(Component.literal(""), onPress -> RequestLevelUp.send(skillId))
                .pos(x, y)
                .size(79, 32));
        this.skillId = normalizeSkillId(skillId);
    }

    public String getSkillId() {
        return this.skillId;
    }

    public void setGateBlocked(boolean blocked, Component missingList) {
        boolean changed = (this.gateBlocked != blocked) || !Objects.equals(this.gateMissing, missingList);
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

        SkillModel skillModel = SkillModel.get(clientPlayer);
        if (skillModel == null) return;

        Font font = minecraft.font;

        RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

        int level = skillModel.getSkillLevel(skillId);
        int maxLevel = Configuration.getMaxLevel();

        int u = getSkillIconU(level, maxLevel);
        int v = getSkillIconV();

        guiGraphics.blit(
                SkillScreen.RESOURCES,
                getX(),
                getY(),
                176,
                (level == maxLevel ? 64 : 0) + (isMouseOver(mouseX, mouseY) ? 32 : 0),
                width,
                height
        );

        guiGraphics.blit(SkillScreen.RESOURCES, getX() + 6, getY() + 8, u, v, 16, 16);

        if (hasBuiltInPerk() && !skillModel.isPerkEnabled(skillId)) {
            int iconX = getX() + width - 10;
            int iconY = getY() + height - 10;
            guiGraphics.drawString(font, "✖", iconX, iconY, 0xFF5555, false);
        }

        guiGraphics.drawString(font, getDisplayNameComponent(), getX() + 25, getY() + 7, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false);

        if (gateBlocked) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88000000);

            RenderSystem.setShaderTexture(0, LOCK_ICON);

            int iconSize = 16;
            int iconX = getX() + width - iconSize - 3;
            int iconY = getY() + 3;
            guiGraphics.blit(LOCK_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

            RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);
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
        if (model == null) return false;

        int level = model.getSkillLevel(skillId);
        int max = Configuration.getMaxLevel();

        if (button == 1) {
            if (hasBuiltInPerk()) {
                TogglePerk.send(skillId);
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
                return true;
            }
            return false;
        }

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

            RequestLevelUp.send(skillId);
            return true;
        }

        return false;
    }

    public List<Component> getTooltipLines(Player player) {
        List<Component> lines = new ArrayList<>();

        SkillModel model = SkillModel.get(player);
        if (model == null) return lines;

        int level = model.getSkillLevel(skillId);

        int cost = Configuration.calculateCostForLevel(level);
        int playerXP = getPlayerTotalXP(player);

        Component xp = Component.literal(String.valueOf(playerXP))
                .withStyle(playerXP >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component costC = Component.literal(String.valueOf(cost));

        lines.add(Component.translatable("tooltip.rereskillable.skill_cost", xp, costC));

        if (hasBuiltInPerk()) {
            boolean enabled = model.isPerkEnabled(skillId);
            lines.add(Component.literal("➤ ")
                    .append(Component.translatable("tooltip.rereskillable.right_click").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable(
                            enabled
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

    private Component getDisplayNameComponent() {
        Skill vanilla = getVanillaSkillOrNull();
        if (vanilla != null) {
            return Component.translatable(vanilla.getDisplayName());
        }

        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(skillId);
        if (custom != null && custom.displayName != null && !custom.displayName.isBlank()) {
            return Component.literal(custom.displayName);
        }

        return Component.literal(skillId);
    }

    private int getSkillIconU(int level, int maxLevel) {
        int filledStep = Math.max(0, (int) Math.ceil((double) level * 4 / maxLevel) - 1);
        return filledStep * 16 + 176;
    }

    private int getSkillIconV() {
        Skill vanilla = getVanillaSkillOrNull();
        if (vanilla != null) {
            return vanilla.index * 16 + 128;
        }

        List<Configuration.CustomSkillSlot> customSkills = Configuration.getCustomSkills();
        for (int i = 0; i < customSkills.size(); i++) {
            Configuration.CustomSkillSlot slot = customSkills.get(i);
            if (slot != null && normalizeSkillId(slot.id).equals(skillId)) {
                return i * 16 + 128;
            }
        }

        return 128;
    }

    private boolean hasBuiltInPerk() {
        Skill vanilla = getVanillaSkillOrNull();
        return vanilla != null && SkillAttributeBonus.getBySkill(vanilla) != null;
    }

    private Skill getVanillaSkillOrNull() {
        try {
            return Skill.valueOf(skillId.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }
}