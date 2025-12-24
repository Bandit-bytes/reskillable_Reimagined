package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;
import java.util.function.Supplier;

public class RequestLevelUp {
    private final int skillIndex;

    public RequestLevelUp(Skill skill) {
        this.skillIndex = skill.index;
    }

    public RequestLevelUp(FriendlyByteBuf buffer) {
        this.skillIndex = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(skillIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (!Configuration.isSkillLevelingEnabled()) {
                player.sendSystemMessage(Component.translatable("message.reskillable.leveling_disabled"));
                return;
            }

            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            Skill skill = getSkillSafe(skillIndex);
            if (skill == null) return;

            int currentLevel = model.getSkillLevel(skill);
            int max = Configuration.getMaxLevel();

            if (currentLevel >= max) {
                player.sendSystemMessage(Component.translatable("message.reskillable.max_level", max));
                return;
            }

            GateResult gate = SkillGateRules.check(model, skill, currentLevel);
            if (!gate.allowed) {
                // Example message:
                // "You can’t level up Attack yet. Missing: Total Skill Levels (30), Mining (5)"
                player.sendSystemMessage(Component.translatable(
                        "message.reskillable.gate_blocked",
                        Component.translatable("skill.reskillable." + skill.name().toLowerCase(Locale.ROOT)),
                        gate.missingListComponent()
                ));
                return;
            }

            int cost = Configuration.calculateCostForLevel(currentLevel);

            if (player.isCreative()) {
                model.increaseSkillLevel(skill, player);
                SyncToClient.send(player);
                return;
            }

            int totalXp = calculateTotalXp(player);
            if (totalXp < cost) {
                player.sendSystemMessage(Component.translatable("message.reskillable.not_enough_xp", cost, totalXp));
                return;
            }

            deductXp(player, cost);

            model.increaseSkillLevel(skill, player);
            int newLevel = model.getSkillLevel(skill);

            // SFX
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundRegistry.LEVEL_UP_EVENT.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );

            if (newLevel % 5 == 0) {
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        SoundRegistry.MILESTONE_EVENT.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.2F
                );
            }

            SyncToClient.send(player);
        });

        ctx.setPacketHandled(true);
    }

    private static Skill getSkillSafe(int idx) {
        Skill[] values = Skill.values();
        if (idx < 0 || idx >= values.length) return null;
        return values[idx];
    }

    private static int calculateTotalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        int base = getCumulativeXpForLevel(level);
        int next = getCumulativeXpForLevel(level + 1);

        return base + Math.round((next - base) * progress);
    }

    private static void deductXp(ServerPlayer player, int cost) {
        int totalXp = calculateTotalXp(player);
        int newTotalXp = Math.max(0, totalXp - cost);

        player.experienceLevel = getLevelForTotalXp(newTotalXp);
        player.experienceProgress = getProgressForLevel(newTotalXp, player.experienceLevel);
        player.totalExperience = newTotalXp;
    }

    private static int getLevelForTotalXp(int totalXp) {
        int level = 0;
        while (getCumulativeXpForLevel(level + 1) <= totalXp) {
            level++;
        }
        return level;
    }

    private static float getProgressForLevel(int totalXp, int level) {
        int levelXp = getCumulativeXpForLevel(level);
        int nextLevelXp = getCumulativeXpForLevel(level + 1);
        int denom = Math.max(1, nextLevelXp - levelXp);
        return (totalXp - levelXp) / (float) denom;
    }

    public static void send(Skill skill) {
        Reskillable.NETWORK.sendToServer(new RequestLevelUp(skill));
    }

    public static int getCumulativeXpForLevel(int level) {
        if (level <= 0) return 0;

        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    private static final class SkillGateRules {

        static GateResult check(SkillModel model, Skill levelingSkill, int currentLevel) {
            List<? extends String> rules;
            try {
                rules = Configuration.SKILL_LEVEL_GATES.get();
            } catch (Throwable t) {
                return GateResult.allowed();
            }

            if (rules == null || rules.isEmpty()) return GateResult.allowed();

            int totalLevels = getTotalSkillLevels(model);
            List<MissingReq> missing = new ArrayList<>();

            for (String line : rules) {
                GateRule rule = GateRule.parse(line);
                if (rule == null) continue;

                if (rule.skill != levelingSkill) continue;
                if (currentLevel < rule.minCurrentLevel) continue;
                if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                    missing.add(MissingReq.total(rule.minTotalLevels));
                }

                for (Map.Entry<Skill, Integer> e : rule.minSkillLevels.entrySet()) {
                    Skill reqSkill = e.getKey();
                    int reqLevel = e.getValue();
                    int actual = model.getSkillLevel(reqSkill);
                    if (actual < reqLevel) {
                        missing.add(MissingReq.skill(reqSkill, reqLevel));
                    }
                }
            }

            if (missing.isEmpty()) return GateResult.allowed();
            missing = dedupe(missing);

            return GateResult.blocked(missing);
        }

        private static int getTotalSkillLevels(SkillModel model) {
            int total = 0;
            for (Skill s : Skill.values()) {
                total += model.getSkillLevel(s);
            }
            return total;
        }

        private static List<MissingReq> dedupe(List<MissingReq> in) {
            Map<String, MissingReq> best = new LinkedHashMap<>();
            for (MissingReq req : in) {
                String key = req.key();
                MissingReq existing = best.get(key);
                if (existing == null || req.requiredLevel > existing.requiredLevel) {
                    best.put(key, req);
                }
            }
            return new ArrayList<>(best.values());
        }
    }

    private static final class GateRule {
        final Skill skill;
        final int minCurrentLevel;
        final Integer minTotalLevels;
        final Map<Skill, Integer> minSkillLevels;

        private GateRule(Skill skill, int minCurrentLevel, Integer minTotalLevels, Map<Skill, Integer> minSkillLevels) {
            this.skill = skill;
            this.minCurrentLevel = minCurrentLevel;
            this.minTotalLevels = minTotalLevels;
            this.minSkillLevels = minSkillLevels;
        }

        static GateRule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            // SKILL:MINLEVEL:REQS
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
                String[] tokens = reqs.split(",");
                for (String raw : tokens) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = kv[0].trim().toUpperCase(Locale.ROOT);
                    String valStr = kv[1].trim();

                    int val;
                    try {
                        val = Integer.parseInt(valStr);
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
                    } catch (IllegalArgumentException ignored) {
                        // unknown skill token -> ignore
                    }
                }
            }

            return new GateRule(skill, Math.max(0, minLevel), totalReq, skillReqs);
        }
    }

    private static final class GateResult {
        final boolean allowed;
        final List<MissingReq> missing;

        private GateResult(boolean allowed, List<MissingReq> missing) {
            this.allowed = allowed;
            this.missing = missing;
        }

        static GateResult allowed() {
            return new GateResult(true, List.of());
        }

        static GateResult blocked(List<MissingReq> missing) {
            return new GateResult(false, missing);
        }

        MutableComponent missingListComponent() {
            // Build: "Total Skill Levels (30), Mining (5), Defense (5)"
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            MutableComponent out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                MissingReq req = missing.get(i);
                if (i > 0) out = out.append(Component.literal(", "));

                out = out.append(req.toComponent());
            }
            return out;
        }
    }

    private static final class MissingReq {
        final Skill skill; // null if TOTAL
        final int requiredLevel;

        private MissingReq(Skill skill, int requiredLevel) {
            this.skill = skill;
            this.requiredLevel = requiredLevel;
        }

        static MissingReq total(int required) {
            return new MissingReq(null, required);
        }

        static MissingReq skill(Skill skill, int required) {
            return new MissingReq(skill, required);
        }

        String key() {
            return (skill == null) ? "TOTAL" : skill.name();
        }

        Component toComponent() {
            if (skill == null) {
                return Component.translatable("message.reskillable.req_total", requiredLevel);
            }
            return Component.translatable(
                    "message.reskillable.req_skill",
                    Component.translatable("skill.reskillable." + skill.name().toLowerCase(Locale.ROOT)),
                    requiredLevel
            );
        }
    }
}
