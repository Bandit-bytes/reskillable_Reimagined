package net.bandit.reskillable.event;

import net.bandit.reskillable.registry.AttributeRegistry;
import net.minecraft.world.food.FoodData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class RegenAttributeHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        if (!player.isAlive() || !player.isHurt()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION)) {
            return;
        }

        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18) {
            return;
        }

        double regen = player.getAttributeValue(AttributeRegistry.HEALTH_REGENERATION);
        if (regen <= 0.0D) {
            return;
        }
        int interval = Math.max(1, (int) Math.round(80.0D / (1.0D + regen)));

        if (player.tickCount % interval == 0) {
            player.heal(1.0F);
        }
    }
}