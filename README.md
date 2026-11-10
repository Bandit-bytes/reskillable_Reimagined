# Reskillable Reimagined

An RPG-style progression mod for Minecraft: earn experience, level up skills, and unlock gear, blocks, mobs, and perks as you grow stronger.

**Download:** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/reskillable-reimagined)

*Minecraft mod — not affiliated with Mojang or Microsoft.*

## Features

- **Skill leveling** — Attack, Defense, Mining, Gathering, Farming, Building, Agility, and Magic
- **Gear gating** — items, tools, and armor can require skill levels to use
- **Entity requirements** — strong mobs can require skill levels to damage
- **Crafting requirements** — lock recipes behind skill levels
- **Perks & attribute bonuses** — configurable rewards as skills level up
- **Custom skills** — modpacks and servers can add their own progression categories
- **Skill level gates** — hold back skill progress until progression milestones are met
- **Fully configurable** — datapack-style JSON customization
- **Developer API** — other mods can read skills, react to level-ups, and build their own UI ([see below](#-api-for-mod-developers))

📖 **[Full guide](docs/GUIDE.md)** — skills, perks, configs, commands, and customization.

## This branch

| | |
|---|---|
| Minecraft | **1.21.1** |
| Loader | NeoForge |
| Java | 21 |

**Required:** NeoForge 21.1+

**Built-in compatibility:** Curios, Iron's Spells 'n Spellbooks, Spell Engine, Timeless and Classics Zero (TaCZ), Jade

## 🔌 API for mod developers

**Give your mod a full RPG progression system without writing one.** Reskillable already handles skill levels, XP costs, perks, item/block/mob requirements, progression gates, custom skills, saving, and server ↔ client syncing. The API lets your mod build on all of it with simple static calls — so players get one progression system instead of several fighting each other.

**What you can do:**

- **Build your own UI** — read skills, icons, perks, levels, XP and level-up costs. On the client these read the synced data, so it's safe to call every frame. Turn on `hideSkillScreen` / `hideRequirementTooltips` and replace Reskillable's screen and tooltips entirely.
- **Explain locked level-ups** — `canLevelUp` runs the exact same checks as a real level-up and tells you *why* it's blocked (`MAX_LEVEL`, `NOT_ENOUGH_XP`, `SPENT_LEVEL_CAP`, `GATE_BLOCKED`…). `getGateMissing` lists what the player still needs.
- **React to progression** — unlock spells, titles, quests or effects when a skill levels up.
- **Control progression** — veto a level-up from your own quest, class, or faction system.
- **Respect the pack's skill locks** — check item, block, and entity requirements with one call, using the pack's own config.
- **Multiplayer-safe** — `levelUp` on the server behaves exactly like the player pressing the button (XP, sounds, sync, events). Client requests are re-checked by the server.
- **Custom skills for free** — skills a pack adds in `custom_skills.json` work through the same API with no extra code.

### Adding Reskillable to your project

Use [CurseMaven](https://www.cursemaven.com). The project ID is in the sidebar of the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/reskillable-reimagined); the file ID is the number at the end of a file's URL.

```gradle
repositories {
    maven {
        url = "https://www.cursemaven.com"
        content { includeGroup "curse.maven" }
    }
}

dependencies {
    compileOnly "curse.maven:reskillable-reimagined-<projectId>:<fileId>"
    runtimeOnly "curse.maven:reskillable-reimagined-<projectId>:<fileId>" // optional: test in dev
}
```

To keep Reskillable optional, add an `optional` dependency on `reskillable` in your `neoforge.mods.toml`, and only touch Reskillable classes when `ModList.get().isLoaded("reskillable")` is true (keeping those calls in their own class is the easiest way).

### Reading skills & levels

```java
import net.bandit.reskillable.common.ReskillableApi;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.RequirementType;

for (String skillId : ReskillableApi.getSkillIds()) {          // built-in + custom skills
    Component name = ReskillableApi.getDisplayName(skillId);
    int level      = ReskillableApi.getLevel(player, skillId);
    int cost       = ReskillableApi.getLevelUpCost(player, skillId);

    switch (ReskillableApi.canLevelUp(player, skillId)) {
        case ALLOWED       -> { /* show the level-up button */ }
        case NOT_ENOUGH_XP -> { /* "Needs " + cost + " XP" */ }
        case GATE_BLOCKED  -> { /* "Needs: " + ReskillableApi.getGateMissing(player, skillId) */ }
        default            -> { /* MAX_LEVEL, SPENT_LEVEL_CAP, ... */ }
    }
}

// Lock your own feature behind a skill
if (ReskillableApi.getLevel(player, "magic") < 15) return;

// Respect the pack's requirements (USE, CRAFT, or ATTACK)
if (!ReskillableApi.meetsRequirements(player, RequirementType.USE, stack)) {
    List<Requirement> missing = ReskillableApi.getMissingRequirements(player, RequirementType.USE, stack);
}
```

Skill ids are the `id` values in `built_in_skills.json` and `custom_skills.json`; built-in skills also accept their original names (`mining`, `attack`, …).

### Events

`SkillLevelChangedEvent` and `AllowLevelUpEvent` (both on the NeoForge event bus):

```java
@EventBusSubscriber(modid = MyMod.MODID)
public class ReskillableHooks {
    // After any level change: level-up, /skills add|set|respec, or the reset on death
    @SubscribeEvent
    static void onLevelChanged(SkillLevelChangedEvent e) {
        if (e.getSkillId().equals("magic") && e.getNewLevel() >= 20) {
            MySpells.unlock(e.getEntity(), "fireball");
        }
    }

    // Before a level-up is paid for: cancel to refuse it
    @SubscribeEvent
    static void onAllowLevelUp(AllowLevelUpEvent e) {
        if (e.getSkillId().equals("magic") && !QuestData.done(e.getEntity(), "arcane_trial")) {
            e.getEntity().sendSystemMessage(Component.literal("Complete the Arcane Trial first!"));
            e.setCanceled(true); // Reskillable sends no message itself, so tell the player why
        }
    }
}
```

### Replacing Reskillable's UI

In `config/reskillable-common.toml`:

- `hideSkillScreen = true` removes the skills screen, its inventory tab and its key (needs a restart).
- `hideRequirementTooltips = true` hides the requirement lines in item tooltips and Jade.

On the client, use `requestLevelUp`, `requestTogglePerk` and `requestGateStatus` — the server re-checks every request. Call `requestGateStatus()` when your screen opens and about once a second while it's open, so gates that depend on advancements update live.

### API at a glance

| | |
|---|---|
| **Skills** | `getSkillIds` · `isSkillEnabled` · `getDisplayName` · `getIcon` · `getPerks` · `getMaxLevel` · `getMaxSpentLevels` |
| **Player** | `getLevel` · `getTotalLevel` · `getSpentLevels` · `isPerkEnabled` · `getXp` · `getLevelUpCost` · `canLevelUp` · `getGateMissing` |
| **Requirements** | `getRequirements` · `getMissingRequirements` · `meetsRequirements` (items, blocks, entities) |
| **Client** | `requestLevelUp` · `requestTogglePerk` · `requestGateStatus` |
| **Server** | `levelUp` |
| **Events** | [`SkillLevelChangedEvent`](https://github.com/Bandit-bytes/reskillable_Reimagined/blob/1.21.1/src/main/java/net/bandit/reskillable/event/SkillLevelChangedEvent.java), [`AllowLevelUpEvent`](https://github.com/Bandit-bytes/reskillable_Reimagined/blob/1.21.1/src/main/java/net/bandit/reskillable/event/AllowLevelUpEvent.java) |

Every method is documented in [`ReskillableApi.java`](https://github.com/Bandit-bytes/reskillable_Reimagined/blob/1.21.1/src/main/java/net/bandit/reskillable/common/ReskillableApi.java).

> The API is available on the 1.21.1 NeoForge and 1.21.1 Fabric versions.

## Branches

Each Minecraft version / loader lives on its own branch:

| Branch | Minecraft | Loader |
|---|---|---|
| [`master`](https://github.com/Bandit-bytes/reskillable_Reimagined/tree/master) | 1.20.1 | Forge |
| [`1.21.1`](https://github.com/Bandit-bytes/reskillable_Reimagined/tree/1.21.1) | 1.21.1 | NeoForge |
| [`1.21.1-fabric`](https://github.com/Bandit-bytes/reskillable_Reimagined/tree/1.21.1-fabric) | 1.21.1 | Fabric |
| [`26.1`](https://github.com/Bandit-bytes/reskillable_Reimagined/tree/26.1) | 26.1.2 | NeoForge |

## Building

Requires JDK 21.

```sh
./gradlew build
```

The built jar ends up in `build/libs`.

## Reporting bugs & suggestions

Please use the [issue tracker](https://github.com/Bandit-bytes/reskillable_Reimagined/issues/new/choose) and pick the bug report or feature request template. For crashes, include a link to your crash report or `latest.log` (via [mclo.gs](https://mclo.gs)).

Pull requests and translations are welcome — thanks to everyone who has [contributed](https://github.com/Bandit-bytes/reskillable_Reimagined/graphs/contributors)!

## License

The **code** of this mod is licensed under the [MIT License](LICENSE).

**Assets and Creative Content Clause:** All textures, icons, models, designs, and other art included in this mod are the exclusive property of Bandit-Bytes / PixelDreamStudio. They are not covered by the MIT License and cannot be reused, copied, or redistributed without explicit written permission.
