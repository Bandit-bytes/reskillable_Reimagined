package net.bandit.reskillable.common.capabilities;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.EntityCapability;

public class SkillCapability {
    public static final EntityCapability<SkillModel, Void> INSTANCE =
            EntityCapability.createVoid(
                   Identifier.fromNamespaceAndPath("reskillable", "skill_model"),
                    SkillModel.class
            );
}
