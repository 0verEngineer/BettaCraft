---
navigation:
  title: Besatz
  icon: bettacraft:tank
  parent: invertebrates.md
  position: 70
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

* Es zählen nur Erwachsene, wie bei den Bettas; Jungtiere nicht.
* Auch ihr Abfall belastet das Wasser (im selben Verhältnis, Jungtiere zur Hälfte).
* Unabhängig vom Besatz hält ein Becken höchstens 200 Garnelen, Krebse und Schnecken
  (`invertebrates.maxCrittersPerTank`); dort hört die Vermehrung auf.
* Die Übersicht im [Aquarium-Menü](../aquarium/menu.md) sowie Jade, The One Probe und WTHIT zeigen den Besatz.
