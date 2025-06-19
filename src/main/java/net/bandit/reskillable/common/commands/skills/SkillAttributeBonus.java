package net.bandit.reskillable.common.commands.skills;

import net.bandit.reskillable.Configuration;
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
    BUILDING(Skill.BUILDING, () -> null, () -> Configuration.BLOCK_REACH_BONUS.get(), AttributeModifier.Operation.ADD_VALUE),
    DEFENSE(Skill.DEFENSE, () -> Attributes.ARMOR.value(), () -> Configuration.ARMOR_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    AGILITY(Skill.AGILITY, () -> null, () -> Configuration.MOVEMENT_SPEED_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
    MAGIC(Skill.MAGIC, Configuration::getConfiguredMagicAttribute, () -> Configuration.LUCK_BONUS.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);


    public final Skill skill;
    private final Supplier<Attribute> attributeSupplier;
    private final Supplier<Double> bonusSupplier;
    public final AttributeModifier.Operation operation;

    SkillAttributeBonus(Skill skill, Supplier<Attribute> attributeSupplier, Supplier<Double> bonusSupplier, AttributeModifier.Operation operation) {
        this.skill = skill;
        this.attributeSupplier = attributeSupplier;
        this.bonusSupplier = bonusSupplier;
        this.operation = operation;
    }

    public double getBonusPerStep() {
        return bonusSupplier.get();
    }

    public Attribute getAttribute() {
        return attributeSupplier.get();
    }

    public static @Nullable SkillAttributeBonus getBySkill(Skill skill) {
        for (SkillAttributeBonus bonus : values()) {
            if (bonus.skill == skill) return bonus;
        }
        return null;

    }
}


