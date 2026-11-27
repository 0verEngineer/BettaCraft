// Bettacraft quests for KubeJS (KubeJS 7 / NeoForge 1.21.1) with FTB Quests: hides the quests of switched-off features.
// Copy it to kubejs/server_scripts/ next to the chapter config/ftbquests/quests/chapters/bettacraft.snbt.
//
// Every quest of the Bettacraft chapter carries FTB Quests tags: bettacraft_feature:<path> for the feature switches of
// config/bettacraft-common.toml it belongs to (features.<path>), bettacraft_mod:<modid> for another mod it needs (Create)
// and bettacraft_needs:power for quests that only work while a device needs power (power.costs EASY or HARD in the server
// config). BettacraftApi.questTagsOn(tags) says whether all of them are on. A quest that is off becomes invisible and
// optional (it never blocks the chapter); when the switch is on again, it shows again. Nothing is written to the quest
// file: the script runs when the server has started, when a player logs in (after /ftbquests reload) and when the
// Bettacraft configs are reloaded while the server runs. Players who already completed a hidden quest keep it (and see it).
// Your own quests can use the same tags. (KubeJS shares one scope between the server scripts: the names here start with
// $Bcq so they never clash with those of bettacraft_events.js.)

const $BcqQuestFile = Java.loadClass('dev.ftb.mods.ftbquests.quest.ServerQuestFile')
const $BcqCompoundTag = Java.loadClass('net.minecraft.nbt.CompoundTag')
const $BcqApi = Java.loadClass('org.overengineer.bettacraft.api.BettacraftApi')
const $BcqFeaturesChanged = Java.loadClass('org.overengineer.bettacraft.api.event.FeaturesChangedEvent')

function bettacraftShowQuests(server) {
  const file = $BcqQuestFile.INSTANCE
  if (!file || !server) return 0
  const registries = server.registryAccess()
  let changed = 0, hidden = 0
  file.getAllChapters().forEach(chapter => {
    chapter.getQuests().forEach(quest => {
      const tags = quest.getTags()
      let ours = false
      tags.forEach(t => { if (String(t).startsWith('bettacraft_')) ours = true })
      if (!ours) return
      const show = $BcqApi.questTagsOn(tags)
      const optional = !show || tags.contains($BcqApi.TAG_OPTIONAL)
      if (!show) hidden++
      // the quest's own data, changed the way the quest editor changes it
      const nbt = new $BcqCompoundTag()
      quest.writeData(nbt, registries)
      if (nbt.getBoolean('invisible') == !show && nbt.getBoolean('optional') == optional) return
      nbt.putBoolean('invisible', !show)
      nbt.putBoolean('optional', optional)
      quest.readData(nbt, registries)
      quest.clearCachedData()
      if (server.getPlayerCount() > 0) $BcqApi.sendQuestEdit(quest) // to the players online, like the quest editor
      changed++
    })
  })
  if (changed > 0) console.info(`[Bettacraft] quests: ${changed} changed, ${hidden} hidden (switched-off features)`)
  return changed
}

ServerEvents.loaded(event => bettacraftShowQuests(event.server))
PlayerEvents.loggedIn(event => bettacraftShowQuests(event.server))
// the configs were reloaded (needs features.compat.modpackEvents, on by default)
NativeEvents.onEvent($BcqFeaturesChanged, event => bettacraftShowQuests(event.getServer()))
