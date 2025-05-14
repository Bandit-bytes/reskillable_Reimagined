package net.bandit.reskillable.common.commands.skills;

/**
 * Enum representing different skills in the game.
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

    /**
     * Constructs a new Skill with the specified index and display name.
     *
     * @param index The index of the skill.
     * @param name The display name of the skill.
     */
    Skill(int index, String name) {
        this.index = index;
        this.displayName = name;
    }

    /**
     * Gets the index of the skill's icon.
     *
     * @return The index of the skill's icon.
     */
    public int getIconIndex() {
        return this.index;
    }

    /**
     * Gets the display name of the skill.
     *
     * @return The display name of the skill.
     */
    public String getDisplayName() {
        return this.displayName;
    }
}
