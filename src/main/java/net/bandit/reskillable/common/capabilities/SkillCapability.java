package net.bandit.reskillable.common.capabilities;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Class for registering and handling the SkillModel capability.
 */
public class SkillCapability {
    // Capability instance for SkillModel
    public static final Capability<SkillModel> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
}
