---
navigation:
  title: Axolotl
  icon: minecraft:axolotl_bucket
  parent: invertebrates.md
  position: 45
---

# Axolotl

![Axolotl in einem kühlen Becken mit ihren Eiern an den Pflanzen](../images/axolotl_tank.png)

Der **Vanilla-Axolotl** kann in einem Bettacraft-Aquarium leben (mit seinem Eimer einsetzen). Er bleibt das Vanilla-Tier
mit Modell und Eimer, andere Mods funktionieren also weiter mit ihm, aber im Becken bekommt er **unsere Pflege und Zucht**
(Einstellung `features.foreignAnimals.axolotlCare`).

## Kühles Wasser

* Axolotl stammen aus den kalten Seen Mexikos: **15 bis 20 °C** sind richtig. Die meisten Biome sind wärmer, das Becken
  braucht also einen [Kühler](../equipment/coolers.md) (oder ein kaltes Biom).
* Ab **21 °C** sind sie gestresst und vermehren sich nicht (das Beckenprotokoll sagt es). Über **23 °C** verlieren sie
  Gesundheit, umso mehr, je wärmer es ist (`invertebrates.axolotlHeatDamage`, 1 Lebenspunkt je Grad und Tag). Ein
  tropisches Betta-Becken (26 °C) ist nichts für sie.
* Sie sind Räuber: Sie jagen kleine Fische und Brut, wie in Vanilla. Am besten unter sich halten.

## Zucht

* Zwei erwachsene Axolotl in Wasser von 15–20 °C legen **Eier an die Pflanzen** des Beckens (fein- und breitblättrige
  Pflanzen, Moos); ohne Pflanzen gibt es keine Eier. Ein Gelege zur Zeit, danach ruhen die Eltern eine Weile.
* Nach etwa 1,5 Tagen (`invertebrates.axolotlEggDays`) schlüpfen 3 bis 6 **Larven**: Baby-Axolotl, die wie in Vanilla
  heranwachsen. Der Kescher nimmt die Eier ab.
* Die Zuchtgeschwindigkeit ist `invertebrates.axolotlBreeding`.

## Farben

Die fünf Vanilla-Farben vererben sich nach Mendel, wie die Garnelenfarben: **leuzistisch** (rosa), **Gold**, **Cyan** und
**Blau** sind jeweils ein rezessives Allel auf einem eigenen Locus gegen die **Wildfarbe** (braun). Ein Axolotl zeigt eine
Farbe nur, wenn er sie doppelt trägt; zwei verschiedene Farben geben wildfarbene Junge, die beide tragen. Bei mehreren
Farben zeigt sich die seltenere: Blau vor Cyan vor Gold vor leuzistisch.

* **Blau ist selten**, wie in Vanilla: Ab und zu trägt ein wilder Axolotl es verdeckt. Zwei Träger geben etwa ein Viertel
  blaue Junge.
* Ein Axolotl, der zum ersten Mal in ein Becken kommt, bekommt Gene nach seiner Farbe (doppelt), manchmal mit einem
  verdeckten Allel. Die [Lupe](../equipment/magnifier.md) am Becken zeigt die Farbgene jedes Axolotls (im Chat).
* Über das Beckenmenü herausgenommen, behält der Eimer die Gene. Ein Vanilla-Eimer in der Welt nicht: Der Axolotl bekommt
  beim nächsten Mal neue Gene nach seiner Farbe.

## Bezug

* Vanilla: Axolotl leben in **üppigen Höhlen**; mit einem Wassereimer fangen.
* **Zucht** im Aquarium (zwei Erwachsene, kühles Wasser, Pflanzen).
