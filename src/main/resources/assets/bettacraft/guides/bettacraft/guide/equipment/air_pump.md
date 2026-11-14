---
navigation:
  title: Air pump
  icon: bettacraft:air_pump
  parent: equipment.md
  position: 32
item_ids:
  - bettacraft:air_pump
  - bettacraft:air_hose
---

# Air pump

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:air_pump" p:facing="north" />
</GameScene>

The <ItemLink id="bettacraft:air_pump" /> is a small membrane pump. Place it **next to an aquarium** (at any side) or
**on its top**: its air hose runs over the glass rim into the water. It drives the [air stones](air_stone.md) and the
[sponge filters](sponge_filter.md) in the [interior](../interior.md) of that aquarium.

* One pump drives **4 air stones or sponge filters** at full strength. With more, they share the air and bubble less
  (a second pump helps).
* It hums quietly while it runs and stands still when nothing in the aquarium needs air.
* **Power regulator:** right-click it (or sneak + mouse wheel while looking at it) to set 0 to 100 %. Less power gives
  less air, needs less energy and is quieter (see [Power regulator](regulator.md)).
* **Energy:** without power mods it simply runs. A server can make it need FE
  (`aeration.airPumpRequiresEnergy`): 2 FE/t per air stone, delivered like for the other equipment (see
  [Power](power.md)).

The <ItemLink id="bettacraft:air_hose" /> is only a crafting part (dried kelp or slime balls).

Air stones and sponge filters bring **oxygen** into the water, see [Oxygen](../aquarium/oxygen.md).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:air_pump" />
<RecipesFor id="bettacraft:air_hose" />
