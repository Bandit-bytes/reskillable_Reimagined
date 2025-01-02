package net.bandit.reskillable.common.capabilities;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.INBTSerializable;

public class SkillModel implements INBTSerializable<CompoundTag> {
    public int[] skillLevels = new int[]{1, 1, 1, 1, 1, 1, 1, 1};
    private int[] skillExperience = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
    private static final int DEFAULT_SKILL_COUNT = 8;

    // Get Skill Level
    public int getSkillLevel(Skill skill) {
        return skillLevels[skill.index];
    }

    // Set Skill Level
    public void setSkillLevel(Skill skill, int level) {
        skillLevels[skill.index] = Math.min(level, Configuration.getMaxLevel());
    }

    // Increase Skill Level
    public void increaseSkillLevel(Skill skill) {
        int currentLevel = skillLevels[skill.index];
        if (currentLevel < Configuration.getMaxLevel()) {
            skillLevels[skill.index]++;
            skillExperience[skill.index] = 0; // Reset XP for the new level
        }
    }

    // Add Experience to Skill
    public void addExperience(Skill skill, int experience) {
        skillExperience[skill.index] += experience;
        checkForLevelUp(skill);
    }

    // Check for Level Up
    private void checkForLevelUp(Skill skill) {
        int level = skillLevels[skill.index];
        int xp = skillExperience[skill.index];

        while (level < Configuration.getMaxLevel() && xp >= Configuration.calculateExperienceCost(level)) {
            int cost = Configuration.calculateExperienceCost(level);
            xp -= cost;
            level++;
        }

        skillExperience[skill.index] = xp;
        skillLevels[skill.index] = level;
    }

    public boolean hasSufficientXP(Player player, Skill skill) {
        int currentLevel = getSkillLevel(skill);
        int xpCost = Configuration.calculateExperienceCost(currentLevel);

        if (player.isCreative()) {
            return true; // Creative players always have sufficient XP
        }

        if (player.level().isClientSide) {
            return true; // Avoid logic execution on client
        }

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= xpCost;
    }


    // Calculate Total XP for a Player
    private int calculateTotalXPFromPlayer(Player player) {
        int level = player.experienceLevel;
        int progress = Math.round(player.experienceProgress * Configuration.calculateExperienceCost(level));
        return Configuration.getCumulativeXpForLevel(level) + progress;
    }

    // Can Use Item
    public boolean canUseItem(Player player, ItemStack item) {
        return canUse(player, item.getItem().builtInRegistryHolder().key().location());
    }

    // Can Use Block
    public boolean canUseBlock(Player player, Block block) {
        return canUse(player, block.builtInRegistryHolder().key().location());
    }

    // Can Use Entity
    public boolean canUseEntity(Player player, Entity entity) {
        return canUse(player, entity.getType().builtInRegistryHolder().key().location());
    }

    // Check if Player Can Use
    private boolean canUse(Player player, ResourceLocation resource) {
        return checkRequirements(player, resource, RequirementType.USE);
    }
    private boolean checkRequirements(Player player, ResourceLocation resource, RequirementType type) {
        Requirement[] requirements = type.getRequirements(resource);
        if (requirements != null) {
            for (Requirement requirement : requirements) {
                if (getSkillLevel(requirement.skill) < requirement.level) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        String message = switch (type) {
                            case ATTACK -> "You are not strong enough to attack this creature.";
                            case CRAFT -> "You are not skilled enough to craft this item.";
                            case USE -> "You are not skilled enough to use this item.";
                        };
                        serverPlayer.sendSystemMessage(Component.literal(message));
                    }
                    return false;
                }
            }
        }
        return true;
    }

    // Get Player Skills
    public static SkillModel get(Player player) {
        return player.getCapability(SkillCapability.INSTANCE).orElseThrow(() ->
                new IllegalArgumentException("Player " + player.getName().getContents() + " does not have a Skill Model!")
        );
    }

//    // Serialize and Deserialize
//    @Override
//    public CompoundTag serializeNBT() {
//        CompoundTag compound = new CompoundTag();
//        compound.putIntArray("skillLevels", skillLevels);
//        compound.putIntArray("skillExperience", skillExperience);
//        return compound;
//    }
//
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        skillLevels = nbt.getIntArray("skillLevels");
//        skillExperience = nbt.getIntArray("skillExperience");
//    }

    // Additional Can-Use Methods
    public boolean canCraftItem(Player player, ItemStack stack) {
        ResourceLocation resource = stack.getItem().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.CRAFT);
    }

    public boolean canAttackEntity(Player player, Entity target) {
        ResourceLocation resource = target.getType().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.ATTACK);
    }
    public void resetSkills() {
        for (int i = 0; i < DEFAULT_SKILL_COUNT; i++) {
            this.skillLevels[i] = 1;
            this.skillExperience[i] = 0;
        }
    }

    public void cloneFrom(SkillModel source) {
        this.skillLevels = source.skillLevels.clone();
        this.skillExperience = source.skillExperience.clone();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = new CompoundTag();
        compound.putIntArray("skillLevels", skillLevels);
        compound.putIntArray("skillExperience", skillExperience);
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        int[] loadedLevels = nbt.getIntArray("skillLevels");
        int[] loadedExperience = nbt.getIntArray("skillExperience");

        if (loadedLevels.length == DEFAULT_SKILL_COUNT) {
            skillLevels = loadedLevels;
        } else {
            resetSkills();
        }

        if (loadedExperience.length == DEFAULT_SKILL_COUNT) {
            skillExperience = loadedExperience;
        } else {
            skillExperience = new int[DEFAULT_SKILL_COUNT];
        }
    }


}
