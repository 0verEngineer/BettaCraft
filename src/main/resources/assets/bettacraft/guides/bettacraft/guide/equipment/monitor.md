---
navigation:
  title: Monitor
  icon: bettacraft:monitor
  parent: equipment.md
  position: 110
item_ids:
  - bettacraft:monitor
---

# Monitor

<ItemImage id="bettacraft:monitor" scale="2" />

A monitor is a screen for your tanks. Place monitor blocks **next to each other** with their screens facing the same
way: they join into one screen of up to **8 x 5 blocks** with a single frame around it. A monitor placed against the
side, top or bottom of another one takes over its direction. A wall bigger than 8 x 5 is cut into several screens.

The screen finds **every tank within 32 blocks** by itself (`monitor.range`, measured to the nearest point of the tank)
and shows their values on tiles. Each tank is named after its [name sign](../aquarium/menu.md). Out of the box
the screen shows two tiles per tank: the **temperature** with a small graph of the last day and the **water quality**.
Right-click a monitor to see its size and how many tanks it found.

* A tank that is out of range or in an unloaded chunk shows **"not loaded"** with the last value the screen saw.
* The values only travel to players who can see the monitor (within their render distance); they come every second.
* Every tank keeps a record of its values for the last 7 in-game days (one point per in-game hour), also while no
  monitor looks at it.
* With a power option on (see [Power](power.md)) a monitor needs **1 FE per tick and block** (`monitor.fePerBlock`);
  without FE it shows "No power". Any block of the screen takes FE.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:monitor" />
