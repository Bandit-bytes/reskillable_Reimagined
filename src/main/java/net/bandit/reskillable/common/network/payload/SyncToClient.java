package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;


public record SyncToClient(Map<Skill, Integer> levels, Map<Skill, Boolean> disabledPerks) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "sync_to_client");
    public static final Type<SyncToClient> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, Skill> SKILL_CODEC =
            StreamCodec.of(
                    (buf, skill) -> buf.writeEnum(skill),
                    buf -> buf.readEnum(Skill.class)
            );

    public static final StreamCodec<FriendlyByteBuf, SyncToClient> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(size -> new EnumMap<>(Skill.class), SKILL_CODEC, ByteBufCodecs.INT),
                    SyncToClient::levels,
                    ByteBufCodecs.map(size -> new EnumMap<>(Skill.class), SKILL_CODEC, ByteBufCodecs.BOOL),
                    SyncToClient::disabledPerks,
                    SyncToClient::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        SkillModel model = SkillModel.get(player);

        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Boolean> disabled = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values()) {
            levels.put(skill, model.getSkillLevel(skill));
            disabled.put(skill, !model.isPerkEnabled(skill));
        }

        PacketDistributor.sendToPlayer(player, new SyncToClient(levels, disabled));
    }
}
