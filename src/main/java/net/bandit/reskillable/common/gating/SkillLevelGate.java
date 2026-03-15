package net.bandit.reskillable.common.gating;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class SkillLevelGate {
    private SkillLevelGate() {}

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    public record GateResult(boolean allowed, List<MissingReq> missing) {
        public static GateResult allow() {
            return new GateResult(true, List.of());
        }

        public static GateResult block(List<MissingReq> missing) {
            return new GateResult(false, missing);
        }

        public Component missingListComponent(ServerPlayer player) {
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            var out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) {
                    out = out.append(Component.literal(", "));
                }
                out = out.append(missing.get(i).toComponent(player));
            }
            return out;
        }
    }

    public record MissingReq(String skillId, int required, ResourceLocation advId) {
        static MissingReq total(int required) {
            return new MissingReq(null, required, null);
        }

        static MissingReq skill(String skillId, int required) {
            return new MissingReq(normalizeSkillId(skillId), required, null);
        }

        static MissingReq adv(ResourceLocation id) {
            return new MissingReq(null, 0, id);
        }

        String key() {
            if (advId != null) return "ADV:" + advId;
            return skillId == null ? "TOTAL" : normalizeSkillId(skillId);
        }

        Component toComponent(ServerPlayer player) {
            if (advId != null) {
                return Component.translatable(
                        "message.reskillable.req_adv",
                        getAdvancementDescription(player, advId)
                );
            }

            if (skillId == null) {
                return Component.translatable("message.reskillable.req_total", required);
            }

            return Component.translatable(
                    "message.reskillable.req_skill",
                    getSkillDisplayComponent(skillId),
                    required
            );
        }
    }

    private static Component getSkillDisplayComponent(String skillId) {
        String normalized = normalizeSkillId(skillId);

        if (Configuration.isVanillaSkill(normalized)) {
            return Component.translatable("skill." + normalized);
        }

        Configuration.CustomSkillSlot customSkill = Configuration.getCustomSkill(normalized);
        if (customSkill != null && customSkill.displayName != null && !customSkill.displayName.isBlank()) {
            return Component.literal(customSkill.displayName);
        }

        return Component.literal(normalized);
    }

    private static Component getAdvancementDescription(ServerPlayer player, ResourceLocation id) {
        if (player == null || player.server == null) {
            return prettyAdvName(id);
        }

        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder == null) {
            return prettyAdvName(id);
        }

        var display = holder.value().display();
        if (display.isPresent()) {
            return display.get().getDescription().copy();
        }

        return prettyAdvName(id);
    }

    private static Component prettyAdvName(ResourceLocation id) {
        String path = id.getPath();
        String last = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        String pretty = last.replace('_', ' ');
        if (!pretty.isEmpty()) {
            pretty = pretty.substring(0, 1).toUpperCase(Locale.ROOT) + pretty.substring(1);
        }
        return Component.literal(id.getNamespace() + ":" + pretty);
    }

    /**
     * Back-compat (no ADV checks possible without a ServerPlayer).
     */
    public static GateResult check(SkillModel model, String levelingSkillId, int currentLevel) {
        return check(null, model, levelingSkillId, currentLevel);
    }

    /**
     * Full check including advancement requirements.
     */
    public static GateResult check(ServerPlayer player, SkillModel model, String levelingSkillId, int currentLevel) {
        List<? extends String> rules;
        try {
            rules = Configuration.SKILL_LEVEL_GATES.get();
        } catch (Throwable t) {
            return GateResult.allow();
        }

        if (rules == null || rules.isEmpty()) {
            return GateResult.allow();
        }

        String normalizedLevelingSkill = normalizeSkillId(levelingSkillId);
        int totalLevels = model.getAllSkillLevels().values().stream().mapToInt(Integer::intValue).sum();

        List<MissingReq> missing = new ArrayList<>();

        for (String line : rules) {
            Rule rule = Rule.parse(line);
            if (rule == null) continue;

            if (!rule.skillId.equals(normalizedLevelingSkill)) continue;
            if (currentLevel < rule.minCurrentLevel) continue;

            if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                missing.add(MissingReq.total(rule.minTotalLevels));
            }

            for (Map.Entry<String, Integer> entry : rule.minSkillLevels.entrySet()) {
                int actual = model.getSkillLevel(entry.getKey());
                if (actual < entry.getValue()) {
                    missing.add(MissingReq.skill(entry.getKey(), entry.getValue()));
                }
            }

            if (player != null && rule.requiredAdvancements != null && !rule.requiredAdvancements.isEmpty()) {
                for (ResourceLocation advId : rule.requiredAdvancements) {
                    if (!AdvancementGateUtil.has(player, advId)) {
                        missing.add(MissingReq.adv(advId));
                    }
                }
            }
        }

        if (missing.isEmpty()) {
            return GateResult.allow();
        }

        Map<String, MissingReq> best = new LinkedHashMap<>();
        for (MissingReq req : missing) {
            String key = req.key();
            MissingReq existing = best.get(key);

            if (existing == null) {
                best.put(key, req);
                continue;
            }

            if (req.advId == null && existing.advId == null && req.required > existing.required) {
                best.put(key, req);
            }
        }

        return GateResult.block(new ArrayList<>(best.values()));
    }

    private record Rule(
            String skillId,
            int minCurrentLevel,
            Integer minTotalLevels,
            Map<String, Integer> minSkillLevels,
            List<ResourceLocation> requiredAdvancements
    ) {
        static Rule parse(String line) {
            if (line == null) return null;

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            String skillId = normalizeSkillId(parts[0]);
            if (!Configuration.isKnownSkill(skillId)) {
                return null;
            }

            int minLevel;
            try {
                minLevel = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }

            Integer totalReq = null;
            Map<String, Integer> skillReqs = new LinkedHashMap<>();
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

                    if (keyRaw.equalsIgnoreCase("ADV")) {
                        ResourceLocation id = ResourceLocation.tryParse(valRaw);
                        if (id != null) {
                            advReqs.add(id);
                        }
                        continue;
                    }

                    int val;
                    try {
                        val = Integer.parseInt(valRaw);
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    if (keyRaw.equalsIgnoreCase("TOTAL")) {
                        totalReq = val;
                        continue;
                    }

                    String requiredSkillId = normalizeSkillId(keyRaw);
                    if (Configuration.isKnownSkill(requiredSkillId)) {
                        skillReqs.put(requiredSkillId, val);
                    }
                }
            }

            return new Rule(
                    skillId,
                    Math.max(0, minLevel),
                    totalReq,
                    skillReqs,
                    advReqs
            );
        }
    }
}