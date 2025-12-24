package net.bandit.reskillable.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.client.KeyMapping;
import net.bandit.reskillable.client.screen.buttons.SkillButton;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.RequestLevelUp;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.chat.Style;


import java.util.*;

public class SkillScreen extends Screen {
    public static final ResourceLocation RESOURCES =
            new ResourceLocation("reskillable", "textures/gui/skills.png");
    public static final ResourceLocation PERKS_TEXTURE =
            new ResourceLocation("reskillable", "textures/gui/perks.png");

    private static final int PERK_BOX_X = 12;
    private static final int PERK_BOX_Y = 29;
    private static final int PERK_ROW_HEIGHT = 15;
    private static final int PERK_TEXT_OFFSET_X = 15;

    private int gatePreviewCooldownTicks = 0;
    private int lastTotalSkillLevels = -1;


    private final Map<Skill, String> xpCostDisplay = new HashMap<>();
    private final Map<Skill, Integer> xpCostColor = new HashMap<>();

    private int page = 0;
    private Button skillsTab;
    private Button perksTab;
    private static final int TITLE_COLOR = 0xE0D0A0;

    public SkillScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int left = (width - 162) / 2;
        int top = (height - 128) / 2;

        this.clearWidgets();

        if (page == 0) {
            for (int i = 0; i < 8; i++) {
                int x = left + i % 2 * 83;
                int y = top + i / 2 * 36;
                Skill skill = Skill.values()[i];

                addRenderableWidget(new SkillButton(x, y, skill));
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
        RequestLevelUp.clearClientGatePreview();
        gatePreviewCooldownTicks = 0;
        lastTotalSkillLevels = -1;
        requestGatePreview();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation background = (page == 0) ? RESOURCES : PERKS_TEXTURE;
        RenderSystem.setShaderTexture(0, background);

        int left = (width - 176) / 2;
        int top = (height - 166) / 2;

        renderBackground(guiGraphics);
        guiGraphics.blit(background, left, top, 0, 0, 176, 166);


        if (page == 0) {
            int i = 0;
            for (Skill skill : Skill.values()) {
                int x = left + (i % 2) * 83 + 10;
                int y = top + (i / 2) * 36 + 20;

                String xpCost = xpCostDisplay.getOrDefault(skill, "N/A");
                int color = xpCostColor.getOrDefault(skill, 0xFFFFFF);

                guiGraphics.drawString(font, "XP: " + xpCost, x, y, color, false);
                i++;
            }
        } else {
            renderPerksPage(guiGraphics, left, top);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

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
            int skillLevel = model.getSkillLevel(skill);

            int boxX = left + PERK_BOX_X;
            int boxY = top + PERK_BOX_Y + (row * PERK_ROW_HEIGHT);

            int iconX = boxX + 1;
            int iconY = boxY + 1;

            int textX = boxX + PERK_TEXT_OFFSET_X;
            int textY = boxY + 5;

            Component line = buildSinglePerkLine(skill, skillLevel);

            float scale = 0.85f; // adjust: 0.75–0.9

            gui.pose().pushPose();
            gui.pose().translate(textX, textY, 0);
            gui.pose().scale(scale, scale, 1.0F);
            gui.drawString(this.font, line, 0, 0, 0xFFFFFF, false);
            gui.pose().popPose();

            row++;
        }
    }

    private Component buildSinglePerkLine(Skill skill, int skillLevel) {
        Component skillName = Component.translatable("skill." + skill.name().toLowerCase())
                .withStyle(ChatFormatting.GOLD);

        boolean locked = skillLevel < 5;

        Component amount;
        Component effect;

        switch (skill) {
            case AGILITY -> {
                int steps = skillLevel / 5;
                double perStep = Configuration.MOVEMENT_SPEED_BONUS.get();
                double pct = steps * perStep * 100.0;

                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);

                effect = Component.translatable("tooltip.rereskillable.run_speed")
                        .withStyle(ChatFormatting.GRAY);
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
                double pct = skillLevel >= 5
                        ? (skillLevel / 5.0) * Configuration.GATHERING_XP_BONUS.get() * 100.0
                        : 0;
                amount = Component.literal(String.format("+%.0f%%", pct))
                        .withStyle(ChatFormatting.AQUA);
                effect = Component.translatable("tooltip.rereskillable.bonus_xp_orbs")
                        .withStyle(ChatFormatting.GRAY);
            }
            case FARMING -> {
                double perStep = Configuration.CROP_GROWTH_CHANCE.get();
                double pct = skillLevel >= 5
                        ? (skillLevel / 5.0) * perStep * 100.0
                        : 0;
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

        MutableComponent line = Component.literal("")
                .append(skillName)
                .append(": ")
                .append(amount)
                .append(" ")
                .append(effect);


        return line;
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

    private int getLevelForTotalXP(int totalXP) {
        int level = 0;
        while (getCumulativeXPForLevel(level + 1) <= totalXP) {
            level++;
        }
        return level;
    }

    private int getCumulativeXPForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return (level * (level + 1)) + (level * 6);
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
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
        if (page == 0) {
            int total = 0;
            for (Skill s : Skill.values()) total += model.getSkillLevel(s);

            if (gatePreviewCooldownTicks > 0) gatePreviewCooldownTicks--;
            boolean totalChanged = (lastTotalSkillLevels != -1 && total != lastTotalSkillLevels);
            boolean periodic = (gatePreviewCooldownTicks == 0);

            if (lastTotalSkillLevels == -1 || totalChanged || periodic) {
                requestGatePreview();
                gatePreviewCooldownTicks = 10;
                lastTotalSkillLevels = total;
            }
        }

        for (var widget : this.renderables) {
            if (!(widget instanceof SkillButton btn)) continue;

            Skill skill = btn.getSkill();
            int level = model.getSkillLevel(skill);

            int cost = Configuration.calculateCostForLevel(level);
            int playerTotalXp = calculateTotalXP(player);
            boolean hasXP = player.isCreative() || playerTotalXp >= cost;

            xpCostDisplay.put(skill, String.valueOf(cost));
            xpCostColor.put(skill, hasXP ? 0x00FF00 : 0xFF0000);

            boolean maxed = level >= max;

            GateUiResult gate = checkGateClient(model, skill, level);

            btn.active = true;
            var preview = RequestLevelUp.getClientGatePreview(skill);

            boolean blockedClient = !gate.allowed;
            boolean blockedServer = preview != null && preview.blocked;

            boolean blockedByGate = levelingEnabled && !maxed && (blockedClient || blockedServer);

            Component tooltip = null;
            if (blockedByGate) {
                if (blockedServer && preview.missing != null && !preview.missing.getString().isEmpty()) {
                    tooltip = preview.missing;
                } else {
                    tooltip = gate.missingListComponent();
                }
            }

            btn.setGateBlocked(blockedByGate, tooltip);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
                return true;
            }

            if (KeyMapping.openKey.matches(keyCode, scanCode)) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    private static class TabButton extends Button {
        public TabButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // no vanilla background/text; SkillScreen handles drawing
        }
    }

    private static GateUiResult checkGateClient(SkillModel model, Skill levelingSkill, int currentLevel) {
        List<? extends String> rules;
        try {
            rules = Configuration.SKILL_LEVEL_GATES.get();
        } catch (Throwable t) {
            return GateUiResult.allowed();
        }
        if (rules == null || rules.isEmpty()) return GateUiResult.allowed();

        int totalLevels = 0;
        for (Skill s : Skill.values()) totalLevels += model.getSkillLevel(s);

        List<MissingUiReq> missing = new ArrayList<>();

        for (String line : rules) {
            GateUiRule rule = GateUiRule.parse(line);
            if (rule == null) continue;

            if (rule.skill != levelingSkill) continue;
            if (currentLevel < rule.minCurrentLevel) continue;

            if (rule.minTotalLevels != null && totalLevels < rule.minTotalLevels) {
                missing.add(MissingUiReq.total(rule.minTotalLevels));
            }

            for (Map.Entry<Skill, Integer> e : rule.minSkillLevels.entrySet()) {
                int actual = model.getSkillLevel(e.getKey());
                if (actual < e.getValue()) {
                    missing.add(MissingUiReq.skill(e.getKey(), e.getValue()));
                }
            }
        }

        if (missing.isEmpty()) return GateUiResult.allowed();

        // dedupe: keep highest requirement per key
        Map<String, MissingUiReq> best = new LinkedHashMap<>();
        for (MissingUiReq req : missing) {
            String key = req.key();
            MissingUiReq existing = best.get(key);
            if (existing == null || req.required > existing.required) best.put(key, req);
        }

        return GateUiResult.blocked(new ArrayList<>(best.values()));
    }
//    private static boolean hasAdvancementClient(ResourceLocation id) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.player.connection == null) return false;
//
//        var clientAdvancements = mc.player.connection.getAdvancements();
//        var holder = clientAdvancements.getAdvancements().get(id);
//        if (holder == null) return false;
//        return clientAdvancements.getProgress(holder).isDone();
//    }

    private static final class GateUiRule {
        final Skill skill;
        final int minCurrentLevel;
        final Integer minTotalLevels;
        final Map<Skill, Integer> minSkillLevels;
        final List<ResourceLocation> requiredAdvancements;

        private GateUiRule(Skill skill, int minCurrentLevel, Integer minTotalLevels,
                           Map<Skill, Integer> minSkillLevels,
                           List<ResourceLocation> requiredAdvancements) {
            this.skill = skill;
            this.minCurrentLevel = minCurrentLevel;
            this.minTotalLevels = minTotalLevels;
            this.minSkillLevels = minSkillLevels;
            this.requiredAdvancements = requiredAdvancements;
        }

        static GateUiRule parse(String line) {
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) return null;

            String[] parts = line.split(":", 3);
            if (parts.length < 3) return null;

            Skill skill;
            try {
                skill = Skill.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }

            int minLevel;
            try {
                minLevel = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                return null;
            }

            Integer totalReq = null;
            Map<Skill, Integer> skillReqs = new LinkedHashMap<>();
            List<ResourceLocation> advReqs = new ArrayList<>();

            String reqs = parts[2].trim();
            if (!reqs.isEmpty()) {
                for (String raw : reqs.split(",")) {
                    String tok = raw.trim();
                    if (tok.isEmpty()) continue;

                    String[] kv = tok.split("=", 2);
                    if (kv.length != 2) continue;

                    String key = kv[0].trim().toUpperCase(Locale.ROOT);
                    String value = kv[1].trim();

                    if (key.equals("TOTAL")) {
                        try {
                            totalReq = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {}
                        continue;
                    }

                    // NEW: ADV requirement (string value, not int)
                    if (key.equals("ADV") || key.equals("ADVANCEMENT")) {
                        try {
                            advReqs.add(new ResourceLocation(value));
                        } catch (Exception ignored) {
                            // invalid RL -> ignore
                        }
                        continue;
                    }

                    // skill reqs (int)
                    try {
                        int val = Integer.parseInt(value);
                        Skill reqSkill = Skill.valueOf(key);
                        skillReqs.put(reqSkill, val);
                    } catch (Exception ignored) {}
                }
            }

            return new GateUiRule(skill, Math.max(0, minLevel), totalReq, skillReqs, advReqs);
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
        final Skill skill; // null means TOTAL
        final int required;
        final ResourceLocation advId;

        private MissingUiReq(Skill skill, int required, ResourceLocation advId) {
            this.skill = skill;
            this.required = required;
            this.advId = advId;
        }

        static MissingUiReq total(int required) {
            return new MissingUiReq(null, required, null);
        }

        static MissingUiReq skill(Skill skill, int required) {
            return new MissingUiReq(skill, required, null);
        }

        static MissingUiReq advancement(ResourceLocation id) {
            return new MissingUiReq(null, 0, id);
        }

        String key() {
            if (advId != null) return "ADV:" + advId;
            return (skill == null) ? "TOTAL" : skill.name();
        }

        Component toComponent() {
            if (advId != null) {
                return Component.literal("Advancement: ")
                        .append(prettyAdvancement(advId))
                        .withStyle(ChatFormatting.YELLOW);
            }

            if (skill == null) {
                return Component.translatable("message.reskillable.req_total", required)
                        .withStyle(ChatFormatting.YELLOW);
            }

            return Component.translatable(
                    "message.reskillable.req_skill",
                    Component.translatable("skill.reskillable." + skill.name().toLowerCase(Locale.ROOT)),
                    required
            ).withStyle(ChatFormatting.YELLOW);
        }
    }
    private void requestGatePreview() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (Reskillable.NETWORK == null) return;

        Reskillable.NETWORK.sendToServer(new RequestLevelUp.RequestGatePreviewPacket());
    }
    private static Component prettyAdvancement(ResourceLocation id) {
        // minecraft:story/mine_diamond → Mine Diamonds
        String path = id.getPath(); // story/mine_diamond
        String name = path.substring(path.lastIndexOf('/') + 1)
                .replace('_', ' ');

        String pretty = Arrays.stream(name.split(" "))
                .map(w -> w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(name);

        return Component.literal(pretty);
    }

}
