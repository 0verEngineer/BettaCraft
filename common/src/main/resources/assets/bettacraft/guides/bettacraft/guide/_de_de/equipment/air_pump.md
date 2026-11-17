---
navigation:
  title: Luftpumpe
  icon: bettacraft:air_pump
  parent: equipment.md
  position: 32
item_ids:
  - bettacraft:air_pump
  - bettacraft:air_hose
---

# Luftpumpe

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:air_pump" p:facing="north" />
</GameScene>

![Eine Luftpumpe neben einem Aquarium: ihr Schlauch geht über den Rand, drei Ausströmer und ein Schwammfilter sprudeln](../images/aeration.png)

Die <ItemLink id="bettacraft:air_pump" /> ist eine kleine Membranpumpe. Stell sie **neben ein Aquarium** (an jede
Seite) oder **oben darauf**: ihr Luftschlauch führt über den Glasrand ins Wasser. Sie treibt die
[Ausströmersteine](air_stone.md) und die [Schwammfilter](sponge_filter.md) im [Innenraum](../interior.md) dieses
Aquariums. Sie kann auch **in den [Aquariumschrank](cabinet.md)** unter dem Aquarium (eigener Platz im Schrank-Menü):
ihr Schlauch läuft dann zusammen mit dem Filterschlauch an der Auslass-Seite des Schranks hoch.

Der Schlauch führt innen an der Scheibe bis zum Boden und über den Boden zum nächsten Ausströmer oder Schwammfilter,
von dort weiter zum nächsten.

* Eine Pumpe treibt **4 Ausströmer oder Schwammfilter** mit voller Stärke. Bei mehr teilen sie sich die Luft und
  sprudeln weniger (eine zweite Pumpe hilft).
* Sie läuft, solange im Aquarium etwas Luft braucht, und steht sonst still. Ihr leises Summen ist standardmäßig aus
  (Client-Config `sound.airPumpSound`).
* **Leistungsregler:** Rechtsklick (oder Schleichen + Mausrad, während du sie ansiehst) stellt 0 bis 100 % ein.
  Weniger Leistung gibt weniger Luft, braucht weniger Energie und ist leiser (siehe [Leistungsregler](regulator.md)).
* **Energie:** ohne Strom-Mods läuft sie einfach. Ein Server kann FE verlangen (`aeration.airPumpRequiresEnergy`):
  2 FE/t je Ausströmer, geliefert wie bei der übrigen Technik (siehe [Strom](power.md)).

Der <ItemLink id="bettacraft:air_hose" /> ist nur ein Bauteil (getrockneter Seetang oder Schleimbälle).

Ausströmer und Schwammfilter bringen **Sauerstoff** ins Wasser, siehe [Sauerstoff](../aquarium/oxygen.md).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:air_pump" />
<RecipesFor id="bettacraft:air_hose" />
