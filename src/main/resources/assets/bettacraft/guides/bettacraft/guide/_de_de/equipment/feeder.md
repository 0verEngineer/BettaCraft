---
navigation:
  title: Futterautomat
  icon: bettacraft:feeder
  parent: equipment.md
  position: 40
item_ids:
  - bettacraft:feeder
---

# Futterautomat

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank" p:fill="14" />
  <Block id="bettacraft:feeder" y="1" />
</GameScene>

Setze den <ItemLink id="bettacraft:feeder" /> **oben auf ein Aquarium** und lege Futter in seine neun Plätze. Trichter,
Rohre anderer Mods und die Busse und Interfaces von AE2 oder Refined Storage können ihn befüllen (und Futter herausnehmen).

* Er füttert standardmäßig **einmal am Tag** morgens. In seinem Menü wählst du 1, 2 oder 3 Fütterungen am Tag (morgens,
  nachmittags, abends).
* Jeder Fisch bekommt das erste passende Futter aus den Plätzen: **Jungfischfutter** für Jungfische, Pellets oder Larven
  für die älteren.
* Die Plätze **wechseln sich ab**: jede Fütterung beginnt mit dem Platz nach dem, der beim letzten Mal zuerst gefüttert
  hat. So geben Pellets, Flocken und Frostfutter im Automaten den Fischen [Abwechslung](../food/food_variety.md).
* Sind seine eigenen Plätze leer, nimmt er Futter aus den [Schränken](cabinet.md) unter dem Aquarium.
* Das Log des Aquariums notiert jede Fütterung und „Der Futterautomat ist leer“.

Kein Platz oben (ein Becken von einem Block Höhe mit Licht)? Der [Klemm-Futterautomat](clip_feeder.md) hängt seitlich an der Scheibe.

Ein Pellet oder eine Portion Mückenlarven füttert bis zu 4 Fische, eine Flasche Jungfischfutter 20 Jungfische.
Überfüttern schadet nicht. Siehe [Futter](../food.md).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:feeder" />
