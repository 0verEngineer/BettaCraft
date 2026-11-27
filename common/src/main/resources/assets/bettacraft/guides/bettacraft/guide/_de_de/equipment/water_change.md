---
navigation:
  title: Wasserwechsel-Steuerung
  icon: bettacraft:water_change_controller
  parent: equipment.md
  position: 45
item_ids:
  - bettacraft:water_change_controller
---

# Wasserwechsel-Steuerung

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
</GameScene>

Aquarien brauchen **Wasserwechsel**, weil sich [Nitrat](../aquarium/nitrate.md) anreichert. Von Hand: mit einem leeren
Eimer Wasser abschöpfen und frisches einfüllen (25 % eines 4-Block-Aquariums sind ein Eimer). Frischwasser hat die
Temperatur der Umgebung, ein großer Wechsel ist also ein kühler Wasserwechsel (Panzerwelse lieben ihn).

Die <ItemLink id="bettacraft:water_change_controller" /> macht das selbst. **Neben ein Aquarium** stellen und
rechtsklicken:

* **Abstand** 1 bis 60 Tage (Vorgabe: was die Schwierigkeit verlangt, 14 Tage) und **Anteil** 5 bis 50 % (Vorgabe 25 %);
* **Automatik an/aus** und **jetzt wechseln**.

Wenn es so weit ist, lässt ihre Ablaufpumpe das alte Wasser in den Abfluss und füllt dieselbe Menge Frischwasser aus
einem [Vorratsbehälter](top_off.md), einem [Rohr](pipes.md) oder dem Tank einer anderen Mod **daneben** nach, oder
unbegrenzt aus einer **Wasserquelle** daneben. Sie lässt nur ab, was sie nachfüllen kann, und nie unter halb voll,
solange Tiere im Aquarium leben. Das Logbuch notiert jeden Wechsel. Mit Stromkosten (`power.costs` `EASY` oder `HARD`) braucht sie FE
(8 FE je 100 mB).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:water_change_controller" />
