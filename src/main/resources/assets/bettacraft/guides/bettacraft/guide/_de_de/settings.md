---
navigation:
  title: Einstellungen und andere Mods
  icon: minecraft:comparator
  parent: index.md
  position: 80
---

# Einstellungen und andere Mods

## Server-Config (pro Welt)

| Option | Standard | |
|---|---|---|
| `tank.maxSizeX/Y/Z` | 16 | größtes Aquarium (bis 64), der umschließende Quader bei freien Formen |
| `tank.freeShapes` | true | jede zusammenhängende Form (L, T, Stufen); false = nur Quader |
| `tank.maxEntities` | 64 | Tiere pro Aquarium |
| `tank.hardCapPerWaterBlock` | 6 | höchstens so viele Adulte pro Wasserblock |
| `tank.overcrowdedPerWaterBlock` | 2 | ab so vielen Adulten pro Wasserblock gibt es Stress |
| `tank.minFillToInsert` | 0.5 | Füllung, ab der Tiere eingesetzt werden dürfen |
| `health.daysUntilSick` | 7 | Tage Hunger oder falsche Temperatur bis krank |
| `health.daysSickUntilDeath` | 14 | Krankheitstage bis zum Tod |
| `health.bothBadMultiplier` | 2 | schnellerer Stress bei zwei schlechten Bedingungen |
| `health.recoveryPerDay` | 2 | geheilte Krankheitstage pro gutem Tag |
| `health.oldAge.enabled` | false | Tod durch Altersschwäche (`years` 2, `daysPerYear` 96) |
| `temperature.tauDays` | 0.1 | wie schnell das Wasser seine Temperatur ändert |
| `breeding.*` | | Temperaturen, Nestbau, Eier, Überlebensrate, Pause und Schaden der Weibchen, Männchen fressen Jungfische, Mutation, Spiegeltraining (`mirrorReadinessBonus` 1,25) |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | Jungfische pro Flasche, Tage für einen Ansatz |
| `stages.*Days` | | Dauer der Lebensstufen |
| `interior.defaultScale` | 16 | 4, 8 oder 16 Innenraum-Blöcke pro Aquarium-Block (neue Aquarien) |
| `interior.substrateCostsBlock` | true | Bodengrund kostet einen Block |
| `interior.maxRenderedBlocks` | 262144 | Limit der Miniatur |
| `interior.leaveSneakTicks` | 60 | wie lange man zum Verlassen schleichen muss |
| `drops.artemiaFromSandChance` | 0.02 | Artemia-Zysten aus Sand |
| `waterQuality.qualityEnabled` | true | Wasserqualität an (Verlust pro Tag 0.01, pro Tier 0.04, pro Überfütterung 0.01, Filter entfernen 0.8, schlecht unter 0.35) |
| `waterQuality.externalFilterBlocks`, `internalFilterBlocks` | 32, 16 | Wasserblöcke, die ein Filter sauber hält |
| `evaporation.evaporationEnabled` | true | offene Aquarien verlieren Wasser (`mbPerBlockPerDay` 20) |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | Ausrüstung braucht FE |
| `power.heaterFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 8, 10, 4, 100000 | FE-Verbrauch und -Speicher |
| `foodBreeding.*` | | Larvenbottich (`larvaTubDays` 2, `larvaTubYield` 3) und Artemia-Zuchtbecken (`hatcheryDays` 1, `hatcheryYield` 2) |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | wann Spieler dieses Handbuch bekommen |
| `simulateUnloadedTime` | false | Zeit läuft in nicht geladenen Chunks weiter |

Die **Common-Config** legt Spawn-Gewicht und Gruppengröße der wilden Bettas fest und das Rezept der Fischtüte
(`recipes.fishBag`: AUTO, KELP oder PLASTIC). Die **Client-Config** enthält Animations- und Miniatur-Distanz, das
Glitzern der Wasseroberfläche, das Filtergeräusch, die verbundenen Glastexturen (`render.connectedGlass`) und die Breite
der Kantenlinien (`render.tankEdgeWidth`, in Pixeln zu 1/16 Block, 0,1 bis 1, Standard 0,25; 0 = keine).

## Dieses Handbuch

Das Handbuch braucht die Mod **GuideME**. Jeder Spieler bekommt es einmal, je nach `guide.giveBook`:

* `ON_JOIN`: beim ersten Betreten der Welt,
* `ON_FIRST_CATCH_OR_CRAFT`: mit dem ersten gefangenen Betta oder dem ersten hergestellten Bettacraft-Item,
* `NEVER`: gar nicht.

Ist das Inventar voll, landet das Buch vor deinen Füßen, und 60 Sekunden lang kannst nur du es aufheben. Ein neues
Exemplar: Stelle es aus einem Buch und Betta-Pellets her, oder bitte einen Operator um
`/guideme give <Spieler> bettacraft:guide`.

Ohne GuideME erklärt sich jedes Bettacraft-Item im Tooltip (Umschalt halten) und in JEI.

## Andere Mods

* **Jade, The One Probe und WTHIT:** Der Blick auf ein Aquarium zeigt Name, Temperatur, Wasser und Tiere (schleichen
  für die Liste; The One Probe braucht dafür seine Sonde), das Nest, die Filter und Warnungen; der Blick durch das Glas
  auf einen Betta zeigt Typ, Geschlecht, Gesundheit, Hunger, Stufe und Gene. Filter und Schränke zeigen ihre
  Heizstäbe und Kühler und das Aquarium, in das sie pumpen; Aquarien zeigen die Wasserqualität und den gespeicherten
  Strom, Larvenbottiche und Zuchtbecken ihren Ansatz.
* **Energie-Mods:** Jede Mod mit Forge-Energy-Kabeln (FE) kann Aquarien versorgen, wenn ein Server `power.*`
  einschaltet; Wasserleitungen können Aquarien füllen und leeren und den Larvenbottich füllen, Trichter und
  Item-Leitungen funktionieren mit Futterautomat, Schrank, Larvenbottich und Zuchtbecken.
* **JEI:** Info-Seiten für alle Bettacraft-Items.
* **Sodium und Iris:** Aquarium-Wasser und Miniaturen funktionieren mit beiden.
* **Better Fish Tanks:** Seine *Fish Bag* setzt Tiere in Bettacraft-Aquarien ein und nimmt sie über das Menü wieder
  heraus. Umgekehrt passen Bettas in seine Tüte und seine Aquarien; eine gefüllte Bettacraft-Fischtüte oder ein
  Betta-Eimer setzt das Tier per Rechtsklick in ein Better-Fish-Tanks-Aquarium.
* **Fintastic:** Seine Fische und das *Fischnetz* funktionieren in beide Richtungen; sein Fischfutter füttert Bettas,
  Feenkrebse, Daphnien und Ruderfußkrebse sind lebende Beute, und seine Wasserpflanzen (auch Wasserlinsen und
  Lotusblätter) lassen sich im trockenen Innenraum setzen. Arapaima und Quastenflosser sind zu groß für ein Aquarium.
* **Fish of Thieves:** Seine Fische leben in Bettacraft-Aquarien (Battlegill, Wrecker und Devilfish sind Raubfische),
  und ein Betta-Eimer setzt einen Betta auf seine *Fish Plaque*. Regenwürmer sind Lebendfutter.
* **Aquaculture 2:** Seine kleinen Fische sind willkommen, die großen (Thunfisch, Arapaima, Heilbutt, Wels, Karpfen …)
  nicht; der Piranha ist ein Raubfisch. Würmer sind Lebendfutter, seine rohen Fische und Filets ergeben Betta-Pellets.
* **Naturalist:** Barsch, Wels und andere kleine Tiere werden angenommen; Piranha und Anglerfisch sind Raubfische.
* **Sümpfe anderer Mods** (Biomes O' Plenty, Regions Unexplored …): Wildbettas erscheinen in jedem Biom aus
  `#c:is_swamp` über `#bettacraft:betta_spawn_biomes`.

## Befehle

* `/bettacraft interior leave`: einen Innenraum verlassen.
* Operatoren: `/bettacraft interior resync` (alle Miniaturen neu senden), `/bettacraft interior bench` (Messung).
