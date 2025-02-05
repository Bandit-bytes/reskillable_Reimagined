package net.bandit.reskillable.common.capabilities;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.network.SyncToClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class SkillModel implements INBTSerializable<CompoundTag> {
    private static final int DEFAULT_SKILL_COUNT = 8;
    private int[] skillLevels = new int[DEFAULT_SKILL_COUNT];
    private int[] skillExperience = new int[DEFAULT_SKILL_COUNT];

    public SkillModel() {
        resetSkills();
    }

    // Get Skill Level
    public int getSkillLevel(Skill skill) {
        return skillLevels[skill.index];
    }

    // Set Skill Level
    public void setSkillLevel(Skill skill, int level) {
        skillLevels[skill.index] = Math.min(level, Configuration.getMaxLevel());
    }

    // Increase Skill Level
    public void increaseSkillLevel(Skill skill, Player player) {
        int currentLevel = skillLevels[skill.index];
        if (currentLevel < Configuration.getMaxLevel()) {
            skillLevels[skill.index]++;
            skillExperience[skill.index] = 0;
            syncSkills(player);
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
            xp -= Configuration.calculateExperienceCost(level);
            level++;
        }

        skillExperience[skill.index] = xp;
        skillLevels[skill.index] = level;
    }

    // Check if a Player Has Sufficient XP
    public boolean hasSufficientXP(Player player, Skill skill) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateExperienceCost(getSkillLevel(skill));
    }

    // Calculate Total XP for a Player
    private int calculateTotalXPFromPlayer(Player player) {
        int level = player.experienceLevel;
        int progress = Math.round(player.experienceProgress * Configuration.calculateExperienceCost(level));
        return Configuration.getCumulativeXpForLevel(level) + progress;
    }

    // Can Use Item/Block/Entity
    public boolean canUseItem(Player player, ItemStack item) {
        return canUse(player, item.getItem().builtInRegistryHolder().key().location());
    }

    public boolean canUseBlock(Player player, Block block) {
        return canUse(player, block.builtInRegistryHolder().key().location());
    }

    public boolean canUseEntity(Player player, Entity entity) {
        return canUse(player, entity.getType().builtInRegistryHolder().key().location());
    }

    private boolean canUse(Player player, ResourceLocation resource) {
        return checkRequirements(player, resource, RequirementType.USE);
    }
    private boolean checkRequirements(Player player, ResourceLocation resource, RequirementType type) {
        Requirement[] requirements = type.getRequirements(resource);
        if (requirements == null || requirements.length == 0) {
            return true;
        }

        List<Requirement> unmetRequirements = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (getSkillLevel(requirement.skill) < requirement.level) {
                unmetRequirements.add(requirement);
            }
        }

        if (!unmetRequirements.isEmpty()) {
            sendSkillRequirementMessage(player, type, unmetRequirements);
            return false;
        }

        return true;
    }
    private void sendSkillRequirementMessage(Player player, RequirementType type, List<Requirement> unmetRequirements) {
        String translationKey = switch (type) {
            case ATTACK -> "message.reskillable.requirement.attack";
            case CRAFT -> "message.reskillable.requirement.craft";
            case USE -> "message.reskillable.requirement.use";
        };

        List<Component> formattedRequirements = new ArrayList<>();
        for (Requirement req : unmetRequirements) {
            String skillTranslationKey = "skill." + req.skill.name().toLowerCase(); // Ensure key matches lang file
            Component translatedSkillName = Component.translatable(skillTranslationKey); // Retrieve translated name
            formattedRequirements.add(
                    Component.literal("")
                            .append(translatedSkillName)
                            .append(" level " + req.level)
            );
        }

        Component joinedRequirements = Component.literal(" ")
                .append(Component.literal(String.join(", ",
                        formattedRequirements.stream()
                                .map(Component::getString)
                                .collect(Collectors.toList()))
                ));
        Component message = Component.translatable(translationKey, joinedRequirements);
        player.displayClientMessage(message, true);
    }


    public static SkillModel get(Player player) {
        return player.getCapability(SkillCapability.INSTANCE).orElse(null);
    }

    // Additional Can-Use Methods
    public boolean canCraftItem(Player player, ItemStack stack) {
        ResourceLocation resource = stack.getItem().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.CRAFT);
    }

    public boolean canAttackEntity(Player player, Entity target) {
        ResourceLocation resource = target.getType().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.ATTACK);
    }

    public void syncSkills(Player player) {
        if (player instanceof ServerPlayer) {
            SyncToClient.send(player);
        }
    }

    public void resetSkills() {
        for (int i = 0; i < DEFAULT_SKILL_COUNT; i++) {
            skillLevels[i] = 1;
            skillExperience[i] = 0;
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
        }

        if (loadedExperience.length == DEFAULT_SKILL_COUNT) {
            skillExperience = loadedExperience;
        }
    }
}
