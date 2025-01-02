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
                player.sendSystemMessage(Component.literal("Skill is already at the maximum level."));
                return;
            }

            // Calculate cost using Configuration's multiplier
            int cost = Configuration.calculateCostForLevel(currentLevel);

            if (player.isCreative()) {
                skillModel.increaseSkillLevel(skill);
                SyncToClient.send(player);
                return;
            }

            int playerTotalXp = calculateTotalXp(player);

            if (playerTotalXp >= cost) {
                deductXp(player, cost);
                skillModel.increaseSkillLevel(skill);
                SyncToClient.send(player);
            } else {
                player.sendSystemMessage(Component.literal("Not enough XP to level up this skill."));
            }
        });

        context.get().setPacketHandled(true);
    }

    private int calculateTotalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        int progress = Math.round(player.experienceProgress * getXpForNextLevel(level));
        return getCumulativeXpForLevel(level) + progress;
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
        double multiplier = Configuration.getXpScalingMultiplier();

        if (level <= 0) return 0;

        if (level <= 16) {
            return (int) ((level * (level + 1)) / 2 * 2 + 7 * level * multiplier);
        } else if (level <= 31) {
            return (int) ((2.5 * level * level - 40.5 * level + 360) * multiplier);
        } else {
            return (int) ((4.5 * level * level - 162.5 * level + 2220) * multiplier);
        }
    }

    private int calculateCost(int level) {
        return Configuration.calculateExperienceCost(level);
    }


}
