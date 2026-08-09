package net.bandit.reskillable.common.skills;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;


public enum SkillAttributeBonus {
    ATTACK(Skill.ATTACK, () -> Attributes.ATTACK_DAMAGE.value(), () -> Configuration.ATTACK_DAMAGE_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    GATHERING(Skill.GATHERING, () -> null, () -> Configuration.GATHERING_XP_BONUS.get(), AttributeModifier.Operation.ADD_VALUE),
    MINING(Skill.MINING, () -> null, () -> Configuration.MINING_SPEED_MULTIPLIER.get(), AttributeModifier.Operation.ADD_VALUE),
    FARMING(Skill.FARMING, () -> null, () -> Configuration.CROP_GROWTH_CHANCE.get(), AttributeModifier.Operation.ADD_VALUE),
    BUILDING(Skill.BUILDING, () -> Attributes.BLOCK_INTERACTION_RANGE.value(), () -> Configuration.BLOCK_REACH_BONUS.get(), AttributeModifier.Operation.ADD_VALUE),
    DEFENSE(Skill.DEFENSE, () -> Attributes.ARMOR.value(), () -> Configuration.ARMOR_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    AGILITY(Skill.AGILITY, () -> null, () -> Configuration.MOVEMENT_SPEED_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    MAGIC(Skill.MAGIC, Configuration::getConfiguredMagicAttribute, () -> Configuration.LUCK_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);


    public final Skill skill;
    private final Supplier<Attribute> attributeSupplier;
    private final Supplier<Double> bonusSupplier;
    private final AttributeModifier.Operation defaultOperation;

    SkillAttributeBonus(Skill skill, Supplier<Attribute> attributeSupplier, Supplier<Double> bonusSupplier, AttributeModifier.Operation operation) {
        this.skill = skill;
        this.attributeSupplier = attributeSupplier;
        this.bonusSupplier = bonusSupplier;
        this.defaultOperation = operation;
    }

    public double getBonusPerStep() {
        return Configuration.getBuiltInPerkAmountPerStep(this.skill, bonusSupplier.get());
    }

    public int getPerkStep() {
        return Configuration.getBuiltInPerkStep(this.skill);
    }

    public Attribute getAttribute() {
        // This enum represents the original built-in perk only. Explicit JSON
        // attribute overrides are applied separately by SkillModel so they can
        // fully replace the legacy perk and support more than one attribute.
        Attribute configured = Configuration.getConfiguredAttribute(this.skill);
        return configured != null ? configured : attributeSupplier.get();
    }

    public AttributeModifier.Operation getOperation() {
        AttributeModifier.Operation legacy = Configuration.getConfiguredOperation(this.skill, defaultOperation);
        return Configuration.getBuiltInPerkOperation(this.skill, legacy);
    }

    public static @Nullable SkillAttributeBonus getBySkill(Skill skill) {
        for (SkillAttributeBonus bonus : values()) {
            if (bonus.skill == skill) return bonus;
        }
        return null;

    }
    public double getTotalBonus(SkillModel model) {
        int steps = model.getSkillLevel(this.skill) / getPerkStep();
        return steps * getBonusPerStep();
    }
    public double getTotalBonusPercent(SkillModel model) {
        return getTotalBonus(model) * 100.0;
    }
}


