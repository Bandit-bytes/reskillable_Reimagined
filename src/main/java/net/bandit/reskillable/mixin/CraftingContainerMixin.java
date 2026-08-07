package net.bandit.reskillable.mixin;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;
@Mixin(CraftingMenu.class)
public class CraftingContainerMixin {
    @Inject(at = @At("HEAD"), method = "slotChangedCraftingGrid", cancellable = true)
    private static void onUpdateCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (!level.isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ItemStack craftResult = ItemStack.EMPTY;

            Optional<RecipeHolder<CraftingRecipe>> optional = Objects.requireNonNull(level.getServer())
                    .getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftSlots.asCraftInput(), level);

            if (optional.isPresent()) {
                CraftingRecipe craftingRecipe = optional.get().value();
                craftResult = craftingRecipe.assemble(craftSlots.asCraftInput(), level.registryAccess());
            }

            if (!craftResult.isEmpty() && !SkillModel.get(serverPlayer).canCraftItem(serverPlayer, craftResult)) {
                ci.cancel();
                resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}
