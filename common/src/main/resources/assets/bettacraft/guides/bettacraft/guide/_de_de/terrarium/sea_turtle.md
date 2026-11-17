---
navigation:
  title: Meeresschildkröte
  icon: minecraft:turtle_egg
  parent: terrarium.md
  position: 54
feature: turtles.seaTurtles
---

# Meeresschildkröte

Die Vanilla-**Meeresschildkröte** ist ein Großtier: Sie lebt nur in einem **großen Meerwasser-Paludarium** mit Strand.
Ausgewachsene und ihre Schlüpflinge folgen derselben [Größenregel wie die großen Fische](../big_tanks.md): 1 m lang, also
braucht das Becken

| | |
|---|---|
| Größe | mindestens **12 × 6 Blöcke und 3 Blöcke hoch** (ein [Großbecken](../big_tanks.md)) |
| Wasser | **100 Wasserblöcke** je Meeresschildkröte im Wasserteil (das Becken ohne sein Land); ein Schlüpfling zählt wie eine ausgewachsene |
| Salz | **Meerwasser**, ab 25 ‰ ([Salzwasser](../saltwater.md)) |
| Land | ein [Paludarium](../terrarium.md) mit **Strand**: Sand oder Erde auf dem Land, dort kommen die Eier hin |
| Klima | Wasser 20 bis 30 °C, Luft 18 bis 32 °C |

Ein Becken, das nicht passt, sagt warum: die nötige Größe, das fehlende Wasser, das Salz oder der fehlende Strand.

* **Schlüpflinge** fängt der [Fischbeutel](../equipment/fish_bag.md) am Strand, **ausgewachsene** der
  [große Kescher](../big_tanks/large_net.md) in eine [Transportwanne](../big_tanks/transport_tub.md) (die Wanne holt eine auch
  aus einem Becken). Beide kommen per Rechtsklick ins Becken.
* Ein Schlüpfling wächst in seinem Becken heran und bleibt dort: Das Becken ist schon groß genug für eine ausgewachsene.
* Sie schwimmt und läuft auf Vanilla-Art und braucht kein Futter von dir.

## Eier am Strand

Zwei ausgewachsene Meeresschildkröten in einem passenden Becken (mit `features.turtles.breeding`) werden ab und zu ein
Paar: Das Weibchen legt **2 bis 4 Eier** in den Sand des Strandes (der Eier-Block zeigt sich in der Miniatur). Die Eier
entwickeln sich nach der Nesttemperatur, der Temperatur des Bodens (die Luft oder [Heizmatten](heat_mat.md) unter dem
Becken): bei 29 °C in etwa 3 Tagen, wärmer schneller; bei 18 °C und darunter warten sie, über 36 °C sterben sie. Wasser
über dem Nest ertränkt sie; wer den Eier-Block im Innenraum abbaut, zerstört sie. Die Schlüpflinge kommen am Nest heraus.
Vanilla-Meeresschildkröten haben keine Geschlechter, die Nesttemperatur bestimmt also nur das Tempo. Ihre eigene
Vanilla-Zucht ist im Becken unterdrückt.

Schalter: `features.turtles.seaTurtles` (aus: keine Meeresschildkröten kommen in Becken; die im Becken bleiben).
