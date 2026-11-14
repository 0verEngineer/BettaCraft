---
navigation:
  title: Equipment
  icon: bettacraft:external_filter
  position: 30
---

# Equipment

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:cabinet" x="0" p:facing="north" p:right="true" />
  <Block id="bettacraft:cabinet" x="1" p:facing="north" p:left="true" />
  <Block id="bettacraft:tank" x="0" y="1" p:east="true" p:fill="14" />
  <Block id="bettacraft:tank" x="1" y="1" p:west="true" p:fill="14" />
  <Block id="bettacraft:tank_light" x="0" y="2" p:axis="x" p:pos="true" />
  <Block id="bettacraft:tank_light" x="1" y="2" p:axis="x" p:neg="true" />
  <Block id="bettacraft:external_filter" x="2" y="1" p:facing="west" />
</GameScene>

| Item | What it does |
|---|---|
| [Heaters](equipment/heaters.md) | warm the water to the target temperature |
| [Coolers](equipment/coolers.md) | cool the water in hot places |
| [Internal and external filter](equipment/filters.md) | keep the water clean; the external filter holds two heaters or coolers |
| [Feeder](equipment/feeder.md) | feeds the fish up to three times a day |
| [Light](equipment/light.md) | a light bar, switchable with redstone |
| [Cabinet](equipment/cabinet.md) | stand for the aquarium with room for a filter and food |
| [Tank lid](equipment/tank_lid.md) | stops the water from evaporating |
| [Tools and containers](equipment/tools.md) | aquarium net, fish bag, algae scraper, mirror stick, breeding box |

By default none of the equipment needs power; a server can switch that on (see [Power](equipment/power.md)).

## In this chapter

<SubPages icons={true} />
