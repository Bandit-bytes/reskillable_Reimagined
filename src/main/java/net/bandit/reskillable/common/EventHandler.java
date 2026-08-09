package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.SyncGateStatus;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import net.bandit.reskillable.registry.AttributeRegistry;
import net.minecraft.world.level.block.state.BlockState;

public final class EventHandler {
    private EventHandler() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.isCreative()) return InteractionResult.PASS;
            SkillModel model = SkillModel.get(player);
            ItemStack stack = player.getItemInHand(hand);
            BlockState state = level.getBlockState(pos);
            return model.canUseItem(player, stack) && model.canUseBlock(player, state.getBlock())
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isCreative()) return InteractionResult.PASS;
            SkillModel model = SkillModel.get(player);
            ItemStack stack = player.getItemInHand(hand);
            BlockState state = level.getBlockState(hit.getBlockPos());
            return model.canUseItem(player, stack) && model.canUseBlock(player, state.getBlock())
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isCreative() || SkillModel.get(player).canUseItem(player, stack)) {
                return InteractionResultHolder.pass(stack);
            }
            return InteractionResultHolder.fail(stack);
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player.isCreative()) return InteractionResult.PASS;
            SkillModel model = SkillModel.get(player);
            ItemStack stack = player.getItemInHand(hand);
            return model.canUseEntity(player, entity) && model.canUseItem(player, stack)
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player.isCreative()) return InteractionResult.PASS;
            SkillModel model = SkillModel.get(player);
            if (!model.canUseItem(player, player.getMainHandItem())
                    || !model.canUseItem(player, player.getOffhandItem())
                    || !model.canAttackEntity(player, entity)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player.isCreative()) return true;
            SkillModel model = SkillModel.get(player);
            boolean allowed = model.canUseItem(player, player.getMainHandItem()) && model.canUseBlock(player, state.getBlock());
            if (!allowed && !level.isClientSide()) {
                player.sendSystemMessage(Component.literal("You lack the skill to use this tool.").withStyle(ChatFormatting.RED));
            }
            return allowed;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncPlayer(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncPlayer(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> syncPlayer(player));
        ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
            if (Configuration.getDeathReset()) {
                SkillModel.get(player).resetSkills();
            }
            return true;
        });

        // Fabric has no direct LivingEquipmentChangeEvent. Validate armor slots on the server tick;
        // this produces the same player-facing result without loader-specific hooks.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isCreative()) validateArmor(player);
                tickHealthRegeneration(player);
            }
        });
    }

    private static void tickHealthRegeneration(ServerPlayer player) {
        if (!player.isAlive() || !player.isHurt()) return;
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) return;

        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18) return;

        double regen = player.getAttributeValue(AttributeRegistry.HEALTH_REGENERATION);
        if (regen <= 0.0D) return;

        int interval = Math.max(1, (int) Math.round(80.0D / (1.0D + regen)));
        if (player.tickCount % interval == 0) player.heal(1.0F);
    }

    private static void syncPlayer(ServerPlayer player) {
        SkillModel model = SkillModel.get(player);
        model.updateSkillAttributeBonuses(player);
        SyncToClient.send(player);
        SyncGateStatus.sendAll(player);
        SyncSkillConfig.send(player);
    }

    private static void validateArmor(ServerPlayer player) {
        SkillModel model = SkillModel.get(player);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (equipped.isEmpty() || model.canUseItem(player, equipped)) continue;

            ItemStack rejected = equipped.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(rejected)) player.drop(rejected, false);
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.literal("You lack the skill to equip this armor.").withStyle(ChatFormatting.RED));
        }
    }

    public static float applyMiningSpeedBonus(Player player, float original) {
        if (Configuration.hasBuiltInPerkOverride(Skill.MINING)) return original;
        SkillModel model = SkillModel.get(player);
        if (!model.isPerkEnabled(Skill.MINING)) return original;
        SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(Skill.MINING);
        if (bonus == null) return original;
        int steps = model.getSkillLevel(Skill.MINING) / Math.max(1, bonus.getPerkStep());
        return steps <= 0 ? original : original * (1.0F + (float) (steps * bonus.getBonusPerStep()));
    }

    public static boolean shouldForceCropGrowth(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (Configuration.hasBuiltInPerkOverride(Skill.FARMING)) return false;
        SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(Skill.FARMING);
        if (bonus == null) return false;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) >= 64.0D) continue;
            SkillModel model = SkillModel.get(player);
            if (!model.isPerkEnabled(Skill.FARMING)) continue;
            int steps = model.getSkillLevel(Skill.FARMING) / Math.max(1, bonus.getPerkStep());
            if (steps > 0 && level.random.nextFloat() < Math.min(1.0F, (float) (steps * bonus.getBonusPerStep()))) return true;
        }
        return false;
    }

    public static int gatheringBonusXp(Player player, int originalXp) {
        if (Configuration.hasBuiltInPerkOverride(Skill.GATHERING)) return 0;
        SkillModel model = SkillModel.get(player);
        if (!model.isPerkEnabled(Skill.GATHERING)) return 0;
        SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(Skill.GATHERING);
        if (bonus == null) return 0;
        int steps = model.getSkillLevel(Skill.GATHERING) / Math.max(1, bonus.getPerkStep());
        return steps <= 0 ? 0 : Math.round(originalXp * (float) (steps * bonus.getBonusPerStep()));
    }

    public static boolean canUseTotem(Player player, ItemStack totem) {
        if (!totem.is(Items.TOTEM_OF_UNDYING)) return true;
        Requirement[] requirements = Configuration.getRequirements(totem.getItem().builtInRegistryHolder().key().location());
        if (requirements == null || requirements.length == 0) return true;
        SkillModel model = SkillModel.get(player);
        for (Requirement req : requirements) {
            if (req != null && model.getSkillLevel(req.skill) < req.level) {
                player.sendSystemMessage(Component.literal("You lack the skill to use the Totem of Undying.").withStyle(ChatFormatting.RED));
                return false;
            }
        }
        return true;
    }
}
