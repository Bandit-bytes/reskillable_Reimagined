package net.bandit.reskillable;

import net.bandit.reskillable.client.ClientInitializer;
import net.bandit.reskillable.common.CuriosCompat;
import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.Commands;
import net.bandit.reskillable.common.network.*;
import net.bandit.reskillable.event.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.bandit.reskillable.common.IronsSpellbooksEventHandler;
import net.bandit.reskillable.common.TaczEventHandler;

import java.util.Optional;

@Mod(Reskillable.MOD_ID)
public class Reskillable {
    public static final String MOD_ID = "reskillable";
    public static SimpleChannel NETWORK;

    public Reskillable() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCaps);

        SoundRegistry.SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configuration.getConfig());

        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinecraftForge.EVENT_BUS.register(new Commands());
        // TACZ
        if (ModList.get().isLoaded("tacz")) {
            MinecraftForge.EVENT_BUS.register(new TaczEventHandler());
        }
        // irons_spellbooks
        if (ModList.get().isLoaded("irons_spellbooks")) {
            MinecraftForge.EVENT_BUS.register(new IronsSpellbooksEventHandler());
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInitializer::registerClientEvents);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Configuration.load();

            NETWORK = NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(MOD_ID, "main_channel"),
                    () -> "1.0",
                    s -> true,
                    s -> true
            );

            NETWORK.registerMessage(1, SyncToClient.class,
                    SyncToClient::encode, SyncToClient::new, SyncToClient::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));

            NETWORK.registerMessage(2, RequestLevelUp.class,
                    RequestLevelUp::encode, RequestLevelUp::new, RequestLevelUp::handle,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER));

            NETWORK.registerMessage(3, NotifyWarning.class,
                    NotifyWarning::encode, NotifyWarning::new, NotifyWarning::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));

            NETWORK.registerMessage(4, SyncSkillConfigPacket.class,
                    SyncSkillConfigPacket::toBytes, SyncSkillConfigPacket::new, SyncSkillConfigPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));

            NETWORK.registerMessage(5, TogglePerkPacket.class,
                    TogglePerkPacket::encode, TogglePerkPacket::new, TogglePerkPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER));

            NETWORK.registerMessage(6, RequestLevelUp.RequestGatePreviewPacket.class,
                    RequestLevelUp.RequestGatePreviewPacket::encode,
                    RequestLevelUp.RequestGatePreviewPacket::new,
                    RequestLevelUp.RequestGatePreviewPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER));

            NETWORK.registerMessage(7, RequestLevelUp.SyncGatePreviewPacket.class,
                    RequestLevelUp.SyncGatePreviewPacket::encode,
                    RequestLevelUp.SyncGatePreviewPacket::new,
                    RequestLevelUp.SyncGatePreviewPacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });

        if (ModList.get().isLoaded("curios")) {
            MinecraftForge.EVENT_BUS.register(new CuriosCompat());
        }
    }

    private void initCaps(RegisterCapabilitiesEvent event) {
        event.register(SkillModel.class);
    }
}
