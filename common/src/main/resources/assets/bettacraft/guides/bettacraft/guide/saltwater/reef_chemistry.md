---
navigation:
  title: Reef chemistry
  icon: bettacraft:alkalinity_canister
  parent: saltwater.md
  position: 15
feature: corals.reefDosing
---

# Reef chemistry ("Balling light")

Stony corals build their skeletons out of the water. Besides the **calcium** of the [salt water](water.md) they need
**alkalinity** and **magnesium**, and **trace elements** give all corals their strong colours. Sea water brings all of
it; a reef tank with growing stony corals uses it up, and water changes or the [dosing pump](dosing_pump.md) bring it
back. This is the classic three-part method of reef keeping (after Hans-Werner Balling), kept simple.

| Value | Sea water | Good | What happens when it is low |
|---|---|---|---|
| Calcium (Ca) | 420 mg/L | 380–450 | below 380 the stony corals slow down, below 250 they stop |
| Alkalinity (KH) | 8 dKH | 7–11 | below 6 slower, below 5 half as fast, below 3 they stop; the pH falls with it |
| Magnesium (Mg) | 1300 mg/L | 1250–1400 | below 1150 calcium and alkalinity fall out: slower, below 1000 much slower |
| Trace elements | 50 % | above 60 % | nothing bad; above fresh sea water's level the corals glow in their strong colours |

## Where it goes

* Every growth stage of a stony coral takes calcium (40 mg/L in one water block, `saltwater.calciumPerStage`), **the
  matching alkalinity** (in the game about 29 mg/L calcium per dKH, so that calcium and alkalinity of sea water run low
  about together) and a little magnesium. In a big tank it is shared by more water.
* **Magnesium** also falls slowly in every salt water tank (`dosing.magnesiumDropPerDay`, 1 mg/L a day).
* **Trace elements** are used up within days (`dosing.traceUsePerDay`, 4 % a day).
* The **pH** of salt water follows the alkalinity: 8 dKH hold it at 8.2, lower alkalinity lets it sink.
* Evaporation leaves calcium, alkalinity and magnesium behind; topping off with fresh water dilutes them back.

## Bringing it back

* **Water changes with sea water** bring every value back towards sea water.
* The **calcium additive** raises the calcium by hand (see [salt water](water.md)).
* The [dosing pump](dosing_pump.md) doses all four every day: calcium and alkalinity in about equal mL, a little
  magnesium, trace elements for the colours.

The water tab of the [tank menu](../aquarium/menu.md) shows the alkalinity next to the calcium; hover over the line for
the magnesium and trace elements.

`features.corals.reefDosing` (common config) switches it off: alkalinity and magnesium no longer slow the corals, the values
stand still and the pump does not dose.
