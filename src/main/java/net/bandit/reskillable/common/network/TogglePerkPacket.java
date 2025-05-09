package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TogglePerkPacket {
    private final int skillIndex;

    public TogglePerkPacket(Skill skill) {
        this.skillIndex = skill.index;
    }

    public TogglePerkPacket(FriendlyByteBuf buf) {
        this.skillIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(skillIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            Skill skill = Skill.values()[skillIndex];
            model.togglePerk(skill, player); // Server toggles the perk too
        });
        ctx.get().setPacketHandled(true);
    }
}
