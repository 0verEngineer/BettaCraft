---
navigation:
  title: Flüssigdünger
  icon: bettacraft:liquid_fertilizer
  parent: plants.md
  position: 12
feature: fertilizer.enabled
item_ids:
  - bettacraft:liquid_fertilizer
  - bettacraft:kelp_meal
  - bettacraft:nutrient_salt
  - bettacraft:fertilizer_canister
---

# Flüssigdünger

<Row>
  <ItemImage id="bettacraft:kelp_meal" scale="2" />
  <ItemImage id="bettacraft:nutrient_salt" scale="2" />
  <ItemImage id="bettacraft:liquid_fertilizer" scale="2" />
  <ItemImage id="bettacraft:fertilizer_canister" scale="2" />
</Row>

Flüssigdünger lässt die [Aquarienpflanzen](../plants.md) und [Schwimmpflanzen](floating_plants.md) von Bettacraft
schneller wachsen. Sie wachsen auch ohne ihn; Pflanzen anderer Mods in einem Becken bleiben unbeeinflusst.

* **Von Hand düngen:** Rechtsklicke ein Aquarium mit einer Flasche <ItemLink id="bettacraft:liquid_fertilizer" />. Eine
  Flasche hat fünf Portionen; eine Portion bringt 4 Wasserblöcke auf den richtigen Spiegel (100 %). Die letzte Portion
  lässt die Glasflasche übrig.
* **Bis 100 %** wachsen die Pflanzen bis zu **anderthalbmal so schnell** (`fertilizer.growthBoost`); weniger Dünger hilft
  weniger. Der Tab **Wasser** im Aquarium-Menü zeigt den Spiegel: keiner, wenig, gut oder überdüngt.
* Die **Pflanzen verbrauchen ihn**, ein dicht bepflanztes Becken schneller als ein karges, und etwas geht von selbst
  verloren. Wasserwechsel verdünnen ihn; in Rohren reist er mit dem Wasser, ein Filterkreislauf behält ihn also.
* **Überdüngt** ab 150 % (`fertilizer.overdoseAt`): mehr bringt kein Wachstum mehr, aber die **Algen** wachsen schneller
  und breiten sich auf dem Glas aus (bei 300 % dreimal so schnell). Ein Wasserwechsel senkt ihn wieder.

## Automatisch düngen

* Die [Düngermaschine](fertilizer_maker.md) schiebt ihren Dünger per Rohr in ein Aquarium: das Aquarium nimmt ihn nur bis
  100 % (`fertilizer.pipeUpTo`), ein Rohr überdüngt also nie.
* Der **Dosierer** der Riffbecken dosiert auch Dünger (`features.fertilizer.dosing`): lege einen
  <ItemLink id="bettacraft:fertilizer_canister" /> (500 mL, zwei Flaschen) in einen Kanal und stelle seine mL pro Tag ein.
  Er gibt den Dünger in Süß- und Salzwasserbecken (50 mL = eine Portion).

## Zwischenprodukte

* <ItemLink id="bettacraft:kelp_meal" />: zwei getrocknete Seetang mit Knochenmehl gemahlen, der organische Teil.
* <ItemLink id="bettacraft:nutrient_salt" />: Knochenmehl (Phosphat), Holzkohle (Kali) und ein Eisennugget, die Mineralien.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:kelp_meal" />
<RecipesFor id="bettacraft:nutrient_salt" />
<RecipesFor id="bettacraft:fertilizer_canister" />
