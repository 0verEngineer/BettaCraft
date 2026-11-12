---
navigation:
  title: Formen
  icon: bettacraft:tank
  parent: aquarium.md
  position: 5
---

# Aquarium-Formen

Ein Aquarium muss kein Quader sein. **Jede Form geht**, solange sich seine Blöcke mit einer Fläche berühren: ein L um
eine Ecke, ein T, ein U, Stufen, ein Turm auf einem breiten Becken.

![Ein L-förmiges Aquarium mit Bettas, ein T aus hellblauem Glas mit erhöhten Enden und ein kleineres L mit eingerichtetem Innenraum](../images/shapes.png)

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" z="0" p:east="true" p:south="true" p:fill="12" />
  <Block id="bettacraft:tank" x="1" z="0" p:west="true" p:fill="12" />
  <Block id="bettacraft:tank" x="0" z="1" p:north="true" p:fill="12" />
</GameScene>

## Bauen

* Ein neuer Block verbindet sich mit **jedem** benachbarten Aquarium aus demselben Glas und mit demselben Maßstab; ein
  Block zwischen zwei Aquarien macht eines daraus.
* Der **umschließende Quader** der Form muss in die Maximalgröße passen (standardmäßig 16x16x16 Blöcke, Server können
  bis 64 erlauben). Ein Block, der das Aquarium größer machen würde, bleibt ein eigenes Aquarium („Verbunden wäre das
  Aquarium größer als …“).
* Blöcke, die sich nur an einer Kante oder Ecke berühren, verbinden sich nicht.
* Die dunklen Kantenlinien laufen an jeder Außenkante, auch an der **inneren Ecke** eines L.
* Server können freie Formen abschalten (`tank.freeShapes = false`): Dann sind Aquarien wie früher Quader.

![Die innere Ecke eines L-förmigen Aquariums](../images/shape_corner.png)

## Wasser

Das Wasser füllt das Aquarium **Schicht für Schicht** von unten: Jeder Block einer Schicht hat dieselbe Wasserhöhe. In
einem Aquarium mit Stufen ist der untere Teil voll, bevor das Wasser im höheren Teil steigt. Das Volumen ist 1000 mB
pro Block, egal welche Form.

* Wer einen Block abbaut, nimmt nur das Wasser dieses Blocks weg. Zerfällt der Rest in mehrere Teile, behält jeder Teil
  das Wasser seiner eigenen Blöcke.
* Wasser verdunstet durch jede offene Oberseite, die das Wasser erreicht, auch durch die Oberseite einer niedrigeren
  Stufe. Ein [Deckel](water_quality.md), der Futterautomat, ein Licht oder jeder volle Block darauf deckt sie ab.

## Tiere und Ausrüstung

* Fische schwimmen nur im Wasser der tatsächlichen Form. Sie wollen nie in die leere Ecke eines L, Futter landet im
  Wasser des angeklickten Blocks, und das Schaumnest entsteht an der Oberfläche.
* **Futterautomat**, **Lichter** und **Deckel** können auf jeder offenen Oberseite stehen, auch auf einer niedrigeren
  Stufe.
* Ein **Außenfilter** kann an jeder Außenwand hängen, auch an der Wand der inneren Ecke. Sein Schlauch geht über den
  Rand der Säule, an der er hängt. **Schränke** funktionieren unter jedem Block, unter dem kein Aquarium-Block ist.
* Das **Namensschild** sitzt auf dem breitesten ebenen Teil der Vorderseite, in der obersten oder untersten Blockreihe.

## Innenraum

Der [Innenraum](../interior.md) umfasst den umschließenden Quader des Aquariums. Die Teile außerhalb der Form sind durch
unsichtbare Wände abgetrennt und haben keinen Boden: Du baust nur dort, wo das Aquarium wirklich ist, und die Miniatur
zeigt genau das.

![Die Miniatur eines L-förmigen Aquariums und sein Innenraum von oben](../images/shape_interior.png)

* Der Boden (und damit der [Bodengrund](../interior/substrate.md)) liegt unter der untersten Schicht des Aquariums. Ein
  erhöhter Teil der Form steht dort auf einer unsichtbaren Wand.
* Wer einen Block abbaut, bekommt den Innenraum-Teil dahinter als Items. Teilt sich das Aquarium, behält jeder Teil den
  Innenraum seiner eigenen Blöcke.
