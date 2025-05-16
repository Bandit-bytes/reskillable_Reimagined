package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillCapability;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Reskillable.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                SkillCapability.INSTANCE,
                EntityType.PLAYER,
                new ICapabilityProvider<Player, Void, SkillModel>() {
                    private final SkillModel model = new SkillModel();

                    @Override
                    public @Nullable SkillModel getCapability(Player player, @Nullable Void context) {
                        return model;
                    }
                }
        );
    }
}
