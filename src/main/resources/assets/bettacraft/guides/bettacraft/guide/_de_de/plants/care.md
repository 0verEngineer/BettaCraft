---
navigation:
  title: Pflege, Schnitt und Ableger
  icon: minecraft:shears
  parent: plants.md
  position: 10
---

# Pflege, Schnitt und Ableger

![Im Aquarium: die Pflanzen in voller Größe](../images/aquatic_plants_interior.png)

## Wachstum

* Pflanzen wachsen mit **Licht** an der Oberfläche (eine eingeschaltete [Aquarienlampe](../equipment/light.md) oder
  Tageslicht), **Nährstoffen** (dem Abfall der Tiere, siehe [Wasserqualität](../aquarium/water_quality.md)) und einer
  Wassertemperatur von 20–30 °C. Aufsitzerpflanzen, Moos und Cryptocorynen kommen mit wenig Licht aus; Stängelpflanzen
  und Monte Carlo brauchen viel Licht.
* Sie wachsen **auch, wenn niemand im Inneren ist**: das Aquarium merkt sich jede Pflanze und schreibt die Blöcke einer
  neuen Stufe, wenn sie fällig ist. Du siehst die Miniatur wachsen.
* **Rote Pflanzen werden bei mehr Licht röter**: Rotala an den Spitzen, Ludwigia, Alternanthera, die braune Crypt, die
  Brownie Red und der Tigerlotus. Die Flamingo-Crypt bleibt weißlich rosa und wird kräftiger.

![Rote Pflanzen unter viel und unter wenig Licht](../images/aquatic_plants_red.png)

* Stängelpflanzen wachsen bis zur Oberfläche und an ihr entlang. Zusammen mit dichten [Schwimmpflanzen](floating_plants.md)
  beschatten sie das Aquarium: das Menü warnt, die Pflanzen darunter wachsen langsamer. Schneide sie zurück.
* Höchstens 48 Pflanzen wachsen in einem Aquarium; Seitentriebe und Ausläufer halten etwas Abstand zueinander.

## Schneiden, Teilen, Herausnehmen

* Eine **Schere** an einem Pflanzenblock im Inneren schneidet die Pflanze um eine oder zwei Stufen zurück; du bekommst
  **Ableger**. Ein gepflanzter Ableger wächst zu einer neuen Pflanze heran: so kommst du vor allem an mehr Pflanzen.
* Eine **Schere am Aquarium von außen** schneidet die Pflanze, die du in der Miniatur ansiehst (Deko davor verdeckt sie).
* **Den Wurzelblock abbauen** (den untersten Block, wo du gepflanzt hast) nimmt die ganze Pflanze heraus; das Item behält
  seine Größe und lässt sich woanders einpflanzen.
* **Ein Rhizomstück** einer Aufsitzerpflanze abbauen teilt sie: die Hälfte kommt als neue Pflanze heraus.
* Stängelpflanzen bilden **Seitentriebe**, Vallisnerie, Monte Carlo und die Cryptocorynen schicken selbst **Ausläufer**.
* Wird ein Aquariumblock abgebaut, fallen die darin wurzelnden Pflanzen als Items heraus.

## Was sie bewirken

* Pflanzen **nehmen Abfall auf**, solange sie Licht haben: je mehr Pflanzenmasse, desto länger bleibt das Wasser sauber.
* Dichte Pflanzen sind **Verstecke**: nach dem Laichen jagt das Männchen das Weibchen seltener (bis zu 60 % weniger).
* Ein gut bepflanztes Aquarium (Pflanzen von mindestens 3 % des Wasservolumens) macht das Männchen etwas schneller
  bereit für ein Nest (1,1-fach).

## Das Aussehen

Jede Pflanze hat **echte Blätter** statt Blattblöcken, im Inneren und in der Miniatur außen am Aquarium, und sie wiegen
sich im Wasser. Die Blöcke sind weiter da (Schere und Abbauen funktionieren wie bisher), sie werden nur anders
gezeichnet. Aus mehr als 24 Blöcken Entfernung zeigt die Miniatur die Pflanzen wieder als Pixel
(`render.miniaturePlantMeshDistance`). In den Mod-Einstellungen (*Darstellung*, `render.plantMeshes`) kann jeder Spieler
zu den Blöcken zurückschalten; `features.visuals.plantMeshes` schaltet es für das ganze Modpack ab.

![Aufsitzer, Rosetten und Stängelpflanzen](../images/plants_mesh_all.png)

![Dieselben Pflanzen in der Miniatur außen am Aquarium](../images/plants_mesh_outside.png)

Alle Werte lassen sich in der Server-Config ändern (`interiorPlants.*`).
