package net.bandit.reskillable.common;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.spell_engine.api.spell.container.SpellContainerHelper;

public class SpellEngineEventHandler extends AbsEventHandler{

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickWizardsItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        var container = SpellContainerHelper
                .containerFromItemStack(stack);
        if (container == null) {
            return;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            return;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }

        if (!checkRequirements(model, player, id)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("reskillable.requirement.not_met"),
                    true
            );
        }
    }
}
