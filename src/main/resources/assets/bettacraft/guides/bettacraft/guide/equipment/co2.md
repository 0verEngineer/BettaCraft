---
navigation:
  title: CO2 dosing
  icon: bettacraft:co2_cylinder
  parent: equipment.md
  position: 102
item_ids:
  - bettacraft:co2_cylinder
  - bettacraft:co2_solenoid
  - bettacraft:co2_station
  - bettacraft:bio_co2
  - bettacraft:yeast
---

# CO2 dosing

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:east="true" p:fill="14" />
  <Block id="bettacraft:tank" x="1" p:west="true" p:fill="14" />
  <Block id="bettacraft:co2_cylinder" x="2" p:facing="west" p:solenoid="true" />
  <Block id="bettacraft:bio_co2" x="-1" p:facing="east" p:stage="2" />
</GameScene>

Plants need CO2. Water in balance with the air holds only about **3 mg/L**; with CO2 dosing the plants grow much
faster, the strong-light plants under an [LED panel or high-power LED](light.md) most (up to 2.5 times; medium-light
plants twice, low-light plants 1.5 times). Well dosed CO2 also holds back the algae that strong light brings.

A CO2 source stands **next to an aquarium or on it** (like the [air pump](air_pump.md)) and bubbles into its water.

## What CO2 does

* **The plants** grow faster from about 5 mg/L on, fully from 25 mg/L (15–30 mg/L is good).
* **The pH drops**: one pH unit for every tenfold CO2 (30 mg/L: about -1). See [water chemistry](../aquarium/water_chemistry.md).
* **Too much is dangerous**: from **35 mg/L** the animals are stressed, from **50 mg/L** the water is bad for them
  (sick, in the end dead). Bettas and gouramis breathe air and bear a little more, shrimp and crayfish less
  (`co2Dosing.co2StressFrom`, `co2LethalFrom`).
* **The surface and the aeration drive it out**: an open surface, [air stones](air_stone.md), sponge filters and
  filters lower the CO2. Strong aeration and CO2 dosing work against each other.
* At **night** the plants take none up: the CO2 rises. Use a solenoid valve.
* The animals breathe out a little, the plants take it up in light.

The tab **Water** of the tank menu shows the CO2 when you hover over the chemistry line: the sources, the bubbles
per second and where the CO2 is heading.

## Bio CO2

<ItemLink id="bettacraft:bio_co2" /> is the cheap way: a bottle with fermenting sugar.

1. Right-click it with a **water bucket**,
2. then with up to **four sugar**,
3. then with <ItemLink id="bettacraft:yeast" /> (brown mushrooms and sugar set up in a bowl).

It ferments for about **four days** (less sugar: shorter, `co2Dosing.bioCo2Days`) and gives about **one bubble per
second**, weak and uneven: it starts slowly, varies and fades at the end. It runs day and night and needs no power.
When it is spent, set it up again. Sneak + empty hand pours it out.

## CO2 cylinder

<ItemLink id="bettacraft:co2_cylinder" /> is strong and adjustable. Its **pressure regulator** sets **0 to 10 bubbles
per second** (right-click with an empty hand, or sneak + mouse wheel); the **bubble counter** on it shows them.
A bigger aquarium needs more bubbles (`co2Dosing.co2PerBubble`: one bubble per second gives a block of water 150 mg/L
a day). A full cylinder lasts **60 days at one bubble per second** (`cylinderBubbleDays`). Broken, the cylinder keeps
its gas.

The <ItemLink id="bettacraft:co2_solenoid" /> (right-click the cylinder with it) **closes the cylinder while the lights
are off**: by the aquarium's daylight timer (see [light](light.md)), or without one at night. Without it the CO2 keeps
flowing at night, when the plants take none up – a common way to lose fish.

## CO2 filling station

<ItemLink id="bettacraft:co2_station" /> fills empty cylinders. It sits **on, beside or above a burning furnace,
smoker, blast furnace or campfire** (`#bettacraft:flue_gas_sources`) and collects the CO2 of the flue gas while the
fire burns: about a day of burning for a full cylinder, more fires fill faster (`co2Dosing.stationTicksPerCylinder`).

* Right-click with a cylinder puts it in, an empty hand takes it out, sneaking shows what it does.
* Hoppers put empty cylinders in and take only full ones out; a comparator reads how full it is.
* With a [power option](power.md) on, its **compressor needs FE** (`stationFe`, 20 FE/t while it fills).
* CO2 gas of other mods (fluid tag `#bettacraft:co2_gas`, with `#c:carbon_dioxide`) goes in through pipes:
  1000 mB fill a cylinder (`foreignCo2Mb`).

`features.co2.enabled` (common config) switches CO2 dosing off: the devices give no CO2 and the water stays at the air's.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:co2_cylinder" />
<RecipesFor id="bettacraft:co2_solenoid" />
<RecipesFor id="bettacraft:co2_station" />
<RecipesFor id="bettacraft:bio_co2" />
<RecipesFor id="bettacraft:yeast" />
