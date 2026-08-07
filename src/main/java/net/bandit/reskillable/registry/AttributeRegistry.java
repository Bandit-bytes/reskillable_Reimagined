package net.bandit.reskillable.registry;

import net.bandit.reskillable.Reskillable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class AttributeRegistry {
    private AttributeRegistry() {}

    public static final Holder.Reference<Attribute> HEALTH_REGENERATION = Registry.registerForHolder(
            BuiltInRegistries.ATTRIBUTE,
            ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "health_regeneration"),
            new RangedAttribute(
                    "attribute.name.reskillable.health_regeneration",
                    0.0D,
                    0.0D,
                    100.0D
            ).setSyncable(true)
    );

    /** Forces static registration to occur during common initialization. */
    public static void register() {
        // no-op
    }
}
