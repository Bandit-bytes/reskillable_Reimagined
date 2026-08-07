package net.bandit.reskillable.mixin;

import net.bandit.reskillable.Configuration;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SheepWoolDropMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void reskillable$disableUnshearedWoolDrops(
            ItemStack stack,
            float yOffset,
            CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object) this instanceof Sheep sheep
                && Configuration.getDisableWool()
                && !sheep.isSheared()
                && stack.is(ItemTags.WOOL)) {
            cir.setReturnValue(null);
        }
    }
}
