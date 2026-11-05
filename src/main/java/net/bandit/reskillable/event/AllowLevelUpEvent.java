package net.bandit.reskillable.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Posted on the NeoForge event bus, on the server, when a level-up from the level-up packet or
 * {@code ReskillableApi.levelUp} has passed Reskillable's checks, before the XP is taken. Cancel it to refuse
 * the level-up: nothing changes, Reskillable sends no message and {@code levelUp} returns {@code REFUSED}.
 * {@code ReskillableApi.canLevelUp} does not post it. A refusal sends the player nothing, so a listener that
 * cancels should say why. Don't call {@code ReskillableApi.levelUp} for the same player from a listener.
 *
 * <p>{@link #getSkillId()} is the skill's public id, as in {@code built_in_skills.json} and
 * {@code custom_skills.json}.
 */
public class AllowLevelUpEvent extends PlayerEvent implements ICancellableEvent {
    private final String skillId;

    public AllowLevelUpEvent(ServerPlayer player, String skillId) {
        super(player);
        this.skillId = skillId;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public String getSkillId() {
        return skillId;
    }
}
