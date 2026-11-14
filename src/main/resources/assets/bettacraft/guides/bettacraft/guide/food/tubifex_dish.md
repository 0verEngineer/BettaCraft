---
navigation:
  title: Tubifex dish
  icon: bettacraft:tubifex_dish
  parent: food/live_food.md
  position: 30
item_ids:
  - bettacraft:tubifex_dish
  - bettacraft:tubifex
  - bettacraft:mulm
---

# Tubifex dish

<Row>
  <ItemImage id="bettacraft:tubifex_dish" scale="2" />
  <ItemImage id="bettacraft:tubifex" scale="2" />
  <ItemImage id="bettacraft:mulm" scale="2" />
</Row>

**Tubifex worms** are live food with a strong **conditioning value**: a breeding fish fed with them stays in the mood
twice as long as with mosquito larvae or daphnia (`liveFood.tubifexConditioning`). The tubifex dish breeds them in mud
and stale water, like they live in the wild.

1. Put in **mud** (right-click, or a hopper) and **water** (a bucket or a pipe; the dish holds 1000 mB and needs 250).
2. Start the culture with a few **tubifex**: mining mud in a swamp sometimes drops them.
3. Feed **mulm** (`#bettacraft:tubifex_food`). You get mulm when you wash dirty filter sponges or ceramic rings in a water
   cauldron (one from a quarter dirty, two from three quarters, see [filter media](../equipment/filter_media.md)).
   The **waste water** of a tank feeds them too: water through a pipe with a quality below 80 % counts as food.
4. A fed dish fills up in about **4 in-game days** (`liveFood.tubifexDays`) and then holds 8 portions
   (`liveFood.tubifexRoom`). Harvest with the **aquarium net** (up to 4 portions) or let a hopper or a
   [live food pump](live_food_pump.md) take them out; a few always stay for the culture.

## Stale water

The water of the dish evaporates (100 mB a day) and slowly goes **stale**, fast when you give too much mulm. Fresh water
rinses it: a bucket of water on a full dish (it runs through) or clean water from a pipe. Worms from a dish that is half
stale or worse are of **poor quality**: each meal of them stresses a fish a little like bad water
(`liveFood.poorTubifexStress`), so a fish that already lives in bad water gets sick sooner. A completely stale dish
dies off.

Right-click with an empty hand shows the water, the culture, the food, the portions ready and how stale it is; a
comparator reads how full it is.

## Obtaining

* <ItemLink id="bettacraft:tubifex" />: **Breaking mud** in swamps: 5 % per block.

Crafting:

<RecipesFor id="bettacraft:tubifex_dish" />
