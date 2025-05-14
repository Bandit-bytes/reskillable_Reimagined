package net.bandit.reskillable.common.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.jetbrains.annotations.Nullable;

public class SkillCapability {
    public static final EntityCapability<SkillModel, @Nullable Void> INSTANCE =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath("reskillable", "skill_model"),
                    SkillModel.class
            );
}
