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

`config/ftbquests/quests/chapters/bettacraft.snbt` is the chapter **Bettacraft**: since 1.36.0 96 quests (33 before) in
branches from the first tank:

1. **The first tank:** build it, fill it, a heater, a filter and a light.
2. **The betta:** catch one, put it in, pellets, the feeder, a varied diet, the mirror stick, the magnifier, nursing a sick
   betta (and, optional, the lesson of two males).
3. **Water care and breeding:** pH and TDS, water changes, 26 to 28 °C, the external filter, reverse osmosis; bubble nest
   to the fifth generation.
4. **Plants and the interior:** substrate, plants, CO2, floating plants, decoration, fertilizer, algae.
5. **Live food:** swamp water, mosquito larvae, larva tub and hatchery, cultures, the live food pump.
6. **Tankmates:** the fish bag, community fish, livebearer litters and the guppy strains, cichlids, puffers, barbs and
   loaches, gouramis; shrimp, snails and crayfish breeding; frogs.
7. **The reef:** sea water, skimmer, corals, dosing (and fertilizer dosing), clownfish, seahorses and their fry.
8. **Terraria and paludaria:** the terrarium box and devices, geckos and their clutches, the paludarium, turtles and
   their clutches, sea turtles, Minecraft pets.
9. **Big tanks and guests:** 64 blocks, food maker, monitors, the large net and transport tub, a big animal; axolotls and
   animals of other mods, mobs in glass.
10. **Technology:** the cabinet, coolers, aeration, sponge filters, LED lights and the daylight timer, the power
    regulator, pipes, stainless steel, bulkheads, the water change controller, the top-off, the multiblock filter with
    filter lines, ports and pipe covers, a filter for two tanks.
11. **Optional:** with a power option on, energy in a tank and the crank; with Create, the drive adapter and a travelling
    tank.

The tasks are Bettacraft [advancements](../progress.md) and items, the rewards some experience and small items: plant
cuttings, food, a spare test kit, filter media, salt, a few glass pipes. The texts are translation keys of Bettacraft
(`quest.bettacraft.*`), so every language of the mod is in the quests too (English and German). Copy the file into
`config/ftbquests/quests/chapters/` and, on Minecraft 1.21.11, also `config/ftbquests/quests/lang/en_us.snbt` of the zip
(FTB Quests 2111 reads the texts only from there; merge its lines into an existing file), then run `/ftbquests reload` or
restart: the chapter appears in the default group; move it, change rewards or add quests in the quest editor as with any
chapter. The texts stay translation keys there.

**Updating:** the ids, tasks and dependencies of the quests never change, so a newer version of the file replaces the old
quests instead of adding them twice, and every player keeps their progress. Advancements a player already has complete
the advancement tasks of new quests by themselves (FTB Quests checks them every few ticks once the quest may start).

**Switched-off features:** every quest has FTB Quests tags for the [feature switches](../settings.md) it needs
(`bettacraft_feature:<path>`, also those of the quests before it), `bettacraft_mod:create` and `bettacraft_needs:power`.
The script `kubejs/server_scripts/bettacraft_quests.js` (KubeJS) hides the quests whose switches are off: they become
invisible and optional, so they never block the chapter, and come back when the switch is on again (it runs when the
server starts, when a player logs in and when the configs are reloaded; it writes nothing into the quest file). Without
KubeJS they stay visible; hide them in the editor. Your own quests can use the same tags.

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
Since 1.36.0 `FeaturesChangedEvent` (`getServer()`) says that the configs were reloaded while the server runs, and
`BettacraftApi.featureEnabled(path)` and `questTagsOn(tags)` answer whether a feature switch (with its parents) or the
tags of a quest are on; the quest script above uses them.

## Map waypoints

With **JourneyMap** an alarm of a monitor puts a red waypoint at its tank, see [Monitors](../equipment/monitor.md)
(`features.compat.journeymap`, `map.showAlarmWaypoints`). Xaero's Minimap is not supported.
