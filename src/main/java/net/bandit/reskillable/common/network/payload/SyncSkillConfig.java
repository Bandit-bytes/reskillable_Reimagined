package net.bandit.reskillable.common.network.payload;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record SyncSkillConfig(Map<String, Requirement[]> skillLocks,
                              Map<String, Requirement[]> craftSkillLocks,
                              Map<String, Requirement[]> attackSkillLocks,
                              boolean isFinalChunk) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("reskillable", "sync_config");
    public static final Type<SyncSkillConfig> TYPE = new Type<>(ID);
    private static final Logger LOGGER = Logger.getLogger(SyncSkillConfig.class.getName());
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type REQ_MAP_TYPE = new TypeToken<Map<String, Requirement[]>>() {}.getType();

    public static final StreamCodec<FriendlyByteBuf, SyncSkillConfig> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                    try {
                        buf.writeByteArray(compress(GSON.toJson(msg.skillLocks)));
                        buf.writeByteArray(compress(GSON.toJson(msg.craftSkillLocks)));
                        buf.writeByteArray(compress(GSON.toJson(msg.attackSkillLocks)));
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
                        boolean isFinal = buf.readBoolean();
                        return new SyncSkillConfig(skillLocks, craftLocks, attackLocks, isFinal);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to decompress SyncSkillConfig", e);
                    }
                }
        );

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
