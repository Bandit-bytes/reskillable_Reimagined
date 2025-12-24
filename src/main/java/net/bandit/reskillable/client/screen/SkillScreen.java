package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.network.payload.RequestGateStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import net.bandit.reskillable.common.gating.SkillLevelGate;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillScreen extends Screen {
    public static final ResourceLocation RESOURCES =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/skills.png");
    public static final ResourceLocation PERKS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/perks.png");

    private static final int PERK_BOX_X = 12;
    private static final int PERK_BOX_Y = 29;
    private static final int PERK_ROW_HEIGHT = 15;
    private static final int PERK_TEXT_OFFSET_X = 15;
    private boolean requestedGateSync = false;

    private final Map<Skill, String> xpCostDisplay = new HashMap<>();
    private final Map<Skill, Integer> xpCostColor = new HashMap<>();

    private int page = 0;
    private Button skillsTab;
    private Button perksTab;

    public SkillScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();

        int left = (width - 162) / 2;
        int top  = (height - 128) / 2;

        this.clearWidgets();

        if (page == 0) {
            GateClientCache.clear();
            RequestGateStatus.send();

            for (int i = 0; i < 8; i++) {
                int x = left + i % 2 * 83;
                int y = top + i / 2 * 36;
                Skill skill = Skill.values()[i];
                addRenderableWidget(new SkillButton(x, y, skill));
            }
        }

        int guiLeft = (width - 176) / 2;
        int guiTop = (height - 166) / 2;

        int tabWidth = 45;
        int tabHeight = 17;

        skillsTab = new TabButton(
                guiLeft + 11,
                guiTop + 4,
                tabWidth,
                tabHeight,
                b -> {
                    if (page != 0) {
                        page = 0;
                        init(minecraft, width, height);
                    }
                }
        );

        // Right tab: PERKS
        perksTab = new TabButton(
                guiLeft + 176 - 11 - tabWidth,
                guiTop + 4,
                tabWidth,
                tabHeight,
                b -> {
                    if (page != 1) {
                        page = 1;
                        init(minecraft, width, height);
                    }
                }
        );

        addRenderableWidget(skillsTab);
        addRenderableWidget(perksTab);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int panelWidth = 176;
        int panelHeight = 166;

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        // guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0x99000000);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        this.renderTransparentBackground(g);

        int left = (width - 176) / 2;
        int top  = (height - 166) / 2;

        renderBackground(g, mouseX, mouseY, pt);

        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        ResourceLocation background = (page == 0) ? RESOURCES : PERKS_TEXTURE;
        g.blit(background, left, top, 0, 0, 176, 166);
        RenderSystem.defaultBlendFunc();

        if (page == 0) {
            int i = 0;
            for (Skill skill : Skill.values()) {
                int x = left + (i % 2) * 83 + 10;
                int y = top  + (i / 2) * 36 + 20;

                String xpCost = xpCostDisplay.getOrDefault(skill, "N/A");
                int color     = xpCostColor.getOrDefault(skill, 0xFFFFFF);
                g.drawString(font, "XP: " + xpCost, x, y, color, false);
                i++;
            }
        } else {
            renderPerksPage(g, left, top);
        }

        super.render(g, mouseX, mouseY, pt);

        if (page == 0) {
            for (var child : this.children()) {
                if (child instanceof SkillButton button && button.isMouseOver(mouseX, mouseY)) {
                    var lines = button.getTooltipLines(Minecraft.getInstance().player);
                    g.renderTooltip(
                            this.font,
                            lines.stream().map(Component::getVisualOrderText).toList(),
                            mouseX, mouseY
                    );
                    break; // don't render multiple tooltips
                }
            }
        }


        g.setColor(1f, 1f, 1f, 1f);
    }

    private void renderPerksPage(GuiGraphics gui, int left, int top) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int row = 0;

        for (Skill skill : Skill.values()) {
            int skillLevel = model.getSkillLevel(skill);

            int boxX = left + PERK_BOX_X;
            int boxY = top + PERK_BOX_Y + (row * PERK_ROW_HEIGHT);

            int textX = boxX + PERK_TEXT_OFFSET_X;
            int textY = boxY + 5;

            Component line = buildSinglePerkLine(skill, skillLevel);

            float scale = 0.85f;

            gui.pose().pushPose();
            gui.pose().translate(textX, textY, 0);
            gui.pose().scale(scale, scale, 1.0F);
            gui.drawString(this.font, line, 0, 0, 0xFFFFFF, false);
            gui.pose().popPose();

            row++;
        }
    }
    private Component buildSinglePerkLine(Skill skill, int skillLevel) {
        Component skillName = Component.translatable("skill." + skill.name().toLowerCase())
                .withStyle(ChatFormatting.GOLD);

        boolean locked = skillLevel < 5;

        Component amount;
        Component effect;

        switch (skill) {
            case AGILITY -> {
                int steps = skillLevel / 5;
                double perStep = Configuration.MOVEMENT_SPEED_BONUS.get();
                double pct = steps * perStep * 100.0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.run_speed")
                        .withStyle(ChatFormatting.GRAY);
            }
            case MINING -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.break_speed")
                        .withStyle(ChatFormatting.GRAY);
            }
            case GATHERING -> {
                double pct = skillLevel >= 5
                        ? (skillLevel / 5.0) * Configuration.GATHERING_XP_BONUS.get() * 100.0
                        : 0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.bonus_xp_orbs")
                        .withStyle(ChatFormatting.GRAY);
            }
            case FARMING -> {
                double perStep = Configuration.CROP_GROWTH_CHANCE.get();
                double pct = skillLevel >= 5
                        ? (skillLevel / 5.0) * perStep * 100.0
                        : 0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.crop_growth")
                        .withStyle(ChatFormatting.GRAY);
            }
            case BUILDING -> {
                double perStep = Configuration.BLOCK_REACH_BONUS.get();
                double blocks = skillLevel >= 5
                        ? (skillLevel / 5.0) * perStep
                        : 0;
                amount = Component.literal(String.format("+%.1f", blocks))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.block_reach")
                        .withStyle(ChatFormatting.GRAY);
            }

            default -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                if (bonus == null) {
                    return Component.empty();
                }

                double perStep = bonus.getBonusPerStep();
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                Attribute attr = bonus.getAttribute();
                effect = (attr != null)
                        ? Component.translatable(attr.getDescriptionId()).withStyle(ChatFormatting.GRAY)
                        : Component.empty();
            }
        }

        MutableComponent line = Component.literal("")
                .append(skillName)
                .append(": ")
                .append(amount)
                .append(" ")
                .append(effect);

        return line;
    }



    private void renderTotalXPTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int totalXP = calculateTotalXP(player);
            int level = getLevelForTotalXP(totalXP);

            Component totalXPComponent = Component.literal(String.valueOf(totalXP)).withStyle(ChatFormatting.GREEN);
            Component levelComponent = Component.literal(String.valueOf(level));
            Component tooltip = Component.translatable("tooltip.rereskillable.total_xp", totalXPComponent, levelComponent);

            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(tooltip);
            tooltipLines.add(Component.literal(" "));

            SkillModel model = SkillModel.get(player);
            if (model != null) {
                for (Skill skill : Skill.values()) {
                    int skillLevel = model.getSkillLevel(skill);

                    var bonus = SkillAttributeBonus.getBySkill(skill);
                    Attribute attribute = bonus != null ? bonus.getAttribute() : null;

                    if (bonus != null && attribute != null && skillLevel >= 5) {
                        double amount = (skillLevel / 5.0) * bonus.getBonusPerStep();

                        if (amount > 0) {
                            Component skillName = Component.translatable("skill." + skill.name().toLowerCase()).withStyle(ChatFormatting.GOLD);
                            Component attrName = Component.translatable(attribute.getDescriptionId()).withStyle(ChatFormatting.GRAY);

                            String percentText = String.format("+%.0f%%", amount * 100);
                            Component bonusLine = Component.literal("")
                                    .append(skillName)
                                    .append(": ")
                                    .append(Component.literal(percentText).withStyle(ChatFormatting.AQUA))
                                    .append(" ")
                                    .append(attrName);

                            tooltipLines.add(bonusLine);
                        }
                    }
                }
            }
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                    mouseX,
                    mouseY
            );
        }
    }

    private int calculateTotalXP(Player player) {
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

    private int getLevelForTotalXP(int totalXP) {
        int level = 0;
        while (getCumulativeXPForLevel(level + 1) <= totalXP) {
            level++;
        }
        return level;
    }

    private int getCumulativeXPForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return (level * (level + 1)) + (level * 6);
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        boolean levelingEnabled = Configuration.isSkillLevelingEnabled();
        int max = Configuration.getMaxLevel();

        int playerTotalXp = calculateTotalXP(player); // reuse your existing helper

        for (var widget : this.renderables) {
            if (!(widget instanceof SkillButton btn)) continue;

            Skill skill = btn.getSkill();
            int level = model.getSkillLevel(skill);

            int cost = Configuration.calculateCostForLevel(level); // cost to go from level -> level+1
            boolean hasXP = player.isCreative() || playerTotalXp >= cost;

            xpCostDisplay.put(skill, String.valueOf(cost));
            xpCostColor.put(skill, hasXP ? 0x00FF00 : 0xFF0000);

            boolean maxed = level >= max;

            var cached = GateClientCache.get(skill);
            boolean blockedByGate = levelingEnabled && !maxed && cached != null && cached.blocked();

            btn.active = true;
            btn.setGateBlocked(
                    blockedByGate,
                    blockedByGate && cached != null ? cached.missingList()
                            : null
            );
        }
    }

    private static class TabButton extends Button {
        public TabButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // no vanilla background/text; GUI textures handle appearance
        }
    }
}
