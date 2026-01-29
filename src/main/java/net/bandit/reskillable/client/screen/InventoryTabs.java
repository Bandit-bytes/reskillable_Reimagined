package net.bandit.reskillable.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.TabButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EventBusSubscriber(modid = "reskillable", value = Dist.CLIENT)
public class InventoryTabs {

    public static final int GUI_W = 176;
    public static final int GUI_H = 166;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH =
            FMLPaths.CONFIGDIR.get().resolve("reskillable/reskillable_tabs.json");

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

        if (!Files.exists(SAVE_PATH)) return;

        try {
            String json = Files.readString(SAVE_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

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
                }
            }
        } catch (Exception ignored) {
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

        ensureLoaded();
        Pos pos = getPosition();

        event.addListener(new TabButton(pos.x, pos.y));
    }

    public record Pos(int x, int y) {}
}
