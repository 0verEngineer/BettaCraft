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
  davon mit, kommt das ganze Becken mit. Geräte außen am Glas (Heizer, Filter, Lampen, Unterschränke) fahren nicht mit:
  sie bleiben stehen und arbeiten wieder, sobald das Becken zurück ist. Müsste die Contraption eines dieser Geräte
  mitnehmen (am Becken verklebt, vor einem Kolben), startet sie nicht.
* **Die Tiere werden** für die Fahrt ins Becken **verpackt**, wie in einen Fischbeutel: Bettas, Beifische und die Fische
  anderer Mods. Auf der Contraption sieht man das Aquarium mit Wasser, Miniatur-Innenraum und den Tieren, die ihre
  Runden schwimmen, aber das ist nur ein Bild der verpackten Tiere.
* **Die Pflege pausiert**, solange das Becken reist: niemand bekommt Hunger, das Wasser bleibt, wie es ist, nichts wächst
  oder vermehrt sich.
* **Abgesetzt** (der Kolben fährt zurück, das Lager hält an, der Zug kommt an, das Luftschiff wird abgebaut) ist es
  wieder dasselbe Aquarium: sein Wasser und dessen Werte, Name und Logbuch, die Schwimmpflanzen, Garnelen und
  Schnecken, der Innenraum mit Pflanzen und Deko und jedes Tier an seinem Platz im Becken. Wird das Becken gedreht
  abgesetzt, dreht sich der Innenraum mit; liegt es auf der Seite, kommt sein alter Bodengrund als Gegenstände heraus.
* Blöcke eines Beckens, die nicht ankommen (die Contraption konnte sie nicht absetzen), zählen als abgebaut.

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
* Pumpen und Steuerungen brauchen die Rotation nur dort, wo ein Server sie FE brauchen lässt (`pipes.pumpsRequireEnergy`).
* Die Osmoseanlage bekommt den Schub ihrer Druckpumpe durch Rotation auch ohne die Strom-Optionen.
* Die Futtermaschine mit FE läuft mit der Rotation; ohne FE zieht die Rotation ihre Feder auf (bei voller Drehzahl
  doppelt so schnell, wie die Maschine sie verbraucht), die Kurbel braucht es nicht.
* Die Geräte kosten **keine Belastung (Stress)**: sie gehören nicht zu Creates Netz, sie nehmen nur die Drehzahl der
  Welle. Ein überlastetes Netz gibt ihnen nichts.

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

Auf einem Luftschiff von Create Aeronautics bleibt das Becken unterwegs, bis das Luftschiff wieder abgebaut wird:
während des Flugs kann man keine Tiere einsetzen oder herausnehmen und den Innenraum nicht betreten.
