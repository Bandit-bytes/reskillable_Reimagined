package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.client.Tooltip;
import net.bandit.reskillable.common.*;
import net.bandit.reskillable.common.commands.Commands;
import net.bandit.reskillable.common.network.NetworkInit;
import net.bandit.reskillable.event.ClientEvents;
import net.bandit.reskillable.event.SkillAttachments;
import net.bandit.reskillable.event.SoundRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Reskillable.MOD_ID)
public class Reskillable {
    public static final String MOD_ID = "reskillable";
    private static final Logger LOGGER = LogUtils.getLogger();


    public Reskillable(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        SkillAttachments.init(modEventBus);
        modEventBus.addListener(NetworkInit::register);
        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(new EventHandler());
        NeoForge.EVENT_BUS.register(new Commands());
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Configuration.CONFIG_SPEC);
        // TACZ
        if (ModList.get().isLoaded("tacz")) {
            NeoForge.EVENT_BUS.register(new TaczEventHandler());
        }
        // irons_spellbooks
        if (ModList.get().isLoaded("irons_spellbooks")) {
            NeoForge.EVENT_BUS.register(new IronsSpellbooksEventHandler());
        }
        // Spell Engine compat
        if (ModList.get().isLoaded("spell_engine")) {
            NeoForge.EVENT_BUS.register(new SpellEngineEventHandler());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Configuration.load();

        if (ModList.get().isLoaded("curios")) {
            NeoForge.EVENT_BUS.register(new CuriosCompat());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from BanditBytes - Reskillable");
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Reskillable Client Setup Successful");
            NeoForge.EVENT_BUS.register(new Tooltip());
            NeoForge.EVENT_BUS.register(new Overlay());
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }
    }
}
