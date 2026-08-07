package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void reskillable$addHealthRegenerationAttribute(
            CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(AttributeRegistry.HEALTH_REGENERATION, 0.0D);
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void reskillable$applyMiningSpeedBonus(
            BlockState state,
            CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(EventHandler.applyMiningSpeedBonus((Player) (Object) this, cir.getReturnValue()));
    }
}
