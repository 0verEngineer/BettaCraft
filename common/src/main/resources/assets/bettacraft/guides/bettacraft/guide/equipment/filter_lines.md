---
navigation:
  title: "Filter lines: one filter, several aquariums"
  icon: bettacraft:stainless_steel_pipe
  parent: equipment/multiblock_filter.md
  position: 1
---

# Filter lines: one filter, several aquariums

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:filter_chamber" p:medium="sponge" />
  <Block id="bettacraft:filter_pump_head" y="1" p:active="true" />
  <Block id="bettacraft:filter_inlet" x="1" p:facing="east" />
  <Block id="bettacraft:filter_outlet" x="1" y="1" p:facing="east" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="3" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" y="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:stainless_steel_pipe" x="3" y="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:tank" x="4" p:fill="14" />
  <Block id="bettacraft:tank" x="4" y="1" p:fill="14" />
</GameScene>

One [multiblock filter](multiblock_filter.md) serves **several aquariums** through ordinary [pipes](pipes.md):

* any glass or stainless steel pipe that reaches the filter's **inlet** is its **intake line** (blue rings);
* any that reaches its **outlet** is its **return line** (orange rings).

A pipe joins every neighbour that fits, so each line **branches by itself** to every aquarium it reaches. An aquarium counts
for the filter when **both lines** dock on it (directly, on any side, the top or the bottom), or when the inlet and the
outlet touch it directly.

## Sharing

The filter's cleaning and its flow are **shared by the water volume** of its aquariums: a big filter is enough for several
small aquariums. Each aquarium keeps its own water. The filter screen says "Serves 3 tanks:" and lists every aquarium with
its share, its flow and its cleaned water blocks, or the reason it is not filtered.

![The filter screen: two aquariums at 50 % each, the third without an intake guard](../images/filter_screen_tanks.png)

## Flow

A line carries at most what its **weakest pipe** carries: glass 50 mB/t, stainless steel 250 mB/t. The pump head moves up
to 100 mB/t, so on glass lines the filter runs at half; the filter screen says "Its lines carry at most ...". Several intake
and return lines (more inlets and outlets on the filter) **add up**: two glass lines each way give it its full flow again.
Lines may run side by side in a [pipe bundle](pipe_bundle.md), also two of the same role.

## Docks and guards

Where a line docks on an aquarium, a [pipe dock](bulkhead.md) appears inside (one per line of a bundle), from which interior
pipes go on to the outlets. Every dock needs its [guard](guards.md): an intake guard on the intake line, an outlet guard on
the return line. **Without its guards the filter does not filter that aquarium.**

## Short circuit

A line that reaches the inlet **and** the outlet is a **short circuit** (red rings): the filter stops and says so in its
screen and in Jade / The One Probe / WTHIT. Keep neighbouring lines apart with the [pipe wrench](pipe_wrench.md). The same
happens when a line of a [water change controller](water_change.md) reaches the filter's inlet or outlet: join the
controller's lines to a **chamber or the pump head** instead (pipes join them on every face, without a role).

With `features.pipes.filterLines = false` a filter serves one aquarium as before, without guards, and its pipes are plain
water pipes.
