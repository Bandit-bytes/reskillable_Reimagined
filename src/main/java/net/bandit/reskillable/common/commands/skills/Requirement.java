package net.bandit.reskillable.common.commands.skills;

import java.util.Objects;

public class Requirement {
    public Skill skill;
    public int level;

    public Requirement() {
        this.skill = null;
        this.level = 0;
    }

    public Requirement(Skill skill, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }
        this.skill = skill;
        this.level = level;
    }

    @Override
    public String toString() {
        return "Requirement{" +
                "skill=" + skill +
                ", level=" + level +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Requirement that = (Requirement) obj;
        return level == that.level && skill == that.skill;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skill, level);
    }
}
