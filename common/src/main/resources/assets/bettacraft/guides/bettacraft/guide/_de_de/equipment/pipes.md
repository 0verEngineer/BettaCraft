---
navigation:
  title: Rohre und Wasserpumpe
  icon: bettacraft:glass_pipe
  parent: equipment/water_system.md
  position: 1
item_ids:
  - bettacraft:glass_pipe
  - bettacraft:stainless_steel_pipe
  - bettacraft:water_pump
---

# Rohre und Wasserpumpe

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:fill="14" />
  <Block id="bettacraft:glass_pipe" x="1" p:west="true" p:east="true" />
  <Block id="bettacraft:water_pump" x="2" p:facing="east" />
  <Block id="bettacraft:stainless_steel_pipe" x="3" p:west="true" p:east="true" />
  <Block id="bettacraft:reservoir" x="4" />
</GameScene>

![Rohre, eine Wasserpumpe und ein Multiblock-Filter an einem Aquarium](../images/pipes.png)

Es gibt **zwei Rohre**, und sie sind für alles da: normale Wasserleitungen, die Leitungen des
[Multiblock-Filters](multiblock_filter.md) und die der [Wasserwechsel-Steuerung](water_change.md), außen und innen im
Aquarium. Wie alles zusammenhängt: [Wasser im Aquarium](water_system.md).

* <ItemLink id="bettacraft:glass_pipe" />: günstig, trägt **50 mB pro Tick**.
* <ItemLink id="bettacraft:stainless_steel_pipe" />: trägt **250 mB pro Tick**.

Rohre tragen **Wasser mit all seinen Werten**: Temperatur, Salz, Qualität und Nitrat reisen mit (auch Umkehrosmose-Wasser,
Meerwasser und Lebendfutter-Suspension). Wasser aus einem Eimer oder einer Wasserquelle ist frisches normales Wasser.

## Verbinden

Ein Rohr verbindet sich **mit jedem passenden Nachbarn, auf jeder Seite**: anderen Rohren, [Rohrbündeln](pipe_bundle.md),
Aquariumblöcken, dem [Vorratsbehälter](reservoir.md), dem [Abfluss](drain.md), den Kammern und dem Pumpenkopf eines
Multiblock-Filters, den Anschlüssen des Filters und der Wasserwechsel-Steuerung, Geräten wie der Wasserpumpe, der
Lebendfutter-Pumpe oder der Dosierpumpe, Kesseln und den **Tanks und Rohren anderer Mods** (Mekanism, EnderIO, Create, AE2,
RS ...: der normale Fluid-Handler). So verzweigt eine Leitung von selbst; besondere Teile braucht es nicht. Soll sich ein Rohr
mit etwas daneben nicht verbinden, schließt die [Rohrzange](pipe_wrench.md) diese Seite. Schleichen + Rechtsklick mit einem
Rohr auf ein Rohr, und beide laufen in einem Block weiter, einem [Bündel](pipe_bundle.md).

Wo eine Leitung ein Aquarium erreicht, dockt sie mit einem Stahlflansch am Glas an; bei Filterleitungen und der
Abwasserleitung der Steuerung erscheint innen ein [Rohranschluss](bulkhead.md), von dem aus Rohre im Innenraum weitergehen.
Ein Glas- oder Edelstahlrohr, das **im** Aquarium gesetzt wird, ist dort ein Innenraum-Rohr.

## Aufgaben und Farbringe

Alle miteinander verbundenen Rohre sind eine **Leitung**, und der **Anschluss**, den eine Leitung erreicht, gibt ihr die
Aufgabe, gezeigt durch einen Farbring an jeder Blockgrenze:

| Anschluss | Leitung | Ring |
|---|---|---|
| Einlass eines Multiblock-Filters | Zulaufleitung | blau |
| Auslass eines Multiblock-Filters | Rücklaufleitung | orange |
| Frischwasser rein oder raus einer Wasserwechsel-Steuerung | Frischwasserleitung | grün |
| Abwasser rein oder raus einer Wasserwechsel-Steuerung | Abwasserleitung | braun |
| keiner | normale Wasserleitung | keiner |

![Ringe nach Aufgabe: eine blaue Zulaufleitung, eine orange Rücklaufleitung und ein roter Kurzschluss zwischen zwei Filteranschlüssen](../images/pipe_roles.png)

Eine Leitung, die Anschlüsse **zweier verschiedener Aufgaben** erreicht, ist ein **Kurzschluss** und wird **rot**: eine
Zulaufleitung, die auch den Auslass des Filters erreicht, oder eine Steuerung, deren Frischwasser rein und Frischwasser raus
an einer Leitung hängen (beide grün, aber zwei Aufgaben). Der Filter oder die Steuerung steht still und sagt es in ihrem
Fenster und in Jade / The One Probe / WTHIT; Spieler in der Nähe bekommen einen Chat-Hinweis. Mehrere Anschlüsse
**derselben** Aufgabe an einer Leitung sind in Ordnung (zwei Steuerungen an einer Frischwasser-Versorgung).

Filterleitungen gehören ihrem Filter und tragen nur sein Wasser. Die Leitungen der Steuerung tragen, was sie bewegt und was
Pumpen in ihren Frischwasser-Einlass drücken; normale Leitungen tragen, was eine Pumpe hineindrückt.

## Durchfluss

Eine Leitung trägt höchstens, was ihr **schwächstes Rohr** trägt. Ein Multiblock-Filter pumpt bis zu 100 mB/t, an einer
Glasleitung läuft er also nur halb; mehrere Leitungen einer Aufgabe zählen zusammen (zwei Glas-Zulaufleitungen und zwei
Glas-Rücklaufleitungen geben ihm seinen vollen Durchfluss). Ein Wasserwechsel bewegt je Schritt, was seine Leitungen tragen.
Jade, The One Probe und WTHIT zeigen die Aufgabe eines Rohrs und was seine Leitung pro Sekunde trägt (50 mB pro Tick =
1.000 mB pro Sekunde); ein Rohr speichert kein Wasser.

## Wasserpumpe

**Rohre pumpen nicht.** Die <ItemLink id="bettacraft:water_pump" /> zieht Wasser aus dem Block hinter ihr und drückt es in
den Block vor ihr, **200 mB pro Tick**. Beim Setzen dorthin schauen, wohin das Wasser soll. Rechtsklick oder Schleichen +
Mausrad: [Leistungsregler](regulator.md). Redstone hält sie an. Mit Stromkosten (`power.costs` `EASY` oder `HARD`) braucht
sie FE (`pumpFePerTick` 8). Eine Pumpe an einer Wasserquelle mit einem Rohr zum Frischwasser-Einlass einer
Wasserwechsel-Steuerung gibt ihr unbegrenzt Leitungswasser.

Rohre tragen auch **Lebendfutter**: eine [Lebendfutter-Pumpe](../food/live_food_pump.md) neben einer Zucht drückt
Lebendfutter-Suspension durch sie in die Becken.

## Rohre aus älteren Versionen

Welten von vor 1.57 stellen sich beim Laden von selbst um, und nichts geht verloren: die Filterrohre aller vier Arten, ihre
T-Stücke und die Frisch- und Abwasseranschlüsse des Filters sind **Edelstahlrohre** mit genau den Verbindungen, die sie
hatten (die übrigen Seiten sind zu, wie mit der Zange); Bündel behalten ihre Leitungen. In Inventaren, Kisten und den
Speichern anderer Mods werden sie zu Edelstahlrohren, das alte Innenraum-Rohr zu einem Glasrohr.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:glass_pipe" />
<RecipesFor id="bettacraft:stainless_steel_pipe" />
<RecipesFor id="bettacraft:water_pump" />
