---
navigation:
  title: Filterleitungen und Schutzaufsätze
  icon: bettacraft:filter_inlet_pipe
  parent: equipment/multiblock_filter.md
  position: 1
item_ids:
  - bettacraft:filter_inlet_pipe
  - bettacraft:filter_outlet_pipe
  - bettacraft:pipe_tee
  - bettacraft:intake_guard
  - bettacraft:outlet_guard
---

# Filterleitungen und Schutzaufsätze

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:filter_inlet_pipe" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_inlet_pipe" x="1" p:west="true" p:east="true" p:up="true" p:junction="true" />
  <Block id="bettacraft:filter_inlet_pipe" x="2" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_inlet_pipe" x="1" y="1" p:down="true" p:up="true" />
  <Block id="bettacraft:filter_outlet_pipe" z="1" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_outlet_pipe" x="1" z="1" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_outlet_pipe" x="2" z="1" p:west="true" p:east="true" />
</GameScene>

![Ein Multiblock-Filter für drei Aquarien: die blaue Einlassleitung und die orange Rücklaufleitung mit T-Stücken](../images/filter_lines.png)

Ein [Multiblock-Filter](multiblock_filter.md) versorgt über eigene Rohre **mehrere Aquarien**:

* <ItemLink id="bettacraft:filter_inlet_pipe" /> (blau): die **Einlassleitung**, von den Aquarien zum Einlass des Filters.
* <ItemLink id="bettacraft:filter_outlet_pipe" /> (orange): die **Rücklaufleitung**, vom Auslass des Filters zurück in die
  Aquarien.

Ein Filterrohr verbindet nur Rohre seiner eigenen Leitung, seinen Anschluss am Filter und Aquariumblöcke. Ein normales Rohr
verbindet **höchstens zwei Nachbarn** (gerade oder als Bogen; zuerst den Block, an den du es setzt). Ein
<ItemLink id="bettacraft:pipe_tee" /> auf einem Rohr macht es zur **Abzweigung**: es verbindet dann jeden Nachbarn seiner
Leitung, bis zu allen sechs Seiten, so erreicht ein Filter mehrere Aquarien. Wer eine Abzweigung abbaut, bekommt Rohr und
T-Stück zurück.

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

Wer das auf seinem Server nicht will, schaltet die Filterleitungen ab: `features.pipes.filterLines = false` in der
Common-Config. Dann versorgt ein Filter wie früher ein Aquarium, ohne Schutzaufsätze, und die Filterrohre leiten nichts.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:filter_inlet_pipe" />
<RecipesFor id="bettacraft:filter_outlet_pipe" />
<RecipesFor id="bettacraft:pipe_tee" />
<RecipesFor id="bettacraft:intake_guard" />
<RecipesFor id="bettacraft:outlet_guard" />
