package net.bandit.reskillable.common.commands.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Enum representing different types of requirements for actions in the game.
 */
public enum RequirementType {
    USE(Configuration::getRequirements, Configuration::getRequirementsForKey),
    CRAFT(Configuration::getCraftRequirements, Configuration::getCraftRequirementsForKey),
    ATTACK(Configuration::getEntityAttackRequirements, Configuration::getEntityAttackRequirementsForKey);

    private final Function<ResourceLocation, Requirement[]> resourceRequirementMap;
    private final Function<String, Requirement[]> stringRequirementMap;

    RequirementType(Function<ResourceLocation, Requirement[]> resourceRequirementMap,
                    Function<String, Requirement[]> stringRequirementMap) {
        this.resourceRequirementMap = resourceRequirementMap;
        this.stringRequirementMap = stringRequirementMap;
    }

    /**
     * Gets the requirements for the specified resource.
     *
     * @param resource The ResourceLocation to get requirements for.
     * @return An array of Requirements for the specified resource.
     */
    public Requirement[] getRequirements(ResourceLocation resource) {
        return this.resourceRequirementMap.apply(resource);
    }

    /**
     * Gets the requirements for the specified raw string key.
     * Supports wildcard-style keys such as:
     * tconstruct:broad_axe__tconstruct_pig_iron__*
     *
     * @param key The raw requirement key.
     * @return An array of Requirements for the specified key.
     */
    public Requirement[] getRequirementsForKey(String key) {
        return this.stringRequirementMap.apply(key);
    }
}