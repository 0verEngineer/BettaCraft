---
navigation:
  title: Schildkrötenzucht
  icon: bettacraft:slider_turtle_spawn_egg
  parent: terrarium.md
  position: 55
---

# Schildkrötenzucht

Alle Schildkröten der Mod vermehren sich im Becken: die [Moschusschildkröte](musk_turtle.md), die
[Schmuckschildkröte](slider_turtle.md), die [Griechische Landschildkröte](hermann_tortoise.md) und die
[Dreikielschildkröte](reeves_turtle.md).

## Eiablage

Ein ausgewachsenes **Weibchen** legt, wenn ein ausgewachsenes **Männchen** seiner Art im selben Becken lebt, es in den
letzten zwei Tagen gefressen hat, nicht in Winterstarre ist und nichts ihm schadet oder es stark stresst. Es läuft zu einem
**Nistplatz**: einer Stelle auf dem **Land**, deren Oberseite **Sand oder Erde** ist (Sand, roter Sand, Erde, Gras, Podsol,
Schlamm, grobe Erde, Moos …; der Block-Tag `#bettacraft:turtle_nest_ground`), mit Platz darüber. Ein Sand-Bodengrund zählt,
wo der Boden Land ist (ein trockenes Terrarium). Stein, Kies, Holz oder Land unter Wasser: kein Nistplatz, keine Eier (das
Logbuch sagt es einmal am Tag).

Dort legt es **2 bis 6 Eier** (je nach Art), halb vergraben: du siehst sie in der Miniatur und als Block im
[Innenraum](../interior.md). Danach wartet es 4 Spieltage (`terrarium.clutchIntervalDays`).

Es legt nicht, solange seine Schlüpflinge das Becken **überfüllen** würden (der Besatz zählt Wasser und Land, die Eier mit).
Beim Schlüpfen kommen nur so viele heraus, wie die harte Grenze des Beckens erlaubt; die anderen sterben im Ei.

## Brut und das Geschlecht der Schlüpflinge

Schildkröten haben **keine Geschlechtschromosomen**: die **Temperatur des Nests** entscheidet, ob ein Schlüpfling ein
Weibchen oder ein Männchen wird (temperaturabhängige Geschlechtsbestimmung). Es zählt das mittlere Drittel der Brut; der
Klima-Reiter zeigt die Gelege, wie weit sie sind, die Nesttemperatur und was sie daraus macht.

Das Nest hat die Temperatur des **Bodens**: mit [Heizmatten](heat_mat.md) unter dem Becken wärmer als die Luft, ohne sie die
Luft. Wärmer schlüpft auch schneller.

| Art | Männchen (90 % oder mehr) | Halb und halb | Weibchen (90 % oder mehr) | Brutdauer bei der Pivottemperatur |
|---|---|---|---|---|
| Schmuckschildkröte | unter 28 °C | 29,2 °C | über 30,4 °C | 3,5 Tage |
| Griechische Landschildkröte | unter 30,3 °C | 31,5 °C | über 32,7 °C | 4 Tage |
| Dreikielschildkröte | unter 27,3 °C | 28,5 °C | über 29,7 °C | 3,5 Tage |
| Moschusschildkröte | 22,2 bis 25,8 °C | 21 und 27 °C | unter 19,8 und über 28,2 °C | 3 Tage |

Die Moschusschildkröte hat das Muster ihrer Gattung (*Sternotherus*): Weibchen an **beiden** Enden, Männchen in der Mitte.
Bei den anderen gilt: kühl gibt Männchen, warm gibt Weibchen. Unter 18 °C entwickeln sich die Eier nicht (sie warten); über
36 °C sterben die Embryonen; Wasser über dem Nest ertränkt sie. Wer die Eier im Innenraum abbaut, zerstört sie. Jedes Gelege
hat einen festen Zufallswert: es gibt immer dieselben Jungen. Die Config `terrarium.incubationSpeed` beschleunigt die Brut.

Jeder Schlüpfling bekommt die Varietät seiner Mutter oder seines Vaters (die Schmuckschildkröte: Rot- oder Gelbwange). Ein
Schlüpfling ist klein (etwa ein Drittel eines ausgewachsenen Tiers) und wächst in 4 bis 8 Spieltagen heran; sein Geschlecht
zeigt sich, wenn er ausgewachsen ist (der Tooltip des Eimers).

## Schlüpflinge der Meeresschildkröte

Die Vanilla-**Meeresschildkröte** ist viel zu groß für jedes Becken. Ihre **Schlüpflinge** passen: fang einen mit dem
[Fischbeutel](../equipment/fish_bag.md) am Strand und setz ihn in ein Paludarium (Wasser 20 bis 30 °C, etwas Land).
Wenn er heranwächst, verlässt er das Becken in einem Fischbeutel davor, wie jedes Tier, das aus seinem Becken herauswächst.
Eine ausgewachsene Meeresschildkröte wird abgelehnt.

## Grünfutter

Siehe [Griechische Landschildkröte](hermann_tortoise.md#grünfutter): `#bettacraft:turtle_greens` (Löwenzahn, Blütenteppich,
Gras, Farn, Margerite).

## Einstellungen

Common-Config `features.turtleBreeding` (aus: keine neuen Gelege; gelegte schlüpfen noch). Server-Config
`terrarium.incubationSpeed`, `terrarium.clutchIntervalDays`, `terrarium.brumation`, `terrarium.brumationBelow`.
