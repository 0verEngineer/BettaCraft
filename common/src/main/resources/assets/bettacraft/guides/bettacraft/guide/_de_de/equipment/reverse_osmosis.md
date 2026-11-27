---
navigation:
  title: Osmoseanlage
  icon: bettacraft:ro_unit
  parent: equipment.md
  position: 103
item_ids:
  - bettacraft:ro_unit
  - bettacraft:ro_membrane
  - bettacraft:ro_prefilter
  - bettacraft:ro_water_bucket
---

# Osmoseanlage

<GameScene zoom="4" interactive={true}>
  <Block id="minecraft:water" x="-1" />
  <Block id="bettacraft:ro_unit" x="0" p:facing="north" p:active="true" />
  <Block id="bettacraft:glass_pipe" x="1" p:west="true" p:east="true" />
  <Block id="bettacraft:tank" x="2" p:fill="14" />
</GameScene>

Die <ItemLink id="bettacraft:ro_unit" /> macht **Osmosewasser**: Wasser ohne gelöste Stoffe (TDS 0, pH 6,8).
Weichwasserfische und -garnelen brauchen es; mit normalem Wasser oder [Aufhärtesalz](../aquarium/water_chemistry.md)
gemischt ergibt es jede gewünschte Härte.

* **Wasser hinein**: über Rohre an jeder Seite, einen Wassereimer (oder Meerwasser: Die Membran hält das Salz zurück)
  oder einen **Wasserquellblock daneben** (endlos, wie ein Wasserhahn).
* **Sie läuft einfach**, ohne Strom und ohne Wasserdruck, langsam: 1 mB Osmosewasser pro Tick (`osmosis.roRate`).
* **Mit [Stromkosten](power.md) und FE** macht ihre Druckpumpe sie **viermal schneller** (`roPumpBoost`,
  `roPumpFe` 20 FE/t). Ohne FE läuft sie langsam weiter.
* **Ausbeute**: Für jeden Teil Osmosewasser fließen drei Teile **Abwasser** ab (`roWastePerPart`). Es geht in ein Rohr
  oder einen Tank einer anderen Mod **unter** der Anlage, sonst in den Abfluss.
* **Osmosewasser heraus**: Ein leerer Eimer ergibt einen <ItemLink id="bettacraft:ro_water_bucket" />; Rohre und
  [Wasserpumpen](pipes.md) holen es heraus. Sie fasst 8 Eimer.
* **Membran und Vorfilter**: <ItemLink id="bettacraft:ro_membrane" /> (etwa 128 Eimer) und
  <ItemLink id="bettacraft:ro_prefilter" /> (etwa 32 Eimer) nutzen sich ab und müssen ersetzt werden: Rechtsklick mit
  einem neuen auf die Anlage (der alte kommt heraus) oder ein Trichter setzt ihn ein. Schleichen + leere Hand nimmt
  sie heraus. Ohne beide steht sie still.
* Rechtsklick mit leerer Hand zeigt den Zustand: wie schnell sie läuft, das Osmosewasser, das Wasser am Zulauf und
  wie viel von Membran und Vorfilter übrig ist.

Die Rotation von Create treibt sie nicht an (keine feste Abhängigkeit von Create); sie läuft ohnehin auch so.

Ein Wasserwechsel mit Osmosewasser mischt dessen Werte ins Aquarium (TDS und pH nach Volumen, siehe
[Wasser als Flüssigkeit](../aquarium/water_fluids.md)).

`features.osmosis.enabled` (Common-Config) schaltet die Anlage ab.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:ro_unit" />
<RecipesFor id="bettacraft:ro_membrane" />
<RecipesFor id="bettacraft:ro_prefilter" />
