---
navigation:
  title: Kurbel und Aufziehen
  icon: bettacraft:crank
  parent: equipment.md
  position: 91
item_ids:
  - bettacraft:crank
---

# Kurbel und Aufziehen

<ItemImage id="bettacraft:crank" scale="2" />

![Die Kurbel zieht einen Unterschrank unter einem Aquarium auf](../images/crank.png)

Die Kurbel zieht Geräte mit einer **Feder** auf. Rechtsklicke das Gerät mit der Kurbel in der Hand: jeder Klick zieht
ein Stück auf und die Ratsche klickt etwas höher. **Taste halten** zieht weiter auf: eine **Ladung** (ein Spieltag
Arbeit eines Geräts) dauert etwa **fünf Sekunden**. Die Aktionsleiste zeigt, wie weit die Feder aufgezogen ist.

## Welche Geräte die Kurbel nehmen

**Ohne Strom-Optionen** brauchen sie nur zwei Maschinen: sie haben keinen anderen Antrieb.

* [Futtermaschine](../food/food_maker.md) und [Düngermaschine](../plants/fertilizer_maker.md): eine volle Feder arbeitet einen Tag.
* Alle anderen Geräte laufen einfach, ohne Strom und ohne Aufziehen.

**Mit einer Strom-Option** (siehe [Strom](power.md)) lässt sich jedes Gerät, das FE braucht, stattdessen auch aufziehen
(`features.power.winding`): so hältst du ein Aquarium ganz ohne Stromerzeuger.

| Gerät | Strom-Option | Feder |
|---|---|---|
| [Futtermaschine](../food/food_maker.md), [Düngermaschine](../plants/fertilizer_maker.md) | jede | 1 Ladung; erst FE, dann die Feder |
| [Außenfilter](filters.md) | `power.filterRequiresEnergy` oder `heaterRequiresEnergy` | 1 Ladung; treibt auch die Heizer und Kühler darin an |
| [Multiblock-Filter](multiblock_filter.md) (an jedem Teil aufziehen) | `pipes.pumpsRequireEnergy` | 6 Ladungen, sechsmal so langes Aufziehen |
| [Unterschrank](cabinet.md) unter einem Aquarium | jede `power.*`-Option | 6 Ladungen für **alle Geräte seines Aquariums** |
| [Osmoseanlage](reverse_osmosis.md) | jede | 1 Ladung für die Druckpumpe (viermal so viel Wasser) |
| [Wasserwechsel-Steuerung](water_change.md), [Wasserpumpe](pipes.md) | `pipes.pumpsRequireEnergy` | 1 Ladung: ein Tag Pumpenarbeit |

Innen- und Eckfilter, Lichter, Futterautomat, Dosierer, Sprühanlagen und Heizmatten haben keine eigene Feder: zieh den
**Unterschrank** unter dem Aquarium auf. Er speichert das Aufziehen und gibt es an alle Geräte seines Aquariums, die FE
brauchen: seinen Filter und seine Luftpumpe, Außen- und Innenfilter, Heizer und Kühler, Heizmatten, Sprühanlagen, Lichter
und Luftpumpen. Jedes arbeitende Gerät verbraucht pro Tick einen Geräte-Tick, also **je mehr Geräte, desto schneller ist
er leer**: ein Filter läuft mit einem vollen Schrank sechs Tage, sechs Geräte einen Tag.

* **FE geht immer vor.** Die Feder läuft nur, solange FE fehlt; Akku und Feder ergänzen sich.
* Eine Feder läuft nur, solange ihr Chunk geladen ist; Zeit in nicht geladenen Chunks zählt nicht.
* Ohne FE und ohne aufgezogene Feder reinigt ein Filter nicht, die Tiere leben aber weiter (wie immer ohne Strom).
* Das Fenster des [Außenfilters](filters.md), des Unterschranks und der Maschinen zeigt die Feder als Messinganzeige; auch
  Jade, The One Probe und WTHIT zeigen sie. Der Tab **Wasser** sagt, wie viele Geräte die Federn gerade antreiben.

Die Einstellungen stehen in der Server-Config unter `winding`: `ticksPerCharge` (24000), `clicksPerCharge` (25) und die
Ladungen jedes Geräts (`externalFilterCharges` 1, `multiblockFilterCharges` 6, `cabinetCharges` 6, ...).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:crank" />
