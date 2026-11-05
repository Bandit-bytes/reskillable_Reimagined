package net.bandit.reskillable.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired on the server after a player's level in a skill changes: a level-up, {@code /skills add},
 * {@code /skills set}, {@code /skills respec}, or the reset on death when {@code deathSkillReset} is on.
 * {@code SkillModel.get(player)} already holds the new level.
 *
 * <p>{@code skillId} is the skill's public id, as in {@code built_in_skills.json} and {@code custom_skills.json}.
 */
@FunctionalInterface
public interface SkillLevelChangedCallback {
    Event<SkillLevelChangedCallback> EVENT = EventFactory.createArrayBacked(SkillLevelChangedCallback.class,
            listeners -> (player, skillId, oldLevel, newLevel) -> {
                for (SkillLevelChangedCallback listener : listeners) {
                    listener.onLevelChanged(player, skillId, oldLevel, newLevel);
                }
            });

    void onLevelChanged(ServerPlayer player, String skillId, int oldLevel, int newLevel);
}
