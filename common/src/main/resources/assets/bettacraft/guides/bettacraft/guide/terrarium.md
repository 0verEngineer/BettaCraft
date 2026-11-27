---
navigation:
  title: Terrarium and paludarium
  icon: bettacraft:basking_lamp
  position: 77
feature: terrariums.enabled
---

# Terrarium and paludarium

Every tank can also be a **terrarium** or a **paludarium**. Normally you do not switch it: the tank works out what it is
from its water and from the **land** you build in its [interior](interior.md). You can also fix the type (see below).

* **Aquarium**: water and little or no land. Everything as before.
* **Paludarium**: water and a land part. Build soil, sand, stones or wood in the interior so that it rises **above the
  water line**, then fill the tank only partly. From **15 %** land (the share of the floor whose top is above the water;
  `terrarium.paludariumLandShare`) the tank is a paludarium.
* **Terrarium**: no water at all, or so much land (from 90 %, `terrarium.terrariumLandShare`) that the water is only a
  dish.

The tank log notes when the kind changes. A terrarium or paludarium gets a new tab **Climate** in the tank menu; aquaria
look as they always did.

## Fixing the tank type

A terrarium that is filled with water by mistake would quietly become a paludarium or an aquarium. To prevent that, the
button **Type** (in the overview tab next to the target temperature, and at the top of the climate tab) sets the type:

* **Automatic** (every tank starts like this, old tanks too): the kind follows the water and the land as described above.
* **Aquarium**: the tank stays an aquarium, whatever land it has. Water as much as fits.
* **Paludarium**: the tank stays a paludarium. Water only comes in up to the height where the land part stays at 15 %
  (`terrarium.paludariumLandShare`); the climate tab shows how much that is. Build more land higher up and more water fits.
* **Terrarium**: the tank stays a terrarium and takes **no water at all** (for a water dish leave the type on automatic).

The limit holds for every way water comes in: buckets, pipes and pumps (also of other mods such as Create: they get the
part that fits), the reservoir, the top-off of the water change controller and the water an animal brings along. A water change only
replaces the water it took out, and rain never fills a tank. Instead of the water you get a message ("Terrarium: no water",
"Paludarium: water only up to 1.8 blocks high").

**Nothing is ever removed.** If you fix a type that does not fit the water in the tank (a full tank set to terrarium), the
water stays, the menu says that it is too much and none comes in; draining with a bucket or a pump works as always.
Clicking cycles through the types (Shift-click goes back); the tank log notes who set which type. Locked tanks, claims
and spawn protection apply. The info overlays show a fixed type as "(fixed)". Switch: `features.terrariums.tankMode`
(off: every tank is automatic again; the chosen type is kept).

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

## Terrarium plants

The land of a terrarium or paludarium has [plants of its own](terrarium/plants.md): bromeliads, ferns, mosses and orchids for
the rainforest, carnivorous plants and sedges for the shore, cacti and succulents for the desert. They grow by the light,
the humidity and the air temperature of the tank, raise the humidity and give the animals food and hiding places.

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
| [Sea turtle](terrarium/sea_turtle.md) (vanilla) | big sea water paludarium | a big animal (12 × 6 × 3 blocks), a sand beach for its eggs |

They lay eggs into sand or soil on the land, and the nest temperature decides the sex of the hatchlings:
[turtle breeding](terrarium/turtle_breeding.md). Wild ones live in swamps and rivers (the water turtles) and in savannas
and dry shrublands (the tortoise).

The [amphibians and reptiles](terrarium/herps.md):

| Animal | Tank | Needs |
|---|---|---|
| [Poison dart frogs](terrarium/dart_frog.md) (three species) | paludarium, mostly land | high humidity, fruit flies; tadpoles in the water |
| [Cranwell's horned frog](terrarium/horned_frog.md) | moist terrarium | sits buried in the ground; crickets |
| [Leopard gecko](terrarium/leopard_gecko.md) | dry terrarium | basking spot, crickets; sex by the nest temperature |
| [Crested gecko](terrarium/crested_gecko.md) | terrarium | climbs the glass; not too warm; fruit and insects |
| [Corn snake](terrarium/corn_snake.md) | dry terrarium, 8 blocks | basking spot, a prey animal a week; brumates |

They eat [feeder insects](terrarium/feeder_insects.md) (crickets, fruit flies), fruit or prey.

[Land animals of other mods](terrarium/foreign_land_animals.md) with a terrarium profile: the tortoise and the alligator of
Naturalist (the alligator in a big paludarium), caught with the terrarium box or the large net.

[Minecraft animals as pets](terrarium/minecraft_pets.md): chickens, rabbits, bees, frogs, cats, foxes, parrots and more at their
real size, the big ones (cows, horses, pandas ...) in big terraria; with needs of their own (grass, flowers, a beehive), no drops.

[Mobs in glass](terrarium/mobs_in_glass.md): drowned, zombies, spiders, small slimes and a few more live shrunk and harmless
in tanks (mob jar, bait, the capture items of other mods). They drop nothing.

## Water care in a terrarium

A terrarium without water has no water values: water quality, filters, oxygen, CO2 and the water chemistry stand still
and the menu says so. A paludarium keeps all of them for its water.

## Settings

Server config `terrarium.*`: the land shares of the kinds, the strength of the devices, the time constants, the water of
the mister, the days to harm; `power.*`: heat mats use the heaters' energy switch (`heatMatFePerTick`), misters their own
(`power.costs`). Common config: `features.terrariums.enabled` (off: every tank is an aquarium),
`features.terrariums.devices`, `features.terrariums.tankMode`, `features.turtles.enabled`, `features.turtles.breeding`, `features.turtles.wildSpawns`, `features.turtles.seaTurtles`, `features.foreignAnimals.terrarium`, `features.terrariums.herps`,
`features.terrariums.herpBreeding`, `features.terrariums.wildHerps`, `features.mobsInGlass.enabled`. Turtles (and the egg layers of the amphibians and reptiles):
`terrarium.incubationSpeed`, `terrarium.clutchIntervalDays`, `terrarium.brumation`, `terrarium.brumationBelow`. Land animals of other mods without a profile for the terrarium box: `terrarium.boxAllow`.
