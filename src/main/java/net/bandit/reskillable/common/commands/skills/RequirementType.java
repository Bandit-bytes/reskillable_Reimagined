package net.bandit.reskillable.common.commands.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Enum representing different types of requirements for actions in the game.
 */
public enum RequirementType {
    USE(Configuration::getRequirements),
    CRAFT(Configuration::getCraftRequirements),
    ATTACK(Configuration::getEntityAttackRequirements);

    private final Function<ResourceLocation, Requirement[]> requirementMap;

    /**
     * Constructs a new RequirementType with the specified requirement mapping function.
     *
     * @param requirementMap A function that maps a ResourceLocation to an array of Requirements.
     */
    RequirementType(Function<ResourceLocation, Requirement[]> requirementMap) {
        this.requirementMap = requirementMap;
    }

    /**
     * Gets the requirements for the specified resource.
     *
     * @param resource The ResourceLocation to get requirements for.
     * @return An array of Requirements for the specified resource.
     */
    public Requirement[] getRequirements(ResourceLocation resource) {
        return this.requirementMap.apply(resource);
    }
}
