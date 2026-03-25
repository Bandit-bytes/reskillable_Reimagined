package net.bandit.reskillable.registry;

import net.bandit.reskillable.Reskillable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AttributeRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, Reskillable.MOD_ID);

    public static final Holder<Attribute> HEALTH_REGENERATION = ATTRIBUTES.register(
            "health_regeneration",
            () -> new RangedAttribute(
                    "attribute.name.reskillable.health_regeneration",
                    0.0D,
                    0.0D,
                    100.0D
            ).setSyncable(true)
    );

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, HEALTH_REGENERATION, 0.0D);
    }
}