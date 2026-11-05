package net.bandit.reskillable.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired on the server when a level-up from the level-up packet or {@code ReskillableApi.levelUp} has passed
 * Reskillable's checks, before the XP is taken. Return false to refuse it: nothing changes, Reskillable sends
 * no message and {@code levelUp} returns {@code REFUSED}. The first listener that returns false decides.
 * {@code ReskillableApi.canLevelUp} does not fire it. A refusal sends the player nothing, so a listener that
 * refuses should say why. Don't call {@code ReskillableApi.levelUp} for the same player from a listener.
 *
 * <p>{@code skillId} is the skill's public id, as in {@code built_in_skills.json} and {@code custom_skills.json}.
 */
@FunctionalInterface
public interface AllowLevelUpCallback {
    Event<AllowLevelUpCallback> EVENT = EventFactory.createArrayBacked(AllowLevelUpCallback.class,
            listeners -> (player, skillId) -> {
                for (AllowLevelUpCallback listener : listeners) {
                    if (!listener.allowLevelUp(player, skillId)) {
                        return false;
                    }
                }
                return true;
            });

    boolean allowLevelUp(ServerPlayer player, String skillId);
}
