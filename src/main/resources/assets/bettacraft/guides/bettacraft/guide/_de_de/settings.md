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
| `tank.maxSizeX/Y/Z` | 16 | größtes Aquarium (bis 64) |
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
| `breeding.*` | | Temperaturen, Nestbau, Eier, Überlebensrate, Pause und Schaden der Weibchen, Männchen fressen Jungfische, Mutation |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | Jungfische pro Flasche, Tage für einen Ansatz |
| `stages.*Days` | | Dauer der Lebensstufen |
| `interior.defaultScale` | 16 | 4, 8 oder 16 Innenraum-Blöcke pro Aquarium-Block (neue Aquarien) |
| `interior.substrateCostsBlock` | true | Bodengrund kostet einen Block |
| `interior.maxRenderedBlocks` | 262144 | Limit der Miniatur |
| `interior.leaveSneakTicks` | 60 | wie lange man zum Verlassen schleichen muss |
| `drops.artemiaFromSandChance` | 0.02 | Artemia-Zysten aus Sand |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | wann Spieler dieses Handbuch bekommen |
| `simulateUnloadedTime` | false | Zeit läuft in nicht geladenen Chunks weiter |

Die **Common-Config** legt Spawn-Gewicht und Gruppengröße der wilden Bettas fest und das Rezept der Fischtüte
(`recipes.fishBag`: AUTO, KELP oder PLASTIC). Die **Client-Config** enthält Animations- und Miniatur-Distanz, das
Glitzern der Wasseroberfläche und das Filtergeräusch.

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

* **Jade:** Der Blick auf ein Aquarium zeigt Name, Temperatur, Wasser und Tiere (schleichen für die Liste), das Nest und
  Warnungen; der Blick durch das Glas auf einen Betta zeigt Typ, Geschlecht, Gesundheit, Hunger und Stufe. Filter und
  Schränke zeigen ihre Heizstäbe.
* **JEI:** Info-Seiten für alle Bettacraft-Items.
* **Sodium und Iris:** Aquarium-Wasser und Miniaturen funktionieren mit beiden.

## Befehle

* `/bettacraft interior leave`: einen Innenraum verlassen.
* Operatoren: `/bettacraft interior resync` (alle Miniaturen neu senden), `/bettacraft interior bench` (Messung).
