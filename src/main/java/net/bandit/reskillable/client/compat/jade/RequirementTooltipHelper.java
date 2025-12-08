package net.bandit.reskillable.client.compat.jade;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.ITooltip;

public class RequirementTooltipHelper {

    public static void appendRequirements(ITooltip tooltip,
                                          Player player,
                                          ResourceLocation key) {
        if (player == null || key == null) {
            return;
        }

        Requirement[] requirements = Configuration.getRequirements(key);
        if (requirements == null || requirements.length == 0) {
            return;
        }

        SkillModel skillModel = SkillModel.get(player);
        if (skillModel == null) {
            return;
        }
        tooltip.add(Component.literal(""));

        tooltip.add(
                Component.translatable("tooltip.requirements")
                        .append(":")
                        .withStyle(ChatFormatting.GRAY)
        );

        for (Requirement req : requirements) {
            boolean meets = skillModel.getSkillLevel(req.skill) >= req.level;

            Component line = Component.translatable(req.skill.displayName)
                    .append(" " + req.level)
                    .withStyle(meets ? ChatFormatting.GREEN : ChatFormatting.RED);

            tooltip.add(line);
        }
    }
}
