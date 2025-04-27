package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, Reskillable.MOD_ID);

    public static final ResourceLocation LEVEL_UP_SOUND = new ResourceLocation(Reskillable.MOD_ID, "level_up");
    public static final ResourceLocation MILESTONE_SOUND = new ResourceLocation(Reskillable.MOD_ID, "milestone_up");

    public static final RegistryObject<SoundEvent> LEVEL_UP_EVENT = SOUND_EVENTS.register("level_up",
            () -> SoundEvent.createVariableRangeEvent(LEVEL_UP_SOUND));
    public static final RegistryObject<SoundEvent> MILESTONE_EVENT = SOUND_EVENTS.register("milestone_up",
            () -> SoundEvent.createVariableRangeEvent(MILESTONE_SOUND));
}
