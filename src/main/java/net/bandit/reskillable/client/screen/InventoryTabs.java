package net.bandit.reskillable.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.TabButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InventoryTabs {

    public static final int GUI_W = 176;
    public static final int GUI_H = 166;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("reskillable/reskillable_tabs.json");

    private static Pos position = getDefault();
    private static boolean loaded = false;

    public static Pos getDefault() {
        return new Pos(-28, 7);
    }

    public static Pos getPosition() {
        ensureLoaded();
        return position;
    }

    public static void setPosition(int relX, int relY) {
        ensureLoaded();
        position = new Pos(relX, relY);
        save();
    }

    public static void resetToDefaults() {
        position = getDefault();
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        position = getDefault();

        if (!Files.exists(SAVE_PATH)) {
            save();
            return;
        }

        try {
            String json = Files.readString(SAVE_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                save();
                return;
            }

            if (root.has("x") && root.has("y")) {
                int x = root.get("x").getAsInt();
                int y = root.get("y").getAsInt();
                position = new Pos(x, y);
                return;
            }

            if (root.has("SKILLS")) {
                JsonObject obj = root.getAsJsonObject("SKILLS");
                if (obj != null) {
                    int x = obj.has("x") ? obj.get("x").getAsInt() : position.x;
                    int y = obj.has("y") ? obj.get("y").getAsInt() : position.y;
                    position = new Pos(x, y);
                    save();
                    return;
                }
            }

            save();
        } catch (Exception ignored) {
            save();
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("x", position.x);
            root.addProperty("y", position.y);

            Files.createDirectories(SAVE_PATH.getParent());
            Files.writeString(SAVE_PATH, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }

    public static int getGuiLeft(Screen screen) {
        if (screen instanceof InventoryScreen inventoryScreen) {
            return inventoryScreen.getRecipeBookComponent().updateScreenPosition(screen.width, GUI_W);
        }
        return (screen.width - GUI_W) / 2;
    }

    public static int getGuiTop(Screen screen) {
        return (screen.height - GUI_H) / 2;
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!Configuration.shouldShowTabButtons()) return;
            boolean isInventory = (screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen);
            boolean isSkills = (screen instanceof SkillScreen);
            if (!isInventory && !isSkills) return;
            ensureLoaded();
            Pos pos = getPosition();
            Screens.getButtons(screen).add(new TabButton(pos.x, pos.y));
        });
    }

    public record Pos(int x, int y) {}
}
