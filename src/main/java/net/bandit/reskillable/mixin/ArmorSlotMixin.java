package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses armor the player can't use at the armor slot itself, by normal click or shift-click.
 * The equipment check only ran after the armor was already on, so clicking fast could get it
 * equipped anyway (issue #75). Runs on both sides so the client doesn't predict the move.
 */
@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void reskillable$checkArmorRequirement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(((Slot) (Object) this).container instanceof Inventory inventory)) return;

        Player player = inventory.player;
        if (player == null || player.isCreative()) return;

        SkillModel model = SkillModel.get(player);
        if (model != null && !model.meetsItemUseRequirements(stack)) {
            cir.setReturnValue(false);
        }
    }
}
