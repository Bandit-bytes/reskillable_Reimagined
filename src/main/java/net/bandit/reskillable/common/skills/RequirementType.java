package net.bandit.reskillable.common.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;


public enum RequirementType {
    USE(Configuration::getRequirements),
    CRAFT(Configuration::getCraftRequirements),
    ATTACK(Configuration::getEntityAttackRequirements);

    private final Function<ResourceLocation, Requirement[]> requirementMap;

    RequirementType(Function<ResourceLocation, Requirement[]> requirementMap) {
        this.requirementMap = requirementMap;
    }

    public Requirement[] getRequirements(ResourceLocation resource) {
        return this.requirementMap.apply(resource);
    }
}
