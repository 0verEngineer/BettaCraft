---
navigation:
  title: Pflanzen anderer Mods
  icon: minecraft:kelp
  parent: plants.md
  position: 14
feature: aquaticPlants.foreignPlants
---

# Pflanzen anderer Mods

![Kelp, Seegras, Korallen und Pflanzen anderer Mods in einem Aquarium](../images/foreign_plants.png)

Kelp, Seegras, Seerosenblätter, Seegurken und Korallen von Minecraft und die Wasserpflanzen anderer Mods können
ebenfalls im [Innenraum](../interior.md) eines Aquariums stehen, und sie **wirken** dort wie die Aquarienpflanzen von
Bettacraft: Sie senken das Nitrat und nehmen Abfall auf, sie zählen als Pflanzen für die Zucht (der Bonus eines gut
bepflanzten Beckens, Verstecke nach dem Laichen), für die jungen Garnelen und das Futter der Kleintiere, für den
Sauerstoff und gegen Algen. Schwimmblätter beschatten die Oberfläche wie die Blätter eines Tigerlotus. In der Miniatur
hinter dem Glas schwanken sie wie unsere Pflanzen.

Sie **wachsen nicht** und vermehren sich nicht, und sie haben **keine Bedürfnisse**: kein Licht-Minimum, keine Wärme,
keinen [Flüssigdünger](fertilizer.md) (sie verbrauchen ihn nicht und wachsen damit auch nicht schneller), kein Calcium
oder Magnesium für die Korallen und keinen Salzgehalt. Dafür wirken sie etwas schwächer als unsere.

## Wie stark

Die Blöcke einer Pflanze, die sich berühren, sind eine Pflanze: eine Kelpsäule, ein Seegras mit beiden Hälften, ein
Teppich aus Algen, ein Riesenseerosenblatt aus vier Blöcken. Jede Pflanze wirkt wie eine **ausgewachsene Pflanze von
Bettacraft derselben Wuchsform** (eine Stängelpflanze, eine Rosette, ein Bodendecker ...), mal
`interiorPlants.foreignPlantFactor` (Standard 0,6). Höchstens `interiorPlants.maxPlantsPerTank` von ihnen zählen in einem
Aquarium, wie bei unseren eigenen Pflanzen.

![Korallen und Seegurken von Minecraft auf Fintastics Lebendgestein in einem Meerwasseraquarium](../images/foreign_corals.png)

Das **Lebendgestein** von Fintastic ist im Meerwasseraquarium ein kleiner biologischer Filter (0,6-mal unser
[Lebendgestein](../saltwater/reef_rock.md)); sein trockenes Gestein (Dead Live Rock) ist Deko. Korallen und
Aufsitzerpflanzen wachsen auf beiden. Treibholz: Stämme jeder Mod (`#minecraft:logs`) und Mangrovenwurzeln senken den pH
wie unsere Treibholz-Deko (`#bettacraft:driftwood`); das Treibholz von Aquaculture ist ein Item, kein Block, und lässt
sich nicht platzieren.

Der Pflanzen-Reiter des Aquariums zeigt, wie viele Pflanzen anderer Mods zählen und ihre Pflanzenmasse.

## Die Pflanzen

| Mod | Blöcke | Zählt als | Wirkung |
|---|---|---|---|
| Minecraft | `#minecraft:coral_blocks` | Koralle | 190 Blöcke Pflanzenmasse pro Pflanze |
| Minecraft | `#minecraft:corals`, `#minecraft:wall_corals` | Koralle | 190 Blöcke Pflanzenmasse pro Pflanze |
| Minecraft | `minecraft:kelp`, `minecraft:kelp_plant` | Stängelpflanze | 120 Blöcke Pflanzenmasse pro Pflanze |
| Minecraft | `minecraft:lily_pad` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Minecraft | `minecraft:sea_pickle` | Koralle | 190 Blöcke Pflanzenmasse pro Pflanze |
| Minecraft | `minecraft:seagrass`, `minecraft:tall_seagrass` | Bandpflanze | 83 Blöcke Pflanzenmasse pro Pflanze |
| Alex's Caves | `alexscaves:ping_pong_sponge` | Koralle | 190 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:amazon_sword`, `fintastic:tall_amazon_sword` | Rosette | 71 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:anubias` | Aufsitzerpflanze | 68 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:aquatic_moss_block`, `fintastic:aquatic_moss_carpet`, `fintastic:aquatic_moss_phyllid` | Moos | 78 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:blue_hypnea` | Moos | 78 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:caulerpa` | Bandpflanze | 83 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:dragons_breath_algae` | Moos | 78 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:duckweed` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Fintastic | `fintastic:green_algae_block`, `fintastic:green_algae_carpet` | Bodendecker | 44 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:hornwort` | Stängelpflanze | 120 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:live_rock`, `fintastic:porous_live_rock`, `fintastic:green_algae_live_rock`, `fintastic:red_algae_live_rock` | Lebendgestein | biologischer Filter im Meerwasser, 60 % unseres Lebendgesteins |
| Fintastic | `fintastic:lotus`, `fintastic:lotus_flower` | Knollenpflanze | 100 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:lotus_pad` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Fintastic | `fintastic:mermaid_fan` | Moos | 78 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:red_algae`, `fintastic:red_algae_fan`, `fintastic:red_algae_wall_fan` | Moos | 78 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:red_algae_block`, `fintastic:red_algae_carpet` | Bodendecker | 44 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:sea_grapes`, `fintastic:sea_grapes_plant` | Stängelpflanze | 120 Blöcke Pflanzenmasse pro Pflanze |
| Fintastic | `fintastic:stromatolite_growths` | Bodendecker | 44 Blöcke Pflanzenmasse pro Pflanze |
| Regions Unexplored | `regions_unexplored:cattail` | Bandpflanze | 83 Blöcke Pflanzenmasse pro Pflanze |
| Regions Unexplored | `regions_unexplored:duckweed` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Regions Unexplored | `regions_unexplored:flowering_lily_pad` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Regions Unexplored | `regions_unexplored:giant_lily_pad` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Twilight Forest | `twilightforest:huge_lily_pad` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |
| Twilight Forest | `twilightforest:huge_water_lily` | Schwimmblätter | 44 Blöcke Pflanzenmasse pro Pflanze, Schatten |

## Für Modpacks

Was ein Block ist, steht in Datapack-Profilen, `data/<namespace>/bettacraft/foreign_plants/<name>.json`:

```json
{ "blocks": ["fintastic:hornwort"], "role": "stem", "factor": 1.0, "required": false }
```

`blocks` nimmt Block-IDs und `#Tags`; `role` ist eins von `stem`, `rosette`, `epiphyte`, `moss`, `carpet`, `ribbon`,
`bulb`, `floating`, `coral`, `live_rock` oder `none` (eine spätere Datei mit `none` nimmt einen Standard wieder heraus);
`factor` wirkt zusätzlich zum Config-Faktor. Profile von Mods, die nicht installiert sind, werden still übersprungen;
eine ID, die in einer geladenen Mod fehlt (in einer neueren Version umbenannt), kommt ins Log, mit ähnlichen IDs dieser
Mod. `/bettacraft foreign plants` (Operatoren) listet die Pflanzen anderer Mods im Aquarium, auf das man schaut, oder in
allen Aquarien in der Nähe, mit ihrem Profil, und die Pflanzenblöcke anderer Mods darin, die keins haben. Schalter:
`features.aquaticPlants.foreignPlants`.
