package net.bandit.reskillable.common;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public class CuriosCompat
{
    // Change Curio

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeCurio(CurioChangeEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            if (!player.isCreative())
            {
                ItemStack item = event.getTo();

                // Check if the player cannot use the item
                if (!SkillModel.get(player).canUseItem(player, item))
                {
                    player.drop(item.copy(), false);
                    item.setCount(0);
                }
            }
        }
    }
}
