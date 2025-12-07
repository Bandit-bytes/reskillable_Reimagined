package net.bandit.reskillable.common;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

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
            ResourceLocation itemRegistryName = BuiltInRegistries.ITEM.getKey(item.getItem());

            CustomData data = item.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data != null ? data.copyTag() : null;

            if (itemRegistryName != null && tag != null && tag.contains("GunId")) {

                String rawId = tag.getString("GunId");
                // handles: "taczm95", "tacz:m95", "m95"
                String cleanId = rawId
                        .replace("tacz:", "")
                        .replace("tacz", "");

                String gunKey = "%s__%s".formatted(
                        itemRegistryName.getPath(),
                        cleanId
                );

                ResourceLocation checkKey = ResourceLocation.fromNamespaceAndPath("tacz", gunKey);

                if (!checkRequirements(model, player, checkKey)) {
                    event.setCanceled(true);
                }

            } else {
                event.setCanceled(true);
            }
        }
    }
}
