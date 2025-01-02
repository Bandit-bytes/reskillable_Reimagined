package net.bandit.reskillable.common.network;

import com.google.gson.Gson;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SyncSkillConfigPacket {
    private static final Logger LOGGER = Logger.getLogger(SyncSkillConfigPacket.class.getName());
    private final Map<String, Requirement[]> skillLocks;
    private final Map<String, Requirement[]> craftSkillLocks;
    private final Map<String, Requirement[]> attackSkillLocks;

    public SyncSkillConfigPacket(Map<String, Requirement[]> skillLocks, Map<String, Requirement[]> craftSkillLocks, Map<String, Requirement[]> attackSkillLocks) {
        this.skillLocks = skillLocks;
        this.craftSkillLocks = craftSkillLocks;
        this.attackSkillLocks = attackSkillLocks;
    }

    public SyncSkillConfigPacket(FriendlyByteBuf buf) {
        Type type = Configuration.getSkillLocksType();
        Gson gson = new Gson();

        try {
            this.skillLocks = gson.fromJson(buf.readUtf(), type);
            this.craftSkillLocks = gson.fromJson(buf.readUtf(), type);
            this.attackSkillLocks = gson.fromJson(buf.readUtf(), type);
        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Failed to deserialize SyncSkillConfigPacket data", e);
            throw new IllegalArgumentException("Malformed packet data", e);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        Gson gson = new Gson();
        buf.writeUtf(gson.toJson(skillLocks));
        buf.writeUtf(gson.toJson(craftSkillLocks));
        buf.writeUtf(gson.toJson(attackSkillLocks));
    }

    public static void handle(SyncSkillConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Configuration.setSkillLocks(msg.skillLocks);
                Configuration.setCraftSkillLocks(msg.craftSkillLocks);
                Configuration.setAttackSkillLocks(msg.attackSkillLocks);

                refreshClientUI();
            }
        });
        ctx.get().setPacketHandled(true);
    }


    private static void refreshClientUI() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("Skill configuration updated!"));
            }
        });
    }


    public static void sendToAllClients() {
        try {
            Map<String, Requirement[]> skillLocks = Configuration.getSkillLocks();
            Map<String, Requirement[]> craftSkillLocks = Configuration.getCraftSkillLocks();
            Map<String, Requirement[]> attackSkillLocks = Configuration.getAttackSkillLocks();
            SyncSkillConfigPacket packet = new SyncSkillConfigPacket(skillLocks, craftSkillLocks, attackSkillLocks);
            Reskillable.NETWORK.send(PacketDistributor.ALL.noArg(), packet);
            LOGGER.info("Sent SyncSkillConfigPacket to all clients.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send SyncSkillConfigPacket", e);
        }
    }
}
