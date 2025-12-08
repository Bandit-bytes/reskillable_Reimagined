package net.bandit.reskillable.client.compat.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ReskillableJadePlugin implements IWailaPlugin {

    public static final ResourceLocation REQUIREMENTS_BLOCK_UID =
            ResourceLocation.fromNamespaceAndPath("reskillable", "requirements_block");
    public static final ResourceLocation REQUIREMENTS_ENTITY_UID =
            ResourceLocation.fromNamespaceAndPath("reskillable", "requirements_entity");
    public static final ResourceLocation REQUIREMENTS_ITEM_UID =
            ResourceLocation.fromNamespaceAndPath("reskillable", "requirements_item");

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                BlockRequirementComponentProvider.INSTANCE,
                Block.class
        );

        registration.registerEntityComponent(
                EntityRequirementComponentProvider.INSTANCE,
                Entity.class
        );
    }
}
