---
navigation:
  title: Filterleitungen und Schutzaufsätze
  icon: bettacraft:stainless_steel_pipe
  parent: equipment/multiblock_filter.md
  position: 1
item_ids:
  - bettacraft:pipe_wrench
  - bettacraft:intake_guard
  - bettacraft:outlet_guard
---

# Filterleitungen und Schutzaufsätze

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:stainless_steel_pipe" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="1" p:west="true" p:east="true" p:up="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="1" y="1" p:down="true" p:up="true" p:ring="intake" />
  <Block id="bettacraft:glass_pipe" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:glass_pipe" x="1" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:glass_pipe" x="2" z="1" p:west="true" p:east="true" p:ring="return" />
</GameScene>

![Ein Multiblock-Filter für drei Aquarien: die blaue Zulaufleitung und die orange Rücklaufleitung verzweigen zu jedem Aquarium](../images/filter_lines.png)

Ein [Multiblock-Filter](multiblock_filter.md) versorgt über [Rohre](pipes.md) **mehrere Aquarien**. Eigene Filterrohre gibt
es nicht: jedes Glas- oder Edelstahlrohr, das den **Einlass** des Filters erreicht, ist seine **Zulaufleitung** (blaue Ringe
an den Blockgrenzen), jedes, das seinen **Auslass** erreicht, seine **Rücklaufleitung** (orange Ringe). Die Aufgabe ergibt
sich aus dem Anschluss, den die Leitung erreicht; ein Rohr ohne Anschluss ist eine normale Wasserleitung ohne Ring.

Ein Rohr verbindet sich **mit jedem passenden Nachbarn, auf jeder Seite**, so verzweigt eine Leitung von selbst zu jedem
Aquarium, das sie erreicht. Wo zwei Leitungen nebeneinander laufen, hält die <ItemLink id="bettacraft:pipe_wrench" /> sie
getrennt: Rechtsklick auf die Seite eines Rohrs schließt sie (eine geschlossene Seite zeigt eine Kappe; noch ein Rechtsklick
öffnet sie). Eine Leitung, die den Einlass **und** den Auslass erreicht, ist ein **Kurzschluss** (rote Ringe): der Filter
steht still und sagt es in seinem Fenster und in Jade / The One Probe / WTHIT.

Eine Leitung trägt höchstens, was ihr **schwächstes Rohr** trägt: ein Glasrohr 50 mB/t, ein Edelstahlrohr 250 mB/t. Der
Pumpenkopf bewegt bis zu 100 mB/t, an Glasleitungen läuft der Filter also nur halb. Mehrere Zulauf- und Rücklaufleitungen
(mehr Einlässe und Auslässe am Filter) **zählen zusammen**: zwei Glasleitungen je Richtung geben ihm seinen vollen
Durchfluss zurück. Zwei Leitungen können auch durch einen Block laufen, ein [Rohrbündel](pipe_bundle.md).

Ein Aquarium zählt für den Filter, wenn **beide Leitungen** hinführen. Reinigung und Durchfluss des Filters werden **nach
dem Wasservolumen** seiner Aquarien aufgeteilt: ein großer Filter reicht für mehrere kleine Aquarien. Jedes Aquarium behält
sein eigenes Wasser; das Filter-Fenster zeigt jedes Aquarium mit Anteil, Durchfluss und gereinigten Wasserblöcken, oder
warum es nicht gefiltert wird.

![Das Filter-Fenster: zwei Aquarien mit je 50 %, das dritte ohne Ansaugschutz](../images/filter_screen_tanks.png)

## Andocken überall

Die Filterrohre docken an **jeder Seite, oben oder unten** an einem Aquarium an. Wo ein Rohr außen andockt, erscheint innen
ein **Rohranschluss**, in der Mitte genau dieser Fläche (an der Wand, unter dem Deckel oder am Boden; bei einer
[Bodendurchführung](bulkhead.md) wird deren Anschluss genommen). Vom Anschluss legst du [Innenraum-Rohre](bulkhead.md)
dorthin, wo das Wasser hinein- oder hinausgehen soll. Ein Rohr, das **ins Aquarium hinein und wieder hinaus** führt, geht
nicht (es wäre nur ein Rohr durchs Aquarium): es lässt sich nicht setzen, und wird es trotzdem gebaut, bleiben beide
Anschlüsse zu.

## Schutzaufsätze

Jeder Anschluss braucht im Aquarium einen **Schutzaufsatz**, damit kein Tier eingesaugt wird:

* <ItemLink id="bettacraft:intake_guard" />: ein feines Gitter an der Einlassleitung (ein Ansaugkorb zählt auch).
* <ItemLink id="bettacraft:outlet_guard" />: eine Gitterkappe an der Rücklaufleitung (eine Lily Pipe, Düse oder ein
  Sprühbalken zählen auch).

Den Schutz innen am Anschluss oder am Ende der Innenraum-Rohre setzen, oder von außen auf das Glas anwenden, wo das Rohr
andockt: er kommt direkt vor den Anschluss. **Ohne seinen Schutz filtert der Filter dieses Aquarium nicht**: eine rote
Zeile im Aquariummenü (Reiter Wasser), in Jade / The One Probe / WTHIT und im Filter-Fenster sagt, was fehlt.

Ein Schutz, der neben ein Rohr oder einen Anschluss gesetzt wird, dreht sein Gitter von selbst davon weg (beim Setzen schleichen: er schaut
wohin du siehst); Schleichen + Rechtsklick mit leerer Hand auf einen Schutz dreht ihn zur nächsten freien Seite.

![Der Ansaugschutz und der Rohranschluss an der Seitenwand im Aquarium](../images/pipe_guards.png)

## Filter von vor dem Update

**Wichtig:** Die Schutzaufsätze braucht **jeder** Multiblock-Filter, auch die, die vor dieser Version gebaut wurden. Nach dem
Update behält ein alter Filter Medien, Regler und Strom, **filtert aber nicht mehr, bis seine Aquarien ihre Schutzaufsätze
haben**; Spieler in der Nähe bekommen eine Chatnachricht mit seiner Position, und das Filter-Fenster zeigt eine rote
Notiz. Einen Ansaug- und einen Auslassschutz dort einbauen, wo seine Rohre ins Aquarium führen, und er filtert wieder.

Die Filterrohre älterer Versionen (alle vier Leitungen), ihre T-Stücke und die Frisch- und Abwasseranschlüsse des Filters
sind jetzt **Edelstahlrohre**, mit genau den Verbindungen, die sie hatten (die Seiten, an denen sie nicht verbunden waren, sind
zu, wie mit der Zange); Bündel behalten ihre Leitungen. In Inventaren und Kisten werden sie zu Edelstahlrohren, das
Innenraum-Rohr zu einem Glasrohr.

Wer das auf seinem Server nicht will, schaltet die Filterleitungen ab: `features.pipes.filterLines = false` in der
Common-Config. Dann versorgt ein Filter wie früher ein Aquarium, ohne Schutzaufsätze, und seine Rohre sind normale Wasserrohre.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:pipe_wrench" />
<RecipesFor id="bettacraft:intake_guard" />
<RecipesFor id="bettacraft:outlet_guard" />
