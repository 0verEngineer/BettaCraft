---
navigation:
  title: Großbecken
  icon: bettacraft:transport_tub
  position: 75
feature: bigTanks.enabled
---

# Großbecken und große Tiere

Becken können bis zu **32 × 16 × 32 Blöcke** groß werden (Config `bigTankRules.bigMaxSize*`). In ein solches Becken passen
die **großen Tiere**: Riesenwelse, Haie, Rochen und sogar der Vanilla-**Delfin**. Kleine Fische kommen wie bisher in jedes
Becken; ein großes Tier nur in ein Becken, das groß genug für es ist.

![Ein 32 × 8 × 16 Meerwasserbecken mit zwei Delfinen](images/big_tank_dolphins.png)

## Die Größenregel

Ein Tier bekannter Länge braucht je Meter seiner Länge:

* **12 Blöcke** entlang der längeren Beckenseite, **6 Blöcke** entlang der kürzeren und **3 Blöcke** Höhe;
* **100 Wasserblöcke** je Kubikmeter seiner Länge, für jedes große Tier (sie addieren sich).

Ein 1 m langer Pangasius oder Bambushai braucht also mindestens 12 × 6 Blöcke und 3 Blöcke Höhe (100 Wasserblöcke), ein
2,6 m langer Delfin 32 × 16 Blöcke und 8 Blöcke Höhe (etwa 1760 Wasserblöcke; zwei Delfine passen in 32 × 16 × 8). Ist
ein Becken zu klein, nennt die Meldung die Größe, die das Tier braucht. Die Faktoren stehen in der Config
(`bigAnimalRules.*`).

Wale, Bosse und feindliche Mobs kommen nie in ein Becken; Tiere anderer Mods zählen als groß, wenn sie im Tag
`#bettacraft:big_tank_animals` stehen (ihre Länge ergibt sich aus ihrer Größe).

## Der Innenraum eines Großbeckens

Ein Becken, das länger als **16 Blöcke** ist, bekommt einen kleineren Innenraum-Maßstab: **8** statt 16 (und **4** ab 33
Blöcken), damit sein Innenraum etwa 256 Blöcke lang bleibt (`bigTankRules.interiorScale8Above`, `interiorScale4Above`).
Becken bis 16 Blöcke behalten Maßstab 16.

Wächst ein Becken mit eingerichtetem Innenraum **über 16 Blöcke hinaus**, wird sein Innenraum in den gröberen
**verkleinert**: jeder Würfel aus 2 × 2 × 2 Blöcken wird ein Block (der häufigste), das Layout bleibt in halber Größe
erhalten. Nichts geht verloren: Blöcke, die nicht bleiben, Geräte mit Blockdaten, Truhen samt Inhalt und die
Aquarienpflanzen (ganz, mit ihrer Stufe) kommen als Items vor das Becken, bei vielen in Truhen gepackt. Bezahlter Bodengrund
über den neuen Boden hinaus kommt ebenfalls zurück.

## Delfine

Der Vanilla-Delfin lebt nur in einem **sehr großen Meerwasserbecken** (ab 32 × 16 × 8 Blöcken, Salz ab 25 ‰). Im Becken
atmet er an der Oberfläche und bleibt von selbst feucht. Füttere ihn mit **rohem Fisch** per Rechtsklick aufs Becken,
oder leg rohen Fisch in einen [Futterautomaten](equipment/feeder.md) auf dem Becken: bei jeder Fütterung bekommt jeder
Delfin, der eine Weile keinen Fisch hatte, einen. Ein Delfin, der einen Tag ohne Fisch war, **jagt die kleinen Fische**
seines Beckens (Beifische, Bettas, Frösche; einen am Tag); nach zwei Tagen ohne Fisch (`bigAnimalRules.dolphinHungryDays`)
verliert er Gesundheit, ebenso in zu süßem Wasser. Fang einen mit dem [großen Kescher](big_tanks/large_net.md) in eine
[Transportwanne](big_tanks/transport_tub.md).

## Fangen und tragen

Große Tiere passen in keinen Eimer und keinen Fischbeutel. Fang sie mit dem [großen Kescher](big_tanks/large_net.md) oder
beim Angeln mit einem [Stahlvorfach](big_tanks/steel_leader.md), immer in eine leere
[Transportwanne](big_tanks/transport_tub.md), die ein Tier trägt und dich bremst, solange du sie trägst. Aus einem Becken
nimmst du ein großes Tier im Beckenmenü (eine leere Transportwanne im Inventar).

Große Tiere fressen die kleinen Fische, jungen Bettas und Garnelen ihres Beckens; halte sie mit Fischen ihrer Größe. Sie
schwimmen weite Bögen und wenden langsam.

## Riesen des Süßwassers

Neben dem [Pangasius](big_tanks/pangasius.md) leben vier weitere Riesen im Süßwasser: der räuberische
[Rotflossen-Antennenwels](big_tanks/redtail_catfish.md) und der [Arapaima](big_tanks/arapaima.md), ein Luftatmer, aus den
Dschungelgewässern, der [Europäische Wels](big_tanks/wels_catfish.md) kühler Flüsse (keine Heizung nötig) und der
[Süßwasserstechrochen](big_tanks/motoro_stingray.md), der über den Sand gleitet und auf ihm liegt. Die Welse ruhen tagsüber
am Boden. Nur der Stechrochen vermehrt sich im Becken: er bringt lebende Junge zur Welt, so viele, wie die Größenregel
Platz lässt.

## Riesen des Meeres

Fünf große Tiere leben in **Meerwasserbecken**: der [Bambushai](big_tanks/bamboo_shark.md), ein kleiner Bodenhai, der
**Eikapseln** („Nixentäschchen“) unter die Deko legt, der [Schwarzspitzen-Riffhai](big_tanks/blacktip_reef_shark.md), der
ständig schwimmen muss und jeden kleinen Fisch frisst, der [Gefleckte Adlerrochen](big_tanks/spotted_eagle_ray.md) und der
Schwärme bildende [Kuhnasenrochen](big_tanks/cownose_ray.md), die mit langsamen Flügelschlägen durchs freie Wasser fliegen,
und die [Große Netzmuräne](big_tanks/honeycomb_moray.md), die in einer Höhle unter der Deko lebt und nachts und zum Fressen
herauskommt. Wild leben sie in warmen (der Kuhnasenrochen auch in lauwarmen) Ozeanen.

![Ein großes Meerwasserbecken mit Haien und Rochen](images/big_tank_sea.png)

## In diesem Kapitel

<SubPages icons={true} />

## Große Tiere

| Art | °C | Schwarm | Betta-Becken | Zucht |
|---|---|---|---|---|
| <ItemImage id="bettacraft:pangasius_spawn_egg" /> [Pangasius](big_tanks/pangasius.md) | 22–30 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:redtail_catfish_spawn_egg" /> [Rotflossen-Antennenwels](big_tanks/redtail_catfish.md) | 20–28 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:wels_catfish_spawn_egg" /> [Europäischer Wels](big_tanks/wels_catfish.md) | 4–26 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:arapaima_spawn_egg" /> [Arapaima](big_tanks/arapaima.md) | 24–31 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:motoro_stingray_spawn_egg" /> [Süßwasserstechrochen](big_tanks/motoro_stingray.md) | 24–30 | – | nein (zu groß) | lebendgebärend |
| <ItemImage id="bettacraft:bamboo_shark_spawn_egg" /> [Bambushai](big_tanks/bamboo_shark.md) | 22–28 | – | nein (zu groß) | legt Eikapseln |
| <ItemImage id="bettacraft:blacktip_reef_shark_spawn_egg" /> [Schwarzspitzen-Riffhai](big_tanks/blacktip_reef_shark.md) | 24–30 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:spotted_eagle_ray_spawn_egg" /> [Gefleckter Adlerrochen](big_tanks/spotted_eagle_ray.md) | 24–29 | – | nein (zu groß) | vermehrt sich nicht im Aquarium |
| <ItemImage id="bettacraft:cownose_ray_spawn_egg" /> [Kuhnasenrochen](big_tanks/cownose_ray.md) | 18–28 | 3 | nein (zu groß) | lebendgebärend |
| <ItemImage id="bettacraft:honeycomb_moray_spawn_egg" /> [Große Netzmuräne](big_tanks/honeycomb_moray.md) | 24–28 | – | nein (frisst sie) | vermehrt sich nicht im Aquarium |
