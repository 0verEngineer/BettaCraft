---
navigation:
  title: Water
  icon: minecraft:water_bucket
  parent: aquarium.md
  position: 20
---

# Water

An aquarium holds **real vanilla water**, measured in millibuckets: 1 water block = 1000 mB. The water fills the
aquarium from the bottom up and its surface is drawn at the exact height, calm and without currents. Water in other
places of the world is not changed.

| Action | Water |
|---|---|
| Water bucket on the aquarium | +1000 mB |
| Empty bucket on the aquarium | −1000 mB, you get a water bucket |
| Betta bucket | +1000 mB and the fish |
| Fish bag | +100 mB and the fish |
| Taking a fish out with a bucket (menu) | −1000 mB |
| Taking a fish out with a fish bag (menu) | −100 mB |

* Animals can be put in from **50 %** fill (`tank.minFillToInsert`).
* While animals live in it, you cannot drain the aquarium below that level.
* The aquarium is open at the top but the walls are closed: the water does not flow out.
* You can swim in big aquariums yourself. The inhabitants cannot jump out: the top acts as a lid for them, but not for
  players.

The tank menu shows the volume as "Water 12000 / 16000 mB (75 %)".

Water also gets **used up and dirty**: open aquariums evaporate slowly, and waste lowers the water quality until you
change some water. See [Water quality and evaporation](water_quality.md). Pipes and pumps of other mods can fill and
drain an aquarium like buckets.
