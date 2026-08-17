package net.bandit.reskillable.common.commands.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;


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


    public Requirement[] getRequirements(ResourceLocation resource) {
        return this.resourceRequirementMap.apply(resource);
    }

    public Requirement[] getRequirementsForKey(String key) {
        return this.stringRequirementMap.apply(key);
    }
}