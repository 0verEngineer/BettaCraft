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
  durchgehender Arbeitsplatte, und auch nach hinten: eine ganze Fläche unter einem großen Becken wird ein Unterschrank
  (siehe unten).
* **Filterplatz:** Lege einen [Außenfilter](filters.md) hinein. Ein Schrank direkt unter dem Aquarium zählt dann als
  angeschlossener Außenfilter; der Schlauch läuft an der Wand seiner **Auslass-Seite** hoch (siehe unten; an der
  Rückwand, wenn du keine andere wählst). Die zwei Heizstab-Plätze im Schrank-Menü sind die Heizstab-Plätze dieses Filters.
* **Luftpumpen-Platz:** Eine [Luftpumpe](air_pump.md) im Schrank treibt die Ausströmer und Schwammfilter des Aquariums
  darüber an, wie eine daneben; ihr Luftschlauch läuft neben dem Filterschlauch an der Auslass-Seite hoch.
* **Neun Lagerplätze** für Futter und anderes. Trichter können sie benutzen, und der [Futterautomat](feeder.md) auf dem
  Aquarium nimmt Futter aus den Schränken darunter.
* Abgebaut droppt der Schrank seinen Inhalt wie eine Truhe und behält sein Holz, seine Auslass-Seite und seine Front.

## Unter dem ganzen Becken

![Ein 10 × 5 Becken auf einer ganzen Fläche aus Schränken von vorne und von hinten, und ein Ring aus Schränken um Stein](../images/cabinet_area.png)

Schränke verbinden sich auch **nach hinten**, so steht selbst ein 10 × 5 Becken auf einem durchgehenden Unterschrank:

* Die **vordere Reihe** zeigt die Fronten (Schubladen an den Enden, Türen dazwischen), die Seiten der äußeren Schränke
  die glatten Seitenwände. Jeder Schrank, vor dem ein anderer steht (die hinteren Reihen und das Innere der Fläche), ist
  ein glatter Korpus: keine Türen, Deckplatte und Seiten gehen durch.
* Du kannst die ganze Fläche aus Schränken bauen (bei 4 × 4 auch die inneren vier) oder nur einen **Ring** aus Schränken
  mit **normalen Blöcken in der Mitte** (billiger): von außen sieht es trotzdem aus wie aus einem Stück.
* Ein Schrank, den du an die Seite, Rückseite oder Front eines anderen setzt, übernimmt **dessen Ausrichtung**, so kannst
  du die Fläche von jeder Seite aus bauen. Auf den Boden gesetzt schaut er wie bisher zu dir.
* Die Einstellung `features.equipment.cabinetArea` schaltet das ab: Schränke verbinden sich dann nur links und rechts,
  und jeder zeigt seine Front.

**Ein größerer Energiespeicher.** Mit Stromkosten (siehe [Strom](power.md)) fügt jeder Schrankblock unter einem Becken
dem einen Energiespeicher des Beckens 20.000 FE hinzu (`power.storePerCabinet`), ebenso jeder daran angebaute Schrank,
der unter keinem anderen Becken steht (ein angebauter Schrank zwischen zwei Becken gehört zum näheren). Die
[Kurbel](crank.md) an jedem dieser Schränke lädt diesen Speicher. Federn, die Schränke älterer Welten aufgezogen hatten,
gehen in den Speicher. Die Einstellung `features.power.cabinetSharedSpring` lässt nur die Schränke direkt unter dem
Becken zählen.

## Fronten

![Schrankreihen von einem bis zwölf Blöcken mit ihren Fronten](../images/cabinet_fronts.png)

Eine Reihe nebeneinander stehender Schränke (in dieselbe Richtung) zeigt **eine Front** statt zwei schmaler Türen auf
jedem Block:

* **1 Block:** die zwei Türen wie immer. **2 Blöcke:** eine Doppeltür.
* **Ab 3 Blöcken:** ein **Schubladenblock an beiden Enden** und dazwischen **Doppeltüren** über je zwei Blöcke. Eine
  ungerade Reihe bekommt in der Mitte eine **Einzeltür** mit seitlichem Griff (bei 5, 9, 13 … Blöcken mit einem
  Schubladenblock auf jeder Seite, damit die Reihe symmetrisch bleibt).
* Die Fronten folgen der Reihe von selbst, wenn du einen Schrank setzt, abbaust oder drehst, bei Reihen bis 32 Blöcke
  (längere Reihen bekommen durchgehend Doppeltüren). Zwei Reihen, die sich an einer Ecke treffen, behalten ihre eigenen
  Fronten.

![Das Schrank-Menü mit den Knöpfen Front und Fugen](../images/cabinet_front_menu.png)

Das Schrank-Menü ist seit 1.55.0 größer (der Text des Front-Knopfs passt hinein). Unter der Heizer-Reihe zeigt es die **Energie** im Speicher
des Beckens, das der Filter des Schranks versorgt (mit einem Balken): der Filter speichert selbst keine Energie, was in ihn hineinfließt, geht in
diesen Speicher, und alle Geräte des Beckens ziehen von dort (siehe [Strom](power.md)).

Der Knopf **„Schrankfront: Automatisch“** oben rechts im Schrank-Menü legt die Front dieses einen Schranks fest:
**Auto** (aus der Reihe, Standard), **Doppeltür, linker Flügel**, **Doppeltür, rechter Flügel**, **Einzeltür**,
**Schubladen** oder **zwei Türen (alt)**. Klick: nächste, Schleichen + Klick: vorige. Der kleine Knopf daneben schaltet
die dünnen **dunklen Fugen** zwischen Türen und Schubladen (standardmäßig an; aus: nur Bretter). Abgebaut und neu gesetzt
behält der Schrank beides (sein Item zeigt es an). Schränke aus älteren Welten werden zu Auto mit Fugen. Die Einstellung
`features.equipment.cabinetFronts` schaltet die Fronten ab: Dann zeigt jeder Schrank wie bisher seine zwei Türen, und die
Knöpfe sind ausgeblendet (die Einstellungen bleiben erhalten).

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
  der Rückwand. Die Einstellung `features.equipment.cabinetOutletSide` schaltet den Knopf ab (dann nutzt jeder Schrank die Rückwand,
  und die Strömung ist wieder gleichmäßig).

## Create-Welle

Mit **Create** nimmt ein Schrank eine drehende Welle, die das Aquarium darüber mit Strom versorgt, wie ein
[Antriebsadapter](drive_adapter.md): der Knopf mit dem Wellen-Symbol links vom Filterplatz wählt die Seite (**keine**,
hinten, links, rechts, vorne oder unten; Klick: nächste, Schleichen + Klick: vorige), ein Messingflansch markiert sie.
Eine Welle, ein Zahnrad oder Getriebe dort lädt den Energiespeicher des Beckens und kostet Stress im Netz von Create. Der Schrank behält die Seite auf seinem Item. Server können das
abschalten (`features.compat.create.cabinetDrive`) oder nur einige Seiten erlauben (`createDrive.cabinetSides`).

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
  welche ausschließen. Die Einstellung `features.equipment.stoneCabinets` schaltet das Steinrezept ab.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:cabinet" />
