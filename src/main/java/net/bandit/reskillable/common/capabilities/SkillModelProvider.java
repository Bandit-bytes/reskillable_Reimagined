package net.bandit.reskillable.common.capabilities;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

public class SkillModelProvider implements ICapabilityProvider<Player, Void, SkillModel> {
    @Override
    public SkillModel getCapability(Player player, @Nullable Void context) {
        return SkillModel.get(player);
    }
}
