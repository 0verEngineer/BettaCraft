---
navigation:
  title: Schmelztiegel und Edelstahl
  icon: bettacraft:crucible
  parent: equipment.md
  position: 47
item_ids:
  - bettacraft:crucible
  - bettacraft:stainless_steel_ingot
---

# Schmelztiegel und Edelstahl

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:crucible" p:lit="true" />
</GameScene>

Rohre und die Wassergeräte sind aus **Edelstahl**. Echter Edelstahl ist Eisen mit Chrom und Nickel, die es in Minecraft
nicht gibt; Bettacraft schmilzt ihn im <ItemLink id="bettacraft:crucible" /> aus

* **4 Eisenbarren**, **1 Kohle** (der Kohlenstoff) und **1 Netherquarz** (statt Chrom)
* zu **3** <ItemLink id="bettacraft:stainless_steel_ingot" /> in 10 Sekunden.

Die Zutaten kommen in die vier Felder links (Reihenfolge egal), dazu braucht er Hitze:

* **Lava** (eine Lavaquelle, ein Lavakessel oder ein Magmablock) **direkt darunter** heizt kostenlos;
* **FE**, solange eine Strom-Option an ist (Strom-Mods; `crucible.fePerTick` 20 FE/t);
* sonst **Brennstoff** im Brennstofffeld: ein Lavaeimer (100 s, der Eimer bleibt), eine Lohenrute (12 s) oder Lohenstaub (6 s).

Er glüht und raucht, während er schmilzt. Trichter füllen ihn von oben und den Seiten (Lavaeimer, Lohenruten und Lohenstaub
nur ins Brennstofffeld) und holen den Stahl unten heraus.

**Andere Mods:** Mit Mekanism (metallurgischer Infusor: angereichertes Eisen + Redstone), EnderIO (Legierungsofen),
Create (beheiztes Mischen) oder Thermal (Induktionsschmelzer) machen auch deren Maschinen Edelstahl. Der Barren trägt
den Tag `#c:ingots/stainless_steel`. **Stahl anderer Mods** (`#c:ingots/steel`) ist kein Edelstahl und gilt in den
Rezepten nicht.

## Bezug

* <ItemLink id="bettacraft:stainless_steel_ingot" />: **[Schmelztiegel](crucible.md)**: 4 × <ItemLink id="minecraft:iron_ingot" /> + 1 × <ItemLink id="minecraft:coal" /> + 1 × <ItemLink id="minecraft:quartz" /> → 3.
* Auch im metallurgischen Infusor (Mekanism), im Legierungsofen (EnderIO), beim beheizten Mischen (Create) und im Induktionsschmelzer (Thermal), wenn installiert.

Herstellung:

<RecipesFor id="bettacraft:crucible" />
