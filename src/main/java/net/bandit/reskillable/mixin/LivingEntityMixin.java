package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void reskillable$gateTotemUse(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player) || player.isCreative()) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.TOTEM_OF_UNDYING) && !EventHandler.canUseTotem(player, stack)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
