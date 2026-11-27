// Bettacraft quests for KubeJS (KubeJS 8 / NeoForge 26.1.2, KubeJS 7 / 1.21.1) with FTB Quests: hides the quests of
// switched-off features.
// Copy it to kubejs/server_scripts/ next to the chapter config/ftbquests/quests/chapters/bettacraft.json5 (Minecraft 26.1.2;
// FTB Quests of 1.21.x: bettacraft.snbt).
//
// Every quest of the Bettacraft chapter carries FTB Quests tags: bettacraft_feature_<path> for the feature switches of
// config/bettacraft-common.toml it belongs to (features.<path>, lower case, "__" for "."; FTB Quests 26.1 keeps only tags
// of [a-z0-9_]; 1.21.x: bettacraft_feature:<path>), bettacraft_mod_<modid> for another mod it needs (Create)
// and bettacraft_needs_power for quests that only work while a device needs power (power.costs EASY or HARD in the server
// config). BettacraftApi.questTagsOn(tags) says whether all of them are on. A quest that is off becomes invisible and
// optional (it never blocks the chapter); when the switch is on again, it shows again. Nothing is written to the quest
// file: the script runs when the server has started, when a player logs in (after /ftbquests reload) and when the
// Bettacraft configs are reloaded while the server runs. Players who already completed a hidden quest keep it (and see it).
// Your own quests can use the same tags. (KubeJS shares one scope between the server scripts: the names here start with
// $Bcq so they never clash with those of bettacraft_events.js.)

const $BcqQuestFile = Java.loadClass('dev.ftb.mods.ftbquests.quest.ServerQuestFile')
// FTB Quests 26.1 keeps the quest data as JSON5 (de.marhali.json5), older versions as NBT
let $BcqData = null
try { $BcqData = Java.loadClass('de.marhali.json5.Json5Object') } catch (e) { $BcqData = Java.loadClass('net.minecraft.nbt.CompoundTag') }
const $BcqJson5 = String($BcqData).indexOf('json5') >= 0
const $BcqApi = Java.loadClass('org.overengineer.bettacraft.api.BettacraftApi')
const $BcqFeaturesChanged = Java.loadClass('org.overengineer.bettacraft.api.event.FeaturesChangedEvent')

function bettacraftFlag(data, key) {
  if (!$BcqJson5) return data.getBoolean(key)
  return data.has(key) && data.get(key).getAsBoolean()
}

function bettacraftShowQuests(server) {
  let file = null
  try { file = $BcqQuestFile.getInstance() } catch (e) { file = $BcqQuestFile.INSTANCE } // 26.1 / 1.21.1
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
      const data = new $BcqData()
      quest.writeData(data, registries)
      if (bettacraftFlag(data, 'invisible') == !show && bettacraftFlag(data, 'optional') == optional) return
      if ($BcqJson5) {
        data.addProperty('invisible', !show)
        data.addProperty('optional', optional)
      } else {
        data.putBoolean('invisible', !show)
        data.putBoolean('optional', optional)
      }
      quest.readData(data, registries)
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
