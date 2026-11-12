---
navigation:
  title: Bettas
  icon: bettacraft:betta_bucket
  parent: index.md
  position: 20
item_ids:
  - bettacraft:betta_bucket
  - bettacraft:fish_bag
  - bettacraft:betta_spawn_egg
---

# Bettas

Bettas (*Betta splendens*) sind kleine Labyrinthfische aus den Reisfeldern und Sümpfen Südostasiens. In Bettacraft sind
sie echte Tiere mit Genen, Geschlecht, sechs Lebensstufen und Bedürfnissen.

<GameScene zoom="5" interactive={true}>
  <Entity id="bettacraft:betta" x="1.2" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:male,stage:adult,genome:{finform:[halfmoon,halfmoon],fin_length:[short,short],pectoral:[normal,normal],body_color:[white,white],fin_color:[white,white],pattern:[koi,koi],pattern_color:[red,red],iridescence:[high,high],iridescence_color:[royal_blue,royal_blue]}}}" />
  <Entity id="bettacraft:betta" x="2.4" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:male,stage:adult,genome:{finform:[halfmoon,halfmoon],fin_length:[long,long],pectoral:[normal,normal],body_color:[navy,navy],fin_color:[mustard,mustard],pattern:[none,none],pattern_color:[black,black],iridescence:[high,high],iridescence_color:[royal_blue,royal_blue]}}}" />
  <Entity id="bettacraft:betta" x="3.6" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:female,stage:adult,genome:{finform:[crowntail,crowntail],fin_length:[long,long],pectoral:[normal,normal],body_color:[royal_blue,royal_blue],fin_color:[red,red],pattern:[butterfly,butterfly],pattern_color:[white,white],iridescence:[low,low],iridescence_color:[steel,steel]}}}" />
</GameScene>

## Wilde Bettas

Wilde Bettas erscheinen im Wasser von Sumpf-Biomen (Biom-Tag `#bettacraft:betta_spawn_biomes`, der `#c:is_swamp`
enthält). Sie tragen die Gene der Wildform: kurze Flossen, Rundschwanz und gedeckte Farben. In der Natur haben sie keine
Bedürfnisse und kämpfen nicht.

## Fangen

* **Wassereimer:** Rechtsklick auf den Fisch. Du bekommst einen <ItemLink id="bettacraft:betta_bucket" />.
* **Fischtüte:** Rechtsklick mit einer leeren <ItemLink id="bettacraft:fish_bag" /> auf den Fisch. Eine Tüte fasst
  einen Fisch und 100 mB Wasser und funktioniert für jedes kleine Wassertier. Sie ist wiederverwendbar; leere Tüten
  stapeln sich bis 16.

Das Rezept der Fischtüte wechselt auf Plastik, wenn eine Mod mit Plastikplatten installiert ist (mehr Tüten pro
Rezept):

<RecipesFor id="bettacraft:fish_bag" />

Der Tooltip von Eimer und Tüte zeigt Typ, Geschlecht, Farben, Muster und Schimmer des Fisches. Auf ein
[Aquarium](aquarium/inhabitants.md) angewendet setzt du den Fisch ein, auf Wasser lässt du ihn frei.

## Männchen und Weibchen

Männchen haben lange, bunte Flossen. Weibchen haben kürzere Flossen, sind etwas blasser und zeigen in Zuchtstimmung
senkrechte Querstreifen. Das Geschlecht steht als ♂ und ♀ im Aquarium-Menü und in Jade.

## Mehr über Bettas

* [Lebensstufen](bettas/life_stages.md)
* [Gesundheit](bettas/health.md)
* [Genetik](bettas/genetics.md)
* [Typen und Farben](bettas/types.md)
* [Kämpfe, Flaring und Raubfische](bettas/fighting.md)
