---
navigation:
  title: Kämpfe, Flaring und Raubfische
  icon: minecraft:iron_sword
  parent: bettas.md
  position: 50
item_ids:
  - bettacraft:mirror_stick
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

### Spiegelstab

<RecipeFor id="bettacraft:mirror_stick" />

Nimm den <ItemLink id="bettacraft:mirror_stick" /> in eine Hand und sieh ein Männchen an: Jedes adulte Männchen bis 6
Blöcke vor dir, das den Spiegel sehen kann (auch durch das Glas), flart vor seinem Spiegelbild, hält still und zeigt
ihm seine Breitseite: Bettas sehen mit den Augen an den Kopfseiten, darum stellt sich ein imponierendes Männchen quer
vor seinen Rivalen und pendelt dabei langsam. Das ist vor allem zum Anschauen, dazu gibt es ein wenig **Spiegeltraining**: Ein Männchen, das am letzten
Ingame-Tag vor dem Spiegel geflart hat, ist 1,25-mal schneller bereit für ein Schaumnest
(`breeding.mirrorReadinessBonus`, 1 = rein kosmetisch). Zucht-Tab und Info-Overlays zeigen „Am Spiegel trainiert“.

![Ein Männchen flart vor dem Spiegelstab](../images/mirror_stick.png)

## Raubfische

Tiere im Entity-Tag `#bettacraft:predators` jagen im Aquarium: der Vanilla-**Kugelfisch** und, falls installiert,
Piranha und Anglerfisch (Naturalist) sowie Battlegill, Wrecker und Devilfish (Fish of Thieves). Etwa viermal am Tag
(`combat.predatorAttacksPerDay`) greift ein Raubfisch das kleinste Tier an: Junge Bettas und winzige Tiere werden
gefressen, Adulte verletzt (der Kugelfisch vergiftet sie). Der Zucht-Tab warnt „Ein Raubfisch jagt die kleinen
Fische!“.
