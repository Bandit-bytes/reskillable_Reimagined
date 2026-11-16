package net.bandit.reskillable.event;

import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Iron's Arms 'n Artifice fires guns from its own input and packet, never through
 * vanilla item use, so the gun's use requirement is checked here instead.
 * The event fires on both sides before any ammo, recoil or bullets are applied.
 */
public class IronsArtificeEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGunAboutToShoot(GunAboutToShootEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player instanceof FakePlayer || player.isCreative()) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack gun = event.getShotProfile().itemStack();
        if (!gun.isEmpty() && !model.canUseItem(player, gun)) {
            event.setCanceled(true);
        }
    }
}
