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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SyncSkillConfigPacket {
    private static final Logger LOGGER = Logger.getLogger(SyncSkillConfigPacket.class.getName());
    private static final int CHUNK_SIZE = 100; // Number of entries per chunk
    private final Map<String, Requirement[]> skillLocks;
    private final Map<String, Requirement[]> craftSkillLocks;
    private final Map<String, Requirement[]> attackSkillLocks;
    private final boolean isFinalChunk;

    public SyncSkillConfigPacket(Map<String, Requirement[]> skillLocks,
                                 Map<String, Requirement[]> craftSkillLocks,
                                 Map<String, Requirement[]> attackSkillLocks,
                                 boolean isFinalChunk) {
        this.skillLocks = skillLocks;
        this.craftSkillLocks = craftSkillLocks;
        this.attackSkillLocks = attackSkillLocks;
        this.isFinalChunk = isFinalChunk;
    }

    public SyncSkillConfigPacket(FriendlyByteBuf buf) {
        Gson gson = new Gson();
        try {
            Type mapType = new TypeToken<Map<String, Requirement[]>>() {}.getType();

            this.skillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
            this.craftSkillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
            this.attackSkillLocks = gson.fromJson(decompress(buf.readByteArray()), mapType);
            this.isFinalChunk = buf.readBoolean();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize SyncSkillConfigPacket", e);
            throw new IllegalArgumentException("Malformed packet data", e);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        Gson gson = new Gson();
        try {
            buf.writeByteArray(compress(gson.toJson(skillLocks)));
            buf.writeByteArray(compress(gson.toJson(craftSkillLocks)));
            buf.writeByteArray(compress(gson.toJson(attackSkillLocks)));
            buf.writeBoolean(isFinalChunk);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize SyncSkillConfigPacket", e);
            throw new RuntimeException("Failed to compress packet data", e);
        }
    }

    public static void handle(SyncSkillConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CompletableFuture.runAsync(() -> {
                try {
                    if (ctx.get().getDirection().getReceptionSide().isClient()) {
                        Configuration.setSkillLocks(msg.skillLocks);
                        Configuration.setCraftSkillLocks(msg.craftSkillLocks);
                        Configuration.setAttackSkillLocks(msg.attackSkillLocks);

                        if (msg.isFinalChunk) {
                            refreshClientUI();
                            LOGGER.info("All skill configuration chunks received and applied.");
                        }
                    } else {
                        LOGGER.warning("Received SyncSkillConfigPacket on server. Ignoring.");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error updating skill locks", e);
                }
            });
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
                LOGGER.warning("Skill locks or craft/attack locks are null, not sending packet.");
                return;
            }

            List<Map.Entry<String, Requirement[]>> skillLockEntries = new ArrayList<>(skillLocks.entrySet());
            List<Map.Entry<String, Requirement[]>> craftSkillLockEntries = new ArrayList<>(craftSkillLocks.entrySet());
            List<Map.Entry<String, Requirement[]>> attackSkillLockEntries = new ArrayList<>(attackSkillLocks.entrySet());

            int maxSize = Math.max(Math.max(skillLockEntries.size(), craftSkillLockEntries.size()), attackSkillLockEntries.size());

            for (int i = 0; i < maxSize; i += CHUNK_SIZE) {
                Map<String, Requirement[]> skillLockChunk = skillLockEntries.stream()
                        .skip(i)
                        .limit(CHUNK_SIZE)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, Requirement[]> craftSkillLockChunk = craftSkillLockEntries.stream()
                        .skip(i)
                        .limit(CHUNK_SIZE)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Map<String, Requirement[]> attackSkillLockChunk = attackSkillLockEntries.stream()
                        .skip(i)
                        .limit(CHUNK_SIZE)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                boolean isFinalChunk = (i + CHUNK_SIZE) >= maxSize;
                SyncSkillConfigPacket packet = new SyncSkillConfigPacket(skillLockChunk, craftSkillLockChunk, attackSkillLockChunk, isFinalChunk);
                Reskillable.NETWORK.send(PacketDistributor.ALL.noArg(), packet);

                LOGGER.info("Sent chunk " + (i / CHUNK_SIZE + 1) + " to all clients.");
            }
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