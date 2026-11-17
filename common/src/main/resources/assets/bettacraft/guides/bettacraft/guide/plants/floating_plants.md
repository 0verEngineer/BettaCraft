---
navigation:
  title: Floating plants
  icon: bettacraft:amazon_frogbit
  parent: plants.md
  position: 60
feature: floatingPlants.enabled
---

# Floating plants

![An aquarium with duckweed, Amazon frogbit, salvinia and red root floater under a tank light](../images/floating_plants.png)

Floating plants drift on the **water surface** of an aquarium. They are no blocks: use one on the aquarium and it floats
where you look at the water. They grow with light and nutrients, put out offshoots, take up waste from the water and
give the male a good place for his bubble nest. Hornwort has no roots: its shoots hang deep into the water. [Riccia](riccia_fluitans.md) floats too (or is tied onto stone and wood).

## Species

| | Plant | Growth | Nest site |
|---|---|---|---|
| <ItemImage id="bettacraft:duckweed" /> | [Duckweed](duckweed.md) | very fast | so-so |
| <ItemImage id="bettacraft:amazon_frogbit" /> | [Amazon Frogbit](amazon_frogbit.md) | medium | the favourite |
| <ItemImage id="bettacraft:salvinia" /> | [Salvinia](salvinia.md) | fast | good |
| <ItemImage id="bettacraft:red_root_floater" /> | [Red Root Floater](red_root_floater.md) | medium | good |
| <ItemImage id="bettacraft:water_lettuce" /> | [Water Lettuce](water_lettuce.md) | medium | good |
| <ItemImage id="bettacraft:hornwort" /> | [Hornwort](hornwort.md) | very fast | so-so |
| <ItemImage id="bettacraft:riccia_fluitans" /> | [Riccia (Crystalwort)](riccia_fluitans.md) | fast | the favourite |

## Growing

![Frogbit roots seen through the glass](../images/floating_plants_roots.png)

* Plants need **light**: a lit [tank light](../equipment/light.md) on the aquarium (strong light) or daylight from above.
  Glass does not block the light, a lid or a full block above does. In the dark they do not grow.
* They need **nutrients**: the waste of the animals in the water ([water quality](../aquarium/water_quality.md)). In very clean
  water they grow slowly, in a tank with animals faster.
* A full-grown plant puts out an **offshoot** next to itself, until the leaves cover **60 %** of the surface.
* While they have light the plants **take up waste**: the water stays clean a little longer (no replacement for a
  filter or a water change).
* The **red root floater** turns red over about a quarter of a day under strong light (a tank light or full daylight),
  and green again under weak light.
* The plants drift slowly over the surface, push each other aside and keep away from where the jet of an
  [external filter](../equipment/filters.md) hits the water. In a [free shape](../aquarium/shapes.md) they stay over the water and
  never drift into a notch or over a closed-off step.

## Taking them out

* **Right-click** the surface with an **empty hand** while looking down at it: takes out the plants where you look
  (you get them as items). Where no plant floats the tank menu opens as usual.
* The <ItemLink id="bettacraft:tank_net" /> takes out the plants where you look from **any angle**, also from the
  side through the glass; **sneaking** with the net thins the whole aquarium out by half (lily pads stay). Aimed at a
  shrimp, crayfish, snail, clutch or shed shell it takes that out instead, and in open water it
  [catches small animals](../getting_started/catching.md).
* The **Plants** tab of the [tank menu](../aquarium/menu.md): number, cover, light, growth, waste uptake and a **Thin out** button.

* When the last block of an aquarium is broken its plants drop as items.

## Nest and dense cover

* The male builds his [bubble nest](../bettas/breeding.md) under the **densest cluster** of floating plants, frogbit most of all.
* Floating plants (from 10 % cover) make the male ready for a nest a little faster (1.15x).
* **Dense cover** (from 50 %): the menu and the info overlays warn, the plants shade each other and grow slower, and some
  pellets of the [feeder](../equipment/feeder.md) stay on the leaves and rot in the water. Thin them out now and then.

![The floating plants page of the tank menu](../images/menu_plants.png)

All values can be changed in the server config (`floatingPlants.*`): growth speed (0 = no growth), most cover, dense
cover, waste uptake, breeding bonus, food on the leaves, the chances of the bottle and of fishing.
