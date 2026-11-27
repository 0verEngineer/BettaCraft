---
navigation:
  title: Niveausensor
  icon: bettacraft:level_sensor
  parent: equipment/water_system.md
  position: 7
item_ids:
  - bettacraft:level_sensor
---

# Niveausensor

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:fill="14" />
  <Block id="bettacraft:level_sensor" x="1" p:facing="east" />
</GameScene>

Offene Aquarien verlieren Wasser durch Verdunstung (eine [Abdeckung](tank_lid.md) hilft). Der
<ItemLink id="bettacraft:level_sensor" /> misst es, wie ein Schwimmerschalter:

* **An ein Aquarium** setzen: daneben oder darauf (oder neben den Unterschrank eines [Technikbeckens](sump.md), für das Aquarium
  darüber).
* Rechtsklick stellt sein **Ziel** ein: 100 % (Vorgabe), 95, 90, 80, 70, 60 oder 50 %.
* Liegt das Wasser 50 mB oder mehr unter dem Ziel, sagt er es und gibt ein **Redstone-Signal** (Stärke 15, an jede Seite),
  für eigene Schaltungen oder die Pumpen anderer Mods. Sonst ist er still.
* Er **misst nur**: kein Rohr verbindet sich mit ihm, er bewegt kein Wasser und braucht keinen Strom.

![Ein Niveausensor an einem Aquarium, dem Wasser fehlt: sein Redstone-Signal lässt eine Lampe leuchten](../images/level_sensor_lamp.png)

## Nachfüllen

Eine [Wasserwechsel-Steuerung](water_change.md), die dieses Aquarium befüllt (durch Berührung, über ihre
Frischwasserleitung oder über einen Multiblock-Filter), liest jeden Sensor des Aquariums **ohne Kabel** und füllt zwischen
ihren Wasserwechseln über ihre Frischwasserleitung nach, was fehlt. **Nur Süßwasser:** wenn Wasser verdunstet, bleibt das
Salz eines [Riffbeckens](../saltwater.md) zurück, und Nachfüllen mit Süßwasser bringt den Salzgehalt genau dorthin zurück,
wo er war; eine salzige Quelle wird abgelehnt.

Ohne Steuerung misst der Sensor nur und gibt sein Signal. Jade, The One Probe und WTHIT zeigen sein Ziel, wie viel fehlt,
das Signal und ob eine Steuerung das Aquarium nachfüllt.

Mit `features.pipes.topOff = false` tut der Sensor nichts, und die Steuerung wechselt nur Wasser.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:level_sensor" />
