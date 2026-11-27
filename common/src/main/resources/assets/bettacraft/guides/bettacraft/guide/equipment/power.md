---
navigation:
  title: Power
  icon: minecraft:redstone
  parent: equipment.md
  position: 90
feature: power.enabled
---

# Power

By default **no equipment needs power**. A server chooses how much the devices cost with **one setting**,
`power.costs`:

| `power.costs` | What the devices take |
|---|---|
| `OFF` (default) | nothing: every device simply runs |
| `EASY` | a third of the FE values below, for every device |
| `HARD` | the FE values as they are (the consumption of older versions) |

Power means **Forge Energy (FE)** and comes from any mod that makes FE, from Create's rotation or from the [crank](crank.md).

## One store per tank

Every aquarium has **one energy store** for all its devices. Everything that charges the tank goes in there, and every
device of the tank draws from it:

* a cable or a generator at **any tank block**, at an attached **external filter**, at a **cabinet** under the aquarium
  or at a **light** on it;
* the [crank](crank.md) at a **cabinet** under the tank or at an attached **external filter** (no power mod needed):
  holding it for about **20 seconds** charges a day of a medium tank on `EASY`;
* with Create a [drive adapter](drive_adapter.md) on the tank, or a shaft on the cabinet under it.

The store **grows with the tank**: 80,000 FE per water block (`power.storePerWaterBlock`) and 20,000 FE per cabinet
block under it (`power.storePerCabinet`; cabinets joined to them count too). A full store runs a 2 x 2 x 2 tank with a
filter, a light and a heater that heats for **one in-game day** on `EASY` (a third of a day on `HARD`); a bigger tank has a
bigger store and needs more.

## What the devices take

The values are the `HARD` ones; `EASY` takes a third of each.

* Heaters and coolers: 8 FE per tick for every water block they heat or cool (only the kind that works right now and
  only up to the water in the aquarium); heat mats 4 FE per tick while they heat.
* Every filter 10 FE per tick, every lit light block 4 FE per tick, air pumps 2 FE per tick for every air stone or
  sponge filter they drive, misters 6 FE per tick while they spray.
* Without enough FE the group stops: heaters and coolers no longer change the temperature, filters stop cleaning the
  water, lights on the aquarium go dark. Filters get the power first, then air pumps, heaters and coolers, lights and
  misters. Every device uses less FE at a lower [power regulator](regulator.md) setting.

Devices without a tank (water pumps, the multiblock filter, the water change controller, the reverse osmosis unit, the
food and fertilizer makers, the CO2 compressor, the crucible) have their own small buffer and follow the same
`power.costs`; most of them have their own spring for the crank.

The **overview** of the tank menu shows the stored FE, the capacity and what the aquarium needs; its tooltip says how
many hours the store lasts and which group is unpowered. The windows of the cabinet and the external filter show the
same store. Lights that are not on an aquarium (hanging from a ceiling) always shine.

**Older worlds:** a config with one of the old switches (`power.heaterRequiresEnergy`, `filterRequiresEnergy`,
`lightRequiresEnergy`, `aeration.airPumpRequiresEnergy`, `pipes.pumpsRequireEnergy`, `terrarium.misterRequiresEnergy`) on
becomes `HARD`; the springs of cabinets and external filters turn into FE in the tank's store (nothing is lost).
