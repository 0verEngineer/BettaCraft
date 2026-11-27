---
navigation:
  title: Wasserwechsel-Steuerung
  icon: bettacraft:water_change_controller
  parent: equipment.md
  position: 45
item_ids:
  - bettacraft:water_change_controller
  - bettacraft:drain
---

# Wasserwechsel-Steuerung

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
</GameScene>

Aquarien brauchen **Wasserwechsel**, weil sich [Nitrat](../aquarium/nitrate.md) anreichert. Von Hand: mit einem leeren
Eimer Wasser abschöpfen und frisches einfüllen (25 % eines 4-Block-Aquariums sind ein Eimer). Frischwasser hat die
Temperatur der Umgebung, ein großer Wechsel ist also ein kühler Wasserwechsel (Panzerwelse lieben ihn).

Die <ItemLink id="bettacraft:water_change_controller" /> macht das selbst. Rechtsklicken:

* **Abstand** 1 bis 60 Tage (Vorgabe: was die Schwierigkeit verlangt, 14 Tage) und **Anteil** 5 bis 50 % (Vorgabe 25 %);
* **Automatik an/aus** und **jetzt wechseln**.

Sie hat **vier Rohranschlüsse** (von vorne gesehen): **Frischwasser rein** links (ein [Rohr](pipes.md) von einem
[Vorratsbehälter](top_off.md), einer Wasserpumpe an einer Wasserquelle, einer Osmoseanlage oder dem Tank einer anderen
Mod), **Frischwasser raus** zu den Aquarien hinten, **Abwasser rein** von den Aquarien oben, **Abwasser raus** rechts (zu
einem <ItemLink id="bettacraft:drain" />, einem Vorratsbehälter oder dem Tank einer anderen Mod). **Direkt an einem
Aquarium** oder Multiblock-Filter ersetzt die Berührung die beiden beckenseitigen Anschlüsse, dann reichen zwei Rohre.
Frischwasser kommt nie aus dem, was nur daneben steht. Ein Wechsel bewegt, was ihre Rohre tragen; sie lässt nur ab, was
sie nachfüllen kann, und nie unter halb voll, solange Tiere im Aquarium leben. Sie füllt auch Aquarien mit
[Niveausensor](top_off.md) nach. Wo sie ein Aquarium entleert, hält ein Ansaugschutz innen die Jungtiere drin. Das Logbuch notiert jeden Wechsel. Mit Stromkosten (`power.costs` `EASY` oder `HARD`) braucht sie FE
(8 FE je 100 mB).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:water_change_controller" />
<RecipesFor id="bettacraft:drain" />
