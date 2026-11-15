---
navigation:
  title: Mobs in glass
  icon: bettacraft:mob_jar
  parent: terrarium.md
  position: 70
item_ids:
  - bettacraft:mob_jar
---

# Mobs in glass

A few Minecraft mobs can live in a tank too: **shrunk** to a third of a block (the `generic.scale` attribute) and
**harmless**. A drowned walks over the bottom of an aquarium, a spider climbs the glass of a terrarium, a small magma
cube hops through a dry one.

## Which mobs

| Mob | Tank | Breeds (food) | Bait |
|---|---|---|---|
| Drowned | aquarium or paludarium | yes, a baby (rotten flesh) | rotten flesh |
| Zombie | any tank; under water it becomes a drowned after 45 s | yes, a baby (rotten flesh) | rotten flesh |
| Husk | terrarium or paludarium | yes, a baby (rotten flesh) | rotten flesh |
| Skeleton, stray | terrarium or paludarium | no | bone |
| Spider, cave spider | terrarium or paludarium; climbs the glass | yes, spiderlings (crickets) | crickets |
| Silverfish, endermite | dry terrarium | no | none |
| Slime (small) | terrarium or paludarium | yes, a new small one (slime ball) | slime ball |
| Magma cube (small) | dry terrarium, at least 15 °C | yes, a new small one (magma cream) | magma cream |

Not allowed: creepers (they explode), endermen (they teleport), witches, wither skeletons and every boss. A server can
add more hostile mobs with the list `inhabitants.allow` and take mobs off with `inhabitants.deny`.

## Catching

* **Mob jar** <ItemLink id="bettacraft:mob_jar" />: weaken the mob below a quarter of its health
  (`terrarium.mobCatchHealth`), then right-click it with an empty jar. The jar needs three seconds after every try. Then
  right-click the tank with the full jar; the mob shrinks inside. On any other block it comes out at full size.
* **Bait**: right-click a tank with an **open top** (at least one top block without a lid, lamp or feeder) with a bait
  from the table. For a day, mobs of that kind within 12 blocks walk to the tank, and the first that reaches the open
  top falls in and shrinks. It has to get there: the top within a jump of where it stands (a tank one block high, or one
  sunk into the ground); spiders climb any tank. One bait, one mob; a restart clears the bait.
* **Other mods**: the EnderIO **soul vial** and the Industrial Foregoing **mob imprisonment tool** with a mob of the table
  go in too: right-click the tank with the full item, you get it back empty. Modpacks can add other capture items with
  the tag `#bettacraft:mob_capture_items`.

A mob in a tank comes out at any health: right-click it with an empty mob jar, or use the tank menu with an empty jar in
the inventory.

## Behaviour

In the tank a mob attacks nothing, through the glass or otherwise: no targets, no arrows, slimes do not hurt on touch,
zombies call no reinforcements, and nothing outside goes for it. The undead do not burn in daylight, the glass protects
them (`terrarium.mobUndeadBurn`: they do under an open top). It does not drown and does not despawn, also not in
peaceful. Its tank is chosen like for the terrarium animals: the wrong kind of tank costs health after a while.

A **zombie** in an aquarium (or one that wanders under the water of a paludarium) becomes a **drowned** after 45
seconds, a husk a zombie, as in the open world. In a terrarium it stays a zombie.

**Breeding**: feed two grown mobs of one kind their food by hand (right-click the tank). Within a day they have a young
one: a baby zombie, husk or drowned, a spiderling, a new small slime or magma cube. It grows up in two days. A mob that
bred waits two days before it breeds again (`terrarium.mobBreeding` switches it off). The stocking of the tank limits
them.

## No drops

A mob that has lived in glass **never drops anything and gives no experience**, whoever or whatever kills it (a player,
another mob, fire, a fall), in the tank and after it left it. Bred mobs are decoration and a collection, not a farm.
A mob caught with items in its hands keeps them, but they are lost when it dies.

## Settings

Common config `features.mobsInGlass.enabled` (off: no mob goes in; those in tanks stay). Server config
`terrarium.mobCatchHealth`, `terrarium.mobUndeadBurn`, `terrarium.mobBreeding`, and the lists `inhabitants.allow` and
`inhabitants.deny`.

## Obtaining

Crafting:

<RecipesFor id="bettacraft:mob_jar" />
