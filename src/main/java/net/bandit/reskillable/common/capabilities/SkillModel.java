package net.bandit.reskillable.common.capabilities;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Configuration.CustomSkillSlot;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.SyncToClient;
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
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

public class SkillModel implements INBTSerializable<CompoundTag> {
    private static final int DEFAULT_SKILL_COUNT = 8;

    private int[] skillLevels = new int[DEFAULT_SKILL_COUNT];
    private int[] skillExperience = new int[DEFAULT_SKILL_COUNT];

    private final Map<String, Integer> customSkillLevels = new HashMap<>();
    private final Map<String, Integer> customSkillExperience = new HashMap<>();

    private final Set<Skill> disabledPerks = new HashSet<>();
    private final Set<String> disabledCustomPerks = new HashSet<>();

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

    public int getCustomSkillLevel(String customSkillId) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return 0;
        }
        return customSkillLevels.getOrDefault(customSkillId.toLowerCase(Locale.ROOT), 1);
    }

    public void setCustomSkillLevel(String customSkillId, int level) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return;
        }
        customSkillLevels.put(customSkillId.toLowerCase(Locale.ROOT), Math.min(level, Configuration.getMaxLevel()));
    }

    public int getCustomSkillExperience(String customSkillId) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return 0;
        }
        return customSkillExperience.getOrDefault(customSkillId.toLowerCase(Locale.ROOT), 0);
    }

    public void setCustomSkillExperience(String customSkillId, int experience) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return;
        }
        customSkillExperience.put(customSkillId.toLowerCase(Locale.ROOT), Math.max(0, experience));
    }

    public void increaseSkillLevel(Skill skill, Player player) {
        if (skill == null || player == null || !Configuration.isBuiltInSkillEnabled(skill)) {
            return;
        }

        if (!canSpendAnotherLevel()) {
            return;
        }

        int currentLevel = skillLevels[skill.index];
        if (currentLevel < Configuration.getMaxLevel()) {
            skillLevels[skill.index]++;
            skillExperience[skill.index] = 0;

            updateSkillAttributeBonuses(player);
            syncSkills(player);

            int newLevel = skillLevels[skill.index];
            SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
            if (bonus != null && newLevel % bonus.getPerkStep() == 0) {
                Attribute attr = bonus.getAttribute();
                if (attr != null) {
                    double amount = bonus.getBonusPerStep();
                    String attributeName = attr.getDescriptionId().replace("attribute.name.", "");
                }
            }
        }
    }

    public void increaseCustomSkillLevel(String customSkillId, Player player) {
        if (customSkillId == null || customSkillId.isBlank() || player == null) {
            return;
        }

        if (!canSpendAnotherLevel()) {
            return;
        }

        String id = customSkillId.toLowerCase(Locale.ROOT);
        int currentLevel = getCustomSkillLevel(id);
        if (currentLevel < Configuration.getMaxLevel()) {
            customSkillLevels.put(id, currentLevel + 1);
            customSkillExperience.put(id, 0);
            updateSkillAttributeBonuses(player);
            syncSkills(player);
        }
    }

    public void addExperience(Skill skill, int experience) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) {
            return;
        }
        skillExperience[skill.index] += experience;
        checkForLevelUp(skill);
    }

    public void addCustomExperience(String customSkillId, int experience) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return;
        }

        String id = customSkillId.toLowerCase(Locale.ROOT);
        customSkillExperience.put(id, getCustomSkillExperience(id) + experience);
        checkForCustomLevelUp(id);
    }

    private void checkForLevelUp(Skill skill) {
        int level = skillLevels[skill.index];
        int xp = skillExperience[skill.index];
        int spentLevels = getTotalSpentLevels();
        int maxSpent = Configuration.getMaxSpendableLevels();

        while (level < Configuration.getMaxLevel()
                && xp >= Configuration.calculateExperienceCost(level)
                && (maxSpent < 0 || spentLevels < maxSpent)) {
            xp -= Configuration.calculateExperienceCost(level);
            level++;
            spentLevels++;
        }

        skillExperience[skill.index] = xp;
        skillLevels[skill.index] = level;
    }

    private void checkForCustomLevelUp(String customSkillId) {
        int level = getCustomSkillLevel(customSkillId);
        int xp = getCustomSkillExperience(customSkillId);
        int spentLevels = getTotalSpentLevels();
        int maxSpent = Configuration.getMaxSpendableLevels();

        while (level < Configuration.getMaxLevel()
                && xp >= Configuration.calculateExperienceCost(level)
                && (maxSpent < 0 || spentLevels < maxSpent)) {
            xp -= Configuration.calculateExperienceCost(level);
            level++;
            spentLevels++;
        }

        customSkillLevels.put(customSkillId, level);
        customSkillExperience.put(customSkillId, xp);
    }

    public boolean hasSufficientXP(Player player, Skill skill) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateCostForLevel(getSkillLevel(skill) + 1);
    }

    public boolean hasSufficientXPForCustomSkill(Player player, String customSkillId) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateCostForLevel(getCustomSkillLevel(customSkillId) + 1);
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
            if (!meetsRequirement(requirement)) {
                unmetRequirements.add(requirement);
            }
        }

        if (!unmetRequirements.isEmpty()) {
            sendSkillRequirementMessage(player, type, unmetRequirements);
            return false;
        }

        return true;
    }

    private boolean meetsRequirement(Requirement requirement) {
        if (requirement == null) {
            return true;
        }

        if (requirement.isVanillaSkill()) {
            return getSkillLevel(requirement.skill) >= requirement.level;
        }

        if (requirement.isCustomSkill()) {
            return getCustomSkillLevel(requirement.customSkillId) >= requirement.level;
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
            Component translatedSkillName;

            if (req.isVanillaSkill()) {
                String configuredName = Configuration.getBuiltInSkillDisplayName(req.skill);
                translatedSkillName = configuredName.isBlank()
                        ? Component.translatable(req.skill.getDisplayName())
                        : Component.literal(configuredName);
            } else if (req.isCustomSkill()) {
                CustomSkillSlot slot = Configuration.findCustomSkillById(req.customSkillId);
                String displayName = slot != null ? slot.getDisplayName() : req.customSkillId;
                translatedSkillName = Component.literal(displayName);
            } else {
                translatedSkillName = Component.literal("Unknown");
            }

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

        customSkillLevels.clear();
        customSkillExperience.clear();

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                customSkillLevels.put(slot.getId(), 1);
                customSkillExperience.put(slot.getId(), 0);
            }
        }
    }

    public void cloneFrom(SkillModel source) {
        this.skillLevels = source.skillLevels.clone();
        this.skillExperience = source.skillExperience.clone();

        this.customSkillLevels.clear();
        this.customSkillLevels.putAll(source.customSkillLevels);

        this.customSkillExperience.clear();
        this.customSkillExperience.putAll(source.customSkillExperience);

        this.disabledPerks.clear();
        this.disabledPerks.addAll(source.disabledPerks);

        this.disabledCustomPerks.clear();
        this.disabledCustomPerks.addAll(source.disabledCustomPerks);

    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = new CompoundTag();
        compound.putIntArray("skillLevels", skillLevels);
        compound.putIntArray("skillExperience", skillExperience);

        CompoundTag customLevelsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : customSkillLevels.entrySet()) {
            customLevelsTag.putInt(entry.getKey(), entry.getValue());
        }
        compound.put("customSkillLevels", customLevelsTag);

        CompoundTag customExperienceTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : customSkillExperience.entrySet()) {
            customExperienceTag.putInt(entry.getKey(), entry.getValue());
        }
        compound.put("customSkillExperience", customExperienceTag);

        CompoundTag disabledTag = new CompoundTag();
        for (Skill skill : disabledPerks) {
            disabledTag.putBoolean(skill.name(), true);
        }
        compound.put("disabledPerks", disabledTag);

        CompoundTag disabledCustomTag = new CompoundTag();
        for (String skillId : disabledCustomPerks) {
            disabledCustomTag.putBoolean(skillId, true);
        }
        compound.put("disabledCustomPerks", disabledCustomTag);


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

        customSkillLevels.clear();
        if (nbt.contains("customSkillLevels", CompoundTag.TAG_COMPOUND)) {
            CompoundTag customLevelsTag = nbt.getCompound("customSkillLevels");
            for (String key : customLevelsTag.getAllKeys()) {
                customSkillLevels.put(key.toLowerCase(Locale.ROOT), customLevelsTag.getInt(key));
            }
        }

        customSkillExperience.clear();
        if (nbt.contains("customSkillExperience", CompoundTag.TAG_COMPOUND)) {
            CompoundTag customExperienceTag = nbt.getCompound("customSkillExperience");
            for (String key : customExperienceTag.getAllKeys()) {
                customSkillExperience.put(key.toLowerCase(Locale.ROOT), customExperienceTag.getInt(key));
            }
        }

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                customSkillLevels.putIfAbsent(slot.getId(), 1);
                customSkillExperience.putIfAbsent(slot.getId(), 0);
            }
        }
        disabledCustomPerks.clear();
        if (nbt.contains("disabledCustomPerks", CompoundTag.TAG_COMPOUND)) {
            CompoundTag disabledCustomTag = nbt.getCompound("disabledCustomPerks");
            for (String key : disabledCustomTag.getAllKeys()) {
                disabledCustomPerks.add(key.toLowerCase(Locale.ROOT));
            }
        }

        disabledPerks.clear();
        if (nbt.contains("disabledPerks", CompoundTag.TAG_COMPOUND)) {
            CompoundTag disabledTag = nbt.getCompound("disabledPerks");
            for (String key : disabledTag.getAllKeys()) {
                try {
                    Skill skill = Skill.valueOf(key);
                    disabledPerks.add(skill);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private final Map<UUID, Attribute> appliedBuiltInPerkAttributes = new HashMap<>();
    private final Map<UUID, Attribute> appliedCustomPerkAttributes = new HashMap<>();

    private static final UUID[] ATTRIBUTE_MODIFIER_IDS = new UUID[Skill.values().length];

    static {
        for (int i = 0; i < ATTRIBUTE_MODIFIER_IDS.length; i++) {
            ATTRIBUTE_MODIFIER_IDS[i] = UUID.nameUUIDFromBytes(("reskillable:skill_bonus_" + i).getBytes());
        }
    }
    private static UUID getCustomPerkModifierId(String customSkillId) {
        return UUID.nameUUIDFromBytes(("reskillable:custom_skill_bonus:" + customSkillId.toLowerCase(Locale.ROOT)).getBytes());
    }
    public void updateSkillAttributeBonuses(Player player) {
        clearAppliedBuiltInPerkModifiers(player);
        clearAppliedCustomPerkModifiers(player);

        // Built-in perks. Explicit JSON attributes replace the original perk.
        for (SkillAttributeBonus bonus : SkillAttributeBonus.values()) {
            Skill skill = bonus.skill;
            if (!Configuration.isBuiltInSkillEnabled(skill) || !isPerkEnabled(skill)) continue;

            Configuration.BuiltInSkillSlot slot = Configuration.getBuiltInSkill(skill);
            if (slot != null && slot.hasPerkOverride()) {
                if (slot.perkAttributes != null) {
                    for (int i = 0; i < slot.perkAttributes.size(); i++) {
                        Configuration.PerkAttributeDefinition definition = slot.perkAttributes.get(i);
                        if (definition == null) continue;
                        applyBuiltInAttributeModifier(player, skill, i,
                                definition.getResolvedAttribute(), definition.getResolvedOperation(),
                                definition.getAmountPerStep(), definition.getPerkStep());
                    }
                } else {
                    applyBuiltInAttributeModifier(player, skill, 0,
                            slot.getResolvedLegacySinglePerkAttribute(), bonus.getOperation(),
                            bonus.getBonusPerStep(), bonus.getPerkStep());
                }
                continue;
            }

            applyBuiltInAttributeModifier(player, skill, -1,
                    bonus.getAttribute(), bonus.getOperation(), bonus.getBonusPerStep(), bonus.getPerkStep());
        }

        // Custom perks. perkAttributes, when present, replaces the legacy
        // single custom perk and can grant multiple attributes.
        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled() || !isCustomPerkEnabled(slot.getId())) {
                continue;
            }

            String skillId = slot.getId().toLowerCase(Locale.ROOT);
            if (slot.perkAttributes != null) {
                for (int i = 0; i < slot.perkAttributes.size(); i++) {
                    Configuration.PerkAttributeDefinition definition = slot.perkAttributes.get(i);
                    if (definition == null) continue;
                    applyCustomAttributeModifier(player, skillId, i,
                            definition.getResolvedAttribute(), definition.getResolvedOperation(),
                            definition.getAmountPerStep(), definition.getPerkStep());
                }
                continue;
            }

            applyCustomAttributeModifier(player, skillId, -1,
                    slot.getResolvedPerkAttribute(), slot.getResolvedPerkOperation(),
                    slot.getPerkAmountPerStep(), slot.getPerkStep());
        }

        handleHealthBonus(player);
    }

    private void clearAppliedBuiltInPerkModifiers(Player player) {
        for (Map.Entry<UUID, Attribute> entry : appliedBuiltInPerkAttributes.entrySet()) {
            var instance = player.getAttribute(entry.getValue());
            if (instance != null) instance.removeModifier(entry.getKey());
        }
        appliedBuiltInPerkAttributes.clear();
    }

    private void applyBuiltInAttributeModifier(Player player, Skill skill, int index, Attribute attribute,
                                               AttributeModifier.Operation operation, double amountPerStep, int perkStep) {
        if (attribute == null || amountPerStep <= 0.0) return;
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        int steps = getSkillLevel(skill) / Math.max(1, perkStep);
        double totalBonus = steps * amountPerStep;
        if (totalBonus <= 0.0) return;
        UUID modifierId = index < 0
                ? UUID.nameUUIDFromBytes(("reskillable:" + skill.name().toLowerCase(Locale.ROOT)).getBytes())
                : UUID.nameUUIDFromBytes(("reskillable:built_in:" + skill.name().toLowerCase(Locale.ROOT) + ":" + index).getBytes());
        instance.removeModifier(modifierId);
        instance.addTransientModifier(new AttributeModifier(
                modifierId,
                "Reskillable Built-In Bonus: " + skill.name() + (index < 0 ? "" : " #" + index),
                totalBonus,
                operation
        ));
        appliedBuiltInPerkAttributes.put(modifierId, attribute);
    }

    private void clearAppliedCustomPerkModifiers(Player player) {
        for (Map.Entry<UUID, Attribute> entry : appliedCustomPerkAttributes.entrySet()) {
            var instance = player.getAttribute(entry.getValue());
            if (instance != null) instance.removeModifier(entry.getKey());
        }
        appliedCustomPerkAttributes.clear();
    }

    private void applyCustomAttributeModifier(Player player, String skillId, int index, Attribute attribute,
                                              AttributeModifier.Operation operation, double amountPerStep, int perkStep) {
        if (attribute == null || amountPerStep <= 0.0) return;
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        int steps = getCustomSkillLevel(skillId) / Math.max(1, perkStep);
        double totalBonus = steps * amountPerStep;
        if (totalBonus <= 0.0) return;
        UUID modifierId = index < 0
                ? getCustomPerkModifierId(skillId)
                : UUID.nameUUIDFromBytes(("reskillable:custom_skill_bonus:" + skillId + ":" + index).getBytes());
        instance.removeModifier(modifierId);
        instance.addTransientModifier(new AttributeModifier(
                modifierId,
                "Reskillable Custom Bonus: " + skillId + (index < 0 ? "" : " #" + index),
                totalBonus,
                operation
        ));
        appliedCustomPerkAttributes.put(modifierId, attribute);
    }

    private void handleHealthBonus(Player player) {
        if (!Configuration.HEALTH_BONUS.get()) {
            return;
        }

        int totalSkillLevels = 0;
        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            totalSkillLevels += getSkillLevel(skill);
        }

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                totalSkillLevels += getCustomSkillLevel(slot.getId());
            }
        }

        int levelsPerHeart = Configuration.LEVELS_PER_HEART.get();
        double healthPerHeart = Configuration.HEALTH_PER_HEART.get();

        int hearts = totalSkillLevels / levelsPerHeart;
        double newBonus = hearts * healthPerHeart;

        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(GLOBAL_HEALTH_BONUS_ID);

            if (newBonus > 0) {
                AttributeModifier healthModifier = new AttributeModifier(
                        GLOBAL_HEALTH_BONUS_ID,
                        "Reskillable Total Level Bonus",
                        newBonus,
                        AttributeModifier.Operation.ADDITION
                );
                healthAttr.addTransientModifier(healthModifier);
            }

            double max = player.getMaxHealth();
            if (player.getHealth() > max) {
                player.setHealth((float) max);
            }

            if (player.getHealth() == 20.0 && max > 20.0) {
                player.setHealth((float) max);
            }
        }
    }

    public boolean isPerkEnabled(Skill skill) {
        return skill != null && Configuration.isBuiltInSkillEnabled(skill) && !disabledPerks.contains(skill);
    }
    public boolean isCustomPerkEnabled(String customSkillId) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return true;
        }
        return !disabledCustomPerks.contains(customSkillId.toLowerCase(Locale.ROOT));
    }

    public void toggleCustomPerk(String customSkillId, Player player) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return;
        }

        String id = customSkillId.toLowerCase(Locale.ROOT);
        if (!disabledCustomPerks.add(id)) {
            disabledCustomPerks.remove(id);
        }

        updateSkillAttributeBonuses(player);
        syncSkills(player);
    }


    public void togglePerk(Skill skill, Player player) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) {
            return;
        }
        if (!disabledPerks.add(skill)) {
            disabledPerks.remove(skill);
        }
        updateSkillAttributeBonuses(player);
        syncSkills(player);
    }
    public int getTotalSpentLevels() {
        int total = 0;

        // Enabled built-in skills. Disabled skills keep their saved data for compatibility,
        // but do not count toward caps/health/progression totals.
        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            total += Math.max(0, getSkillLevel(skill) - 1);
        }

        // Custom skills
        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                total += Math.max(0, getCustomSkillLevel(slot.getId()) - 1);
            }
        }

        return total;
    }

    public boolean hasSpentLevelCap() {
        int max = Configuration.getMaxSpendableLevels();
        return max >= 0 && getTotalSpentLevels() >= max;
    }

    public boolean canSpendAnotherLevel() {
        int max = Configuration.getMaxSpendableLevels();
        return max < 0 || getTotalSpentLevels() < max;
    }

    public int getRemainingSpendableLevels() {
        int max = Configuration.getMaxSpendableLevels();
        if (max < 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, max - getTotalSpentLevels());
    }
    public int getRefundForLevel(int currentLevel) {
        int refund = 0;

        for (int lvl = 1; lvl < currentLevel; lvl++) {
            refund += Configuration.calculateCostForLevel(lvl);
        }

        return refund;
    }

    public int getTotalRespecRefund() {
        int refund = 0;

        for (Skill skill : Skill.values()) {
            refund += getRefundForLevel(getSkillLevel(skill));
        }

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            // Disabled custom skills can still have saved player investment. Refund it
            // before reset so temporarily hiding a skill never destroys spent XP.
            if (slot != null && !slot.getId().isBlank()) {
                refund += getRefundForLevel(getCustomSkillLevel(slot.getId()));
            }
        }

        return refund;
    }

    public int resetAllSkillsAndReturnRefund(Player player) {
        int refund = getTotalRespecRefund();

        for (int i = 0; i < DEFAULT_SKILL_COUNT; i++) {
            skillLevels[i] = 1;
            skillExperience[i] = 0;
        }

        customSkillLevels.clear();
        customSkillExperience.clear();

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                customSkillLevels.put(slot.getId(), 1);
                customSkillExperience.put(slot.getId(), 0);
            }
        }

        disabledPerks.clear();
        disabledCustomPerks.clear();

        updateSkillAttributeBonuses(player);
        syncSkills(player);

        return refund;
    }

}
