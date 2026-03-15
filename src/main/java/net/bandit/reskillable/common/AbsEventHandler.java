package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.RequirementType;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AbsEventHandler {

    public static boolean checkRequirements(SkillModel skillModel, Player player, ResourceLocation resource) {
        Requirement[] requirements = RequirementType.USE.getRequirements(resource);
        if (requirements == null || requirements.length == 0) {
            return true;
        }

        List<Requirement> unmetRequirements = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (skillModel.getSkillLevel(requirement.skill) < requirement.level) {
                unmetRequirements.add(requirement);
            }
        }

        if (!unmetRequirements.isEmpty()) {
            sendSkillRequirementMessage(player, RequirementType.USE, unmetRequirements);
            return false;
        }

        return true;
    }

    public static void sendSkillRequirementMessage(Player player, RequirementType type, List<Requirement> unmetRequirements) {
        String translationKey = switch (type) {
            case ATTACK -> "message.reskillable.requirement.attack";
            case CRAFT -> "message.reskillable.requirement.craft";
            case USE -> "message.reskillable.requirement.use";
        };

        List<Component> formattedRequirements = new ArrayList<>();
        for (Requirement req : unmetRequirements) {
            Component translatedSkillName = getSkillDisplayComponent(req.skill);
            formattedRequirements.add(
                    Component.literal("")
                            .append(translatedSkillName)
                            .append(" level " + req.level)
            );
        }

        Component joinedRequirements = Component.literal(" ")
                .append(Component.literal(String.join(", ",
                        formattedRequirements.stream()
                                .map(Component::getString)
                                .collect(Collectors.toList()))
                ));

        Component message = Component.translatable(translationKey, joinedRequirements);
        player.displayClientMessage(message, true);
    }

    private static MutableComponent getSkillDisplayComponent(String skillId) {
        String normalized = normalizeSkillId(skillId);

        Skill vanilla = getVanillaSkillOrNull(normalized);
        if (vanilla != null) {
            return Component.translatable("skill." + normalized);
        }

        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(normalized);
        if (custom != null) {
            return Component.literal(custom.getDisplayName());
        }

        return Component.literal(normalized);
    }

    private static Skill getVanillaSkillOrNull(String skillId) {
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