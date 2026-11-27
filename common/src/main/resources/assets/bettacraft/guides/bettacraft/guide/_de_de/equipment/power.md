---
navigation:
  title: Strom
  icon: minecraft:redstone
  parent: equipment.md
  position: 90
feature: power.enabled
---

# Strom

Standardmäßig braucht **keine Ausrüstung Strom**. Ein Server wählt mit **einer Einstellung**, `power.costs`, was die
Geräte kosten:

| `power.costs` | Was die Geräte nehmen |
|---|---|
| `OFF` (Standard) | nichts: jedes Gerät läuft einfach |
| `EASY` | ein Drittel der FE-Werte unten, bei allen Geräten |
| `HARD` | die FE-Werte wie eingestellt (der Verbrauch älterer Versionen) |

Strom heißt **Forge Energy (FE)** und kommt von jeder Mod, die FE erzeugt, von der Drehung von Create oder von der
[Kurbel](crank.md).

## Ein Speicher pro Becken

Jedes Aquarium hat **einen Energiespeicher** für alle seine Geräte. Alles, was das Becken lädt, geht dort hinein, und
jedes Gerät des Beckens zieht von dort:

* ein Kabel oder Generator an **einem beliebigen Beckenblock**, an einem angeschlossenen **Außenfilter**, an einem
  **Unterschrank** unter dem Aquarium oder an einem **Licht** darauf;
* die [Kurbel](crank.md) an einem **Unterschrank** unter dem Becken oder an einem angeschlossenen **Außenfilter** (ohne
  Strom-Mod): etwa **20 Sekunden** Halten laden einen Tag eines mittleren Beckens auf `EASY`;
* mit Create ein [Antriebsadapter](drive_adapter.md) am Becken oder eine Welle am Schrank darunter.

Der Speicher **wächst mit dem Becken**: 80.000 FE je Wasserblock (`power.storePerWaterBlock`) und 20.000 FE je
Schrankblock darunter (`power.storePerCabinet`; daran angebaute Schränke zählen mit). Ein voller Speicher betreibt ein
2 x 2 x 2-Becken mit Filter, Licht und heizendem Heizer **einen Spieltag** auf `EASY` (ein Drittel Tag auf `HARD`); ein
größeres Becken hat einen größeren Speicher und braucht mehr.

## Was die Geräte nehmen

Die Werte gelten für `HARD`; `EASY` nimmt jeweils ein Drittel.

* Heizer und Kühler: 8 FE pro Tick für jeden Wasserblock, den sie heizen oder kühlen (nur die Sorte, die gerade arbeitet,
  und nur bis zum Wasser im Aquarium); Heizmatten 4 FE pro Tick beim Heizen.
* Jeder Filter 10 FE pro Tick, jeder leuchtende Lichtblock 4 FE pro Tick, Luftpumpen 2 FE pro Tick je Ausströmer oder
  Schwammfilter, Sprühanlagen 6 FE pro Tick beim Sprühen.
* Ohne genug FE bleibt die Gruppe stehen: Heizer und Kühler ändern die Temperatur nicht mehr, Filter reinigen nicht mehr,
  Lichter auf dem Aquarium gehen aus. Filter bekommen den Strom zuerst, dann Luftpumpen, Heizer und Kühler, Lichter und
  Sprühanlagen. Jedes Gerät braucht bei kleinerem [Leistungsregler](regulator.md) weniger FE.

Geräte ohne Becken (Wasserpumpen, Multiblock-Filter, Wasserwechsel-Steuerung, Osmoseanlage, Futter- und Düngermaschine,
CO2-Kompressor, Schmelztiegel) haben einen eigenen kleinen Puffer und folgen demselben `power.costs`; die meisten haben
eine eigene Feder für die Kurbel.

Die **Übersicht** des Beckenmenüs zeigt das gespeicherte FE, die Kapazität und den Verbrauch des Aquariums; ihr Tooltip
sagt, wie viele Stunden der Speicher reicht und welche Gruppe ohne Strom ist. Die Fenster von Unterschrank und Außenfilter
zeigen denselben Speicher. Lichter, die nicht auf einem Aquarium stehen (an einer Decke hängend), leuchten immer.

**Ältere Welten:** eine Config, in der einer der alten Schalter (`power.heaterRequiresEnergy`, `filterRequiresEnergy`,
`lightRequiresEnergy`, `aeration.airPumpRequiresEnergy`, `pipes.pumpsRequireEnergy`, `terrarium.misterRequiresEnergy`) an
war, wird `HARD`; die Federn von Unterschränken und Außenfiltern werden zu FE im Speicher des Beckens (nichts geht
verloren).
