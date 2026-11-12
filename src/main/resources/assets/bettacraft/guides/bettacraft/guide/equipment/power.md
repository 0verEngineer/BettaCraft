---
navigation:
  title: Power
  icon: minecraft:redstone
  parent: equipment.md
  position: 60
---

# Power

By default **no equipment needs power**. A server can switch it on separately for heaters and coolers, filters and
lights (`power.heaterRequiresEnergy`, `power.filterRequiresEnergy`, `power.lightRequiresEnergy`, all `false` by
default). Power means **Forge Energy (FE)** and comes from any mod that makes FE.

* Every aquarium stores FE for its equipment (100,000 FE by default, `power.bufferFe`).
* Connect a cable or a generator to **any tank block**, to an attached **external filter**, to a **cabinet** under the
  aquarium or to a **light** on it.
* Heaters and coolers need 1 FE per tick for every water block they heat or cool (only the kind that works right now
  and only up to the water in the aquarium), every filter 10 FE per tick, every lit light block 4 FE per tick.
* Without enough FE the group stops: heaters and coolers no longer change the temperature, filters stop cleaning the
  water, lights on the aquarium go dark. Filters get the power first, then heaters and coolers, then lights.

The **Water** tab of the tank menu shows the stored FE, how much the aquarium needs and which group is unpowered; the
overview warns as well. Lights that are not on an aquarium (hanging from a ceiling) always shine.
