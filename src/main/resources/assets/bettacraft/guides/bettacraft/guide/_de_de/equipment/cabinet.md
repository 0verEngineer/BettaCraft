---
navigation:
  title: Schrank
  icon: bettacraft:cabinet
  parent: equipment.md
  position: 80
item_ids:
  - bettacraft:cabinet
---

# Aquariumschrank

![Schränke aus verschiedenen Hölzern mit Aquarien](../images/cabinets.png)

Der <ItemLink id="bettacraft:cabinet" /> ist ein Unterbau für das Aquarium aus **jedem Holz**: fünf Bretter einer Sorte
(auch aus anderen Mods), drei Holzstufen und eine Holztruhe. Der Schrank bekommt das Aussehen seiner Bretter.

* Nebeneinander stehende Schränke, die in dieselbe Richtung zeigen, verbinden sich zu einem langen Schrank mit
  durchgehender Arbeitsplatte.
* **Filterplatz:** Lege einen [Außenfilter](filters.md) hinein. Ein Schrank direkt unter dem Aquarium zählt dann als
  angeschlossener Außenfilter; der Schlauch läuft an der Wand seiner **Auslass-Seite** hoch (siehe unten; an der
  Rückwand, wenn du keine andere wählst). Die zwei Heizstab-Plätze im Schrank-Menü sind die Heizstab-Plätze dieses Filters.
* **Luftpumpen-Platz:** Eine [Luftpumpe](air_pump.md) im Schrank treibt die Ausströmer und Schwammfilter des Aquariums
  darüber an, wie eine daneben; ihr Luftschlauch läuft neben dem Filterschlauch an der Auslass-Seite hoch.
* **Neun Lagerplätze** für Futter und anderes. Trichter können sie benutzen, und der [Futterautomat](feeder.md) auf dem
  Aquarium nimmt Futter aus den Schränken darunter.
* Abgebaut droppt der Schrank seinen Inhalt wie eine Truhe und behält sein Holz und seine Auslass-Seite.

## Auslass-Seite

![Die vier Auslass-Seiten: hinten, links, rechts und vorne](../images/cabinet_outlets.png)

Der Knopf **„Auslass: hinten“** im Schrank-Menü wählt die Beckenwand, an der der Filterschlauch hochgeht, von der
Schranktür aus gesehen: **hinten** (Standard), **links**, **rechts** oder **vorne** (unüblich, geht aber). Jeder Schrank
hat seine eigene Seite, auch mehrere Schränke unter einem großen Becken. Klick: nächste Seite, Schleichen + Klick: vorige.

* Schlauch, Auslass, Wasserstrahl und Blasen wandern an diese Wand.
* Die **Strömung** kommt von dort: Schwimmpflanzen treiben vom Auslass weg, und vor ihm strömt das Wasser stärker als im
  übrigen Becken (bis fast doppelt so stark wie die Strömung des Beckens, über acht Blöcke schwächer werdend).
  [Seepferdchen](../saltwater/seahorses.md) dort leiden früher unter der Strömung, also den Auslass auf die andere Seite
  ihrer Haltepunkte legen; Korallen vor ihm bekommen mehr Strömung (gut für Acropora, zu viel für Scheibenanemonen).
* Abgebaut und neu gesetzt behält der Schrank die Seite (sein Item zeigt sie an). Schränke aus älteren Welten bleiben an
  der Rückwand. Die Einstellung `features.cabinetOutletSide` schaltet den Knopf ab (dann nutzt jeder Schrank die Rückwand,
  und die Strömung ist wieder gleichmäßig).

## Steinschränke

![Schränke aus verschiedenen Steinen mit Aquarien](../images/cabinets_stone.png)

Ein Schrank lässt sich auch aus **Stein** bauen: fünf Blöcke einer Steinart, drei **Stufen derselben Steinart** und eine
Holztruhe (Steinziegel mit Steinziegelstufen, Sandstein mit Sandsteinstufen …). Jede Steinart mit eigenen Stufen geht:
Stein, Bruchstein, Granit, Diorit, Andesit, Tiefenschiefer, Tuffstein, Schwarzstein, Sandstein, Ziegel, Netherziegel,
Quarz, Terrakotta mit Stufen und die Steine anderer Mods. Metall- und Kupferblöcke, Glas, Purpur, Endstein, Prismarin,
Beton und Schlammziegel gehen nicht.

<Recipe id="bettacraft:stone_cabinet" />

* Der Steinschrank sieht aus wie sein Stein und kann dasselbe wie ein Holzschrank (Filter, Heizstäbe, Lager,
  verbundene Arbeitsplatte).
* Er wird abgebaut wie sein Stein: mit der **Spitzhacke** (mit der Hand droppt nur sein Inhalt), so hart und so
  explosionsfest wie der Stein.
* Modpacks können mit dem Item-Tag `#bettacraft:cabinet_stones` Steine hinzufügen und mit `#bettacraft:cabinet_excluded`
  welche ausschließen. Die Einstellung `features.stoneCabinets` schaltet das Steinrezept ab.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:cabinet" />
