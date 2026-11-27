---
navigation:
  title: CO2-Düngung
  icon: bettacraft:co2_cylinder
  parent: equipment.md
  position: 102
item_ids:
  - bettacraft:co2_cylinder
  - bettacraft:co2_solenoid
  - bettacraft:co2_station
  - bettacraft:bio_co2
  - bettacraft:yeast
---

# CO2-Düngung

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:east="true" p:fill="14" />
  <Block id="bettacraft:tank" x="1" p:west="true" p:fill="14" />
  <Block id="bettacraft:co2_cylinder" x="2" p:facing="west" p:solenoid="true" />
  <Block id="bettacraft:bio_co2" x="-1" p:facing="east" p:stage="2" />
</GameScene>

Pflanzen brauchen CO2. Wasser im Gleichgewicht mit der Luft hat nur etwa **3 mg/L**; mit CO2-Düngung wachsen die
Pflanzen viel schneller, am meisten die Starklichtpflanzen unter einem [LED-Panel oder Hochleistungs-LED](light.md)
(bis zum 2,5-fachen; Mittellichtpflanzen doppelt, Schwachlichtpflanzen 1,5-fach). Gut dosiertes CO2 bremst auch die
Algen, die Starklicht mit sich bringt.

Eine CO2-Quelle steht **neben einem Aquarium oder darauf** (wie die [Luftpumpe](air_pump.md)) und perlt in sein Wasser.

## Was CO2 bewirkt

* **Die Pflanzen** wachsen ab etwa 5 mg/L schneller, voll ab 25 mg/L (15–30 mg/L sind gut).
* **Der pH sinkt**: eine pH-Einheit je Verzehnfachung (30 mg/L: etwa -1). Siehe [Wasserchemie](../aquarium/water_chemistry.md).
* **Zu viel ist gefährlich**: ab **35 mg/L** sind die Tiere gestresst, ab **50 mg/L** ist das Wasser schlecht für sie
  (krank, am Ende tot). Bettas und Guramis atmen Luft und vertragen etwas mehr, Garnelen und Krebse weniger
  (`co2Dosing.co2StressFrom`, `co2LethalFrom`).
* **Oberfläche und Belüftung treiben es aus**: eine offene Oberfläche, [Ausströmersteine](air_stone.md),
  Schwammfilter und Filter senken das CO2. Starke Belüftung und CO2-Düngung arbeiten gegeneinander.
* **Nachts** nehmen die Pflanzen keines auf: Das CO2 steigt. Dafür gibt es das Magnetventil.
* Die Tiere atmen etwas CO2 aus, die Pflanzen nehmen es im Licht auf.

Der Reiter **Wasser** im Beckenmenü zeigt das CO2, wenn man über die Chemie-Zeile fährt: die Quellen, die Blasen pro
Sekunde und wohin das CO2 strebt.

## Bio-CO2

<ItemLink id="bettacraft:bio_co2" /> ist der günstige Weg: eine Flasche mit gärendem Zucker.

1. Rechtsklick mit einem **Wassereimer**,
2. dann mit bis zu **vier Zucker**,
3. dann mit <ItemLink id="bettacraft:yeast" /> (braune Pilze und Zucker in einer Schüssel angesetzt).

Sie gärt etwa **vier Tage** (weniger Zucker: kürzer, `co2Dosing.bioCo2Days`) und gibt etwa **eine Blase pro Sekunde**,
schwach und ungleichmäßig: Sie startet langsam, schwankt und lässt am Ende nach. Sie läuft Tag und Nacht und braucht
keinen Strom. Ist sie verbraucht, neu ansetzen. Schleichen + leere Hand gießt sie aus.

## CO2-Flasche

<ItemLink id="bettacraft:co2_cylinder" /> ist stark und regelbar. Ihr **Druckminderer** stellt **0 bis 10 Blasen pro
Sekunde** ein (Rechtsklick mit leerer Hand oder Schleichen + Mausrad); der **Blasenzähler** darauf zeigt sie.
Größere Aquarien brauchen mehr Blasen (`co2Dosing.co2PerBubble`: eine Blase pro Sekunde gibt einem Wasserblock
150 mg/L am Tag). Eine volle Flasche reicht **60 Tage bei einer Blase pro Sekunde** (`cylinderBubbleDays`). Abgebaut
behält die Flasche ihr Gas.

Das <ItemLink id="bettacraft:co2_solenoid" /> (Rechtsklick damit auf die Flasche) **schließt die Flasche, solange das
Licht aus ist**: nach der Lichtsteuerung des Aquariums (siehe [Licht](light.md)), ohne sie in der Nacht. Ohne Ventil
strömt das CO2 nachts weiter, wenn die Pflanzen keines aufnehmen – ein häufiger Grund für tote Fische.

## CO2-Abfüllstation

<ItemLink id="bettacraft:co2_station" /> füllt leere Flaschen. Sie sitzt **auf, neben oder über einem brennenden Ofen,
Räucherofen, Schmelzofen oder Lagerfeuer** (`#bettacraft:flue_gas_sources`) und sammelt das CO2 des Rauchgases,
solange das Feuer brennt: etwa ein Tag Brennen für eine volle Flasche, mehr Feuer füllen schneller
(`co2Dosing.stationTicksPerCylinder`).

* Rechtsklick mit einer Flasche setzt sie ein, die leere Hand nimmt sie heraus, Schleichen zeigt den Zustand.
* Trichter setzen leere Flaschen ein und holen nur volle heraus; ein Komparator liest den Füllstand.
* Mit [Stromkosten](power.md) braucht ihr **Kompressor FE** (`stationFe`, 20 FE/t beim Füllen).
* CO2-Gas anderer Mods (Fluid-Tag `#bettacraft:co2_gas`, mit `#c:carbon_dioxide`) kommt über Rohre: 1000 mB füllen
  eine Flasche (`foreignCo2Mb`).

`features.co2.enabled` (Common-Config) schaltet die CO2-Düngung ab: Die Geräte geben kein CO2, das Wasser bleibt beim Wert der Luft.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:co2_cylinder" />
<RecipesFor id="bettacraft:co2_solenoid" />
<RecipesFor id="bettacraft:co2_station" />
<RecipesFor id="bettacraft:bio_co2" />
<RecipesFor id="bettacraft:yeast" />
