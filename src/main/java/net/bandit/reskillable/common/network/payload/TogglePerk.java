package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public record TogglePerk(String skillId) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "toggle_perk");
    public static final Type<TogglePerk> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, TogglePerk> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    TogglePerk::skillId,
                    TogglePerk::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TogglePerk msg, ServerPlayer player) {
        SkillModel model = SkillModel.get(player);
        if (model != null) {
            model.togglePerk(normalize(msg.skillId()), player);
        }
    }

    public static void send(String skillId) {
        PacketDistributor.sendToServer(new TogglePerk(normalize(skillId)));
    }

    private static String normalize(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }
}