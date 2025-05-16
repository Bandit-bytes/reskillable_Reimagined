package net.bandit.reskillable.common.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.jetbrains.annotations.Nullable;

public class SkillCapability {
    public static final EntityCapability<SkillModel, Void> INSTANCE =
            EntityCapability.createVoid(
                   ResourceLocation.fromNamespaceAndPath("reskillable", "skill_model"),
                    SkillModel.class
            );
}
