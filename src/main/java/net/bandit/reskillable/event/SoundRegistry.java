package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundRegistry {
    private SoundRegistry() {}

    public static final ResourceLocation LEVEL_UP_SOUND = ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "level_up");
    public static final ResourceLocation MILESTONE_SOUND = ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "milestone_up");

    public static final SoundEvent LEVEL_UP_EVENT = SoundEvent.createVariableRangeEvent(LEVEL_UP_SOUND);
    public static final SoundEvent MILESTONE_EVENT = SoundEvent.createVariableRangeEvent(MILESTONE_SOUND);

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, LEVEL_UP_SOUND, LEVEL_UP_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, MILESTONE_SOUND, MILESTONE_EVENT);
    }
}
