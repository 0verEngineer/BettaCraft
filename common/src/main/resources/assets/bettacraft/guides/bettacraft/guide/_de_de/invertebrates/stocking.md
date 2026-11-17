---
navigation:
  title: Besatz
  icon: bettacraft:tank
  parent: invertebrates.md
  position: 50
---

# Besatz

Die Besatzgrenzen eines Aquariums zählen Fische: Ab 2 adulten Tieren pro Wasserblock stehen die Bettas unter Stress,
höchstens 6 pro Wasserblock passen hinein (`tank.overcrowdedPerWaterBlock`, `tank.hardCapPerWaterBlock`). Kleintiere
zählen anteilig:

| Tiere | zählen wie ein Fisch | Config |
|---|---|---|
| Schnecken | 50 | `stocking.snailsPerFish` |
| Garnelen | 25 | `stocking.shrimpPerFish` |
| Zwergkrebse | 12 | `stocking.crayfishPerFish` |
| Zwergkrallenfrösche | 1 | `stocking.frogsPerFish` |

* Jungtiere zählen mit dem Anteil eines juvenilen Bettas (`stocking.juvenileStocking`, 0,1) davon: 25 junge Garnelen
  sind ein Zehntel Fisch. Junge Bettas: siehe [Bewohner](../aquarium/inhabitants.md).
* Auch ihr Abfall belastet das Wasser (im selben Verhältnis, Jungtiere zur Hälfte).
* Unabhängig vom Besatz hält ein Becken höchstens 200 Garnelen, Krebse und Schnecken
  (`invertebrates.maxCrittersPerTank`); dort hört die Vermehrung auf. Sie hört auch auf, wenn der ganze Besatz (Fische
  und Kleintiere) die Obergrenze erreicht.
* Die Übersicht im [Aquarium-Menü](../aquarium/menu.md) sowie Jade, The One Probe und WTHIT zeigen den Besatz.
