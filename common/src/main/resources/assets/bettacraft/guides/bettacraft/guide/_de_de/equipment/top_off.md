---
navigation:
  title: Nachfüllsystem
  icon: bettacraft:level_sensor
  parent: equipment.md
  position: 46
item_ids:
  - bettacraft:level_sensor
  - bettacraft:reservoir
---

# Nachfüllsystem

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:fill="14" />
  <Block id="bettacraft:level_sensor" x="1" p:facing="east" />
  <Block id="bettacraft:reservoir" x="2" p:fill="6" />
</GameScene>

Offene Aquarien verlieren Wasser durch Verdunstung (ein Deckel hilft). Der <ItemLink id="bettacraft:level_sensor" />
misst das:

* **Neben ein Aquarium** stellen (daneben oder darauf). Er misst nur, wie ein Schwimmerschalter; kein Rohr verbindet sich
  mit ihm.
* Ist das Wasser etwas unter sein **Ziel** gesunken (100 %; Rechtsklick: 95, 90, 80, 70, 60, 50 %), meldet er das und
  gibt ein **Redstone-Signal**.
* Eine [Wasserwechsel-Steuerung](water_change.md), die dieses Aquarium versorgt, liest ihn ohne Kabel und **füllt nach**,
  über ihre Frischwasserleitung (aus einem <ItemLink id="bettacraft:reservoir" />, einer Pumpe an einer Wasserquelle oder
  dem Tank einer anderen Mod).
* **Nur Süßwasser.** Verdunstet Wasser, bleibt das Salz eines [Riffbeckens](../saltwater.md) zurück; Nachfüllen mit
  Süßwasser bringt den Salzgehalt genau dorthin zurück, wo er war. Eine salzige Quelle wird abgelehnt.

Der **Vorratsbehälter** fasst 16 Eimer. Mit Eimern oder Rohren füllen; jedes Wasser mischt sich (Rechtsklick zeigt den
Füllstand und welches Wasser drin ist; ein Komparator liest ihn).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:level_sensor" />
<RecipesFor id="bettacraft:reservoir" />
