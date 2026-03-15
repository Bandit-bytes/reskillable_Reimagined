package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.RequestLevelUp;
import net.bandit.reskillable.common.network.payload.TogglePerk;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.skills.SkillAttributeBonus;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SkillScreen extends Screen {
    public static final ResourceLocation RESOURCES =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/skills.png");
    public static final ResourceLocation PERKS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/perks.png");
    public static final ResourceLocation PERKS_ADDITIONAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/perks_additional.png");
    private static final ResourceLocation LOCK_ICON =
            ResourceLocation.fromNamespaceAndPath("reskillable", "textures/gui/lock_icon.png");

    private static final int PERK_BOX_X = 12;
    private static final int PERK_BOX_Y = 29;
    private static final int PERK_ROW_HEIGHT = 15;
    private static final int PERK_TEXT_OFFSET_X = 15;
    private static final int SUBPAGE_NAV_Y = -12;
    private static final int SUBPAGE_TITLE_Y = -10;
    private static final int TITLE_COLOR = 0xE0D0A0;


    private final Map<String, String> xpCostDisplay = new HashMap<>();
    private final Map<String, Integer> xpCostColor = new HashMap<>();

    private int page = 0;
    private int skillSubPage = 0;
    private int perkSubPage = 0;

    private Button skillsTab;
    private Button perksTab;
    private Button previousSkillPageButton;
    private Button nextSkillPageButton;

    public SkillScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int left = (width - 162) / 2;
        int top = (height - 128) / 2;

        this.clearWidgets();

        if (page == 0) {
            if (skillSubPage == 0) {
                Skill[] skills = Skill.values();
                for (int i = 0; i < Math.min(8, skills.length); i++) {
                    int x = left + i % 2 * 83;
                    int y = top + i / 2 * 36;
                    addRenderableWidget(new SkillButton(x, y, normalizeSkillId(skills[i].name())));
                }
            } else if (skillSubPage == 1 && Configuration.isSecondSkillPageEnabled()) {
                List<Configuration.CustomSkillSlot> customSkills = Configuration.getCustomSkills();
                for (int i = 0; i < 8; i++) {
                    int x = left + i % 2 * 83;
                    int y = top + i / 2 * 36;

                    Configuration.CustomSkillSlot slot = i < customSkills.size() ? customSkills.get(i) : null;
                    addRenderableWidget(new CustomSkillButton(x, y, slot));
                }
            }
        }

        int guiLeft = (width - 176) / 2;
        int guiTop = (height - 166) / 2;

        int tabWidth = 45;
        int tabHeight = 17;

        skillsTab = new TabButton(
                guiLeft + 11,
                guiTop + 4,
                tabWidth,
                tabHeight,
                b -> {
                    if (page != 0) {
                        page = 0;
                        init(minecraft, width, height);
                    }
                }
        );

        perksTab = new TabButton(
                guiLeft + 176 - 11 - tabWidth,
                guiTop + 4,
                tabWidth,
                tabHeight,
                b -> {
                    if (page != 1) {
                        page = 1;
                        init(minecraft, width, height);
                    }
                }
        );

        addRenderableWidget(skillsTab);
        addRenderableWidget(perksTab);

        if (Configuration.isSecondSkillPageEnabled()) {
            int centerX = guiLeft + 88;
            int navY = guiTop + SUBPAGE_NAV_Y;

            if (page == 0) {
                previousSkillPageButton = addRenderableWidget(new SubPageButton(
                        centerX - 52,
                        navY,
                        "<",
                        b -> {
                            if (skillSubPage > 0) {
                                skillSubPage--;
                                init(minecraft, width, height);
                            }
                        }
                ));

                nextSkillPageButton = addRenderableWidget(new SubPageButton(
                        centerX + 36,
                        navY,
                        ">",
                        b -> {
                            if (skillSubPage < 1) {
                                skillSubPage++;
                                init(minecraft, width, height);
                            }
                        }
                ));

                previousSkillPageButton.active = skillSubPage > 0;
                nextSkillPageButton.active = skillSubPage < 1;
            } else if (page == 1) {
                previousSkillPageButton = addRenderableWidget(new SubPageButton(
                        centerX - 52,
                        navY,
                        "<",
                        b -> {
                            if (perkSubPage > 0) {
                                perkSubPage--;
                                init(minecraft, width, height);
                            }
                        }
                ));

                nextSkillPageButton = addRenderableWidget(new SubPageButton(
                        centerX + 36,
                        navY,
                        ">",
                        b -> {
                            if (perkSubPage < 1) {
                                perkSubPage++;
                                init(minecraft, width, height);
                            }
                        }
                ));

                previousSkillPageButton.active = perkSubPage > 0;
                nextSkillPageButton.active = perkSubPage < 1;
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation background;
        if (page == 0) {
            background = RESOURCES;
        } else {
            background = (perkSubPage == 0) ? PERKS_TEXTURE : PERKS_ADDITIONAL_TEXTURE;
        }

        int left = (width - 176) / 2;
        int top = (height - 166) / 2;

        RenderSystem.setShaderTexture(0, background);

        // Optional dark overlay without blur
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        guiGraphics.blit(background, left, top, 0, 0, 176, 166);

        if (page == 0) {
            if (skillSubPage == 0) {
                int i = 0;
                for (Skill skill : Skill.values()) {
                    if (i >= 8) break;

                    int x = left + (i % 2) * 83 + 10;
                    int y = top + (i / 2) * 36 + 20;

                    String skillId = normalizeSkillId(skill.name());
                    String xpCost = xpCostDisplay.getOrDefault(skillId, "N/A");
                    int color = xpCostColor.getOrDefault(skillId, 0xFFFFFF);

                    guiGraphics.drawString(font, "XP: " + xpCost, x, y, color, false);
                    i++;
                }
            } else if (skillSubPage == 1 && Configuration.isSecondSkillPageEnabled()) {
                List<Configuration.CustomSkillSlot> customSkills = Configuration.getCustomSkills();

                for (int i = 0; i < 8; i++) {
                    int x = left + (i % 2) * 83 + 10;
                    int y = top + (i / 2) * 36 + 20;

                    Configuration.CustomSkillSlot slot = i < customSkills.size() ? customSkills.get(i) : null;
                    String skillId = slot != null ? normalizeSkillId(slot.id) : "";
                    String xpCost = xpCostDisplay.getOrDefault(skillId, "N/A");
                    int color = xpCostColor.getOrDefault(skillId, 0xFFFFFF);

                    if (slot != null && slot.isEnabled()) {
                        guiGraphics.drawString(font, "XP: " + xpCost, x, y, color, false);
                    }
                }
            }
        } else {
            if (perkSubPage == 0) {
                renderPerksPage(guiGraphics, left, top);
            } else {
                renderCustomPerksPage(guiGraphics, left, top);
            }
        }

        if (Configuration.isSecondSkillPageEnabled()) {
            Component subPageTitle;
            if (page == 0) {
                subPageTitle = Component.literal(skillSubPage == 0 ? "Built-In Skills" : "Custom Skills");
            } else {
                subPageTitle = Component.literal(perkSubPage == 0 ? "Built-In Perks" : "Custom Perks");
            }

            int titleX = left + 88 - (font.width(subPageTitle) / 2);
            int titleY = top + SUBPAGE_TITLE_Y;
            guiGraphics.drawString(font, subPageTitle, titleX, titleY, TITLE_COLOR, false);
        }

        // Render widgets manually instead of super.render(...)
        for (var renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        if (page == 0) {
            for (var widget : this.renderables) {
                if (widget instanceof SkillButton button && button.isMouseOver(mouseX, mouseY)) {
                    var tooltipLines = button.getTooltipLines(Minecraft.getInstance().player);
                    guiGraphics.renderTooltip(
                            font,
                            tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                            mouseX,
                            mouseY
                    );
                    break;
                } else if (widget instanceof CustomSkillButton button && button.isMouseOver(mouseX, mouseY)) {
                    var tooltipLines = button.getTooltipLines(Minecraft.getInstance().player);
                    guiGraphics.renderTooltip(
                            font,
                            tooltipLines.stream().map(Component::getVisualOrderText).toList(),
                            mouseX,
                            mouseY
                    );
                    break;
                }
            }
        }
    }

    private void renderPerksPage(GuiGraphics gui, int left, int top) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int row = 0;

        for (Skill skill : Skill.values()) {
            if (row >= 8) break;

            int skillLevel = model.getSkillLevel(normalizeSkillId(skill.name()));

            int boxX = left + PERK_BOX_X;
            int boxY = top + PERK_BOX_Y + (row * PERK_ROW_HEIGHT);

            int textX = boxX + PERK_TEXT_OFFSET_X;
            int textY = boxY + 5;

            Component line = buildSinglePerkLine(skill, skillLevel);

            float scale = 0.85f;

            gui.pose().pushPose();
            gui.pose().translate(textX, textY, 0);
            gui.pose().scale(scale, scale, 1.0F);
            gui.drawString(this.font, line, 0, 0, 0xFFFFFF, false);
            gui.pose().popPose();

            row++;
        }
    }

    private void renderCustomPerksPage(GuiGraphics gui, int left, int top) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int row = 0;

        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot == null || !slot.isEnabled() || !slot.hasPerk()) {
                continue;
            }

            int skillLevel = model.getSkillLevel(normalizeSkillId(slot.id));

            int boxX = left + PERK_BOX_X;
            int boxY = top + PERK_BOX_Y + (row * PERK_ROW_HEIGHT);

            ResourceLocation icon = null;
            try {
                if (slot.icon != null && !slot.icon.isBlank()) {
                    icon = ResourceLocation.parse(slot.icon);
                }
            } catch (Exception ignored) {
            }

            if (icon != null) {
                RenderSystem.setShaderTexture(0, icon);
                gui.blit(icon, boxX + 2, boxY + 2, 0, 0, 12, 12, 12, 12);
                RenderSystem.setShaderTexture(0, SkillScreen.PERKS_ADDITIONAL_TEXTURE);
            }

            int textX = boxX + PERK_TEXT_OFFSET_X;
            int textY = boxY + 5;

            Component line = buildCustomPerkLine(slot, skillLevel);

            float scale = 0.85f;

            gui.pose().pushPose();
            gui.pose().translate(textX, textY, 0);
            gui.pose().scale(scale, scale, 1.0F);
            gui.drawString(this.font, line, 0, 0, 0xFFFFFF, false);
            gui.pose().popPose();

            row++;
        }
    }

    private Component buildCustomPerkLine(Configuration.CustomSkillSlot slot, int skillLevel) {
        Component skillName = Component.literal(slot.displayName)
                .withStyle(ChatFormatting.GOLD);

        int step = Math.max(1, slot.perkStep);
        int bonusSteps = skillLevel / step;
        double totalBonus = bonusSteps * slot.perkAmountPerStep;

        String operation = slot.perkOperation == null
                ? "ADDITION"
                : slot.perkOperation.trim().toUpperCase(Locale.ROOT);

        String amountText;
        if (operation.contains("MULTIPLY")) {
            amountText = String.format("+%.0f%%", totalBonus * 100.0);
        } else {
            amountText = String.format("+%.2f", totalBonus);
        }

        Component amount = Component.literal(amountText).withStyle(ChatFormatting.AQUA);

        Attribute attr = null;
        try {
            if (slot.perkAttribute != null && !slot.perkAttribute.isBlank()) {
                attr = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(slot.perkAttribute));
            }
        } catch (Exception ignored) {
        }

        Component effect = attr != null
                ? Component.translatable(attr.getDescriptionId()).withStyle(ChatFormatting.GRAY)
                : Component.literal("No Attribute").withStyle(ChatFormatting.GRAY);

        return Component.literal("")
                .append(skillName)
                .append(": ")
                .append(amount)
                .append(" ")
                .append(effect);
    }

    private Component buildSinglePerkLine(Skill skill, int skillLevel) {
        Component skillName = Component.translatable("skill." + skill.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GOLD);

        Component amount;
        Component effect;

        switch (skill) {
            case AGILITY -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                Attribute attr = bonus != null ? bonus.getAttribute() : null;
                effect = attr != null
                        ? Component.translatable(attr.getDescriptionId()).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("tooltip.rereskillable.run_speed").withStyle(ChatFormatting.GRAY);
            }

            case MINING -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.break_speed")
                        .withStyle(ChatFormatting.GRAY);
            }

            case GATHERING -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                effect = Component.translatable("tooltip.rereskillable.bonus_xp_orbs")
                        .withStyle(ChatFormatting.GRAY);
            }

            case FARMING -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                double perStep = bonus != null ? bonus.getBonusPerStep() : 0.0;
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                effect = Component.translatable("tooltip.rereskillable.crop_growth")
                        .withStyle(ChatFormatting.GRAY);
            }

            default -> {
                SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(skill);
                if (bonus == null) {
                    return Component.empty();
                }

                double perStep = bonus.getBonusPerStep();
                double pct = skillLevel >= 5 ? (skillLevel / 5.0) * perStep * 100.0 : 0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                Attribute attr = bonus.getAttribute();
                effect = attr != null
                        ? Component.translatable(attr.getDescriptionId()).withStyle(ChatFormatting.GRAY)
                        : Component.empty();
            }
        }

        return Component.literal("")
                .append(skillName)
                .append(": ")
                .append(amount)
                .append(" ")
                .append(effect);
    }

    private int calculateTotalXP(Player player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        if (level <= 16) {
            return (level * level) + (6 * level) + Math.round(progress * (2 * level + 7));
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360) + Math.round(progress * (5 * level - 38));
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220) + Math.round(progress * (9 * level - 158));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        boolean levelingEnabled = Configuration.isSkillLevelingEnabled();
        int max = Configuration.getMaxLevel();


        for (var widget : this.renderables) {
            if (widget instanceof SkillButton btn) {
                String skillId = btn.getSkillId();
                Skill skill = getVanillaSkillOrNull(skillId);
                if (skill == null) continue;

                int level = model.getSkillLevel(skillId);

                int cost = Configuration.calculateCostForLevel(level);
                int playerTotalXp = calculateTotalXP(player);
                boolean hasXP = player.isCreative() || playerTotalXp >= cost;

                xpCostDisplay.put(skillId, String.valueOf(cost));
                xpCostColor.put(skillId, hasXP ? 0x00FF00 : 0xFF0000);

                boolean maxed = level >= max;

                GateUiResult gate = checkGateClient(model, skillId, level);

                btn.active = true;

                boolean blockedByGate = levelingEnabled && !maxed && !gate.allowed;
                Component tooltip = blockedByGate ? gate.missingListComponent() : null;

                btn.setGateBlocked(blockedByGate, tooltip);
            } else if (widget instanceof CustomSkillButton btn) {
                Configuration.CustomSkillSlot slot = btn.getSkillSlot();
                if (slot == null || !slot.isEnabled()) {
                    btn.active = false;
                    continue;
                }

                String skillId = normalizeSkillId(slot.id);
                int level = model.getSkillLevel(skillId);

                int cost = Configuration.calculateCostForLevel(level);
                int playerTotalXp = calculateTotalXP(player);
                boolean hasXP = player.isCreative() || playerTotalXp >= cost;

                xpCostDisplay.put(skillId, String.valueOf(cost));
                xpCostColor.put(skillId, hasXP ? 0x00FF00 : 0xFF0000);

                boolean maxed = level >= max;

                GateUiResult gate = checkGateClient(model, skillId, level);
                btn.active = true;

                boolean blockedByGate = levelingEnabled && !maxed && !gate.allowed;
                Component tooltip = blockedByGate ? gate.missingListComponent() : null;

                btn.setGateBlocked(blockedByGate, tooltip);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            return true;
        }

        if (KeyBinding.SKILLS_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static GateUiResult checkGateClient(SkillModel model, String levelingSkillId, int currentLevel) {
        List<? extends String> rules;
        try {
            rules = Configuration.SKILL_LEVEL_GATES.get();
        } catch (Throwable t) {
            return GateUiResult.allowed();
        }

        if (rules == null || rules.isEmpty()) return GateUiResult.allowed();

        int totalLevels = 0;
        for (Skill s : Skill.values()) totalLevels += model.getSkillLevel(normalizeSkillId(s.name()));
        for (Configuration.CustomSkillSlot slot : Configuration.getCustomSkills()) {
            if (slot != null && slot.isEnabled()) {
                totalLevels += model.getSkillLevel(normalizeSkillId(slot.id));
            }
        }

        List<MissingUiReq> missing = new ArrayList<>();

        for (String line : rules) {
            GateUiRule rule = GateUiRule.parse(line);
            if (rule == null) continue;

            if (!rule.matchesTarget(levelingSkillId)) continue;
            if (currentLevel < rule.minCurrentLevel) continue;

            if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                missing.add(MissingUiReq.total(rule.minTotalLevels));
            }

            for (Map.Entry<String, Integer> e : rule.minSkillLevels.entrySet()) {
                String reqSkillId = normalizeSkillId(e.getKey());
                int reqLevel = e.getValue();

                int actual = model.getSkillLevel(reqSkillId);
                if (actual < reqLevel) {
                    missing.add(MissingUiReq.skill(reqSkillId, reqLevel));
                }
            }

            for (ResourceLocation advId : rule.requiredAdvancements) {
                missing.add(MissingUiReq.advancement(advId));
            }
        }

        if (missing.isEmpty()) return GateUiResult.allowed();

        Map<String, MissingUiReq> best = new LinkedHashMap<>();
        for (MissingUiReq req : missing) {
            String key = req.key();
            MissingUiReq existing = best.get(key);
            if (existing == null || req.required > existing.required) {
                best.put(key, req);
            }
        }

        return GateUiResult.blocked(new ArrayList<>(best.values()));
    }

    private static final class GateUiRule {
        final String targetSkillId;
        final int minCurrentLevel;
        final Integer minTotalLevels;
        final Map<String, Integer> minSkillLevels;
        final List<ResourceLocation> requiredAdvancements;

        private GateUiRule(String targetSkillId, int minCurrentLevel, Integer minTotalLevels,
                           Map<String, Integer> minSkillLevels,
                           List<ResourceLocation> requiredAdvancements) {
            this.targetSkillId = targetSkillId;
            this.minCurrentLevel = minCurrentLevel;
            this.minTotalLevels = minTotalLevels;
            this.minSkillLevels = minSkillLevels;
            this.requiredAdvancements = requiredAdvancements;
        }

        boolean matchesTarget(String skillId) {
            return targetSkillId.equals(normalizeSkillId(skillId));
        }

        static GateUiRule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            String targetSkillId = normalizeSkillId(parts[0]);

            boolean validBuiltIn = isBuiltInSkill(targetSkillId);
            boolean validCustom = Configuration.getCustomSkill(targetSkillId) != null;
            if (!validBuiltIn && !validCustom) {
                return null;
            }

            int minLevel;
            try {
                minLevel = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }

            Integer totalReq = null;
            Map<String, Integer> skillReqs = new LinkedHashMap<>();
            List<ResourceLocation> advReqs = new ArrayList<>();

            String reqs = parts[2].trim();
            if (!reqs.isEmpty()) {
                for (String raw : reqs.split(",")) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = normalizeSkillId(kv[0]);
                    String value = kv[1].trim();

                    if (key.equals("total")) {
                        try {
                            totalReq = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                        }
                        continue;
                    }

                    if (key.equals("adv") || key.equals("advancement")) {
                        try {
                            advReqs.add(ResourceLocation.parse(value));
                        } catch (Exception ignored) {
                        }
                        continue;
                    }

                    try {
                        int val = Integer.parseInt(value);
                        if (isBuiltInSkill(key) || Configuration.getCustomSkill(key) != null) {
                            skillReqs.put(key, val);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            return new GateUiRule(targetSkillId, Math.max(0, minLevel), totalReq, skillReqs, advReqs);
        }
    }

    private static final class GateUiResult {
        final boolean allowed;
        final List<MissingUiReq> missing;

        private GateUiResult(boolean allowed, List<MissingUiReq> missing) {
            this.allowed = allowed;
            this.missing = missing;
        }

        static GateUiResult allowed() {
            return new GateUiResult(true, List.of());
        }

        static GateUiResult blocked(List<MissingUiReq> missing) {
            return new GateUiResult(false, missing);
        }

        MutableComponent missingListComponent() {
            if (missing == null || missing.isEmpty()) {
                return Component.translatable("message.reskillable.gate_missing_unknown");
            }

            MutableComponent out = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) out.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                out.append(missing.get(i).toComponent());
            }
            return out;
        }
    }

    private static final class MissingUiReq {
        final String skillId;
        final int required;
        final ResourceLocation advId;

        private MissingUiReq(String skillId, int required, ResourceLocation advId) {
            this.skillId = skillId;
            this.required = required;
            this.advId = advId;
        }

        static MissingUiReq total(int required) {
            return new MissingUiReq(null, required, null);
        }

        static MissingUiReq skill(String skillId, int required) {
            return new MissingUiReq(skillId, required, null);
        }

        static MissingUiReq advancement(ResourceLocation id) {
            return new MissingUiReq(null, 0, id);
        }

        String key() {
            if (advId != null) return "ADV:" + advId;
            return (skillId == null) ? "TOTAL" : skillId;
        }

        Component toComponent() {
            if (advId != null) {
                return Component.literal("Advancement: ")
                        .append(getAdvancementDescription(advId))
                        .withStyle(ChatFormatting.YELLOW);
            }

            if (skillId == null) {
                return Component.translatable("message.reskillable.req_total", required)
                        .withStyle(ChatFormatting.YELLOW);
            }

            Skill builtIn = getVanillaSkillOrNull(skillId);
            if (builtIn != null) {
                return Component.translatable(
                        "message.reskillable.req_skill",
                        Component.translatable(builtIn.getDisplayName()),
                        required
                ).withStyle(ChatFormatting.YELLOW);
            }

            Configuration.CustomSkillSlot slot = Configuration.getCustomSkill(skillId);
            String display = slot != null ? slot.displayName : skillId;
            return Component.literal(display + " level " + required).withStyle(ChatFormatting.YELLOW);
        }
    }

    private static Component getAdvancementDescription(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return prettyAdvancement(id);
        }

        try {
            AdvancementHolder holder = mc.player.connection.getAdvancements().get(id);
            if (holder == null) {
                return prettyAdvancement(id);
            }

            DisplayInfo display = holder.value().display().orElse(null);
            if (display != null) {
                return display.getDescription().copy();
            }
        } catch (Throwable ignored) {
        }

        return prettyAdvancement(id);
    }

    private static Component prettyAdvancement(ResourceLocation id) {
        String path = id.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1)
                .replace('_', ' ');

        String pretty = Arrays.stream(name.split(" "))
                .filter(s -> !s.isEmpty())
                .map(w -> w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(name);

        return Component.literal(pretty);
    }

    private static boolean isBuiltInSkill(String skillId) {
        return getVanillaSkillOrNull(skillId) != null;
    }

    private static Skill getVanillaSkillOrNull(String skillId) {
        try {
            return Skill.valueOf(normalizeSkillId(skillId).toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static class TabButton extends Button {
        public TabButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        }
    }

    private class SubPageButton extends Button {
        private final String arrow;

        public SubPageButton(int x, int y, String arrow, OnPress onPress) {
            super(x, y, 16, 14, Component.literal(arrow), onPress, DEFAULT_NARRATION);
            this.arrow = arrow;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);

            int color;
            if (!this.active) {
                color = 0x666666;
            } else if (hovered) {
                color = 0xFFFFFF;
            } else {
                color = 0xD8C79A;
            }

            Font font = Minecraft.getInstance().font;
            int textX = getX() + (width / 2) - (font.width(arrow) / 2);
            int textY = getY() + (height / 2) - 4;

            guiGraphics.drawString(font, Component.literal(arrow), textX, textY, color, false);
        }
    }

    private class CustomSkillButton extends Button {
        private final Configuration.CustomSkillSlot skillSlot;
        private boolean gateBlocked = false;
        private Component gateMissing = null;

        public CustomSkillButton(int x, int y, Configuration.CustomSkillSlot skillSlot) {
            super(x, y, 79, 32, Component.empty(), b -> {
                if (skillSlot != null && skillSlot.isEnabled()) {
                    RequestLevelUp.send(normalizeSkillId(skillSlot.id));
                }
            }, DEFAULT_NARRATION);
            this.skillSlot = skillSlot;
        }

        public Configuration.CustomSkillSlot getSkillSlot() {
            return skillSlot;
        }

        public void setGateBlocked(boolean blocked, Component missingList) {
            this.gateBlocked = blocked;
            this.gateMissing = missingList;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (skillSlot == null) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;

            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);

            boolean hover = isMouseOver(mouseX, mouseY);
            guiGraphics.blit(SkillScreen.RESOURCES, getX(), getY(), 176, hover ? 32 : 0, width, height);

            if (!skillSlot.isEnabled()) {
                guiGraphics.drawString(font, "-", getX() + 35, getY() + 12, 0x777777, false);
                return;
            }

            String skillId = normalizeSkillId(skillSlot.id);
            int level = model.getSkillLevel(skillId);
            int maxLevel = Configuration.getMaxLevel();

            ResourceLocation customIcon = null;
            try {
                if (skillSlot.icon != null && !skillSlot.icon.isBlank()) {
                    customIcon = ResourceLocation.parse(skillSlot.icon);
                }
            } catch (Exception ignored) {
            }

            if (customIcon != null) {
                RenderSystem.setShaderTexture(0, customIcon);
                guiGraphics.blit(customIcon, getX() + 6, getY() + 8, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);
            } else {
                guiGraphics.drawString(font, "?", getX() + 10, getY() + 10, 0xAAAAAA, false);
            }

            guiGraphics.drawString(font, Component.literal(skillSlot.displayName), getX() + 25, getY() + 7, 0xFFFFFF, false);
            guiGraphics.drawString(font, Component.literal(level + "/" + maxLevel), getX() + 25, getY() + 18, 0xBEBEBE, false);

            if (skillSlot.hasPerk() && !model.isPerkEnabled(skillId)) {
                int iconX = getX() + width - 10;
                int iconY = getY() + height - 10;
                guiGraphics.drawString(font, "✖", iconX, iconY, 0xFF5555, false);
            }

            if (gateBlocked) {
                guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88000000);

                RenderSystem.setShaderTexture(0, LOCK_ICON);

                int iconSize = 16;
                int iconX = getX() + width - iconSize - 3;
                int iconY = getY() + 3;

                guiGraphics.blit(LOCK_ICON, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

                RenderSystem.setShaderTexture(0, SkillScreen.RESOURCES);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.visible || !this.isMouseOver(mouseX, mouseY)) return false;
            if (skillSlot == null || !skillSlot.isEnabled()) return false;

            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return false;

            SkillModel model = SkillModel.get(player);
            if (model == null) return false;

            String skillId = normalizeSkillId(skillSlot.id);
            int level = model.getSkillLevel(skillId);
            int max = Configuration.getMaxLevel();

            if (button == 1) {
                if (skillSlot.hasPerk()) {
                    TogglePerk.send(skillId);
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.0F);
                    return true;
                }
                return false;
            }

            if (button == 0) {
                if (!Configuration.isSkillLevelingEnabled()) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                    return true;
                }

                if (level >= max) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                    return true;
                }

                if (gateBlocked) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                    return true;
                }

                int cost = Configuration.calculateCostForLevel(level);
                int playerXP = calculateTotalXP(player);
                if (!player.isCreative() && playerXP < cost) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 0.6F);
                    return true;
                }

                RequestLevelUp.send(skillId);
                return true;
            }

            return false;
        }

        @Override
        public void onPress() {
            if (skillSlot != null && skillSlot.isEnabled()) {
                RequestLevelUp.send(normalizeSkillId(skillSlot.id));
            }
        }

        public List<Component> getTooltipLines(Player player) {
            List<Component> lines = new ArrayList<>();
            if (skillSlot == null || !skillSlot.isEnabled()) {
                return lines;
            }

            SkillModel model = SkillModel.get(player);
            if (model == null) return lines;

            String skillId = normalizeSkillId(skillSlot.id);
            int level = model.getSkillLevel(skillId);
            int cost = Configuration.calculateCostForLevel(level);
            int playerXP = calculateTotalXP(player);

            Component xp = Component.literal(String.valueOf(playerXP))
                    .withStyle(playerXP >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
            Component costC = Component.literal(String.valueOf(cost));

            lines.add(Component.translatable("tooltip.rereskillable.skill_cost", xp, costC));

            if (skillSlot.hasPerk()) {
                boolean enabled = model.isPerkEnabled(skillId);
                lines.add(Component.literal("➤ ")
                        .append(Component.translatable("tooltip.rereskillable.right_click").withStyle(ChatFormatting.GOLD))
                        .append(Component.translatable(enabled
                                ? "tooltip.rereskillable.disable_perk"
                                : "tooltip.rereskillable.enable_perk"
                        ).withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)));
            }

            if (!Configuration.isSkillLevelingEnabled()) {
                lines.add(Component.translatable("message.reskillable.leveling_disabled").withStyle(ChatFormatting.RED));
                return lines;
            }

            int maxLevel = Configuration.getMaxLevel();
            if (level >= maxLevel) {
                lines.add(Component.translatable("message.reskillable.max_level", maxLevel).withStyle(ChatFormatting.RED));
                return lines;
            }

            if (gateBlocked) {
                boolean shift = Screen.hasShiftDown();

                lines.add(
                        Component.literal("🔒 ")
                                .append(Component.translatable("tooltip.reskillable.locked"))
                                .withStyle(ChatFormatting.RED)
                );

                if (!shift) {
                    lines.add(
                            Component.translatable("tooltip.reskillable.hold_shift")
                                    .withStyle(ChatFormatting.DARK_GRAY)
                    );
                    return lines;
                }

                if (gateMissing != null) {
                    lines.add(Component.empty());

                    for (Component c : gateMissing.getSiblings()) {
                        lines.add(
                                Component.literal("• ")
                                        .append(c.copy())
                                        .withStyle(ChatFormatting.YELLOW)
                        );
                    }
                }

                return lines;
            }

            lines.add(Component.literal("➤ ")
                    .append(Component.translatable("tooltip.rereskillable.left_click").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable("tooltip.rereskillable.level_up").withStyle(ChatFormatting.AQUA)));

            return lines;
        }
    }
}