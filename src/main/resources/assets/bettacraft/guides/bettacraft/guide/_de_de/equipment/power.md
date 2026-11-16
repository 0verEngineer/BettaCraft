---
navigation:
  title: Strom
  icon: minecraft:redstone
  parent: equipment.md
  position: 90
feature: power.enabled
---

# Strom

Standardmäßig braucht **keine Ausrüstung Strom**. Ein Server kann ihn getrennt für Heizer und Kühler, Filter und Lichter
einschalten (`power.heaterRequiresEnergy`, `power.filterRequiresEnergy`, `power.lightRequiresEnergy`, alle standardmäßig
`false`). Strom heißt **Forge Energy (FE)** und kommt aus jeder Mod, die FE erzeugt.

* Jedes Aquarium speichert FE für seine Ausrüstung (standardmäßig 100.000 FE, `power.bufferFe`).
* Schließe ein Kabel oder einen Generator an **einen beliebigen Aquariumblock**, an einen angeschlossenen
  **Außenfilter**, an einen **Schrank** unter dem Aquarium oder an ein **Licht** darauf an.
* Heizer und Kühler brauchen 8 FE pro Tick für jeden Wasserblock, den sie heizen oder kühlen (nur die Sorte, die gerade
  arbeitet, und höchstens für das Wasser im Aquarium), jeder Filter 10 FE pro Tick, jeder leuchtende Lichtblock 4 FE
  pro Tick.
* Ohne genug FE bleibt die Gruppe stehen: Heizer und Kühler ändern die Temperatur nicht mehr, Filter reinigen das Wasser
  nicht mehr, Lichter auf dem Aquarium gehen aus. Zuerst bekommen die Filter Strom, dann Heizer und Kühler, dann Lichter.

**Luftpumpen** laufen ohne Strom, solange der Server `aeration.airPumpRequiresEnergy` nicht einschaltet: dann brauchen
sie 2 FE pro Tick für jeden angetriebenen Ausströmer oder Schwammfilter (Kabel an die Pumpe oder ans Aquarium). Jedes
Gerät braucht bei kleinerem [Leistungsregler](regulator.md) weniger FE.

**Kein Stromerzeuger?** Mit `features.power.winding` (an) lässt sich jedes Gerät, das FE braucht, stattdessen mit der
[Kurbel](crank.md) aufziehen: der Außenfilter mit seinen Heizern, der Multiblock-Filter, die Osmoseanlage, die
Wassergeräte und der **Unterschrank** unter dem Aquarium, der alle Geräte seines Aquariums versorgt. FE geht vor, die
Feder springt ein, solange es fehlt.

**Create statt FE:** mit Create lädt ein [Antriebsadapter](drive_adapter.md) am Becken oder eine Welle am Schrank darunter
den Puffer mit Drehkraft (mit echtem Stress).

Der Tab **Wasser** im Aquarium-Menü zeigt die gespeicherte FE, den Bedarf und welche Gruppe ohne Strom ist; auch die
Übersicht warnt. Lichter, die nicht auf einem Aquarium stehen (an einer Decke hängen), leuchten immer.
