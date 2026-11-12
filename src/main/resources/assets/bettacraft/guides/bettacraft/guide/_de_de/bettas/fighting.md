---
navigation:
  title: Kämpfe, Flaring und Raubfische
  icon: minecraft:iron_sword
  parent: bettas.md
  position: 50
---

# Kämpfe, Flaring und Raubfische

## Kämpfe

Bettas heißen nicht umsonst *Siamesische Kampffische*: **Zwei adulte Männchen im selben Aquarium kämpfen**, bis eines
stirbt. Ein Kampf dauert etwa einen Ingame-Tag (`combat.fightDays`), du hast also Zeit zu reagieren. Aquarium-Menü, Log
und Jade warnen sofort („Männchen kämpfen! Trennen!“). Nimm eines der beiden mit Eimer oder Fischtüte heraus.

* Weibchen kämpfen nicht miteinander; ein Männchen jagt Weibchen nur, solange es ein [Schaumnest](../breeding.md)
  bewacht.
* Wilde Bettas kämpfen in der Natur nicht. Ein wildes Männchen im Aquarium schon.
* Server können Kämpfe abschalten (`combat.malesFight = false`).

## Flaring

Sieht ein adultes Männchen ein anderes adultes Männchen im Umkreis von 6 Blöcken, **flart** es: Es spreizt Flossen und
Kiemendeckel, und seine Farben werden kräftiger. Glas und Wasser versperren die Sicht nicht; zwei Männchen in
benachbarten Aquarien flaren sich also an, ohne zu kämpfen. Undurchsichtige Blöcke zwischen den Aquarien verhindern es.

## Raubfische

Tiere im Entity-Tag `#bettacraft:predators` jagen im Aquarium: der Vanilla-**Kugelfisch** und, falls installiert,
Piranha und Anglerfisch (Naturalist) sowie Battlegill, Wrecker und Devilfish (Fish of Thieves). Etwa viermal am Tag
(`combat.predatorAttacksPerDay`) greift ein Raubfisch das kleinste Tier an: Junge Bettas und winzige Tiere werden
gefressen, Adulte verletzt (der Kugelfisch vergiftet sie). Der Zucht-Tab warnt „Ein Raubfisch jagt die kleinen
Fische!“.
