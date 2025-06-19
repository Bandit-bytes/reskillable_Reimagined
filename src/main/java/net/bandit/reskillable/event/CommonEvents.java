package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillCapability;
import net.bandit.reskillable.common.capabilities.SkillModelProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Reskillable.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        event.registerEntity(
                SkillCapability.INSTANCE,
                EntityType.PLAYER,
                new SkillModelProvider()
        );
    }
}
