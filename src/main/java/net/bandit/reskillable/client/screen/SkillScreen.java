package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.Configuration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
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
        guiGraphics.drawString(font, title.getVisualOrderText(), width / 2 - font.width(title) / 2, top + 6, 0x3F3F3F, false);

        // Render precomputed XP costs and colors
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

            // Pre-compute XP costs and colors
            for (Skill skill : Skill.values()) {
                int skillLevel = skillModel.getSkillLevel(skill);
                int xpCost = Configuration.calculateExperienceCost(skillLevel);
                boolean hasXP = skillModel.hasSufficientXP(player, skill);

                xpCostDisplay.put(skill, String.valueOf(xpCost));
                xpCostColor.put(skill, hasXP ? 0x00FF00 : 0xFF0000);
            }
        }
    }
}