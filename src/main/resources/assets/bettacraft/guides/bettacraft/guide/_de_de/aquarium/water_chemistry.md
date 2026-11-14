---
navigation:
  title: Wasserchemie
  icon: bettacraft:ph_test_kit
  parent: aquarium.md
  position: 37
item_ids:
  - bettacraft:remineralising_salt
  - bettacraft:almond_leaf
---

# Wasserchemie

Neben Temperatur, Qualität und Nitrat hat das Wasser eines Aquariums drei einfache chemische Werte. Der Reiter
**Wasser** des Beckenmenüs zeigt sie in seiner zweiten Zeile (mit der Maus darüber: was sie bewegt und welchen Tieren
das Wasser nicht passt); der [TDS-Messstift und der pH-Tropfentest](../equipment/water_tests.md) messen sie am Becken.

* **TDS** (gelöste Stoffe, ppm): die Mineralien im Wasser. **Leitungswasser hat etwa 250 ppm**, [Osmosewasser](../equipment/reverse_osmosis.md) 0.
  Futter und Abfall der Tiere heben ihn langsam (`chemistry.tdsPerDay`, 1,5 ppm am Tag in einem normal besetzten
  Becken), Verdunstung lässt ihn zurück (Nachfüllen mit Leitungswasser hebt ihn), Wasserwechsel bringen ihn zurück.
  <ItemLink id="bettacraft:remineralising_salt" /> hebt ihn gezielt: eine Portion bringt 400 ppm in einen Eimer
  Wasser; schleichend kommt so viel hinein, dass das Aquarium 150 ppm erreicht.
* **pH** (5 bis 9): Leitungswasser hat 7,5. Der pH bewegt sich in etwa einem Tag dorthin, wo das Aquarium ihn hält:
  * Soil am Boden (Erde, Schlamm, Podsol, Moos ... `#bettacraft:acid_substrate`) zieht ihn Richtung 6,2;
  * Kalkstein (Kalzit, Tropfsteinblock, tote Koralle, Kalkstein von Create ... `#bettacraft:limestone`) zieht ihn
    Richtung 8,2, als Bodengrund ganz, als Steine im Aquarium halb;
  * Torf im [Multiblock-Filter](../equipment/filter_media.md), Wurzeln (Stämme und Wurzeln im Aquarium,
    `#bettacraft:driftwood`), <ItemLink id="bettacraft:almond_leaf" /> und [CO2](../equipment/co2.md) senken ihn;
  * weiches Wasser (wenig TDS) ist kaum gepuffert und wird von selbst leicht sauer.
* **Härte** ist kein eigener Wert: **weich, mittelhart oder hart** aus TDS und Bodengrund (Soil macht weich,
  Kalkstein hart). Leitungswasser ist mittelhart.

Seemandelbaumblätter färben das Wasser mit Gerbstoffen (ein Blatt je Wasserblock färbt es ganz); sie verblassen in
etwa einer Woche (`chemistry.leafHalfLifeDays`). Dschungellaub im Räucherofen, Ofen oder auf dem Lagerfeuer trocknen.

## Die Tiere

Jedes Tier hat seinen Bereich für pH und TDS; **Leitungswasser passt allen** außer den
[Bienengarnelen](../invertebrates/caridina.md) (sie brauchen weiches Wasser: Osmosewasser, leicht aufgehärtet, und
Soil), für Aquarien älterer Versionen ändert sich also nichts. Etwas außerhalb sind die Tiere nur gestresst (das Menü warnt, Kleintiere vermehren sich langsamer);
weit außerhalb (ein halber pH-Schritt oder 50 ppm jenseits des Bereichs) gilt das Wasser für sie als schlecht: sie
werden krank und sterben am Ende.

| Tiere | pH | TDS (ppm) |
|---|---|---|
| Bettas | 6,0–8,0 | 0–600 |
| Salmler, Bärblinge, Dornauge (Weichwasser) | 5,0–7,8 | 0–450 |
| Panzerwelse, Otocinclus | 5,8–8,0 | 0–500 |
| Harnischwelse, Prachtschmerle | 5,8–8,0 | 0–550 |
| Guppy, Endler, Platy (Lebendgebärende) | 6,8–8,6 | 100–800 |
| Zwergfadenfisch | 6,0–8,0 | 0–500 |
| Neocaridina | 6,2–8,2 | 80–500 |
| Zwergkrebs | 6,5–8,5 | 100–600 |
| [Bienengarnelen (Caridina)](../invertebrates/caridina.md) | 5,5–6,8 | 100–200 |
| [Kardinalgarnele](../invertebrates/sulawesi_shrimp.md), [Tylomelania](../invertebrates/tylomelania.md) (Sulawesi) | 7,5–8,6 | 150–350 |
| Schnecken (Posthorn-, Blasen- und Turmdeckelschnecke: 6,2–8,6, 50–800) | 6,8–8,6 | 100–700 |
| Meerestiere | 7,8–8,6 | (nur der pH) |

Die Artenseiten nennen den Bereich jedes Tiers. Salzwasser ist durch seine Karbonathärte gepuffert: sein pH bleibt
bei etwa 8,2, und der Bodengrund bewegt ihn kaum; verbrauchen die Steinkorallen die Karbonathärte, sinkt er (siehe
[Riffchemie](../saltwater/reef_chemistry.md)).

`features.waterChemistry` (Common-Config) schaltet alles ab: die Werte stehen still und kein Tier stört sich daran.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:remineralising_salt" />
<RecipesFor id="bettacraft:almond_leaf" />
