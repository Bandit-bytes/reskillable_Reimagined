package net.bandit.reskillable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mod.EventBusSubscriber
public class Configuration {
    public static final ForgeConfigSpec CONFIG_SPEC;


    private static final ForgeConfigSpec.BooleanValue DISABLE_WOOL;
    private static final ForgeConfigSpec.BooleanValue SHOW_TAB_BUTTONS;
    private static final ForgeConfigSpec.BooleanValue DEATH_RESET;
    private static final ForgeConfigSpec.IntValue STARTING_COST;
    private static final ForgeConfigSpec.IntValue MAXIMUM_LEVEL;
    private static final ForgeConfigSpec.DoubleValue XP_SCALING_MULTIPLIER;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKILL_ALIAS;

    private static boolean disableWool;
    private static boolean showTabButtons;
    private static boolean deathReset;
    private static int startingCost;
    private static int maximumLevel;
    private static double xpScalingMultiplier;
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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Disable wool drops to force the player to get shears.");
        DISABLE_WOOL = builder.define("disableWoolDrops", true);

        builder.comment("Reset all skills to 1 when a player dies.");
        DEATH_RESET = builder.define("deathSkillReset", false);

        builder.comment("Toggle the visibility of the tab buttons in the inventory.");
        SHOW_TAB_BUTTONS = builder.define("showTabButtons", true);

        builder.comment("Starting cost of upgrading to level 2, in experience points.");
        STARTING_COST = builder.defineInRange("startingCost", 7, 0, 1000);

        builder.comment("Global scaling multiplier for XP costs.");
        XP_SCALING_MULTIPLIER = builder.defineInRange("xpScalingMultiplier", 1.0, 0.1, 10.0);

        builder.comment("Maximum level each skill can be upgraded to.");
        MAXIMUM_LEVEL = builder.defineInRange("maximumLevel", 32, 2, 100);

        builder.comment("List of substitutions to perform in names in skill lock lists.",
                "Useful if you're using a resource pack to change the names of skills, this config doesn't affect gameplay, just accepted values in other configs so it's easier to think about",
                "Format: key=value",
                "Valid values: attack, defense, mining, gathering, farming, building, agility, magic");
        SKILL_ALIAS = builder.defineList("skillAliases", List.of("defense=defense"), obj -> true);

        CONFIG_SPEC = builder.build();
    }

    // Initialize

    public static void load() {
        disableWool = DISABLE_WOOL.get();
        showTabButtons = SHOW_TAB_BUTTONS.get();
        deathReset = DEATH_RESET.get();
        startingCost = STARTING_COST.get();
//        costIncrease = COST_INCREASE.get();
        xpScalingMultiplier = XP_SCALING_MULTIPLIER.get();
        maximumLevel = MAXIMUM_LEVEL.get();

        Map<String, Map<String, List<String>>> skillData = loadJsonConfig(FMLPaths.CONFIGDIR.get().resolve("reskillable/skill_locks.json").toString(), DEFAULT_SKILL_LOCKS);
        Map<String, Map<String, List<String>>> craftData = loadJsonConfig(FMLPaths.CONFIGDIR.get().resolve("reskillable/craft_skill_locks.json").toString(), DEFAULT_CRAFT_SKILL_LOCKS);
        Map<String, Map<String, List<String>>> attackData = loadJsonConfig(FMLPaths.CONFIGDIR.get().resolve("reskillable/attack_skill_locks.json").toString(), DEFAULT_ATTACK_SKILL_LOCKS);

        skillLocks = parseSkillLocks(skillData.get("skillLocks"));
        craftSkillLocks = parseSkillLocks(craftData.get("craftSkillLocks"));
        attackSkillLocks = parseSkillLocks(attackData.get("attackSkillLocks"));
    }

    private static Map<String, Requirement[]> parseSkillLocks(Map<String, List<String>> data) {
        Map<String, Requirement[]> locks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            List<String> rawRequirements = entry.getValue();
            Requirement[] requirements = new Requirement[rawRequirements.size()];

            for (int i = 0; i < rawRequirements.size(); i++) {
                String[] skills = rawRequirements.get(i).split(", ");
                Requirement[] multipleRequirements = new Requirement[skills.length];

                for (int j = 0; j < skills.length; j++) {
                    String[] req = skills[j].split(":");

                    for (String alias : SKILL_ALIAS.get()) {
                        String[] aliasInfo = alias.split("=");
                        if (req[0].equalsIgnoreCase(aliasInfo[0])) {
                            req[0] = aliasInfo[1];
                        }
                    }

                    multipleRequirements[j] = new Requirement(Skill.valueOf(req[0].toUpperCase()), Integer.parseInt(req[1]));
                }

                requirements[i] = multipleRequirements[0];
            }

            locks.put(entry.getKey(), requirements);
        }

        return locks;
    }

    private static Map<String, Map<String, List<String>>> loadJsonConfig(String filename, String defaultContent) {
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
            Type mapType = new TypeToken<Map<String, Map<String, List<String>>>>() {
            }.getType();
            Map<String, Map<String, List<String>>> data = new Gson().fromJson(jsonObject, mapType);
            System.out.println("Loaded data from " + filename + ": " + data);
            return data;
        } catch (Exception e) {
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
        return xpScalingMultiplier;
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

        if (level <= 50) {
            return (int) Math.ceil(totalXpCosts[level - 1] * multiplier);
        }
        int baseCost = totalXpCosts[49];
        int additionalCostPerLevel = 300;
        int extraLevels = level - 50;
        int dynamicCost = baseCost + (extraLevels * additionalCostPerLevel);

        return (int) Math.ceil(dynamicCost * multiplier);
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

    public static ForgeConfigSpec getConfig() {
        return CONFIG_SPEC;
    }

    public static Map<String, Requirement[]> getSkillLocks() {
        return skillLocks;
    }

    public static void setSkillLocks(Map<String, Requirement[]> newSkillLocks) {
        skillLocks = newSkillLocks;
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

    public static Type getSkillLocksType() {
        return new TypeToken<Map<String, List<String>>>() {
        }.getType();
    }
    public static int calculateExperienceCost(int level) {
        if (level <= 0) return 0;

        int baseCost;
        if (level <= 16) {
            baseCost = 2 * level + 7;
        } else if (level <= 31) {
            baseCost = 5 * level - 38;
        } else {
            baseCost = 9 * level - 158;
        }

        double multiplier = getXpScalingMultiplier();
        return (int) Math.ceil(baseCost * multiplier);
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

}
