---
navigation:
  title: Terrarium und Paludarium
  icon: bettacraft:basking_lamp
  position: 77
---

# Terrarium und Paludarium

Jedes Becken kann auch ein **Terrarium** oder ein **Paludarium** sein. Du schaltest das nicht um: Das Becken erkennt
selbst, was es ist, an seinem Wasser und am **Land**, das du in seinem [Innenraum](interior.md) baust.

* **Aquarium**: Wasser und wenig oder kein Land. Alles wie bisher.
* **Paludarium**: Wasser und ein Landteil. Bau Erde, Sand, Steine oder Holz im Innenraum so, dass sie **über die
  Wasserlinie** ragen, und füll das Becken nur teilweise. Ab **15 %** Land (der Anteil des Bodens, dessen Oberkante über
  dem Wasser liegt; `terrarium.paludariumLandShare`) ist das Becken ein Paludarium.
* **Terrarium**: gar kein Wasser oder so viel Land (ab 90 %, `terrarium.terrariumLandShare`), dass das Wasser nur eine
  Schale ist.

Das Logbuch des Beckens vermerkt, wenn sich die Art ändert. Ein Terrarium oder Paludarium bekommt im Beckenmenü einen
neuen Reiter **Klima**; Aquarien sehen aus wie immer.

## Das Klima

Terrarien und Paludarien haben drei neue Werte. Jeder bewegt sich langsam auf das zu, was Raum, Becken und Geräte
ergeben (die Luft in etwa einer Stunde Spielzeit):

* **Lufttemperatur**: der Raum (Biom, Tag und Nacht) plus die [Heizmatten](terrarium/heat_mat.md) und die
  [Wärmespots](terrarium/basking_lamp.md). Im Paludarium zieht das Wasser die Luft zu seiner eigenen Temperatur (so
  wärmen die Wasserheizer die Luft mit). Nur die Heizmatten haben einen Thermostat: Sie heizen, solange die Luft unter der
  **Zieltemperatur** des Beckens liegt (die Einstellung im Übersichts-Reiter).
* **Luftfeuchte** (%): der Raum (trocken in Wüsten, feucht in Dschungeln und Sümpfen, mehr, wenn es auf ein offenes Becken
  regnet) plus die Wasseroberfläche (mehr bei warmem Wasser), die **Pflanzen auf dem Land** (Gras, Farne, Blumen, Moos,
  Laub, Aquarienpflanzen) und die [Sprühanlagen](terrarium/mister.md). Warme Luft hält mehr Wasser: Heizen senkt die
  Luftfeuchte.
* **UV** am Sonnenplatz: Ein leuchtender [Wärmespot](terrarium/basking_lamp.md) gibt UV 5; Sonnenlicht durch eine
  **offene Oberseite** tagsüber bis zu 3 (Glas und Deckel halten UV ab).

Ein **Deckel** (oder alles, was einen oberen Block abdeckt: eine Lampe, der Futterautomat, eine Sprühanlage) hält Wärme
und Feuchte drin; eine offene Oberseite lässt sie hinaus. Der Klima-Reiter zeigt, wie viele obere Blöcke abgedeckt sind.

Zwei weitere Werte folgen aus der Luft: Der **Boden** über den Heizmatten ist wärmer als die Luft, der **Sonnenplatz**
unter einem leuchtenden Wärmespot viel wärmer (12 K, `terrarium.baskingSpotLift`).

## Wo du die Werte siehst

* Im Reiter **Klima** des Beckenmenüs: die Art des Beckens, Land- und Wasseranteil, alle Werte mit Balken, die Geräte und
  was den Tieren fehlt.
* Der Übersichts-Reiter zeigt die Luft statt nur das Wasser.
* Das [Thermo-Hygrometer](terrarium/thermo_hygrometer.md) am Glas, die Info-Anzeigen (Jade, The One Probe, WTHIT) und die
  [Monitore](equipment/monitor.md) (Werte Lufttemperatur, Luftfeuchte, UV).

## Die Geräte

| Gerät | Wo | Wirkung |
|---|---|---|
| [Heizmatte](terrarium/heat_mat.md) | unter dem Becken oder an einer Wand | wärmt den Boden und etwas die Luft, mit der Zieltemperatur als Thermostat |
| [Wärmespot](terrarium/basking_lamp.md) | auf einem oberen Block (oder hängend darüber) | ein warmer Sonnenplatz mit UV auf dem Land darunter, Licht wie eine Lampe |
| [Sprühanlage](terrarium/mister.md) | auf oder neben dem Becken | sprüht, solange die Luftfeuchte unter ihrer Einstellung liegt; braucht Wasser |
| [Thermo-Hygrometer](terrarium/thermo_hygrometer.md) | am Glas | zeigt Lufttemperatur und Luftfeuchte |

## Terrarientiere

Ein Terrarientier lebt in der Art von Becken, die es braucht (eine Sumpfschildkröte im Paludarium, eine Landschildkröte
im trockenen Terrarium), und kommt **ohne den Mindest-Wasserstand** der Fische hinein. Es läuft auf dem Land des
Innenraums, klettert aus dem Wasser darauf, schwimmt oder läuft am Grund und geht zum **Sonnenplatz**, um sich
aufzuwärmen (öfter, wenn die Luft kalt ist). Menü und Logbuch sagen, was ihm fehlt: falsche Art von Becken, zu kalt oder
zu heiß (mit Sonnenplatz zählt die wärmste Stelle), zu trocken oder zu feucht, zu wenig UV, kein Sonnenplatz, zu wenig
Land oder Wasser, Wasser zu kalt oder zu warm. Das falsche Becken, zu kalt oder zu heiß kostet nach einem Tag Leben
(`terrarium.harmAfterDays`); der Rest stresst es nur. Es frisst, wenn das Becken gefüttert wird (von Hand oder mit dem
[Futterautomaten](equipment/feeder.md)).

Die Schildkröten:

| Schildkröte | Becken | Braucht |
|---|---|---|
| [Moschusschildkröte](terrarium/musk_turtle.md) | Paludarium, wenig Land | Wasser 20-28 °C |
| [Schmuckschildkröte](terrarium/slider_turtle.md) (Rotwange, Gelbwange) | Paludarium, halb Land | Wärmespot, UV; **wird groß** (12 Blöcke) |
| [Griechische Landschildkröte](terrarium/hermann_tortoise.md) | trockenes Terrarium | Wärmespot, viel UV, Grünzeug; Winterstarre bei Kälte |
| [Dreikielschildkröte](terrarium/reeves_turtle.md) | Paludarium | Sonnenplatz, etwas kühler |
| Schlüpflinge der Meeresschildkröte (Vanilla) | Paludarium | nur Schlüpflinge; ausgewachsene sind zu groß |

Sie legen Eier in Sand oder Erde auf dem Land, und die Nesttemperatur bestimmt das Geschlecht der Schlüpflinge:
[Schildkrötenzucht](terrarium/turtle_breeding.md). Wilde leben in Sümpfen und Flüssen (die Wasserschildkröten) und in
Savannen und trockenem Buschland (die Landschildkröte).

Die [Amphibien und Reptilien](terrarium/herps.md):

| Tier | Becken | Braucht |
|---|---|---|
| [Pfeilgiftfrösche](terrarium/dart_frog.md) (drei Arten) | Paludarium, viel Land | hohe Luftfeuchte, Fruchtfliegen; Kaulquappen im Wasser |
| [Schmuckhornfrosch](terrarium/horned_frog.md) | feuchtes Terrarium | sitzt eingegraben im Boden; Heimchen |
| [Leopardgecko](terrarium/leopard_gecko.md) | trockenes Terrarium | Sonnenplatz, Heimchen; Geschlecht nach Nesttemperatur |
| [Kronengecko](terrarium/crested_gecko.md) | Terrarium | klettert am Glas; nicht zu warm; Obst und Insekten |
| [Kornnatter](terrarium/corn_snake.md) | trockenes Terrarium, 8 Blöcke | Sonnenplatz, ein Beutetier pro Woche; Winterstarre |

Sie fressen [Futterinsekten](terrarium/feeder_insects.md) (Heimchen, Fruchtfliegen), Obst oder Beutetiere.

[Mobs im Glas](terrarium/mobs_in_glass.md): Ertrunkene, Zombies, Spinnen, kleine Schleime und ein paar weitere leben verkleinert
und harmlos in Becken (Mobglas, Köder, die Fang-Items anderer Mods). Sie droppen nichts.

## Wasserpflege im Terrarium

Ein Terrarium ohne Wasser hat keine Wasserwerte: Wasserqualität, Filter, Sauerstoff, CO2 und Wasserchemie stehen still,
und das Menü sagt das. Ein Paludarium behält sie alle für sein Wasser.

## Einstellungen

Server-Config `terrarium.*`: die Landanteile der Arten, die Stärke der Geräte, die Zeitkonstanten, das Wasser der
Sprühanlage, die Tage bis zum Schaden; `power.*`: Heizmatten nutzen den Energie-Schalter der Heizer (`heatMatFePerTick`),
Sprühanlagen ihren eigenen (`terrarium.misterRequiresEnergy`). Common-Config: `features.terrarium` (aus: jedes Becken ist ein
Aquarium), `features.terrariumDevices`, `features.turtles`, `features.turtleBreeding`, `features.wildTurtles`, `features.herps`,
`features.herpBreeding`, `features.wildHerps`, `features.mobsInGlass`. Schildkröten (und die Eierleger unter den Amphibien und Reptilien):
`terrarium.incubationSpeed`, `terrarium.clutchIntervalDays`, `terrarium.brumation`, `terrarium.brumationBelow`.
