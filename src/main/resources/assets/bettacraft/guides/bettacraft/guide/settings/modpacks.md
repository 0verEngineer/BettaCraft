---
navigation:
  title: "Modpacks: quests and scripts"
  icon: minecraft:writable_book
  parent: settings.md
  position: 40
---

# Modpacks: quests and scripts

Since 1.25.0 Bettacraft brings a quest chapter for **FTB Quests** and events for **KubeJS** scripts. Both are for
modpack makers: they are not in the mod jar but in the file `bettacraft-modpack-<version>.zip` of every release (in the
repository the folder `modpack/`). Its folders are those of an instance: copy `config/` and `kubejs/` into it.

## Presets

The quickest way to tune Bettacraft for a pack is one key in `config/bettacraft-common.toml`: `preset = "SIMPLE"`,
`"REALISTIC"` or `"DECOR"`. It sets the feature switches and the game values of every world's server config at once;
values the pack changes itself still win, and `presetKeep` keeps single defaults. The table is on the
[settings page](../settings.md) under Feature switches.

## FTB Quests chapter

`config/ftbquests/quests/chapters/bettacraft.snbt` is the chapter **Bettacraft**, 33 quests from the first aquarium to
the reef and the terrarium:

1. **The first tank:** build it, fill it, a heater, a filter and a light.
2. **The betta:** catch one in a swamp, put it in, pellets, the feeder.
3. **Water care:** pH and TDS, a water change, 26 to 28 °C, the external filter, the reverse osmosis unit.
4. **Breeding:** bubble nest, spawning, food for the fry, free swimming, grown up, a named variety, the fifth
   generation.
5. **Plants:** into the interior, substrate, the first plant, CO2.
6. **The reef:** sea water, protein skimmer and live rock, the first coral, the dosing pump.
7. **Big tanks:** 64 blocks, the food maker, the monitors.
8. **The terrarium:** the terrarium box, basking lamp, mister and thermo-hygrometer, a leopard gecko.

The tasks are Bettacraft advancements and items, the rewards a little experience. The texts are translation keys of
Bettacraft (`quest.bettacraft.*`), so every language of the mod is in the quests too (English and German). Copy the
file into `config/ftbquests/quests/chapters/` and run `/ftbquests reload` or restart: the chapter appears in the default
group; move it, change rewards or add quests in the quest editor as with any chapter. FTB Quests moves the texts into
its `lang/en_us.snbt` the first time it saves; they stay translation keys there. The ids of the quests never change, so
a newer version of the file replaces the old quests instead of adding them twice.

Heracles (the other quest mod) is not supported.

## KubeJS events

Bettacraft posts three events on the NeoForge event bus (server side). KubeJS 7 listens to them with
`NativeEvents.onEvent`; other mods can listen to them like to any NeoForge event. `kubejs/server_scripts/bettacraft_events.js`
is an example with all three. Every event names the tank: `getLevel()`, `getTankPos()` (its controller block),
`getTankId()` (stays the same when the tank grows, shrinks or travels) and `getTankName()` (its sign, "" without one).

| Event (`org.overengineer.bettacraft.api.event.`) | When | Also |
|---|---|---|
| `AnimalBornEvent` | young were born or hatched in a tank: betta fry swim free, tankmates are born or leave their clutch, shrimp, snails and crayfish are born, axolotl, turtle and herp eggs hatch, the mobs in glass and the animals of other mods with a [profile](foreign_profiles.md) breed | `getSpecies()` (entity id, for shrimp and snails the critter id), `getCount()`, `isHatched()`, `isForeign()`, `isCritter()` |
| `AnimalDiedEvent` | an animal of a tank died: of our care (hunger, temperature, water ...), eaten, in a fight, killed | `getSpecies()`, `getEntity()` (null for shrimp and snails), `getCount()`, `getCause()` (`hunger`, `temperature`, `water_quality`, `salinity`, `fight`, `predator`, `eaten`, `player`, ...), `getKiller()` |
| `TankAlarmEvent` | an [alarm tile](../equipment/monitor.md) of a monitor went off (`isActive()`) or cleared | `getValue()` (`temperature`, `water_quality`, `ph`, ...), `getCurrent()`, `getLimit()`, `isBelow()`, `getMonitorPos()` |

```js
const $AnimalBornEvent = Java.loadClass('org.overengineer.bettacraft.api.event.AnimalBornEvent')
NativeEvents.onEvent($AnimalBornEvent, event => {
  console.info(`${event.getCount()} x ${event.getSpecies()} born in ${event.getTankName()}`)
})
```

For reading a tank, `org.overengineer.bettacraft.api.BettacraftApi` has `isTank(level, pos)`, `tankId`, `tankName`,
`tankController` and `tankValues(level, pos)` (the values of the monitors by id: `temperature`, `water_quality`, `ph`,
`tds`, `salinity`, ...); `pos` can be any block of the tank. The switch `features.compat.modpackEvents` turns the events off.

## Map waypoints

With **JourneyMap** an alarm of a monitor puts a red waypoint at its tank, see [Monitors](../equipment/monitor.md)
(`features.compat.journeymap`, `map.showAlarmWaypoints`). Xaero's Minimap is not supported.
