---
navigation:
  title: Daphnien-Fass
  icon: bettacraft:daphnia_barrel
  parent: food/live_food.md
  position: 40
item_ids:
  - bettacraft:daphnia_barrel
  - bettacraft:daphnia
  - bettacraft:green_water_culture
  - bettacraft:green_water_bottle
---

# Daphnien-Fass

<Row>
  <ItemImage id="bettacraft:daphnia_barrel" scale="2" />
  <ItemImage id="bettacraft:daphnia" scale="2" />
  <ItemImage id="bettacraft:green_water_bottle" scale="2" />
</Row>

**Daphnien** (Wasserflöhe) sind Lebendfutter für Bettas, Beifische, Zwergkrallenfrösche und Seepferdchen. Das Daphnien-Fass
hält eine Kultur davon, ohne Fische darin.

1. Mit **Wasser** füllen: mindestens 2 Eimer (2000 mB), es fasst 4000; Rohre füllen es auch.
2. Die Kultur mit ein paar **Daphnien** starten: eine Flasche Sumpfwasser bringt manchmal welche mit
   (`liveFood.bottleDaphniaChance`).
3. Mit **Hefe** oder **Grünwasser** füttern (`#bettacraft:daphnia_food`; Grünwasser nährt mehr). Grünwasser: Sumpfwasser
   mit Knochenmehl ergibt einen Grünwasser-Ansatz, der nach einem Tag im Licht grün wird. Von Hand behältst du die leere
   Flasche, aus einem Trichter bleibt sie im Fass (ein Trichter darunter holt sie heraus).
4. Es braucht **Licht** 9 oder mehr und keine **heiße Sonne**: in einem heißen Biom (Wüste, Savanne, Tafelberge) stirbt
   ein Fass unter freiem Himmel am Tag, also in den Schatten mit einer Lampe stellen. Im Nether ist es immer zu heiß.
5. Gefüttert und beleuchtet füllt es sich in etwa **3 Spieltagen** (`liveFood.daphniaDays`) und hält dann 16 Portionen
   (`liveFood.daphniaRoom`).

## Rechtzeitig ernten

Ein volles Fass ist **überbesetzt**. Erntet niemand einen Tag lang (`liveFood.crowdedDays`), **bricht die Kultur
zusammen** auf wenige Tiere und muss neu wachsen. Mit dem **Kescher** ernten (bis zu 4 Portionen), mit einem Trichter
darunter oder mit einer [Lebendfutter-Pumpe](live_food_pump.md), die deine Becken von selbst füttert; ein paar bleiben
immer für die Kultur.

Rechtsklick mit leerer Hand zeigt Wasser, Kultur, Futter und fertige Portionen; ein Komparator liest ab, wie voll es ist.

## Bezug

* <ItemLink id="bettacraft:daphnia" />: **Glasflasche** mit Wasser gefüllt in Sümpfen: 8 % pro Flasche, zusätzlich zum Wasser.
* <ItemLink id="bettacraft:green_water_culture" />: **[Aquarianer](../getting_started/aquarist.md)** (Experte): 1 für 3 Smaragde; eins von etwa 3 in diesem Angebot (Config `features.sources.aquaristVillager`).

Herstellung:

<RecipesFor id="bettacraft:daphnia_barrel" />
<RecipesFor id="bettacraft:green_water_culture" />
