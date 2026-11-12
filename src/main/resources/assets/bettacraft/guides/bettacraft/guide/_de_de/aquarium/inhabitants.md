---
navigation:
  title: Bewohner
  icon: bettacraft:fish_bag
  parent: aquarium.md
  position: 20
---

# Bewohner

## Tiere einsetzen

Rechtsklicke das Aquarium mit einem **Betta-Eimer**, einem anderen **Fischeimer** oder einer gefüllten **Fischtüte**.
Das Tier erscheint mit all seinen Daten im Wasser. Das Aquarium muss mindestens halb voll sein.

Angenommen werden:

1. Tiere im Entity-Tag `#bettacraft:tank_inhabitants` (Axolotl, Kaulquappe),
2. jedes kleine Wassertier, das in einen Eimer passt (Hitbox bis 0,8 x 0,8),
3. nie Tiere in `#bettacraft:tank_blacklist` (Riesenfische anderer Mods).

Andere Tiere behalten ihr eigenes Verhalten und haben im Aquarium **keine Bedürfnisse**; nur Bettas werden hungrig,
brauchen Wärme und vermehren sich. Kleine Wassertiere, die ohne Eimer ins Aquarium kommen (Spawn-Ei, Befehl), werden
ebenfalls übernommen.

## Tiere entnehmen

* **Menü:** Öffne das Aquarium-Menü, wähle das Tier im Tab *Bewohner* und klicke *Eimer* oder *Fischtüte*. Du brauchst
  einen leeren Eimer bzw. eine leere Fischtüte im Inventar. Ist nicht genug Wasser da, ist der Knopf ausgegraut.
* **Direkt:** Rechtsklicke einen Fisch im Aquarium mit einem Wassereimer, wie überall sonst.

## Besatz

Nur **adulte** Tiere zählen. Eier, Larven, Jungfische, Juvenile und Subadulte nicht.

* Ein Adulter pro Wasserblock ist in Ordnung.
* Ab **2 Adulten pro Wasserblock** ist das Aquarium überbesetzt: Die Bettas geraten in Stress und werden irgendwann krank
  (siehe [Gesundheit](../bettas/health.md)). Das Menü zeigt „Überbesetzt!“.
* Höchstens **6 Adulte pro Wasserblock** passen überhaupt hinein.
* Ein Aquarium fasst insgesamt höchstens 64 Tiere, Jungtiere eingerechnet (`tank.maxEntities`).

## Raubfische

Kugelfische und die Raubfische anderer Mods fressen kleine Fische. Siehe
[Kämpfe, Flaring und Raubfische](../bettas/fighting.md).
