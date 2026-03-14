package net.bandit.reskillable.common.commands.skills;

import java.util.Locale;
import java.util.Objects;

public class Requirement {
    public Skill skill;
    public String customSkillId;
    public int level;

    public Requirement() {
        this.skill = null;
        this.customSkillId = "";
        this.level = 0;
    }

    public Requirement(Skill skill, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }
        this.skill = skill;
        this.customSkillId = "";
        this.level = level;
    }

    public Requirement(String customSkillId, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level must be non-negative.");
        }

        this.skill = null;
        this.customSkillId = customSkillId == null ? "" : customSkillId.trim().toLowerCase(Locale.ROOT);
        this.level = level;
    }

    public boolean isVanillaSkill() {
        return this.skill != null;
    }

    public boolean isCustomSkill() {
        return this.customSkillId != null && !this.customSkillId.isBlank();
    }

    public String getSkillKey() {
        if (isVanillaSkill()) {
            return this.skill.name().toLowerCase(Locale.ROOT);
        }
        return this.customSkillId == null ? "" : this.customSkillId;
    }

    @Override
    public String toString() {
        return "Requirement{" +
                "skill=" + skill +
                ", customSkillId='" + customSkillId + '\'' +
                ", level=" + level +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Requirement that = (Requirement) obj;
        return level == that.level
                && skill == that.skill
                && Objects.equals(customSkillId, that.customSkillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skill, customSkillId, level);
    }
}
