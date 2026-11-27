---
navigation:
  title: Schutzaufsätze
  icon: bettacraft:intake_guard
  parent: equipment/water_system.md
  position: 5
item_ids:
  - bettacraft:intake_guard
  - bettacraft:outlet_guard
---

# Schutzaufsätze

![Der Ansaugschutz und der Rohranschluss an der Seitenwand im Aquarium](../images/pipe_guards.png)

Wo eine Leitung im Aquarium endet, hält ein **Schutzaufsatz** die Tiere aus dem Rohr:

* <ItemLink id="bettacraft:intake_guard" />: ein feines Gitter für die Leitungen, die **saugen**: die Zulaufleitung eines
  [Multiblock-Filters](multiblock_filter.md) und die Abwasserleitung einer [Wasserwechsel-Steuerung](water_change.md) (auch
  dort, wo die Steuerung das Aquarium berührt). Ein Ansaugkorb zählt auch.
* <ItemLink id="bettacraft:outlet_guard" />: eine Gitterkappe für die Leitungen, die **Wasser bringen**: die Rücklaufleitung
  eines Filters. Eine Lily Pipe, Düse oder ein Sprühbalken zählen auch. Die Frischwasserleitung der Steuerung braucht keinen
  Schutz.

## Einen Schutz setzen

* **Innen:** am [Rohranschluss](bulkhead.md) der Leitung oder am Ende ihrer Innenraum-Rohre.
* **Von außen:** den Schutz auf das Glas anwenden, wo die Leitung andockt. An einem [Rohrbündel](pipe_bundle.md) hat jede
  Leitung ihren eigenen kleinen Flansch: den Flansch der gemeinten Leitung anklicken, und der Schutz kommt direkt vor den
  Anschluss **dieser Leitung** (der Flansch, der dem Klick am nächsten liegt).
* Ein Schutz, der neben ein Rohr oder einen Anschluss gesetzt wird, dreht sein Gitter von selbst davon weg (beim Setzen
  schleichen: er schaut, wohin du siehst); Schleichen + Rechtsklick mit leerer Hand auf einen Schutz dreht ihn zur nächsten
  freien Seite.

Jede Leitung hat ihren **eigenen** Anschluss und braucht ihren eigenen Schutz: ein Auslassschutz an der Rücklaufleitung
schützt die Zulaufleitung daneben nicht.

![Zwei Anschlüsse eines Bündels: der Ansaugschutz am blauen Anschluss, der Auslassschutz am orangen](../images/pipe_line_docks.png)

## Ohne Schutz

* **Filter:** ohne beide Schutzaufsätze in einem Aquarium **filtert der Filter dieses Aquarium nicht**. Eine rote Zeile im
  Aquariummenü (Reiter Wasser), in Jade / The One Probe / WTHIT und im Filter-Fenster sagt, was fehlt. Das gilt für jeden
  Multiblock-Filter, auch für die, die gebaut wurden, bevor es Schutzaufsätze gab: sie filtern wieder, sobald die
  Schutzaufsätze drin sind.
* **Wasserwechsel:** die Steuerung wechselt das Wasser trotzdem, aber ihre Abwasserleitung **saugt Jungtiere ein**
  (Jungfische, Junggarnelen und Kaulquappen; keine Schnecken, keine erwachsenen Tiere), so viele, wie es dem abgelassenen
  Anteil entspricht: ein Wechsel von 25 % nimmt etwa ein Viertel der Jungtiere. Ihr Fenster, das Aquariummenü (Reiter Technik
  und Zucht) und die Overlays warnen "saugt Jungtiere ein"; das Logbuch notiert jedes verlorene Tier. Das passiert nur mit
  `features.breeding.fryIntake` an und in einem Pflegemodus, in dem Tiere sterben können.

Wer auf seinem Server keine Schutzaufsätze für Filter will, schaltet die Filterleitungen ab: `features.pipes.filterLines =
false` in der Common-Config. Dann versorgt ein Filter wie früher ein Aquarium, ohne Schutzaufsätze, und seine Rohre sind
normale Wasserrohre.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:intake_guard" />
<RecipesFor id="bettacraft:outlet_guard" />
