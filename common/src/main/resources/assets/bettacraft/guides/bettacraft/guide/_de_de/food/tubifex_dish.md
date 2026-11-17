---
navigation:
  title: Tubifex-Zuchtschale
  icon: bettacraft:tubifex_dish
  parent: food/live_food.md
  position: 30
item_ids:
  - bettacraft:tubifex_dish
  - bettacraft:tubifex
  - bettacraft:mulm
---

# Tubifex-Zuchtschale

<Row>
  <ItemImage id="bettacraft:tubifex_dish" scale="2" />
  <ItemImage id="bettacraft:tubifex" scale="2" />
  <ItemImage id="bettacraft:mulm" scale="2" />
</Row>

**Tubifex** sind Lebendfutter mit hohem **Konditionierungswert**: ein Zuchtfisch, der sie frisst, bleibt doppelt so lange
in Stimmung wie mit Mückenlarven oder Daphnien (`liveFood.tubifexConditioning`). Die Zuchtschale hält sie in Schlamm und
abgestandenem Wasser, so wie sie in der Natur leben.

1. **Schlamm** hineingeben (Rechtsklick oder Trichter) und **Wasser** (Eimer oder Rohr; die Schale fasst 1000 mB und
   braucht 250).
2. Die Kultur mit ein paar **Tubifex** starten: Schlamm im Sumpf abbauen bringt manchmal welche.
3. Mit **Mulm** füttern (`#bettacraft:tubifex_food`). Mulm bekommst du, wenn du schmutzige Filterschwämme oder
   Keramikröhrchen in einem Wasserkessel auswäschst (einen ab einem Viertel Schmutz, zwei ab drei Vierteln, siehe
   [Filtermedien](../equipment/filter_media.md)). Auch das **Abwasser** eines Beckens füttert sie: Wasser aus einem Rohr
   mit einer Qualität unter 80 % zählt als Futter.
4. Gefüttert füllt sich die Schale in etwa **4 Spieltagen** (`liveFood.tubifexDays`) und hält dann 8 Portionen
   (`liveFood.tubifexRoom`). Mit dem **Kescher** ernten (bis zu 4 Portionen) oder einen Trichter bzw. eine
   [Lebendfutter-Pumpe](live_food_pump.md) sie herausholen lassen; ein paar bleiben immer für die Kultur.

## Abgestandenes Wasser

Das Wasser der Schale verdunstet (100 mB am Tag) und steht langsam **ab**, schnell bei zu viel Mulm. Frisches Wasser
spült sie durch: ein Eimer Wasser auf die volle Schale (er läuft durch) oder sauberes Wasser aus einem Rohr. Würmer aus
einer Schale, die halb oder mehr abgestanden ist, sind von **schlechter Qualität**: jede Mahlzeit davon stresst einen Fisch
ein wenig wie schlechtes Wasser (`liveFood.poorTubifexStress`), ein Fisch in schlechtem Wasser wird also früher krank.
Eine völlig verdorbene Schale stirbt ab.

Rechtsklick mit leerer Hand zeigt Wasser, Kultur, Futter, fertige Portionen und wie abgestanden sie ist; ein Komparator
liest ab, wie voll sie ist.

## Bezug

* <ItemLink id="bettacraft:tubifex" />: **Schlamm abbauen** in Sümpfen: 5 % pro Block.

Herstellung:

<RecipesFor id="bettacraft:tubifex_dish" />
