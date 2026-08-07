package net.bandit.reskillable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class Configuration {
    private static final ConfigValue<Boolean> DISABLE_WOOL;
    private static final ConfigValue<Boolean> SHOW_TAB_BUTTONS;
    private static final ConfigValue<Boolean> SHOW_SUBPAGE_TITLES;
    private static final ConfigValue<Boolean> DEATH_RESET;
    public static final ConfigValue<Boolean> HEALTH_BONUS;
    public static ConfigValue<List<? extends String>> SKILL_LEVEL_GATES;
    private static final ConfigValue<Integer> MAXIMUM_LEVEL;
    private static final ConfigValue<Double> XP_SCALING_MULTIPLIER;
    private static final ConfigValue<List<? extends String>> SKILL_ALIAS;
    private static final ConfigValue<Boolean> ENABLE_SKILL_LEVELING;
    private static final ConfigValue<Boolean> ENABLE_SKILL_UP_MESSAGE;
    public static final ConfigValue<Integer> LEVELS_PER_HEART;
    public static final ConfigValue<Double> HEALTH_PER_HEART;
    public static ConfigValue<Integer> MAX_TOTAL_SPENT_LEVELS;

    public static ConfigValue<Double> ATTACK_DAMAGE_BONUS;
    public static ConfigValue<Double> ARMOR_BONUS;
    public static ConfigValue<Double> MOVEMENT_SPEED_BONUS;
    public static ConfigValue<Double> LUCK_BONUS;
    public static ConfigValue<Double> BLOCK_REACH_BONUS;
    public static ConfigValue<Double> MINING_SPEED_MULTIPLIER;
    public static ConfigValue<Double> CROP_GROWTH_CHANCE;
    public static final ConfigValue<Double> GATHERING_XP_BONUS;

    public static final ConfigValue<String> MAGIC_ATTRIBUTE_ID;
    public static ConfigValue<String> ATTACK_ATTRIBUTE_ID;
    public static ConfigValue<String> DEFENSE_ATTRIBUTE_ID;
    public static ConfigValue<String> AGILITY_ATTRIBUTE_ID;
    public static ConfigValue<String> BUILDING_ATTRIBUTE_ID;

    public static ConfigValue<String> ATTACK_OPERATION;
    public static ConfigValue<String> DEFENSE_OPERATION;
    public static ConfigValue<String> AGILITY_OPERATION;
    public static ConfigValue<String> BUILDING_OPERATION;
    public static ConfigValue<String> MAGIC_OPERATION;

    private static final ConfigValue<Boolean> ENABLE_SECOND_SKILL_PAGE;
    private static final ConfigValue<String> SUBPAGE_NAV_POSITION;

    private static final int MAX_CUSTOM_SKILLS = 8;
    private static List<BuiltInSkillSlot> builtInSkills = new ArrayList<>();
    private static List<CustomSkillSlot> customSkills = new ArrayList<>();

    private static boolean disableWool;
    private static boolean showTabButtons;
    private static boolean deathReset;
    private static boolean healthBonus;
    private static int maximumLevel;
    private static double xpScalingMultiplier;
    private static boolean enableSkillLeveling;

    private static Map<String, Requirement[]> skillLocks = new HashMap<>();
    private static Map<String, Requirement[]> craftSkillLocks = new HashMap<>();
    private static Map<String, Requirement[]> attackSkillLocks = new HashMap<>();

    private static final String DEFAULT_SKILL_LOCKS = """
            {
              "skillLocks": {
                "minecraft:iron_sword": ["attack:5"],
                "minecraft:iron_shovel": ["gathering:5"],
                "minecraft:iron_pickaxe": ["mining:5"],
                "minecraft:iron_axe": ["gathering:5"],
                "minecraft:iron_hoe": ["farming:5"],
                "minecraft:iron_helmet": ["defense:5"],
                "minecraft:iron_chestplate": ["defense:5"],
                "minecraft:iron_leggings": ["defense:5"],
                "minecraft:iron_boots": ["defense:5"],
                "minecraft:diamond_sword": ["attack:15"],
                "minecraft:diamond_shovel": ["gathering:15"],
                "minecraft:diamond_pickaxe": ["mining:15"],
                "minecraft:diamond_axe": ["gathering:15"],
                "minecraft:diamond_hoe": ["farming:15"],
                "minecraft:diamond_helmet": ["defense:15"],
                "minecraft:diamond_chestplate": ["defense:15"],
                "minecraft:diamond_leggings": ["defense:15"],
                "minecraft:diamond_boots": ["defense:15"],
                "minecraft:netherite_sword": ["attack:30"],
                "minecraft:netherite_shovel": ["gathering:30"],
                "minecraft:netherite_pickaxe": ["mining:30"],
                "minecraft:netherite_axe": ["gathering:30"],
                "minecraft:netherite_hoe": ["farming:30"],
                "minecraft:netherite_helmet": ["defense:30"],
                "minecraft:netherite_chestplate": ["defense:30"],
                "minecraft:netherite_leggings": ["defense:30"],
                "minecraft:netherite_boots": ["defense:30"]
              }
            }
            """;

    private static final String DEFAULT_CRAFT_SKILL_LOCKS = """
            {
              "craftSkillLocks": {}
            }
            """;

    private static final String DEFAULT_ATTACK_SKILL_LOCKS = """
            {
              "attackSkillLocks": {
                "minecraft:zombie": ["attack:2"],
                "minecraft:skeleton": ["attack:2"]
              }
            }
            """;

    private static final String DEFAULT_CUSTOM_SKILLS = """
            {
              "customSkills": [
                {
                  "id": "swimming",
                  "displayName": "Swimming",
                  "enabled": true,
                  "perkAttribute": "minecraft:generic.water_movement_efficiency",
                  "icon": "reskillable:textures/gui/custom_skills/swimming.png",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.1,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                },
                {
                  "id": "",
                  "displayName": "",
                  "perkAttribute": "",
                  "icon": "",
                  "perkOperation": "ADDITION",
                  "perkAmountPerStep": 0.0,
                  "perkStep": 5
                }
              ]
            }
            """;

    private static final String DEFAULT_BUILT_IN_SKILLS = """
            {
              "builtInSkills": [
                {
                  "skill": "mining",
                  "id": "mining",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "gathering",
                  "id": "gathering",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "attack",
                  "id": "attack",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "defense",
                  "id": "defense",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "building",
                  "id": "building",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "farming",
                  "id": "farming",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "agility",
                  "id": "agility",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                },
                {
                  "skill": "magic",
                  "id": "magic",
                  "displayName": "",
                  "enabled": true,
                  "icon": "",
                  "perkAttribute": null,
                  "perkOperation": null,
                  "perkAmountPerStep": null,
                  "perkStep": 5
                }
              ]
            }
            """;

    static {
        DISABLE_WOOL = new ConfigValue<>(true);
        DEATH_RESET = new ConfigValue<>(false);
        HEALTH_BONUS = new ConfigValue<>(true);
        SHOW_TAB_BUTTONS = new ConfigValue<>(true);
        XP_SCALING_MULTIPLIER = new ConfigValue<>(1.0);
        MAXIMUM_LEVEL = new ConfigValue<>(32);
        ENABLE_SKILL_LEVELING = new ConfigValue<>(true);
        ENABLE_SKILL_UP_MESSAGE = new ConfigValue<>(true);
        SKILL_ALIAS = new ConfigValue<>(List.of("defense=defense"));

        ATTACK_ATTRIBUTE_ID = new ConfigValue<>("minecraft:generic.attack_damage");
        ATTACK_OPERATION = new ConfigValue<>("MULTIPLY_TOTAL");
        ATTACK_DAMAGE_BONUS = new ConfigValue<>(0.15);

        DEFENSE_ATTRIBUTE_ID = new ConfigValue<>("minecraft:generic.armor");
        DEFENSE_OPERATION = new ConfigValue<>("MULTIPLY_TOTAL");
        ARMOR_BONUS = new ConfigValue<>(0.15);

        AGILITY_ATTRIBUTE_ID = new ConfigValue<>("minecraft:generic.movement_speed");
        AGILITY_OPERATION = new ConfigValue<>("MULTIPLY_TOTAL");
        MOVEMENT_SPEED_BONUS = new ConfigValue<>(0.05);

        MAGIC_ATTRIBUTE_ID = new ConfigValue<>("minecraft:generic.luck");
        MAGIC_OPERATION = new ConfigValue<>("MULTIPLY_TOTAL");
        LUCK_BONUS = new ConfigValue<>(0.05);

        BUILDING_ATTRIBUTE_ID = new ConfigValue<>("minecraft:player.block_interaction_range");
        BUILDING_OPERATION = new ConfigValue<>("ADDITION");
        BLOCK_REACH_BONUS = new ConfigValue<>(0.25);

        MINING_SPEED_MULTIPLIER = new ConfigValue<>(0.25);
        CROP_GROWTH_CHANCE = new ConfigValue<>(0.25);
        GATHERING_XP_BONUS = new ConfigValue<>(0.05);

        LEVELS_PER_HEART = new ConfigValue<>(10);
        HEALTH_PER_HEART = new ConfigValue<>(2.0);
        SKILL_LEVEL_GATES = new ConfigValue<>(List.of());

        ENABLE_SECOND_SKILL_PAGE = new ConfigValue<>(false);
        SUBPAGE_NAV_POSITION = new ConfigValue<>("BOTTOM");
        SHOW_SUBPAGE_TITLES = new ConfigValue<>(true);
        MAX_TOTAL_SPENT_LEVELS = new ConfigValue<>(-1);
    }

    public static void load() {
        loadMainConfig();

        disableWool = DISABLE_WOOL.get();
        showTabButtons = SHOW_TAB_BUTTONS.get();
        deathReset = DEATH_RESET.get();
        healthBonus = HEALTH_BONUS.get();
        xpScalingMultiplier = XP_SCALING_MULTIPLIER.get();
        maximumLevel = MAXIMUM_LEVEL.get();
        enableSkillLeveling = ENABLE_SKILL_LEVELING.get();

        Map<String, Map<String, List<String>>> skillData = loadJsonConfig(
                FabricLoader.getInstance().getConfigDir().resolve("reskillable/skill_locks.json").toString(),
                DEFAULT_SKILL_LOCKS,
                "skillLocks"
        );

        Map<String, Map<String, List<String>>> craftData = loadJsonConfig(
                FabricLoader.getInstance().getConfigDir().resolve("reskillable/craft_skill_locks.json").toString(),
                DEFAULT_CRAFT_SKILL_LOCKS,
                "craftSkillLocks"
        );

        Map<String, Map<String, List<String>>> attackData = loadJsonConfig(
                FabricLoader.getInstance().getConfigDir().resolve("reskillable/attack_skill_locks.json").toString(),
                DEFAULT_ATTACK_SKILL_LOCKS,
                "attackSkillLocks"
        );

        builtInSkills = loadBuiltInSkills(
                FabricLoader.getInstance().getConfigDir().resolve("reskillable/built_in_skills.json").toString(),
                DEFAULT_BUILT_IN_SKILLS
        );

        customSkills = loadCustomSkills(
                FabricLoader.getInstance().getConfigDir().resolve("reskillable/custom_skills.json").toString(),
                DEFAULT_CUSTOM_SKILLS
        );

        validateSkillIds();

        skillLocks = parseSkillLocks(skillData.get("skillLocks"));
        craftSkillLocks = parseSkillLocks(craftData.get("craftSkillLocks"));
        attackSkillLocks = parseSkillLocks(attackData.get("attackSkillLocks"));
    }

    private static List<BuiltInSkillSlot> loadBuiltInSkills(String filename, String defaultContent) {
        File file = new File(filename);

        if (!file.exists()) {
            if (createDefaultJsonFile(file, defaultContent)) {
                System.out.println("Default file created: " + filename);
            } else {
                System.err.println("Failed to create default file: " + filename);
            }
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            if (!jsonObject.has("builtInSkills")) {
                System.err.println("Missing 'builtInSkills' key in JSON: " + filename);
                return createDefaultBuiltInSkillSlots();
            }

            Type listType = new TypeToken<List<BuiltInSkillSlot>>() {}.getType();
            List<BuiltInSkillSlot> loaded = new Gson().fromJson(jsonObject.get("builtInSkills"), listType);
            if (loaded == null) {
                return createDefaultBuiltInSkillSlots();
            }

            List<BuiltInSkillSlot> normalized = new ArrayList<>();
            Set<Skill> seen = EnumSet.noneOf(Skill.class);

            for (BuiltInSkillSlot slot : loaded) {
                if (slot == null) continue;

                Skill baseSkill = Skill.fromString(slot.skill);
                if (baseSkill == null) {
                    System.err.println("[Reskillable] Unknown built-in skill mapping '" + slot.skill + "'.");
                    continue;
                }

                if (!seen.add(baseSkill)) {
                    System.err.println("[Reskillable] Duplicate built-in skill mapping for '" + baseSkill.getSerializedName() + "'. Keeping the first entry.");
                    continue;
                }

                normalized.add(normalizeBuiltInSkillSlot(slot, baseSkill));
            }

            for (Skill skill : Skill.values()) {
                // Deleting an entry intentionally removes it from active progression while
                // preserving the stable internal/save-data slot.
                if (!seen.contains(skill)) {
                    normalized.add(BuiltInSkillSlot.disabled(skill));
                }
            }

            return normalized;
        } catch (Exception e) {
            System.err.println("Error loading built-in skills from file: " + filename);
            e.printStackTrace();
            return createDefaultBuiltInSkillSlots();
        }
    }

    private static List<BuiltInSkillSlot> createDefaultBuiltInSkillSlots() {
        List<BuiltInSkillSlot> defaults = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            defaults.add(BuiltInSkillSlot.defaults(skill));
        }
        return defaults;
    }

    private static BuiltInSkillSlot normalizeBuiltInSkillSlot(BuiltInSkillSlot slot, Skill baseSkill) {
        String id = slot.id == null ? baseSkill.getSerializedName() : slot.id.trim().toLowerCase(Locale.ROOT);
        String displayName = slot.displayName == null ? "" : slot.displayName.trim();
        String icon = slot.icon == null ? "" : slot.icon.trim();
        String perkAttribute = slot.perkAttribute == null ? null : slot.perkAttribute.trim();
        String perkOperation = slot.perkOperation == null ? null : slot.perkOperation.trim().toUpperCase(Locale.ROOT);
        Double perkAmountPerStep = slot.perkAmountPerStep;
        Integer perkStep = slot.perkStep;

        if (!id.isEmpty() && !id.matches("[a-z0-9_]+")) {
            System.err.println("[Reskillable] Invalid built-in skill id '" + id + "' for '" + baseSkill.getSerializedName() + "'. Disabling this entry.");
            return BuiltInSkillSlot.disabled(baseSkill);
        }

        if (!icon.isBlank() && ResourceLocation.tryParse(icon) == null) {
            System.err.println("[Reskillable] Invalid built-in icon '" + icon + "' for skill '" + baseSkill.getSerializedName() + "'. Falling back to the original sprite.");
            icon = "";
        }

        if (perkAttribute != null && !perkAttribute.isBlank() && !perkAttribute.equalsIgnoreCase("none")) {
            ResourceLocation attrId = ResourceLocation.tryParse(perkAttribute);
            if (attrId == null || !BuiltInRegistries.ATTRIBUTE.containsKey(attrId)) {
                System.err.println("[Reskillable] Unknown built-in perk attribute '" + perkAttribute + "' for skill '" + id + "'. Falling back to the legacy config.");
                perkAttribute = null;
            }
        }

        if (perkOperation != null && !perkOperation.isBlank() && !isValidAttributeOperation(perkOperation)) {
            System.err.println("[Reskillable] Invalid built-in perk operation '" + perkOperation + "' for skill '" + id + "'. Falling back to the legacy config.");
            perkOperation = null;
        }

        if (perkAmountPerStep != null && perkAmountPerStep < 0.0) {
            System.err.println("[Reskillable] Negative built-in perk amount for skill '" + id + "'. Falling back to the legacy config.");
            perkAmountPerStep = null;
        }

        if (perkStep != null && perkStep < 1) {
            System.err.println("[Reskillable] Invalid built-in perkStep for skill '" + id + "'. Falling back to 5.");
            perkStep = null;
        }

        return new BuiltInSkillSlot(
                baseSkill.getSerializedName(),
                id,
                displayName,
                slot.enabled && !id.isBlank(),
                icon,
                perkAttribute,
                perkOperation,
                perkAmountPerStep,
                perkStep
        );
    }

    private static List<CustomSkillSlot> loadCustomSkills(String filename, String defaultContent) {
        File file = new File(filename);

        if (!file.exists()) {
            if (createDefaultJsonFile(file, defaultContent)) {
                System.out.println("Default file created: " + filename);
            } else {
                System.err.println("Failed to create default file: " + filename);
            }
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if (!jsonObject.has("customSkills")) {
                System.err.println("Missing 'customSkills' key in JSON: " + filename);
                return createEmptyCustomSkillSlots();
            }

            Type listType = new TypeToken<List<CustomSkillSlot>>() {}.getType();
            List<CustomSkillSlot> loaded = new Gson().fromJson(jsonObject.get("customSkills"), listType);

            if (loaded == null) {
                return createEmptyCustomSkillSlots();
            }

            List<CustomSkillSlot> normalized = new ArrayList<>();
            for (int i = 0; i < MAX_CUSTOM_SKILLS; i++) {
                CustomSkillSlot slot = i < loaded.size() && loaded.get(i) != null
                        ? loaded.get(i)
                        : new CustomSkillSlot("", "", "", "", "ADDITION", 0.0, 5);
                normalized.add(normalizeCustomSkillSlot(slot));
            }

            return normalized;
        } catch (Exception e) {
            System.err.println("Error loading custom skills from file: " + filename);
            e.printStackTrace();
            return createEmptyCustomSkillSlots();
        }
    }

    private static List<CustomSkillSlot> createEmptyCustomSkillSlots() {
        List<CustomSkillSlot> empty = new ArrayList<>();
        for (int i = 0; i < MAX_CUSTOM_SKILLS; i++) {
            empty.add(new CustomSkillSlot("", "", "", "", "ADDITION", 0.0, 5));
        }
        return empty;
    }

    private static CustomSkillSlot normalizeCustomSkillSlot(CustomSkillSlot slot) {
        String id = slot.id == null ? "" : slot.id.trim().toLowerCase(Locale.ROOT);
        String displayName = slot.displayName == null ? "" : slot.displayName.trim();
        String perkAttribute = slot.perkAttribute == null ? "" : slot.perkAttribute.trim();
        String icon = slot.icon == null ? "" : slot.icon.trim();
        String perkOperation = slot.perkOperation == null ? "ADDITION" : slot.perkOperation.trim().toUpperCase(Locale.ROOT);
        double perkAmountPerStep = Math.max(0.0, slot.perkAmountPerStep);
        int perkStep = Math.max(1, slot.perkStep);

        if (!id.isEmpty() && !id.matches("[a-z0-9_]+")) {
            System.err.println("[Reskillable] Invalid custom skill id '" + id + "'. Only lowercase letters, numbers, and underscores are allowed.");
            id = "";
            displayName = "";
            perkAttribute = "";
            icon = "";
            perkAmountPerStep = 0.0;
            perkStep = 5;
            perkOperation = "ADDITION";
        }

        if (!perkAttribute.isBlank()) {
            try {
                ResourceLocation attrId = ResourceLocation.parse(perkAttribute);
                if (!BuiltInRegistries.ATTRIBUTE.containsKey(attrId)) {
                    System.err.println("[Reskillable] Unknown custom perk attribute '" + perkAttribute + "' for skill '" + id + "'.");
                    perkAttribute = "";
                }
            } catch (Exception e) {
                System.err.println("[Reskillable] Invalid custom perk attribute '" + perkAttribute + "' for skill '" + id + "'.");
                perkAttribute = "";
            }
        }

        try {
            String op = perkOperation;
            switch (op) {
                case "ADD_VALUE", "VALUE", "ADDITION",
                     "ADD_MULTIPLIED_BASE", "MULTIPLIED_BASE", "MULTIPLY_BASE",
                     "ADD_MULTIPLIED_TOTAL", "MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> {}
                default -> throw new IllegalArgumentException("Unknown op");
            }
        } catch (Exception e) {
            System.err.println("[Reskillable] Invalid custom perk operation '" + perkOperation + "' for skill '" + id + "'. Defaulting to ADDITION.");
            perkOperation = "ADDITION";
        }

        CustomSkillSlot normalized = new CustomSkillSlot(id, displayName, perkAttribute, icon, perkOperation, perkAmountPerStep, perkStep);
        normalized.enabled = slot.enabled == null || slot.enabled;
        return normalized;
    }

    public static boolean isSecondSkillPageEnabled() {
        return ENABLE_SECOND_SKILL_PAGE.get();
    }

    public static List<BuiltInSkillSlot> getBuiltInSkills() {
        return Collections.unmodifiableList(builtInSkills);
    }

    /** Returns enabled built-ins in the order supplied by built_in_skills.json. */
    public static List<Skill> getEnabledBuiltInSkills() {
        List<Skill> enabled = new ArrayList<>();
        for (BuiltInSkillSlot slot : builtInSkills) {
            if (slot == null || !slot.isEnabled()) continue;
            Skill skill = slot.getBaseSkill();
            if (skill != null) enabled.add(skill);
        }
        return enabled;
    }

    public static BuiltInSkillSlot getBuiltInSkill(Skill skill) {
        if (skill == null) return null;
        for (BuiltInSkillSlot slot : builtInSkills) {
            if (slot != null && skill == slot.getBaseSkill()) {
                return slot;
            }
        }
        return null;
    }

    public static boolean isBuiltInSkillEnabled(Skill skill) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        return slot != null && slot.isEnabled();
    }

    public static String getBuiltInSkillId(Skill skill) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        if (slot != null && !slot.getId().isBlank()) return slot.getId();
        return skill == null ? "" : skill.getSerializedName();
    }

    public static String getBuiltInSkillDisplayName(Skill skill) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        return slot == null ? "" : slot.getDisplayName();
    }

    public static ResourceLocation getBuiltInSkillIcon(Skill skill) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        return slot == null ? null : slot.getResolvedIcon();
    }

    public static Attribute getBuiltInPerkAttribute(Skill skill, Attribute fallback) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        if (slot == null) return fallback;

        String override = slot.getPerkAttributeOverride();
        if (override == null || override.isBlank()) return fallback;
        if (override.equalsIgnoreCase("none")) return null;

        try {
            Attribute resolved = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(override));
            return resolved != null ? resolved : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static AttributeModifier.Operation getBuiltInPerkOperation(Skill skill, AttributeModifier.Operation fallback) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        if (slot == null) return fallback;

        String override = slot.getPerkOperationOverride();
        if (override == null || override.isBlank()) return fallback;
        return parseAttributeOperation(override, fallback);
    }

    public static double getBuiltInPerkAmountPerStep(Skill skill, double fallback) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        if (slot == null || slot.getPerkAmountPerStepOverride() == null) return fallback;
        return Math.max(0.0, slot.getPerkAmountPerStepOverride());
    }

    public static int getBuiltInPerkStep(Skill skill) {
        BuiltInSkillSlot slot = getBuiltInSkill(skill);
        return slot == null ? 5 : slot.getPerkStep();
    }

    /**
     * Resolves either a configured public ID (for example "combat") or the
     * legacy/internal ID (for example "attack") to the semantic built-in skill.
     * Disabled skills intentionally do not resolve through public gameplay APIs.
     */
    public static Skill resolveBuiltInSkill(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);

        for (BuiltInSkillSlot slot : builtInSkills) {
            if (slot != null && slot.isEnabled() && slot.getId().equals(normalized)) {
                return slot.getBaseSkill();
            }
        }

        Skill legacy = Skill.fromString(normalized);
        return legacy != null && isBuiltInSkillEnabled(legacy) ? legacy : null;
    }

    /** Public/canonical ID used by gates and user-facing configuration. */
    public static String canonicalSkillId(String id) {
        Skill builtIn = resolveBuiltInSkill(id);
        if (builtIn != null) return getBuiltInSkillId(builtIn);

        CustomSkillSlot custom = findCustomSkillById(id);
        return custom != null ? custom.getId() : normalizeSkillId(id);
    }

    /** Stable map/save key. Built-in public IDs are mapped back to their original slot. */
    public static String toStorageSkillId(String id) {
        Skill builtIn = resolveBuiltInSkill(id);
        if (builtIn != null) return builtIn.getSerializedName();

        CustomSkillSlot custom = findCustomSkillById(id);
        return custom != null ? custom.getId() : normalizeSkillId(id);
    }

    public enum SubpageNavPosition {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT;

        public static SubpageNavPosition fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return BOTTOM;
            }

            try {
                return SubpageNavPosition.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return BOTTOM;
            }
        }
    }

    public static SubpageNavPosition getSubpageNavPosition() {
        return SubpageNavPosition.fromString(SUBPAGE_NAV_POSITION.get());
    }

    public static List<CustomSkillSlot> getCustomSkills() {
        return Collections.unmodifiableList(customSkills);
    }

    public static boolean shouldShowSubpageTitles() {
        return SHOW_SUBPAGE_TITLES.get();
    }

    public static CustomSkillSlot getCustomSkill(int index) {
        if (index < 0 || index >= customSkills.size()) {
            return new CustomSkillSlot("", "");
        }
        return customSkills.get(index);
    }

    public static boolean hasEnabledCustomSkills() {
        if (!isSecondSkillPageEnabled()) {
            return false;
        }

        for (CustomSkillSlot slot : customSkills) {
            if (slot != null && slot.isEnabled()) {
                return true;
            }
        }

        return false;
    }

    public static List<CustomSkillSlot> getEnabledCustomSkills() {
        List<CustomSkillSlot> enabled = new ArrayList<>();
        if (!isSecondSkillPageEnabled()) {
            return enabled;
        }

        for (CustomSkillSlot slot : customSkills) {
            if (slot != null && slot.isEnabled()) {
                enabled.add(slot);
            }
        }

        return enabled;
    }

    public static CustomSkillSlot findCustomSkillById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (CustomSkillSlot slot : customSkills) {
            if (slot != null && slot.isEnabled() && slot.id.equals(normalized)) {
                return slot;
            }
        }

        return null;
    }

    public static boolean isCustomSkill(String skillName) {
        return findCustomSkillById(skillName) != null;
    }

    public static CustomSkillSlot getCustomSkill(String skillName) {
        return findCustomSkillById(skillName);
    }

    public static boolean isKnownSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return false;
        }

        return resolveBuiltInSkill(skillName) != null || findCustomSkillById(skillName) != null;
    }

    public static boolean isVanillaSkill(String skillName) {
        return resolveBuiltInSkill(skillName) != null;
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateSkillIds() {
        Set<String> legacyBuiltInIds = new HashSet<>();
        for (Skill skill : Skill.values()) {
            legacyBuiltInIds.add(skill.getSerializedName());
        }

        Set<String> usedPublicIds = new HashSet<>();

        for (BuiltInSkillSlot slot : builtInSkills) {
            if (slot == null || !slot.isEnabled()) continue;

            Skill base = slot.getBaseSkill();
            String publicId = slot.getId();

            if (base != null
                    && legacyBuiltInIds.contains(publicId)
                    && !publicId.equals(base.getSerializedName())) {
                System.err.println("[Reskillable] Built-in skill id '" + publicId + "' conflicts with another built-in skill's legacy ID. Disabling mapping for '" + slot.skill + "'.");
                slot.enabled = false;
                continue;
            }

            if (!usedPublicIds.add(publicId)) {
                System.err.println("[Reskillable] Duplicate skill id '" + publicId + "'. Disabling duplicate built-in mapping for '" + slot.skill + "'.");
                slot.enabled = false;
            }
        }

        for (CustomSkillSlot slot : customSkills) {
            if (slot == null || !slot.isEnabled()) continue;

            if (legacyBuiltInIds.contains(slot.getId()) || !usedPublicIds.add(slot.getId())) {
                System.err.println("[Reskillable] Duplicate skill id '" + slot.getId() + "'. Disabling duplicate custom skill.");
                slot.enabled = false;
            }
        }
    }

    private static boolean isValidAttributeOperation(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ADD_VALUE", "VALUE", "ADDITION",
                 "ADD_MULTIPLIED_BASE", "MULTIPLIED_BASE", "MULTIPLY_BASE",
                 "ADD_MULTIPLIED_TOTAL", "MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> true;
            default -> false;
        };
    }

    private static AttributeModifier.Operation parseAttributeOperation(String raw, AttributeModifier.Operation fallback) {
        if (raw == null || raw.isBlank()) return fallback;

        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ADD_VALUE", "VALUE", "ADDITION" -> AttributeModifier.Operation.ADD_VALUE;
            case "ADD_MULTIPLIED_BASE", "MULTIPLIED_BASE", "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "ADD_MULTIPLIED_TOTAL", "MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> fallback;
        };
    }

    public static final class BuiltInSkillSlot {
        public String skill;
        public String id;
        public String displayName;
        public boolean enabled = true;
        public String icon;
        public String perkAttribute;
        public String perkOperation;
        public Double perkAmountPerStep;
        public Integer perkStep;

        public BuiltInSkillSlot() {
            this("", "", "", true, "", null, null, null, null);
        }

        public BuiltInSkillSlot(String skill, String id, String displayName, boolean enabled, String icon,
                                String perkAttribute, String perkOperation, Double perkAmountPerStep, Integer perkStep) {
            this.skill = skill == null ? "" : skill;
            this.id = id == null ? "" : id;
            this.displayName = displayName == null ? "" : displayName;
            this.enabled = enabled;
            this.icon = icon == null ? "" : icon;
            this.perkAttribute = perkAttribute;
            this.perkOperation = perkOperation;
            this.perkAmountPerStep = perkAmountPerStep;
            this.perkStep = perkStep;
        }

        public static BuiltInSkillSlot defaults(Skill skill) {
            String id = skill.getSerializedName();
            return new BuiltInSkillSlot(id, id, "", true, "", null, null, null, 5);
        }

        public static BuiltInSkillSlot disabled(Skill skill) {
            String id = skill.getSerializedName();
            return new BuiltInSkillSlot(id, id, "", false, "", null, null, null, 5);
        }

        public Skill getBaseSkill() {
            return Skill.fromString(skill);
        }

        public boolean isEnabled() {
            return enabled && getBaseSkill() != null && !getId().isBlank();
        }

        public String getId() {
            return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        }

        /** Blank means use the original translated built-in name. */
        public String getDisplayName() {
            return displayName == null ? "" : displayName.trim();
        }

        public String getIcon() {
            return icon == null ? "" : icon.trim();
        }

        public ResourceLocation getResolvedIcon() {
            if (getIcon().isBlank()) return null;
            return ResourceLocation.tryParse(getIcon());
        }

        public String getPerkAttributeOverride() {
            return perkAttribute == null ? null : perkAttribute.trim();
        }

        public String getPerkOperationOverride() {
            return perkOperation == null ? null : perkOperation.trim().toUpperCase(Locale.ROOT);
        }

        public Double getPerkAmountPerStepOverride() {
            return perkAmountPerStep;
        }

        public int getPerkStep() {
            return perkStep == null ? 5 : Math.max(1, perkStep);
        }
    }

    public static final class CustomSkillSlot {
        public String id;
        public String displayName;
        public Boolean enabled = true;
        public String perkAttribute;
        public String icon;
        public String perkOperation;
        public double perkAmountPerStep;
        public int perkStep;

        public CustomSkillSlot() {
            this("", "", "", "", "ADDITION", 0.0, 5);
        }

        public CustomSkillSlot(String id, String displayName) {
            this(id, displayName, "", "", "ADDITION", 0.0, 5);
        }

        public CustomSkillSlot(String id, String displayName, String perkAttribute, String icon, String perkOperation, double perkAmountPerStep, int perkStep) {
            this.id = id == null ? "" : id;
            this.displayName = displayName == null ? "" : displayName;
            this.perkAttribute = perkAttribute == null ? "" : perkAttribute;
            this.icon = icon == null ? "" : icon;
            this.perkOperation = perkOperation == null ? "ADDITION" : perkOperation;
            this.perkAmountPerStep = Math.max(0.0, perkAmountPerStep);
            this.perkStep = Math.max(1, perkStep);
        }

        public boolean isEnabled() {
            return !id.isBlank() && (enabled == null || enabled);
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName == null || displayName.isBlank() ? id : displayName;
        }

        public String getPerkAttribute() {
            return perkAttribute == null ? "" : perkAttribute.trim();
        }

        public String getIcon() {
            return icon == null ? "" : icon.trim();
        }

        public String getPerkOperation() {
            return perkOperation == null || perkOperation.isBlank() ? "ADDITION" : perkOperation.trim().toUpperCase(Locale.ROOT);
        }

        public double getPerkAmountPerStep() {
            return perkAmountPerStep;
        }

        public int getPerkStep() {
            return Math.max(1, perkStep);
        }

        public boolean hasPerk() {
            return !getPerkAttribute().isBlank() && perkAmountPerStep > 0.0;
        }

        public Attribute getResolvedPerkAttribute() {
            if (!hasPerk()) {
                return null;
            }

            try {
                return BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(getPerkAttribute()));
            } catch (Exception e) {
                System.err.println("[Reskillable] Invalid custom perk attribute for skill '" + id + "': " + perkAttribute);
                return null;
            }
        }

        public AttributeModifier.Operation getResolvedPerkOperation() {
            try {
                String op = getPerkOperation();
                return switch (op) {
                    case "ADD_VALUE", "VALUE", "ADDITION" -> AttributeModifier.Operation.ADD_VALUE;
                    case "ADD_MULTIPLIED_BASE", "MULTIPLIED_BASE", "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case "ADD_MULTIPLIED_TOTAL", "MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    default -> AttributeModifier.Operation.ADD_VALUE;
                };
            } catch (Exception e) {
                System.err.println("[Reskillable] Invalid custom perk operation for skill '" + id + "': " + perkOperation);
                return AttributeModifier.Operation.ADD_VALUE;
            }
        }

        public ResourceLocation getResolvedIcon() {
            if (getIcon().isBlank()) {
                return null;
            }

            try {
                return ResourceLocation.parse(getIcon());
            } catch (Exception e) {
                System.err.println("[Reskillable] Invalid custom icon for skill '" + id + "': " + icon);
                return null;
            }
        }
    }

    public static boolean isSkillLevelingEnabled() {
        return enableSkillLeveling;
    }

    public static boolean isSkillUpMessageEnabled() {
        return ENABLE_SKILL_UP_MESSAGE.get();
    }

    public static Attribute getConfiguredMagicAttribute() {
        try {
            String fullPath = MAGIC_ATTRIBUTE_ID.get();
            String[] splitPath = fullPath.split(":", 2);
            ResourceLocation id;

            if (splitPath.length == 2) {
                id = ResourceLocation.fromNamespaceAndPath(splitPath[0], splitPath[1]);
            } else {
                id = ResourceLocation.withDefaultNamespace(splitPath[0]);
            }

            return BuiltInRegistries.ATTRIBUTE.get(id);
        } catch (Exception e) {
            System.err.println("[Reskillable] Invalid attribute ID in config for magicAttribute: " + MAGIC_ATTRIBUTE_ID.get());
            return (Attribute) Attributes.LUCK; // fallback
        }
    }

    public static Attribute getConfiguredAttribute(Skill skill) {
        try {
            String raw = switch (skill) {
                case ATTACK -> ATTACK_ATTRIBUTE_ID.get();
                case DEFENSE -> DEFENSE_ATTRIBUTE_ID.get();
                case AGILITY -> AGILITY_ATTRIBUTE_ID.get();
                case BUILDING -> BUILDING_ATTRIBUTE_ID.get();
                case MAGIC -> MAGIC_ATTRIBUTE_ID.get();
                default -> null;
            };

            if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("none")) {
                return null;
            }

            return BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(raw));
        } catch (Exception e) {
            System.err.println("[Reskillable] Invalid attribute ID for skill " + skill.name());
            return null;
        }
    }

    public static AttributeModifier.Operation getConfiguredOperation(Skill skill, AttributeModifier.Operation fallback) {
        try {
            String raw = switch (skill) {
                case ATTACK -> ATTACK_OPERATION.get();
                case DEFENSE -> DEFENSE_OPERATION.get();
                case AGILITY -> AGILITY_OPERATION.get();
                case BUILDING -> BUILDING_OPERATION.get();
                case MAGIC -> MAGIC_OPERATION.get();
                default -> null;
            };

            return parseAttributeOperation(raw, fallback);
        } catch (Exception e) {
            System.err.println("[Reskillable] Invalid operation for skill " + skill.name());
            return fallback;
        }
    }

    private static Map<String, Requirement[]> parseSkillLocks(Map<String, List<String>> data) {
        Map<String, Requirement[]> locks = new HashMap<>();

        if (data == null) {
            System.err.println("No data found for skill locks.");
            return locks;
        }

        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            try {
                String rawKey = entry.getKey();
                List<String> rawRequirements = entry.getValue();
                Requirement[] requirements = parseRequirements(rawRequirements);

                if (requirements.length == 0) {
                    System.err.println("No valid requirements found for key: " + rawKey);
                    continue;
                }

                if (rawKey.contains("*")) {
                    expandWildcardLock(rawKey, requirements, locks);
                } else {
                    mergeRequirementsIntoLock(rawKey, requirements, locks);
                }
            } catch (Exception e) {
                System.err.println("Error parsing skill lock for key: " + entry.getKey());
                e.printStackTrace();
            }
        }

        return locks;
    }

    private static Requirement[] parseRequirements(List<String> rawRequirements) {
        List<Requirement> parsed = new ArrayList<>();

        for (String rawRequirement : rawRequirements) {
            try {
                String[] reqParts = rawRequirement.split(":");
                if (reqParts.length != 2) {
                    System.err.println("Invalid requirement format: " + rawRequirement);
                    continue;
                }

                String skillName = reqParts[0].trim().toLowerCase(Locale.ROOT);
                int level = Integer.parseInt(reqParts[1].trim());

                Skill builtInSkill = resolveBuiltInSkill(skillName);

                if (builtInSkill != null) {
                    parsed.add(new Requirement(builtInSkill, level));
                    continue;
                }

                CustomSkillSlot customSkill = findCustomSkillById(skillName);
                if (customSkill != null) {
                    parsed.add(new Requirement(customSkill.getId(), level));
                    continue;
                }

                System.err.println("Unknown skill in requirement: " + rawRequirement);
            } catch (Exception e) {
                System.err.println("Failed to parse requirement: " + rawRequirement);
                e.printStackTrace();
            }
        }

        return parsed.toArray(new Requirement[0]);
    }

    private static void expandWildcardLock(String wildcardKey, Requirement[] requirements, Map<String, Requirement[]> locks) {
        String[] parts = wildcardKey.split(":", 2);
        if (parts.length != 2) {
            System.err.println("Invalid wildcard key format (must be namespace:path): " + wildcardKey);
            return;
        }

        String namespace = parts[0];
        String pathPattern = parts[1];

        int starIndex = pathPattern.indexOf('*');

        if (starIndex == -1) {
            System.err.println("Wildcard key does not contain '*': " + wildcardKey);
            return;
        }

        if (starIndex != pathPattern.length() - 1) {
            System.err.println("Wildcard '*' is only supported at the end of the path: " + wildcardKey);
            return;
        }

        String prefix = pathPattern.substring(0, pathPattern.length() - 1);

        int matches = 0;

        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(namespace)) {
                continue;
            }

            if (id.getPath().startsWith(prefix)) {
                mergeRequirementsIntoLock(id.toString(), requirements, locks);
                matches++;
            }
        }

        if (matches == 0) {
            System.err.println("Wildcard lock matched no items: " + wildcardKey);
        } else {
            System.out.println("Wildcard lock '" + wildcardKey + "' matched " + matches + " item(s).");
        }
    }

    private static void mergeRequirementsIntoLock(String key, Requirement[] newRequirements, Map<String, Requirement[]> locks) {
        Requirement[] existingRequirements = locks.get(key);

        if (existingRequirements == null || existingRequirements.length == 0) {
            locks.put(key, newRequirements);
            return;
        }

        Map<String, Integer> mergedLevels = new LinkedHashMap<>();

        for (Requirement req : existingRequirements) {
            if (req != null && req.skill != null && !req.skill.isBlank()) {
                mergedLevels.merge(req.skill, req.level, Integer::sum);
            }
        }

        for (Requirement req : newRequirements) {
            if (req != null && req.skill != null && !req.skill.isBlank()) {
                mergedLevels.merge(req.skill, req.level, Integer::sum);
            }
        }

        List<Requirement> mergedRequirements = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : mergedLevels.entrySet()) {
            Skill builtInSkill = resolveBuiltInSkill(entry.getKey());

            if (builtInSkill != null) {
                mergedRequirements.add(new Requirement(builtInSkill, entry.getValue()));
            } else {
                mergedRequirements.add(new Requirement(entry.getKey(), entry.getValue()));
            }
        }

        locks.put(key, mergedRequirements.toArray(new Requirement[0]));
    }

    private static Map<String, Map<String, List<String>>> loadJsonConfig(String filename, String defaultContent, String expectedKey) {
        File file = new File(filename);

        if (!file.exists()) {
            if (createDefaultJsonFile(file, defaultContent)) {
                System.out.println("Default file created: " + filename);
            } else {
                System.err.println("Failed to create default file: " + filename);
            }
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if (!jsonObject.has(expectedKey)) {
                System.err.println("Missing '" + expectedKey + "' key in JSON: " + filename);
                return new HashMap<>();
            }

            Type mapType = new TypeToken<Map<String, Map<String, List<String>>>>() {}.getType();
            return new Gson().fromJson(jsonObject, mapType);
        } catch (Exception e) {
            System.err.println("Error loading JSON from file: " + filename);
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static boolean createDefaultJsonFile(File file, String content) {
        try {
            if (file.getParentFile().mkdirs() || file.getParentFile().exists()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(content);
                    return true;
                }
            } else {
                System.err.println("Failed to create directories for file: " + file.getPath());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static final class ConfigValue<T> {
        private T value;
        public ConfigValue(T value) { this.value = value; }
        public T get() { return value; }
        public void set(T value) { this.value = value; }
    }

    private static void loadMainConfig() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("reskillable-common.toml");
        File file = path.toFile();
        if (!file.exists()) {
            writeDefaultMainConfig(file);
            return;
        }

        try {
            for (String raw : java.nio.file.Files.readAllLines(path)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                int split = line.indexOf('=');
                String key = unquote(line.substring(0, split).trim());
                String value = line.substring(split + 1).trim();
                applyMainConfigValue(key, value);
            }
        } catch (Exception e) {
            System.err.println("[Reskillable] Failed to read " + path + "; using defaults.");
            e.printStackTrace();
        }
    }

    private static void applyMainConfigValue(String key, String raw) {
        try {
            switch (key) {
                case "disableWoolDrops" -> DISABLE_WOOL.set(parseBoolean(raw));
                case "deathSkillReset" -> DEATH_RESET.set(parseBoolean(raw));
                case "HealthBonus" -> HEALTH_BONUS.set(parseBoolean(raw));
                case "showTabButtons" -> SHOW_TAB_BUTTONS.set(parseBoolean(raw));
                case "xpScalingMultiplier" -> XP_SCALING_MULTIPLIER.set(clamp(parseDouble(raw), 0.1, 10.0));
                case "maximumLevel" -> MAXIMUM_LEVEL.set((int) clamp(parseInt(raw), 2, 100));
                case "enableSkillLeveling" -> ENABLE_SKILL_LEVELING.set(parseBoolean(raw));
                case "enableSkillUpMessage" -> ENABLE_SKILL_UP_MESSAGE.set(parseBoolean(raw));
                case "skillAliases" -> SKILL_ALIAS.set(parseStringList(raw));
                case "attackAttribute" -> ATTACK_ATTRIBUTE_ID.set(parseString(raw));
                case "attackOperation" -> ATTACK_OPERATION.set(parseString(raw));
                case "attackDamageBonus" -> ATTACK_DAMAGE_BONUS.set(clamp(parseDouble(raw), 0.0, 10.0));
                case "defenseAttribute" -> DEFENSE_ATTRIBUTE_ID.set(parseString(raw));
                case "defenseOperation" -> DEFENSE_OPERATION.set(parseString(raw));
                case "armorBonus" -> ARMOR_BONUS.set(clamp(parseDouble(raw), 0.0, 10.0));
                case "agilityAttribute" -> AGILITY_ATTRIBUTE_ID.set(parseString(raw));
                case "agilityOperation" -> AGILITY_OPERATION.set(parseString(raw));
                case "Agility bonus" -> MOVEMENT_SPEED_BONUS.set(clamp(parseDouble(raw), 0.0, 1.0));
                case "magicAttribute" -> MAGIC_ATTRIBUTE_ID.set(parseString(raw));
                case "magicOperation" -> MAGIC_OPERATION.set(parseString(raw));
                case "Magic Bonus" -> LUCK_BONUS.set(clamp(parseDouble(raw), 0.0, 10.0));
                case "buildingAttribute" -> {
                    String id = parseString(raw);
                    if ("forge:block_reach".equals(id) || "neoforge:block_reach".equals(id)) id = "minecraft:player.block_interaction_range";
                    BUILDING_ATTRIBUTE_ID.set(id);
                }
                case "buildingOperation" -> BUILDING_OPERATION.set(parseString(raw));
                case "Building Bonus" -> BLOCK_REACH_BONUS.set(clamp(parseDouble(raw), 0.0, 5.0));
                case "miningSpeedMultiplier" -> MINING_SPEED_MULTIPLIER.set(clamp(parseDouble(raw), 0.0, 5.0));
                case "Farming Bonus" -> CROP_GROWTH_CHANCE.set(clamp(parseDouble(raw), 0.0, 1.0));
                case "gathering Bonus" -> GATHERING_XP_BONUS.set(clamp(parseDouble(raw), 0.0, 1.0));
                case "levelsPerHeart" -> LEVELS_PER_HEART.set((int) clamp(parseInt(raw), 1, 100));
                case "healthPerHeart" -> HEALTH_PER_HEART.set(clamp(parseDouble(raw), 0.5, 20.0));
                case "skill_level_gates" -> SKILL_LEVEL_GATES.set(parseStringList(raw));
                case "enableSecondSkillPage" -> ENABLE_SECOND_SKILL_PAGE.set(parseBoolean(raw));
                case "subpageNavPosition" -> SUBPAGE_NAV_POSITION.set(parseString(raw));
                case "showSubpageTitles" -> SHOW_SUBPAGE_TITLES.set(parseBoolean(raw));
                case "maxTotalSpentLevels" -> MAX_TOTAL_SPENT_LEVELS.set(Math.max(-1, parseInt(raw)));
            }
        } catch (Exception e) {
            System.err.println("[Reskillable] Invalid config value for '" + key + "': " + raw);
        }
    }

    private static boolean parseBoolean(String value) { return Boolean.parseBoolean(value.trim()); }
    private static int parseInt(String value) { return Integer.parseInt(value.trim()); }
    private static double parseDouble(String value) { return Double.parseDouble(value.trim()); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static String parseString(String value) { return unquote(value.trim()); }

    private static String unquote(String value) {
        String v = value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) return v.substring(1, v.length() - 1);
        return v;
    }

    private static List<? extends String> parseStringList(String raw) {
        String v = raw.trim();
        if (!v.startsWith("[") || !v.endsWith("]")) return List.of();
        v = v.substring(1, v.length() - 1).trim();
        if (v.isEmpty()) return List.of();
        List<String> values = new ArrayList<>();
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '"' && (i == 0 || v.charAt(i - 1) != '\\')) { inString = !inString; continue; }
            if (c == ',' && !inString) { values.add(current.toString().trim()); current.setLength(0); }
            else current.append(c);
        }
        if (!current.isEmpty()) values.add(current.toString().trim());
        return values;
    }

    private static void writeDefaultMainConfig(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            String content = """
                    # Reskillable Reimagined common configuration (Fabric)
                    disableWoolDrops = true
                    deathSkillReset = false
                    HealthBonus = true
                    showTabButtons = true
                    xpScalingMultiplier = 1.0
                    maximumLevel = 32
                    enableSkillLeveling = true
                    enableSkillUpMessage = true
                    skillAliases = ["defense=defense"]
                    attackAttribute = "minecraft:generic.attack_damage"
                    attackOperation = "MULTIPLY_TOTAL"
                    attackDamageBonus = 0.15
                    defenseAttribute = "minecraft:generic.armor"
                    defenseOperation = "MULTIPLY_TOTAL"
                    armorBonus = 0.15
                    agilityAttribute = "minecraft:generic.movement_speed"
                    agilityOperation = "MULTIPLY_TOTAL"
                    "Agility bonus" = 0.05
                    magicAttribute = "minecraft:generic.luck"
                    magicOperation = "MULTIPLY_TOTAL"
                    "Magic Bonus" = 0.05
                    buildingAttribute = "minecraft:player.block_interaction_range"
                    buildingOperation = "ADDITION"
                    "Building Bonus" = 0.25
                    miningSpeedMultiplier = 0.25
                    "Farming Bonus" = 0.25
                    "gathering Bonus" = 0.05
                    levelsPerHeart = 10
                    healthPerHeart = 2.0
                    skill_level_gates = []
                    enableSecondSkillPage = false
                    subpageNavPosition = "BOTTOM"
                    showSubpageTitles = true
                    maxTotalSpentLevels = -1
                    """;
            java.nio.file.Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            System.err.println("[Reskillable] Failed to create " + file);
            e.printStackTrace();
        }
    }

    public static boolean getDisableWool() {
        return disableWool;
    }

    public static boolean shouldShowTabButtons() {
        return showTabButtons;
    }

    public static boolean getDeathReset() {
        return deathReset;
    }

    public static double getXpScalingMultiplier() {
        return XP_SCALING_MULTIPLIER.get();
    }

    public static int calculateCostForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be 1 or greater");
        }

        int[] totalXpCosts = {
                7, 16, 27, 40, 55, 72, 91, 112, 135, 160,
                187, 216, 247, 280, 315, 352, 394, 441, 493, 550,
                612, 679, 751, 828, 910, 997, 1089, 1186, 1288, 1395,
                1507, 1628, 1758, 1897, 2045, 2202, 2368, 2543, 2727, 2920,
                3122, 3333, 3553, 3782, 4020, 4267, 4523, 4788, 5062, 5345
        };

        double multiplier = getXpScalingMultiplier();

        if (level <= totalXpCosts.length) {
            return (int) Math.ceil(totalXpCosts[level - 1] * multiplier);
        }

        int vanillaXp = (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        return (int) Math.ceil(vanillaXp * multiplier);
    }

    public static int calculateExperienceCost(int level) {
        int[] totalXpCosts = {
                7, 16, 27, 40, 55, 72, 91, 112, 135, 160,
                187, 216, 247, 280, 315, 352, 394, 441, 493, 550,
                612, 679, 751, 828, 910, 997, 1089, 1186, 1288, 1395,
                1507, 1628, 1758, 1897, 2045, 2202, 2368, 2543, 2727, 2920,
                3122, 3333, 3553, 3782, 4020, 4267, 4523, 4788, 5062, 5345
        };

        double multiplier = getXpScalingMultiplier();

        if (level <= 1) return (int) Math.ceil(totalXpCosts[0] * multiplier);
        if (level <= totalXpCosts.length) {
            int baseCost = totalXpCosts[level - 1] - totalXpCosts[level - 2];
            return (int) Math.ceil(baseCost * multiplier);
        }

        int cost;
        if (level >= 32) {
            cost = 9 * level - 158;
        } else if (level >= 17) {
            cost = 5 * level - 38;
        } else {
            cost = 2 * level + 7;
        }

        return (int) Math.ceil(cost * multiplier);
    }

    public static int getCumulativeXpForLevel(int level) {
        if (level <= 0) return 0;

        double multiplier = getXpScalingMultiplier();
        if (level <= 16) {
            return (int) Math.ceil((level * (level + 1)) / 2 * 2 + 7 * level * multiplier);
        } else if (level <= 31) {
            return (int) Math.ceil((2.5 * level * level - 40.5 * level + 360) * multiplier);
        } else {
            return (int) Math.ceil((4.5 * level * level - 162.5 * level + 2220) * multiplier);
        }
    }

    public static int getMaxLevel() {
        return maximumLevel;
    }

    public static Requirement[] getRequirements(ResourceLocation key) {
        return skillLocks.get(key.toString());
    }

    public static Requirement[] getCraftRequirements(ResourceLocation key) {
        return craftSkillLocks.get(key.toString());
    }

    public static Requirement[] getEntityAttackRequirements(ResourceLocation key) {
        return attackSkillLocks.get(key.toString());
    }

    public static Map<String, Requirement[]> getSkillLocks() {
        return skillLocks;
    }

    public static void setSkillLocks(Map<String, Requirement[]> newSkillLocks) {
        if (skillLocks == null) {
            skillLocks = new HashMap<>();
        }
        skillLocks.putAll(newSkillLocks);
    }

    public static Map<String, Requirement[]> getCraftSkillLocks() {
        return craftSkillLocks;
    }

    public static void setCraftSkillLocks(Map<String, Requirement[]> newCraftSkillLocks) {
        craftSkillLocks = newCraftSkillLocks;
    }

    public static Map<String, Requirement[]> getAttackSkillLocks() {
        return attackSkillLocks;
    }

    public static void setAttackSkillLocks(Map<String, Requirement[]> newAttackSkillLocks) {
        attackSkillLocks = newAttackSkillLocks;
    }

    private static final Map<String, List<String>> RANGED_WEAPON_REQUIREMENTS = Map.of(
            "minecraft:bow", List.of("agility:10", "defense:5"),
            "minecraft:crossbow", List.of("agility:10", "defense:5"),
            "modid:longbow", List.of("agility:15", "defense:10"),
            "modid:netherite_crossbow", List.of("agility:30", "defense:25")
    );

    private static final Map<String, ArmorStats> VANILLA_ARMOR_BENCHMARKS = Map.of(
            "leather", new ArmorStats(3, 0.0),
            "chainmail", new ArmorStats(12, 0.0),
            "iron", new ArmorStats(15, 0.0),
            "gold", new ArmorStats(11, 0.0),
            "diamond", new ArmorStats(20, 2.0),
            "netherite", new ArmorStats(20, 3.0)
    );

    public static int scanModItems(String modId) {
        Map<String, List<String>> newEntries = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && id.getNamespace().equals(modId)) {
                List<String> defaultRequirement = getDefaultRequirement(item);
                if (!defaultRequirement.isEmpty()) {
                    newEntries.put(id.toString(), defaultRequirement);
                }
            }
        }

        if (newEntries.isEmpty()) {
            return 0;
        }

        try {
            File file = FabricLoader.getInstance().getConfigDir().resolve("reskillable/skill_locks.json").toFile();
            JsonObject skillLocksJson = new JsonObject();

            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject loaded = new Gson().fromJson(reader, JsonObject.class);
                    if (loaded != null) {
                        skillLocksJson = loaded;
                    }
                }
            }

            JsonObject skillLocks = skillLocksJson.has("skillLocks")
                    ? skillLocksJson.getAsJsonObject("skillLocks")
                    : new JsonObject();

            for (Map.Entry<String, List<String>> entry : newEntries.entrySet()) {
                if (!skillLocks.has(entry.getKey())) {
                    skillLocks.add(entry.getKey(), new Gson().toJsonTree(entry.getValue()));
                }
            }

            skillLocksJson.add("skillLocks", skillLocks);

            try (FileWriter writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(skillLocksJson, writer);
            }

            return newEntries.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static List<String> getDefaultRequirement(Item item) {
        if (item instanceof ArmorItem armor) {
            int defense = armor.getDefense();
            double toughness = armor.getToughness();
            int level = determineArmorSkillLevel(defense, toughness);
            return List.of("defense:" + level);
        } else if (item instanceof SwordItem sword) {
            double attackDamage = sword.getTier().getAttackDamageBonus() + 3.0D;
            return List.of("attack:" + determineAttackLevel(attackDamage));
        } else if (item instanceof PickaxeItem pickaxe) {
            int harvestLevel = pickaxe.getTier().getEnchantmentValue();
            return List.of("mining:" + determineHarvestLevel(harvestLevel));
        } else if (item instanceof ShovelItem shovel) {
            int harvestLevel = shovel.getTier().getEnchantmentValue();
            return List.of("gathering:" + determineHarvestLevel(harvestLevel));
        } else if (item instanceof AxeItem axe) {
            int harvestLevel = axe.getTier().getEnchantmentValue();
            return List.of("gathering:" + determineHarvestLevel(harvestLevel));
        } else if (item instanceof HoeItem hoe) {
            int harvestLevel = hoe.getTier().getEnchantmentValue();
            return List.of("farming:" + determineHarvestLevel(harvestLevel));
        } else if (item instanceof BowItem || item instanceof CrossbowItem) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null) {
                String itemKey = itemId.toString();
                return RANGED_WEAPON_REQUIREMENTS.getOrDefault(itemKey, List.of("agility:10", "defense:5"));
            }
        }else if (item.getClass().getSimpleName().toLowerCase().contains("scythe")) {
            return List.of("attack:20", "defense:15");
        } else if (item.getClass().getSimpleName().toLowerCase().contains("staff")) {
            return List.of("magic:25");
        }
        return List.of();
    }

    private static int determineArmorSkillLevel(int defense, double toughness) {
        for (Map.Entry<String, ArmorStats> entry : VANILLA_ARMOR_BENCHMARKS.entrySet()) {
            ArmorStats benchmark = entry.getValue();
            if (defense <= benchmark.totalDefense && toughness <= benchmark.toughness) {
                return switch (entry.getKey()) {
                    case "leather" -> 5;
                    case "chainmail" -> 10;
                    case "iron" -> 15;
                    case "gold" -> 15;
                    case "diamond" -> 20;
                    case "netherite" -> 30;
                    default -> 5;
                };
            }
        }
        return 35;
    }

    private static int determineAttackLevel(double attackDamage) {
        if (attackDamage < 6) return 5;
        if (attackDamage < 10) return 15;
        return 30;
    }

    private static int determineHarvestLevel(int harvestLevel) {
        if (harvestLevel < 2) return 5;
        if (harvestLevel == 2) return 15;
        return 30;
    }
    public static int getMaxSpendableLevels() {
        return MAX_TOTAL_SPENT_LEVELS.get();
    }

    private static class ArmorStats {
        int totalDefense;
        double toughness;

        ArmorStats(int totalDefense, double toughness) {
            this.totalDefense = totalDefense;
            this.toughness = toughness;
        }
    }
}