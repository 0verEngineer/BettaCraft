---
navigation:
  title: Filter
  icon: bettacraft:internal_filter
  parent: equipment.md
  position: 30
item_ids:
  - bettacraft:internal_filter
  - bettacraft:external_filter
---

# Filter

Filter bringen Bewegung ins Aquarium: einen Wasserstrahl, Blasen und ein leises Blubbern (abschaltbar in der
Client-Config, `sound.filterSound`). Die Strömung selbst wirkt nicht auf die Fische, aber Filter **halten das Wasser
sauber**: Ein Außenfilter schafft 32 Wasserblöcke, ein Innenfilter unter Wasser 16 (siehe
[Wasserqualität](../aquarium/water_quality.md)).

## Innenfilter

<ItemImage id="bettacraft:internal_filter" scale="2" />

Setze den <ItemLink id="bettacraft:internal_filter" /> in den [Innenraum](../interior.md): Er wird zu einem Filterturm
(2x2x8 Blöcke bei Maßstab 16), dessen Auslass zu dir zeigt. Das Aquarium zeigt an seinem Auslass einen Wasserstrahl.

## Außenfilter

![Ein Außenfilter neben einem Aquarium](../images/external_filter.png)

Der <ItemLink id="bettacraft:external_filter" /> steht **neben dem Aquarium** und zeigt darauf: Gegen einen
Aquarium-Block gesetzt dreht er sich zu ihm, an jeder Seitenwand und in jeder Höhe. Ein Schlauch führt über den
Glasrand, ein Strahl plätschert ins Wasser (unter Wasser ist es eine kurze Strömung).

* Er hat **zwei Plätze** für [Heizstäbe](heaters.md) oder [Kühler](coolers.md) beliebiger Größe. Öffne ihn per
  Rechtsklick oder rechtsklicke ihn mit einem Heizstab. Solange der Filter angeschlossen ist, heizen seine Heizstäbe
  und kühlen seine Kühler das Aquarium.
* Abgebaut behält er seine Heizstäbe im Item (der Tooltip listet sie auf).
* Er passt auch in einen [Aquariumschrank](cabinet.md) unter dem Aquarium.

Das Aquarium-Menü zeigt „Filter: 1 außen (Heizstäbe: 2), 1 innen“.

## Weitere Innenfilter, Jungfische und der Leistungsregler

* Der [Schwammfilter](sponge_filter.md) (luftbetrieben, schonend für Jungfische) und der [Eckfilter](corner_filter.md)
  sind weitere Innenfilter.
* Filter mit Motor (Innen-, Eck-, Außen- und Schrankfilter) **saugen Jungfische ein**, wenn kein
  [Ansaugschutz](intake_sponge.md) angebracht ist.
* Jeder Filter hat einen [Leistungsregler](regulator.md) (0 bis 100 %): weniger Leistung reinigt weniger, braucht
  weniger Energie und ist leiser. Filterauslässe bringen auch etwas **Sauerstoff** ins Wasser (siehe
  [Sauerstoff](../aquarium/oxygen.md)).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:internal_filter" />
<RecipesFor id="bettacraft:external_filter" />
