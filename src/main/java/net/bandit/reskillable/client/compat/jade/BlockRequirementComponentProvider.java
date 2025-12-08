package net.bandit.reskillable.client.compat.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BlockRequirementComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip,
                              BlockAccessor accessor,
                              IPluginConfig config) {
        Block block = accessor.getBlock();
        if (block == null) {
            return;
        }

        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        RequirementTooltipHelper.appendRequirements(tooltip, accessor.getPlayer(), key);
    }

    @Override
    public ResourceLocation getUid() {
        return ReskillableJadePlugin.REQUIREMENTS_BLOCK_UID;
    }
}
