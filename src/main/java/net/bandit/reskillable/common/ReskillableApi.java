package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.gating.SkillLevelGate;
import net.bandit.reskillable.common.network.ClientNetworkInit;
import net.bandit.reskillable.common.network.payload.RequestLevelUp;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.RequirementType;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
import net.bandit.reskillable.event.AllowLevelUpCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reskillable's API for other mods. Methods work on the server and the client unless they say otherwise.
 * For level changes, listen to {@link net.bandit.reskillable.event.SkillLevelChangedCallback}. To allow or refuse
 * a level-up, listen to {@link AllowLevelUpCallback}.
 *
 * <p>Skill ids are the {@code id} values in {@code built_in_skills.json} and {@code custom_skills.json}.
 * Built-in skills also accept their original names ({@code mining}, {@code attack} and so on).
 *
 * <p>On the client, these methods read what the server has synced, so a screen can call them every frame:
 * <ul>
 *   <li>Skill definitions (name, icon, enabled, perks), item, block and entity requirements, and the leveling
 *   settings ({@code maximumLevel}, {@code xpScalingMultiplier}, {@code maxTotalSpentLevels},
 *   {@code enableSkillLeveling}) come in {@code SyncSkillConfig} when the player joins, respawns or changes
 *   dimension, and after {@code /skills reload}.</li>
 *   <li>Levels and disabled perks come in {@code SyncToClient} after every change.</li>
 *   <li>Gate status comes in {@code SyncGateStatus} on join, after each level-up and after
 *   {@link #requestGateStatus()}. Call it when your screen opens and about once a second while it is open,
 *   as Reskillable's screen does, so a gate that needs an advancement opens without reopening the screen.</li>
 *   <li>XP level and progress are vanilla's.</li>
 * </ul>
 */
public final class ReskillableApi {
    private ReskillableApi() {}

    /** Whether a level-up is allowed now, and if not, why. */
    public enum LevelUpStatus {
        ALLOWED,
        /** Not the id of an enabled skill. */
        UNKNOWN_SKILL,
        /** {@code enableSkillLeveling} is false. */
        LEVELING_DISABLED,
        /** The skill is at {@code maximumLevel}. */
        MAX_LEVEL,
        /** The player has spent {@code maxTotalSpentLevels} levels. */
        SPENT_LEVEL_CAP,
        /** A {@code skill_level_gates} rule is not met. {@link #getGateMissing} lists what is missing. */
        GATE_BLOCKED,
        /** The player has less XP than {@link #getLevelUpCost}. Never returned for creative players. */
        NOT_ENOUGH_XP,
        /** An {@link AllowLevelUpCallback} listener refused it. Only {@link #levelUp} returns this. */
        REFUSED
    }

    /**
     * A bonus a skill gives every {@code perkStep} levels. {@code attribute} is null for the built-in mining,
     * gathering and farming perks (break speed, bonus XP, crop growth chance), which are not attributes. Their
     * {@code amountPerStep} is a fraction: 0.25 is 25%.
     */
    public record Perk(Component name, @Nullable Attribute attribute, AttributeModifier.Operation operation,
                       double amountPerStep, int perkStep) {
        /** The bonus at {@code level}. */
        public double bonusAt(int level) {
            return (level / perkStep) * amountPerStep;
        }
    }

    // Skills

    /** Ids of the enabled skills: built-in skills in {@code built_in_skills.json} order, then custom skills. */
    public static List<String> getSkillIds() {
        List<String> ids = new ArrayList<>();
        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            ids.add(Configuration.getBuiltInSkillId(skill));
        }
        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                ids.add(slot.getId());
            }
        }
        return ids;
    }

    public static boolean isSkillEnabled(String skillId) {
        return Configuration.isKnownSkill(skillId);
    }

    public static Component getDisplayName(String skillId) {
        Skill builtIn = Configuration.resolveBuiltInSkill(skillId);
        if (builtIn != null) {
            String configuredName = Configuration.getBuiltInSkillDisplayName(builtIn);
            return configuredName.isBlank()
                    ? Component.translatable(builtIn.getDisplayName())
                    : Component.literal(configuredName);
        }

        Configuration.CustomSkillSlot custom = Configuration.findCustomSkillById(skillId);
        return Component.literal(custom != null ? custom.getDisplayName() : String.valueOf(skillId));
    }

    /**
     * The icon texture set in the skill's {@code icon} field, or null when there is none and Reskillable draws
     * its own sprite from {@code reskillable:textures/gui/skills.png}.
     */
    public static @Nullable ResourceLocation getIcon(String skillId) {
        Skill builtIn = Configuration.resolveBuiltInSkill(skillId);
        if (builtIn != null) {
            return Configuration.getBuiltInSkillIcon(builtIn);
        }

        Configuration.CustomSkillSlot custom = Configuration.findCustomSkillById(skillId);
        return custom != null ? custom.getResolvedIcon() : null;
    }

    /** The skill's perks, as {@code SkillModel.updateSkillAttributeBonuses} applies them. */
    public static List<Perk> getPerks(String skillId) {
        List<Perk> perks = new ArrayList<>();

        Skill builtIn = Configuration.resolveBuiltInSkill(skillId);
        if (builtIn != null) {
            Configuration.BuiltInSkillSlot slot = Configuration.getBuiltInSkill(builtIn);
            SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(builtIn);
            if (slot != null && slot.perkAttributes != null) {
                addPerks(perks, slot.perkAttributes);
            } else if (bonus != null && slot != null && slot.hasPerkOverride()) {
                addPerk(perks, null, slot.getResolvedLegacySinglePerkAttribute(),
                        bonus.getOperation(), bonus.getBonusPerStep(), bonus.getPerkStep());
            } else if (bonus != null) {
                addPerk(perks, getBuiltInEffectName(builtIn), bonus.getAttribute(),
                        bonus.getOperation(), bonus.getBonusPerStep(), bonus.getPerkStep());
            }
            return perks;
        }

        Configuration.CustomSkillSlot custom = Configuration.findCustomSkillById(skillId);
        if (custom != null && custom.perkAttributes != null) {
            addPerks(perks, custom.perkAttributes);
        } else if (custom != null) {
            addPerk(perks, null, custom.getResolvedPerkAttribute(),
                    custom.getResolvedPerkOperation(), custom.getPerkAmountPerStep(), custom.getPerkStep());
        }
        return perks;
    }

    public static int getMaxLevel() {
        return Configuration.getMaxLevel();
    }

    /** {@code maxTotalSpentLevels}, or -1 for no cap. */
    public static int getMaxSpentLevels() {
        return Configuration.getMaxSpendableLevels();
    }

    // Player

    /** The player's level in a skill. 1 for a skill they have not leveled or an unknown id. */
    public static int getLevel(Player player, String skillId) {
        return SkillModel.get(player).getSkillLevel(skillId);
    }

    /** The sum of the player's levels in the enabled skills, as {@code TOTAL} in a gate rule counts it. */
    public static int getTotalLevel(Player player) {
        SkillModel model = SkillModel.get(player);
        int total = 0;
        for (String skillId : getSkillIds()) {
            total += model.getSkillLevel(skillId);
        }
        return total;
    }

    /** Levels spent above level 1, which {@code maxTotalSpentLevels} caps. */
    public static int getSpentLevels(Player player) {
        return SkillModel.get(player).getTotalSpentLevels();
    }

    public static boolean isPerkEnabled(Player player, String skillId) {
        return SkillModel.get(player).isPerkEnabled(skillId);
    }

    /** The player's XP in points, which a level-up spends. */
    public static int getXp(Player player) {
        return RequestLevelUp.getTotalXp(player);
    }

    /** XP points the skill's next level costs. */
    public static int getLevelUpCost(Player player, String skillId) {
        return Configuration.calculateCostForLevel(getLevel(player, skillId));
    }

    /** The same checks a level-up runs, in the same order. */
    public static LevelUpStatus canLevelUp(Player player, String skillId) {
        return RequestLevelUp.check(player, skillId);
    }

    /**
     * What a gate rule still needs before the skill can level up, as a comma-separated list, or empty when no
     * gate blocks it. On the client, this is the status the server last sent.
     */
    public static Component getGateMissing(Player player, String skillId) {
        if (player instanceof ServerPlayer serverPlayer) {
            SkillModel model = SkillModel.get(serverPlayer);
            SkillLevelGate.GateResult gate = SkillLevelGate.check(serverPlayer, model, skillId, model.getSkillLevel(skillId));
            return gate.allowed() ? Component.empty() : gate.missingListComponent(serverPlayer);
        }

        GateClientCache.Entry gate = GateClientCache.get(skillId);
        return gate == null || !gate.blocked() ? Component.empty() : gate.missingList();
    }

    // Client

    /** Client only. Asks the server to level up the skill. The server runs {@link #canLevelUp} again. */
    public static void requestLevelUp(String skillId) {
        ClientNetworkInit.sendLevelUp(skillId);
    }

    /** Client only. Asks the server to turn the skill's perk off, or back on. */
    public static void requestTogglePerk(String skillId) {
        ClientNetworkInit.sendTogglePerk(skillId);
    }

    /** Client only. Asks the server for the gate status of every skill. */
    public static void requestGateStatus() {
        ClientNetworkInit.requestGateStatus();
    }

    // Server

    /**
     * Server only. Levels up the skill if {@link #canLevelUp} allows it, as the level-up packet does: takes the XP,
     * plays the sounds, syncs the client and fires {@code SkillLevelChangedCallback}. Otherwise tells the player
     * why not. An {@link AllowLevelUpCallback} listener can still refuse it; then nothing changes, no message is
     * sent and this returns {@link LevelUpStatus#REFUSED}.
     */
    public static LevelUpStatus levelUp(ServerPlayer player, String skillId) {
        return RequestLevelUp.levelUp(player, skillId);
    }

    // Requirements. These ignore creative mode, which Reskillable lets through.

    /** An item's requirements for {@code USE} or {@code CRAFT}. Empty when it has none. */
    public static List<Requirement> getRequirements(RequirementType type, ItemStack stack) {
        return getRequirements(type, getId(stack));
    }

    /** A block's requirements for {@code USE}. Empty when it has none. */
    public static List<Requirement> getRequirements(RequirementType type, Block block) {
        return getRequirements(type, getId(block));
    }

    /** An entity's requirements for {@code USE} or {@code ATTACK}. Empty when it has none. */
    public static List<Requirement> getRequirements(RequirementType type, Entity entity) {
        return getRequirements(type, getId(entity));
    }

    public static List<Requirement> getMissingRequirements(Player player, RequirementType type, ItemStack stack) {
        return SkillModel.get(player).getUnmetRequirements(getId(stack), type);
    }

    public static List<Requirement> getMissingRequirements(Player player, RequirementType type, Block block) {
        return SkillModel.get(player).getUnmetRequirements(getId(block), type);
    }

    public static List<Requirement> getMissingRequirements(Player player, RequirementType type, Entity entity) {
        return SkillModel.get(player).getUnmetRequirements(getId(entity), type);
    }

    public static boolean meetsRequirements(Player player, RequirementType type, ItemStack stack) {
        return getMissingRequirements(player, type, stack).isEmpty();
    }

    public static boolean meetsRequirements(Player player, RequirementType type, Block block) {
        return getMissingRequirements(player, type, block).isEmpty();
    }

    public static boolean meetsRequirements(Player player, RequirementType type, Entity entity) {
        return getMissingRequirements(player, type, entity).isEmpty();
    }

    private static List<Requirement> getRequirements(RequirementType type, ResourceLocation id) {
        Requirement[] requirements = type.getRequirements(id);
        return requirements == null ? List.of() : Collections.unmodifiableList(Arrays.asList(requirements));
    }

    // The same ids SkillModel.canUseItem, canUseBlock and canUseEntity look up.
    private static ResourceLocation getId(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location();
    }

    private static ResourceLocation getId(Block block) {
        return block.builtInRegistryHolder().key().location();
    }

    private static ResourceLocation getId(Entity entity) {
        return entity.getType().builtInRegistryHolder().key().location();
    }

    private static void addPerks(List<Perk> perks, List<Configuration.PerkAttributeDefinition> definitions) {
        for (Configuration.PerkAttributeDefinition definition : definitions) {
            if (definition == null) continue;
            addPerk(perks, null, definition.getResolvedAttribute(), definition.getResolvedOperation(),
                    definition.getAmountPerStep(), definition.getPerkStep());
        }
    }

    private static void addPerk(List<Perk> perks, @Nullable Component name, @Nullable Attribute attribute,
                                AttributeModifier.Operation operation, double amountPerStep, int perkStep) {
        if (amountPerStep <= 0.0 || (attribute == null && name == null)) return;
        Component perkName = name != null ? name : Component.translatable(attribute.getDescriptionId());
        perks.add(new Perk(perkName, attribute, operation, amountPerStep, Math.max(1, perkStep)));
    }

    // Built-in perks that are not attributes. EventHandler applies them.
    private static @Nullable Component getBuiltInEffectName(Skill skill) {
        return switch (skill) {
            case MINING -> Component.translatable("tooltip.rereskillable.break_speed");
            case GATHERING -> Component.translatable("tooltip.rereskillable.bonus_xp_orbs");
            case FARMING -> Component.translatable("tooltip.rereskillable.crop_growth");
            default -> null;
        };
    }
}
