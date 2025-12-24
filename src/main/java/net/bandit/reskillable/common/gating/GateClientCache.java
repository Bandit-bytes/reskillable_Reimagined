package net.bandit.reskillable.common.gating;

import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public final class GateClientCache {
    private GateClientCache() {}

    private static final Map<Skill, Entry> CACHE = new EnumMap<>(Skill.class);

    public record Entry(boolean blocked, Component missingList) {}

    public static Entry get(Skill skill) {
        return CACHE.get(skill);
    }

    public static void set(Skill skill, boolean blocked, Component missingList) {
        CACHE.put(skill, new Entry(blocked, missingList));
    }

    public static void clear() {
        CACHE.clear();
    }
}
