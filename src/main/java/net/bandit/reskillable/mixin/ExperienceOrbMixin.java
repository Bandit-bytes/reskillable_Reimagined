package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.EventHandler;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Shadow public int value;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void reskillable$awardGatheringBonusXp(Player player, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        int bonusXp = EventHandler.gatheringBonusXp(player, value);
        if (bonusXp > 0) player.giveExperiencePoints(bonusXp);
    }
}
