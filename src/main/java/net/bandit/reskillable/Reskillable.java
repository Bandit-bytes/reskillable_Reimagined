package net.bandit.reskillable;

import com.mojang.logging.LogUtils;
import net.bandit.reskillable.common.EventHandler;
import net.bandit.reskillable.common.network.NetworkInit;
import net.bandit.reskillable.event.RegenAttributeHandler;
import net.bandit.reskillable.event.SkillAttachments;
import net.bandit.reskillable.event.SoundRegistry;
import net.bandit.reskillable.registry.AttributeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Reskillable.MOD_ID)
public class Reskillable {
    public static final String MOD_ID = "reskillable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Reskillable(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkInit::register);
        modEventBus.addListener(AttributeRegistry::modifyEntityAttributes);

        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        AttributeRegistry.ATTRIBUTES.register(modEventBus);
        SkillAttachments.init(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Configuration.CONFIG_SPEC);

        NeoForge.EVENT_BUS.register(new EventHandler());
        NeoForge.EVENT_BUS.register(new RegenAttributeHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
