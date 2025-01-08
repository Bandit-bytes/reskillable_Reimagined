package net.bandit.reskillable.common.network;

import com.google.common.reflect.TypeToken;
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

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
        Gson gson = new Gson();
        try {
            // Use the correct type for deserialization
            Type mapType = new TypeToken<Map<String, Requirement[]>>() {}.getType();

            this.skillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
            this.craftSkillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
            this.attackSkillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize SyncSkillConfigPacket", e);
            throw new IllegalArgumentException("Malformed packet data", e);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        Gson gson = new Gson();
        try {
            // Serialize maps into JSON strings
            buf.writeByteArray(compress(gson.toJson(skillLocks)));
            buf.writeByteArray(compress(gson.toJson(craftSkillLocks)));
            buf.writeByteArray(compress(gson.toJson(attackSkillLocks)));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize SyncSkillConfigPacket", e);
            throw new RuntimeException("Failed to compress packet data", e);
        }
    }


    public static void handle(SyncSkillConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Configuration.setSkillLocks(msg.skillLocks);
                Configuration.setCraftSkillLocks(msg.craftSkillLocks);
                Configuration.setAttackSkillLocks(msg.attackSkillLocks);
                refreshClientUI();
            } else {
                LOGGER.warning("Received SyncSkillConfigPacket on server. Ignoring.");
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

            if (skillLocks == null || craftSkillLocks == null || attackSkillLocks == null) {
                System.err.println("Skill locks or craft/attack locks are null, not sending packet.");
                return;
            }

            SyncSkillConfigPacket packet = new SyncSkillConfigPacket(skillLocks, craftSkillLocks, attackSkillLocks);
            Reskillable.NETWORK.send(PacketDistributor.ALL.noArg(), packet);
            LOGGER.info("Sent SyncSkillConfigPacket to all clients.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send SyncSkillConfigPacket", e);
        }
    }


    // Compression helper methods
    private static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes());
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static String decompress(byte[] compressed) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             InputStreamReader reader = new InputStreamReader(gzipInputStream);
             BufferedReader in = new BufferedReader(reader)) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                out.append(line);
            }
            return out.toString();
        }
    }
}
