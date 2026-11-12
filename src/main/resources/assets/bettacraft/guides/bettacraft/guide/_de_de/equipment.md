---
navigation:
  title: Ausrüstung
  icon: bettacraft:external_filter
  parent: index.md
  position: 40
---

# Ausrüstung

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:cabinet" x="0" p:facing="north" p:right="true" />
  <Block id="bettacraft:cabinet" x="1" p:facing="north" p:left="true" />
  <Block id="bettacraft:tank" x="0" y="1" p:east="true" p:fill="14" />
  <Block id="bettacraft:tank" x="1" y="1" p:west="true" p:fill="14" />
  <Block id="bettacraft:tank_light" x="0" y="2" p:axis="x" p:pos="true" />
  <Block id="bettacraft:tank_light" x="1" y="2" p:axis="x" p:neg="true" />
  <Block id="bettacraft:external_filter" x="2" y="1" p:facing="west" />
</GameScene>

| Item | Wozu |
|---|---|
| [Heizstäbe](equipment/heaters.md) | wärmen das Wasser auf die Zieltemperatur |
| [Kühler](equipment/coolers.md) | kühlen das Wasser in heißen Gegenden |
| [Innen- und Außenfilter](equipment/filters.md) | halten das Wasser sauber; der Außenfilter fasst zwei Heizer oder Kühler |
| [Futterautomat](equipment/feeder.md) | füttert die Fische bis zu dreimal am Tag |
| [Licht](equipment/light.md) | eine Lichtleiste, per Redstone schaltbar |
| [Schrank](equipment/cabinet.md) | Unterbau für das Aquarium mit Platz für Filter und Futter |
| [Aquarium-Abdeckung](aquarium/water_quality.md) | verhindert, dass das Wasser verdunstet |

Standardmäßig braucht keine Ausrüstung Strom; ein Server kann das einschalten (siehe [Strom](equipment/power.md)).
