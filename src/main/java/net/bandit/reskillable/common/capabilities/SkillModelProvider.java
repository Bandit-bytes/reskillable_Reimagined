package net.bandit.reskillable.common.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public class SkillModelProvider implements ICapabilityProvider<Player, Void, SkillModel>, INBTSerializable<CompoundTag> {
    private final SkillModel model = new SkillModel();

    @Override
    public SkillModel getCapability(Player player, @Nullable Void context) {
        return model;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return model.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        model.deserializeNBT(provider, tag);
    }
}

