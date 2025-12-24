package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.network.PacketDistributor;
import net.bandit.reskillable.common.gating.SkillLevelGate;



public record RequestLevelUp(int skillIndex) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "request_levelup");

    public static final Type<RequestLevelUp> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RequestLevelUp> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RequestLevelUp::skillIndex,
                    RequestLevelUp::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestLevelUp msg, ServerPlayer player) {
        if (!Configuration.isSkillLevelingEnabled()) {
            player.sendSystemMessage(Component.translatable("reskillable.server.disabled"));
            return;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null || msg.skillIndex < 0 || msg.skillIndex >= Skill.values().length) return;

        Skill skill = Skill.values()[msg.skillIndex];
        int currentLevel = model.getSkillLevel(skill);

        if (currentLevel >= Configuration.getMaxLevel()) {
            player.sendSystemMessage(Component.translatable("reskillable.maxlevel"));
            return;
        }
        SkillLevelGate.GateResult gate = SkillLevelGate.check(player, model, skill, currentLevel);

        if (!gate.allowed()) {
            player.sendSystemMessage(
                    Component.translatable("message.reskillable.gate_blocked_short")
                            .append(Component.literal(" "))
                            .append(gate.missingListComponent())
            );
            return;
        }

        int cost = Configuration.calculateCostForLevel(currentLevel);
        int totalXp = getTotalXp(player);

        if (player.isCreative() || totalXp >= cost) {
            if (!player.isCreative()) deductXp(player, cost);

            model.increaseSkillLevel(skill, player);

            player.level().playSound(null, player.blockPosition(), SoundRegistry.LEVEL_UP_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            if (model.getSkillLevel(skill) % 5 == 0) {
                player.level().playSound(null, player.blockPosition(), SoundRegistry.MILESTONE_EVENT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
            }

            SyncToClient.send(player);
            SyncGateStatus.sendAll(player);
        } else {
            player.sendSystemMessage(Component.translatable("reskillable.not_enough"));
        }
    }

    private static int getTotalXp(ServerPlayer player) {
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
    public static void send(Skill skill) {
        PacketDistributor.sendToServer(new RequestLevelUp(skill.index));
    }
}