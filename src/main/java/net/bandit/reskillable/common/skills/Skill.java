package net.bandit.reskillable.common.skills;

import java.util.Locale;

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

    /**
     * Stable internal/save-data identifier for this semantic built-in skill.
     * Public IDs exposed to pack authors may be changed in built_in_skills.json.
     */
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Skill fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Skill.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean isBuiltInSkill(String value) {
        return fromString(value) != null;
    }
}
