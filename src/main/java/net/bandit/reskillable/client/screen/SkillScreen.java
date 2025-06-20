package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.KeyMapping;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillScreen extends Screen {
    public static final ResourceLocation RESOURCES = new ResourceLocation("reskillable", "textures/gui/skills.png");
    private final Map<Skill, String> xpCostDisplay = new HashMap<>();
    private final Map<Skill, Integer> xpCostColor = new HashMap<>();


    public SkillScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int left = (width - 162) / 2;
        int top = (height - 128) / 2;

        for (int i = 0; i < 8; i++) {
            int x = left + i % 2 * 83;
            int y = top + i / 2 * 36;
            Skill skill = Skill.values()[i];

            addRenderableWidget(new SkillButton(x, y, skill));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderTexture(0, RESOURCES);

        int left = (width - 176) / 2;
        int top = (height - 166) / 2;

        renderBackground(guiGraphics);
        guiGraphics.blit(RESOURCES, left, top, 0, 0, 176, 166);

        int i = 0;
        for (Skill skill : Skill.values()) {
            int x = left + (i % 2) * 83 + 10;
            int y = top + (i / 2) * 36 + 20;

            String xpCost = xpCostDisplay.getOrDefault(skill, "N/A");
            int color = xpCostColor.getOrDefault(skill, 0xFFFFFF);

            guiGraphics.drawString(font, "XP: " + xpCost, x, y, color, false);
            i++;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int labelX = width / 2 - font.width("Skills") / 2;
        int labelY = top + 6;
        if (mouseX > labelX && mouseX < labelX + font.width("Skills") && mouseY > labelY && mouseY < labelY + font.lineHeight) {
            renderTotalXPTooltip(guiGraphics, mouseX, mouseY);
        }

        for (var widget : this.renderables) {
            if (widget instanceof SkillButton button && button.isMouseOver(mouseX, mouseY)) {
                var tooltipLines = button.getTooltipLines(Minecraft.getInstance().player);
                guiGraphics.renderTooltip(
                        font,
                        tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                        mouseX,
                        mouseY
                );
            }
        }

    }

    private void renderTotalXPTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int totalXP = calculateTotalXP(player);
        int level = getLevelForTotalXP(totalXP);

        Component header = Component.translatable(
                "tooltip.rereskillable.total_xp",
                Component.literal(String.valueOf(totalXP)).withStyle(ChatFormatting.GREEN),
                Component.literal(String.valueOf(level))
        );

        List<Component> lines = new ArrayList<>();
        lines.add(header);
        lines.add(Component.empty());

        SkillModel model = SkillModel.get(player);
        if (model != null) {
            for (Skill skill : Skill.values()) {
                int skillLevel = model.getSkillLevel(skill);
                Component skillName = Component.translatable("skill." + skill.name().toLowerCase())
                        .withStyle(ChatFormatting.GOLD);

                var bonus = SkillAttributeBonus.getBySkill(skill);
                Attribute attr = bonus != null ? bonus.getAttribute() : null;

                /* ---- 2a · Generic attribute-driven bonus ---- */
                if (bonus != null && attr != null && skillLevel >= 5) {
                    double pct = (skillLevel / 5.0) * bonus.getBonusPerStep() * 100.0;
                    if (pct > 0) {
                        Component amount = Component.literal(String.format("+%.0f%%", pct))
                                .withStyle(ChatFormatting.AQUA);
                        Component attrName = Component.translatable(attr.getDescriptionId())
                                .withStyle(ChatFormatting.GRAY);

                        lines.add(Component.literal("")
                                .append(skillName).append(": ").append(amount).append(" ").append(attrName));
                    }
                    continue;
                }

                Component amount;
                Component effect;
                boolean needsL5 = skillLevel < 5;

                switch (skill) {
                    case AGILITY -> {
                        double pct = skillLevel >= 5 ? (skillLevel / 5.0) * 25 : 0;
                        amount = Component.literal(String.format("+%.0f%%", pct)).withStyle(ChatFormatting.AQUA);
                        effect = Component.translatable("tooltip.rereskillable.run_speed").withStyle(ChatFormatting.GRAY);
                    }
                    case MINING -> {
                        double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                        double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;
                        amount = Component.literal(String.format("+%.0f%%", pct)).withStyle(ChatFormatting.AQUA);
                        effect = Component.translatable("tooltip.rereskillable.break_speed").withStyle(ChatFormatting.GRAY);
                    }
                    case GATHERING -> {
                        double pct = skillLevel >= 5
                                ? (skillLevel / 5.0) * Configuration.GATHERING_XP_BONUS.get() * 100.0
                                : 0;
                        amount = Component.literal(String.format("+%.0f%%", pct)).withStyle(ChatFormatting.AQUA);
                        effect = Component.translatable("tooltip.rereskillable.bonus_xp_orbs").withStyle(ChatFormatting.GRAY);
                    }
                    case FARMING -> {
                        double pct = skillLevel >= 5 ? (skillLevel / 5.0) * 25 : 0;
                        amount = Component.literal("+" + (int) pct + "%").withStyle(ChatFormatting.AQUA);
                        effect = Component.translatable("tooltip.rereskillable.crop_growth").withStyle(ChatFormatting.GRAY);
                    }
                    default -> {
                        continue;
                    }
                }

                MutableComponent line = Component.literal("")
                        .append(skillName)
                        .append(": ")
                        .append(amount)
                        .append(" ")
                        .append(effect);
                if (needsL5) line.append(" ")
                        .append(Component.translatable("tooltip.rereskillable.requires_level_5")
                                .withStyle(ChatFormatting.DARK_GRAY));

                lines.add(line);
            }
        }

        gui.renderTooltip(
                Minecraft.getInstance().font,
                lines.stream().map(Component::getVisualOrderText).toList(),
                mouseX, mouseY
        );

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
        if (player != null) {
            SkillModel skillModel = SkillModel.get(player);
            for (Skill skill : Skill.values()) {
                int skillLevel = skillModel.getSkillLevel(skill);
                int xpCost = Configuration.calculateCostForLevel(skillLevel + 1);
                boolean hasXP = skillModel.hasSufficientXP(player, skill);

                xpCostDisplay.put(skill, String.valueOf(xpCost));
                xpCostColor.put(skill, hasXP ? 0x00FF00 : 0xFF0000);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
                return true;
            }

            if (KeyMapping.openKey.matches(keyCode, scanCode)) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}