package net.bandit.reskillable.client;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.RequirementType;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Overlay {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("reskillable", "skill_warning");

    private static List<Requirement> requirements = null;
    private static int showTicks = 0;
    private static String messageKey = "";

    public static final Overlay INSTANCE = new Overlay();

    public static void showWarning(Identifier resource, RequirementType type) {
        Requirement[] reqs = type.getRequirements(resource);
        if (reqs == null || reqs.length == 0) {
            requirements = null;
            showTicks = 0;
            return;
        }

        requirements = Arrays.asList(reqs);
        messageKey = "overlay.message." + type.name().toLowerCase(Locale.ROOT);
        showTicks = 60;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        if (showTicks > 0) {
            showTicks--;
        }
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (showTicks <= 0 || requirements == null || requirements.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }

        SkillModel model = SkillModel.get(mc.player);
        if (model == null) {
            return;
        }

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 4;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SkillScreen.RESOURCES, cx - 71, cy - 4, 0, 194, 142, 40, 256, 256);

        String message = Component.translatable(messageKey).getString();
        guiGraphics.text(mc.font, message, cx - mc.font.width(message) / 2, cy, 0xFFFF5555, false);

        int count = requirements.size();
        for (int i = 0; i < count; i++) {
            Requirement req = requirements.get(i);

            int x = cx + i * 20 - count * 10 + 2;
            int y = cy + 15;

            renderRequirementIcon(guiGraphics, req, x, y);

            String levelStr = Integer.toString(req.level);
            boolean met = model.getSkillLevel(req.skill) >= req.level;
            guiGraphics.text(
                    mc.font,
                    levelStr,
                    x + 17 - mc.font.width(levelStr),
                    y + 9,
                    met ? 0xFF55FF55 : 0xFFFF5555,
                    false
            );
        }

    }

    private void renderRequirementIcon(GuiGraphicsExtractor guiGraphics, Requirement req, int x, int y) {
        String skillId = normalizeSkillId(req.skill);

        Skill vanilla = getVanillaSkillOrNull(skillId);
        if (vanilla != null) {
            int maxLevel = Math.max(1, Configuration.getMaxLevel());
            int divisor = Math.max(1, maxLevel / 4);

            int u = Math.min(req.level, maxLevel - 1) / divisor * 16 + 176;
            int v = vanilla.index * 16 + 128;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SkillScreen.RESOURCES, x, y, u, v, 16, 16, 256, 256);
            return;
        }

        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(skillId);
        if (custom != null) {
            Identifier icon = custom.getResolvedIcon();
            if (icon != null) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }

        guiGraphics.text(
                Minecraft.getInstance().font,
                "?",
                x + 5,
                y + 4,
                0xFFFFFFFF,
                false
        );
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static Skill getVanillaSkillOrNull(String skillId) {
        try {
            return Skill.valueOf(skillId.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}