---
navigation:
  title: "Wasser im Aquarium: wie alles zusammenhängt"
  icon: bettacraft:stainless_steel_pipe
  parent: equipment.md
  position: 41
---

# Wasser im Aquarium: wie alles zusammenhängt

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:fill="14" />
  <Block id="bettacraft:water_change_controller" x="1" p:facing="east" />
  <Block id="bettacraft:glass_pipe" x="1" z="1" p:north="true" p:south="true" p:ring="fresh" />
  <Block id="bettacraft:reservoir" x="1" z="2" p:fill="6" />
  <Block id="bettacraft:drain" x="1" z="-1" />
  <Block id="bettacraft:level_sensor" x="0" y="1" p:facing="east" />
</GameScene>

Wasser bewegt sich zwischen Aquarien, Filtern und Geräten durch **ein einziges Rohrsystem**. Es gibt nur zwei Rohre, Glas
und Edelstahl, und was ein Rohr tut, ergibt sich daraus, woran es angeschlossen ist. Diese Seite zeigt, wie die Teile
zusammenspielen; jedes Teil hat unten seine eigene Seite.

## Die Teile

| Teil | Was es tut |
|---|---|
| [Rohre und Wasserpumpe](pipes.md) | Glas- (50 mB/t) und Edelstahlrohre (250 mB/t) verbinden sich mit jedem passenden Nachbarn; eine Pumpe drückt Wasser |
| [Rohrzange](pipe_wrench.md) | schließt und öffnet einzelne Seiten eines Rohrs, damit Leitungen nebeneinander getrennt bleiben |
| [Rohrbündel](pipe_bundle.md) | zwei bis vier Rohre in einem Block, jedes seine eigene Leitung |
| [Rohranschlüsse und Innenraum-Rohre](bulkhead.md) | wo eine Leitung ins Aquarium führt, und die Rohre und Auslässe darin |
| [Schutzaufsätze](guards.md) | ein Ansaug- oder Auslassschutz an jedem Anschluss hält die Tiere drin |
| [Multiblock-Außenfilter](multiblock_filter.md) und seine [Filterleitungen](filter_lines.md) | ein Filter für mehrere Aquarien: eine Zulauf- und eine Rücklaufleitung |
| [Wasserwechsel-Steuerung](water_change.md) | Wasserwechsel und Nachfüllen über vier Anschlüsse: Frischwasser rein und raus, Abwasser rein und raus |
| [Niveausensor](level_sensor.md) | misst den Wasserstand, gibt ein Redstone-Signal; die Steuerung füllt danach nach |
| [Abfluss](drain.md) | das Ende einer Abwasserleitung: entsorgt das Wasser |
| [Vorratsbehälter](reservoir.md) | fasst 16 Eimer: eine Frischwasserquelle |
| [Rohrverkleidung und Facaden](pipe_cover.md) | versteckt Rohre hinter einem beliebigen vollen Block |

## Leitungen und ihre Aufgaben

Alle Rohre, die miteinander verbunden sind, bilden eine **Leitung**. Eine Leitung bekommt ihre Aufgabe vom **Anschluss**,
den sie erreicht, und zeigt sie mit einem **Farbring** an jeder Blockgrenze:

* der **Einlass** eines Multiblock-Filters: Zulaufleitung, **blau**;
* der **Auslass** eines Multiblock-Filters: Rücklaufleitung, **orange**;
* die Frischwasser-Anschlüsse einer Wasserwechsel-Steuerung (rein links, raus hinten): Frischwasserleitung, **grün**;
* die Abwasser-Anschlüsse einer Wasserwechsel-Steuerung (rein oben, raus rechts): Abwasserleitung, **braun**;
* gar kein Anschluss: eine normale Wasserleitung ohne Ring (sie trägt, was eine Pumpe hineindrückt).

Eine Leitung, die Anschlüsse **zweier verschiedener Aufgaben** erreicht, ist ein **Kurzschluss** (rote Ringe): der Filter
oder die Steuerung steht still und sagt warum. Solche Leitungen hält die [Rohrzange](pipe_wrench.md) auseinander, oder sie
laufen durch ein [Bündel](pipe_bundle.md). Eine Leitung trägt höchstens, was ihr **schwächstes Rohr** trägt.

Frischwasser kommt immer **durch ein Rohr**: ein Vorratsbehälter, eine Wasserpumpe an einer Wasserquelle, eine
Umkehrosmose-Anlage oder der Tank einer anderen Mod am Ende der Frischwasserleitung der Steuerung. Was nur neben der
Steuerung oder dem Niveausensor steht, zählt nicht.

## Beispiel: ein einzelnes Aquarium

Die Szene oben: eine Wasserwechsel-Steuerung mit dem **Rücken am Glas**. Die Berührung ersetzt ihre beiden beckenseitigen
Anschlüsse, sie braucht also nur zwei Rohre:

1. ein Glasrohr von einem <ItemLink id="bettacraft:reservoir" /> (oder einer Wasserpumpe an einer Wasserquelle) zu ihrem
   **linken** Anschluss (Frischwasser rein, grüner Ring);
2. ein <ItemLink id="bettacraft:drain" /> direkt an ihrem **rechten** Anschluss (Abwasser raus);
3. ein **Ansaugschutz** im Aquarium dort, wo die Steuerung es berührt, damit der Wasserwechsel keine Jungtiere einsaugt;
4. ein <ItemLink id="bettacraft:level_sensor" /> am Aquarium: die Steuerung liest ihn und füllt verdunstetes Wasser nach.

Ein Filter nach Wahl hält das Wasser sauber: ein Innen- oder Außenfilter oder ein Multiblock-Filter.

## Beispiel: ein Filter, mehrere Aquarien

1. Einen [Multiblock-Filter](multiblock_filter.md) bauen und von seinem **Einlass** und seinem **Auslass** je ein Rohr zu
   jedem Aquarium legen. Die Rohre verzweigen von selbst; die Leitungen werden blau und orange.
2. Wo die Leitungen andocken, in jedem Aquarium einen **Ansaugschutz** an den blauen und einen **Auslassschutz** an den
   orangen Anschluss setzen (von außen auf den Flansch reicht, siehe [Schutzaufsätze](guards.md)).
3. Glasleitungen tragen 50 mB/t, der Filter pumpt bis zu 100: Edelstahl nehmen, oder zwei Leitungen je Richtung.
4. Eine [Wasserwechsel-Steuerung](water_change.md) **an eine Filterkammer oder den Pumpenkopf** stellen (oder ihre Leitungen
   Frischwasser raus und Abwasser rein zu einer Kammer oder dem Pumpenkopf führen). Sie wechselt das Wasser **aller
   Aquarien des Filters** und füllt die mit Niveausensor nach. Das Frischwasser geht über den Rücklauf des Filters hinein.

Die Leitungen der Steuerung nicht an Einlass oder Auslass des Filters anschließen: das ist ein Kurzschluss.

## Beispiel: ein zentraler Technikraum

Alles steht in einem Raum, die Aquarien woanders im Haus:

* **Frischwasser:** ein Vorratsbehälter (oder eine Wasserpumpe an einer Wasserquelle, oder eine Umkehrosmose-Anlage) und eine
  grüne Leitung zum Frischwasser-Einlass **mehrerer Steuerungen**. Anschlüsse derselben Aufgabe an einer Leitung sind in
  Ordnung, kein Kurzschluss.
* **Jede Steuerung steht frei** und nutzt alle vier Anschlüsse: Frischwasser raus (hinten) und Abwasser rein (oben) laufen zu
  ihrem Aquarium; beide Leitungen dürfen am selben Beckenblock andocken, als [Bündel](pipe_bundle.md) an einer Fläche oder an
  zwei Flächen eines Kantenblocks. Die Abwasser-Ausgänge (rechts) teilen sich eine Leitung zu einem Abfluss.
* **Filter:** Multiblock-Filter im Raum mit ihren Zulauf- und Rücklaufleitungen zu den Aquarien. Die Leitungen laufen
  nebeneinander in Bündeln durch Wände und Böden; [Rohrverkleidungen](pipe_cover.md) verstecken sie.
* **Leitungen getrennt halten:** wo sich zwei Leitungen berühren, die Seite mit der [Rohrzange](pipe_wrench.md) schließen;
  ein roter Ring zeigt, wo zwei Aufgaben aufeinandertreffen.
* Jade, The One Probe und WTHIT zeigen Aufgabe und Durchfluss jeder Leitung, die du ansiehst.

<SubPages icons={true} />
