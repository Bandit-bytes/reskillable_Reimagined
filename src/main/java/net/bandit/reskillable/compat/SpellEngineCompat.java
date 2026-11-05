package net.bandit.reskillable.compat;

import net.bandit.reskillable.common.AbsEventHandler;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.internals.casting.SpellCast;

/**
 * Stops a spell cast from an item whose skill requirements the player does not meet, through Spell
 * Engine's casting attempt event (Spell Engine 1.10 and later). Only touched when Spell Engine is loaded.
 */
public final class SpellEngineCompat {
    private SpellEngineCompat() {}

    public static void register() {
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            Player player = args.caster();
            ItemStack stack = args.itemStack();
            if (player.isCreative() || stack.isEmpty()) {
                return null;
            }
            if (SpellContainerHelper.containerFromItemStack(stack) == null) {
                return null;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (AbsEventHandler.checkRequirements(SkillModel.get(player), player, itemId)) {
                return null;
            }
            return SpellCast.Attempt.none();
        });
    }
}
