package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class ToggleCustomPerkPacket {
    private final String customSkillId;

    public ToggleCustomPerkPacket(String customSkillId) {
        this.customSkillId = customSkillId == null ? "" : customSkillId.toLowerCase(Locale.ROOT);
    }

    public ToggleCustomPerkPacket(FriendlyByteBuf buf) {
        this.customSkillId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(customSkillId);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            model.toggleCustomPerk(customSkillId, player);
        });
        ctx.setPacketHandled(true);
    }
}
