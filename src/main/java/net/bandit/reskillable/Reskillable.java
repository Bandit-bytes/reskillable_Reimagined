package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.client.Keybind;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.client.Tooltip;
import net.bandit.reskillable.common.CuriosCompat;
import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.common.commands.Commands;
import net.bandit.reskillable.common.network.NetworkInit;
import net.bandit.reskillable.event.ClientEvents;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.client.Minecraft;
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
        modEventBus.addListener(NetworkInit::registerPayloadHandlers);
        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        NeoForge.EVENT_BUS.register(new EventHandler());
        NeoForge.EVENT_BUS.register(new Commands());
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Configuration.CONFIG_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Configuration.load();
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (ModList.get().isLoaded("curios")) {
            NeoForge.EVENT_BUS.register(new CuriosCompat());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            NeoForge.EVENT_BUS.register(new Tooltip());
            NeoForge.EVENT_BUS.register(new Overlay());
//            NeoForge.EVENT_BUS.register(new Keybind());
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }
    }
}
