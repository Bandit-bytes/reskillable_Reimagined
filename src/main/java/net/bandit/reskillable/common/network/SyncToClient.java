package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class SyncToClient
{
    private final CompoundTag skillModel;

    public SyncToClient(CompoundTag skillModel)
    {
        this.skillModel = skillModel;
    }

    public SyncToClient(FriendlyByteBuf buffer)
    {
        skillModel = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeNbt(skillModel);
    }

    public static void handle(SyncToClient msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncToClient msg) {
        Player clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer != null) {
            SkillModel clientSkillModel = SkillModel.get(clientPlayer);
            if (clientSkillModel != null) {
                clientSkillModel.deserializeNBT(msg.skillModel);
            }
        }
    }

    // Send Packet
    public static void send(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Reskillable.NETWORK.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncToClient(SkillModel.get(player).serializeNBT()));
        }
    }
}