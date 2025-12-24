package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.gating.GateClientCache;
import net.bandit.reskillable.common.gating.SkillLevelGate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record SyncGateStatus(int skillIndex, boolean blocked, Component missing) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "sync_gate_status");
    public static final Type<SyncGateStatus> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGateStatus> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,  SyncGateStatus::skillIndex,
                    ByteBufCodecs.BOOL, SyncGateStatus::blocked,
                    ComponentSerialization.TRUSTED_STREAM_CODEC, SyncGateStatus::missing,
                    SyncGateStatus::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, Skill skill) {
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int level = model.getSkillLevel(skill);

        SkillLevelGate.GateResult gate = SkillLevelGate.check(player, model, skill, level);
        boolean blocked = !gate.allowed();
        Component missing = blocked ? gate.missingListComponent() : Component.empty();

        PacketDistributor.sendToPlayer(player, new SyncGateStatus(skill.index, blocked, missing));
    }

    public static void sendAll(ServerPlayer player) {
        for (Skill s : Skill.values()) send(player, s);
    }
    // CLIENT: apply cache
    public static void handleClient(SyncGateStatus msg) {
        if (msg.skillIndex < 0 || msg.skillIndex >= Skill.values().length) return;
        Skill skill = Skill.values()[msg.skillIndex];
        GateClientCache.set(skill, msg.blocked, msg.missing);
    }

}
