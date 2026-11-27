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
| [Light](equipment/light.md) | three tiers: light bar, [LED panel](equipment/led_panel.md), [high-power LED](equipment/high_power_led.md); dimmer and daylight timer |
| [Air pump](equipment/air_pump.md) and [air stone](equipment/air_stone.md) | bubble columns and oxygen |
| [Sponge filter](equipment/sponge_filter.md), [corner filter](equipment/corner_filter.md), [intake sponge](equipment/intake_sponge.md) | more internal filters; the sponge protects the fry |
| [Power regulator](equipment/regulator.md) | 0 to 100 % for filters, pumps and air stones |
| [Multiblock external filter](equipment/multiblock_filter.md) and [filter media](equipment/filter_media.md) | a big filter of any size; media clog and are washed |
| [Water in the tank: how it all fits together](equipment/water_system.md) | pipes, wrench, bundles, docks and guards, water change and top-off, with example setups |
| [Pipes and water pump](equipment/pipes.md) | two pipes for everything; a line takes its role from the port it reaches |
| [Pipe docks and interior pipes](equipment/bulkhead.md) | where lines enter the aquarium; interior pipes and bundles, outlets with their own current |
| [Water change controller](equipment/water_change.md), [level sensor](equipment/level_sensor.md) and [drain](equipment/drain.md) | automatic water changes through four ports, top-off by the sensor, the end of the waste water |
| [Crucible and stainless steel](equipment/crucible.md) | the steel for pipes and water devices |
| [Cabinet](equipment/cabinet.md) | stand for the aquarium with room for a filter and food |
| [Tank lid](equipment/tank_lid.md) | stops the water from evaporating |
| [Tools and containers](equipment/tools.md) | aquarium net, fish bag, algae scraper, mirror stick, breeding box |
| [TDS meter and pH drop test](equipment/water_tests.md) | measure the [water chemistry](aquarium/water_chemistry.md) |
| [CO2 dosing](equipment/co2.md) | bio CO2, CO2 cylinder with solenoid valve, filling station: faster plant growth |
| [Reverse osmosis unit](equipment/reverse_osmosis.md) | reverse osmosis water (nothing dissolved) for soft water |
| [Monitor](equipment/monitor.md) | screens of up to 8 x 5 blocks with the values of every tank nearby |
| [Dosing pump](saltwater/dosing_pump.md) | doses calcium, alkalinity, magnesium and trace elements into reef tanks |
| [Sump](equipment/sump.md) and [refugium](equipment/refugium.md) | a second tank in the cabinet: more water, chambers for heaters, skimmer, media and Chaetomorpha |
| [Calcium reactor](saltwater/calcium_reactor.md) | dissolves coral rubble with CO2: calcium and alkalinity for the stony corals |

By default none of the equipment needs power; a server can switch that on (see [Power](equipment/power.md)).

## In this chapter

<SubPages icons={true} />
