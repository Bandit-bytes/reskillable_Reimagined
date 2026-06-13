package net.bandit.reskillable.common.network.payload;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.skills.Requirement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record SyncSkillConfig(Map<String, Requirement[]> skillLocks,
                              Map<String, Requirement[]> craftSkillLocks,
                              Map<String, Requirement[]> attackSkillLocks,
                              boolean skillLevelingEnabled,
                              int maximumLevel,
                              int maxTotalSpentLevels,
                              List<Configuration.CustomSkillSlot> customSkills,
                              boolean isFinalChunk) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("reskillable", "sync_config");
    public static final Type<SyncSkillConfig> TYPE = new Type<>(ID);
    private static final Logger LOGGER = Logger.getLogger(SyncSkillConfig.class.getName());
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type REQ_MAP_TYPE = new TypeToken<Map<String, Requirement[]>>() {}.getType();
    private static final java.lang.reflect.Type CUSTOM_SKILL_LIST_TYPE = new TypeToken<List<Configuration.CustomSkillSlot>>() {}.getType();

    public static final StreamCodec<FriendlyByteBuf, SyncSkillConfig> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                try {
                    buf.writeByteArray(compress(GSON.toJson(msg.skillLocks)));
                    buf.writeByteArray(compress(GSON.toJson(msg.craftSkillLocks)));
                    buf.writeByteArray(compress(GSON.toJson(msg.attackSkillLocks)));
                    buf.writeBoolean(msg.skillLevelingEnabled);
                    buf.writeInt(msg.maximumLevel);
                    buf.writeInt(msg.maxTotalSpentLevels);
                    buf.writeByteArray(compress(GSON.toJson(msg.customSkills)));
                    buf.writeBoolean(msg.isFinalChunk);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to compress SyncSkillConfig", e);
                }
            },
            (buf) -> {
                try {
                    Map<String, Requirement[]> skillLocks = GSON.fromJson(decompress(buf.readByteArray()), REQ_MAP_TYPE);
                    Map<String, Requirement[]> craftLocks = GSON.fromJson(decompress(buf.readByteArray()), REQ_MAP_TYPE);
                    Map<String, Requirement[]> attackLocks = GSON.fromJson(decompress(buf.readByteArray()), REQ_MAP_TYPE);
                    boolean skillLevelingEnabled = buf.readBoolean();
                    int maximumLevel = buf.readInt();
                    int maxTotalSpentLevels = buf.readInt();
                    List<Configuration.CustomSkillSlot> customSkills = GSON.fromJson(decompress(buf.readByteArray()), CUSTOM_SKILL_LIST_TYPE);
                    boolean isFinal = buf.readBoolean();
                    return new SyncSkillConfig(
                            skillLocks,
                            craftLocks,
                            attackLocks,
                            skillLevelingEnabled,
                            maximumLevel,
                            maxTotalSpentLevels,
                            customSkills,
                            isFinal
                    );
                } catch (IOException e) {
                    throw new RuntimeException("Failed to decompress SyncSkillConfig", e);
                }
            }
    );

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncSkillConfig(
                Configuration.getSkillLocks(),
                Configuration.getCraftSkillLocks(),
                Configuration.getAttackSkillLocks(),
                Configuration.isSkillLevelingEnabled(),
                Configuration.getMaxLevel(),
                Configuration.getMaxSpendableLevels(),
                Configuration.getCustomSkills(),
                true
        ));
    }

    private static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data.getBytes());
        }
        return out.toByteArray();
    }

    private static String decompress(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(in);
             InputStreamReader reader = new InputStreamReader(gzip);
             BufferedReader buf = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = buf.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
