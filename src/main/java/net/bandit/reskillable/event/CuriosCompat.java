package net.bandit.reskillable.event;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public class CuriosCompat
{

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeCurio(CurioChangeEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            if (!player.isCreative() && !(player instanceof FakePlayer))
            {
                SkillModel model = SkillModel.get(player);
                ItemStack item = event.getTo();

                if (model != null && !model.canUseItem(player, item))
                {
                    player.drop(item.copy(), false);
                    item.setCount(0);
                }
            }
        }
    }

    // A curio that slips past onChangeCurio still shouldn't give its attributes (issue #63).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCurioAttributes(CurioAttributeModifierEvent event)
    {
        if (event.getSlotContext() == null) return;
        if (!(event.getSlotContext().entity() instanceof Player player)) return;
        if (player.isCreative() || player instanceof FakePlayer) return;

        SkillModel model = SkillModel.get(player);
        if (model != null && !model.meetsItemUseRequirements(event.getItemStack()))
        {
            event.clearModifiers();
        }
    }
}
