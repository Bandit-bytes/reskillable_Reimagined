package net.bandit.reskillable.common.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

public class SkillProvider implements ICapabilityProvider<Object, Void, SkillModel> {
    private final SkillModel skillModel = new SkillModel();

    @Override
    public SkillModel getCapability(Object object, @Nullable Void context) {
        return skillModel;
    }

    public CompoundTag serialize() {
        return skillModel.serializeNBT(null);
    }

    public void deserialize(CompoundTag tag) {
        skillModel.deserializeNBT(null, tag);
    }
}
