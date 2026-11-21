---
navigation:
  title: Einrichtung und Deko
  icon: bettacraft:decor_castle
  position: 100
feature: interior.enabled
---

# Einrichtung und Deko

Jedes Aquarium hat einen eigenen Raum: seinen **Innenraum**. Geh hinein, bau mit beliebigen Blöcken, was dir gefällt,
und das Aquarium zeigt es als **Miniatur** hinter dem Glas.

![Im Aquarium: eine Burg auf einem Boden aus Kies und Sand](images/interior.png)

## Betreten und verlassen

* **Betreten:** **Schleichen + Rechtsklick** mit leerer Hand auf das Aquarium, oder der Knopf *Innenraum* im
  Aquarium-Menü. Beim ersten Mal wird der Raum vorbereitet („Der Innenraum wird vorbereitet ... 40 %“), das dauert einen
  Moment.
* **Verlassen:** **3 Sekunden schleichen und dabei steil nach oben schauen** (an die Decke) oder
  `/bettacraft interior leave`. Du kehrst dorthin zurück, wo du standest. Schleichen allein zählt nicht, so kannst du
  schleichend Bodengrund setzen, ohne hinauszufliegen; Setzen oder Abbauen eines Blocks lässt die 3 Sekunden neu
  beginnen. Ein kleiner Hinweis unten links erinnert daran („Schleichen + nach oben schauen: verlassen“) und füllt beim
  Halten einen Balken; die Client-Config `hud.interiorLeaveHint` schaltet den Hinweis ab (der Balken bleibt).
* Wer sich im Innenraum ausloggt, bleibt dort. Mehrere Spieler können gleichzeitig im selben Innenraum sein; jeder sieht,
  was die anderen bauen, und die Miniatur draußen folgt.
* Solange jemand drin ist, lässt sich das Aquarium **nicht abbauen**: weder von Hand (keiner seiner Blöcke) noch durch
  Explosionen, und Kolben bewegen Aquarium-Blöcke ohnehin nicht. Die Meldung nennt, wer drin ist.

![Der Verlassen-Hinweis unten links](images/interior_leave_hint.png)
* Bei einem gesperrten Aquarium dürfen nur Besitzer und Operatoren hinein und bauen.

## Größe

Beim Standard-**Maßstab 16** wird ein Aquarium-Block zu 16x16x16 Innenraum-Blöcken, ein Innenraum-Block ist also ein
Pixel des Aquariums. Ein 2x1x1-Aquarium hat einen Innenraum von 32x16x16 Blöcken. Server können für neue Aquarien
Maßstab 8 oder 4 wählen (`interior.defaultScale`); ein Aquarium behält den Maßstab, mit dem es gebaut wurde. Bei einem Aquarium mit
[freier Form](aquarium/shapes.md) umfasst der Innenraum den umschließenden Quader; die Teile außerhalb der Form sind
abgetrennt.

## Bauen

* Bau mit jedem Block aus dem Spiel und aus anderen Mods. Wände und Boden des Raums sind unzerstörbar.
* Der Innenraum ist **trocken**, aber Wasserpflanzen funktionieren dort: Seegras, Seetang, Korallen und Seegurken lassen
  sich setzen und gehen nicht kaputt; Korallen sterben nicht ab.
* **Seerosen** (`#bettacraft:floating_plants`) lassen sich überall setzen und schweben. Unter ihnen bauen Bettas ihr
  Schaumnest.
* Betten und Seelenanker lassen sich im Innenraum nicht benutzen.
* **Kein Aquarium im Aquarium:** Aquarium-Blöcke (auch die Fish Tanks von Better Fish Tanks, Block-Tag
  `#bettacraft:not_in_tank_interior`) lassen sich hier nicht setzen, weder von Hand noch von Maschinen. Per Befehl oder
  Bauwerkzeug gesetzt, zerbricht der Block sofort und fällt als Item heraus.
* Bettas schwimmen um feste Deko herum. Andere Tiere schwimmen hindurch.
* [Heizstäbe](equipment/heaters.md) und [Innenfilter](equipment/filters.md) werden hier aufgestellt.

## Die Miniatur

Die Miniatur wird im Hintergrund gebaut und Block für Block aktualisiert. Block-Entities wie Truhen oder Schilder werden
nicht gezeigt. Sehr große Bauten enden an einem Limit (`interior.maxRenderedBlocks`); der Tooltip des Innenraum-Knopfs im Menü meldet
dann „Render-Limit erreicht“.

## In diesem Kapitel

<SubPages icons={true} />
