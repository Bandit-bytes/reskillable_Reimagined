package net.bandit.reskillable.client.compat.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum EntityRequirementComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip,
                              EntityAccessor accessor,
                              IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (entity == null) {
            return;
        }

        ResourceLocation key;
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().isEmpty()) {
                return;
            }
            key = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
        } else {
            key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        }

        RequirementTooltipHelper.appendRequirements(tooltip, accessor.getPlayer(), key);
    }

    @Override
    public ResourceLocation getUid() {
        return ReskillableJadePlugin.REQUIREMENTS_ENTITY_UID;
    }
}
