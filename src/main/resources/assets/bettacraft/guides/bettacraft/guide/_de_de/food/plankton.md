---
navigation:
  title: Phytoplankton und Rotatorien
  icon: bettacraft:rotifers
  parent: food.md
  position: 15
item_ids:
  - bettacraft:phytoplankton_culture
  - bettacraft:phytoplankton
  - bettacraft:rotifer_culture
  - bettacraft:rotifers
---

# Phytoplankton und Rotatorien

![Clownfisch-Larven mit Rotatorien](../images/reef_larvae.png)

Die Futterkette der [Clownfisch-Larven](../saltwater/reef_animals.md): **Phytoplankton** (Mikroalgen) ernährt die
**Rotatorien** (Rädertierchen), die Rotatorien ernähren die Larven; später nehmen die Larven auch
[Artemia-Nauplien](fry_food.md). Schalter: `features.saltwater.larvaRearing`.

## Phytoplankton

1. Mische eine <ItemLink id="bettacraft:salt_water_bottle" /> mit **Seegras oder Seetang** und **Knochenmehl**: eine
   <ItemLink id="bettacraft:phytoplankton_culture" />.
2. Warte **einen Spieltag im Licht** (Lichtstufe 9 oder mehr, wenn sie fertig ist).
3. Sie wird zu einer <ItemLink id="bettacraft:phytoplankton" />.

Auf ein Riffbecken angewendet, färbt eine Flasche Phytoplankton das Wasser grün (die Flasche bekommst du zurück). Es verblasst
über einige Tage.

## Rotatorien

1. **Start:** eine Flasche Phytoplankton mit einer Flasche [Infusorien](fry_food.md) (darin leben Rotatorien): eine
   <ItemLink id="bettacraft:rotifer_culture" />. Der Aquarianer verkauft auch Kulturen.
2. Warte **einen Spieltag**: eine <ItemLink id="bettacraft:rotifers" />.
3. **Weiterführen:** eine Flasche Rotatorien und zwei Phytoplankton ergeben **zwei** Kulturen.

Rotatorien sind das feinste Aufzuchtfutter (alle Jungfische fressen sie vom ersten Tag an). In ein Riffbecken gefüttert,
**bleiben sie im Wasser**: Mit Phytoplankton leben sie weiter und vermehren sich, ohne sterben die meisten innerhalb von drei
Tagen. Das Menü zeigt Rotatorien und Phytoplankton in der Riff-Zeile des Wasser-Tabs.

## Die Clownfisch-Larven

Nach dem Schlupf treiben die Clownfisch-Larven **drei Tage** (`reef.clownLarvaDays`), bevor sie sich als Jungfische
niederlassen:

| Larventage | Frisst |
|---|---|
| erste Hälfte | nur Rotatorien |
| zweite Hälfte | Rotatorien oder Artemia-Nauplien |

Jede Larve frisst ein Zwanzigstel Portion am Tag (eine Flasche sind 4 Portionen). Gefüttert überleben die meisten (etwa 80 %),
ohne Futter nur **5 %** (`reef.clownLarvaSurvival`), ein Gelege von 30 bringt also ein, zwei Junge. Wenn mit Rotatorien
aufgezogene Larven sich niederlassen, meldet es das Protokoll (Fortschritt „Rotatorien-Aufzucht“).

**Der Ablauf:** Phytoplankton und eine Rotatorien-Kultur ansetzen, sobald das Clownfisch-Paar laicht (die Eier brauchen
anderthalb Tage), Phytoplankton dosieren, dann jeden Tag eine Flasche Rotatorien, später Artemia-Nauplien.

## Bezug

* <ItemLink id="bettacraft:rotifer_culture" />: **[Aquarianer](../getting_started/aquarist.md)** (Experte): 1 für 5 Smaragde; eins von etwa 5 in diesem Angebot (Config `features.sources.aquaristVillager`).

Herstellung:

<RecipesFor id="bettacraft:phytoplankton_culture" />
<RecipesFor id="bettacraft:rotifer_culture" />
