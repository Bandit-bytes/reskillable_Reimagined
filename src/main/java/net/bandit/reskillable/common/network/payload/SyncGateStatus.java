package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.gating.SkillLevelGate;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record SyncGateStatus(String skillId, boolean blocked, Component missing) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("reskillable", "sync_gate_status");
    public static final Type<SyncGateStatus> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGateStatus> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SyncGateStatus::skillId,
                    ByteBufCodecs.BOOL, SyncGateStatus::blocked,
                    ComponentSerialization.TRUSTED_STREAM_CODEC, SyncGateStatus::missing,
                    SyncGateStatus::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Backward-compatible built-in helper.
     */
    public static void send(ServerPlayer player, Skill skill) {
        if (skill == null || !Configuration.isBuiltInSkillEnabled(skill)) return;
        send(player, Configuration.getBuiltInSkillId(skill));
    }

    /**
     * Sends the authoritative gate state for either a built-in or custom skill.
     */
    public static void send(ServerPlayer player, String requestedSkillId) {
        if (player == null || requestedSkillId == null || requestedSkillId.isBlank()) return;

        String skillId = Configuration.canonicalSkillId(requestedSkillId);
        if (skillId.isBlank() || !Configuration.isKnownSkill(skillId)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int level = model.getSkillLevel(skillId);
        SkillLevelGate.GateResult gate = SkillLevelGate.check(player, model, skillId, level);
        boolean blocked = !gate.allowed();
        Component missing = blocked ? gate.missingListComponent(player) : Component.empty();

        PacketDistributor.sendToPlayer(player, new SyncGateStatus(skillId, blocked, missing));
    }

    public static void sendAll(ServerPlayer player) {
        for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
            send(player, Configuration.getBuiltInSkillId(skill));
        }

        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled()) continue;
            String skillId = slot.getId();
            if (skillId == null || skillId.isBlank()) continue;
            send(player, skillId);
        }
    }

    public static void handleClient(SyncGateStatus msg) {
        if (msg == null || msg.skillId == null || msg.skillId.isBlank()) return;
        GateClientCache.set(msg.skillId, msg.blocked, msg.missing);
    }
}
