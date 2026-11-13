---
navigation:
  title: Schrank
  icon: bettacraft:cabinet
  parent: equipment.md
  position: 50
item_ids:
  - bettacraft:cabinet
---

# Aquariumschrank

![Schränke aus verschiedenen Hölzern mit Aquarien](../images/cabinets.png)

Der <ItemLink id="bettacraft:cabinet" /> ist ein Unterbau für das Aquarium aus **jedem Holz**: fünf Bretter einer Sorte
(auch aus anderen Mods), drei Holzstufen und eine Holztruhe. Der Schrank bekommt das Aussehen seiner Bretter.

<RecipeFor id="bettacraft:cabinet" />

* Nebeneinander stehende Schränke, die in dieselbe Richtung zeigen, verbinden sich zu einem langen Schrank mit
  durchgehender Arbeitsplatte.
* **Filterplatz:** Lege einen [Außenfilter](filters.md) hinein. Ein Schrank direkt unter dem Aquarium zählt dann als
  angeschlossener Außenfilter; der Schlauch läuft an der Rückwand hoch. Die zwei Heizstab-Plätze im Schrank-Menü sind
  die Heizstab-Plätze dieses Filters.
* **Neun Lagerplätze** für Futter und anderes. Trichter können sie benutzen, und der [Futterautomat](feeder.md) auf dem
  Aquarium nimmt Futter aus den Schränken darunter.
* Abgebaut droppt der Schrank seinen Inhalt wie eine Truhe und behält sein Holz.

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
