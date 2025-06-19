package net.bandit.reskillable.common;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public class CuriosCompat
{

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeCurio(CurioChangeEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            if (!player.isCreative())
            {
                ItemStack item = event.getTo();
                if (!SkillModel.get(player).canUseItem(player, item))
                {
                    player.drop(item.copy(), false);
                    item.setCount(0);
                }
            }
        }
    }
}
