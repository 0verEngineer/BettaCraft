---
navigation:
  title: Moving tanks with Create
  icon: minecraft:minecart
  parent: settings.md
  position: 30
---

# Moving tanks with Create

With **Create** an aquarium can travel: on a mechanical piston, a bearing, a rope pulley, a gantry, a minecart or a
train, and with **Create Aeronautics** on an airship (the switch `features.compat.create.contraptions`, on by default).

## The trip

* **The whole tank goes.** All blocks of an aquarium hold together like glued, so a contraption that takes one of them
  takes the whole tank. Equipment outside the glass (heaters, filters, lights, cabinets) does not travel: it stays
  behind and works again once the tank is back. If the contraption would have to take one of these devices (glued to
  the tank, in front of a piston), it does not start.
* **The animals are packed** into the tank for the trip, like into a fish bag: bettas, tankmates and the fish of other
  mods. On the contraption you see the aquarium with its water, its miniature interior and the animals swimming their
  rounds, but they are only a picture of the packed animals.
* **The care pauses** while the tank travels: nobody gets hungry, the water stays as it is, nothing grows or breeds.
* **Put down** (the piston retracts, the bearing stops, the train arrives, the airship is disassembled), the tank is the
  same aquarium again: its water, the values of the water, its name and log, the floating plants, shrimp and snails,
  the interior with its plants and decorations, and every animal at the place it had in the tank. A tank put down turned
  takes its interior with it; lying on its side, its old floor substrate comes out as items.
* Blocks of a tank that do not arrive (the contraption could not put them down) count as broken.

## If a tank cannot be put down

Create drops a block it cannot place (an unbreakable block is in the way) as an item. Nothing of the aquarium is lost:

* If other blocks of the tank came down, they are the aquarium, with its water, values, interior and every animal; only
  the missing block counts as broken.
* If none came down (a tank of one block, or all of them failed), the dropped tank block is a **packed tank**: its
  tooltip says how many animals travel in it. Placed again anywhere, it is the same aquarium with its interior and
  animals; it stands as that one block (the interior of the other blocks comes out as items).

If a contraption with a tank on it is destroyed (killed by a command, fallen out of the world), the tank is gone with
it, and so is its interior. Trains that go into unloaded chunks keep their tanks.

## Rotation for the devices

A turning Create shaft, cogwheel, gearbox or motor whose shaft points at a **water pump**, **filter pump head**,
**water change controller**, **level sensor**, **reverse osmosis unit** or **food maker** drives it instead of FE
(switch `features.compat.create.rotation`):

* **64 RPM** (either direction) is full work, slower is proportional: 32 RPM is half the flow, half the boost.
* The pumps and controllers need the rotation only where a server makes them need FE (`pipes.pumpsRequireEnergy`).
* The reverse osmosis unit gets its pressure-pump boost from rotation even without the power options.
* The food maker on FE runs on the rotation; without FE the rotation winds its spring (at full speed twice as fast as
  the machine uses it), no crank needed.
* The devices cost **no stress**: they are not part of Create's network, they only take the speed of the shaft.
  An overstressed network gives them nothing.

## Display Links

A Create **Display Link** placed on an aquarium block reads **Tank values**: one line per value (temperature, water
quality, pH, TDS, oxygen, CO2, nitrate, fill, animals, food, salinity and so on), as many as the target has rows, so a
sign shows the first four and a display board as many as it is high. On a **monitor** it reads **Monitored tanks**:
one line per tank of the monitor with its name, temperature and water quality (switch `features.compat.create.displayLinks`).

## Create pipes

Create's fluid pipes, pumps, spouts and hose pulleys fill and drain aquariums through the same connection as the
Bettacraft pipes: fresh water in, used water out, like the pipes of every other mod.

## When a tank cannot travel

The contraption does not start, and players nearby read why:

* somebody is inside the tank's interior;
* there are eggs in the bubble nest;
* eggs or young of the tankmates, the breeding box or turtle eggs would not survive the trip.

## On an airship

On a Create Aeronautics airship the tank stays travelling until the airship is disassembled again: no animals can be
put in or taken out and nobody can enter the interior while it flies.
