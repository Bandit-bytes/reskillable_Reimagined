package net.bandit.reskillable.common;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.spell_engine.api.spell.container.SpellContainerHelper;

public class IronsSpellbooksEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpellCastEvent(SpellPreCastEvent event) {
        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        String spellId = event.getSpellId();
        String[] split = spellId.split(":");
        if (split.length == 2) {
            // ISS:     irons_spellbooks:scroll__<spell id>_<level>
            // eg:      irons_spellbooks:scroll__teleport_1
            ResourceLocation ironsSpellbooks = ResourceLocation.fromNamespaceAndPath("irons_spellbooks",
                    "scroll__%s_%d".formatted(
                            split[0x01],
                            event.getSpellLevel()
                    ));
            if (!checkRequirements(model, player, ironsSpellbooks)) {
                event.setCanceled(true);
            }
        } else {
            event.setCanceled(true);
        }
    }
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
                    net.minecraft.network.chat.Component.translatable("reskillable.requirement.not_met"),
                    true
            );
        }
    }

}
