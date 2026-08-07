package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void reskillable$applyFarmingGrowthBonus(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {
        if (EventHandler.shouldForceCropGrowth(level, pos)) {
            ((CropBlock) (Object) this).growCrops(level, pos, state);
            ci.cancel();
        }
    }
}
