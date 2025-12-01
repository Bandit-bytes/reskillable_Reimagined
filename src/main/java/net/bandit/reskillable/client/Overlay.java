package net.bandit.reskillable.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Overlay implements LayeredDraw.Layer {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "skill_warning");
    private static List<Requirement> requirements = null;
    private static int showTicks = 0;
    private static String messageKey = "";

    public static final Overlay INSTANCE = new Overlay();

    public static void showWarning(ResourceLocation resource, RequirementType type) {
        requirements = Arrays.asList(type.getRequirements(resource));
        messageKey = "overlay.message." + type.name().toLowerCase(Locale.ROOT);
        showTicks = 60;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        if (showTicks > 0) showTicks--;
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (showTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        SkillModel model = SkillModel.get(mc.player);
        if (model == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1f, 1f, 1f, 1f);

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 4;
        guiGraphics.blit(SkillScreen.RESOURCES, cx - 71, cy - 4, 0, 194, 142, 40);

        String message = Component.translatable(messageKey).getString();
        guiGraphics.drawString(mc.font, message, cx - mc.font.width(message) / 2, cy, 0xFF5555, false);

        int maxLevel = Configuration.getMaxLevel();
        for (int i = 0; i < requirements.size(); i++) {
            var req = requirements.get(i);
            int x = cx + i * 20 - requirements.size() * 10 + 2;
            int y = cy + 15;

            int u = Math.min(req.level, maxLevel - 1) / (maxLevel / 4) * 16 + 176;
            int v = req.skill.index * 16 + 128;

            guiGraphics.blit(SkillScreen.RESOURCES, x, y, u, v, 16, 16);

            String levelStr = Integer.toString(req.level);
            boolean met = ClientUtils.getClientSkillModel().getSkillLevel(req.skill) >= req.level;
            guiGraphics.drawString(mc.font, levelStr, x + 17 - mc.font.width(levelStr), y + 9, met ? 0x55FF55 : 0xFF5555, false);
        }

        guiGraphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

}
