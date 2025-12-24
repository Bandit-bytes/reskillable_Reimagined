package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public record RequestGateStatus() implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("reskillable", "request_gate_status");
    public static final Type<RequestGateStatus> TYPE = new Type<>(ID);

    // Empty payload codec (no fields)
    public static final StreamCodec<FriendlyByteBuf, RequestGateStatus> STREAM_CODEC =
            StreamCodec.unit(new RequestGateStatus());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // CLIENT: ask server to compute gate status for all skills
    public static void send() {
        PacketDistributor.sendToServer(new RequestGateStatus());
    }

    // SERVER: reply with SyncGateStatus for each skill
    public static void handle(RequestGateStatus msg, ServerPlayer player) {
        for (Skill s : Skill.values()) {
            SyncGateStatus.send(player, s);
        }
    }
}
