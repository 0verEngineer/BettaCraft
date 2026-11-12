---
navigation:
  title: Interior
  icon: bettacraft:decor_castle
  parent: index.md
  position: 70
---

# Interior

Every aquarium has a room of its own: its **interior**. Step inside, build whatever you like with any blocks, and the
aquarium shows it as a **miniature** behind the glass.

![Inside an aquarium: a castle on a gravel and sand floor](images/interior.png)

## Entering and leaving

* **Enter:** **sneak + right-click** the aquarium with an empty hand, or use the *Interior* button in the tank menu.
  The first time the room has to be prepared ("Preparing the tank interior... 40 %"), that takes a moment.
* **Leave:** hold **sneak for 3 seconds**, or use `/bettacraft interior leave`. You return to where you stood.
* If you log out inside, you stay inside. Several players can be in the same interior.
* For a locked aquarium only the owner and operators may enter and build.

## Size

At the default **scale 16** one tank block becomes 16x16x16 interior blocks, so one interior block is one pixel of
the aquarium. A 2x1x1 aquarium has an interior of 32x16x16 blocks. Servers can choose scale 8 or 4 for new aquariums
(`interior.defaultScale`); an aquarium keeps the scale it was built with.

## Building

* Build with any block of the game and of other mods. Walls and floor of the room cannot be broken.
* The interior is **dry**, but water plants work there: seagrass, kelp, corals and sea pickles can be placed and do not
  break; corals do not die.
* **Lily pads** (`#bettacraft:floating_plants`) can be placed anywhere and float. Bettas build their bubble nest under
  them.
* Beds and respawn anchors cannot be used inside.
* Bettas swim around solid decoration. Other animals swim through it.
* [Heaters](equipment/heaters.md) and [internal filters](equipment/filters.md) are placed in here.

## The miniature

The miniature is built in the background and updated block by block. Block entities like chests or signs are not
shown. Very large builds stop at a limit (`interior.maxRenderedBlocks`); the interior tab of the menu then says
"Render limit reached".

## More

* [Substrate](interior/substrate.md)
* [Decoration](interior/decor.md)
