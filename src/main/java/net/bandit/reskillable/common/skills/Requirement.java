package net.bandit.reskillable.common.skills;

import java.util.Locale;
import java.util.Objects;

public class Requirement {
    public String skill;
    public int level;

    public Requirement() {
        this.skill = "";
        this.level = 0;
    }

    public Requirement(String skill, int level) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Skill ID must not be null or blank.");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }

        this.skill = skill.trim().toLowerCase(Locale.ROOT);
        this.level = level;
    }

    public Requirement(Skill skill, int level) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill must not be null.");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }

        this.skill = skill.name().toLowerCase(Locale.ROOT);
        this.level = level;
    }

    public boolean isVanillaSkill() {
        return getVanillaSkillOrNull() != null;
    }

    public Skill getVanillaSkillOrNull() {
        try {
            return Skill.valueOf(skill.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Requirement{" +
                "skill='" + skill + '\'' +
                ", level=" + level +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Requirement that)) return false;
        return level == that.level && Objects.equals(skill, that.skill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skill, level);
    }
}