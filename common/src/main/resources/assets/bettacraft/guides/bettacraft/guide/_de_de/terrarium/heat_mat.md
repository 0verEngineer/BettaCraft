---
navigation:
  title: Heizmatte
  icon: bettacraft:heat_mat
  parent: terrarium.md
  position: 10
item_ids:
  - bettacraft:heat_mat
---

# Heizmatte

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:heat_mat" p:facing="north" p:heating="true" />
</GameScene>

Die <ItemLink id="bettacraft:heat_mat" /> ist eine dünne Heizfolie, die **außen am Glas** klebt: Klick die **Unterseite**
eines Beckenblocks an (der Platz unter dem Becken muss frei sein) oder eine seiner **Wände**. Unter dem Boden wärmt sie den
Boden ganz, an einer Wand halb so stark.

* Sie wärmt den **Boden** darüber und einen Teil der **Luft** (40 %). Matten unter **dem halben Boden** oder mehr geben die
  volle Erwärmung von 8 K (`terrarium.heatMatLift`); weniger Matten weniger.
* Ihr Thermostat ist die **Zieltemperatur** des Beckens (Übersichts-Reiter): Sie heizt, solange die Luft kälter ist, und
  ruht, wenn die Luft sie erreicht hat. Beim Heizen glüht sie orange.
* Sie wirkt nur an einem [Terrarium oder Paludarium](../terrarium.md). Ein Deckel hält mehr von der Wärme drin.
* **Energie:** Mit Stromkosten (`power.costs` `EASY` oder `HARD`) braucht sie FE des Beckens wie ein Heizer, 4 FE/t beim Heizen
  (`power.heatMatFePerTick`).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:heat_mat" />
