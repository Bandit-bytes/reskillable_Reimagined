package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.AbsEventHandler;
import net.bandit.reskillable.common.capabilities.SkillModel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCast;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpellHelper.class)
public abstract class SpellHelperMixin {

    @Inject(
            method = "attemptCasting(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/spell_engine/internals/casting/SpellCast$Attempt;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void reskillable$gateWizardsCasting(Player player,
                                                       ItemStack itemStack,
                                                       ResourceLocation spellId,
                                                       boolean checkAmmo,
                                                       CallbackInfoReturnable<SpellCast.Attempt> cir) {
        // Let creative bypass
        if (player.isCreative()) {
            return;
        }

        if (itemStack.isEmpty()) {
            return;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) {
            return;
        }

        if (!"wizards".equals(itemId.getNamespace())) {
            return;
        }

        String path = itemId.getPath();
        if (!(path.startsWith("wand_") || path.startsWith("staff_"))) {
            return;
        }

        boolean meets = AbsEventHandler.checkRequirements(model, player, itemId);
        if (!meets) {
            cir.setReturnValue(SpellCast.Attempt.none());
        }
    }
}
