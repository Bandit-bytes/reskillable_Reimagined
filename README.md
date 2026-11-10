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

📖 **[Full guide](docs/GUIDE.md)** — skills, perks, configs, commands, and customization.

## This branch

| | |
|---|---|
| Minecraft | **1.21.1** |
| Loader | Fabric |
| Java | 21 |

**Required:** Fabric Loader 0.19.3+, Fabric API

**Built-in compatibility:** Spell Engine, Jade, EMI

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
