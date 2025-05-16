package net.bandit.reskillable.client.data;

import net.bandit.reskillable.common.commands.skills.Skill;

import java.util.EnumMap;
import java.util.Map;

public class ClientSkillModel {
    private static final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
    private static final Map<Skill, Boolean> disabledPerks = new EnumMap<>(Skill.class);

    public static void setLevels(Map<Skill, Integer> syncedLevels) {
        levels.clear();
        levels.putAll(syncedLevels);
    }

    public static void setDisabledPerks(Map<Skill, Boolean> perks)
    {
        disabledPerks.clear();
        disabledPerks.putAll(perks);
    }

    public static int getLevel(Skill skill) {
        return levels.getOrDefault(skill, 1);
    }

    public static boolean isPerkEnabled(Skill skill) {
        return !disabledPerks.getOrDefault(skill, false);
    }
}
