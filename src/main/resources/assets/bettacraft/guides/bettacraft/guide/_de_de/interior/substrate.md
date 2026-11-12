---
navigation:
  title: Bodengrund
  icon: minecraft:gravel
  parent: interior.md
  position: 10
---

# Bodengrund

Der Boden eines Innenraums besteht aus besonderen Bodenblöcken. Gib jedem davon das Aussehen eines anderen Blocks:

* **Schleichen + Rechtsklick** auf einen Bodenblock **mit einem Block** in der Hand: Der Bodenblock übernimmt dessen
  Textur (Sand, Kies, Moos, was du willst). Nur die Oberfläche ändert sich, es wird kein Block daraufgesetzt. Misch nach
  Belieben, Block für Block.
* **Schleichen + Rechtsklick** mit **leerer Hand** entfernt den Bodengrund wieder.
* Ein normaler Rechtsklick baut wie gewohnt auf den Boden.

Jeder belegte Bodenblock **kostet einen Block**; beim Ersetzen oder Entfernen bekommst du ihn zurück. Server können den
Bodengrund kostenlos machen (`interior.substrateCostsBlock = false`).

Nur volle Blöcke mit normalem Modell und ohne Block-Entity gehen. Der Block-Tag `#bettacraft:substrate_blacklist`
schließt weitere Blöcke aus.

Im Aquarium erscheint der Bodengrund als dünne Schicht ganz unten. Der Innenraum-Tab im Menü zählt die belegten
Bodenblöcke.
