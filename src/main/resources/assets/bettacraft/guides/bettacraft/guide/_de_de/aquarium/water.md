---
navigation:
  title: Wasser
  icon: minecraft:water_bucket
  parent: aquarium.md
  position: 10
---

# Wasser

Ein Aquarium enthält **echtes Vanilla-Wasser**, gemessen in Millieimern: 1 Wasserblock = 1000 mB. Das Wasser füllt das
Aquarium von unten nach oben, die Oberfläche liegt auf der genauen Höhe, ruhig und ohne Strömung. Wasser an anderen
Stellen der Welt bleibt unverändert.

| Aktion | Wasser |
|---|---|
| Wassereimer auf das Aquarium | +1000 mB |
| Leerer Eimer auf das Aquarium | −1000 mB, du bekommst einen Wassereimer |
| Betta-Eimer | +1000 mB und der Fisch |
| Fischtüte | +100 mB und der Fisch |
| Fisch mit Eimer entnehmen (Menü) | −1000 mB |
| Fisch mit Fischtüte entnehmen (Menü) | −100 mB |

* Tiere lassen sich ab **50 %** Füllung einsetzen (`tank.minFillToInsert`).
* Solange Tiere darin leben, kannst du das Aquarium nicht unter diese Grenze ablassen.
* Das Aquarium ist oben offen, aber die Wände sind dicht: Das Wasser fließt nicht aus.
* In großen Aquarien kannst du selbst schwimmen. Die Bewohner können nicht herausspringen: Die Oberseite wirkt für sie
  wie ein Deckel, für Spieler nicht.

Das Aquarium-Menü zeigt das Volumen als „Wasser 12000 / 16000 mB (75 %)“.

Wasser wird außerdem **verbraucht und schmutzig**: Offene Aquarien verdunsten langsam, und Abfall senkt die
Wasserqualität, bis du einen Teil des Wassers wechselst. Siehe [Wasserqualität und Verdunstung](water_quality.md).
Leitungen und Pumpen anderer Mods können ein Aquarium wie Eimer füllen und leeren.
