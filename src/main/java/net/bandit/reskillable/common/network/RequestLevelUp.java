package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Configuration.CustomSkillSlot;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class RequestLevelUp {
    private final boolean customSkill;
    private final int skillIndex;
    private final String customSkillId;

    public RequestLevelUp(Skill skill) {
        this.customSkill = false;
        this.skillIndex = skill.index;
        this.customSkillId = "";
    }

    public RequestLevelUp(String customSkillId) {
        this.customSkill = true;
        this.skillIndex = -1;
        this.customSkillId = customSkillId == null ? "" : customSkillId.trim().toLowerCase(Locale.ROOT);
    }

    public RequestLevelUp(FriendlyByteBuf buffer) {
        this.customSkill = buffer.readBoolean();
        this.skillIndex = buffer.readInt();
        this.customSkillId = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(customSkill);
        buffer.writeInt(skillIndex);
        buffer.writeUtf(customSkillId == null ? "" : customSkillId);
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

            if (customSkill) {
                handleCustomSkillLevelUp(player, model);
            } else {
                handleBuiltInSkillLevelUp(player, model);
            }
        });

        ctx.setPacketHandled(true);
    }

    private void handleBuiltInSkillLevelUp(ServerPlayer player, SkillModel model) {
        Skill skill = getSkillSafe(skillIndex);
        if (skill == null) return;

        int currentLevel = model.getSkillLevel(skill);
        int max = Configuration.getMaxLevel();

        if (currentLevel >= max) {
            player.sendSystemMessage(Component.translatable("message.reskillable.max_level", max));
            return;
        }

        GateResult gate = SkillGateRules.check(player, model, skill, null, currentLevel);
        if (!gate.allowed) {
            player.sendSystemMessage(Component.translatable(
                    "message.reskillable.gate_blocked",
                    Component.translatable("skill.reskillable." + skill.name().toLowerCase(Locale.ROOT)),
                    gate.missingListComponent(player)
            ));
            return;
        }

        int cost = Configuration.calculateCostForLevel(currentLevel);

        if (player.isCreative()) {
            model.increaseSkillLevel(skill, player);
            SyncToClient.send(player);
            sendGatePreview(player, model);
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

        playLevelSounds(player, newLevel);

        SyncToClient.send(player);
        sendGatePreview(player, model);
    }

    private void handleCustomSkillLevelUp(ServerPlayer player, SkillModel model) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return;
        }

        CustomSkillSlot slot = Configuration.findCustomSkillById(customSkillId);
        if (slot == null || !slot.isEnabled()) {
            return;
        }

        int currentLevel = model.getCustomSkillLevel(slot.getId());
        int max = Configuration.getMaxLevel();

        if (currentLevel >= max) {
            player.sendSystemMessage(Component.translatable("message.reskillable.max_level", max));
            return;
        }

        GateResult gate = SkillGateRules.check(player, model, null, slot.getId(), currentLevel);
        if (!gate.allowed) {
            player.sendSystemMessage(Component.translatable(
                    "message.reskillable.gate_blocked",
                    Component.literal(slot.getDisplayName()),
                    gate.missingListComponent(player)
            ));
            return;
        }

        int cost = Configuration.calculateCostForLevel(currentLevel);

        if (player.isCreative()) {
            model.increaseCustomSkillLevel(slot.getId(), player);
            SyncToClient.send(player);
            sendGatePreview(player, model);
            return;
        }

        int totalXp = calculateTotalXp(player);
        if (totalXp < cost) {
            player.sendSystemMessage(Component.translatable("message.reskillable.not_enough_xp", cost, totalXp));
            return;
        }

        deductXp(player, cost);

        model.increaseCustomSkillLevel(slot.getId(), player);
        int newLevel = model.getCustomSkillLevel(slot.getId());

        playLevelSounds(player, newLevel);

        SyncToClient.send(player);
        sendGatePreview(player, model);
    }

    private static void playLevelSounds(ServerPlayer player, int newLevel) {
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
    }

    private static void sendGatePreview(ServerPlayer player, SkillModel model) {
        Reskillable.NETWORK.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncGatePreviewPacket(buildPreview(player, model), buildCustomPreview(player, model))
        );
    }

    private static Map<Skill, GatePreview> buildPreview(ServerPlayer player, SkillModel model) {
        Map<Skill, GatePreview> preview = new EnumMap<>(Skill.class);

        for (Skill s : Skill.values()) {
            int lvl = model.getSkillLevel(s);
            GateResult r = SkillGateRules.check(player, model, s, null, lvl);

            boolean blocked = !r.allowed;
            Component missing = blocked ? r.missingListComponent(player) : Component.empty();

            preview.put(s, new GatePreview(blocked, missing));
        }

        return preview;
    }

    private static Map<String, GatePreview> buildCustomPreview(ServerPlayer player, SkillModel model) {
        Map<String, GatePreview> preview = new LinkedHashMap<>();

        for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled()) continue;

            int lvl = model.getCustomSkillLevel(slot.getId());
            GateResult r = SkillGateRules.check(player, model, null, slot.getId(), lvl);

            boolean blocked = !r.allowed;
            Component missing = blocked ? r.missingListComponent(player) : Component.empty();

            preview.put(slot.getId(), new GatePreview(blocked, missing));
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

    public static void sendCustom(String customSkillId) {
        Reskillable.NETWORK.sendToServer(new RequestLevelUp(customSkillId));
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

        static GateResult check(ServerPlayer player, SkillModel model, Skill levelingSkill, String customSkillId, int currentLevel) {
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

                if (!rule.matchesTarget(levelingSkill, customSkillId)) continue;
                if (currentLevel < rule.minCurrentLevel) continue;

                if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                    missing.add(MissingReq.total(rule.minTotalLevels));
                }

                for (Map.Entry<String, Integer> e : rule.minSkillLevels.entrySet()) {
                    String reqSkillId = e.getKey();
                    int reqLevel = e.getValue();

                    int actual;
                    Skill builtIn = Skill.fromString(reqSkillId);
                    if (builtIn != null) {
                        actual = model.getSkillLevel(builtIn);
                    } else {
                        actual = model.getCustomSkillLevel(reqSkillId);
                    }

                    if (actual < reqLevel) {
                        missing.add(MissingReq.skill(reqSkillId, reqLevel));
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
            Advancement adv = player.server.getAdvancements().getAdvancement(id);
            if (adv == null) return false;

            return player.getAdvancements().getOrStartProgress(adv).isDone();
        }

        private static int getTotalSkillLevels(SkillModel model) {
            int total = 0;
            for (Skill s : Skill.values()) {
                total += model.getSkillLevel(s);
            }

            for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
                if (slot != null && slot.isEnabled()) {
                    total += model.getCustomSkillLevel(slot.getId());
                }
            }

            return total;
        }

        private static List<MissingReq> dedupe(List<MissingReq> in) {
            Map<String, MissingReq> best = new LinkedHashMap<>();
            for (MissingReq req : in) {
                String key = req.key();
                MissingReq existing = best.get(key);

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
        final String targetSkillId;
        final int minCurrentLevel;
        final Integer minTotalLevels;
        final Map<String, Integer> minSkillLevels;
        final List<ResourceLocation> requiredAdvancements;

        private GateRule(
                String targetSkillId,
                int minCurrentLevel,
                Integer minTotalLevels,
                Map<String, Integer> minSkillLevels,
                List<ResourceLocation> requiredAdvancements
        ) {
            this.targetSkillId = targetSkillId;
            this.minCurrentLevel = minCurrentLevel;
            this.minTotalLevels = minTotalLevels;
            this.minSkillLevels = minSkillLevels;
            this.requiredAdvancements = requiredAdvancements;
        }

        boolean matchesTarget(Skill builtInSkill, String customSkillId) {
            if (builtInSkill != null) {
                return targetSkillId.equals(builtInSkill.getSerializedName());
            }
            return customSkillId != null && targetSkillId.equals(customSkillId.toLowerCase(Locale.ROOT));
        }

        static GateRule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            String targetSkillId = parts[0].trim().toLowerCase(Locale.ROOT);

            boolean validBuiltIn = Skill.isBuiltInSkill(targetSkillId);
            boolean validCustom = Configuration.findCustomSkillById(targetSkillId) != null;
            if (!validBuiltIn && !validCustom) {
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
                String[] tokens = reqs.split(",");
                for (String raw : tokens) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = kv[0].trim().toLowerCase(Locale.ROOT);
                    String valStr = kv[1].trim();
                    if (valStr.isEmpty()) continue;

                    if (key.equals("adv") || key.equals("advancement")) {
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

                    if (key.equals("total")) {
                        totalReq = val;
                        continue;
                    }

                    if (Skill.isBuiltInSkill(key) || Configuration.findCustomSkillById(key) != null) {
                        skillReqs.put(key, val);
                    }
                }
            }

            return new GateRule(targetSkillId, Math.max(0, minLevel), totalReq, skillReqs, advReqs);
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

        MutableComponent missingListComponent(ServerPlayer player) {
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            MutableComponent out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) out.append(Component.literal(", "));
                out.append(missing.get(i).toComponent(player));
            }
            return out;
        }
    }

    private static final class MissingReq {
        final String skillId;
        final int requiredLevel;
        final ResourceLocation advancementId;

        private MissingReq(String skillId, int requiredLevel, ResourceLocation advancementId) {
            this.skillId = skillId;
            this.requiredLevel = requiredLevel;
            this.advancementId = advancementId;
        }

        static MissingReq total(int required) {
            return new MissingReq(null, required, null);
        }

        static MissingReq skill(String skillId, int required) {
            return new MissingReq(skillId, required, null);
        }

        static MissingReq advancement(ResourceLocation id) {
            return new MissingReq(null, 0, id);
        }

        String key() {
            if (advancementId != null) return "ADV:" + advancementId;
            return (skillId == null) ? "TOTAL" : skillId;
        }

        Component toComponent(ServerPlayer player) {
            if (advancementId != null) {
                return Component.translatable(
                        "message.reskillable.req_advancement",
                        getAdvancementDescription(player, advancementId)
                );
            }

            if (skillId == null) {
                return Component.translatable("message.reskillable.req_total", requiredLevel);
            }

            Skill builtIn = Skill.fromString(skillId);
            if (builtIn != null) {
                return Component.translatable(
                        "message.reskillable.req_skill",
                        Component.translatable("skill.reskillable." + builtIn.name().toLowerCase(Locale.ROOT)),
                        requiredLevel
                );
            }

            CustomSkillSlot slot = Configuration.findCustomSkillById(skillId);
            String display = slot != null ? slot.getDisplayName() : skillId;
            return Component.literal(display + " level " + requiredLevel);
        }
    }

    private static Component getAdvancementDescription(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return prettyAdvancement(id);
        }

        DisplayInfo display = advancement.getDisplay();
        if (display != null) {
            return display.getDescription().copy();
        }

        return prettyAdvancement(id);
    }

    private static Component prettyAdvancement(ResourceLocation id) {
        String path = id.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');

        String pretty = Arrays.stream(name.split(" "))
                .filter(s -> !s.isEmpty())
                .map(w -> w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(name);

        return Component.literal(pretty);
    }

    private static final Map<Skill, GatePreview> CLIENT_GATE_PREVIEW = new EnumMap<>(Skill.class);
    private static final Map<String, GatePreview> CLIENT_CUSTOM_GATE_PREVIEW = new HashMap<>();

    public static GatePreview getClientGatePreview(Skill skill) {
        return CLIENT_GATE_PREVIEW.get(skill);
    }

    public static GatePreview getClientCustomGatePreview(String customSkillId) {
        if (customSkillId == null || customSkillId.isBlank()) {
            return null;
        }
        return CLIENT_CUSTOM_GATE_PREVIEW.get(customSkillId.toLowerCase(Locale.ROOT));
    }

    public static void clearClientGatePreview() {
        CLIENT_GATE_PREVIEW.clear();
        CLIENT_CUSTOM_GATE_PREVIEW.clear();
    }

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

                Map<Skill, GatePreview> preview = buildPreview(player, model);
                Map<String, GatePreview> customPreview = buildCustomPreview(player, model);

                Reskillable.NETWORK.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncGatePreviewPacket(preview, customPreview)
                );

            });
            ctx.setPacketHandled(true);
        }
    }

    public static class SyncGatePreviewPacket {
        private final Map<Skill, GatePreview> preview;
        private final Map<String, GatePreview> customPreview;

        public SyncGatePreviewPacket(Map<Skill, GatePreview> preview, Map<String, GatePreview> customPreview) {
            this.preview = preview;
            this.customPreview = customPreview;
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

            int customSize = buf.readVarInt();
            this.customPreview = new HashMap<>();

            for (int i = 0; i < customSize; i++) {
                String id = buf.readUtf();
                boolean blocked = buf.readBoolean();
                Component missing = buf.readComponent();
                this.customPreview.put(id.toLowerCase(Locale.ROOT), new GatePreview(blocked, missing));
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

            buf.writeVarInt(customPreview.size());

            for (var e : customPreview.entrySet()) {
                buf.writeUtf(e.getKey());
                buf.writeBoolean(e.getValue().blocked);
                buf.writeComponent(e.getValue().missing == null ? Component.empty() : e.getValue().missing);
            }
        }

        public void handle(Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                CLIENT_GATE_PREVIEW.clear();
                CLIENT_GATE_PREVIEW.putAll(preview);

                CLIENT_CUSTOM_GATE_PREVIEW.clear();
                CLIENT_CUSTOM_GATE_PREVIEW.putAll(customPreview);
            });
            ctx.setPacketHandled(true);
        }
    }
}
