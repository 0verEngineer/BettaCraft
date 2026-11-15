---
navigation:
  title: Landtiere anderer Mods
  icon: bettacraft:terrarium_box
  parent: terrarium.md
  position: 75
feature: foreignAnimals.terrarium
---

# Landtiere anderer Mods

Land- und Sumpftiere anderer Mods können in unseren Terrarien und Paludarien leben, wenn sie ein **Terrarium-Profil**
haben ([Profile für Tiere anderer Mods](../settings/foreign_profiles.md)). Bettacraft bringt Profile mit für:

| Tier | Mod | Becken | Größenregel |
|---|---|---|---|
| [Landschildkröte](naturalist_tortoise.md) | Naturalist | trockenes Terrarium | 1 m: 12 × 6 × 3 Blöcke, 100 Blöcke Land |
| [Alligator](naturalist_alligator.md) | Naturalist | großes Paludarium | 2 m: 24 × 12 × 6 Blöcke, 800 Blöcke Becken |

Mit einem Profil bekommt ein Tier:

* **Sein Becken:** ein trockenes Terrarium, ein Paludarium oder beides, mit Luft, Luftfeuchte, UV und Sonnenplatz wie unsere
  [Terrarientiere](../terrarium.md). Ein großes hat eine Länge und folgt der [Größenregel der Großtiere](../big_tanks.md),
  gemessen über das **Land** (eine Landschildkröte), das **Wasser** oder das **ganze Becken** (ein Alligator, in beidem zu Hause).
* **Unsere Pflege:** Es bekommt Hunger, frisst sein Futter von Hand auf das Becken und aus dem
  [Futterautomaten](../equipment/feeder.md) (auch Grünzeug und rohes Fleisch), achtet auf sein Klima, wird ohne Pflege krank
  und stirbt.
* **Unseren Läufer:** Im Becken läuft es über das Land des Innenraums, schwimmt im Wasser und geht zum Sonnenplatz, statt der
  KI seiner Mod (die das Land der Miniatur nicht sieht).
* **Unsere Zucht:** Ein gefüttertes Paar vermehrt sich ab und zu; ein Eierleger legt seine Eier in Sand oder Erde auf dem
  Land, die Nesttemperatur brütet sie aus (wärmer schneller). Seine eigene Zucht ist im Becken unterdrückt.
* **Die Terrarienbox:** Eine leere <ItemLink id="bettacraft:terrarium_box" /> fängt es (Rechtsklick aufs Tier), eine volle
  setzt es in ein Becken oder lässt es frei. Tiere, die zu groß für eine Box sind (der Alligator), kommen mit dem
  [großen Kescher](../big_tanks/large_net.md) in eine [Transportwanne](../big_tanks/transport_tub.md). Tiere ohne Profil lehnt
  die Box ab, außer der Server nennt sie in `terrarium.boxAllow` (sie leben dann ohne unsere Pflege in Terrarien und
  Paludarien). Ein zahmes Tier kommt nur für seinen Besitzer in die Box.

Schalter: `features.foreignAnimals.terrarium` (aus: keins davon kommt in Becken, die Box nimmt keins; die im Becken bleiben).
