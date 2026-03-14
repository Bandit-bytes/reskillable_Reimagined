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
                if (bonus != null) {
                    Attribute attr = bonus.getAttribute();
                    if (attr != null) {
                        double amount = bonus.getBonusPerStep();
                        String attributeName = attr.getDescriptionId().replace("attribute.name.", "");
                    }
                }
            }
        }
    }

    public void increaseCustomSkillLevel(String customSkillId, Player player) {
        if (customSkillId == null || customSkillId.isBlank()) {
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

        while (level < Configuration.getMaxLevel() && xp >= Configuration.calculateExperienceCost(level)) {
            xp -= Configuration.calculateExperienceCost(level);
            level++;
        }

        skillExperience[skill.index] = xp;
        skillLevels[skill.index] = level;
    }

    private void checkForCustomLevelUp(String customSkillId) {
        int level = getCustomSkillLevel(customSkillId);
        int xp = getCustomSkillExperience(customSkillId);

        while (level < Configuration.getMaxLevel() && xp >= Configuration.calculateExperienceCost(level)) {
            xp -= Configuration.calculateExperienceCost(level);
            level++;
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
                String skillTranslationKey = "skill." + req.skill.name().toLowerCase(Locale.ROOT);
                translatedSkillName = Component.translatable(skillTranslationKey);
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
        // Built-in perks
        for (SkillAttributeBonus bonus : SkillAttributeBonus.values()) {
            Attribute attr = bonus.getAttribute();
            if (attr == null) continue;

            UUID modifierId = UUID.nameUUIDFromBytes(("reskillable:" + bonus.skill.name().toLowerCase(Locale.ROOT)).getBytes());
            var attrInstance = player.getAttribute(attr);
            if (attrInstance == null) continue;

            attrInstance.getModifiers().stream()
                    .filter(mod -> mod.getId().equals(modifierId))
                    .toList()
                    .forEach(mod -> attrInstance.removeModifier(mod.getId()));

            if (isPerkEnabled(bonus.skill)) {
                int skillLevel = getSkillLevel(bonus.skill);
                int bonusSteps = skillLevel / 5;
                double totalBonus = bonusSteps * bonus.getBonusPerStep();

                if (totalBonus > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            modifierId,
                            "Reskillable Bonus: " + bonus.skill.name(),
                            totalBonus,
                            bonus.getOperation()
                    );
                    attrInstance.addTransientModifier(modifier);
                }
            }
        }

        // Custom perks
        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled()) {
                continue;
            }

            Attribute attr = slot.getResolvedPerkAttribute();
            if (attr == null) {
                continue;
            }

            var attrInstance = player.getAttribute(attr);
            if (attrInstance == null) {
                continue;
            }

            UUID modifierId = getCustomPerkModifierId(slot.getId());

            attrInstance.getModifiers().stream()
                    .filter(mod -> mod.getId().equals(modifierId))
                    .toList()
                    .forEach(mod -> attrInstance.removeModifier(mod.getId()));

            if (!slot.hasPerk() || !isCustomPerkEnabled(slot.getId())) {
                continue;
            }


            int skillLevel = getCustomSkillLevel(slot.getId());
            int bonusSteps = skillLevel / slot.getPerkStep();
            double totalBonus = bonusSteps * slot.getPerkAmountPerStep();

            if (totalBonus > 0) {
                AttributeModifier modifier = new AttributeModifier(
                        modifierId,
                        "Reskillable Custom Bonus: " + slot.getId(),
                        totalBonus,
                        slot.getResolvedPerkOperation()
                );
                attrInstance.addTransientModifier(modifier);
            }
        }

        handleHealthBonus(player);
    }

    private void handleHealthBonus(Player player) {
        if (!Configuration.HEALTH_BONUS.get()) {
            return;
        }

        int totalSkillLevels = Arrays.stream(skillLevels).sum();

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
        return !disabledPerks.contains(skill);
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
        if (!disabledPerks.add(skill)) {
            disabledPerks.remove(skill);
        }
        updateSkillAttributeBonuses(player);
        syncSkills(player);
    }
}
