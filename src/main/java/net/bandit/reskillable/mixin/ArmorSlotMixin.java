package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The armor slots in the player inventory are an anonymous Slot inside InventoryMenu.
 * Refusing the item here stops it ever reaching the armor slot, by normal click or
 * shift-click, so there is no window between equipping and the equipment-change check
 * for fast clicking to slip through. Runs on both sides so the client doesn't predict it.
 */
@Mixin(targets = "net.minecraft.world.inventory.InventoryMenu$1")
public abstract class ArmorSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void reskillable$checkArmorRequirement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(((Slot) (Object) this).container instanceof Inventory inventory)) return;

        Player player = inventory.player;
        if (player == null || player instanceof FakePlayer || player.isCreative()) return;

        SkillModel model = SkillModel.get(player);
        if (model != null && !model.meetsItemUseRequirements(stack)) {
            cir.setReturnValue(false);
        }
    }
}
