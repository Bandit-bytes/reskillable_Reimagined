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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

@EventBusSubscriber
public class Configuration {
    public static final ModConfigSpec CONFIG_SPEC;

    private static final ModConfigSpec.BooleanValue DISABLE_WOOL;
    private static final ModConfigSpec.BooleanValue SHOW_TAB_BUTTONS;
    private static final ModConfigSpec.BooleanValue DEATH_RESET;
    public static final ModConfigSpec.BooleanValue HEALTH_BONUS;
    public static ModConfigSpec.ConfigValue<List<? extends String>> SKILL_LEVEL_GATES;
    private static final ModConfigSpec.IntValue MAXIMUM_LEVEL;
    private static final ModConfigSpec.DoubleValue XP_SCALING_MULTIPLIER;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SKILL_ALIAS;
    private static final ModConfigSpec.BooleanValue ENABLE_SKILL_LEVELING;
    private static final ModConfigSpec.BooleanValue ENABLE_SKILL_UP_MESSAGE;
    public static final ModConfigSpec.IntValue LEVELS_PER_HEART;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_HEART;

    public static ModConfigSpec.DoubleValue ATTACK_DAMAGE_BONUS;
    public static ModConfigSpec.DoubleValue ARMOR_BONUS;
    public static ModConfigSpec.DoubleValue MOVEMENT_SPEED_BONUS;
    public static ModConfigSpec.DoubleValue LUCK_BONUS;
    public static ModConfigSpec.DoubleValue BLOCK_REACH_BONUS;
    public static ModConfigSpec.DoubleValue MINING_SPEED_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CROP_GROWTH_CHANCE;
    public static final ModConfigSpec.DoubleValue GATHERING_XP_BONUS;

    public static final ModConfigSpec.ConfigValue<String> MAGIC_ATTRIBUTE_ID;
    public static ModConfigSpec.ConfigValue<String> ATTACK_ATTRIBUTE_ID;
    public static ModConfigSpec.ConfigValue<String> DEFENSE_ATTRIBUTE_ID;
    public static ModConfigSpec.ConfigValue<String> AGILITY_ATTRIBUTE_ID;
    public static ModConfigSpec.ConfigValue<String> BUILDING_ATTRIBUTE_ID;

    public static ModConfigSpec.ConfigValue<String> ATTACK_OPERATION;
    public static ModConfigSpec.ConfigValue<String> DEFENSE_OPERATION;
    public static ModConfigSpec.ConfigValue<String> AGILITY_OPERATION;
    public static ModConfigSpec.ConfigValue<String> BUILDING_OPERATION;
    public static ModConfigSpec.ConfigValue<String> MAGIC_OPERATION;

    private static final ModConfigSpec.BooleanValue ENABLE_SECOND_SKILL_PAGE;

    private static final int MAX_CUSTOM_SKILLS = 8;
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
                  "perkAttribute": "neoforge:swim_speed",
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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Disable wool drops to force the player to get shears.");
        DISABLE_WOOL = builder.define("disableWoolDrops", true);

        builder.comment("Reset all skills to 1 when a player dies.");
        DEATH_RESET = builder.define("deathSkillReset", false);

        builder.comment("Should you gain health on set level ups?");
        HEALTH_BONUS = builder.define("HealthBonus", true);

        builder.comment("Toggle the visibility of the tab buttons in the inventory.");
        SHOW_TAB_BUTTONS = builder.define("showTabButtons", true);

        builder.comment("Global scaling multiplier for XP costs.");
        XP_SCALING_MULTIPLIER = builder.defineInRange("xpScalingMultiplier", 1.0, 0.1, 10.0);

        builder.comment("Maximum level each skill can be upgraded to.");
        MAXIMUM_LEVEL = builder.defineInRange("maximumLevel", 32, 2, 100);

        builder.comment(
                "List of substitutions to perform in names in skill lock lists.",
                "Useful if you're using a resource pack to change the names of skills, this config doesn't affect gameplay, just accepted values in other configs so it's easier to think about",
                "Format: key=value",
                "Valid values: attack, defense, mining, gathering, farming, building, agility, magic"
        );

        ENABLE_SKILL_LEVELING = builder.comment("Enable or disable skill leveling via GUI or selection. If disabled, skill levels must be granted by commands.")
                .define("enableSkillLeveling", true);

        ENABLE_SKILL_UP_MESSAGE = builder.comment("Enable or disable the skill level-up message.")
                .define("enableSkillUpMessage", true);

        SKILL_ALIAS = builder.defineList("skillAliases", List.of("defense=defense"), obj -> true);

        ATTACK_ATTRIBUTE_ID = builder
                .comment("The registry ID of the attribute to use for the Attack skill.")
                .define("attackAttribute", "minecraft:generic.attack_damage");

        ATTACK_OPERATION = builder
                .comment("Operation for the Attack skill perk. Valid: ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL")
                .define("attackOperation", "MULTIPLY_TOTAL");

        ATTACK_DAMAGE_BONUS = builder.defineInRange("attackDamageBonus", 0.15, 0.0, 10.0);

        DEFENSE_ATTRIBUTE_ID = builder
                .comment("The registry ID of the attribute to use for the Defense skill.")
                .define("defenseAttribute", "minecraft:generic.armor");

        DEFENSE_OPERATION = builder
                .comment("Operation for the Defense skill perk. Valid: ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL")
                .define("defenseOperation", "MULTIPLY_TOTAL");

        ARMOR_BONUS = builder.defineInRange("armorBonus", 0.15, 0.0, 10.0);

        AGILITY_ATTRIBUTE_ID = builder
                .comment("The registry ID of the attribute to use for the Agility skill.")
                .define("agilityAttribute", "minecraft:generic.movement_speed");

        AGILITY_OPERATION = builder
                .comment("Operation for the Agility skill perk. Valid: ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL")
                .define("agilityOperation", "MULTIPLY_TOTAL");

        MOVEMENT_SPEED_BONUS = builder.defineInRange("Agility bonus", 0.05, 0.0, 1.0);

        MAGIC_ATTRIBUTE_ID = builder
                .comment("The registry ID of the attribute to use for the Magic skill (e.g. 'modid:spell_power')")
                .define("magicAttribute", "minecraft:generic.luck");

        MAGIC_OPERATION = builder
                .comment("Operation for the Magic skill perk. Valid: ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL")
                .define("magicOperation", "MULTIPLY_TOTAL");

        LUCK_BONUS = builder.defineInRange("Magic Bonus", 0.05, 0.0, 10.0);

        BUILDING_ATTRIBUTE_ID = builder
                .comment("The registry ID of the attribute to use for the Building skill.")
                .define("buildingAttribute", "forge:block_reach");

        BUILDING_OPERATION = builder
                .comment("Operation for the Building skill perk. Valid: ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL")
                .define("buildingOperation", "ADDITION");

        BLOCK_REACH_BONUS = builder.defineInRange("Building Bonus", 0.25, 0.0, 5.0);

        MINING_SPEED_MULTIPLIER = builder.defineInRange("miningSpeedMultiplier", 0.25, 0.0, 5.0);
        CROP_GROWTH_CHANCE = builder.defineInRange("Farming Bonus", 0.25, 0.0, 1.0);

        GATHERING_XP_BONUS = builder
                .comment("Bonus XP multiplier per 5 levels of Gathering (e.g., 0.05 = +5% XP per step)")
                .defineInRange("gathering Bonus", 0.05, 0.0, 1.0);

        LEVELS_PER_HEART = builder
                .comment("How many total skill levels are required for each heart gained.")
                .defineInRange("levelsPerHeart", 10, 1, 100);

        HEALTH_PER_HEART = builder
                .comment("How much health (in half-hearts) is granted per configured levelsPerHeart.")
                .defineInRange("healthPerHeart", 2.0, 0.5, 20.0);

        SKILL_LEVEL_GATES = builder
                .comment(
                        "Skill gating rules. (all skills start at level 1 so add 8 to a total count)",
                        "Format: SKILL:MIN_CURRENT_LEVEL:REQS",
                        "Example: ATTACK:10:TOTAL=30,MINING=5,DEFENSE=5",
                        "New token: ADV=<namespace:path> (player must have completed the advancement)",
                        "You can include multiple: ADV=minecraft:story/mine_diamond,ADV=minecraft:nether/root",
                        "Tokens: TOTAL=<n>, OTHER_SKILL=<n>, ADV=<advancement_id>"
                )
                .defineListAllowEmpty("skill_level_gates", List.of(), o -> o instanceof String);

        builder.comment("Enable a second skill page for up to 8 custom skills loaded from custom_skills.json.");
        ENABLE_SECOND_SKILL_PAGE = builder.define("enableSecondSkillPage", false);

        CONFIG_SPEC = builder.build();
    }

    public static void load() {
        disableWool = DISABLE_WOOL.get();
        showTabButtons = SHOW_TAB_BUTTONS.get();
        deathReset = DEATH_RESET.get();
        healthBonus = HEALTH_BONUS.get();
        xpScalingMultiplier = XP_SCALING_MULTIPLIER.get();
        maximumLevel = MAXIMUM_LEVEL.get();
        enableSkillLeveling = ENABLE_SKILL_LEVELING.get();

        Map<String, Map<String, List<String>>> skillData = loadJsonConfig(
                FMLPaths.CONFIGDIR.get().resolve("reskillable/skill_locks.json").toString(),
                DEFAULT_SKILL_LOCKS,
                "skillLocks"
        );

        Map<String, Map<String, List<String>>> craftData = loadJsonConfig(
                FMLPaths.CONFIGDIR.get().resolve("reskillable/craft_skill_locks.json").toString(),
                DEFAULT_CRAFT_SKILL_LOCKS,
                "craftSkillLocks"
        );

        Map<String, Map<String, List<String>>> attackData = loadJsonConfig(
                FMLPaths.CONFIGDIR.get().resolve("reskillable/attack_skill_locks.json").toString(),
                DEFAULT_ATTACK_SKILL_LOCKS,
                "attackSkillLocks"
        );

        customSkills = loadCustomSkills(
                FMLPaths.CONFIGDIR.get().resolve("reskillable/custom_skills.json").toString(),
                DEFAULT_CUSTOM_SKILLS
        );

        skillLocks = parseSkillLocks(skillData.get("skillLocks"));
        craftSkillLocks = parseSkillLocks(craftData.get("craftSkillLocks"));
        attackSkillLocks = parseSkillLocks(attackData.get("attackSkillLocks"));
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

        return new CustomSkillSlot(id, displayName, perkAttribute, icon, perkOperation, perkAmountPerStep, perkStep);
    }

    public static boolean isSecondSkillPageEnabled() {
        return ENABLE_SECOND_SKILL_PAGE.get();
    }

    public static List<CustomSkillSlot> getCustomSkills() {
        return Collections.unmodifiableList(customSkills);
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

        String normalized = skillName.trim().toLowerCase(Locale.ROOT);

        for (Skill skill : Skill.values()) {
            if (skill.name().equalsIgnoreCase(normalized)) {
                return true;
            }
        }

        return isCustomSkill(normalized);
    }

    public static boolean isVanillaSkill(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return false;
        }

        for (Skill skill : Skill.values()) {
            if (skill.name().equalsIgnoreCase(skillName.trim())) {
                return true;
            }
        }

        return false;
    }

    public static final class CustomSkillSlot {
        public String id;
        public String displayName;
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
            return !id.isBlank();
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

            if (raw == null || raw.isBlank()) {
                return fallback;
            }

            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "ADD_VALUE", "VALUE", "ADDITION" -> AttributeModifier.Operation.ADD_VALUE;
                case "ADD_MULTIPLIED_BASE", "MULTIPLIED_BASE", "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "ADD_MULTIPLIED_TOTAL", "MULTIPLIED_TOTAL", "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default -> fallback;
            };
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

                Skill builtInSkill = null;
                try {
                    builtInSkill = Skill.valueOf(skillName.toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {}

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
            Skill builtInSkill = null;
            try {
                builtInSkill = Skill.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {}

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

    public static ModConfigSpec getConfig() {
        return CONFIG_SPEC;
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

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        load();
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
            File file = FMLPaths.CONFIGDIR.get().resolve("reskillable/skill_locks.json").toFile();
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
            double attackDamage = sword.getDamage(new ItemStack(sword));
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

    private static class ArmorStats {
        int totalDefense;
        double toughness;

        ArmorStats(int totalDefense, double toughness) {
            this.totalDefense = totalDefense;
            this.toughness = toughness;
        }
    }
}