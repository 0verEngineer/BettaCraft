---
navigation:
  title: Becken mit Create bewegen
  icon: minecraft:minecart
  parent: settings.md
  position: 30
feature: compat.create.enabled
---

# Becken mit Create bewegen

Mit **Create** kann ein Aquarium reisen: auf einem mechanischen Kolben, einem Lager, einem Seilzug, einem Portalkran,
einer Lore oder einem Zug, und mit **Create Aeronautics** auf einem Luftschiff (Schalter `features.compat.create.contraptions`,
standardmäßig an).

## Die Fahrt

* **Das ganze Becken fährt mit.** Alle Blöcke eines Aquariums halten wie verklebt zusammen; nimmt eine Contraption einen
  davon mit, kommt das ganze Becken mit. Geräte außen am Glas fahren nur mit, wenn sie zur Contraption gehören (mit
  Superkleber ans Becken geklebt, oder ein Unterschrank darunter geklebt): ein Außenfilter mit seinen Heizern und Kühlern,
  der Unterschrank mit seinem Filter, ein [Antriebsadapter](../equipment/drive_adapter.md). Was stehen bleibt, arbeitet
  wieder, sobald das Becken zurück ist.
* **Die Tiere werden** für die Fahrt ins Becken **verpackt**, wie in einen Fischbeutel: Bettas, Beifische und die Fische
  anderer Mods. Auf der Contraption sieht man das Aquarium mit Wasser, Miniatur-Innenraum und den Tieren, die ihre
  Runden schwimmen: ein Bild der verpackten Tiere, wie es ihnen gerade geht (ein hungriges oder krankes Tier schwimmt
  langsamer und tiefer).
* **Die Pflege läuft weiter**, solange das Becken reist (Schalter `features.compat.create.travelCare`, standardmäßig an),
  siehe unten. Mit dem Schalter aus pausiert sie wie bisher: niemand bekommt Hunger, das Wasser bleibt, wie es ist,
  nichts wächst oder vermehrt sich.
* **Abgesetzt** (der Kolben fährt zurück, das Lager hält an, der Zug kommt an, das Luftschiff wird abgebaut) ist es
  wieder dasselbe Aquarium: sein Wasser und dessen Werte, Name und Logbuch, die Schwimmpflanzen, Garnelen und
  Schnecken, der Innenraum mit Pflanzen und Deko und jedes Tier an seinem Platz im Becken. Wird das Becken gedreht
  abgesetzt, dreht sich der Innenraum mit; liegt es auf der Seite, kommt sein alter Bodengrund als Gegenstände heraus.
* Blöcke eines Beckens, die nicht ankommen (die Contraption konnte sie nicht absetzen), zählen als abgebaut.

![Ein Becken auf einer Loren-Contraption in der Kurve: das Wasser wird nach außen gedrückt](../images/travel_curve.png)

## Pflege unterwegs

Auf einer Create-Contraption hat das Aquarium während der Fahrt keinen eigenen Block, nur seine Daten. Jede Sekunde der
Fahrt werden diese Daten gepflegt wie ein Becken am Boden:

* **Hunger und Gesundheit:** Bettas, Beifische und die Tiere anderer Mods mit Pflegeprofil bekommen Hunger, schlechtes
  Wasser oder die falsche Temperatur stressen sie, wie im Becken. Unterwegs stirbt niemand: ein Tier am Limit wartet auf
  die Ankunft (dann entscheidet die normale Pflege). Garnelen, Schnecken und die anderen Kleintiere warten einfach.
* **Temperatur:** das Wasser geht zur Luft des Bioms, in dem die Contraption gerade ist, bei Tag und Nacht. Mitfahrende
  Heizer und Kühler (Heizstäbe im Innenraum, Heizer in einem Filter auf der Contraption) halten den Zielwert.
* **Wasserqualität:** der Abfall der Tiere verschmutzt das Wasser, mitfahrende Filter reinigen es (Innen- und Eckfilter
  des Innenraums, ein Außen- oder Schrankfilter auf der Contraption, Lebendgestein).
* **Die Zucht pausiert** auf einer Contraption, und niemand kann eingesetzt oder herausgenommen werden.
* **Angekommen:** das Logbuch des Beckens nennt, wie lange es unterwegs war, wie viel der Zeit seine Geräte Strom hatten
  und wie viel FE die Contraption gegeben hat.

## Strom unterwegs

Wo ein Server die Geräte FE brauchen lässt (`power.heater`, `power.filter`), nehmen mitfahrende Filter und Heizer es wie
am Boden. Ohne Strom stehen sie still, und die Werte werden langsam schlechter, wie bei einem Stromausfall. Auf einer
Contraption kommt das FE aus:

* dem eigenen **Puffer** des Beckens (was es bei der Abfahrt gespeichert hatte);
* mitfahrenden **Antriebsadaptern** am Becken: auf einer Contraption dreht sich kein Create-Netz, also dreht sich ein
  Adapter mit der Fahrt selbst. Bei `createTravel.dynamoFullSpeed` Blöcken pro Sekunde (standardmäßig 8) gibt er die volle
  Leistung eines Anschlusses (`createDrive.fePerTickPerInput`), langsamer anteilig; auf einem Lager zählt, wie schnell sich
  der Adapter bewegt, einer an der Achse gibt also nichts;
* **FE-Akkus** anderer Mods, die neben dem Becken an die Contraption geklebt sind (bis zu `createTravel.batteryFePerTick`
  FE pro Tick je Akku).

Der Fortschritt **Fischtransport mit Strom** kommt mit der ersten Fahrt, bei der Filter und Heizer mit diesem Strom liefen.

## Schwappendes Wasser

Das Wasser eines reisenden Beckens ist nicht starr: beschleunigt oder bremst die Contraption, fährt sie eine Kurve oder
dreht ein Lager sie, kippt die Oberfläche in die Richtung, in die das Wasser gedrückt wird, schwingt nach einem Halt
zurück, und Wellen laufen darüber. Kippt ein Lager das Becken, bleibt die Oberfläche waagerecht. Nur das Bild ändert
sich, nicht das Wasser oder die Tiere. `render.travelSloshing` (Client-Config) macht es stärker oder schwächer, 0 schaltet
es ab.

![Ein Becken auf einem Lager, das plötzlich anhält: das Wasser schwingt zurück](../images/travel_stop.png)

## Wenn ein Becken nicht abgesetzt werden kann

Create lässt einen Block, den es nicht setzen kann (ein unzerstörbarer Block ist im Weg), als Gegenstand fallen. Vom
Aquarium geht nichts verloren:

* Sind andere Blöcke des Beckens angekommen, sind sie das Aquarium, mit Wasser, Werten, Innenraum und jedem Tier; nur
  der fehlende Block zählt als abgebaut.
* Ist keiner angekommen (ein Becken aus einem Block, oder alle sind gescheitert), ist der fallengelassene Beckenblock
  ein **verpacktes Becken**: sein Tooltip sagt, wie viele Tiere darin reisen. Irgendwo wieder gesetzt, ist es dasselbe
  Aquarium mit Innenraum und Tieren; es steht als dieser eine Block (der Innenraum der anderen Blöcke kommt als
  Gegenstände heraus).

Wird eine Contraption mit einem Becken darauf zerstört (per Befehl getötet, aus der Welt gefallen), ist das Becken mit
ihr verschwunden, und mit ihm sein Innenraum. Züge, die in ungeladene Chunks fahren, behalten ihre Becken.

## Rotation für die Geräte

Eine drehende Create-Welle, ein Zahnrad, Getriebe oder Motor, dessen Welle auf eine **Wasserpumpe**, einen
**Filterpumpenkopf**, eine **Wasserwechsel-Steuerung**, einen **Füllstandssensor**, eine **Osmoseanlage** oder eine
**Futtermaschine** zeigt, treibt sie statt FE an (Schalter `features.compat.create.rotation`):

* **64 U/min** (in jede Richtung) ist volle Leistung, langsamer anteilig: 32 U/min sind der halbe Durchfluss, der halbe
  Schub.
* Pumpen und Steuerungen brauchen die Rotation nur dort, wo ein Server sie FE brauchen lässt (`power.costs`).
* Die Osmoseanlage bekommt den Schub ihrer Druckpumpe durch Rotation auch ohne Stromkosten.
* Die Futtermaschine mit FE läuft mit der Rotation; ohne FE zieht die Rotation ihre Feder auf (bei voller Drehzahl
  doppelt so schnell, wie die Maschine sie verbraucht), die Kurbel braucht es nicht.
* Die Geräte kosten **keine Belastung (Stress)**: sie gehören nicht zu Creates Netz, sie nehmen nur die Drehzahl der
  Welle. Ein überlastetes Netz gibt ihnen nichts.

**Das ganze Becken an einer Welle:** ein [Antriebsadapter](../equipment/drive_adapter.md) an einer Beckenwand oder unter
dem Becken, oder eine Welle an der Seite des Schranks darunter, lädt den FE-Puffer des Beckens, so laufen alle Geräte des
Beckens mit Drehkraft. Diese beiden kosten **echten Stress**, nach der Last der Geräte (Schalter
`features.compat.create.drive`).

## Display Links

Ein **Display Link** von Create an einem Aquariumblock liest **Beckenwerte**: eine Zeile pro Wert (Temperatur,
Wasserqualität, pH, TDS, Sauerstoff, CO2, Nitrat, Füllstand, Tiere, Futter, Salzgehalt usw.), so viele, wie das Ziel
Zeilen hat; ein Schild zeigt also die ersten vier, eine Anzeigetafel so viele, wie sie hoch ist. An einem **Monitor**
liest er **Überwachte Becken**: eine Zeile pro Becken des Monitors mit Name, Temperatur und Wasserqualität (Schalter
`features.compat.create.displayLinks`).

## Create-Rohre

Creates Flüssigkeitsrohre, Pumpen, Ausgießer und Schlauchrollen füllen und leeren Aquarien über denselben Anschluss wie
die Bettacraft-Rohre: frisches Wasser hinein, verbrauchtes heraus, wie die Rohre jeder anderen Mod.

## Wann ein Becken nicht reisen kann

Die Contraption startet nicht, und Spieler in der Nähe lesen den Grund:

* jemand ist im Innenraum des Beckens;
* im Schaumnest liegen Eier;
* Eier oder Jungtiere der Beifische, die Ablaichbox oder Schildkröteneier würden die Fahrt nicht überstehen.

## Auf einem Luftschiff

Auf einem Luftschiff von Create Aeronautics bleibt das Aquarium ein echter Block auf dem Schiff. Mit
`features.compat.create.travelCare` an lebt es einfach weiter: die Bettas und die anderen Tiere von Bettacraft schwimmen
darin auf dem Schiff, fressen, wachsen und vermehren sich, und Pflege, Geräte und Strom arbeiten wie am Boden: ein
Filter, Heizer oder eine Lampe auf dem Schiff, FE aus Kabeln und Akkus, ein [Antriebsadapter](../equipment/drive_adapter.md)
an einer Welle, die ein Generator des Schiffs dreht. Die Temperatur kommt von der Luft, wo das Schiff fliegt. Tiere von
Minecraft und anderen Mods reisen verpackt (das Luftschiff würde sie hinausdrängen), und den Innenraum kann während des
Flugs niemand betreten. Abgebaut ist alles wieder am Boden. Mit dem Schalter aus wartet das Becken verpackt, bis das
Luftschiff wieder abgebaut wird, wie bisher.

![Ein Becken mit seinen Bettas auf einem Luftschiff](../images/travel_airship.png)
