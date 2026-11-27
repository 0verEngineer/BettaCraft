---
navigation:
  title: Aquarium-Menü
  icon: minecraft:name_tag
  parent: aquarium.md
  position: 70
---

# Aquarium-Menü, Namensschild und Sperre

**Rechtsklicke** ein Aquarium mit leerer Hand, um sein Menü zu öffnen. (**Schleichen + Rechtsklick** betritt stattdessen
den [Innenraum](../interior.md).)

Das Menü **wächst mit dem Bildschirm**: Wo die GUI-Skalierung Platz drumherum lässt (ein großes Fenster oder eine kleinere
GUI-Skalierung), wird es breiter und höher, mit Tab-Beschriftungen in voller Größe, längeren Listen und längeren Zeilen; ohne
Platz drumherum (GUI-Skalierung 4 bei 1280 × 960) bleibt es kompakt. Rezept-Anzeigen (JEI, REI, EMI) halten Abstand. Text, der trotzdem nicht passt,
endet mit „...“ und steht vollständig da, wenn die Maus darauf liegt.

## Übersicht

![Der Übersicht-Tab](../images/menu_overview.png)

* **Name** (bis 32 Zeichen) und **Namensschild**: an oder aus, seine Position (oben oder unten, links, Mitte oder
  rechts) und die **Vorderseite** des Aquariums, auf der es sitzt. Das Schild ist schwarz mit weißer Schrift und 0,12 Blöcke
  hoch (jeder Spieler kann es mit der Client-Config `render.signScale` größer oder kleiner stellen, 0,5 bis 2); lange
  Namen bekommen eine kleinere Schrift. Der Name steht auch bei ausgeschaltetem Schild im Menü und als Überschrift in Jade, The One Probe und WTHIT.
* **Sperre**: Wer das Aquarium gesetzt hat, ist sein Besitzer. Ist es gesperrt, dürfen nur Besitzer, seine
  Pfleger (Nutzer-Tab, unten) und Operatoren es ändern (Wasser, Tiere, Einstellungen, Innenraum); ansehen dürfen es alle anderen.
* **Bruchschutz** (der Knopf zwischen Name und Sperre; nur Besitzer und Operatoren, bei neuen Aquarien aus): solange er an
  ist, kann **niemand das Aquarium abbauen**, auch der Besitzer nicht und auch nicht im Kreativmodus: das Glas, den
  Deckel, das Licht und die Futterautomaten darauf, den Klemm-Futterautomaten an der Scheibe und den Schrank darunter.
  Wer es versucht, bekommt den Hinweis „Bruchschutz ist an“. Explosionen und Kolben lassen es ebenfalls in Ruhe
  (Server-Config `tank.protectFromExplosionsAndPistons`, standardmäßig an). Schalter für die ganze Funktion:
  `features.equipment.breakProtection`.
* **A** (rechts neben der Sperre; nur Besitzer und Operatoren): welche [angestellten Aquaristen](../getting_started/hired_aquarist.md)
  an diesem Aquarium arbeiten dürfen: **A** der des Besitzers (und seines Teams, mit FTB Teams), **A+** die aller Spieler
  (freigegeben für Freunde, die ihren Aquaristen teilen), **A-** keiner. Ein gesperrtes Aquarium fasst kein Aquarist an.
  Unsichtbar, solange `features.aquaristNpc.enabled` aus ist.
* Größe, Wassermenge, Wasser- und Umgebungstemperatur, die **Zieltemperatur** (– / +).
* Heizstäbe (oder Kühler, wenn das Ziel unter der Umgebung liegt), Futterautomat, Licht und Filter (ein **-**, wo keins
  eingebaut ist), der gespeicherte [Strom](../equipment/power.md) und was die Geräte ziehen (nur wenn der Server Strom
  verlangt; mit der Maus darauf siehst du, welche Gruppen Strom haben), die Zahl der Tiere
  und der Knopf zum Betreten des Innenraums (sein Tooltip zeigt den Innenraum: Maßstab und Größe, Blöcke in der Miniatur,
  Bodengrund, Heizer und Filter darin). Die letzten Zeilen warnen vor mäßigem Wasser, fehlendem Strom oder einem **zu
  kleinen Schwarm**: „Schwarm zu klein: Neonsalmler: 3 von 6“ nennt die Fische und wie viele es sein sollten (mit der Maus
  darauf alle zu kleinen Schwärme). Ein höheres Menü zeigt jede Warnung, eine pro Zeile.
* Das **Zahnrad** rechts neben dem Innenraum-Knopf öffnet die **Client-Einstellungen** von Bettacraft (dieselben wie in
  der Config-Datei `bettacraft-client.toml` und in der Mod-Liste): darunter **Alarm-Wegpunkte aller Becken**
  (`map.showAlarmWaypoints`) und welche Karten-Mods sie zeigen (`map.journeyMap`, `map.xaerosMinimap`, `map.ftbChunks`).

## Tiere

Die Liste aller Tiere mit Name, Typ, Geschlecht und Stufe (kranke rot, Fische eines zu kleinen Schwarms orange; ein Name,
der nicht passt, steht unter der Maus vollständig da). Zum ausgewählten Fisch: Gesundheits- und Satt-Balken,
Fortschritt der Stufe, der Genotyp (Schwanzform, Körper, Flossen, Muster; alle neun Loci im Tooltip), sobald er mit der
[Lupe](../equipment/magnifier.md) analysiert ist (Knopf **Analysieren**), die Generation, ein Namensfeld und die Knöpfe
zum Entnehmen mit Eimer oder Fischtüte. Ein Zwergkrallenfrosch zeigt Gesundheit,
Futter und Luft.

![Der Tiere-Tab](../images/menu_inhabitants.png)

Der Knopf **Kleintiere** wechselt zu den [Garnelen, Krebsen und Schnecken](../invertebrates.md): jede Sorte (Art und
Farbe) mit Anzahl, Geschlechtern, Jungtieren, Weibchen mit Eiern und Güte; für das ganze Aquarium der Besatz, die Algen
am Glas, wie viel die Putzkolonne frisst, das Futter und die Verstecke. Ein Klick auf eine Sorte klappt die Liste ihrer
Tiere auf: jedes einzelne mit Geschlecht, Alter, Güte und Genen (sobald analysiert); wähle eins, um es genau zu sehen,
es zu **Analysieren** oder genau dieses zu **Entnehmen**. Mit einer gewählten Sorte holt **Entnehmen** das Tier mit der
besten Güte (Umschalt: das schlechteste, Strg: eine volle Tüte bzw. ein voller Eimer auf einmal) des gewählten Geschlechts (Knopf
unter der Liste: beide, Männchen, Weibchen; es ist das Geschlecht, nicht die Menge) in eine Fischtüte oder einen Eimer (der zweite Knopf). **Gelege weg**
nimmt die Gelege der Apfelschnecken vom Glas.

![Die Seite Kleintiere](../images/menu_critters.png)

## Zucht

Ein Knopf oben schaltet zwischen den **Tiergruppen** um, die im Aquarium sind: Bettas, Beifische, Kleintiere (Garnelen, Schnecken,
Krebse mit den Amano-Larven), Frösche, Reptilien und Amphibien, Tiere anderer Mods. Zu jeder Art die Anzahlen, die Bedingungen
mit ✔ und ✘ (das ✘ ist, was fehlt) und was gerade läuft (tragende Weibchen, Gelege, Larven, Laich, Kaulquappen, die Brut).
Bettas: die Warnungen (unter „Ein Schwarm Beifische ist zu klein“ jeder solche Schwarm mit seinen Fischen und der Mindestzahl),
die **Zuchtbedingungen** (solange es kein Nest gibt) und darunter das **Schaumnest**; mit der Maus auf „Jungfische frei in x Tagen,
Pflege n %“ siehst du, woraus sich die Pflege zusammensetzt, was fehlt und wie viele Larven voraussichtlich frei schwimmen; die
Jungfische; die Eltern und die erwarteten Nachkommen. Eine Warnung erscheint, solange Eier oder Jungtiere da sind und ein Motorfilter
keinen [Ansaugschutz-Schwamm](../equipment/intake_sponge.md) hat (mit der Maus darauf siehst du, wie man einen anbringt). Siehe [Zucht](../bettas/breeding.md).

## Wasser

![Der Wasser-Tab](../images/menu_water.png)

Zwei Gruppen mit je einer Überschrift. **Werte:** das Volumen, TDS und pH, die Verdunstung (und die offenen oberen
Blöcke) und der [Sauerstoff](oxygen.md) mit Balken (der weiße Strich ist die Sättigung; mit der Maus darauf siehst du, wohin er
strebt und warum: Austausch und Verbrauch). **Qualität:** die [Wasserqualität](water_quality.md) mit einem Balken (die rote
Marke zeigt, ab wann die Tiere leiden, die gelbe, ab wann das Wasser mäßig wird; mit der Maus darauf siehst du die Stufen), wie
schnell sie sinkt und warum (Tiere pro Wasserblock), die Filter (mit der Maus auf der Zeile siehst du, wie viele Wasserblöcke sie
schaffen und welchen Anteil des Drecks sie entfernen), das Nitrat und das letzte Frischwasser. Heizer und Kühler stehen im Reiter
Technik, der Dünger im Reiter Pflanzen, der gespeicherte [Strom](../equipment/power.md) in der Übersicht. Ein Reiter mit mehr
Zeilen, als hineinpassen, scrollt (Mausrad, Balken am rechten Rand).

## Technik

![Der Reiter Technik](../images/menu_devices.png)

Vier Gruppen mit je einer Überschrift. **Belüftung:** die [Luftpumpen](../equipment/air_pump.md), [Ausströmer](../equipment/air_stone.md) und
[Schwammfilter](../equipment/sponge_filter.md), wie viel CO2 die Belüftung austreibt. **Filter:** die Motorfilter nach Art (innen, Eck,
außen, Unterschrank) und wie viele davon einen [Ansaugschutz-Schwamm](../equipment/intake_sponge.md) haben; die Warnung für die ohne zeigt mit
der Maus, wie man einen anbringt. **Temperatur:** die Heizer und Kühler. **Licht:** die Lichter des Aquariums mit der Lichtstärke und **Dimmer** und
**Tageslicht-Timer** mit ihren Knöpfen (siehe [Licht](../equipment/light.md)). Der [Sauerstoff](oxygen.md) ist ein Wasserwert und
steht im Reiter Wasser.

## Pflanzen

![Der Pflanzen-Tab](../images/menu_plants.png)

In Gruppen mit je einer Überschrift, nur die, die das Aquarium hat: die [Schwimmpflanzen](../plants/floating_plants.md) (Anzahl je Art,
Bedeckung, Licht, Wachstum, die Warnung bei dichter Decke, der Knopf **Ausdünnen**), die **Wasserpflanzen** des Innenraums, die
**Korallen** (Salzwasser) und die **Terrarienpflanzen** (Terrarium, Paludarium). Jede Gruppe zeigt, was sie für das Wasser tut:
Wasserqualität pro Tag, Nitrat, Sauerstoff und CO2. Auch der Dünger steht hier. Am Ende klappt eine Liste der Pflanzen des Innenraums
(Art und Anzahl) auf.

## Nutzer

![Der Nutzer-Tab](../images/menu_users.png)

Der Nutzer-Tab (er hat in 1.43.0 den Innenraum-Tab ersetzt) zeigt, wer an diesem Aquarium was darf:

| Rolle | Darf |
|---|---|
| **Besitzer** | wer es gebaut hat: alles, auch die Nutzer, die Sperre und den Bruchschutz |
| **Pfleger** | alles, was der Besitzer darf: Pflege, Einstellungen, Tiere, Betreten, die Nutzer und die Sperre |
| **Betrachter** | das Menü öffnen und ansehen; ändert nichts und kann nicht ins Aquarium |
| **Nicht eingetragen** | alle anderen: dürfen es ändern, solange es offen ist, gesperrt nur ansehen |

Besitzer und Pfleger tippen den Namen eines Spielers, der online ist (**Tab** ergänzt ihn, Umschalt + Tab den davor), wählen **als Pfleger** oder **als Betrachter** und
drücken **Eintragen**; ein ausgewählter Nutzer bekommt eine andere Rolle oder wird entfernt (jeder Nutzer kann sich selbst
austragen). Spieler in der FTB-Teams-Gruppe des Besitzers zählen als Pfleger. Der Knopf **Andere** ist die Sperre.
Operatoren dürfen immer alles. Config-Schalter: `features.equipment.tankUsers` (aus: die Liste wird ignoriert, nur
Besitzer und Sperre zählen).

**Alarm-Wegpunkte:** Löst ein [Monitor-Alarm](../equipment/monitor.md) aus (oder warnt der
[angestellte Aquarianer](../getting_started/hired_aquarist.md)), bekommen der Besitzer und die eingetragenen Nutzer einen
roten Wegpunkt am Aquarium in ihrer Karten-Mod (JourneyMap, Xaero's Minimap und World Map, FTB Chunks), nie Spieler, die
nur in der Nähe sind. **Mein Wegpunkt** schaltet deinen eigenen für dieses Aquarium (für den Besitzer standardmäßig an,
für alle anderen Nutzer aus); **Wegpunkte für alle** (Besitzer und Pfleger) schaltet sie für alle ab. Das Zahnrad in der
Übersicht schaltet sie für alle Aquarien und je Karten-Mod.

## Log

Was im Aquarium passiert ist: Fütterungen, Eier, Kämpfe, Krankheiten, Todesfälle und ihre Ursachen, mit dem Ingame-Tag.
Es hält 100 Einträge und lässt sich filtern (alles, Zucht, Gesundheit, Futter).
