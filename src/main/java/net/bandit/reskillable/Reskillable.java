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
import org.slf4j.Logger;

public class Reskillable implements ModInitializer {
    public static final String MOD_ID = "reskillable";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        Configuration.load();

        SkillAttachments.init();
        SoundRegistry.register();
        AttributeRegistry.register();
        NetworkInit.register();
        EventHandler.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                Commands.register(dispatcher));

        LOGGER.info("Reskillable Fabric 1.21.1 initialized");
    }
}
