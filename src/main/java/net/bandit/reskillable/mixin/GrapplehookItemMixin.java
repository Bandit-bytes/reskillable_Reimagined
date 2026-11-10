package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grappling Hook Mod fires hooks from its own keybinds and a custom packet,
 * bypassing vanilla item use, so the regular interact events never fire.
 * Runs on both sides: the client check also blocks the client-only
 * ender launch / rocket actions and stops the throw packet being sent.
 */
@Pseudo
@Mixin(targets = "com.yyon.grapplinghook.items.GrapplehookItem", remap = false)
public class GrapplehookItemMixin {
    @Inject(method = "onCustomKeyDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void reskillable$checkUseRequirement(ItemStack stack, Player player, @Coerce Object key, boolean isMainHand, CallbackInfo ci) {
        if (player == null || player instanceof FakePlayer || player.isCreative()) return;

        SkillModel model = SkillModel.get(player);
        if (model != null && !model.canUseItem(player, stack)) {
            ci.cancel();
        }
    }
}
