package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestLevelUp {
    private final int skillIndex;

    public RequestLevelUp(Skill skill) {
        this.skillIndex = skill.index;
    }

    public RequestLevelUp(FriendlyByteBuf buffer) {
        this.skillIndex = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(skillIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            // Check if skill leveling is enabled
            if (!Configuration.isSkillLevelingEnabled()) {
                player.sendSystemMessage(Component.translatable("reskillable.server.disabled"));
                return;
            }

            SkillModel skillModel = SkillModel.get(player);
            if (skillModel == null) {
                return;
            }

            if (skillIndex < 0 || skillIndex >= Skill.values().length) {
                return;
            }

            Skill skill = Skill.values()[skillIndex];
            int currentLevel = skillModel.getSkillLevel(skill);

            if (currentLevel >= Configuration.getMaxLevel()) {
                player.sendSystemMessage(Component.translatable("reskillable.maxlevel"));
                return;
            }

            int cost = Configuration.calculateCostForLevel(currentLevel + 1);


            if (player.isCreative()) {
                skillModel.increaseSkillLevel(skill, player);
                SyncToClient.send(player);
                return;
            }

            int playerTotalXp = calculateTotalXp(player);

            if (playerTotalXp >= cost) {
                deductXp(player, cost);

                int oldLevel = skillModel.getSkillLevel(skill); // Save old level
                skillModel.increaseSkillLevel(skill, player);
                int newLevel = skillModel.getSkillLevel(skill); // Get new level after increasing

                // Play level up sound
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        net.bandit.reskillable.event.SoundRegistry.LEVEL_UP_EVENT.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Play milestone sound if level is a multiple of 5
                if (newLevel % 5 == 0) {
                    player.level().playSound(
                            null,
                            player.blockPosition(),
                            net.bandit.reskillable.event.SoundRegistry.MILESTONE_EVENT.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            1.0F,
                            1.2F
                    );
                }

                SyncToClient.send(player);

        } else {
                player.sendSystemMessage(Component.translatable("reskillable.not_enough"));
            }
        });

        context.get().setPacketHandled(true);
    }

    private int calculateTotalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        int base = getCumulativeXpForLevel(level);
        int next = getCumulativeXpForLevel(level + 1);

        return base + Math.round((next - base) * progress);
    }

    private void deductXp(ServerPlayer player, int cost) {
        // Calculate the total XP the player currently has
        int totalXp = calculateTotalXp(player);

        // Subtract the cost calculated using the same logic as SkillButton
        int newTotalXp = totalXp - cost;

        // Update the player's XP level and progress based on the new total XP
        player.experienceLevel = getLevelForTotalXp(newTotalXp);
        player.experienceProgress = getProgressForLevel(newTotalXp, player.experienceLevel);
        player.totalExperience = newTotalXp;
    }


    private int getXpForNextLevel(int level) {
        return Configuration.calculateExperienceCost(level);
    }

    private int getLevelForTotalXp(int totalXp) {
        int level = 0;
        while (getCumulativeXpForLevel(level + 1) <= totalXp) {
            level++;
        }
        return level;
    }

    private float getProgressForLevel(int totalXp, int level) {
        int levelXp = getCumulativeXpForLevel(level);
        int nextLevelXp = getCumulativeXpForLevel(level + 1);
        return (totalXp - levelXp) / (float) (nextLevelXp - levelXp);
    }

    public static void send(Skill skill) {
        Reskillable.NETWORK.sendToServer(new RequestLevelUp(skill));
    }

    public static int getCumulativeXpForLevel(int level) {
        if (level <= 0) return 0;

        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
}
