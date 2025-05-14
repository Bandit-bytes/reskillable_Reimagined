package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.client.ClientInitializer;
import net.bandit.reskillable.client.Keybind;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.client.Tooltip;
import net.bandit.reskillable.common.CuriosCompat;
import net.bandit.reskillable.common.capabilities.SkillCapability;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.*;
import net.bandit.reskillable.event.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.registration.NetworkChannel;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(Reskillable.MOD_ID)
public class Reskillable {
    public static final String MOD_ID = "reskillable";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final NetworkChannel NETWORK = NetworkChannel.create(new ResourceLocation(MOD_ID, "main_channel"));


    public Reskillable(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::initCaps);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Configuration.CONFIG_SPEC);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Configuration.load();
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            NETWORK.registerMessage(SyncToClient.ID, SyncToClient.class, SyncToClient::new);
            NETWORK.registerMessage(RequestLevelUp.ID, RequestLevelUp.class, RequestLevelUp::new);
            NETWORK.registerMessage(NotifyWarning.ID, NotifyWarning.class, NotifyWarning::new);
            NETWORK.registerMessage(SyncSkillConfigPacket.ID, SyncSkillConfigPacket.class, SyncSkillConfigPacket::new);
            NETWORK.registerMessage(TogglePerkPacket.ID, TogglePerkPacket.class, TogglePerkPacket::new);
        });

        if (ModList.get().isLoaded("curios")) {
            NeoForge.EVENT_BUS.register(new CuriosCompat());
        }
    }
    private void initCaps(RegisterCapabilitiesEvent event) {
        event.register(SkillCapability.INSTANCE, Player.class, (player, ctx) -> new SkillModel());
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
            NeoForge.EVENT_BUS.register(new Keybind());
            NeoForge.EVENT_BUS.register(ClientEvents.class);
        }
    }
}
