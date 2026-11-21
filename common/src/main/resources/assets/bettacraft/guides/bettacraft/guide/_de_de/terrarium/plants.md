---
navigation:
  title: Terrarienpflanzen
  icon: bettacraft:neoregelia
  parent: terrarium.md
  position: 45
feature: terrariumPlants.enabled
---

# Terrarienpflanzen

![Ein Regenwald-Terrarium](../images/terra_plants.png)

Terrarienpflanzen wachsen auf dem **Land** eines [Terrariums oder Paludariums](../terrarium.md): Pflanze einen Steckling im
[Innenraum](../interior.md) auf Erde, Sand oder Kies (die Luftpflanze und die Orchidee auch auf Stein oder Holz, die Moose auf jeden
festen Block), über der Wasserlinie. Wie die [Aquarienpflanzen](../plants.md) wachsen sie vom Steckling zu einer Pflanze aus vielen
Blöcken, auch wenn niemand drin ist.

* [Tropisch-feucht](plants_tropical.md): Bromelien, Farne, Moose, Orchideen, Begonien.
* [Sumpf und Ufer](plants_swamp.md): fleischfressende Pflanzen, Seggen, Binsen, Papyrus.
* [Trocken und heiß](plants_dry.md): Kakteen und Sukkulenten, die Gräser der Savanne.

## Klima

Jede Art braucht ihr **Licht**, ihre **Luftfeuchte** und ihre **Lufttemperatur** (ihr Tooltip und ihre Seite sagen welche; das
[Thermo-Hygrometer](thermo_hygrometer.md) und das Becken-Menü zeigen, was das Becken hat). Das Licht kommt von der Beckenlampe,
vom Himmel durch einen offenen Deckel und von der [Wärmelampe](basking_lamp.md).

* In gutem Klima wächst sie, breitet sich aus und hält die Luft **feucht** (das Land, das sie bedeckt, erhöht die Luftfeuchte
  des Beckens; Kakteen und Sukkulenten kaum, sie halten ihr Wasser).
* In schlechtem **kümmert** sie: alle paar Tage verliert sie eine Stufe, bis zur ersten. Unter Wasser kümmert eine
  Terrarienpflanze auch. Der Pflanzen-Tab des Becken-Menüs zählt die kümmernden.
* **Aquarienpflanzen** über dem Wasser eines feuchten Paludariums (ab etwa 55 % Luftfeuchte, voll ab 75 %) wachsen in ihrer
  **emersen Form**, mit halber Geschwindigkeit.

## Vermehrung

* **Stecklinge**: eine Schere an einer gewachsenen Pflanze (drinnen oder von außen am Becken, wohin du schaust).
* **Ableger**: die Kakteen, Sukkulenten, Bromelien und die Luftpflanze: die Schere nimmt eine kleine bewurzelte Pflanze ab;
  ausgewachsen setzen sie rundherum Ableger an.
* **Teilung**: die Moose und der Moosfarn: die Schere teilt das Polster (die Hälfte als Pflanze); ein abgebautes Stück Moos teilt
  es auch.
* Die Wurzel abbauen nimmt die ganze Pflanze heraus (ein Item, das seine Stufe behält).

## In der Welt

* Terrarienpflanzen wachsen **wild**: in Dschungeln, den üppigen Höhlen, Sümpfen und Mangroven, Wüsten und Badlands, Savannen
  (Biom-Tags `#bettacraft:*_plant_lands`, also zählen auch die Biome von Biom-Mods; Einstellung
  `features.terrariumPlants.wildPlants`). Die Schere schneidet einen Steckling ab (einen Ableger, ein Stück Moos), und die Pflanze
  wächst nach; Behutsamkeit nimmt die ganze Pflanze.
* Jede Terrarienpflanze lässt sich als Deko **in der Welt pflanzen** (außerhalb eines Beckens auf Erde oder Sand benutzen). Dort
  wächst sie nicht; abgebaut gibt sie ihre Pflanze zurück.
* Einige entstehen aus Vanilla-Pflanzen (Herstellung: Kaktus, Farn, großer Farn, Moosblock, toter Busch, kurzes Gras), andere
  gibt es beim [fahrenden Händler](../getting_started/sources.md) und beim [Aquarianer](../getting_started/aquarist.md).

## Die Tiere

* **Landschildkröten** fressen die essbaren Pflanzen (Feigenkaktus, Mauerpfeffer, die Gräser), wenn sie Hunger haben: eine
  Pflanze verliert eine Stufe und die Schildkröte ist satt (`features.terrariumPlants.tortoiseGrazing`).
* **Pfeilgiftfrösche** tragen ihre Kaulquappen in die Kelche halb gewachsener Bromelien, eine je Bromelie: dort brauchen sie weder
  offenes Wasser noch Futter und kommen als Jungfrösche heraus (`features.terrariumPlants.bromeliadTadpoles`).
* **Schlangen und Geckos** verstecken sich zwischen den großen Pflanzen (`features.terrariumPlants.hiding`).

## Alle Terrarienpflanzen

| | Pflanze | Licht | Luftfeuchte | °C | Schere |
|---|---|---|---|---|---|
| <ItemImage id="bettacraft:neoregelia" /> | [Neoregelie (Errötende Bromelie)](plants/neoregelia.md) | mittel | 65–100 % | 20–30 | Ableger |
| <ItemImage id="bettacraft:guzmania" /> | [Guzmanie](plants/guzmania.md) | mittel | 65–100 % | 20–30 | Ableger |
| <ItemImage id="bettacraft:vriesea" /> | [Vriesea (Flammendes Schwert)](plants/vriesea.md) | mittel | 65–100 % | 20–30 | Ableger |
| <ItemImage id="bettacraft:tillandsia" /> | [Tillandsie (Luftpflanze)](plants/tillandsia.md) | viel | 45–100 % | 20–30 | Ableger |
| <ItemImage id="bettacraft:fittonia" /> | [Fittonie (Silbernetzblatt)](plants/fittonia.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:pothos" /> | [Efeutute](plants/pothos.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:marcgravia" /> | [Marcgravia (Schindelpflanze)](plants/marcgravia.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:boston_fern" /> | [Schwertfarn](plants/boston_fern.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:birds_nest_fern" /> | [Nestfarn](plants/birds_nest_fern.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:cushion_moss" /> | [Kissenmoos](plants/cushion_moss.md) | wenig | 65–100 % | 20–30 | Teilung |
| <ItemImage id="bettacraft:reindeer_lichen" /> | [Rentierflechte](plants/reindeer_lichen.md) | mittel | 40–100 % | 10–26 | Teilung |
| <ItemImage id="bettacraft:selaginella" /> | [Selaginella (Moosfarn)](plants/selaginella.md) | wenig | 65–100 % | 20–30 | Teilung |
| <ItemImage id="bettacraft:mini_orchid" /> | [Mini-Orchidee](plants/mini_orchid.md) | mittel | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:begonia_rex" /> | [Königsbegonie](plants/begonia_rex.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:peperomia" /> | [Zwergpfeffer](plants/peperomia.md) | wenig | 65–100 % | 20–30 | Stecklinge |
| <ItemImage id="bettacraft:sundew" /> | [Sonnentau](plants/sundew.md) | viel | 55–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:sarracenia" /> | [Schlauchpflanze (Sarracenia)](plants/sarracenia.md) | viel | 55–100 % | 10–30 | Stecklinge |
| <ItemImage id="bettacraft:nepenthes" /> | [Kannenpflanze (Nepenthes)](plants/nepenthes.md) | mittel | 55–100 % | 16–28 | Stecklinge |
| <ItemImage id="bettacraft:venus_flytrap" /> | [Venusfliegenfalle](plants/venus_flytrap.md) | viel | 55–100 % | 10–30 | Stecklinge |
| <ItemImage id="bettacraft:butterwort" /> | [Fettkraut](plants/butterwort.md) | mittel | 50–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:swamp_sedge" /> | [Sumpf-Segge](plants/swamp_sedge.md) | mittel | 50–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:soft_rush" /> | [Flatter-Binse](plants/soft_rush.md) | mittel | 50–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:papyrus" /> | [Papyrusstaude](plants/papyrus.md) | viel | 55–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:umbrella_sedge" /> | [Zyperngras](plants/umbrella_sedge.md) | mittel | 55–100 % | 15–30 | Stecklinge |
| <ItemImage id="bettacraft:golden_barrel_cactus" /> | [Goldkugelkaktus](plants/golden_barrel_cactus.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:column_cactus" /> | [Säulenkaktus](plants/column_cactus.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:prickly_pear" /> | [Feigenkaktus](plants/prickly_pear.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:mammillaria" /> | [Warzenkaktus](plants/mammillaria.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:echeveria" /> | [Echeverie](plants/echeveria.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:haworthia" /> | [Zebra-Haworthie](plants/haworthia.md) | mittel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:aloe_vera" /> | [Echte Aloe](plants/aloe_vera.md) | viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:agave" /> | [Amerikanische Agave](plants/agave.md) | sehr viel | 10–55 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:lithops" /> | [Lebende Steine (Lithops)](plants/lithops.md) | viel | 10–45 % | 20–38 | Ableger |
| <ItemImage id="bettacraft:sedum" /> | [Mauerpfeffer](plants/sedum.md) | viel | 10–55 % | 20–38 | Stecklinge |
| <ItemImage id="bettacraft:blue_grama" /> | [Moskitogras](plants/blue_grama.md) | viel | 10–70 % | 20–38 | Stecklinge |
| <ItemImage id="bettacraft:needlegrass" /> | [Federgras](plants/needlegrass.md) | viel | 10–70 % | 20–38 | Stecklinge |
