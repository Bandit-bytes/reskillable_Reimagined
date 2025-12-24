package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

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

            GateResult gate = SkillGateRules.check(player, model, skill, currentLevel);
            if (!gate.allowed) {
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
            Reskillable.NETWORK.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new SyncGatePreviewPacket(buildPreview(player, model))
            );
        });

        ctx.setPacketHandled(true);
    }
    private static Map<Skill, GatePreview> buildPreview(ServerPlayer player, SkillModel model) {
        Map<Skill, GatePreview> preview = new EnumMap<>(Skill.class);

        for (Skill s : Skill.values()) {
            int lvl = model.getSkillLevel(s);
            GateResult r = SkillGateRules.check(player, model, s, lvl);

            boolean blocked = !r.allowed;
            Component missing = blocked ? r.missingListComponent() : Component.empty();

            preview.put(s, new GatePreview(blocked, missing));
        }

        return preview;
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

        static GateResult check(ServerPlayer player, SkillModel model, Skill levelingSkill, int currentLevel) {
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

                // Only enforce this rule once you're at/above the specified current level.
                // Example: minCurrentLevel=10 means "to go from 10 -> 11 (and beyond), you must meet reqs"
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

                for (ResourceLocation advId : rule.requiredAdvancements) {
                    if (!hasAdvancement(player, advId)) {
                        missing.add(MissingReq.advancement(advId));
                    }
                }
            }

            if (missing.isEmpty()) return GateResult.allowed();
            return GateResult.blocked(dedupe(missing));
        }

        private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
            var adv = player.server.getAdvancements().getAdvancement(id);
            if (adv == null) return false;

            return player.getAdvancements().getOrStartProgress(adv).isDone();
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

                // For TOTAL and SKILL, keep the highest required level.
                // For ADV, duplicates aren't useful anyway (same key).
                if (existing == null) {
                    best.put(key, req);
                } else if (req.requiredLevel > existing.requiredLevel) {
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
        final List<ResourceLocation> requiredAdvancements;

        private GateRule(
                Skill skill,
                int minCurrentLevel,
                Integer minTotalLevels,
                Map<Skill, Integer> minSkillLevels,
                List<ResourceLocation> requiredAdvancements
        ) {
            this.skill = skill;
            this.minCurrentLevel = minCurrentLevel;
            this.minTotalLevels = minTotalLevels;
            this.minSkillLevels = minSkillLevels;
            this.requiredAdvancements = requiredAdvancements;
        }

        static GateRule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            // SKILL:MINLEVEL:REQS
            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            Skill targetSkill;
            try {
                targetSkill = Skill.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
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
                String[] tokens = reqs.split(",");
                for (String raw : tokens) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = kv[0].trim().toUpperCase(Locale.ROOT);
                    String valStr = kv[1].trim();
                    if (valStr.isEmpty()) continue;

                    // ADV is not an int; parse it first
                    if (key.equals("ADV") || key.equals("ADVANCEMENT")) {
                        try {
                            advReqs.add(new ResourceLocation(valStr));
                        } catch (Exception ignored) {
                        }
                        continue;
                    }

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
                        // unknown token -> ignore
                    }
                }
            }

            return new GateRule(targetSkill, Math.max(0, minLevel), totalReq, skillReqs, advReqs);
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
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            MutableComponent out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) out.append(Component.literal(", "));
                out.append(missing.get(i).toComponent());
            }
            return out;
        }
    }

    private static final class MissingReq {
        final Skill skill;                 // non-null for SKILL requirements
        final int requiredLevel;           // used for TOTAL and SKILL
        final ResourceLocation advancementId; // non-null for ADV requirements

        private MissingReq(Skill skill, int requiredLevel, ResourceLocation advancementId) {
            this.skill = skill;
            this.requiredLevel = requiredLevel;
            this.advancementId = advancementId;
        }

        static MissingReq total(int required) {
            return new MissingReq(null, required, null);
        }

        static MissingReq skill(Skill skill, int required) {
            return new MissingReq(skill, required, null);
        }

        static MissingReq advancement(ResourceLocation id) {
            return new MissingReq(null, 0, id);
        }

        String key() {
            if (advancementId != null) return "ADV:" + advancementId;
            return (skill == null) ? "TOTAL" : skill.name();
        }

        Component toComponent() {
            if (advancementId != null) {
                // Best effort: show the id (you can later enhance this to show the advancement title)
                return Component.translatable("message.reskillable.req_advancement",
                        Component.literal(advancementId.toString()));
            }

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
    private static final Map<Skill, GatePreview> CLIENT_GATE_PREVIEW = new EnumMap<>(Skill.class);

    public static GatePreview getClientGatePreview(Skill skill) {
        return CLIENT_GATE_PREVIEW.get(skill);
    }

    public static void clearClientGatePreview() {
        CLIENT_GATE_PREVIEW.clear();
    }

    // simple holder
    public static final class GatePreview {
        public final boolean blocked;
        public final Component missing;

        public GatePreview(boolean blocked, Component missing) {
            this.blocked = blocked;
            this.missing = missing;
        }
    }
    public static class RequestGatePreviewPacket {
        public RequestGatePreviewPacket() {}
        public RequestGatePreviewPacket(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;

                SkillModel model = SkillModel.get(player);
                if (model == null) return;

                Map<Skill, GatePreview> preview = new EnumMap<>(Skill.class);

                for (Skill s : Skill.values()) {
                    int lvl = model.getSkillLevel(s);
                    GateResult r = SkillGateRules.check(player, model, s, lvl);

                    boolean blocked = !r.allowed;
                    Component missing = blocked ? r.missingListComponent() : Component.empty();

                    preview.put(s, new GatePreview(blocked, missing));
                }

                Reskillable.NETWORK.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SyncGatePreviewPacket(preview)
                );

            });
            ctx.setPacketHandled(true);
        }
    }

    public static class SyncGatePreviewPacket {
        private final Map<Skill, GatePreview> preview;

        public SyncGatePreviewPacket(Map<Skill, GatePreview> preview) {
            this.preview = preview;
        }

        public SyncGatePreviewPacket(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            this.preview = new EnumMap<>(Skill.class);

            for (int i = 0; i < size; i++) {
                Skill s = Skill.values()[buf.readVarInt()];
                boolean blocked = buf.readBoolean();
                Component missing = buf.readComponent();
                this.preview.put(s, new GatePreview(blocked, missing));
            }
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(preview.size());

            for (var e : preview.entrySet()) {
                Skill s = e.getKey();
                GatePreview p = e.getValue();

                buf.writeVarInt(s.index);
                buf.writeBoolean(p.blocked);
                buf.writeComponent(p.missing == null ? Component.empty() : p.missing);
            }
        }

        public void handle(Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                CLIENT_GATE_PREVIEW.clear();
                CLIENT_GATE_PREVIEW.putAll(preview);
            });
            ctx.setPacketHandled(true);
        }
    }
}
