---
navigation:
  title: Power
  icon: minecraft:redstone
  parent: equipment.md
  position: 90
feature: power.enabled
---

# Power

By default **no equipment needs power**. A server can switch it on separately for heaters and coolers, filters and
lights (`power.heaterRequiresEnergy`, `power.filterRequiresEnergy`, `power.lightRequiresEnergy`, all `false` by
default). Power means **Forge Energy (FE)** and comes from any mod that makes FE.

* Every aquarium stores FE for its equipment (100,000 FE by default, `power.bufferFe`).
* Connect a cable or a generator to **any tank block**, to an attached **external filter**, to a **cabinet** under the
  aquarium or to a **light** on it.
* Heaters and coolers need 8 FE per tick for every water block they heat or cool (only the kind that works right now
  and only up to the water in the aquarium), every filter 10 FE per tick, every lit light block 4 FE per tick.
* Without enough FE the group stops: heaters and coolers no longer change the temperature, filters stop cleaning the
  water, lights on the aquarium go dark. Filters get the power first, then heaters and coolers, then lights.

**Air pumps** run without power unless the server switches `aeration.airPumpRequiresEnergy` on: then they need 2 FE
per tick for every air stone or sponge filter they drive (connect the cable to the pump or to the aquarium). Every
device uses less FE at a lower [power regulator](regulator.md) setting.

**No generator?** With `features.power.winding` (on) every device that needs FE can be wound with the
[crank](crank.md) instead: the external filter with its heaters, the multiblock filter, the reverse osmosis unit, the water
devices and the **cabinet** under the tank, which powers every device of its tank. FE goes first, the spring stands in
while it is short.

**Create instead of FE:** with Create a [drive adapter](drive_adapter.md) on the tank, or a shaft on the cabinet under it,
charges the buffer from rotation (with real stress).

The **Water** tab of the tank menu shows the stored FE, how much the aquarium needs and which group is unpowered; the
overview warns as well. Lights that are not on an aquarium (hanging from a ceiling) always shine.
