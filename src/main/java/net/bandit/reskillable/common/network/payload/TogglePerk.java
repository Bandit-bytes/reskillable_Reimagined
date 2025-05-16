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

public record TogglePerk(Skill skill) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "toggle_perk");
    public static final Type<TogglePerk> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, TogglePerk> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of(
                    (FriendlyByteBuf buf, Skill value) -> buf.writeEnum(value),
                    (FriendlyByteBuf buf) -> buf.readEnum(Skill.class)
            ),
            TogglePerk::skill,
            TogglePerk::new
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TogglePerk msg, ServerPlayer player) {
        SkillModel model = SkillModel.get(player);
        if (model != null) {
            model.togglePerk(msg.skill(), player);
        }
    }
    public static void send(Skill skill) {
        PacketDistributor.sendToServer(new TogglePerk(skill));
    }

}
