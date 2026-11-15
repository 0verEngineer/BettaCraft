---
navigation:
  title: Tiere anderer Mods
  icon: minecraft:axolotl_bucket
  parent: settings.md
  position: 10
feature: foreignAnimals.enabled
---

# Tiere anderer Mods

![Ein Aquarium mit Wassertieren mehrerer Mods](../images/foreign_animals.png)

Jedes kleine Wassertier aus Vanilla-Minecraft und aus anderen Mods kann in einem Bettacraft-Aquarium leben: Fische,
Axolotl, Kaulquappen, Tintenfische, Meeresschnecken, Seepferdchen, Seesterne und mehr. Bettacraft muss die Mod dafür
nicht kennen, es schaut sich das Tier selbst an. Eingesetzt wird es mit seinem eigenen Eimer oder einer Fischtüte (siehe
[Bewohner](../aquarium/inhabitants.md)).

## Was abgelehnt wird

Das Aquarium lehnt ein Tier mit einer Meldung ab, die den Grund nennt:

* **zu groß:** Die Hitbox ist breiter oder höher als 0,8 Blöcke (`inhabitants.maxHitbox`). Es zählt die Größe des Tiers
  selbst, daher unterscheiden sich Varianten anderer Mods: Ein Fintastic-Zwergfadenfisch passt, ein Riesengurami nicht.
  Delfine, Schildkröten, Haie, Wale, Rochen und Ähnliches sind zu groß.
* **ein Boss** (`#c:bosses`), z. B. der Leviathan aus L_Ender's Cataclysm.
* **feindlich:** Mobs der Spawn-Kategorie Monster, z. B. Wächter oder der Rotfeuerfisch aus Cataclysm
  (`inhabitants.refuseHostile`).
* **kein Wassertier:** Landtiere in Eimern anderer Mods (Schmetterlinge, Schnecken, Krabben von Naturalist …).
* **gehört nicht in ein Aquarium:** Tiere in `#bettacraft:tank_blacklist`. Das sind Riesenfische mit kleiner Hitbox
  (Thunfisch, Arapaima, Heilbutt, Karpfen … von Aquaculture; Arapaima und Quastenflosser von Fintastic), der Mantarochen
  von Living Things, der Thunfisch von Sushi Go Crafting, Frösche, der Otter von Critters and Companions und die Haustiere
  aus L_Ender's Cataclysm (sie folgen ihrem Besitzer und teleportieren sich zu ihm).

Serverbetreiber können das ändern: `inhabitants.allow` lässt ein Tier immer hinein (`modid:name` oder `modid:*`),
`inhabitants.deny` nie; die Entity-Tags `#bettacraft:tank_inhabitants` und `#bettacraft:tank_blacklist` tun dasselbe per
Datapack. `/bettacraft wateranimals` (Operatoren) listet jedes Wassertier der geladenen Mods und ob Aquarien es nehmen, in
`bettacraft-water-animals.tsv` im Serverordner.

## Im Aquarium

![Seepferdchen, Axolotl, Koi, Laternenfisch, Seestern und mehr](../images/foreign_animals_closeup.png)

Tiere anderer Mods behalten ihr eigenes Verhalten. Bettacraft hält sie im Wasser der echten Tankform und zählt sie für
den Besatz. Tiere **mit Profil** (standardmäßig die Fische von Fintastic, Aquaculture, Naturalist und der Koi von Critters
and Companions) bekommen im Aquarium zusätzlich unsere Pflege: Hunger, ihre Temperatur und ihr Wasser, Krankheit und Tod,
und sie vermehren sich nach unseren Regeln, siehe [Profile für Tiere anderer Mods](foreign_profiles.md). Tieren ohne
Profil schadet unsere Pflege nie.

* **Axolotl** (Vanilla) bekommen als Ausnahme unsere Pflege und Zucht (kühles Wasser, Eier an den Pflanzen,
  Farbgenetik), siehe [Axolotl](../invertebrates/axolotl.md).
* **Raubfische** (`#bettacraft:predators`) jagen kleine Fische und Jungfische: Kugelfische, **Axolotl**, Piranhas, der
  Anglerfisch der Otherside und die Raubfische der Aquarium-Mods. Siehe
  [Kämpfe, Flaring und Raubfische](../bettas/fighting.md).
* **Kaulquappen** wachsen im Aquarium heran. Der junge Frosch hüpft hinaus und landet vor dem Tank (Logbuch-Eintrag). Mit
  `inhabitants.tadpoles = STAY_TADPOLE` bleiben sie Kaulquappen, solange sie in einem Aquarium leben.
* **Tintenfische und Leuchttintenfische** haben keinen Eimer: Fang sie mit einer Fischtüte, die das ganze Tier behält.
  Heraus kommen sie ebenfalls nur mit einer Fischtüte.
* **Fintastic** tauscht Vanilla-Kabeljau und -Lachs beim Spawnen gegen eigene aus. Ein Kabeljau oder Lachs aus dem Eimer
  bleibt in einem Bettacraft-Aquarium, was er ist.
* **Ihre eigene Vermehrung** (Paarungsbereitschaft durch Futter, die Zucht ihrer Mod) ist im Bettacraft-Aquarium
  unterdrückt (`features.foreignAnimals.suppressOwnBreeding`); außerhalb ändert sich nichts. Tiere mit Profil vermehren sich
  stattdessen nach unseren Regeln.
