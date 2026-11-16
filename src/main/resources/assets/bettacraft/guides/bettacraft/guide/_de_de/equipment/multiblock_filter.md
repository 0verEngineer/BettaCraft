---
navigation:
  title: Multiblock-Außenfilter
  icon: bettacraft:filter_pump_head
  parent: equipment.md
  position: 38
item_ids:
  - bettacraft:filter_chamber
  - bettacraft:filter_pump_head
  - bettacraft:filter_inlet
  - bettacraft:filter_outlet
  - bettacraft:filter_inlet_pipe
  - bettacraft:filter_outlet_pipe
  - bettacraft:pipe_tee
  - bettacraft:intake_guard
  - bettacraft:outlet_guard
---

# Multiblock-Außenfilter

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:filter_chamber" p:medium="sponge" />
  <Block id="bettacraft:filter_chamber" y="1" p:medium="ceramic" />
  <Block id="bettacraft:filter_pump_head" y="2" p:active="true" />
  <Block id="bettacraft:filter_inlet" x="-1" p:facing="west" />
  <Block id="bettacraft:filter_outlet" x="-1" y="1" p:facing="west" />
</GameScene>

![Ein Multiblock-Filter aus vier Kammern mit Rohren zu einem Aquarium](../images/multiblock_filter.png)

Ein großer Außenfilter zum Selbstbauen, in **jeder Größe**:

* <ItemLink id="bettacraft:filter_chamber" />: 1 bis 16 Kammern Seite an Seite (eine Säule, ein 2x2-Block ...). Jede
  hält ein [Filtermedium](filter_media.md): Rechtsklick damit setzt es ein, leere Hand nimmt es heraus.
* <ItemLink id="bettacraft:filter_pump_head" />: **genau einer**, an den Kammern.
* <ItemLink id="bettacraft:filter_inlet" /> (blau) und <ItemLink id="bettacraft:filter_outlet" /> (orange): je mindestens
  einer, an eine Kammer gesetzt; ihre Muffe zeigt nach außen und verbindet ein [Rohr](pipes.md) oder einen Aquariumblock.

Der Pumpenkopf zieht Wasser durch den Einlass und drückt es aus dem Auslass (100 mB/t bei voller Leistung). Führen
**Ein- und Auslass zum selben Aquarium** (direkt, über Rohre oder über eine [Bodendurchführung](bulkhead.md)), hält der
Filter dieses Aquarium sauber: jede Kammer trägt bei, was ihr Medium reinigt (Schwamm 12, Keramik 24, Kohle 16, Torf 6
Wasserblöcke), weniger je schmutziger das Medium, mal Leistungsregler. Verstopfte Schwämme bremsen auch den Durchfluss.

Rechtsklick auf den Pumpenkopf (oder mit leerer Hand auf eine Kammer): das Filter-Fenster mit Größe, Problemen, dem
gereinigten Aquarium, Durchfluss und dem Medium jeder Kammer samt Schmutz. Schleichen + Mausrad:
[Leistungsregler](regulator.md). Mit `pipes.pumpsRequireEnergy` braucht der Pumpenkopf FE. Ein
[Niveausensor](top_off.md) kann auch am Filter sitzen.

**Unter dem Aquarium:** Der Filter kann auch direkt unter seinem Aquarium stehen, wie ein Schrankfilter: Kammern am
Boden, Ein- und Auslass obendrauf mit dem Anschluss nach oben in den Aquariumboden (oder Kammern direkt unter dem Boden
und die Anschlüsse seitlich mit Rohren nach oben). Mit einem Block in der Hand setzt ein Klick auf ein Filterteil den
Block, statt den Bildschirm zu öffnen, so kannst du das Aquarium obendrauf bauen.

Der alte Ein-Block-[Außenfilter](filters.md) bleibt die kleine Variante.


## Mehrere Aquarien, Schutzaufsätze

Seit M40 versorgt ein Filter **mehrere Aquarien**. Das <ItemLink id="bettacraft:filter_inlet_pipe" /> (blau) führt von
den Aquarien zum Einlass, das <ItemLink id="bettacraft:filter_outlet_pipe" /> (orange) vom Auslass zurück. Ein Filterrohr
verbindet höchstens zwei Nachbarn; ein <ItemLink id="bettacraft:pipe_tee" /> darauf macht eine Abzweigung. Die Rohre
docken an **jeder Seite, oben oder unten** an einem Aquarium an: der Rohranschluss erscheint innen an dieser Stelle, und
Innenraum-Rohre führen von dort weiter. Reinigung und Durchfluss werden **nach dem Wasservolumen** der Aquarien aufgeteilt.

Jeder Anschluss braucht innen einen **Schutzaufsatz**: einen <ItemLink id="bettacraft:intake_guard" /> (oder einen
Ansaugkorb) an der Einlassleitung, einen <ItemLink id="bettacraft:outlet_guard" /> (oder eine Lily Pipe, Düse oder einen
Sprühbalken) an der Rücklaufleitung. Den Schutz auf das Glas anwenden, wo das Rohr andockt. **Ohne Schutz filtert der
Filter dieses Aquarium nicht** (Warnung im Aquarium- und im Filtermenü), und ein Rohr, das ins Aquarium hinein und wieder
hinaus führt, geht nicht. **Das gilt auch für Filter, die vor dem Update gebaut wurden: Schutzaufsätze nachrüsten.**

## Bezug

Herstellung:

<RecipesFor id="bettacraft:filter_chamber" />
<RecipesFor id="bettacraft:filter_pump_head" />
<RecipesFor id="bettacraft:filter_inlet" />
<RecipesFor id="bettacraft:filter_outlet" />
<RecipesFor id="bettacraft:filter_inlet_pipe" />
<RecipesFor id="bettacraft:filter_outlet_pipe" />
<RecipesFor id="bettacraft:pipe_tee" />
<RecipesFor id="bettacraft:intake_guard" />
<RecipesFor id="bettacraft:outlet_guard" />
