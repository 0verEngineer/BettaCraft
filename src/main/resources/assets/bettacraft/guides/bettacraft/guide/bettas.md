---
navigation:
  title: Bettas
  icon: bettacraft:betta_bucket
  parent: index.md
  position: 20
item_ids:
  - bettacraft:betta_bucket
  - bettacraft:fish_bag
  - bettacraft:betta_spawn_egg
---

# Bettas

Bettas (*Betta splendens*) are small labyrinth fish from the rice paddies and swamps of South-East Asia. In
Bettacraft they are real animals with genes, a sex, six life stages and needs.

<GameScene zoom="5" interactive={true}>
  <Entity id="bettacraft:betta" x="1.2" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:male,stage:adult,genome:{finform:[halfmoon,halfmoon],fin_length:[short,short],pectoral:[normal,normal],body_color:[white,white],fin_color:[white,white],pattern:[koi,koi],pattern_color:[red,red],iridescence:[high,high],iridescence_color:[royal_blue,royal_blue]}}}" />
  <Entity id="bettacraft:betta" x="2.4" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:male,stage:adult,genome:{finform:[halfmoon,halfmoon],fin_length:[long,long],pectoral:[normal,normal],body_color:[navy,navy],fin_color:[mustard,mustard],pattern:[none,none],pattern_color:[black,black],iridescence:[high,high],iridescence_color:[royal_blue,royal_blue]}}}" />
  <Entity id="bettacraft:betta" x="3.6" y="0.5" z="0.5" rotationY="90" data="{betta:{schema:3,sex:female,stage:adult,genome:{finform:[crowntail,crowntail],fin_length:[long,long],pectoral:[normal,normal],body_color:[royal_blue,royal_blue],fin_color:[red,red],pattern:[butterfly,butterfly],pattern_color:[white,white],iridescence:[low,low],iridescence_color:[steel,steel]}}}" />
</GameScene>

## Wild bettas

Wild bettas spawn in water in swamp biomes (the biome tag `#bettacraft:betta_spawn_biomes`, which contains
`#c:is_swamp`). They have the wild type genes: short fins, round tail and muted colours. In the wild they have no
needs and do not fight.

## Catching

* **Water bucket**: right-click the fish. You get a <ItemLink id="bettacraft:betta_bucket" />.
* **Fish bag**: right-click the fish with an empty <ItemLink id="bettacraft:fish_bag" />. A bag holds one fish and
  100 mB of water and works for any small water animal. It is reusable; empty bags stack to 16.

The fish bag recipe switches to plastic when a mod with plastic sheets is installed (more bags per craft):

<RecipesFor id="bettacraft:fish_bag" />

The tooltip of a bucket or bag shows the type, sex, colours, pattern and iridescence of the fish. Use it on an
[aquarium](aquarium/inhabitants.md) to put the fish in, or on water to release it.

## Males and females

Males have long, colourful fins. Females have shorter fins, are a little paler and show vertical bars when they are in
breeding mood. The sex is shown with ♂ and ♀ in the tank menu and in Jade.

## More about bettas

* [Life stages](bettas/life_stages.md)
* [Health](bettas/health.md)
* [Genetics](bettas/genetics.md)
* [Types and colours](bettas/types.md)
* [Fights, flaring and predators](bettas/fighting.md)
