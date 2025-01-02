package net.bandit.reskillable.common.commands.skills;

import java.util.Objects;

public class Requirement {
    public final Skill skill;
    public final int level;

    /**
     * Constructs a new Requirement with the specified skill and level.
     *
     * @param skill The skill required.
     * @param level The level required for the skill (must be non-negative).
     * @throws IllegalArgumentException if the level is negative.
     */
    public Requirement(Skill skill, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }
        this.skill = skill;
        this.level = level;
    }

    /**
     * Returns a string representation of the Requirement.
     * Useful for debugging purposes.
     *
     * @return A string representing the Requirement.
     */
    @Override
    public String toString() {
        return "Requirement{" +
                "skill=" + skill +
                ", level=" + level +
                '}';
    }

    /**
     * Compares this Requirement to another object for equality.
     *
     * @param obj The object to compare.
     * @return True if the object is a Requirement with the same skill and level, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Requirement that = (Requirement) obj;
        return level == that.level && skill == that.skill;
    }

    /**
     * Computes the hash code for this Requirement.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(skill, level);
    }
}
