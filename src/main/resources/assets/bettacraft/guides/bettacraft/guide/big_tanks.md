---
navigation:
  title: Big tanks
  icon: bettacraft:transport_tub
  position: 75
---

# Big tanks and big animals

Tanks can grow up to **32 × 16 × 32 blocks** (config `bigTankRules.bigMaxSize*`). Such a tank holds the **big animals**:
giant catfish, sharks, rays and even the vanilla **dolphin**. Small fish go into any tank as before; a big animal only goes
into a tank that is big enough for it.

![A 32 x 8 x 16 sea water tank with two dolphins](images/big_tank_dolphins.png)

## The size rule

An animal of known length needs, per metre of its length:

* **12 blocks** along the longer side of the tank, **6 blocks** along the shorter side and **3 blocks** of height;
* **100 water blocks** per cubic metre of its length, for each big animal (they add up).

So a 1 m pangasius or bamboo shark needs at least 12 × 6 blocks and 3 blocks of height (100 water blocks), a 2.6 m dolphin
32 × 16 blocks and 8 blocks of height (about 1760 water blocks; two dolphins fit into 32 × 16 × 8). When a tank is too
small, the message names the size the animal needs. The factors are in the config (`bigAnimalRules.*`).

Whales, bosses and hostile mobs never go into a tank; animals of other mods count as big when they are in the tag
`#bettacraft:big_tank_animals` (their length comes from their size).

## The interior of a big tank

A tank longer than **16 blocks** gets a smaller interior scale: **8** instead of 16 (and **4** from 33 blocks), so its
interior stays about 256 blocks long (`bigTankRules.interiorScale8Above`, `interiorScale4Above`). Tanks up to 16 blocks keep
scale 16.

When a tank with a decorated interior **grows past 16 blocks**, its interior is **shrunk** into the coarser one: every
cube of 2 × 2 × 2 blocks becomes one block (the most common one), so the layout stays at half the size. Nothing is lost:
the blocks that do not stay, devices with block entities, chests with their contents and the aquarium plants (whole, with
their stage) come out as items in front of the tank, packed into chests when there are many. Paid substrate beyond the new
floor comes back too.

## Dolphins

The vanilla dolphin lives only in a **very large sea water tank** (from 32 × 16 × 8 blocks, salt from 25 ‰). In the tank
it breathes at the surface and stays wet by itself. Feed it **raw fish** by right-clicking the tank with it; after two
days without fish (`bigAnimalRules.dolphinHungryDays`) it loses health, also in too fresh water. Catch one with the
[large net](big_tanks/large_net.md) into a [transport tub](big_tanks/transport_tub.md).

## Catching and carrying

Big animals do not go into buckets or fish bags. Catch them with the [large net](big_tanks/large_net.md) or, while
fishing, with a [steel leader](big_tanks/steel_leader.md), always into an empty [transport tub](big_tanks/transport_tub.md),
which carries one animal and slows you down while you carry it. Take a big animal out of a tank in the tank menu (an
empty transport tub in your inventory).

Big animals eat the small fish, young bettas and shrimp of their tank; keep them with fish of their own size. They swim
in wide curves and turn slowly.

## In this chapter

<SubPages icons={true} />

## Big animals

| Species | °C | Shoal | Betta tank | Breeding |
|---|---|---|---|---|
| <ItemImage id="bettacraft:pangasius_spawn_egg" /> [Pangasius](big_tanks/pangasius.md) | 22–30 | – | no (eats them) | Does not breed in a tank |
