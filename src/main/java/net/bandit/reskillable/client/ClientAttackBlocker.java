package net.bandit.reskillable.client;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Cancels the attack key, and the arm swing with it, while holding an item the player can't use
 * (issue #63). Some mods fire projectiles or effects from the swing itself, which still happened
 * even though the server cancelled the hit.
 */
public class ClientAttackBlocker {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative()) return;

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.isEmpty()) return;

        SkillModel model = SkillModel.get(player);
        if (model != null && !model.meetsItemUseRequirements(stack)) {
            model.canUseItem(player, stack); // shows the missing-skill message
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
