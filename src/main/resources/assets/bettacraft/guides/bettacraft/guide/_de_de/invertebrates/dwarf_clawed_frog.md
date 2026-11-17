---
navigation:
  title: Zwergkrallenfrosch
  icon: bettacraft:dwarf_frog_bucket
  parent: invertebrates.md
  position: 40
item_ids:
  - bettacraft:dwarf_frog_bucket
  - bettacraft:dwarf_frog_spawn_egg
---

# Zwergkrallenfrösche

![Ein Zwergkrallenfrosch holt an der Oberfläche Luft](../images/critters_frog.png)

Der Afrikanische Zwergkrallenfrosch (*Hymenochirus*, etwa 3,5 cm) ist wie die Bettas ein echtes Tier im Becken. Er läuft
und paddelt am Grund und schwimmt etwa alle zwei Minuten **zum Luftholen an die Oberfläche**. Er findet seinen Weg um
die Deko des Innenraums herum (Steine, Wurzeln, Pflanzen, Höhlen), statt durch sie hindurchzulaufen.

## Luft

* Ein Frosch braucht **Luft über dem Wasser**: den Luftspalt unter dem Glas (`tank.waterHeadroom` > 0, der Standard)
  oder ein Becken, das nicht bis oben voll ist. Ohne ertrinkt er langsam; Logbuch und Menü warnen dich.
* Im Tiere-Tab des Menüs zeigt ein Frosch seine Gesundheit, sein Futter und seine Luft.

## Futter

* Frösche fressen **Mückenlarven**, **Artemia** und **Pellets** (sie sinken zu Boden), kein Jungfischfutter.
* Sie sind **langsame Fresser**: Mit Bettas im Becken bekommt jeder Frosch seinen Anteil nur mit einer Chance (40 %,
  `frogs.frogFoodShareWithBettas`): Die Bettas schnappen ihn weg.
* **Schleichen + Rechtsklick** mit Futter auf das Aquarium: Es geht direkt an die Frösche. Der Futterautomat erreicht
  sie mit zwei oder drei Fütterungen am Tag öfter.
* Nach 4 Tagen ohne Futter beginnt ein Frosch zu hungern.

## Zucht

![Froschlaich an der Oberfläche und Kaulquappen](../images/frog_breeding.png)

Zwergkrallenfrösche vermehren sich wie in der Natur (Config `features.frogs.breeding`):

1. **Amplexus:** ein **kühler Wasserwechsel** (ein Viertel des Wassers innerhalb eines Vierteltags, 1,5 °C kühler, der
   Auslöser der Panzerwelse) in einem Becken mit **24–28 °C** bringt ein Paar in Stimmung: innerhalb des nächsten Tages
   umklammert ein erwachsenes, satt gefüttertes Männchen ein erwachsenes, satt gefüttertes Weibchen von hinten, und sie
   trägt ihn hinauf zur Oberfläche. Jeder Frosch hat ein Geschlecht (Frösche aus der Zeit vor 1.37 haben es aus ihrer
   UUID bekommen).
2. **Laich:** an der Oberfläche gibt sie 20–50 Eier ab; der Laich treibt dort zwischen den Schwimmpflanzen 2 Tage lang
   (`frogs.frogSpawnHatchDays`). **Bettas und andere Fische fressen ihn**, die Frösche auch ein wenig; **Schwimmpflanzen**
   verstecken das meiste.
3. **Kaulquappen** schlüpfen (höchstens 40 pro Becken, `frogs.maxTadpoles`). Sie schweben im freien Wasser und brauchen
   **feines Futter**: [Infusorien](../food/live_food.md), Artemia-Nauplien oder Aufzuchtfutter (ein bepflanztes Becken
   bildet ein wenig selbst). Zwei Tage ohne genug Futter, und sie verhungern. **Mit Bettas im Becken überlebt kaum eine**;
   Pflanzen verstecken sie ein wenig.
4. Nach 6 Tagen (`frogs.tadpoleDays`) wird eine Kaulquappe zum kleinen **Jungfrosch**, der in 4 Tagen
   (`frogs.frogletDays`) erwachsen wird und erst dann züchtet.

![Ein Paar im Amplexus: das Männchen hält sich am Rücken des Weibchens fest](../images/frog_amplexus.png)

Das Beckenprotokoll meldet jeden Schritt. Ein eigenes Becken für die Frösche, dichte Schwimmpflanzen und eine
Infusorien-Kultur sind das beste Zuchtbecken.

## Bezug

* **[Kescher](../equipment/tank_net.md)** in Sümpfen: 1,3 % pro Schwung (etwa 1 von 76 Schwüngen).
* **[Kescher](../equipment/tank_net.md)** in Dschungelgewässern: 4,2–4,8 % pro Schwung (etwa 1 von 21–24 Schwüngen).
* **Wild** in Sümpfen, in Gruppen von 1–2 (Spawn-Gewicht 3) (Config `features.frogs.wildSpawns`).
* **Wild** in Dschungelgewässern, in Gruppen von 1–2 (Spawn-Gewicht 4) (Config `features.frogs.wildSpawns`).
* **Fahrender Händler** (seltene Angebote): 1 für 6 Smaragde.

Die Biome von Biome-Mods zählen mit ([Biom-Tags](../settings.md)).
