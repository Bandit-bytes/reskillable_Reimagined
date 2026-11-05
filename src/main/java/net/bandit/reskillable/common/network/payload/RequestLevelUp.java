package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.ReskillableApi.LevelUpStatus;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.gating.SkillLevelGate;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
import net.bandit.reskillable.event.AllowLevelUpEvent;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public record RequestLevelUp(String skillId) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "request_levelup");
    public static final Type<RequestLevelUp> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RequestLevelUp> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    RequestLevelUp::skillId,
                    RequestLevelUp::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestLevelUp msg, ServerPlayer player) {
        levelUp(player, msg.skillId());
    }

    /**
     * Whether {@code player} can level up {@code skillId} now. On the server the gate check includes
     * advancements. On the client it uses the gate status the server last sent.
     */
    public static LevelUpStatus check(Player player, String requestedSkillId) {
        if (!Configuration.isSkillLevelingEnabled()) {
            return LevelUpStatus.LEVELING_DISABLED;
        }

        String skillId = normalize(requestedSkillId);
        if (skillId.isBlank() || !Configuration.isKnownSkill(skillId)) {
            return LevelUpStatus.UNKNOWN_SKILL;
        }

        SkillModel model = SkillModel.get(player);
        int currentLevel = model.getSkillLevel(skillId);

        if (currentLevel >= Configuration.getMaxLevel()) {
            return LevelUpStatus.MAX_LEVEL;
        }

        if (!model.canSpendAnotherLevel()) {
            return LevelUpStatus.SPENT_LEVEL_CAP;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (!SkillLevelGate.check(serverPlayer, model, skillId, currentLevel).allowed()) {
                return LevelUpStatus.GATE_BLOCKED;
            }
        } else {
            GateClientCache.Entry gate = GateClientCache.get(skillId);
            if (gate != null && gate.blocked()) {
                return LevelUpStatus.GATE_BLOCKED;
            }
        }

        int cost = Configuration.calculateCostForLevel(currentLevel);
        if (!player.isCreative() && getTotalXp(player) < cost) {
            return LevelUpStatus.NOT_ENOUGH_XP;
        }

        return LevelUpStatus.ALLOWED;
    }

    /**
     * Levels up {@code skillId} when {@link #check} allows it and no {@link AllowLevelUpEvent} listener cancels it:
     * takes the XP, plays the sounds and syncs the client. When {@link #check} does not, tells the player why not.
     */
    public static LevelUpStatus levelUp(ServerPlayer player, String requestedSkillId) {
        String skillId = normalize(requestedSkillId);
        LevelUpStatus status = check(player, skillId);
        if (status == LevelUpStatus.ALLOWED && NeoForge.EVENT_BUS.post(
                new AllowLevelUpEvent(player, Configuration.canonicalSkillId(skillId))).isCanceled()) {
            return LevelUpStatus.REFUSED;
        }
        // A listener may have spent XP or changed levels, so what check() saw may no longer hold.
        if (status == LevelUpStatus.ALLOWED) {
            status = check(player, skillId);
        }
        SkillModel model = SkillModel.get(player);

        switch (status) {
            case LEVELING_DISABLED -> player.sendSystemMessage(Component.translatable("reskillable.server.disabled"));
            case MAX_LEVEL -> player.sendSystemMessage(Component.translatable("reskillable.maxlevel"));
            case SPENT_LEVEL_CAP -> player.sendSystemMessage(Component.translatable(
                    "message.reskillable.spent_level_cap",
                    model.getTotalSpentLevels(),
                    Configuration.getMaxSpendableLevels()
            ));
            case GATE_BLOCKED -> player.sendSystemMessage(
                    Component.translatable("message.reskillable.gate_blocked_short")
                            .append(Component.literal(" "))
                            .append(SkillLevelGate.check(player, model, skillId, model.getSkillLevel(skillId))
                                    .missingListComponent(player))
            );
            case NOT_ENOUGH_XP -> player.sendSystemMessage(Component.translatable("reskillable.not_enough"));
            default -> {
            }
        }

        if (status == LevelUpStatus.ALLOWED) {
            int cost = Configuration.calculateCostForLevel(model.getSkillLevel(skillId));
            if (!player.isCreative()) {
                deductXp(player, cost);
            }

            model.increaseSkillLevel(skillId, player);

            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundRegistry.LEVEL_UP_EVENT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            int newLevel = model.getSkillLevel(skillId);
            Skill builtInMilestone = Configuration.resolveBuiltInSkill(skillId);
            boolean perkMilestone = builtInMilestone != null
                    ? Configuration.isBuiltInPerkMilestone(builtInMilestone, newLevel)
                    : Configuration.isCustomPerkMilestone(skillId, newLevel);
            if (perkMilestone) {
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundRegistry.MILESTONE_EVENT.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.2F
                );
            }

            SyncToClient.send(player);
            SyncGateStatus.sendAll(player);
        }
        return status;
    }

    public static void send(String skillId) {
        PacketDistributor.sendToServer(new RequestLevelUp(normalize(skillId)));
    }

    private static String normalize(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static int getPerkStep(String skillId) {
        Skill builtIn = Configuration.resolveBuiltInSkill(skillId);
        if (builtIn != null) {
            SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(builtIn);
            return Math.max(1, bonus != null ? bonus.getPerkStep() : Configuration.getBuiltInPerkStep(builtIn));
        }

        Configuration.CustomSkillSlot custom = Configuration.findCustomSkillById(skillId);
        return Math.max(1, custom != null ? custom.getPerkStep() : 5);
    }

    /** The player's XP in points, from their XP level and progress. */
    public static int getTotalXp(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        int base = getXpForLevel(level);
        int next = getXpForLevel(level + 1);
        return base + Math.round((next - base) * progress);
    }

    private static void deductXp(ServerPlayer player, int cost) {
        int totalXp = getTotalXp(player);
        int newXp = totalXp - cost;
        player.experienceLevel = getLevelForTotalXp(newXp);
        player.experienceProgress = getProgressForLevel(newXp, player.experienceLevel);
        player.totalExperience = newXp;
    }

    private static int getXpForLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    private static int getLevelForTotalXp(int xp) {
        int level = 0;
        while (getXpForLevel(level + 1) <= xp) level++;
        return level;
    }

    private static float getProgressForLevel(int xp, int level) {
        int base = getXpForLevel(level);
        int next = getXpForLevel(level + 1);
        return (xp - base) / (float) (next - base);
    }
}