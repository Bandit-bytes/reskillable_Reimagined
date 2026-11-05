package net.bandit.reskillable.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Posted on the NeoForge event bus, on the server, after a player's level in a skill changes: a level-up,
 * {@code /skills add}, {@code /skills set}, {@code /skills respec}, or the reset on death when
 * {@code deathSkillReset} is on. {@code SkillModel.get(player)} already holds the new level.
 *
 * <p>{@link #getSkillId()} is the skill's public id, as in {@code built_in_skills.json} and
 * {@code custom_skills.json}.
 */
public class SkillLevelChangedEvent extends PlayerEvent {
    private final String skillId;
    private final int oldLevel;
    private final int newLevel;

    public SkillLevelChangedEvent(ServerPlayer player, String skillId, int oldLevel, int newLevel) {
        super(player);
        this.skillId = skillId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public String getSkillId() {
        return skillId;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
