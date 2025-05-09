package net.bandit.reskillable.common;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
            ResourceLocation ironsSpellbooks = new ResourceLocation("irons_spellbooks",
                    "scroll__%s_%d".formatted(
                            split[0x01],
                            event.getSpellLevel()
                    ));
            if (!checkRequirements(model, player, ironsSpellbooks)) {
                event.setCanceled(true);
            }
        } else {
            // 不知道什么情况，不准施法
            event.setCanceled(true);
        }
    }
}