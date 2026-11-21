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

`config/ftbquests/quests/chapters/bettacraft.snbt` ist das Kapitel **Bettacraft**: seit 1.36.0 96 Quests (vorher 33)
in Zweigen vom ersten Becken aus:

1. **Das erste Becken:** bauen, füllen, Heizer, Filter und Licht.
2. **Der Betta:** einen fangen, einsetzen, Pellets, der Futterautomat, abwechslungsreiches Futter, der Spiegelstab, die
   Lupe, einen kranken Betta gesund pflegen (und, optional, die Lehre von zwei Männchen).
3. **Wasserpflege und Zucht:** pH und TDS, Wasserwechsel, 26 bis 28 °C, der Außenfilter, die Osmose; Schaumnest bis zur
   fünften Generation.
4. **Pflanzen und Innenraum:** Bodengrund, Pflanzen, CO2, Schwimmpflanzen, Dekoration, Dünger, Algen.
5. **Lebendfutter:** Sumpfwasser, Mückenlarven, Larvenbottich und Brutanlage, Kulturen, die Lebendfutterpumpe.
6. **Beifische:** der Fischbeutel, Schwarmfische, Würfe der Lebendgebärenden und die Guppy-Linien, Buntbarsche,
   Kugelfische, Barben und Schmerlen, Guramis; Zucht von Garnelen, Schnecken und Krebsen; Frösche.
7. **Das Riff:** Meerwasser, Abschäumer, Korallen, Dosierung (auch von Dünger), Clownfische, Seepferdchen und ihre Jungen.
8. **Terrarien und Paludarien:** Terrarienbox und Geräte, Geckos und ihre Gelege, das Paludarium, Schildkröten und ihre
   Gelege, Meeresschildkröten, Minecraft-Haustiere.
9. **Große Becken und Gäste:** 64 Blöcke, Futtermaschine, Monitore, großer Kescher und Transportwanne, ein großes Tier;
   Axolotl und Tiere anderer Mods, Mobs im Glas.
10. **Technik:** der Unterschrank, Kühler, Belüftung, Schwammfilter, LED-Licht und Tageslicht-Zeitschaltung, der
    Leistungsregler, Rohre, Edelstahl, Durchführungen, die Wasserwechsel-Steuerung, das Nachfüllen, der Multiblock-Filter
    mit Filterleitungen, Anschlüssen und Rohrverkleidungen, ein Filter für zwei Becken.
11. **Optional:** mit eingeschalteter Stromoption Energie im Becken und die Kurbel; mit Create der Antriebsadapter und ein
    reisendes Becken.

Die Aufgaben sind Bettacraft-[Fortschritte](../progress.md) und -Gegenstände, die Belohnungen etwas Erfahrung und kleine
Gegenstände: Pflanzenstecklinge, Futter, ein Ersatz-Testkit, Filtermedien, Salz, ein paar Glasrohre. Die Texte sind
Übersetzungsschlüssel von Bettacraft (`quest.bettacraft.*`), also kommt jede Sprache der Mod auch in die Quests (Englisch
und Deutsch). Die Datei nach `config/ftbquests/quests/chapters/` kopieren und `/ftbquests reload` ausführen oder neu
starten: Das Kapitel erscheint in der Standardgruppe; verschieben, Belohnungen ändern oder Quests ergänzen geht im
Quest-Editor wie bei jedem Kapitel. FTB Quests verschiebt die Texte beim ersten Speichern in seine `lang/en_us.snbt`; dort
bleiben es Übersetzungsschlüssel.

**Aktualisieren:** IDs, Aufgaben und Abhängigkeiten der Quests ändern sich nie; eine neuere Fassung der Datei ersetzt also
die alten Quests, statt sie doppelt anzulegen, und jeder Spieler behält seinen Fortschritt. Fortschritte, die ein Spieler
schon hat, erfüllen die Fortschritts-Aufgaben neuer Quests von selbst (FTB Quests prüft sie alle paar Ticks, sobald die
Quest beginnen darf).

**Abgeschaltete Features:** Jede Quest trägt FTB-Quests-Tags für die [Feature-Schalter](../settings.md), die sie braucht
(`bettacraft_feature:<Pfad>`, auch die der Quests davor), `bettacraft_mod:create` und `bettacraft_needs:power`. Das
Skript `kubejs/server_scripts/bettacraft_quests.js` (KubeJS) blendet Quests aus, deren Schalter aus sind: Sie werden
unsichtbar und optional, blockieren das Kapitel also nie, und kommen zurück, wenn der Schalter wieder an ist (es läuft beim
Serverstart, beim Einloggen eines Spielers und beim Neuladen der Configs; in die Quest-Datei schreibt es nichts). Ohne
KubeJS bleiben sie sichtbar; im Editor ausblenden. Eigene Quests können dieselben Tags nutzen.

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
Seit 1.36.0 meldet `FeaturesChangedEvent` (`getServer()`), dass die Configs bei laufendem Server neu geladen wurden, und
`BettacraftApi.featureEnabled(path)` und `questTagsOn(tags)` sagen, ob ein Feature-Schalter (mit seinen Eltern) bzw. die
Tags einer Quest an sind; das Quest-Skript oben nutzt sie.

## Wegpunkte auf der Karte

Mit **JourneyMap**, **Xaero's Minimap** (und seiner World Map) oder **FTB Chunks** setzt ein Alarm eines Monitors einen roten
Wegpunkt an sein Becken, für den Besitzer und die eingetragenen Nutzer, siehe [Monitor](../equipment/monitor.md) und den
[Nutzer-Tab](../aquarium/menu.md#nutzer) (`features.compat.journeymap`, Client: `map.showAlarmWaypoints` und ein Schalter je
Karten-Mod, `map.journeyMap`, `map.xaerosMinimap`, `map.ftbChunks`; ein Pack mit mehreren Karten-Mods kann einen anlassen).
