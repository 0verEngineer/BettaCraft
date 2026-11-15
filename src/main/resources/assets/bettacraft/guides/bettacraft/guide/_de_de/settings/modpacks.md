---
navigation:
  title: "Modpacks: Quests und Skripte"
  icon: minecraft:writable_book
  parent: settings.md
  position: 40
---

# Modpacks: Quests und Skripte

Seit 1.25.0 bringt Bettacraft ein Quest-Kapitel für **FTB Quests** und Events für **KubeJS**-Skripte mit. Beides ist für
Modpack-Macher: Es steckt nicht im Mod-Jar, sondern in der Datei `bettacraft-modpack-<Version>.zip` jedes Releases (im
Repository der Ordner `modpack/`). Ihre Ordner sind die einer Instanz: `config/` und `kubejs/` dort hineinkopieren.

## Voreinstellungen

Am schnellsten passt man Bettacraft mit einem Schlüssel in `config/bettacraft-common.toml` an ein Pack an:
`preset = "SIMPLE"`, `"REALISTIC"` oder `"DECOR"`. Er setzt die Feature-Schalter und die Spielwerte der Server-Config
jeder Welt auf einmal; Werte, die das Pack selbst ändert, gewinnen weiterhin, und `presetKeep` behält einzelne Standards.
Die Tabelle steht auf der [Einstellungsseite](../settings.md) unter Feature-Schalter.

## Kapitel für FTB Quests

`config/ftbquests/quests/chapters/bettacraft.snbt` ist das Kapitel **Bettacraft**, 33 Quests vom ersten Aquarium bis zum
Riff und zum Terrarium:

1. **Das erste Becken:** bauen, füllen, Heizer, Filter und Licht.
2. **Der Betta:** einen im Sumpf fangen, einsetzen, Pellets, der Futterautomat.
3. **Wasserpflege:** pH und TDS, ein Wasserwechsel, 26 bis 28 °C, der Außenfilter, die Osmoseanlage.
4. **Zucht:** Schaumnest, Laichen, Futter für die Jungfische, Freischwimmer, erwachsen, eine benannte Zuchtform, die
   fünfte Generation.
5. **Pflanzen:** in den Innenraum, Bodengrund, die erste Pflanze, CO2.
6. **Das Riff:** Meerwasser, Eiweißabschäumer und Lebendgestein, die erste Koralle, die Dosierpumpe.
7. **Große Becken:** 64 Blöcke, die Futtermaschine, die Monitore.
8. **Das Terrarium:** die Terrarienbox, Wärmespot, Sprühanlage und Thermo-Hygrometer, ein Leopardgecko.

Die Aufgaben sind Bettacraft-Fortschritte und -Gegenstände, die Belohnungen etwas Erfahrung. Die Texte sind
Übersetzungsschlüssel von Bettacraft (`quest.bettacraft.*`), also kommt jede Sprache der Mod auch in die Quests (Englisch
und Deutsch). Die Datei nach `config/ftbquests/quests/chapters/` kopieren und `/ftbquests reload` ausführen oder neu
starten: Das Kapitel erscheint in der Standardgruppe; verschieben, Belohnungen ändern oder Quests ergänzen geht im
Quest-Editor wie bei jedem Kapitel. FTB Quests verschiebt die Texte beim ersten Speichern in seine `lang/en_us.snbt`; dort
bleiben es Übersetzungsschlüssel. Die IDs der Quests ändern sich nie, eine neuere Fassung der Datei ersetzt also die alten
Quests, statt sie doppelt anzulegen.

Heracles (die andere Quest-Mod) wird nicht unterstützt.

## Events für KubeJS

Bettacraft meldet drei Events auf dem NeoForge-Event-Bus (Serverseite). KubeJS 7 hört sie mit `NativeEvents.onEvent`;
andere Mods hören sie wie jedes NeoForge-Event. `kubejs/server_scripts/bettacraft_events.js` ist ein Beispiel mit allen
dreien. Jedes Event nennt das Becken: `getLevel()`, `getTankPos()` (sein Controller-Block), `getTankId()` (bleibt gleich,
wenn das Becken wächst, schrumpft oder reist) und `getTankName()` (sein Schild, "" ohne).

| Event (`org.overengineer.bettacraft.api.event.`) | Wann | Dazu |
|---|---|---|
| `AnimalBornEvent` | Junge wurden in einem Becken geboren oder sind geschlüpft: Betta-Jungfische schwimmen frei, Beifische werden geboren oder verlassen ihr Gelege, Garnelen, Schnecken und Krebse werden geboren, Axolotl-, Schildkröten- und Herp-Eier schlüpfen, die Mobs im Glas und die Tiere anderer Mods mit [Profil](foreign_profiles.md) vermehren sich | `getSpecies()` (Entity-ID, bei Garnelen und Schnecken die Kleintier-ID), `getCount()`, `isHatched()`, `isForeign()`, `isCritter()` |
| `AnimalDiedEvent` | ein Tier eines Beckens ist gestorben: an unserer Pflege (Hunger, Temperatur, Wasser ...), gefressen, im Kampf, getötet | `getSpecies()`, `getEntity()` (null bei Garnelen und Schnecken), `getCount()`, `getCause()` (`hunger`, `temperature`, `water_quality`, `salinity`, `fight`, `predator`, `eaten`, `player`, ...), `getKiller()` |
| `TankAlarmEvent` | eine [Alarm-Kachel](../equipment/monitor.md) eines Monitors hat ausgelöst (`isActive()`) oder ist wieder aus | `getValue()` (`temperature`, `water_quality`, `ph`, ...), `getCurrent()`, `getLimit()`, `isBelow()`, `getMonitorPos()` |

```js
const $AnimalBornEvent = Java.loadClass('org.overengineer.bettacraft.api.event.AnimalBornEvent')
NativeEvents.onEvent($AnimalBornEvent, event => {
  console.info(`${event.getCount()} x ${event.getSpecies()} born in ${event.getTankName()}`)
})
```

Zum Lesen eines Beckens hat `org.overengineer.bettacraft.api.BettacraftApi` die Methoden `isTank(level, pos)`, `tankId`,
`tankName`, `tankController` und `tankValues(level, pos)` (die Werte der Monitore nach ID: `temperature`,
`water_quality`, `ph`, `tds`, `salinity`, ...); `pos` darf jeder Block des Beckens sein. Der Schalter
`features.compat.modpackEvents` schaltet die Events ab.

## Wegpunkte auf der Karte

Mit **JourneyMap** setzt ein Alarm eines Monitors einen roten Wegpunkt an sein Becken, siehe [Monitor](../equipment/monitor.md)
(`features.compat.journeymap`, `map.showAlarmWaypoints`). Xaero's Minimap wird nicht unterstützt.
