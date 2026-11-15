---
navigation:
  title: Futtervielfalt
  icon: bettacraft:fish_flakes
  parent: food.md
  position: 40
item_ids:
  - bettacraft:fish_flakes
  - bettacraft:food_granules
  - bettacraft:shrimp_food
  - bettacraft:frozen_mosquito_larvae
  - bettacraft:frozen_artemia
  - bettacraft:frozen_daphnia
  - bettacraft:shell_food
---

# Futtervielfalt

<Row>
  <ItemImage id="bettacraft:fish_flakes" scale="2" />
  <ItemImage id="bettacraft:food_granules" scale="2" />
  <ItemImage id="bettacraft:frozen_mosquito_larvae" scale="2" />
  <ItemImage id="bettacraft:frozen_artemia" scale="2" />
  <ItemImage id="bettacraft:frozen_daphnia" scale="2" />
  <ItemImage id="bettacraft:shrimp_food" scale="2" />
  <ItemImage id="bettacraft:shell_food" scale="2" />
</Row>

Jedes Fischfutter gehört zu einer **Futtergruppe**: Pellets, Flocken, Granulat, Tabletten, Frostfutter oder
Lebendfutter. Jeder Fisch merkt sich, welche Gruppen er in den letzten 3 Tagen gefressen hat (`diet.varietyDays`).

* **Abwechslung:** Ein Fisch, der in dieser Zeit **3 verschiedene Gruppen** gefressen hat (`diet.varietyGroups`), erholt
  sich **anderthalbmal so schnell** von Stress und Krankheit (`diet.varietyRecovery`), und ein Betta zeigt etwas kräftigere
  Farben. Fahre im Aquarium-Menü über den Futterbalken eines Fisches, um zu sehen, wie viele Sorten er hatte. Lege mehrere
  Futter in den [Futterautomaten](../equipment/feeder.md): seine Plätze wechseln sich ab.
* **Lieblingsfutter** einer Art konditioniert ein erwachsenes Tier für die Zucht (wie Lebendfutter).
* **Futter, das schlecht zu einer Art passt**, sättigt nur eine Weile: der Fisch hat einen halben Tag früher wieder
  Hunger (`diet.poorFoodFedDays`).

| Fresser | Lieblingsfutter | Passt schlecht |
|---|---|---|
| Bettas | Lebend- und Frostfutter | Tabletten (sie fressen sie nicht) |
| Fische im freien Wasser (Salmler, Bärblinge, Lebendgebärende, Guramis, Meeresfische) | Flocken, Frostfutter | Tabletten |
| Bodenfische (Panzerwelse, Schmerlen) | Granulat, Tabletten, Frostfutter | Flocken (sie schwimmen oben) |
| Weidegänger (Harnischwelse, Otocinclus, Doktorfische) | Tabletten | Lebend- und Frostfutter, Flocken |
| Seepferdchen | Frostfutter | |
| Kugelfische | Schalenfutter, Frost- und Lebendfutter | Flocken, Pellets |

Datapacks ändern das mit den Item-Tags `#bettacraft:food_groups/<gruppe>` und `#bettacraft:diet/<fresser>/favourite`
und `/poor` (`betta`, `omnivore`, `bottom`, `grazer`, `seahorse`, `puffer`).

## Die Futter

* **Flockenfutter** schwimmt an der Oberfläche: die Fische im freien Wasser mögen es, die Bodenfische bekommen kaum etwas.
* **Granulatfutter** sinkt langsam: die Bodenfische bekommen ihren Anteil auch neben schnelleren Fischen.
* **Frostfutter** (Mückenlarven, Artemia, Daphnien mit Eis gefroren): wird ab Juvenil wie Lebendfutter gefressen,
  konditioniert für die Zucht und hält sich. Es geht nicht durch die [Lebendfutter-Pumpe](live_food_pump.md).
* **Garnelenfutter** sinkt zu den Garnelen, Krebsen und Schnecken; die Fische lassen es liegen. Es gibt ihnen doppelt so
  viel Futter wie ein übriges Pellet (`diet.shrimpFoodFactor`).
* **Schalenfutter** ist hartes Futter mit zerstoßenen Schalen (`#bettacraft:hard_food`): Es sinkt, die Fische fressen es wie
  Granulat, und die [Kugelfische](../tankmates/puffers.md) schleifen sich daran die Zähne ab wie an Schnecken.

Alle kommen aus der [Futtermaschine](food_maker.md).

## Bezug

* <ItemLink id="bettacraft:fish_flakes" />: **[Futtermaschine](food_maker.md)**: 1 × <ItemLink id="minecraft:cod" /> + 1 × <ItemLink id="minecraft:wheat" /> + 1 × <ItemLink id="minecraft:kelp" /> + 1 × <ItemLink id="minecraft:sugar" /> → 12.
* <ItemLink id="bettacraft:food_granules" />: **[Futtermaschine](food_maker.md)**: 1 × <ItemLink id="minecraft:spider_eye" /> + 2 × <ItemLink id="minecraft:wheat" /> + 1 × <ItemLink id="minecraft:dried_kelp" /> → 12.
* <ItemLink id="bettacraft:shrimp_food" />: **[Futtermaschine](food_maker.md)**: 1 × <ItemLink id="minecraft:kelp" /> + 1 × <ItemLink id="minecraft:carrot" /> + 1 × <ItemLink id="minecraft:bone_meal" /> → 8.
* <ItemLink id="bettacraft:frozen_mosquito_larvae" />: **[Futtermaschine](food_maker.md)**: 4 × <ItemLink id="bettacraft:mosquito_larvae" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
* <ItemLink id="bettacraft:frozen_artemia" />: **[Futtermaschine](food_maker.md)**: 4 × <ItemLink id="bettacraft:artemia_nauplii" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
* <ItemLink id="bettacraft:frozen_daphnia" />: **[Futtermaschine](food_maker.md)**: 4 × <ItemLink id="bettacraft:daphnia" /> + 1 × <ItemLink id="minecraft:ice" /> → 4.
* <ItemLink id="bettacraft:shell_food" />: **[Futtermaschine](food_maker.md)**: 2 × <ItemLink id="minecraft:bone_meal" /> + 1 × <ItemLink id="minecraft:cod" /> + 1 × <ItemLink id="minecraft:dried_kelp" /> → 8.
