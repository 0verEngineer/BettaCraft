---
navigation:
  title: Profile für Tiere anderer Mods
  icon: minecraft:tropical_fish_bucket
  parent: settings.md
  position: 20
feature: foreignAnimals.enabled
---

# Profile für Tiere anderer Mods

Ein **Profil** gibt einem Tier einer anderen Mod (oder aus Vanilla) im Bettacraft-Aquarium unsere Pflege und unsere Zucht.
Außerhalb des Aquariums ändert sich nichts: Dort lebt das Tier nach seiner eigenen Mod.

## Was ein Profil bewirkt

* **Pflege** (`features.foreignAnimals.care`): Das Tier bekommt Hunger wie ein Betta (es frisst das Futter des Beckens: von Hand,
  aus dem Futterautomaten, aus der Lebendfutter-Pumpe), achtet auf seinen Temperaturbereich, die Wasserqualität, pH und
  TDS, den Besatz und (mit `saltwater.foreignAnimals = REALISTIC`) den Salzgehalt. Tage mit Problemen machen es krank, ein krankes Tier, dem nicht geholfen wird,
  stirbt (Logbuch mit Ursache). Ein gefüttertes Tier heilt. Das Beckenmenü zeigt seine Gesundheit und seinen Hunger.
* **Zucht** (`features.foreignAnimals.breeding`): Zwei ausgewachsene, satte, gesunde Tiere derselben Art in Wasser ihrer
  Zuchttemperatur vermehren sich hin und wieder. Ein **Lebendgebärender** trägt die Jungen aus und bringt sie zur Welt,
  ein **Eierleger** laicht und die Eier schlüpfen (manche brauchen Pflanzen, Speisefische ein großes Aquarium). Die
  Jungen erben die Varianten ihrer Eltern (Farben, Muster: was ihre Mod über sie speichert), je Merkmal den Wert eines
  Elternteils.
* **Ihre eigene Zucht unterdrückt** (`features.foreignAnimals.suppressOwnBreeding`, für jedes Tier aus Vanilla und anderen Mods im
  Aquarium): keine Paarungsbereitschaft durch Futter, keine Jungen aus der Zucht ihrer Mod. Unsere eigene Zucht von
  Axolotln, Schildkröten und den Mobs im Glas bleibt davon unberührt.

## Die mitgelieferten Profile

| Mod | Tiere | Wie | Vermehrung |
|---|---|---|---|
| Fintastic | Guppy | unser [Guppy](../tankmates.md) | lebendgebärend, Flossen- und Schwanzfarben werden vererbt |
| Fintastic | Gurami | unser Zwergfadenfisch | Eierleger, Größe und Haut werden vererbt (der Riesengurami ist zu groß) |
| Fintastic | Pleco, kleiner Wels | Antennenwels, Panda-Panzerwels | Eierleger |
| Fintastic | Elritze | kühles Wasser (14 bis 26 °C) | Eier an Pflanzen |
| Fintastic | Moony | Brackwasser | nein |
| Fintastic, Aquaculture, Naturalist | Kabeljau, Lachs, Hering, Sonnenbarsch, Flussbarsch, Barsch, Forelle, Boulti, Wels, Piranha | Speisefische | nur in Aquarien ab 24 Wasserblöcken (Piranha 16) |
| Aquaculture | Synodontis | Wels | nein |
| Critters and Companions | Koi | Teichfisch (4 bis 28 °C) | Eier an Pflanzen, 16 Wasserblöcke |

## Ein Profil schreiben

Profile sind Daten: `data/<namespace>/bettacraft/foreign_animals/<name>.json` in einem Datapack (oder einem
KubeJS-Datenordner), neu gelesen mit `/reload`. Eine Datei für dasselbe Entity in einem späteren Pack ersetzt die frühere.
Entities, deren Mod nicht installiert ist, werden still übersprungen (außer mit `"required": true`). Beispiel:

```json
{
  "entity": ["somemod:neon", "somemod:cardinal"],
  "species": "neon_tetra",
  "group": "tetra",
  "care": {"temperature": [22, 27], "min_quality": 0.5, "salinity": "fresh", "ph": [5.5, 7.5], "tds": [0, 300], "stock": 0.35},
  "food": ["normal", "live", "fry", "#somemod:fish_food"],
  "breeding": {
    "type": "eggs", "temperature": [24, 26], "needs_plants": true, "min_water_blocks": 0,
    "days": 2, "young": [2, 5], "pause_days": 3, "rate": 0.5,
    "inherit": ["Variant"], "offspring_nbt": "{FromBucket:1b}", "baby": true
  },
  "suppress_own_breeding": true
}
```

| Schlüssel | Bedeutung |
|---|---|
| `entity` | eine Entity-ID oder eine Liste |
| `species` | optional: eine unserer Arten (`guppy`, `dwarf_gourami`, `neon_tetra` …), deren Werte es übernimmt; jeder Schlüssel der Datei geht vor |
| `group` | eine Bezeichnung (`livebearer`, `pond_fish`, `food_fish` …) |
| `care.temperature`, `min_quality`, `salinity` | Bereich in °C, niedrigste Wasserqualität (0 bis 1), `fresh`, `brackish`, `brackish_only`, `marine`, `hardy_marine`, `any` oder `[min, max]` in ppt |
| `care.ph`, `care.tds` | sein pH- und TDS-Bereich (weggelassen: pH und TDS sind egal, außer die Art hat einen) |
| `care.labyrinth`, `hunger_days`, `stock` | atmet Luft (wenig Sauerstoff schadet nicht), Tage bis zum Hunger (1 wie ein Betta), wie viel es für den Besatz zählt |
| `food` | `normal` (Pellets, Flocken …), `live` (Lebend- und Frostfutter), `fry` (Jungfischfutter; Jungtiere fressen es immer), `#item_tag` oder eine Item-ID |
| `breeding.type` | `livebearer`, `eggs` oder `none` |
| `breeding.temperature`, `needs_plants`, `min_water_blocks` | wann sich ein Paar vermehrt |
| `breeding.days`, `young`, `pause_days`, `rate` | Tage bis zu den Jungen, Junge pro Brut, Pause der Mutter, Bruten pro Paar und Tag |
| `breeding.offspring`, `offspring_nbt` | das Entity der Jungen (Standard: dasselbe) und NBT für jedes Junge |
| `breeding.inherit` | NBT-Schlüssel, die jedes Junge von einem seiner Eltern erbt (zu finden mit `/data get entity`) |
| `breeding.baby` | die Jungen kommen als Jungtiere ihrer Mod zur Welt (Standard true) |
| `terrarium` | statt eines Aquariums: `habitat` (`dry`, `paludarium`, `any`), `air`, `humidity`, `land`, `water`, `uv`, `basking` wie bei den [Terrarientieren](../terrarium.md) |
| `suppress_own_breeding` | `false` lässt die eigene Zucht des Tiers im Aquarium zusätzlich laufen |

Tiere mit eigener Pflege in Bettacraft (unsere Tiere, Axolotl, Delfin, Meeresschildkröte und die Mobs im Glas) können kein
Profil bekommen. Das Serverlog nennt die geladenen Profile und jeden Fehler in einer Datei.

## Profile in einer Welt prüfen

* **Umbenannte Entity:** Ist die Mod eines Profils installiert, die Entity-ID aber nicht registriert (eine neuere Version der
  Mod hat sie umbenannt), warnt das Server-Log mit der Profil-Datei, der fehlenden ID und den ähnlichen IDs dieser Mod,
  damit sich die Datei korrigieren lässt. Ein Profil für eine Mod, die nicht installiert ist, bleibt still.
* **`/bettacraft foreign list [Radius]`** (Operatoren): jedes Tier aus Vanilla oder einer anderen Mod im angeschauten Becken,
  sonst in allen Becken im Radius (Standard 32 Blöcke), mit seiner Entity-ID und der Profil-Datei, die es abbildet, oder
  „kein Profil“. Am Ende die Entity-Typen anderer Mods, die ohne Profil in diesen Becken leben: die, für die sich ein Profil
  lohnen würde.
