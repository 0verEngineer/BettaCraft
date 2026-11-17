---
navigation:
  title: Crucible and stainless steel
  icon: bettacraft:crucible
  parent: equipment.md
  position: 47
item_ids:
  - bettacraft:crucible
  - bettacraft:stainless_steel_ingot
---

# Crucible and stainless steel

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:crucible" p:lit="true" />
</GameScene>

Pipes and the water devices are made of **stainless steel**. Real stainless steel is iron with chromium and nickel,
which Minecraft does not have; Bettacraft melts it in the <ItemLink id="bettacraft:crucible" /> from

* **4 iron ingots**, **1 coal** (the carbon) and **1 nether quartz** (standing in for the chromium)
* into **3** <ItemLink id="bettacraft:stainless_steel_ingot" /> in 10 seconds.

Put the ingredients into the four slots on the left (any order) and give it heat:

* **lava** (a lava source, a lava cauldron or a magma block) **right under it** heats it for free;
* **FE** while a power option is on (power mods; `crucible.fePerTick` 20 FE/t);
* otherwise **fuel** in the fuel slot: a lava bucket (100 s, the bucket stays), a blaze rod (12 s) or blaze powder (6 s).

It glows and smokes while it melts. Hoppers fill it from the top and the sides (lava buckets, blaze rods and blaze powder
only into the fuel slot) and take the steel from below.

**Other mods:** with Mekanism (metallurgic infuser: enriched iron + redstone), EnderIO (alloy smelter), Create (heated
mixing) or Thermal (induction smelter) installed, their machines make stainless steel too. The ingot carries the tag
`#c:ingots/stainless_steel`. The **steel of other mods** (`#c:ingots/steel`) is not stainless steel and does not
work in the recipes.

## Obtaining

* <ItemLink id="bettacraft:stainless_steel_ingot" />: **[Crucible](crucible.md)**: 4 × <ItemLink id="minecraft:iron_ingot" /> + 1 × <ItemLink id="minecraft:coal" /> + 1 × <ItemLink id="minecraft:quartz" /> → 3.
* Also in the metallurgic infuser (Mekanism), the alloy smelter (EnderIO), heated mixing (Create) and the induction smelter (Thermal), when installed.

Crafting:

<RecipesFor id="bettacraft:crucible" />
