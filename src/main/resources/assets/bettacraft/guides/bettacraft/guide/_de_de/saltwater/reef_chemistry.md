---
navigation:
  title: Riffchemie
  icon: bettacraft:alkalinity_canister
  parent: saltwater.md
  position: 15
---

# Riffchemie („Balling light“)

Steinkorallen bauen ihr Skelett aus dem Wasser. Neben dem **Kalzium** des [Meerwassers](water.md) brauchen sie
**Alkalinität** (Karbonathärte) und **Magnesium**, und **Spurenelemente** geben allen Korallen ihre kräftigen Farben.
Meerwasser bringt alles mit; ein Riffbecken mit wachsenden Steinkorallen verbraucht es, und Wasserwechsel oder die
[Dosierpumpe](dosing_pump.md) bringen es zurück. Das ist die klassische Drei-Komponenten-Methode der Riffaquaristik
(nach Hans-Werner Balling), einfach gehalten.

| Wert | Meerwasser | Gut | Was passiert, wenn er niedrig ist |
|---|---|---|---|
| Kalzium (Ca) | 420 mg/L | 380–450 | unter 380 wachsen die Steinkorallen langsamer, unter 250 gar nicht |
| Alkalinität (KH) | 8 °dKH | 7–11 | unter 6 langsamer, unter 5 halb so schnell, unter 3 gar nicht; der pH sinkt mit |
| Magnesium (Mg) | 1300 mg/L | 1250–1400 | unter 1150 fallen Kalzium und Alkalinität aus: langsamer, unter 1000 viel langsamer |
| Spurenelemente | 50 % | über 60 % | nichts Schlimmes; über dem Wert von frischem Meerwasser leuchten die Korallen in ihren kräftigen Farben |

## Wohin es geht

* Jede Wachstumsstufe einer Steinkoralle nimmt Kalzium (40 mg/L in einem Wasserblock, `saltwater.calciumPerStage`),
  **die passende Alkalinität** (im Spiel etwa 29 mg/L Kalzium pro °dKH, damit Kalzium und Alkalinität des
  Meerwassers etwa gleichzeitig knapp werden) und etwas Magnesium. In einem großen Becken verteilt es
  sich auf mehr Wasser.
* **Magnesium** sinkt außerdem langsam in jedem Meerwasserbecken (`dosing.magnesiumDropPerDay`, 1 mg/L am Tag).
* **Spurenelemente** sind in wenigen Tagen verbraucht (`dosing.traceUsePerDay`, 4 % am Tag).
* Der **pH** des Meerwassers folgt der Alkalinität: 8 °dKH halten ihn bei 8,2, weniger Alkalinität lässt ihn sinken.
* Die Verdunstung lässt Kalzium, Alkalinität und Magnesium zurück; Nachfüllen mit Süßwasser verdünnt sie wieder.

## Zurückbringen

* **Wasserwechsel mit Meerwasser** bringen jeden Wert wieder Richtung Meerwasser.
* Der **Kalziumzusatz** hebt das Kalzium von Hand (siehe [Meerwasser](water.md)).
* Die [Dosierpumpe](dosing_pump.md) dosiert alle vier jeden Tag: Kalzium und Alkalinität in etwa gleichen mL, etwas
  Magnesium, Spurenelemente für die Farben.

Der Reiter Wasser des [Beckenmenüs](../aquarium/menu.md) zeigt die Alkalinität neben dem Kalzium; mit der Maus über der
Zeile stehen Magnesium und Spurenelemente.

`features.reefDosing` (Common-Config) schaltet das ab: Alkalinität und Magnesium bremsen die Korallen nicht mehr, die
Werte stehen still und die Pumpe dosiert nicht.
