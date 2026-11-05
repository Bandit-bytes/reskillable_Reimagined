package net.bandit.reskillable.common.gating;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GateClientCache {
    private GateClientCache() {}

    private static final Map<String, Entry> CACHE = new HashMap<>();

    public record Entry(boolean blocked, Component missingList) {}

    private static String key(String skillId) {
        String normalized = skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return "";
        return Configuration.canonicalSkillId(normalized);
    }

    public static Entry get(String skillId) {
        String key = key(skillId);
        return key.isBlank() ? null : CACHE.get(key);
    }

    public static void set(String skillId, boolean blocked, Component missingList) {
        String key = key(skillId);
        if (key.isBlank()) return;
        CACHE.put(key, new Entry(blocked, missingList == null ? Component.empty() : missingList));
    }

    public static Entry get(Skill skill) {
        if (skill == null) return null;
        return get(Configuration.getBuiltInSkillId(skill));
    }

    public static void set(Skill skill, boolean blocked, Component missingList) {
        if (skill == null) return;
        set(Configuration.getBuiltInSkillId(skill), blocked, missingList);
    }

    public static void clear() {
        CACHE.clear();
    }
}
