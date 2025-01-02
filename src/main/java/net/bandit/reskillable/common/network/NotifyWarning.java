package net.bandit.reskillable.common.network;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.common.commands.skills.RequirementType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class NotifyWarning {
    private final ResourceLocation resource;
    private final RequirementType type;

    public NotifyWarning(ResourceLocation resource, RequirementType type) {
        this.resource = resource;
        this.type = type;
    }

    public NotifyWarning(FriendlyByteBuf buffer) {
        resource = buffer.readResourceLocation();
        type = buffer.readEnum(RequirementType.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(resource);
        buffer.writeEnum(this.type);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player !=null){
                Overlay.showWarning(resource, type);
            }
        });
        context.get().setPacketHandled(true);
    }
    public static void send(Player player, ResourceLocation resource, RequirementType type) {
        Reskillable.NETWORK.send(PacketDistributor.PLAYER.with(() -> {
            return (ServerPlayer)player;
        }), new NotifyWarning(resource, type));
    }
    // Client-Only Handler
    private static class ClientHandler {
        private static void handleWarning(ResourceLocation resource, RequirementType type) {
            Overlay.showWarning(resource, type);
        }
    }
}
