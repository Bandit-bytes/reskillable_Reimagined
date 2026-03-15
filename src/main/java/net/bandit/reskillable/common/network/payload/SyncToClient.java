package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record SyncToClient(Map<String, Integer> levels, Map<String, Boolean> disabledPerks) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "sync_to_client");
    public static final Type<SyncToClient> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncToClient> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.INT),
                    SyncToClient::levels,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL),
                    SyncToClient::disabledPerks,
                    SyncToClient::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player) {
        SkillModel model = SkillModel.get(player);

        Map<String, Integer> levels = new HashMap<>();
        Map<String, Boolean> disabled = new HashMap<>();

        for (Map.Entry<String, Integer> entry : model.getAllSkillLevels().entrySet()) {
            levels.put(entry.getKey(), entry.getValue());
        }

        Set<String> disabledPerkIds = model.getDisabledPerks();
        for (String skillId : model.getAllSkillLevels().keySet()) {
            disabled.put(skillId, disabledPerkIds.contains(skillId));
        }

        PacketDistributor.sendToPlayer(player, new SyncToClient(levels, disabled));
    }
}