---
navigation:
  title: Licht
  icon: bettacraft:tank_light
  parent: equipment.md
  position: 40
item_ids:
  - bettacraft:tank_light
---

# Aquariumlicht

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank_light" x="0" p:axis="x" p:pos="true" />
  <Block id="bettacraft:tank_light" x="1" p:axis="x" p:neg="true" p:pos="true" />
  <Block id="bettacraft:tank_light" x="2" p:axis="x" p:neg="true" />
</GameScene>

Das <ItemLink id="bettacraft:tank_light" /> leuchtet mit Lichtstufe 15.

<RecipeFor id="bettacraft:tank_light" />

* Lichter in einer Reihe verbinden sich zu **einer langen Leiste** (nur der Länge nach, nicht zu einer Fläche).
* Setze es oben auf das Aquarium, auf den Glasrand oder **hängend** unter einen Block über dem Aquarium.
* Es ist standardmäßig an. Ein **Redstone-Signal** an irgendeinem Teil der Leiste schaltet die ganze Leiste aus, zum
  Beispiel mit einem Tageslichtsensor.
* Das Aquarium-Menü zeigt, wie viele Lichter an sind („Licht: 4/4 an“).
