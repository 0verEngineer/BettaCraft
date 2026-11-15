---
navigation:
  title: Terrarium and paludarium
  icon: bettacraft:basking_lamp
  position: 77
---

# Terrarium and paludarium

Every tank can also be a **terrarium** or a **paludarium**. You do not switch it: the tank works out what it is from its
water and from the **land** you build in its [interior](interior.md).

* **Aquarium**: water and little or no land. Everything as before.
* **Paludarium**: water and a land part. Build soil, sand, stones or wood in the interior so that it rises **above the
  water line**, then fill the tank only partly. From **15 %** land (the share of the floor whose top is above the water;
  `terrarium.paludariumLandShare`) the tank is a paludarium.
* **Terrarium**: no water at all, or so much land (from 90 %, `terrarium.terrariumLandShare`) that the water is only a
  dish.

The tank log notes when the kind changes. A terrarium or paludarium gets a new tab **Climate** in the tank menu; aquaria
look as they always did.

## The climate

Terraria and paludaria have three new values. Each moves slowly towards what the room, the tank and its devices give
(the air in about an hour of game time):

* **Air temperature**: the room (the biome, day and night) plus the [heat mats](terrarium/heat_mat.md) and the
  [basking lamps](terrarium/basking_lamp.md). In a paludarium the water pulls the air towards its own temperature (so
  the water heaters warm the air too). The heat mats are the only devices with a thermostat: they heat while the air is
  below the tank's **target temperature** (the setting on the overview tab).
* **Humidity** (%): the room (dry in deserts, humid in jungles and swamps, more while it rains on an open tank) plus the
  water surface (more when the water is warm), the **plants on the land** (grass, ferns, flowers, moss, leaves, aquarium
  plants) and the [misters](terrarium/mister.md). Warm air holds more water: heating lowers the humidity.
* **UV** at the basking spot: a lit [basking lamp](terrarium/basking_lamp.md) gives UV 5; sunlight through an **open
  top** gives up to 3 by day (glass and lids block UV).

A **lid** (or anything that covers a top block: a light, the feeder, a mister) keeps warmth and moisture in; an open top
lets them out. The climate tab shows how many top blocks are covered.

Two more values follow from the air: the **ground** over the heat mats is warmer than the air, the **basking spot** under
a lit lamp much warmer (12 K, `terrarium.baskingSpotLift`).

## Where you see the values

* The **Climate** tab of the tank menu: the kind of tank, its land and water share, all values with bars, the devices
  and what the animals lack.
* The overview tab shows the air instead of only the water.
* The [thermo-hygrometer](terrarium/thermo_hygrometer.md) on the glass, the info overlays (Jade, The One Probe, WTHIT)
  and the [monitors](equipment/monitor.md) (values air temperature, humidity, UV).

## The devices

| Device | Where | Does |
|---|---|---|
| [Heat mat](terrarium/heat_mat.md) | under the tank or on a wall | warms the ground and a bit of the air, with the target temperature as thermostat |
| [Basking lamp](terrarium/basking_lamp.md) | on a top block (or hanging above) | a warm basking spot with UV on the land under it, light like a lamp |
| [Mister](terrarium/mister.md) | on top of or beside the tank | sprays while the humidity is below its setting; uses water |
| [Thermo-hygrometer](terrarium/thermo_hygrometer.md) | on the glass | shows air temperature and humidity |

## Terrarium animals

A terrarium animal lives in the kind of tank it needs (a turtle of the swamps in a paludarium, a tortoise in a dry
terrarium) and goes in **without the minimum water level** of the fish. It walks on the land of the interior, climbs out
of the water onto it, swims or walks on the bottom and goes to the **basking spot** to warm up (more often when the air is
cold). The menu and the log say what it lacks: wrong kind of tank, too cold or too hot (with a basking spot it counts the
warmest place), too dry or too humid, not enough UV, no basking spot, too little land or water, water too cold or too
warm. Being in the wrong tank, too cold or too hot costs health after a day (`terrarium.harmAfterDays`); the rest only
stresses it. It eats when the tank is fed (by hand or by the [feeder](equipment/feeder.md)).

The turtles:

| Turtle | Tank | Needs |
|---|---|---|
| [Musk turtle](terrarium/musk_turtle.md) | paludarium, little land | water 20-28 °C |
| [Pond slider](terrarium/slider_turtle.md) (red-eared, yellow-bellied) | paludarium, half land | basking lamp, UV; **grows large** (12 blocks) |
| [Hermann's tortoise](terrarium/hermann_tortoise.md) | dry terrarium | basking lamp, strong UV, greens; brumates in the cold |
| [Reeves' turtle](terrarium/reeves_turtle.md) | paludarium | basking spot, a little cooler |
| Sea turtle hatchlings (vanilla) | paludarium | only hatchlings; grown ones are too big |

They lay eggs into sand or soil on the land, and the nest temperature decides the sex of the hatchlings:
[turtle breeding](terrarium/turtle_breeding.md). Wild ones live in swamps and rivers (the water turtles) and in savannas
and dry shrublands (the tortoise).

## Water care in a terrarium

A terrarium without water has no water values: water quality, filters, oxygen, CO2 and the water chemistry stand still
and the menu says so. A paludarium keeps all of them for its water.

## Settings

Server config `terrarium.*`: the land shares of the kinds, the strength of the devices, the time constants, the water of
the mister, the days to harm; `power.*`: heat mats use the heaters' energy switch (`heatMatFePerTick`), misters their own
(`terrarium.misterRequiresEnergy`). Common config: `features.terrarium` (off: every tank is an aquarium),
`features.terrariumDevices`, `features.turtles`, `features.turtleBreeding`, `features.wildTurtles`. Turtles:
`terrarium.incubationSpeed`, `terrarium.clutchIntervalDays`, `terrarium.brumation`, `terrarium.brumationBelow`.
