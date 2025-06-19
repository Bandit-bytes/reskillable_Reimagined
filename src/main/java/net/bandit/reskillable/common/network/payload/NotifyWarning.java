package net.bandit.reskillable.common.network.payload;

import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public record NotifyWarning(ResourceLocation resource, RequirementType requirementType) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "notify_warning");
    public static final Type<NotifyWarning> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RequirementType> REQUIREMENT_TYPE_CODEC =
            StreamCodec.of(
                    (buf, value) -> buf.writeEnum(value),
                    (buf) -> buf.readEnum(RequirementType.class)
            );

    public static final StreamCodec<FriendlyByteBuf, NotifyWarning> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    NotifyWarning::resource,
                    REQUIREMENT_TYPE_CODEC,
                    NotifyWarning::requirementType,
                    NotifyWarning::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

