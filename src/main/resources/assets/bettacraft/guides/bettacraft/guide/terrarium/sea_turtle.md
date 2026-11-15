---
navigation:
  title: Sea turtle
  icon: minecraft:turtle_egg
  parent: terrarium.md
  position: 54
feature: turtles.seaTurtles
---

# Sea turtle

The vanilla **sea turtle** is a big animal: it lives only in a **big sea water paludarium** with a beach. Grown sea turtles
and their hatchlings go by the same [size rule as the big fish](../big_tanks.md): 1 m long, so the tank needs

| | |
|---|---|
| Size | at least **12 × 6 blocks and 3 blocks high** (a [big tank](../big_tanks.md)) |
| Water | **100 water blocks** per sea turtle in the water part (the tank without its land); a hatchling counts as a grown one |
| Salt | **sea water**, 25 ppt or more ([salt water](../saltwater.md)) |
| Land | a [paludarium](../terrarium.md) with a **beach**: sand or soil on the land, where the eggs go |
| Climate | water 20 to 30 °C, air 18 to 32 °C |

A tank that does not fit says why: the size it needs, the water it lacks, the salt or the missing beach.

* **Hatchlings** are caught with the [fish bag](../equipment/fish_bag.md) on the beach, **grown ones** with the
  [large net](../big_tanks/large_net.md) into a [transport tub](../big_tanks/transport_tub.md) (the tub also takes one out
  of a tank). Both go into the tank with a right-click.
* A hatchling grows up in its tank and stays there: the tank is already big enough for a grown one.
* It swims and walks the vanilla way and needs no food from you.

## Eggs on the beach

Two grown sea turtles in a fitting tank (with `features.turtles.breeding`) now and then become a pair: the female lays
**2 to 4 eggs** into the sand of the beach (the egg block shows in the miniature). The eggs develop by the nest
temperature, the temperature of the ground (the air, or [heat mats](heat_mat.md) under the tank): at 29 °C in about 3
days, warmer faster; at 18 °C and below they wait, above 36 °C they die. Water over the nest drowns them; breaking the
egg block in the interior destroys them. The hatchlings come out at the nest. Vanilla sea turtles have no sexes, so the
nest temperature only sets the speed. Their own vanilla breeding is suppressed in the tank.

Switch: `features.turtles.seaTurtles` (off: no sea turtles go into tanks; those in tanks stay).
