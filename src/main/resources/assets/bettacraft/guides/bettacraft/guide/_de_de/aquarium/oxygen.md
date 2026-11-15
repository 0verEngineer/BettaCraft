---
navigation:
  title: Sauerstoff
  icon: minecraft:bubble_coral
  parent: aquarium.md
  position: 35
---

# Sauerstoff

Jedes Aquarium hat einen **Sauerstoff**-Wert in mg/L, angezeigt im Reiter **Technik** des Aquarium-Menüs (mit Balken;
der weiße Strich ist die volle Sättigung).

**Er sinkt durch**

* **warmes Wasser:** je wärmer, desto weniger Sauerstoff hält das Wasser (etwa 8,3 mg/L bei 25 °C, 7,5 bei 30 °C), und
  die Tiere verbrauchen mehr;
* **viele Tiere** in wenig Wasser;
* verrottende Abfälle (schlechte Wasserqualität);
* **Nacht in bepflanzten Aquarien:** Pflanzen bilden bei Licht Sauerstoff und verbrauchen im Dunkeln etwas; ohne Licht
  (oder nachts unter dem [Tageslicht-Timer](../equipment/light.md)) verliert ein dicht bepflanztes Aquarium etwas.

**Er steigt durch**

* die Wasseroberfläche (ein flaches, breites Aquarium bekommt mehr als ein tiefes);
* [Ausströmer](../equipment/air_stone.md) (am meisten), [Schwammfilter](../equipment/sponge_filter.md), Filterauslässe
  und Strömungspumpen (je nach [Leistungsregler](../equipment/regulator.md));
* Pflanzen bei gutem Licht.

**Wirkung:** ab 5 mg/L ist er gut, unter 5 niedrig, unter 2 kritisch (das Logbuch vermerkt es). Unter **3,5 mg/L** sind
Fische, Garnelen und Krebse **gestresst** wie bei schlechtem Wasser (sie werden krank und sterben, wenn es anhält);
Schnecken merken es wenig, Krallenfrösche gar nicht. **Bettas und Zwergfadenfische** sind Labyrinthfische: sie atmen
Luft an der Oberfläche und leiden erst unter 1 mg/L.

Starke Belüftung **treibt auch CO2 aus** (steht im Menü); das wird wichtig, sobald CO2 für die Pflanzen dosiert werden
kann.

Ein Server kann den Sauerstoff abschalten (`features.waterChemistry.oxygen`) und jeden Wert ändern (`aeration.*`).
