package net.bandit.reskillable.common.capabilities;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.RequirementType;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.bandit.reskillable.event.SkillAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SkillModel {
    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<String, Integer> skillExperience = new HashMap<>();
    private final Set<String> disabledPerks = new HashSet<>();

    public static final Codec<SkillModel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("skillLevels", Map.of())
                    .forGetter(model -> model.skillLevels),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("skillExperience", Map.of())
                    .forGetter(model -> model.skillExperience),
            Codec.STRING.listOf()
                    .optionalFieldOf("disabledPerks", List.of())
                    .forGetter(model -> new ArrayList<>(model.disabledPerks))
    ).apply(instance, SkillModel::fromCodec));

    private static SkillModel fromCodec(Map<String, Integer> levels,
                                        Map<String, Integer> experience,
                                        List<String> disabled) {
        SkillModel model = new SkillModel();
        model.skillLevels.clear();
        model.skillExperience.clear();
        model.disabledPerks.clear();
        model.resetSkills();

        levels.forEach((id, level) -> model.skillLevels.put(normalizeSkillId(id), level));
        experience.forEach((id, xp) -> model.skillExperience.put(normalizeSkillId(id), xp));
        disabled.forEach(id -> model.disabledPerks.add(normalizeSkillId(id)));
        return model;
    }

    public SkillModel() {
        resetSkills();
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Built-in skills keep their original map/NBT key even when their public ID is renamed.
     * This preserves existing player data and prevents reordering/renaming from creating a new skill.
     */
    private static String toStorageSkillId(String skillId) {
        String normalized = normalizeSkillId(skillId);
        if (normalized.isBlank()) return "";

        Skill legacy = Skill.fromString(normalized);
        if (legacy != null) {
            return legacy.getSerializedName();
        }

        return Configuration.toStorageSkillId(normalized);
    }

    public static SkillModel get(Player player) {
        return player.getAttachedOrCreate(SkillAttachments.SKILL_MODEL);
    }

    public void resetSkills() {
        skillLevels.clear();
        skillExperience.clear();
        disabledPerks.clear();

        for (Skill skill : Skill.values()) {
            String id = skill.getSerializedName();
            skillLevels.put(id, 1);
            skillExperience.put(id, 0);
        }

        for (Configuration.CustomSkillSlot customSkill : Configuration.getCustomSkills()) {
            String id = normalizeSkillId(customSkill.id);
            if (!id.isBlank()) {
                skillLevels.putIfAbsent(id, 1);
                skillExperience.putIfAbsent(id, 0);
            }
        }
    }

    public void ensureSkillExists(String skillId) {
        String storageId = toStorageSkillId(skillId);
        if (storageId.isBlank()) {
            return;
        }

        skillLevels.putIfAbsent(storageId, 1);
        skillExperience.putIfAbsent(storageId, 0);
    }

    public int getSkillLevel(Skill skill) {
        return getSkillLevel(skill.getSerializedName());
    }

    public int getSkillLevel(String skillId) {
        String storageId = toStorageSkillId(skillId);
        return skillLevels.getOrDefault(storageId, 1);
    }

    public void setSkillLevel(Skill skill, int level) {
        setSkillLevel(skill.getSerializedName(), level);
    }

    public void setSkillLevel(String skillId, int level) {
        String storageId = toStorageSkillId(skillId);
        if (storageId.isBlank()) {
            return;
        }

        skillLevels.put(storageId, Math.min(level, Configuration.getMaxLevel()));
        skillExperience.putIfAbsent(storageId, 0);
    }

    public int getSkillExperience(Skill skill) {
        return getSkillExperience(skill.getSerializedName());
    }

    public int getSkillExperience(String skillId) {
        return skillExperience.getOrDefault(toStorageSkillId(skillId), 0);
    }

    public void setSkillExperience(Skill skill, int xp) {
        setSkillExperience(skill.getSerializedName(), xp);
    }

    public void setSkillExperience(String skillId, int xp) {
        String storageId = toStorageSkillId(skillId);
        if (storageId.isBlank()) {
            return;
        }

        skillExperience.put(storageId, Math.max(0, xp));
        skillLevels.putIfAbsent(storageId, 1);
    }

    public void increaseSkillLevel(Skill skill, Player player) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) return;
        increaseSkillLevel(Configuration.getBuiltInSkillId(skill), player);
    }

    public void increaseSkillLevel(String skillId, Player player) {
        String normalized = normalizeSkillId(skillId);
        if (normalized.isBlank() || !Configuration.isKnownSkill(normalized)) {
            return;
        }

        String storageId = toStorageSkillId(normalized);
        ensureSkillExists(storageId);

        if (!canSpendAnotherLevel()) {
            return;
        }

        int currentLevel = getSkillLevel(storageId);
        if (currentLevel < Configuration.getMaxLevel()) {
            skillLevels.put(storageId, currentLevel + 1);
            skillExperience.put(storageId, 0);

            updateSkillAttributeBonuses(player);
            syncSkills(player);
        }
    }

    public void addExperience(Skill skill, int experience) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) return;
        addExperience(Configuration.getBuiltInSkillId(skill), experience);
    }

    public void addExperience(String skillId, int experience) {
        String normalized = normalizeSkillId(skillId);
        if (normalized.isBlank() || !Configuration.isKnownSkill(normalized)) {
            return;
        }

        String storageId = toStorageSkillId(normalized);
        ensureSkillExists(storageId);
        skillExperience.put(storageId, getSkillExperience(storageId) + experience);
        checkForLevelUp(storageId);
    }

    private void checkForLevelUp(String skillId) {
        int level = getSkillLevel(skillId);
        int xp = getSkillExperience(skillId);
        int spentLevels = getTotalSpentLevels();
        int maxSpent = Configuration.getMaxSpendableLevels();

        while (level < Configuration.getMaxLevel()
                && xp >= Configuration.calculateExperienceCost(level)
                && (maxSpent < 0 || spentLevels < maxSpent)) {
            xp -= Configuration.calculateExperienceCost(level);
            level++;
            spentLevels++;
        }

        skillLevels.put(skillId, level);
        skillExperience.put(skillId, xp);
    }

    public boolean hasSufficientXP(Player player, Skill skill) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateCostForLevel(getSkillLevel(skill) + 1);
    }

    public boolean hasSufficientXP(Player player, String skillId) {
        if (player.isCreative() || player.level().isClientSide) return true;

        int totalXP = calculateTotalXPFromPlayer(player);
        return totalXP >= Configuration.calculateCostForLevel(getSkillLevel(skillId) + 1);
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

    public boolean canCraftItem(Player player, ItemStack stack) {
        ResourceLocation resource = stack.getItem().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.CRAFT);
    }

    public boolean canAttackEntity(Player player, Entity target) {
        ResourceLocation resource = target.getType().builtInRegistryHolder().key().location();
        return checkRequirements(player, resource, RequirementType.ATTACK);
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

        List<String> formattedRequirements = new ArrayList<>();
        for (Requirement req : unmetRequirements) {
            formattedRequirements.add(getRequirementDisplayName(req.skill) + " level " + req.level);
        }

        Component joinedRequirements = Component.literal(String.join(", ", formattedRequirements));
        Component message = Component.translatable(translationKey, joinedRequirements);
        player.displayClientMessage(message, true);
    }

    private String getRequirementDisplayName(String skillId) {
        String normalized = normalizeSkillId(skillId);

        Skill builtIn = Configuration.resolveBuiltInSkill(normalized);
        if (builtIn != null) {
            String configuredName = Configuration.getBuiltInSkillDisplayName(builtIn);
            if (!configuredName.isBlank()) return configuredName;
            return Component.translatable(builtIn.getDisplayName()).getString();
        }

        Configuration.CustomSkillSlot customSkill = Configuration.getCustomSkill(normalized);
        if (customSkill != null && customSkill.displayName != null && !customSkill.displayName.isBlank()) {
            return customSkill.displayName;
        }

        return normalized;
    }

    public void syncSkills(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SyncToClient.send(serverPlayer);
        }
    }

    public void cloneFrom(SkillModel source) {
        this.skillLevels.clear();
        this.skillLevels.putAll(source.skillLevels);

        this.skillExperience.clear();
        this.skillExperience.putAll(source.skillExperience);

        this.disabledPerks.clear();
        this.disabledPerks.addAll(source.disabledPerks);
    }

    public boolean isPerkEnabled(Skill skill) {
        return skill != null
                && Configuration.isBuiltInSkillEnabled(skill)
                && !disabledPerks.contains(skill.getSerializedName());
    }

    public boolean isPerkEnabled(String skillId) {
        String normalized = normalizeSkillId(skillId);
        if (normalized.isBlank() || !Configuration.isKnownSkill(normalized)) return false;
        return !disabledPerks.contains(toStorageSkillId(normalized));
    }

    public void togglePerk(Skill skill, Player player) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) return;
        togglePerk(Configuration.getBuiltInSkillId(skill), player);
    }

    public void togglePerk(String skillId, Player player) {
        String normalized = normalizeSkillId(skillId);
        if (normalized.isBlank() || !Configuration.isKnownSkill(normalized)) {
            return;
        }

        String storageId = toStorageSkillId(normalized);
        if (!disabledPerks.add(storageId)) {
            disabledPerks.remove(storageId);
        }

        updateSkillAttributeBonuses(player);
        syncSkills(player);
    }

    public void updateSkillAttributeBonuses(Player player) {
        // Built-in skill perks
        for (SkillAttributeBonus bonus : SkillAttributeBonus.values()) {
            Attribute attribute = bonus.getAttribute();
            if (attribute == null) continue;

            Holder.Reference<Attribute> attr = BuiltInRegistries.ATTRIBUTE
                    .getResourceKey(attribute)
                    .flatMap(BuiltInRegistries.ATTRIBUTE::getHolder)
                    .orElse(null);

            if (attr == null) continue;

            var attrInstance = player.getAttributes().getInstance(attr);
            if (attrInstance == null) continue;

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "reskillable",
                    bonus.skill.name().toLowerCase(Locale.ROOT)
            );

            attrInstance.getModifiers().stream()
                    .filter(mod -> mod.id().equals(id))
                    .findFirst()
                    .ifPresent(attrInstance::removeModifier);

            if (isPerkEnabled(bonus.skill)) {
                int skillLevel = getSkillLevel(bonus.skill);
                int bonusSteps = skillLevel / bonus.getPerkStep();
                double totalBonus = bonusSteps * bonus.getBonusPerStep();

                if (totalBonus > 0) {
                    AttributeModifier modifier = new AttributeModifier(
                            id,
                            totalBonus,
                            bonus.getOperation()
                    );
                    attrInstance.addTransientModifier(modifier);
                }
            }
        }

        // Custom skill perks
        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled() || !slot.hasPerk()) {
                continue;
            }

            Attribute attribute = slot.getResolvedPerkAttribute();
            if (attribute == null) {
                continue;
            }

            Holder.Reference<Attribute> attr = BuiltInRegistries.ATTRIBUTE
                    .getResourceKey(attribute)
                    .flatMap(BuiltInRegistries.ATTRIBUTE::getHolder)
                    .orElse(null);

            if (attr == null) continue;

            var attrInstance = player.getAttributes().getInstance(attr);
            if (attrInstance == null) continue;

            String skillId = normalizeSkillId(slot.getId());

            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    "reskillable",
                    "custom_" + skillId
            );

            attrInstance.getModifiers().stream()
                    .filter(mod -> mod.id().equals(modifierId))
                    .findFirst()
                    .ifPresent(attrInstance::removeModifier);

            if (!isPerkEnabled(skillId)) {
                continue;
            }

            int skillLevel = getSkillLevel(skillId);
            int step = Math.max(1, slot.getPerkStep());
            int bonusSteps = skillLevel / step;
            double totalBonus = bonusSteps * slot.getPerkAmountPerStep();

            if (totalBonus <= 0) {
                continue;
            }

            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    totalBonus,
                    slot.getResolvedPerkOperation()
            );

            attrInstance.addTransientModifier(modifier);
        }

        handleHealthBonus(player);
        forceAttributeSync(player);
    }

    private void handleHealthBonus(Player player) {
        if (!Configuration.HEALTH_BONUS.get()) return;

        int totalSkillLevels = 0;
        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            totalSkillLevels += getSkillLevel(skill);
        }
        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                totalSkillLevels += getSkillLevel(slot.getId());
            }
        }
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

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void forceAttributeSync(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundUpdateAttributesPacket(
                    sp.getId(),
                    sp.getAttributes().getSyncableAttributes()
            ));
        }
    }

    public void readFromNetwork(SyncToClient msg) {
        this.skillLevels.clear();
        this.skillExperience.clear();
        this.disabledPerks.clear();

        resetSkills();

        for (Map.Entry<String, Integer> entry : msg.levels().entrySet()) {
            this.skillLevels.put(normalizeSkillId(entry.getKey()), entry.getValue());
        }

        for (Map.Entry<String, Boolean> entry : msg.disabledPerks().entrySet()) {
            if (entry.getValue()) {
                this.disabledPerks.add(normalizeSkillId(entry.getKey()));
            }
        }
    }

    public Map<String, Integer> getAllSkillLevels() {
        return Collections.unmodifiableMap(skillLevels);
    }

    public Map<String, Integer> getAllSkillExperience() {
        return Collections.unmodifiableMap(skillExperience);
    }

    public Set<String> getDisabledPerks() {
        return Collections.unmodifiableSet(disabledPerks);
    }
    public int getTotalSpentLevels() {
        int total = 0;

        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            total += Math.max(0, getSkillLevel(skill) - 1);
        }

        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                total += Math.max(0, getSkillLevel(slot.getId()) - 1);
            }
        }

        return total;
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

        for (int level : skillLevels.values()) {
            refund += getRefundForLevel(level);
        }

        return refund;
    }

    public int resetAllSkillsAndReturnRefund(Player player) {
        int refund = getTotalRespecRefund();

        resetSkills();

        updateSkillAttributeBonuses(player);
        syncSkills(player);

        return refund;
    }
}