package net.bandit.reskillable.common.gating;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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

    public record MissingReq(Skill skill, int required, ResourceLocation advId) {
        static MissingReq total(int required) { return new MissingReq(null, required, null); }
        static MissingReq skill(Skill skill, int required) { return new MissingReq(skill, required, null); }
        static MissingReq adv(ResourceLocation id) { return new MissingReq(null, 0, id); }

        String key() {
            if (advId != null) return "ADV:" + advId;
            return skill == null ? "TOTAL" : skill.name();
        }

        Component toComponent() {
            if (advId != null) {
                // Show a clean-ish requirement. You can later swap this to the real advancement title in the UI layer.
                return Component.translatable("message.reskillable.req_adv", prettyAdvName(advId));
            }
            if (skill == null) {
                return Component.translatable("message.reskillable.req_total", required);
            }
            return Component.translatable(
                    "message.reskillable.req_skill",
                    Component.translatable("skill." + skill.name().toLowerCase(Locale.ROOT)),
                    required
            );
        }

        private static Component prettyAdvName(ResourceLocation id) {
            // Fallback formatting: use last path segment, Title Case-ish
            String path = id.getPath();
            String last = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            String pretty = last.replace('_', ' ');
            if (!pretty.isEmpty()) {
                pretty = pretty.substring(0, 1).toUpperCase(Locale.ROOT) + pretty.substring(1);
            }
            // Include namespace so it's not ambiguous in modpacks
            return Component.literal(id.getNamespace() + ":" + pretty);
        }
    }

    /**
     * Back-compat (no ADV checks possible without a ServerPlayer).
     * Keeps old call sites compiling; ADV tokens will be ignored here.
     */
    public static GateResult check(SkillModel model, Skill levelingSkill, int currentLevel) {
        return check(null, model, levelingSkill, currentLevel);
    }

    /**
     * Full check including ADV= requirements (when player is provided).
     */
    public static GateResult check(ServerPlayer player, SkillModel model, Skill levelingSkill, int currentLevel) {
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

            // ADV requirements (only enforce if we actually have a server player)
            if (player != null && rule.requiredAdvancements != null && !rule.requiredAdvancements.isEmpty()) {
                for (ResourceLocation advId : rule.requiredAdvancements) {
                    if (!AdvancementGateUtil.has(player, advId)) {
                        missing.add(MissingReq.adv(advId));
                    }
                }
            }
        }

        if (missing.isEmpty()) return GateResult.allow();

        // dedupe: keep highest requirement per key.
        // For ADV entries, we just keep one per advancement id.
        Map<String, MissingReq> best = new LinkedHashMap<>();
        for (MissingReq req : missing) {
            String key = req.key();
            MissingReq existing = best.get(key);

            if (existing == null) {
                best.put(key, req);
                continue;
            }

            // For numeric requirements, keep the highest.
            if (req.advId == null && existing.advId == null && req.required > existing.required) {
                best.put(key, req);
            }
        }

        return GateResult.block(new ArrayList<>(best.values()));
    }

    private record Rule(
            Skill skill,
            int minCurrentLevel,
            Integer minTotalLevels,
            Map<Skill, Integer> minSkillLevels,
            List<ResourceLocation> requiredAdvancements
    ) {
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
            List<ResourceLocation> advReqs = new ArrayList<>();

            String reqs = parts[2].trim();
            if (!reqs.isEmpty()) {
                for (String raw : reqs.split(",")) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String keyRaw = kv[0].trim();
                    String valRaw = kv[1].trim();

                    // ADV token(s)
                    if (keyRaw.equalsIgnoreCase("ADV")) {
                        ResourceLocation id = ResourceLocation.tryParse(valRaw);
                        if (id != null) advReqs.add(id);
                        continue;
                    }

                    String key = keyRaw.toUpperCase(Locale.ROOT);

                    int val;
                    try {
                        val = Integer.parseInt(valRaw);
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

            return new Rule(skill, Math.max(0, minLevel), totalReq, skillReqs, advReqs);
        }
    }
}
