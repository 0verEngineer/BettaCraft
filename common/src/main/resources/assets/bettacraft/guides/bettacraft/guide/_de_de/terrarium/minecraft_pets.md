---
navigation:
  title: Minecraft-Tiere als Haustiere
  icon: minecraft:carrot
  parent: terrarium.md
  position: 72
feature: foreignAnimals.vanillaPets
---

# Minecraft-Tiere als Haustiere

Auch die friedlichen Tiere von Minecraft leben in unseren Terrarien und Paludarien, und zwar **in echter Größe**: ein Huhn in
einem kleinen trockenen Terrarium, ein Frosch im Paludarium, eine Kuh nach der Größenregel der Großtiere in einem großen
Terrarium. Im Becken bewegt sie unser Läufer (ihre eigene KI liefe gegen das Glas), unsere Pflege füttert sie und unsere Zucht
vermehrt sie, wie die Landtiere anderer Mods ([Landtiere anderer Mods](foreign_land_animals.md)).

![Hühner, Kaninchen, eine Katze, ein Fuchs und ein Papagei in einem Terrarium auf Gras](../images/minecraft_pets.png)

## Welche Tiere

| Tier | Becken | Braucht | Zucht |
|---|---|---|---|
| Huhn | trockenes Terrarium mit Deckel (es flattert) | Samen, Weizen, Insekten | die Henne legt Eier in Sand oder Erde, Küken schlüpfen |
| Kaninchen | trockenes Terrarium | **Gras** an Land (es grast dort); Karotten, Löwenzahn | Junge (2–6) |
| Biene | trockenes Terrarium mit Deckel | **Blumen** (ihr Nektar) und einen **Bienenstock** im Innenraum | junge Bienen aus dem Stock |
| Frosch | Paludarium (er braucht Wasser) | Futterinsekten, Schleimbälle | junge Frösche |
| Katze, Ozelot | trockenes Terrarium | roher Fisch | Kätzchen |
| Fuchs | trockenes Terrarium | Süßbeeren, rohes Hühnchen und Kaninchen | Welpen |
| Papagei | trockenes Terrarium mit Deckel | Samen, Obst | Eier |
| Gürteltier | trockenes, warmes Terrarium | Spinnenaugen, Insekten; gräbt sich ein | vier Junge auf einmal |
| Fledermaus | feuchtes Terrarium mit Deckel | Insekten, Obst | ein Junges |
| Wolf (1 m) | trockenes Terrarium ab 12 × 6 × 3 | Fleisch | Welpen |

![Frösche in einem Paludarium: zwei am Ufer, einer im Wasser](../images/minecraft_frogs.png)

**Große** nach der Größenregel (ihre Länge in Metern; gefangen mit dem großen Kescher und der Transportwanne, ab 1,2 m ist die
Box zu klein): Schwein (1,2 m), Ziege (1,2 m) und Schaf (1,3 m) vermehren sich noch; Kuh und Mooshroom (2 m), Pferd (2,2 m),
Esel und Maultier (1,9 m), Lama (2 m), Panda (1,6 m) und Eisbär (2,2 m, kalt) sind zu groß, um sich im Becken zu vermehren.
Schafe, Ziegen, Kühe, Pferde, Esel, Maultiere und Lamas grasen: sie brauchen Gras an Land.

**Abgelehnt** mit Grund: Kamel und Schnüffler (viel zu groß), der Schreiter (er lebt auf Lava) und Skelett- und Zombiepferd
(untot). Fische, Tintenfische, Axolotl, Delfine, Schildkröten und Kaulquappen haben schon lange ihre eigene Pflege.

## Ihre Bedürfnisse

* **Gras**: ein Grasblock als Boden oder Gras und Farne darauf, auf mindestens einem Zehntel des Landes im Innenraum.
* **Blumen**: mindestens drei Blumen an Land.
* **Ein Bienenstock**: ein Bienenstock oder Bienennest im Innenraum: dort ist er ein Mini-Bienenstock, ein Sechzehntel des
  Beckenblocks.

Fehlt etwas, steht es als Problem im Menü und im Beckenlog, und das Tier vermehrt sich nicht. Ein Grasfresser (Kaninchen,
Schaf, Biene …) wird von seinem Gras oder seinen Blumen satt, solange sie da sind; sonst von Hand oder aus dem
Futterautomaten füttern.

## Fangen

* Die **Terrarienbox** fängt die kleinen (Rechtsklick; ein zahmes nur für seinen Besitzer), der **große Kescher** und die
  **Transportwanne** die großen.
* **Köder**: mit ihrem Futter auf ein Becken mit **offener Oberseite** rechtsklicken, wenn kein Tier darin es frisst: einen Tag
  lang läuft ein passendes Tier im Umkreis von 12 Blöcken zum Becken und geht hinein, wenn die Oberseite in Sprunghöhe liegt.
  Nicht für die Flieger (sie brauchen einen Deckel) und nicht für die großen.

## Keine Farm

Wie die [Mobs im Glas](mobs_in_glass.md) lässt ein Minecraft-Tier, das in einem unserer Becken gelebt hat (oder dort geboren
wurde), beim Tod nichts fallen und gibt keine Erfahrung, egal wodurch es stirbt. Schere, Eimer, Schüssel und Pinsel bewirken
nichts, und ein Huhn legt im Becken keine Eier. Junge gibt es nur durch unsere Zucht.

## Einstellungen

Der Schalter `features.foreignAnimals.vanillaPets` (in der Common-Config) schaltet es ab: dann kommt kein Minecraft-Tier in
ein Becken, die Box nimmt keines, und die Ablehnungen oben entfallen auch; Tiere in Becken bleiben. Die Profile sind die
Dateien `data/bettacraft/bettacraft/foreign_animals/minecraft_*.json`; ein Datenpaket kann sie wie jedes
[Profil](../settings/foreign_profiles.md) ändern, mit den Schlüsseln `needs` und `grazes`.
