// Bettacraft events for KubeJS (KubeJS 7 / NeoForge 1.21.1), an example for modpack makers.
// Copy it to kubejs/server_scripts/ and change what the handlers do. Bettacraft posts these events on the NeoForge
// event bus (server side); the pack switch features.modpackEvents turns them off. Every event has getLevel(),
// getTankPos() (the controller block), getTankId() and getTankName() (the tank sign, "" without one).

const $AnimalBornEvent = Java.loadClass('org.overengineer.bettacraft.api.event.AnimalBornEvent')
const $AnimalDiedEvent = Java.loadClass('org.overengineer.bettacraft.api.event.AnimalDiedEvent')
const $TankAlarmEvent = Java.loadClass('org.overengineer.bettacraft.api.event.TankAlarmEvent')
// read-only helpers: isTank(level, pos), tankId, tankName, tankController, tankValues(level, pos) -> {temperature: 26.0, ...}
const $BettacraftApi = Java.loadClass('org.overengineer.bettacraft.api.BettacraftApi')

// Young born or hatched in a tank: getSpecies() (entity id such as bettacraft:betta, or a critter id such as
// bettacraft:cherry_shrimp with isCritter()), getCount(), isHatched() (eggs / larvae) and isForeign() (an animal of
// another mod bred by its Bettacraft profile).
NativeEvents.onEvent($AnimalBornEvent, event => {
  console.info(`[Bettacraft] ${event.getCount()} x ${event.getSpecies()} born in tank ${event.getTankName()} at ${event.getTankPos()}`)
})

// An animal of a tank died: getSpecies(), getEntity() (null for shrimp and snails), getCount(), getCause() (hunger,
// temperature, water_quality, salinity, fight, predator, eaten, player, ...) and getKiller().
NativeEvents.onEvent($AnimalDiedEvent, event => {
  console.info(`[Bettacraft] ${event.getSpecies()} died of ${event.getCause()} in tank ${event.getTankName()} at ${event.getTankPos()}`)
})

// An alarm tile of a monitor went off (isActive() true) or cleared: getValue() (temperature, water_quality, ph, ...),
// getCurrent(), getLimit(), isBelow() and getMonitorPos().
NativeEvents.onEvent($TankAlarmEvent, event => {
  if (!event.isActive()) return
  const values = $BettacraftApi.tankValues(event.getLevel(), event.getTankPos())
  event.getLevel().getServer().getPlayerList().broadcastSystemMessage(
    Text.red(`Tank alarm: ${event.getTankName() || 'a tank'} ${event.getValue()} = ${event.getCurrent().toFixed(1)} (temperature ${values.get('temperature')})`), false)
})
