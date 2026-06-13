package net.bandit.reskillable.common.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.resources.Identifier;

import java.util.function.Function;


public enum RequirementType {
    USE(Configuration::getRequirements),
    CRAFT(Configuration::getCraftRequirements),
    ATTACK(Configuration::getEntityAttackRequirements);

    private final Function<Identifier, Requirement[]> requirementMap;

    RequirementType(Function<Identifier, Requirement[]> requirementMap) {
        this.requirementMap = requirementMap;
    }

    public Requirement[] getRequirements(Identifier resource) {
        return this.requirementMap.apply(resource);
    }
}
