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
Das Tier erscheint mit all seinen Daten im Wasser. Das Aquarium muss mindestens halb voll sein. Eine gefüllte *Fish Bag*
von Better Fish Tanks (+100 mB) und ein volles *Fischnetz* von Fintastic (ohne Wasser) gehen ebenfalls.

Angenommen wird jedes **kleine Wassertier** (Hitbox bis 0,8 x 0,8), auch aus anderen Mods: Fische, Axolotl, Kaulquappen,
Tintenfische (mit der Fischtüte, sie haben keinen Eimer) und mehr. Abgelehnt werden zu große Tiere, Bosse, feindliche
Mobs, Landtiere und die Tiere in `#bettacraft:tank_blacklist` (Riesenfische anderer Mods); die Meldung nennt den Grund.
Die Tiere in `#bettacraft:tank_inhabitants` (z. B. der Fintastic-Pleco) sind immer willkommen. Einzelheiten und
Einstellungen: [Tiere anderer Mods](other_animals.md).

Andere Tiere behalten ihr eigenes Verhalten und haben im Aquarium **keine Bedürfnisse**; nur Bettas werden hungrig,
brauchen Wärme und vermehren sich. Kleine Wassertiere, die ohne Eimer ins Aquarium kommen (Spawn-Ei, Befehl), werden
ebenfalls übernommen.

## Tiere entnehmen

* **Menü:** Öffne das Aquarium-Menü, wähle das Tier im Tab *Bewohner* und klicke *Eimer* oder *Fischtüte*. Du brauchst
  einen leeren Eimer bzw. eine leere Fischtüte im Inventar. Eine Fischtüte nimmt jedes Tier, ein Eimer nur Tiere mit
  Eimer. Ist nicht genug Wasser da, ist der Knopf ausgegraut. Der
  Knopf *Fischtüte* füllt auch eine leere Fish Bag von Better Fish Tanks (-100 mB) oder ein Fintastic-Fischnetz (ohne
  Wasser).
* **Direkt:** Rechtsklicke einen Fisch im Aquarium mit einem Wassereimer, wie überall sonst.

## Besatz

Nur **adulte** Tiere zählen. Eier, Larven, Jungfische, Juvenile und Subadulte nicht.

* Ein Adulter pro Wasserblock ist in Ordnung.
* Ab **2 Adulten pro Wasserblock** ist das Aquarium überbesetzt: Die Bettas geraten in Stress und werden irgendwann krank
  (siehe [Gesundheit](../bettas/health.md)). Das Menü zeigt „Überbesetzt!“.
* Höchstens **6 Adulte pro Wasserblock** passen überhaupt hinein.
* Ein Aquarium fasst insgesamt höchstens 64 Tiere, Jungtiere eingerechnet (`tank.maxEntities`).

## Raubfische

Kugelfische, Axolotl und die Raubfische anderer Mods fressen kleine Fische. Siehe
[Kämpfe, Flaring und Raubfische](../bettas/fighting.md).

## Lebende Beute

Kleine Beutetiere anderer Mods (`#bettacraft:betta_prey`: Feenkrebse, Daphnien und Ruderfußkrebse von Fintastic) sind
Lebendfutter: Ein hungriger Betta frisst eines und gilt als mit Lebendfutter gefüttert, was auch die Zucht fördert.
