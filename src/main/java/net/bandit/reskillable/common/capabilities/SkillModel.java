package net.bandit.reskillable.common.capabilities;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.bandit.reskillable.event.SkillAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.stream.Collectors;


public class SkillModel implements INBTSerializable<CompoundTag> {
    private static final int DEFAULT_SKILL_COUNT = 8;
    private int[] skillLevels = new int[DEFAULT_SKILL_COUNT];
    private int[] skillExperience = new int[DEFAULT_SKILL_COUNT];
    private final Set<Skill> disabledPerks = new HashSet<>();
    private static final UUID GLOBAL_HEALTH_BONUS_ID = UUID.nameUUIDFromBytes("reskillable:global_health_bonus".getBytes());


    public SkillModel() {
        resetSkills();
    }

    public int getSkillLevel(Skill skill) {
        return skillLevels[skill.index];
    }

    public void setSkillLevel(Skill skill, int level) {
        skillLevels[skill.index] = Math.min(level, Configuration.getMaxLevel());
    }

    public void increaseSkillLevel(Skill skill, Player player) {
        int currentLevel = skillLevels[skill.index];
        if (currentLevel < Configuration.getMaxLevel()) {
            skillLevels[skill.index]++;
            skillExperience[skill.index] = 0;
            updateSkillAttributeBonuses(player);
            syncSkills(player);
            updateSkillAttributeBonuses(player);
            int newLevel = skillLevels[skill.index];
            if (newLevel % 5 == 0) {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                Attribute attr = bonus.getAttribute();
                if (bonus != null && attr != null) {
                    double amount = bonus.getBonusPerStep();
                    String attributeName = attr.getDescriptionId().replace("attribute.name.", "");

//                    player.displayClientMessage(Component.literal(
//                             skill.name().toLowerCase() + " level " + newLevel +
//                                    "! Bonus: +" + amount + " to " + attributeName), false);
                }
            }
        }
    }

    public void addExperience(Skill skill, int experience) {
        skillExperience[skill.index] += experience;
        checkForLevelUp(skill);
    }

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

    public boolean hasSufficientXP(Player player, Skill skill) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateCostForLevel(getSkillLevel(skill) + 1);

    }

    private int calculateTotalXPFromPlayer(Player player) {
        int level = player.experienceLevel;
        int progress = Math.round(player.experienceProgress * Configuration.calculateExperienceCost(level));
        return Configuration.getCumulativeXpForLevel(level) + progress;
    }

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
        return player.getData(SkillAttachments.SKILL_MODEL.get());
    }

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
            SyncToClient.send((ServerPlayer) player);
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


    private static final UUID[] ATTRIBUTE_MODIFIER_IDS = new UUID[Skill.values().length];

    static {
        for (int i = 0; i < ATTRIBUTE_MODIFIER_IDS.length; i++) {
            ATTRIBUTE_MODIFIER_IDS[i] = UUID.nameUUIDFromBytes(("reskillable:skill_bonus_" + i).getBytes());
        }
    }
    public void updateSkillAttributeBonuses(Player player) {
        for (SkillAttributeBonus bonus : SkillAttributeBonus.values()) {
            Holder.Reference<Attribute> attr = BuiltInRegistries.ATTRIBUTE
                    .getResourceKey(bonus.getAttribute())
                    .flatMap(BuiltInRegistries.ATTRIBUTE::getHolder)
                    .orElse(null);

            if (attr == null) continue;

            var attrInstance = player.getAttributes().getInstance(attr);
            if (attrInstance == null) continue;

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("reskillable", bonus.skill.name().toLowerCase());

            attrInstance.getModifiers().stream()
                    .filter(mod -> mod.id().equals(id))
                    .findFirst()
                    .ifPresent(attrInstance::removeModifier);

            if (isPerkEnabled(bonus.skill)) {
                int skillLevel = getSkillLevel(bonus.skill);
                int bonusSteps = skillLevel / 5;
                double totalBonus = bonusSteps * bonus.getBonusPerStep();

                if (totalBonus > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            id,
                            totalBonus,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    attrInstance.addTransientModifier(modifier);
                }
            }
        }

        handleHealthBonus(player);
    }

    private void handleHealthBonus(Player player) {
        if (!Configuration.HEALTH_BONUS.get()) return;

        int totalSkillLevels = Arrays.stream(skillLevels).sum();
        int levelsPerHeart = Configuration.LEVELS_PER_HEART.get();
        double healthPerHeart = Configuration.HEALTH_PER_HEART.get();

        int hearts = totalSkillLevels / levelsPerHeart;
        double healthBonus = hearts * healthPerHeart;

        var healthAttr = player.getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("reskillable", "health_bonus");

            healthAttr.getModifiers().stream()
                    .filter(mod -> mod.id().equals(id))
                    .findFirst()
                    .ifPresent(healthAttr::removeModifier);

            if (healthBonus > 0) {
                AttributeModifier healthModifier = new AttributeModifier(
                        id,
                        healthBonus,
                        AttributeModifier.Operation.ADD_VALUE
                );
                healthAttr.addTransientModifier(healthModifier);
            }
        }
    }

    public boolean isPerkEnabled(Skill skill) {
        return !disabledPerks.contains(skill);
    }

    public void togglePerk(Skill skill, Player player) {
        if (!disabledPerks.add(skill)) {
            disabledPerks.remove(skill);
        }
        updateSkillAttributeBonuses(player);
        syncSkills(player);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compound = new CompoundTag();
        compound.putIntArray("skillLevels", skillLevels);
        compound.putIntArray("skillExperience", skillExperience);

        List<String> disabledSkillNames = disabledPerks.stream()
                .map(skill -> skill.name())
                .collect(Collectors.toList());

        compound.putIntArray("skillLevels", skillLevels);
        compound.putIntArray("skillExperience", skillExperience);

        CompoundTag disabledTag = new CompoundTag();
        for (Skill skill : disabledPerks) {
            disabledTag.putBoolean(skill.name(), true);
        }
        compound.put("disabledPerks", disabledTag);

        return compound;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compoundTag) {
        int[] loadedLevels = compoundTag.getIntArray("skillLevels");
        int[] loadedExperience = compoundTag.getIntArray("skillExperience");

        if (loadedLevels.length == DEFAULT_SKILL_COUNT) {
            skillLevels = loadedLevels;
        }

        if (loadedExperience.length == DEFAULT_SKILL_COUNT) {
            skillExperience = loadedExperience;
        }

        disabledPerks.clear();
        if (compoundTag.contains("disabledPerks", CompoundTag.TAG_COMPOUND)) {
            CompoundTag disabledTag = compoundTag.getCompound("disabledPerks");
            for (String key : disabledTag.getAllKeys()) {
                try {
                    Skill skill = Skill.valueOf(key);
                    disabledPerks.add(skill);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
    public void readFromNetwork(SyncToClient msg) {
        for (Map.Entry<Skill, Integer> entry : msg.levels().entrySet()) {
            this.skillLevels[entry.getKey().index] = entry.getValue();
        }

        this.disabledPerks.clear();
        for (Map.Entry<Skill, Boolean> entry : msg.disabledPerks().entrySet()) {
            if (entry.getValue()) {
                this.disabledPerks.add(entry.getKey());
            }
        }
    }

}
