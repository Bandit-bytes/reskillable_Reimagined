package net.bandit.reskillable.common;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * oh no
 * @see SkillModel#checkRequirements(Player, ResourceLocation, RequirementType)
 * @see SkillModel#sendSkillRequirementMessage(Player, RequirementType, List)
 */
public class AbsEventHandler {

    protected boolean checkRequirements(SkillModel skillModel, Player player, ResourceLocation resource) {
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

    protected void sendSkillRequirementMessage(Player player, RequirementType type, List<Requirement> unmetRequirements) {
        String translationKey = switch (type) {
            case ATTACK -> "message.reskillable.requirement.attack";
            case CRAFT -> "message.reskillable.requirement.craft";
            case USE -> "message.reskillable.requirement.use";
        };

        List<Component> formattedRequirements = new ArrayList<>();
        for (Requirement req : unmetRequirements) {
            String skillTranslationKey = "skill." + req.skill.name().toLowerCase(); // Ensure key matches lang file
            Component translatedSkillName = Component.translatable(skillTranslationKey); // Retrieve translated name
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
}
