package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.common.commands.Commands;
import net.bandit.reskillable.common.network.NetworkInit;
import net.bandit.reskillable.event.SkillAttachments;
import net.bandit.reskillable.event.SoundRegistry;
import net.bandit.reskillable.registry.AttributeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;

public class Reskillable implements ModInitializer {
    public static final String MOD_ID = "reskillable";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        // Load config/JSON before gameplay callbacks begin using them.
        Configuration.load();

        SkillAttachments.init();
        SoundRegistry.register();
        AttributeRegistry.register();
        NetworkInit.register();
        EventHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                Commands.register(dispatcher));

        // Reload once every mod has registered its content, so attributes added by
        // mods that initialize after this one resolve in perkAttributes (same as
        // NeoForge's ServerStartingEvent reload).
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Configuration.load());

        LOGGER.info("Reskillable Fabric 1.21.1 initialized");
    }
}
