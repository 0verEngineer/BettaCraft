---
navigation:
  title: Seepferdchen
  icon: bettacraft:lined_seahorse_bucket
  parent: saltwater.md
  position: 60
---

# Seepferdchen

![Gestreifte Seepferdchen halten sich an Pflanzen und Korallen fest](../images/seahorses.png)

Seepferdchen sind Fische ganz eigener Art: Sie schwimmen langsam und **aufrecht**, steuern mit der kleinen Flosse auf dem
Rücken und verbringen den größten Teil des Tages damit, sich **mit dem Schwanz festzuhalten**. In einem Bettacraft-Becken
brauchen sie ein ruhiges eigenes Meerwasserbecken statt eines belebten Gesellschaftsbeckens.

## Arten

| Art | °C | Schwarm | Wasser | Zucht |
|---|---|---|---|---|
| <ItemImage id="bettacraft:lined_seahorse_bucket" /> [Gestreiftes Seepferdchen](lined_seahorse.md) | 18–27 | – | Meerwasser, auch starkes Brackwasser | Bruttasche: das Männchen trägt die Eier |
| <ItemImage id="bettacraft:dwarf_seahorse_bucket" /> [Zwergseepferdchen](dwarf_seahorse.md) | 21–28 | – | Meerwasser, auch starkes Brackwasser | Bruttasche: das Männchen trägt die Eier |

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
