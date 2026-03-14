package net.bandit.reskillable.common.commands.skills;

import java.util.Locale;

/**
 * Enum representing different built-in skills in the game.
 */
public enum Skill {
    MINING(0, "skill.mining"),
    GATHERING(1, "skill.gathering"),
    ATTACK(2, "skill.attack"),
    DEFENSE(3, "skill.defense"),
    BUILDING(4, "skill.building"),
    FARMING(5, "skill.farming"),
    AGILITY(6, "skill.agility"),
    MAGIC(7, "skill.magic");

    public final int index;
    public final String displayName;

    Skill(int index, String name) {
        this.index = index;
        this.displayName = name;
    }

    public int getIconIndex() {
        return this.index;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static Skill fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        try {
            return Skill.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean isBuiltInSkill(String name) {
        return fromString(name) != null;
    }
}
