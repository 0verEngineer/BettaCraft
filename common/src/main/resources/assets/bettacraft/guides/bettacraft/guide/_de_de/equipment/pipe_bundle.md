---
navigation:
  title: Rohrbündel, Frisch- und Abwasser
  icon: bettacraft:filter_fresh_pipe
  parent: equipment/multiblock_filter.md
  position: 2
item_ids:
  - bettacraft:filter_fresh_pipe
  - bettacraft:filter_waste_pipe
  - bettacraft:filter_fresh_port
  - bettacraft:filter_waste_port
---

# Rohrbündel, Frisch- und Abwasser

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:filter_fresh_pipe" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_waste_pipe" z="1" p:west="true" p:east="true" />
  <Block id="bettacraft:filter_chamber" x="2" p:medium="ceramic" />
  <Block id="bettacraft:filter_fresh_port" x="1" p:facing="west" />
  <Block id="bettacraft:filter_waste_port" x="2" z="1" p:facing="south" />
</GameScene>

![Eine Reihe Rohrbündel mit allen vier Leitungen zwischen einem Aquarium und dem Filter](../images/pipe_bundle.png)

## Rohrbündel

Bis zu **vier Filterleitungen laufen in einem Block**, wie die Conduits anderer Mods: ein Filterrohr mit dem Rohr **einer
anderen Leitung** rechtsklicken, und beide laufen in einem **Rohrbündel** weiter (bis zu vier). Schleichen mit dem Rohr in
der Hand setzt es stattdessen daneben. Jede Leitung im Bündel behält ihre eigenen Verbindungen und ihr eigenes T-Stück (ein
T-Stück auf einem Bündel kommt auf die angeschaute Leitung); die Leitungen **mischen sich nie**. Schleichen + Rechtsklick
mit leerer Hand auf ein Bündel nimmt die angeschaute Leitung wieder heraus: ihr Rohr (und T-Stück) kommen zurück, und bei
einer verbleibenden Leitung ist der Block wieder ein normales Rohr. Wer ein Bündel abbaut, bekommt das Rohr jeder Leitung
zurück.

Die vier Leitungen und ihre Farben: Einlass (blau) und Rücklauf (orange) der [Filterleitungen](filter_lines.md),
Frischwasser (grün) und Abwasser (braun).

## Frisch- und Abwasser

* <ItemLink id="bettacraft:filter_fresh_pipe" /> (grün): von einer Wasserpumpe, einem [Reservoir](pipes.md), einem ME- oder
  RS-Exportbus oder den Rohren anderer Mods zum Filter.
* <ItemLink id="bettacraft:filter_waste_pipe" /> (braun): vom Filter zu einem Reservoir, einem Aquarium, dem Rohr oder
  Speicher einer anderen Mod. Rohre und Pumpen anderer Mods dürfen das Altwasser daraus ziehen.
* <ItemLink id="bettacraft:filter_fresh_port" /> und <ItemLink id="bettacraft:filter_waste_port" />: an eine Kammer des
  Filters gesetzt, die Muffe nach außen. Jeder fasst 8 Eimer. Eine Wasserquelle neben dem Frischwasseranschluss ist
  endloses Leitungswasser.

![Der Filter mit Frischwasseranschluss (grün), Abwasseranschluss (braun), Niveausensor und Wasserwechsel-Steuerung](../images/filter_water.png)

Ein [Niveausensor](top_off.md) oder eine [Wasserwechsel-Steuerung](water_change.md), die **irgendein Teil des Filters
berühren**, wirken dann auf **jedes Aquarium des Filters**: sie füllen verdunstetes Wasser nach und wechseln einen Teil des
Wassers von jedem, bei knappem Wasser aufgeteilt nach dem, was jedes Aquarium braucht. Das Altwasser geht aus dem
Abwasseranschluss bis ans Ende seiner Leitung; **ohne Abwasseranschluss wird es verworfen**.

Nur die Frisch- und die Abwasserleitung bieten ihr Wasser anderen Mods an (Rohre von EnderIO, Pipez, Mekanism, die Busse
von Applied Energistics 2 und Refined Storage docken an); Einlass- und Rücklaufleitung bleiben im Filter.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:filter_fresh_pipe" />
<RecipesFor id="bettacraft:filter_waste_pipe" />
<RecipesFor id="bettacraft:filter_fresh_port" />
<RecipesFor id="bettacraft:filter_waste_port" />
