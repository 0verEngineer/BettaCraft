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
  Tiere mit bekannter Länge (die großen Fische anderer Mods über ihr [Profil](foreign_profiles.md), Delfine,
  Meeresschildkröten) kommen stattdessen nach der Größenregel in [große Becken](../big_tanks.md); die Meldung nennt das
  nötige Becken.
* **ein Boss** (`#c:bosses`), z. B. der Leviathan aus L_Ender's Cataclysm.
* **feindlich:** Mobs der Spawn-Kategorie Monster, z. B. Wächter oder der Rotfeuerfisch aus Cataclysm
  (`inhabitants.refuseHostile`).
* **kein Wassertier:** Landtiere in Eimern anderer Mods (Schmetterlinge, Schnecken, Krabben von Naturalist …).
* **ungeeignet, mit Grund:** Tiefseetiere (Anglerfisch und Blobfisch von Naturalist, die Wesen des Abgrunds aus Alex's
  Caves), Tiere, die für jedes Becken viel zu groß sind (Weißer Hai und Wal von Naturalist), Pilzwesen (die Shroomas von
  Aquaculture), die an ihre Höhlen gebundenen Wesen aus Alex's Caves und die feindlichen Käfer und Spinnen des Twilight
  Forest. Ihre Profile sagen das (`"refuse"`); die [Liste](foreign_animal_list.md) nennt sie alle.
* **gehört nicht in ein Aquarium:** Tiere in `#bettacraft:tank_blacklist`: der Thunfisch von Sushi Go Crafting, Frösche
  und die Haustiere aus L_Ender's Cataclysm (sie folgen ihrem Besitzer und teleportieren sich zu ihm). Die Riesenfische
  von Aquaculture und Fintastic und der Mantarochen von Living Things sind jetzt Großtiere (siehe oben).

Serverbetreiber können das ändern: `inhabitants.allow` lässt ein Tier immer hinein (`modid:name` oder `modid:*`),
`inhabitants.deny` nie; die Entity-Tags `#bettacraft:tank_inhabitants` und `#bettacraft:tank_blacklist` tun dasselbe per
Datapack. `/bettacraft wateranimals` (Operatoren) listet jedes Wassertier der geladenen Mods und ob Aquarien es nehmen, in
`bettacraft-water-animals.tsv` im Serverordner.

## Im Aquarium

![Seepferdchen, Axolotl, Koi, Laternenfisch, Seestern und mehr](../images/foreign_animals_closeup.png)

Tiere anderer Mods behalten ihr eigenes Verhalten. Bettacraft hält sie im Wasser der echten Tankform und zählt sie für
den Besatz. Tiere **mit Profil** (standardmäßig die Fische, Wirbellosen und Schildkröten von Fintastic, Aquaculture,
Fish of Thieves, Naturalist, Critters and Companions und Living Things, siehe die [Liste](foreign_animal_list.md))
bekommen im Aquarium zusätzlich unsere Pflege: Hunger, ihre Temperatur und ihr Wasser, Krankheit und Tod, und sie
vermehren sich nach unseren Regeln, siehe [Profile für Tiere anderer Mods](foreign_profiles.md). Tieren ohne Profil
schadet unsere Pflege nie.

* **Axolotl** (Vanilla) bekommen als Ausnahme unsere Pflege und Zucht (kühles Wasser, Eier an den Pflanzen,
  Farbgenetik), siehe [Axolotl](../invertebrates/axolotl.md).
* **Raubfische** (`#bettacraft:predators`) jagen kleine Fische und Jungfische: Kugelfische, **Axolotl**, Piranhas, der
  Anglerfisch der Otherside und die Raubfische der Aquarium-Mods. Siehe
  [Kämpfe, Flaring und Raubfische](../bettas/fighting.md).
* **Kaulquappen** wachsen im Aquarium heran. Der junge Frosch hüpft hinaus und landet vor dem Tank (Logbuch-Eintrag). Mit
  `inhabitants.tadpoles = STAY_TADPOLE` bleiben sie Kaulquappen, solange sie in einem Aquarium leben.
* **Tintenfische und Leuchttintenfische** haben keinen Eimer: Fang sie mit einer Fischtüte, die das ganze Tier behält.
  Heraus kommen sie ebenfalls nur mit einer Fischtüte.
* **Shiny** tauscht Vanilla-Tiere beim Spawnen gegen ihre glänzenden Versionen aus. Ein glänzender Fisch kommt ins
  Aquarium und glänzend wieder heraus (die Fischtüte behält das ganze Tier); die Tiere von Bettacraft werden nie
  ausgetauscht (ihre Farben kommen aus ihren Genen).
* **Fintastic** tauscht Vanilla-Kabeljau und -Lachs beim Spawnen gegen eigene aus. Ein Kabeljau oder Lachs aus dem Eimer
  bleibt in einem Bettacraft-Aquarium, was er ist.
* **Ihre eigene Vermehrung** (Paarungsbereitschaft durch Futter, die Zucht ihrer Mod) ist im Bettacraft-Aquarium
  unterdrückt (`features.foreignAnimals.suppressOwnBreeding`); außerhalb ändert sich nichts. Tiere mit Profil vermehren sich
  stattdessen nach unseren Regeln.
