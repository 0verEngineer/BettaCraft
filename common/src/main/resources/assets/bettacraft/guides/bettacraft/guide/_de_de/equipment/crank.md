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

Die Kurbel zieht Geräte mit einer **Feder** auf, und sie lädt den **Energiespeicher eines Beckens**. Mit der Kurbel in der
Hand rechtsklicken: jeder Klick zieht einen Schritt auf, und die Ratsche klickt etwas höher. **Taste halten**, um weiter
aufzuziehen (etwa fünf Klicks pro Sekunde). Die Aktionsleiste zeigt, wie weit es aufgezogen ist.

## Ein Becken: Unterschrank und Außenfilter

Die Kurbel an einem [Unterschrank](cabinet.md) unter einem Becken oder an einem angeschlossenen [Außenfilter](filters.md)
lädt den **einen Energiespeicher** des Beckens (siehe [Strom](power.md)), denselben Speicher, den FE-Kabel laden. Alle
Geräte des Beckens laufen damit: Filter, Heizer und Kühler, Heizmatten, Lichter, Luftpumpen, Sprühanlagen.

* Jeder Klick gibt **6.500 FE** in den Speicher (`power.crankFePerClick`). Etwa **100 Klicks, 20 Sekunden** Halten,
  betreiben ein mittleres Becken (2 x 2 x 2 mit Filter, Licht und Heizer) **einen Spieltag** auf `power.costs` `EASY`; auf
  `HARD` dauert dasselbe dreimal so lange, und der Speicher reicht ein Drittel Tag.
* Die Aktionsleiste zeigt den Speicher in Prozent, sein FE und wie viele Stunden er beim Verbrauch des Beckens reicht.
  Ist er voll, sagt sie es.
* Ein Schrank, der unter keinem Becken steht (und an keine Schränke eines Beckens angebaut ist), und ein Filter, der an
  keinem Becken hängt, haben nichts zu laden.
* Mit `power.costs` `OFF` braucht kein Gerät Strom, und die Kurbel hat dort nichts zu tun.

## Geräte mit eigener Feder

**Mit `power.costs` `OFF`** brauchen sie nur zwei Maschinen: sie haben keinen anderen Antrieb.

* [Futtermaschine](../food/food_maker.md) und [Düngermaschine](../plants/fertilizer_maker.md): eine volle Feder arbeitet
  einen Tag.
* Jedes andere Gerät läuft einfach, ohne Strom und ohne Aufziehen.

**Mit `power.costs` `EASY` oder `HARD`** lassen sich die Geräte ohne Becken statt mit FE aufziehen
(`features.power.winding`):

| Gerät | Feder |
|---|---|
| [Futtermaschine](../food/food_maker.md), [Düngermaschine](../plants/fertilizer_maker.md) | 1 Ladung; erst FE, dann die Feder |
| [Multiblock-Filter](multiblock_filter.md) (an jedem Teil aufziehen) | 6 Ladungen, sechsmal so lange zum Aufziehen |
| [Osmoseanlage](reverse_osmosis.md) | 1 Ladung für die Druckpumpe (viermal so viel Wasser) |
| [Wasserwechsel-Steuerung](water_change.md), [Wasserpumpe](pipes.md) | 1 Ladung: ein Tag Arbeit einer Pumpe |

Eine Ladung ist ein Spieltag Arbeit eines Geräts und braucht etwa fünf Sekunden Aufziehen.

* **FE geht immer vor.** Eine Feder läuft nur, solange FE fehlt, so sichern sich Akku und Feder gegenseitig ab.
* Eine Feder läuft nur in geladenen Chunks ab; Zeit in nicht geladenen Chunks zählt nicht.
* Die Fenster der Maschinen zeigen die Feder als Messinganzeige; Jade, The One Probe und WTHIT zeigen sie auch (an einem
  Unterschrank oder Außenfilter: den Speicher des Beckens).

**Ältere Welten:** Federn, die Unterschränke und Außenfilter früher aufgezogen hatten, werden beim ersten Lauf des Beckens
zu FE in seinem Speicher (10 FE für jeden Tick Arbeit eines Geräts, den sie hielten), auch über die Kapazität des
Speichers hinaus: nichts geht verloren.

Die Einstellungen stehen in der Server-Config: `power.crankFePerClick` (6500) für die Becken, und im Abschnitt `winding`
`ticksPerCharge` (24000), `clicksPerCharge` (25) und die Ladungen jedes Geräts (`multiblockFilterCharges` 6, ...). Der
Schalter `features.power.tankCrank` schaltet die Kurbel an Becken ab (dann lädt sie nur FE).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:crank" />
