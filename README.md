---
title: Reskillable Reimagined
description: "Reskillable Reimagined Wiki"
icon: trending
---

# Reskillable Reimagined

Reskillable is an RPG-style progression mod that introduces a **skill-based locking and leveling system** for Minecraft.  
Players must earn experience and level up skills in order to use powerful gear, defeat strong enemies, and access advanced content.

This guide covers the **entire system**, including skills, perks, configs, commands, and customization options.

---

## 🔥 Core Features

Reskillable adds:

- A full **skill leveling system**
- **Gear gating** by player skill level
- **Entity attack requirements** (mobs can require skill levels to damage)
- **Crafting requirements**
- A modular **perk system**
- Configurable **attribute bonuses**
- XP-scaling and level progression
- Full datapack-style JSON customization

---

## 🎯 Skill System

Each player has multiple skills they can level up using experience.

### Available Skills

| Skill | Purpose |
|------|--------|
| **Attack** | Controls ability to use weapons and defeat strong enemies |
| **Defense** | Controls armor usage and survivability |
| **Mining** | Controls use of mining tools |
| **Gathering** | Controls use of gathering tools like shovels & axes |
| **Farming** | Controls hoe/tool usage for crops |
| **Building** | Controls building-related actions (block reach, etc.) |
| **Agility** | Grants movement and mobility perks |
| **Magic** | Controls magical items & spell-based gear |

# 🧪 Custom Skills

Reskillable also supports **custom skills**, allowing modpacks and servers to add their own progression categories beyond the built-in skills.

Custom skills are defined in:

Example:
config/reskillable/custom_skills.json
```json
{
  "id": "swimming",
  "displayName": "Swimming",
  "perkAttribute": "forge:swim_speed",
  "icon": "reskillable:textures/gui/custom_skills/swimming.png",
  "perkOperation": "ADDITION",
  "perkAmountPerStep": 0.1,
  "perkStep": 5
}
```
### Field Breakdown

| Field | Description |
|------|-------------|
| `id` | Internal skill ID used by the mod, commands, locks, and gates |
| `displayName` | Name shown in the skill screen |
| `perkAttribute` | Attribute granted by this skill's perk |
| `icon` | Texture path used for the skill icon |
| `perkOperation` | Attribute modifier operation, such as `ADDITION` |
| `perkAmountPerStep` | How much bonus is granted each milestone |
| `perkStep` | How many levels are required per perk milestone |

### Notes

- `id` should be lowercase and unique.
- The `id` is what you use in:
  - skill locks
  - skill level gates
  - commands
- Custom skills start at **level 1**, just like built-in skills.
- If a custom skill has a valid perk setup, it can also appear in the **custom perks** page.
- Custom skill icons should point to a valid texture path.

### Using Custom Skills in Locks or Gates

Example skill lock:

```json
{
  "skillLocks": {
    "minecraft:trident": ["swimming:10"]
  }
}
```

Example skill gate:
```toml
skill_level_gates = [
  "SWIMMING:10:TOTAL=25"
]

```

## 📈 Leveling & XP System

- Players spend XP to upgrade skills via the skills menu.
- XP cost scales dynamically using both:
  - Predefined XP scaling curves
  - A configurable multiplier

### XP Scaling
- XP requirements scale similarly to vanilla Minecraft levels.
- XP progression beyond level 50 uses a fallback scaling system.
- Fully configurable using:

```
xpScalingMultiplier
levelsPerHeart
healthPerHeart
maximumLevel
```

---

## 💥 Attribute Bonuses

Every **5 levels** in a skill grants permanent bonuses.

These bonuses are configurable via the config.

### Example Bonuses

| Skill | Bonus |
|------|-------|
| **Attack** | Extra attack damage |
| **Defense** | Extra armor + armor toughness |
| **Agility** | Movement speed |
| **Magic** | Custom attribute (configurable ID) |
| **Building** | Block reach distance |
| **Gathering** | Bonus XP gain |
| **Mining** | Mining speed |
| **Farming** | Crop growth chance |

All of these are adjustable via config.

---

## 🧩 Perk System

At certain levels, skills unlock **perks** that modify gameplay.

Examples:
- Increase movement speed  
- Bonus mining reach  
- Extra block break power  
- Faster gathering speed  
- Magical attribute boosts  

Perks can be toggled and updated dynamically.

---

## 🛠 Skill Locking System

Reskillable can restrict:

✅ Item usage  
✅ Entity attacks  
✅ Crafting recipes  

By skill level.

### 1. Item Skill Restrictions

Controlled by:
```
config/reskillable/skill_locks.json
```

Example:
```json
{
  "skillLocks": {
    "minecraft:diamond_sword": ["attack:15"],
    "minecraft:netherite_chestplate": ["defense:30"]
  }
}
```

Meaning:
- You need **Attack 15** to use a diamond sword.
- You need **Defense 30** for netherite armor.

---

### 2. Entity Attack Restrictions

You can restrict attacking mobs:

```
config/reskillable/attack_skill_locks.json
```

Example:
```json
{
  "attackSkillLocks": {
    "minecraft:warden": ["attack:25"]
  }
}
```

If your skill is too low:
- You do no damage
- Or reduced damage (depending on config)

---

### 3. Craft Skill Restrictions

Crafting can also be locked:

```
config/reskillable/craft_skill_locks.json
```

You can make crafting recipes require certain skill levels.

---

## ⚙ Config Options (Config File)

The main config controls mod behavior.

### Important Config Options

| Config | Description                                                                      |
|--------|----------------------------------------------------------------------------------|
| `disableWoolDrops` | Disables wool drops unless shears are used                                       |
| `deathSkillReset` | Reset skills on player death                                                     |
| `maxSpendableLevels` | Maximum total levels a player can spend across all skills                        |
| `maximumLevel` | Max level a skill can reach                                                      |
| `xpScalingMultiplier` | Global XP scaling multiplier                                                     |
| `enableSkillLeveling` | Toggle skill leveling entirely                                                   |
| `enableSecondSkillPage` | Toggle second page for custom skills!                                            |
| `enableSkillUpMessage` | Shows skill-up chat messages                                                     |
| `levelsPerHeart` | Total combined skill levels required per extra heart                             |
| `healthPerHeart` | Amount of health gained per heart milestone                                      |
| `magicAttribute` | Attribute ID used for the Magic skill                                            |
| `attackDamageBonus` | Attack damage bonus per milestone                                                |
| `armorBonus` | Defense armor bonus per milestone                                                |
| `movementSpeedBonus` | Agility movement speed bonus                                                     |
| `blockReachBonus` | Building block reach bonus                                                       |
| `miningSpeedMultiplier` | Mining speed bonus per milestone                                                 |
| `cropGrowthChancePer5Levels` | Farming crop growth bonus                                                        |
| `gatheringXpBonus` | Bonus XP gained from gathering actions                                           |
| `skill_level_gates` | Defines progression gates that restrict skill leveling until requirements are met |

---

## 🔒 Skill Level Gating

Reskillable supports **skill level gating**, allowing you to prevent players from leveling a skill until certain progression requirements are met.

This is configured using the `skill_level_gates` option in the main config.

Skill gates do **not** prevent XP gain — they only prevent leveling up until requirements are satisfied.

---

### 📄 Config Format

```toml
# "Format: SKILL:MIN_CURRENT_LEVEL:REQS",
# "Example: ATTACK:10:TOTAL=30,MINING=5,DEFENSE=5",
# "New token: ADV=<namespace:path> (player must have completed the advancement)",
# "You can include multiple: ADV=minecraft:story/mine_diamond,ADV=minecraft:nether/root",
# "Tokens: TOTAL=<n>, OTHER_SKILL=<n>, ADV=<advancement_id>"
```

skill_level_gates = []

### Example's:

```toml
skill_level_gates = [
  "ATTACK:15:ADV=minecraft:story/mine_diamond"
]
```
### Prevents leveling after ATTACK 15 until mine diamonds advancement has been completed:

- you can use custom advancements or default ones
- graet for ftbquests or other ways of preventing skill leveling


```toml
skill_level_gates = [
  "ATTACK:10:TOTAL=30"
]
```
- Attack can be leveled freely up to level 10
- from level 10 onward, the player must have 30 total skill levels

```toml
skill_level_gates = [
  "ATTACK:15:TOTAL=40,MINING=10,DEFENSE=10"
]
```
### Attack level 15+ requires:
- 40 total skill levels
- MINING level 10
- DEFENSE level 10
`all Requirements must be met to level attack past level 15`

```toml
skill_level_gates = [
  "ATTACK:10:TOTAL=25",
  "ATTACK:20:TOTAL=50",
  "MAGIC:15:ATTACK=10"
]
```

### Example of Multiple settings and 2 for 1:
- To level ATTACK past 10 you need 25 levels 
- To level ATTACK past 20 you need 50 levels
- to level MAGIC past 15 you need 10 ATTACK levels
---

## ❤️ Health Scaling

Reskillable allows players to gain bonus health:

- Every X total skill levels → gain a heart  
- Fully controlled via:
  - `levelsPerHeart`
  - `healthPerHeart`

This makes progression feel more RPG-like.

---

## 🧠 Magic Skill Integration

The **Magic** skill connects to other mods using attributes.

You can define a custom mod attribute:

```
magicAttribute = "modid:spell_power"
```

If missing or invalid, it defaults to `minecraft:generic.luck`.

---

## 📟 Commands

Reskillable provides powerful commands:

### `/skills`

Base command

### `/skills set`

Set a player's skill level manually.

### `/skills set <player> all <level>`

Sets all built-in and enabled custom skills to the same level.

### `/skills add <player> all <amount>`

Adds levels to all built-in and custom skills (in enabled).

### `/skills get`

Check a player's skill levels.

### `/skills reload`

Reloads all configs and JSON files live.
(Works in singleplayer only and only if your file is a certain size after it gets to big you will need to restart game.)

### `/skills scanmod <modid>`

Automatically scans a mod and adds all its items into your skill lock config. (Singleplayer only)
(1.21.1 adds all items/1.20.1 only adds weapons/tools/armor. Values added(if any)are merely placeholders
you should triple check anything this mod command adds(it does not overwrite existing.))

### `/skills respec <player>`

Resets all built-in and custom skills back to level 1 and refunds the XP spent on leveling them.

Example:
```mcfunction
/skills respec PlayerName
```
This command:

resets built-in skills

resets custom skills

clears stored skill XP

refunds spent XP

reapplies perks and attributes
---

## 🔍 JSON Presets & Defaults

Reskillable automatically generates:

- `skill_locks.json`
- `custom_skills.json`
- `attack_skill_locks.json`
- `craft_skill_locks.json`

It also includes default vanilla benchmarks for:
- Armor tiers
- Weapon power
- Tool progression

---

## 🔌 Modpack & Developer Tools

Reskillable is made for:

- Modpacks
- RPG progression systems
- Hardcore difficulty packs
- Servers with progression gates

It supports:
- Easy integration with other mods
- Custom attribute linking
- Datapack-style progression

---

## 🌟 Who Should Use Reskillable?

Perfect for:

✅ RPG modpacks  
✅ Progression-based servers  
✅ Hardcore survival worlds  
✅ Magic-focused packs  
✅ Adventure maps  

---

## ❓ FAQ

### Can I completely disable leveling?
Yes:
```
enableSkillLeveling = false
```
Skills then must be granted via commands or datapacks.

---

### Can I lock modded items?
Yes!  Just enter modid:itemid


---

### Can I use my custom magic attribute for perks?
Yes.  
Just set:
```
magicAttribute = "yourmod:attribute"
```

---

### Can players bypass skill locks?
Only via creative mode or commands.

---

## ✅ Final Notes

Reskillable is designed to be:
- Modular  
- Datapack-friendly  
- Highly configurable  
- Easy to expand  

Every aspect of player progression is fully customizable.

---

⚔️ Reskill your world.  
📈 Level with purpose.  
🔥 Earn your power.
