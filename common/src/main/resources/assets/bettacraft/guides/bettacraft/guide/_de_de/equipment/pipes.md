---
navigation:
  title: Rohre und Wasserpumpe
  icon: bettacraft:glass_pipe
  parent: equipment.md
  position: 41
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

Rohre tragen **Wasser mit allen Werten**: Temperatur, Salz, Qualität und Nitrat reisen mit (später auch Osmosewasser,
Meerwasser und Lebendfutter-Suspension). Wasser aus einem Eimer oder einer Wasserquelle ist frisches normales Wasser.

* <ItemLink id="bettacraft:glass_pipe" />: günstig, trägt **50 mB pro Tick**.
* <ItemLink id="bettacraft:stainless_steel_pipe" />: trägt **250 mB pro Tick**. Ein Netz aus beiden Sorten trägt, was
  sein schwächstes Rohr trägt.
* Rohre verbinden sich miteinander, mit jedem Aquariumblock, dem [Vorratsbehälter](top_off.md), dem
  [Multiblock-Filter](multiblock_filter.md), Kesseln und den **Tanks und Rohren anderer Mods** (Mekanism, EnderIO,
  Create, AE2, RS ...: der normale Fluid-Handler).
* Wo ein Rohr (auch ein Filterrohr oder eine Leitung eines Rohrbündels) an ein Aquarium anschließt, führt eine stählerne
  **Glasdurchführung** durch die Scheibe: außen ein Flansch, innen der gleiche Flansch mit einem kurzen Stutzen ins Becken, aus
  dem die Rohre im Becken weitergehen.
* **Rohre pumpen nicht.** Das Wasser bewegt sich, wenn etwas hineindrückt: die <ItemLink id="bettacraft:water_pump" />,
  der Pumpenkopf eines [Multiblock-Filters](multiblock_filter.md) oder die Pumpe einer anderen Mod. Es erreicht dann
  die Blöcke an den Enden der Rohre.

**Wasserpumpe:** zieht Wasser aus dem Block dahinter und drückt es in den Block davor, **200 mB pro Tick**. Beim Setzen
dorthin schauen, wohin das Wasser soll. Rechtsklick oder Schleichen + Mausrad: [Leistungsregler](regulator.md).
Redstone hält sie an. Mit `pipes.pumpsRequireEnergy` braucht sie FE (`pumpFePerTick` 8).

Ein Aquariumblock nimmt Wasser aus einem Rohr an jeder Seite. Von **unten** lässt ein Bodenblock mit
[Bodendurchführung](bulkhead.md) es durch Ausläufe herein, die du im Aquarium setzt.

Rohre tragen auch **Lebendfutter**: eine [Lebendfutter-Pumpe](../food/live_food_pump.md) neben einer Zuchtanlage drückt
Lebendfutter-Suspension durch sie in die Becken.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:glass_pipe" />
<RecipesFor id="bettacraft:stainless_steel_pipe" />
<RecipesFor id="bettacraft:water_pump" />
