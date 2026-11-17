---
navigation:
  title: Griechische Landschildkröte
  icon: bettacraft:hermann_tortoise_box
  parent: terrarium.md
  position: 52
item_ids:
  - bettacraft:hermann_tortoise_box
  - bettacraft:hermann_tortoise_spawn_egg
  - bettacraft:terrarium_box
---

# Griechische Landschildkröte

Die Griechische Landschildkröte (*Testudo hermanni*, etwa 18 cm) lebt im trockenen, sonnigen Buschland und auf den Wiesen
rund ums Mittelmeer: ein hoch gewölbter Panzer aus ockerfarbenen Schilden mit schwarzen Flecken. Sie schwimmt nie; sie lebt
im **trockenen Terrarium**.

## Pflege

| | |
|---|---|
| Becken | ein [Terrarium](../terrarium.md) ohne Wasser (eine Wasserschale geht: 90 % Land oder mehr); 8 Blöcke oder mehr für ein ausgewachsenes Tier |
| Luft | 20 bis 32 °C; Luftfeuchte 35 bis 75 % |
| UV, Sonnenplatz | braucht einen [Wärmespot](basking_lamp.md) (UV 4 oder mehr: so viel gibt nur eine Lampe) |
| Boden | Sand oder Erde (für die Eier); [Heizmatten](heat_mat.md) halten den Boden warm |
| Futter | **nur Grünzeug**: Löwenzahn, Klee (Blütenteppich), Kräuter (Gras, Farn), Margerite |
| Besatz | ein Schlüpfling 0,5 Bettas, wächst auf 5 |

* Sie reist in einer **Terrarienbox**, nicht im Wasser: Rechtsklick mit einer leeren
  <ItemLink id="bettacraft:terrarium_box" /> auf die Landschildkröte, dann Rechtsklick mit der vollen Box aufs Becken (oder
  auf einen Block, um sie freizulassen). Die leere Box kommt zurück.
* Füttere das Grünzeug per Rechtsklick aufs Becken (die hungrigste Landschildkröte frisst eins) oder leg es in einen
  [Futterautomaten](../equipment/feeder.md).
* Sie läuft langsam übers Land und geht zum Sonnenplatz, um sich aufzuwärmen; sie klettert Stufen von einem viertel Block.

## Grünfutter

Echte Griechische Landschildkröten fressen Wildkräuter, Löwenzahn und Klee. Minecraft hat keinen Klee, darum steht der
Futter-Tag `#bettacraft:turtle_greens` dafür: **Löwenzahn**, **Blütenteppich** (für den Klee), **Gras** und **Farn** (für
die Kräuter) und die **Margerite**. Modpacks können eigenes Grünzeug in den Tag aufnehmen. Schmuck- und
Dreikielschildkröten fressen es auch.

## Winterstarre

In der Natur verschläft die Landschildkröte den Winter. Im Becken tut sie dasselbe: Fällt die **Luft unter 12 °C**
(`terrarium.brumationBelow`), geht sie in **Winterstarre**: Kopf und Beine in den Panzer gezogen, sie bewegt sich nicht,
frisst nicht und wird nicht hungriger, und die Kälte schadet ihr nicht. Sie erwacht, wenn die Luft wieder 2 K wärmer ist.
Nur **Frost** (Luft unter 2 °C) schadet ihr trotzdem. Mit `terrarium.brumation` aus schadet ihr die Kälte wie jeder anderen
Schildkröte.

Der Klima-Reiter zeigt „in Winterstarre“; das Logbuch notiert Anfang und Ende. Ein Weibchen in Winterstarre legt keine Eier.

## Zucht

Sie legt ihre Eier in Sand oder Erde (siehe [Schildkrötenzucht](turtle_breeding.md)). Pivottemperatur 31,5 °C: ein Nest
braucht Heizmatten unter dem Becken, um warm genug für Weibchen zu werden.

## Bezug

* <ItemLink id="bettacraft:hermann_tortoise_box" />: **Wild** in Savannen und trockenem Buschland, einzeln (Spawn-Gewicht 4) (Config `features.turtles.wildSpawns`).

Herstellung:

<RecipesFor id="bettacraft:terrarium_box" />
