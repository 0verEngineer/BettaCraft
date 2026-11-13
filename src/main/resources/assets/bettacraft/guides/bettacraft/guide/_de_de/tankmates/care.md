---
navigation:
  title: Pflege der Beifische
  icon: bettacraft:algae_wafer
  parent: tankmates.md
  position: 1
---

# Pflege der Beifische

Beifische haben dieselben Gesundheits-Timer wie die Bettas: Hunger, die falsche Temperatur, schlechtes Wasser und Stress
machen sie nach einigen Tagen krank (Standard 7), und kranke Fische sterben, wenn sich nichts ändert.

![Neonsalmler und Panzerwelse in einem bepflanzten Becken](../images/tm_care.png)

## Schwärme

* Salmler, Bärblinge, Panzerwelse, Otocinclus, Dornaugen und Prachtschmerlen sind **Schwarmfische**. In einer Gruppe
  unter der Mindestzahl ihrer Art (im Menü, 5 bis 8) sind sie gestresst (Config `tankmates.behaviour.shoalStress`).
* Lebendgebärende wollen eine kleine Gruppe (3), Harnischwelse und Zwergfadenfisch kommen allein zurecht.
* Der Schwarm folgt einem Leittier durchs Becken; Panzerwelse gründeln gemeinsam am Boden.

## Temperatur und Wasser

* Jede Art verträgt ihren eigenen Bereich (siehe [Tabelle](../tankmates.md)); außerhalb ist der Fisch gestresst.
* Manche sind **empfindlich gegen schlechtes Wasser** (Otocinclus, Roter Neon, Zebra-Harnischwels): Halte die
  Wasserqualität mit Filtern und Wasserwechseln hoch. Lebendgebärende und Antennenwelse sind robust.
* Frisches Wasser hat die Umgebungstemperatur: **Ein Wasserwechsel kühlt ein beheiztes Becken etwas ab**
  (`tankmates.breeding.waterChangeCools`). Das bringt die Panzerwelse in Laichstimmung (siehe [Zucht](breeding.md)).

## Futter

* Beifische fressen das Futter der Bettas: Pellets und Mückenlarven (Lebendfutter macht sie laichbereit), die
  Jungfische <ItemLink id="bettacraft:fry_powder" /> (oder Infusorien und Artemia wie die Betta-Jungen).
* **Bodenfische** (Panzerwelse, Harnischwelse, Otocinclus, Schmerlen) bekommen vom Futter, das an schnelleren Fischen
  vorbei sinkt, nur einen Teil ab (`tankmates.behaviour.bottomFoodShare`, 0,6). <ItemLink id="bettacraft:algae_wafer" />
  sinken direkt zu ihnen: Bettas und Fische im freien Wasser fressen keine Tabletten.
* **Algenfresser** (Otocinclus, Antennenwels; der L134 ein wenig) weiden die [Algen](../invertebrates/algae.md) am Glas
  ab und leben davon, solange welche da sind.

## Besatz

* Jede Art zählt einen Anteil eines Fisches: ein Moskitobärbling 0,12, ein Neon 0,35, ein Panzerwels 0,2 bis 0,6, ein
  Antennenwels 1,2, eine Prachtschmerle 2,5 (`tankmates.behaviour.tankmateStockingFactor`). Die Jungen zählen weniger,
  wie junge Bettas.
* Das gilt für die Besatz-Grenzen, die Belastung des Wassers (ein Neon ein Drittel eines Bettas) und den Zuchtstopp: Ein
  volles Becken bekommt keinen Nachwuchs mehr.

## Bettas und andere Fische

* **Betta-Männchen jagen Guppy-Männchen** (lange bunte Flossen) **und Zwergfadenfisch-Männchen** (Labyrinthfisch-Rivalen;
  sie wehren sich) und verletzen sie (`tankmates.behaviour.bettaAggression`). Guppy-, Endler- und Platy-Weibchen sind
  sicher.
* Zwei Zwergfadenfisch-Männchen kämpfen um ihr Revier; Harnischwels-Männchen ohne eigene Höhle werden vertrieben.
* **Prachtschmerlen fressen Schnecken** (`tankmates.behaviour.clownLoachSnailsPerDay`): eine natürliche
  Schneckenbekämpfung, aber nichts für ein Schneckenbecken.
* Jeder Erwachsene (Bettas und Beifische) **jagt die Jungfische** der Beifische und frisst die Eier, die er erreicht
  (`fryEatenPerDay`, `eggsEatenPerDay`). Pflanzen, Javamoos und Schwimmpflanzen verstecken die Jungen, ein Laichmop die
  Eier.

## Beschaffung

* Mit dem [Kescher](../invertebrates/catching.md): in **Dschungelgewässern** die südamerikanischen Arten (Salmler,
  Panzerwelse, Harnischwelse, Otocinclus), in **Sümpfen** die südostasiatischen (Bärblinge, Dornauge, Zwergfadenfisch,
  selten Prachtschmerlen), in **Flüssen** Lebendgebärende, Antennenwelse und Gelbflossen-Panzerwelse. Ein Teil der Fänge
  sind Fische (`tankmates.behaviour.netFishShare`), Schwarmfische kommen zu eins bis vier.
* Der **fahrende Händler** verkauft einen Schwarm von sechs (oder ein Paar) in einer Fischtüte, ab und zu in einer
  Zuchtform.
* Und durch Zucht.
