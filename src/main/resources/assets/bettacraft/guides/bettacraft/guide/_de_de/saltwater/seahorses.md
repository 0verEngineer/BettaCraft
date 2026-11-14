---
navigation:
  title: Seepferdchen
  icon: bettacraft:lined_seahorse_bucket
  parent: saltwater.md
  position: 5
item_ids:
  - bettacraft:lined_seahorse_bucket
  - bettacraft:dwarf_seahorse_bucket
  - bettacraft:lined_seahorse_spawn_egg
  - bettacraft:dwarf_seahorse_spawn_egg
---

# Seepferdchen

![Gestreifte Seepferdchen halten sich an Pflanzen und Korallen fest](../images/seahorses.png)

Seepferdchen sind Fische ganz eigener Art: Sie schwimmen langsam und **aufrecht**, steuern mit der kleinen Flosse auf dem
Rücken und verbringen den größten Teil des Tages damit, sich **mit dem Schwanz festzuhalten**. In einem Bettacraft-Becken
brauchen sie ein ruhiges eigenes Meerwasserbecken statt eines belebten Gesellschaftsbeckens.

| Seepferdchen | ‰ | °C | Besatz | Hinweise |
|---|---|---|---|---|
| Gestreiftes Seepferdchen (*Hippocampus erectus*) | 20–42 | 18–27 | 1,5 | das häufigste nachgezüchtete Seepferdchen; dunkel, gelb oder orange |
| Zwergseepferdchen (*Hippocampus zosterae*) | 20–42 | 21–28 | 0,3 | nur 2–3 cm, für kleine Becken |

## Was sie brauchen

* **Haltepunkte**: Aquarienpflanzen, Korallen oder Deko, die im Innenraum aus dem Bodengrund aufragen. Ein Seepferdchen
  schwimmt hin, rollt den Schwanz darum und bleibt dort; im Menü *hält es sich an einem Haltepunkt fest*. Ohne Haltepunkt
  liegt es am Boden und hat Stress (`seahorses.needHitchingPosts`). Seegras und Seetang aus Minecraft zählen auch; eine
  Seegraswiese mit ein paar Tangstängeln ist ideal.
* **Wenig Strömung**: Sie schwimmen schlecht. Strömungspumpen und starke Filter setzen sie unter Stress
  (`seahorses.maxFlow`, Standard 0,6; ein Außenfilter in einem Becken mit 16 Wasserblöcken gibt 0,5). Bei starker Strömung
  lassen sie ihren Haltepunkt kaum noch los.
* **Lebendfutter, oft**: Seepferdchen haben keinen Magen. Sie fressen nur **Artemia-Nauplien** und **Mückenlarven** (keine
  Pellets, Tabletten oder Infusorien) und wollen **zweimal am Tag** gefüttert werden (`seahorses.feedingsPerDay`). Neben
  schnellen Fressern (Bettas, Fische im freien Wasser) bekommen sie nur ab und zu ihren Anteil.
* **Ruhige Mitbewohner**: Doktorfische, Riffbarsche, Schwalbenschwänzchen und Clownfische sind zu schnell und
  aufdringlich; sie bedrängen Seepferdchen (das Menü zeigt *Wird gejagt*). Grundeln, Putzergarnelen, Turboschnecken und
  Einsiedlerkrebse passen.
* **Wasser**: Meerwasser oder starkes Brackwasser ab 20 ‰; das Gestreifte Seepferdchen mag es etwas kühler als die
  Rifffische (18–27 °C).

## Zucht: die Bruttasche

![Ein Männchen mit voller Bruttasche](../images/seahorse_pouch.png)

Seepferdchen vermehren sich im Becken, wenn ein gesundes, gefüttertes Paar in Wasser mit 21–25 °C lebt
(Zwergseepferdchen 24–27 °C). Das **Weibchen legt seine Eier in die Bruttasche des Männchens**; sein Bauch schwillt an, und
das Menü zeigt *Bruttasche voll* mit dem Fortschritt. Nach einigen Tagen (`seahorses.pouchDays`, Standard 4) **gebiert er**
die Jungen. Junge Seepferdchen sind groß genug, um vom ersten Tag an **Artemia-Nauplien** zu fressen; halte einen
[Artemia-Ansatz](../food.md) bereit. Lebendfutter bringt das Paar doppelt so schnell in Stimmung.

## Woher bekommen

* Mit dem **Aquarienkescher** in einem **lauwarmen** oder **warmen Ozean** (Gestreiftes Seepferdchen) bzw. einem warmen
  Ozean (Zwergseepferdchen), selten.
* Der **fahrende Händler** bietet ab und zu ein Paar unter seinen seltenen Angeboten an.
* `features.seahorses` schaltet sie ab (keine neuen aus Kescher und Händler).
