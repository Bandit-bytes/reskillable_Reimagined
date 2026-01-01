package net.bandit.reskillable.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.TabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import net.neoforged.fml.loading.FMLPaths;

@EventBusSubscriber(modid = "reskillable", value = Dist.CLIENT)
public class InventoryTabs {

    public static final int GUI_W = 176;
    public static final int GUI_H = 166;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH =
            FMLPaths.CONFIGDIR.get().resolve("reskillable/reskillable_tabs.json");

    private static final Map<TabButton.TabType, Pos> POSITIONS = new EnumMap<>(TabButton.TabType.class);
    private static boolean loaded = false;

    public static Pos getDefault(TabButton.TabType type) {
        return switch (type) {
            case INVENTORY -> new Pos(-28, 7);
            case SKILLS -> new Pos(-28, 36);
        };
    }

    public static Pos getPosition(TabButton.TabType type) {
        ensureLoaded();
        return POSITIONS.getOrDefault(type, getDefault(type));
    }

    public static void setPosition(TabButton.TabType type, int relX, int relY) {
        ensureLoaded();
        POSITIONS.put(type, new Pos(relX, relY));
        save();
    }

    public static void resetToDefaults() {
        ensureLoaded();
        POSITIONS.clear();
        for (var type : TabButton.TabType.values()) {
            POSITIONS.put(type, getDefault(type));
        }
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        for (var type : TabButton.TabType.values()) {
            POSITIONS.put(type, getDefault(type));
        }

        if (!Files.exists(SAVE_PATH)) return;

        try {
            String json = Files.readString(SAVE_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

            for (var type : TabButton.TabType.values()) {
                if (!root.has(type.name())) continue;
                JsonObject obj = root.getAsJsonObject(type.name());
                if (obj == null) continue;

                int x = obj.has("x") ? obj.get("x").getAsInt() : getDefault(type).x;
                int y = obj.has("y") ? obj.get("y").getAsInt() : getDefault(type).y;

                POSITIONS.put(type, new Pos(x, y));
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            for (var entry : POSITIONS.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", entry.getValue().x);
                obj.addProperty("y", entry.getValue().y);
                root.add(entry.getKey().name(), obj);
            }
            Files.createDirectories(SAVE_PATH.getParent());
            Files.writeString(SAVE_PATH, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }
    public static int getGuiLeft(Screen screen) {
        return (screen.width - GUI_W) / 2;
    }

    public static int getGuiTop(Screen screen) {
        return (screen.height - GUI_H) / 2;
    }

    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (!Configuration.shouldShowTabButtons()) return;

        Screen screen = event.getScreen();

        boolean isInventory = (screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen);
        boolean isSkills = (screen instanceof SkillScreen);

        if (!isInventory && !isSkills) return;

        boolean isSkillsOpen = isSkills;
        ensureLoaded();

        Pos invPos = getPosition(TabButton.TabType.INVENTORY);
        Pos sklPos = getPosition(TabButton.TabType.SKILLS);

        event.addListener(new TabButton(invPos.x, invPos.y, TabButton.TabType.INVENTORY, !isSkillsOpen));
        event.addListener(new TabButton(sklPos.x, sklPos.y, TabButton.TabType.SKILLS, isSkillsOpen));
    }

    public record Pos(int x, int y) {}
}
