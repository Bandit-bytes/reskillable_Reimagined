package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            BuiltInRegistries.SOUND_EVENT, Reskillable.MOD_ID);

    public static final ResourceLocation LEVEL_UP_SOUND = ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "level_up");
    public static final ResourceLocation MILESTONE_SOUND = ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "milestone_up");

    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP_EVENT = SOUND_EVENTS.register("level_up",
            () -> SoundEvent.createVariableRangeEvent(LEVEL_UP_SOUND));
    public static final DeferredHolder<SoundEvent, SoundEvent> MILESTONE_EVENT = SOUND_EVENTS.register("milestone_up",
            () -> SoundEvent.createVariableRangeEvent(MILESTONE_SOUND));
}
