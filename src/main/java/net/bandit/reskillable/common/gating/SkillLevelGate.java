package net.bandit.reskillable.common.gating;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.chat.Component;

import java.util.*;

public final class SkillLevelGate {
    private SkillLevelGate() {}

    public record GateResult(boolean allowed, List<MissingReq> missing) {
        public static GateResult allow() { return new GateResult(true, List.of()); }
        public static GateResult block(List<MissingReq> missing) { return new GateResult(false, missing); }

        public Component missingListComponent() {
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            var out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) out = out.append(Component.literal(", "));
                out = out.append(missing.get(i).toComponent());
            }
            return out;
        }
    }

    public record MissingReq(Skill skill, int required) {
        static MissingReq total(int required) { return new MissingReq(null, required); }
        static MissingReq skill(Skill skill, int required) { return new MissingReq(skill, required); }

        String key() { return skill == null ? "TOTAL" : skill.name(); }

        Component toComponent() {
            if (skill == null) {
                return Component.translatable("message.reskillable.req_total", required);
            }
            return Component.translatable(
                    "message.reskillable.req_skill",
                    Component.translatable("skill." + skill.name().toLowerCase(Locale.ROOT)),
                    required
            );
        }
    }

    public static GateResult check(SkillModel model, Skill levelingSkill, int currentLevel) {
        List<? extends String> rules;
        try {
            rules = Configuration.SKILL_LEVEL_GATES.get();
        } catch (Throwable t) {
            return GateResult.allow();
        }
        if (rules == null || rules.isEmpty()) return GateResult.allow();

        int totalLevels = 0;
        for (Skill s : Skill.values()) totalLevels += model.getSkillLevel(s);

        List<MissingReq> missing = new ArrayList<>();

        for (String line : rules) {
            Rule rule = Rule.parse(line);
            if (rule == null) continue;

            if (rule.skill != levelingSkill) continue;
            if (currentLevel < rule.minCurrentLevel) continue;

            if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                missing.add(MissingReq.total(rule.minTotalLevels));
            }

            for (var e : rule.minSkillLevels.entrySet()) {
                int actual = model.getSkillLevel(e.getKey());
                if (actual < e.getValue()) {
                    missing.add(MissingReq.skill(e.getKey(), e.getValue()));
                }
            }
        }

        if (missing.isEmpty()) return GateResult.allow();

        // dedupe: keep highest requirement per key
        Map<String, MissingReq> best = new LinkedHashMap<>();
        for (MissingReq req : missing) {
            String key = req.key();
            MissingReq existing = best.get(key);
            if (existing == null || req.required > existing.required) best.put(key, req);
        }

        return GateResult.block(new ArrayList<>(best.values()));
    }

    private record Rule(Skill skill, int minCurrentLevel, Integer minTotalLevels, Map<Skill, Integer> minSkillLevels) {
        static Rule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            Skill skill;
            try {
                skill = Skill.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }

            int minLevel;
            try {
                minLevel = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }

            Integer totalReq = null;
            Map<Skill, Integer> skillReqs = new LinkedHashMap<>();

            String reqs = parts[2].trim();
            if (!reqs.isEmpty()) {
                for (String raw : reqs.split(",")) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = kv[0].trim().toUpperCase(Locale.ROOT);

                    int val;
                    try {
                        val = Integer.parseInt(kv[1].trim());
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    if (key.equals("TOTAL")) {
                        totalReq = val;
                        continue;
                    }

                    try {
                        Skill reqSkill = Skill.valueOf(key);
                        skillReqs.put(reqSkill, val);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return new Rule(skill, Math.max(0, minLevel), totalReq, skillReqs);
        }
    }
}
