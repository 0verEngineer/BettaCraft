---
navigation:
  title: "Filterleitungen: ein Filter, mehrere Aquarien"
  icon: bettacraft:stainless_steel_pipe
  parent: equipment/multiblock_filter.md
  position: 1
---

# Filterleitungen: ein Filter, mehrere Aquarien

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:filter_chamber" p:medium="sponge" />
  <Block id="bettacraft:filter_pump_head" y="1" p:active="true" />
  <Block id="bettacraft:filter_inlet" x="1" p:facing="east" />
  <Block id="bettacraft:filter_outlet" x="1" y="1" p:facing="east" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="3" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" y="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:stainless_steel_pipe" x="3" y="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:tank" x="4" p:fill="14" />
  <Block id="bettacraft:tank" x="4" y="1" p:fill="14" />
</GameScene>

Ein [Multiblock-Filter](multiblock_filter.md) versorgt über gewöhnliche [Rohre](pipes.md) **mehrere Aquarien**:

* jedes Glas- oder Edelstahlrohr, das den **Einlass** des Filters erreicht, ist seine **Zulaufleitung** (blaue Ringe);
* jedes, das seinen **Auslass** erreicht, seine **Rücklaufleitung** (orange Ringe).

Ein Rohr verbindet sich mit jedem passenden Nachbarn, so **verzweigt** jede Leitung **von selbst** zu jedem Aquarium, das sie
erreicht. Ein Aquarium zählt für den Filter, wenn **beide Leitungen** an ihm andocken (direkt, an jeder Seite, oben oder
unten), oder wenn Einlass und Auslass es direkt berühren.

![Ein Multiblock-Filter mit blauer Zulauf- und oranger Rücklaufleitung, die zu drei Aquarien verzweigen](../images/filter_lines.png)

## Teilen

Reinigung und Durchfluss des Filters werden **nach dem Wasservolumen** seiner Aquarien aufgeteilt: ein großer Filter reicht
für mehrere kleine Aquarien. Jedes Aquarium behält sein eigenes Wasser. Das Filter-Fenster sagt „Versorgt 3 Becken:“ und
zeigt jedes Aquarium mit Anteil, Durchfluss und gereinigten Wasserblöcken, oder warum es nicht gefiltert wird.

![Das Filter-Fenster: zwei Aquarien mit je 50 %, das dritte ohne Ansaugschutz](../images/filter_screen_tanks.png)

## Durchfluss

Eine Leitung trägt höchstens, was ihr **schwächstes Rohr** trägt: Glas 50 mB/t, Edelstahl 250 mB/t. Der Pumpenkopf bewegt
bis zu 100 mB/t, an Glasleitungen läuft der Filter also nur halb; das Filter-Fenster sagt „Seine Leitungen tragen höchstens
...“. Mehrere Zulauf- und Rücklaufleitungen (mehr Einlässe und Auslässe am Filter) **zählen zusammen**: zwei Glasleitungen
je Richtung geben ihm seinen vollen Durchfluss zurück. Leitungen können nebeneinander in einem [Rohrbündel](pipe_bundle.md)
laufen, auch zwei derselben Aufgabe.

## Anschlüsse und Schutzaufsätze

Wo eine Leitung an einem Aquarium andockt, erscheint innen ein [Rohranschluss](bulkhead.md) (einer je Leitung eines Bündels),
von dem aus Innenraum-Rohre zu den Auslässen weitergehen. Jeder Anschluss braucht seinen [Schutzaufsatz](guards.md): einen
Ansaugschutz an der Zulaufleitung, einen Auslassschutz an der Rücklaufleitung. **Ohne seine Schutzaufsätze filtert der
Filter dieses Aquarium nicht.**

## Kurzschluss

Eine Leitung, die den Einlass **und** den Auslass erreicht, ist ein **Kurzschluss** (rote Ringe): der Filter steht still und
sagt es in seinem Fenster und in Jade / The One Probe / WTHIT. Benachbarte Leitungen hält die [Rohrzange](pipe_wrench.md)
auseinander. Dasselbe passiert, wenn eine Leitung einer [Wasserwechsel-Steuerung](water_change.md) den Einlass oder Auslass
des Filters erreicht: die Leitungen der Steuerung stattdessen an eine **Kammer oder den Pumpenkopf** anschließen (Rohre
verbinden sich dort mit jeder Fläche, ohne Aufgabe).

![Ein Kurzschluss: eine Leitung verbindet Einlass und Auslass des Filters, ihre Ringe werden rot](../images/filter_short_circuit.png)

Mit `features.pipes.filterLines = false` versorgt ein Filter wie früher ein Aquarium, ohne Schutzaufsätze, und seine Rohre
sind normale Wasserrohre.
