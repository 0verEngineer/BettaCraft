---
navigation:
  title: Oxygen
  icon: minecraft:bubble_coral
  parent: aquarium.md
  position: 35
---

# Oxygen

Every aquarium has an **oxygen** value in mg/L, shown in the tab **Devices** of the tank menu (with a bar; the white
mark is full saturation).

**It falls with**

* **warm water:** water holds less oxygen the warmer it is (about 8.3 mg/L at 25 °C, 7.5 at 30 °C) and the animals
  use more;
* **many animals** in little water;
* rotting waste (poor water quality);
* **night in planted aquariums:** plants make oxygen in light and use a little in the dark; without light (or under a
  [daylight timer](../equipment/light.md) at night) a densely planted aquarium loses some.

**It rises with**

* the water surface (a flat, wide aquarium gets more than a deep one);
* [air stones](../equipment/air_stone.md) (the most), [sponge filters](../equipment/sponge_filter.md), filter outlets
  and flow pumps (by their [power regulator](../equipment/regulator.md));
* plants in good light.

**Effects:** from 5 mg/L it is good, below 5 low, below 2 critical (the tank log notes it). Below **3.5 mg/L** fish,
shrimp and crayfish are **stressed** like in bad water (they fall ill and die if it lasts); snails mind it a little,
dwarf clawed frogs not at all. **Bettas and dwarf gouramis** are labyrinth fish: they breathe air at the surface and
only suffer below 1 mg/L.

Strong aeration also **drives out CO2** (shown in the menu); that will matter once CO2 can be dosed for the plants.

A server can switch the oxygen off (`features.oxygen`) and change every value (`aeration.*`).
