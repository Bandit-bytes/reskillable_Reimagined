package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            BuiltInRegistries.SOUND_EVENT, Reskillable.MOD_ID);

    public static final Identifier LEVEL_UP_SOUND = Identifier.fromNamespaceAndPath(Reskillable.MOD_ID, "level_up");
    public static final Identifier MILESTONE_SOUND = Identifier.fromNamespaceAndPath(Reskillable.MOD_ID, "milestone_up");

    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP_EVENT = SOUND_EVENTS.register("level_up",
            () -> SoundEvent.createVariableRangeEvent(LEVEL_UP_SOUND));
    public static final DeferredHolder<SoundEvent, SoundEvent> MILESTONE_EVENT = SOUND_EVENTS.register("milestone_up",
            () -> SoundEvent.createVariableRangeEvent(MILESTONE_SOUND));
}
