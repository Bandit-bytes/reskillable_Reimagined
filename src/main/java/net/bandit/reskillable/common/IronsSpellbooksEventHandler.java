package net.bandit.reskillable.common;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class IronsSpellbooksEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpellCastEvent(SpellPreCastEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        if (player.level().isClientSide) {
            return;
        }

        if (player.isCreative()) {
            return;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        String spellIdStr = event.getSpellId();
        String[] split = spellIdStr.split(":");
        if (split.length != 2) {
            event.setCanceled(true);
            return;
        }

        ResourceLocation checkKey = ResourceLocation.fromNamespaceAndPath(
                "irons_spellbooks",
                "scroll__%s_%d".formatted(
                        split[1],
                        event.getSpellLevel()
                )
        );

        if (!checkRequirements(model, player, checkKey)) {
            System.out.println("⚠ Blocking spell cast: unmet requirements for " + checkKey);
            event.setCanceled(true);
        }
    }

}
