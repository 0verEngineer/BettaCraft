---
navigation:
  title: Wasser als Flüssigkeit
  icon: bettacraft:ro_water_bucket
  parent: aquarium.md
  position: 38
---

# Wasser als Flüssigkeit

Minecraft hat ein Wasser. Bettacraft bringt zwei eigene Flüssigkeiten mit, damit die Tanks und Speicher anderer Mods
(AE2, Refined Storage, Mekanism, Create ...) sie vom Wasser getrennt halten:

| Flüssigkeit | Werte | Eimer |
|---|---|---|
| `bettacraft:ro_water` Osmosewasser | TDS 0, pH 6,8, nichts gelöst | <ItemLink id="bettacraft:ro_water_bucket" /> |
| `bettacraft:sea_water` Meerwasser | 35 ppt Salz, das Kalzium des Meeres, pH 8,2 | <ItemLink id="bettacraft:sea_water_bucket" /> |

* **In die Welt gegossen** sind beide normales Wasser (es gibt nur einen Wasserblock).
* **Ins Aquarium** (Eimer, Rohr, [Wasserpumpe](../equipment/pipes.md), der [Wasserwechsel-Regler](../equipment/water_change.md)
  oder das Rohr einer anderen Mod) werden sie zu **Wasser mit ihren Werten**: Sie mischen sich wie jeder Wasserwechsel
  nach Volumen ins Aquarium (Salz, Kalzium, TDS und pH; siehe [Wasserchemie](water_chemistry.md)). So bleiben auch die
  Wassertiere anderer Mods zufrieden.
* Die [Rohre](../equipment/pipes.md) und der Wasserspeicher leiten sie wie Wasser; Wasser, das ein Aquarium verlässt,
  ist normales Wasser mit den Werten des Aquariums.
* Meerwassereimer aus älteren Versionen bleiben gültig (dasselbe Item); andere Mods sehen jetzt `bettacraft:sea_water` darin.
* Osmosewasser kommt aus der [Osmoseanlage](../equipment/reverse_osmosis.md), Meerwasser aus dem Ozean oder aus Wasser
  und [Meersalz](../saltwater/water.md).
* Die [Osmoseanlage](../equipment/reverse_osmosis.md) nimmt auch Meerwasser: Die Membran hält das Salz zurück.
