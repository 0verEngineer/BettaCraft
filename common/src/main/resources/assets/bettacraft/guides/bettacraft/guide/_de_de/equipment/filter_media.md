---
navigation:
  title: Filtermedien
  icon: bettacraft:ceramic_rings
  parent: equipment.md
  position: 39
item_ids:
  - bettacraft:filter_foam
  - bettacraft:ceramic_rings
  - bettacraft:activated_carbon
  - bettacraft:filter_peat
---

# Filtermedien

Was in die Kammern des [Multiblock-Filters](multiblock_filter.md) kommt:

| Medium | Reinigt | Verschleiß |
|---|---|---|
| <ItemLink id="bettacraft:filter_foam" /> | 12 Wasserblöcke (mechanisch) | setzt sich am schnellsten zu; ein verstopfter Schwamm bremst auch den Durchfluss |
| <ItemLink id="bettacraft:ceramic_rings" /> | 24 Wasserblöcke (Bakterien) | setzt sich halb so schnell zu |
| <ItemLink id="bettacraft:activated_carbon" /> | 16 Wasserblöcke (chemisch) | verbraucht sich, dann ersetzen |
| <ItemLink id="bettacraft:filter_peat" /> | 6 Wasserblöcke; macht das Wasser weich und senkt den [pH](../aquarium/water_chemistry.md) | verbraucht sich langsam |

Medien verschmutzen, während der Filter läuft, schneller mit mehr Fischen (`multiblockFilter.dirtPerDay`). Tooltip und
Filter-Fenster zeigen den Schmutz; ein verstopfter Schwamm oder verstopfte Keramik reinigt noch ein Fünftel.

**Reinigen:** Mit einem schmutzigen Schwamm oder Keramikröhrchen einen **Wasserkessel** rechtsklicken: das Medium ist
ausgewaschen, der Kessel verliert eine Stufe. Kohle und Torf werden nicht gereinigt: frische einsetzen. Ein Medium, das zu
einem Viertel oder mehr verschmutzt war, hinterlässt <ItemLink id="bettacraft:mulm" /> (zwei ab drei Vierteln): Futter für
die [Tubifex-Zuchtschale](../food/tubifex_dish.md).

**Automatik:** Trichter, Rohre und die Busse von ME und RS setzen ein frisches Medium in eine leere Kammer und nehmen ein
Medium heraus, sobald es verbraucht ist (ganz verstopft bzw. aufgebraucht); ein Medium, das noch arbeitet, bleibt drin, so
zieht ein Import-Bus das neue nicht gleich wieder heraus. Config-Schalter: `features.equipment.filterMediaAutomation`.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:filter_foam" />
<RecipesFor id="bettacraft:ceramic_rings" />
<RecipesFor id="bettacraft:activated_carbon" />
<RecipesFor id="bettacraft:filter_peat" />
