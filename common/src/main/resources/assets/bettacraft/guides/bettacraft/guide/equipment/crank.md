---
navigation:
  title: Crank and winding
  icon: bettacraft:crank
  parent: equipment.md
  position: 91
item_ids:
  - bettacraft:crank
---

# Crank and winding

<ItemImage id="bettacraft:crank" scale="2" />

![The crank winding a cabinet under a tank](../images/crank.png)

The crank winds devices that have a **spring**, and it charges the **energy store of a tank**. Right-click with the crank
in your hand: every click winds a step and the ratchet clicks a little higher. **Hold the button** to keep winding (about
five clicks a second). The action bar shows how far it is wound.

## A tank: the cabinet and the external filter

The crank at a [cabinet](cabinet.md) under a tank or at an [external filter](filters.md) attached to one charges the tank's
**one energy store** (see [Power](power.md)), the same store that FE cables charge. Every device of the tank runs on it:
filters, heaters and coolers, heat mats, lights, air pumps, misters.

* Every click puts **6,500 FE** into the store (`power.crankFePerClick`). About **100 clicks, 20 seconds** of holding,
  run a medium tank (2 x 2 x 2 with a filter, a light and a heater) for **one in-game day** on `power.costs` `EASY`; on
  `HARD` the same takes three times as long and the store holds a third of a day.
* The action bar shows the store in percent, its FE and how many hours it lasts at the tank's demand. When it is full,
  it says so.
* A cabinet that is not under a tank (or joined to the cabinets of one) and a filter that hangs on no tank have nothing
  to charge.
* With `power.costs` `OFF` no device needs power, and the crank has nothing to do there.

## Devices with their own spring

**With `power.costs` `OFF`** only two machines need it: they have no other drive.

* [Food maker](../food/food_maker.md) and [fertilizer maker](../plants/fertilizer_maker.md): a full spring works for a day.
* Every other device simply runs, without power and without winding.

**With `power.costs` `EASY` or `HARD`** the devices without a tank can be wound instead of FE (`features.power.winding`):

| Device | Spring |
|---|---|
| [Food maker](../food/food_maker.md), [fertilizer maker](../plants/fertilizer_maker.md) | 1 charge; FE first, then the spring |
| [Multiblock filter](multiblock_filter.md) (wind any part) | 6 charges, takes six times as long to wind |
| [Reverse osmosis unit](reverse_osmosis.md) | 1 charge for the pressure pump (four times the water) |
| [Water change controller](water_change.md), [water pump](pipes.md) | 1 charge: a day of a pump's work |

One charge is one in-game day of one device's work and takes about five seconds of winding.

* **FE always goes first.** A spring only runs while the FE is short, so a battery and a spring back each other up.
* A spring only runs down while its chunk is loaded; time in unloaded chunks does not count.
* The windows of the machines show the spring as a brass gauge; Jade, The One Probe and WTHIT show it too (at a cabinet
  or an external filter: the tank's store).

**Older worlds:** springs that cabinets and external filters had wound before turn into FE in the tank's store the first
time the tank runs (10 FE for every tick of a device's work they held), also beyond the store's capacity: nothing is lost.

The settings are in the server config: `power.crankFePerClick` (6500) for the tanks, and in the section `winding`
`ticksPerCharge` (24000), `clicksPerCharge` (25) and the charges of each device (`multiblockFilterCharges` 6, ...). The
switch `features.power.tankCrank` turns the crank at tanks off (then only FE charges them).

## Obtaining

Crafting:

<RecipesFor id="bettacraft:crank" />
