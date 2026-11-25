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


## Mehrere Aquarien: Filterleitungen

Seit Version 1.32 versorgt ein Filter über eigene Rohre **mehrere Aquarien**, und bis zu vier Leitungen laufen in einem Block:

* [Filterleitungen und Schutzaufsätze](filter_lines.md): die Einlass- und Rücklaufrohre, T-Stücke, Andocken an jeder
  Seite, die Schutzaufsätze.
* Das Menü des Filters sagt „Versorgt 3 Becken:“ über der Liste seiner Becken, und sein **Frischwasser** ist, was er nutzen kann: das Wasser in seinen
  Frischwasser-Anschlüssen plus das an den Enden ihrer Leitungen (ein [Vorratsbehälter](top_off.md), der Tank einer anderen Mod) oder „Leitungswasser,
  unbegrenzt“, wenn ein Wasserquellenblock an einem Anschluss liegt.
* [Rohrbündel, Frisch- und Abwasser](pipe_bundle.md): vier Leitungen in einem Block, Wasserwechsel und Nachfüllen für
  jedes Aquarium des Filters.
* [Rohrverkleidung und Facaden](pipe_cover.md): die Rohre hinter jedem vollen Block verstecken.

**Wichtig für alte Welten:** Jeder Multiblock-Filter braucht jetzt in jedem seiner Aquarien einen **Ansaug- und einen
Auslassschutz**, auch Filter, die vor dem Update gebaut wurden. Bis sie eingebaut sind, **filtert der Filter nicht** (rote
Notiz in seinem Fenster und im Aquariummenü). Server können mit `features.pipes.filterLines = false` beim alten Weg bleiben.

<SubPages icons={true} />

## Bezug

Herstellung:

<RecipesFor id="bettacraft:filter_chamber" />
<RecipesFor id="bettacraft:filter_pump_head" />
<RecipesFor id="bettacraft:filter_inlet" />
<RecipesFor id="bettacraft:filter_outlet" />
