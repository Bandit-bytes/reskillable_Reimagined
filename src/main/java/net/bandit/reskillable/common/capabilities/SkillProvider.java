package net.bandit.reskillable.common.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class SkillProvider implements ICapabilityProvider<Object, Void, SkillModel>, INBTSerializable<CompoundTag> {
    private final SkillModel skillModel = new SkillModel();

    @Override
    public SkillModel getCapability(Object object, @Nullable Void context) {
        return skillModel;
    }

    public SkillModel getSkillModel() {
        return skillModel;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return skillModel.serializeNBT(null);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compoundTag) {
        skillModel.deserializeNBT(null, compoundTag);
    }
}
