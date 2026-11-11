package net.bandit.reskillable.event;

import com.google.common.collect.Multimap;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stops held items and worn armor from giving their attribute modifiers while the player
 * doesn't meet the item's use requirement (issue #63). Blocking use alone left things like
 * attack damage, armor and Iron's Spells magic attributes active just from holding the item.
 *
 * Vanilla applies equipment modifiers during the entity tick, so they are taken off again at
 * the end of the same tick. Vanilla only re-applies them when the stack changes, so when the
 * player later meets the requirement the modifiers are put back here.
 *
 * Server side only: attribute values reach the client through the vanilla attribute sync.
 */
public class AttributeLockHandler {
    // Keyed by the player object, not UUID, so a respawned player entity starts fresh.
    // Only touched from the server thread.
    private static final Map<Player, EnumMap<EquipmentSlot, ItemStack>> STRIPPED = new WeakHashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide() || player instanceof FakePlayer) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        EnumMap<EquipmentSlot, ItemStack> stripped = STRIPPED.get(player);
        boolean bypass = player.isCreative();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack current = player.getItemBySlot(slot);
            ItemStack strippedStack = stripped == null ? null : stripped.get(slot);

            // The stack changed since we stripped it, so vanilla has already swapped the
            // modifiers over. Forget the old record and judge the new stack fresh.
            if (strippedStack != null && !ItemStack.matches(strippedStack, current)) {
                stripped.remove(slot);
                strippedStack = null;
            }

            if (current.isEmpty()) continue;

            boolean locked = !bypass && !model.meetsItemUseRequirements(current);
            Multimap<Attribute, AttributeModifier> modifiers = current.getAttributeModifiers(slot);

            if (locked && strippedStack == null) {
                if (modifiers.isEmpty()) continue;
                player.getAttributes().removeAttributeModifiers(modifiers);
                if (stripped == null) {
                    stripped = new EnumMap<>(EquipmentSlot.class);
                    STRIPPED.put(player, stripped);
                }
                stripped.put(slot, current.copy());
            } else if (!locked && strippedStack != null) {
                // Remove first: adding a modifier whose UUID is already present throws.
                player.getAttributes().removeAttributeModifiers(modifiers);
                player.getAttributes().addTransientAttributeModifiers(modifiers);
                stripped.remove(slot);
            }
        }

        if (stripped != null && stripped.isEmpty()) {
            STRIPPED.remove(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STRIPPED.remove(event.getEntity());
    }
}
