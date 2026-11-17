---
navigation:
  title: Aquarianer
  icon: bettacraft:breeding_basin
  parent: getting_started.md
  position: 30
item_ids:
  - bettacraft:breeding_basin
feature: sources.aquaristVillager
---

# Aquarianer

![Ein Aquarianer neben seinem Zuchtbecken](../images/aquarist_villager.png)

Der **Aquarianer** ist ein Dorfbewohner-Beruf von Bettacraft: ein Dorfbewohner mit türkiser Gummischürze und blauen
Handschuhen, der verkauft, was ein Aquarium braucht, und **deine Bettas und gezüchteten Beifische** für Smaragde kauft,
je nach ihren Genen.

## Das Zuchtbecken

<ItemImage id="bettacraft:breeding_basin" />

Das <ItemLink id="bettacraft:breeding_basin" /> ist ein kleines Glasbecken mit Kies, Wasser und Pflanzen auf einem
Holzgestell, der Arbeitsplatz des Aquarianers. Stell es in ein Dorf: ein **arbeitsloser Dorfbewohner** in der Nähe nimmt
es und wird Aquarianer, wie bei den Berufen von Minecraft mit ihren Arbeitsplätzen. Das geht in jedem Dorf (auch in den
Dörfern anderer Mods); ein eigenes Aquarianer-Haus gibt es nicht. Das Becken ist nur Deko, darin lebt nichts.

## Handel

Wie jeder Dorfbewohner steigt er durch Handel auf. Auf jeder Stufe hat er zwei Angebote (jedes wählt eine seiner Waren):

| Stufe | Verkauft | Kauft |
|---|---|---|
| Neuling | Betta-Pellets, Flocken oder Schwimmpflanzen (Froschbiss, Schwimmfarn, Wasserlinse, Muschelblume) | **einen Betta im Eimer** |
| Lehrling | Aquarienpflanzen (Javafarn, Anubias, Javamoos, Cryptocoryne, Vallisnerie, Amazonas-Schwertpflanze, Rotala, Sagittaria) | **einen Betta oder gezüchtete Beifische im Fischbeutel** |
| Geselle | Neocaridina-Garnelen, Posthorn-, Zebrarennschnecken oder Apfelschnecken; einen pH-Test, ein TDS-Messgerät, Artemia-Eier oder Aktivboden | |
| Experte | eine Infusorien-, Artemia- oder Grünwasser-Kultur; eine seltene Pflanze (Tigerlotus, Cryptocoryne 'Flamingo', Bucephalandra, rote Stängelpflanzen, Mooskugel ...) | |
| Meister | zwei Angebote mit **einem Paar einer seltenen Zuchtform** eines Beifischs (Albino-Antennenwels, German Blue Ram, Koi-Skalar ...) | |

Der fahrende Händler behält seine eigenen Angebote ([Wo finde ich was](sources.md)).

## Was er zahlt

Leg einen Eimer oder Fischbeutel mit dem Fisch in das Handelsfeld eines Kaufangebots: das Ergebnis zeigt, was dieser Fisch
wert ist (das Angebot selbst zeigt „Preis nach den Genen des Fischs“). Er sieht nur die **sichtbaren** Gene; verdeckte
sind ihm nichts wert.

![Das Handelsmenü eines Meister-Aquarianers: ein gezüchteter Betta im Handelsfeld ist 9 Smaragde wert](../images/aquarist_trades.png)

* **Bettas** (erwachsen): 2 Smaragde, +1 für einen im Becken gezüchteten Betta, +0,5 für jede Generation (bis 6), +3 für
  eine benannte Zuchtform (Koi, Mustard Gas ...), +1 für jede seltene Flossenform, Brustflosse, Musterung oder jeden
  seltenen Glanz (Dumbo, Rosetail, Doubletail, Dalmatiner, Piebald ...), +1 für lange Flossen, +4 für Betta imbellis oder
  smaragdina; höchstens 24.
* **Beifische** (erwachsen, im Fischbeutel, auch mehrere in einem Beutel): nur **gezüchtete** Fische, keine Wildfänge. 1
  für eine häufige Art, 3 für eine seltene, +3 für eine gezüchtete Farbform (Black Ram, Albino-Antennenwels ...), mehr für
  wertvolle Arten (Diskus und Zebra-Harnischwels +5, Seepferdchen +3 / +4, Skalar +2); höchstens 16 pro Fisch.
* Jungfische und Halbwüchsige kauft er nicht.

Die Tabelle ist eine Datendatei: `data/<namespace>/bettacraft/aquarist_prices.json` (unsere im Namespace `bettacraft`).
Ein Datapack eines Modpacks kann einzelne Werte ändern, z. B. `{"betta": {"base": 4}, "tankmate": {"only_bred": false}}`.

## Einstellungen

Config `features.sources.aquaristVillager` (standardmäßig an). Aus: kein Dorfbewohner wird Aquarianer, Aquarianer
verlieren Beruf und Angebote, das Zuchtbecken lässt sich nicht herstellen.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:breeding_basin" />
