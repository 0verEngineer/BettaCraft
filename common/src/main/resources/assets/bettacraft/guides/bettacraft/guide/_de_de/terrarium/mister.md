---
navigation:
  title: Sprühanlage
  icon: bettacraft:mister
  parent: terrarium.md
  position: 30
item_ids:
  - bettacraft:mister
---

# Sprühanlage

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:mister" p:facing="north" />
</GameScene>

Die <ItemLink id="bettacraft:mister" /> (ein Nebler) sprüht feinen Nebel in ein [Terrarium oder Paludarium](../terrarium.md).
Stell sie **auf einen oberen Block** oder **neben das Becken**.

* Sie ist ein **Hygrostat**: Sie sprüht, solange die Luftfeuchte unter ihrer Einstellung liegt. **Rechtsklick** erhöht die
  Einstellung um 5 %, **Schleichen + Rechtsklick** senkt sie (40 bis 95 %, anfangs 75 %); die Meldung nennt auch ihr Wasser
  und die Luftfeuchte jetzt.
* Eine sprühende Sprühanlage bringt in einem geschlossenen Becken bis zu 40 % Luftfeuchte (`terrarium.misterHumidity`);
  eine offene Oberseite lässt das meiste hinaus. Zwei oder drei Sprühanlagen sprühen etwas mehr.
* Sie fasst **4 Eimer Wasser** und verbraucht beim Sprühen 2 mB pro Sekunde (`terrarium.misterMbPerSecond`). Füll sie mit
  einem Eimer, über [Rohre](../equipment/pipes.md) oder Rohre anderer Mods, oder stell sie neben ein Reservoir oder einen
  Wasserquellblock: Von dort füllt sie sich selbst nach. Ein Komparator liest, wie voll sie ist.
* **Energie:** Mit Stromkosten (`power.costs` `EASY` oder `HARD`) braucht sie beim Sprühen FE des Beckens (6 FE/t).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:mister" />
