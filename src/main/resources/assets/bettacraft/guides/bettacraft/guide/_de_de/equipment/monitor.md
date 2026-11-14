---
navigation:
  title: Monitor
  icon: bettacraft:monitor
  parent: equipment.md
  position: 110
item_ids:
  - bettacraft:monitor
---

# Monitor

<ItemImage id="bettacraft:monitor" scale="2" />

Ein Monitor ist ein Bildschirm für deine Becken. Stelle Monitor-Blöcke **nebeneinander**, mit dem Bildschirm in
dieselbe Richtung: Sie verbinden sich zu einem Bildschirm von bis zu **8 x 5 Blöcken** mit einem gemeinsamen Rahmen.
Ein Monitor, der an die Seite, Oberseite oder Unterseite eines anderen gesetzt wird, übernimmt dessen Richtung. Eine
Wand größer als 8 x 5 wird in mehrere Bildschirme geteilt.

Der Bildschirm findet von selbst **jedes Becken im Umkreis von 32 Blöcken** (`monitor.range`, gemessen bis zum
nächsten Punkt des Beckens) und zeigt seine Werte auf Kacheln. Jedes Becken heißt wie sein
[Namensschild](../aquarium/menu.md). Ab Werk zeigt der Bildschirm zwei Kacheln pro Becken: die **Temperatur** mit
einem kleinen Verlauf des letzten Tages und die **Wasserqualität**. Ein Rechtsklick auf einen Monitor zeigt seine Größe
und wie viele Becken er gefunden hat.

* Ein Becken außer Reichweite oder in einem nicht geladenen Chunk zeigt **„nicht geladen“** mit dem letzten Wert, den
  der Bildschirm gesehen hat.
* Die Werte gehen nur an Spieler, die den Monitor sehen können (innerhalb ihrer Sichtweite); sie kommen jede Sekunde.
* Jedes Becken zeichnet seine Werte der letzten 7 Spieltage auf (ein Punkt pro Spielstunde), auch wenn kein Monitor
  hinschaut.
* Mit einer Strom-Option (siehe [Strom](power.md)) braucht ein Monitor **1 FE pro Tick und Block**
  (`monitor.fePerBlock`); ohne FE zeigt er „Kein Strom“. Jeder Block des Bildschirms nimmt FE an.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:monitor" />
