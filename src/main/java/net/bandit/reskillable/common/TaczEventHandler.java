package net.bandit.reskillable.common;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class TaczEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGunFireEvent(GunFireEvent event) {
        if (event.getShooter() instanceof Player player) {
            if (player.isCreative()) {
                return;
            }
            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            ItemStack item = event.getGunItemStack();
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(item.getItem());
            CompoundTag tag = item.getTag();

            String gun = null;
            if (itemRegistryName != null && tag != null) {
                // TACZ:    tacz:<gun type>__<gunid>
                // eg:      tacz:modern_kinetic_gun__bf1_tg1918
                gun = "%s__%s".formatted(
                        itemRegistryName.getPath(),
                        tag.getString("GunId").replaceAll(":", "_"));

                if (!checkRequirements(model, player, new ResourceLocation("tacz", gun))) {
                    event.setCanceled(true);
                }
            } else {
                // 不知道什么情况，不准开枪
                event.setCanceled(true);
            }
        }
    }
}