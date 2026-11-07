# OreSpawn: DangerZone

Creatures, items and things to do from **DangerZone**, the game TheyCallMeDanger built after OreSpawn,
brought to the [OreSpawn port](https://github.com/CoolFreeze23/Orespawn) for NeoForge 1.21.1.

**Version:** 0.1.0 · **Minecraft:** 1.21.1 · **Loader:** NeoForge 21.1+ · **Requires:** OreSpawn 2.0.0-beta.15 and GeckoLib

![The creatures, in game](docs/creatures.png)

## What's in it

| | |
|---|---|
| **Nastysaurus Prime** | A boss with a boss bar that bites hard and calls a Needlenose pack into the fight. Mining and Chaos. |
| **Needlenose** | The Prime's pack hunter. It joins any fight a Prime nearby is having. Mining. |
| **Ant Queen** | A nearly nine-block-wide boss. Her strikes reach eight blocks, and every ant type climbs out beside her. Chaos and windswept hills. |
| **Bloodlust Ant** | The Queen's soldier. Chaos and windswept hills. |
| **Nurse Bug** | A daylight flyer that seeks out hurt players, Girlfriends and Boyfriends and heals them. Forests and several OreSpawn dimensions. |
| **Dimension Staff** | Walks the world's dimensions at the same coordinates. Use for the next one, sneak and use for the one before. You land on the nearest footing, or on a small platform if there is none. |
| **Dragon's Eye Sword** | Every hit blinks you to a safe spot near the target, facing it. |
| **A Freakin' Shrink Ray!!!** | Shrinks anything but a boss to a quarter of its size, with a sixteenth of its bite and a quarter of its armour. Players grow back after a minute; creatures don't. Shrunk creatures drop nothing. |
| **Duct Tape repair** | The port's Duct Tape plus any damaged unstackable item, in a crafting grid, repairs it fully and keeps its enchantments. |
| **Things To Do** | DangerZone's task list as an advancement tab, with its own titles, wherever the item or creature exists in the port. |

## Getting the items

- **Dimension Staff:** a Vortex Eye over two Ender Pearls and two Titanium Ingots, or a rare Ant Queen drop.
- **Dragon's Eye Sword:** two Dragon's Eyes on a stick. Nastysaurus Primes drop the eyes, and so do Dragons, Water Dragons, Spyros and Leonopteryxes.
- **Shrink Ray:** a Ray Gun surrounded by eight Amethyst Shards. Recharge it at an anvil with more shards.

## Faithful, with these differences

- Models, textures and sounds are DangerZone's own, and the models keep their original animation code.
- Health and damage are scaled to the port's numbers, keeping DangerZone's proportions between creatures.
- Summoning has caps, which DangerZone didn't: see `orespawn_dangerzone-common.toml`.
- The Dimension Staff lands you safely, and has a recipe, since Kraken Kings don't exist here yet.
- Amethyst replaces Shrink Berries in the Shrink Ray recipe, and Dragons replace the Dragon's Lair as the source of Dragon's Eyes.
- Real-world political jokes from the original are left out.

## License & Ownership

**DangerZone and OreSpawn, their code, art and sounds, are the work of TheyCallMeDanger and the original
authors. All rights to the originals remain theirs.**

This is an unofficial preservation project. No ownership of the original work is claimed by anyone
involved, and it isn't sold or monetised. It contains DangerZone's textures and sounds, models converted
from its decompiled code, and task text from it. If you are an original author and want anything changed
or removed, open an issue and it will be, immediately.

`mods.toml` license field: **All Rights Reserved** (the originals' rights, not ours).

## Credits

- **TheyCallMeDanger** (Richard H. Clark) and the DangerZone and OreSpawn authors: the game, the mod, and
  everything taken from them.
- The Internet Archive, and the people who archived DangerZone there.
- The NeoForge port team: this add-on and the OreSpawn port.

## Reporting issues

[Issues](https://github.com/CoolFreeze23/orespawn-dangerzone/issues): include the mod version, exact steps,
what you expected and saw, and the log for crashes.

## Building

See `libs/README.md` for the jars to place first, then run `./gradlew build`.
`./gradlew runGameTestServer` runs the tests.
