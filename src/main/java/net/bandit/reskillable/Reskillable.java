package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.compat.SpellEngineCompat;
import net.bandit.reskillable.common.commands.Commands;
import net.bandit.reskillable.common.network.NetworkInit;
import net.bandit.reskillable.event.SkillAttachments;
import net.bandit.reskillable.event.SoundRegistry;
import net.bandit.reskillable.registry.AttributeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
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
        registerSpellEngine();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                Commands.register(dispatcher));

        // Reload once every mod has registered its content, so attributes added by
        // mods that initialize after this one resolve in perkAttributes (same as
        // NeoForge's ServerStartingEvent reload).
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Configuration.load());

        LOGGER.info("Reskillable Fabric 1.21.1 initialized");
    }

    /** Spell casting checks skill requirements through Spell Engine's casting attempt event, added in 1.10. */
    private static void registerSpellEngine() {
        boolean supported = FabricLoader.getInstance().getModContainer("spell_engine").map(spellEngine -> {
            try {
                return VersionPredicate.parse(">=1.10.0").test(spellEngine.getMetadata().getVersion());
            } catch (VersionParsingException e) {
                return false;
            }
        }).orElse(false);
        if (supported) {
            SpellEngineCompat.register();
        } else if (FabricLoader.getInstance().isModLoaded("spell_engine")) {
            LOGGER.warn("Spell Engine is older than 1.10, so spell casting does not check skill requirements");
        }
    }
}
