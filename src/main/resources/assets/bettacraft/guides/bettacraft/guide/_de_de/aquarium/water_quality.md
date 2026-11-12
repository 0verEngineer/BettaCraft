---
navigation:
  title: Wasserqualität
  icon: bettacraft:tank_lid
  parent: aquarium.md
  position: 15
item_ids:
  - bettacraft:tank_lid
---

# Wasserqualität und Verdunstung

![Ein Aquarium mit Abdeckungen, Licht und Futterautomat obenauf, Kühler und Heizer im Filter; links Larvenbottich und Artemia-Zuchtbecken](../images/water_care.png)

## Wasserqualität

Jedes Aquarium hat eine **Wasserqualität** von 100 % (sauber) bis 0 %. Der Tab **Wasser** im Aquarium-Menü zeigt sie
mit einem Balken, Jade, The One Probe und WTHIT zeigen sie beim Blick auf das Aquarium.

* Das Wasser verliert von selbst langsam an Qualität (1 % pro Tag) und mit **mehr Tieren** schneller: 2 Bettas in
  4 Wasserblöcken kosten weitere 2 % pro Tag.
* **Überfütterung:** Ein Fisch, der innerhalb eines halben Tages wieder Futter bekommt, war nicht hungrig; seine
  Portion verrottet im Wasser. Dreimal am Tag zu füttern verdreckt das Wasser deshalb schneller.
* **Filter** entfernen den Großteil des Abfalls (80 %), solange sie groß genug sind: Ein
  [Außenfilter](../equipment/filters.md) hält 32 Wasserblöcke sauber, ein Innenfilter unter Wasser 16. Ein kleinerer
  Filter entfernt einen passenden Teil.
* **Schwimmpflanzen** nehmen etwas Abfall auf, solange sie Licht haben, siehe [Schwimmpflanzen](floating_plants.md).
* **Wasserwechsel:** Wasser mit leeren Eimern entnehmen und Frischwasser nachgießen. Frischwasser verdünnt den Abfall;
  Wasser entnehmen ändert die Qualität nicht. Leitungen anderer Mods können das Aquarium ebenfalls füllen und leeren.

| Qualität | Zustand | |
|---|---|---|
| ab 85 % | sauber | |
| 60 % bis 85 % | gut | |
| 35 % bis 60 % | mäßig | das Menü warnt |
| unter 35 % | schlecht | die Bettas leiden wie bei falscher Temperatur, siehe [Gesundheit](../bettas/health.md) |

Das Aquarium-Log vermerkt, wenn das Wasser mäßig oder schlecht wird. Bestehende Aquarien starten mit sauberem Wasser.

## Verdunstung

Offene Aquarien verlieren langsam Wasser: etwa **20 mB pro Tag** je oberem Block bei 25 °C (wärmeres Wasser verdunstet
schneller). Der Abfall bleibt im Wasser, die Qualität sinkt also ebenfalls etwas. Fülle ab und zu mit Eimern nach.

<ItemImage id="bettacraft:tank_lid" scale="2" />

Ein oberer Block verdunstet nicht, wenn etwas darauf liegt: eine <ItemLink id="bettacraft:tank_lid" />, der
[Futterautomat](../equipment/feeder.md), ein [Licht](../equipment/light.md) oder ein beliebiger voller Block. Der Tab
Wasser zeigt, wie viele obere Blöcke noch offen sind.

<RecipeFor id="bettacraft:tank_lid" />

Wasserqualität und Verdunstung lassen sich in der Server-Config abschalten oder abmildern (`waterQuality.*`,
`evaporation.*`).
