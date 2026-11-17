---
navigation:
  title: Antriebsadapter (Create)
  icon: minecraft:grindstone
  parent: equipment.md
  position: 92
item_ids:
  - bettacraft:drive_adapter
feature: compat.create.drive
mod: create
---

# Antriebsadapter (Create)

<ItemImage id="bettacraft:drive_adapter" scale="2" />

![Ein Aquarium an Create: ein Antriebsadapter an seiner Wand, eine Welle in den Schrank darunter](../images/drive_adapter.png)

Nur mit **Create**. Der Antriebsadapter macht aus der Drehkraft von Create den [Strom](power.md) eines Aquariums: eine
Welle daran lädt den FE-Puffer des Beckens, und jedes Gerät, das FE aus dem Becken nimmt, läuft damit, ganz ohne
FE-Kabel: Filter, Strömungspumpen und Abschäumer, Heizer, Kühler und Heizmatten, Licht, Luftpumpen und Sprühanlagen.
Freistehende Maschinen (Wasserpumpe, Pumpenkopf des Filters, Wasserwechsel-Steuerung, Niveausensor, Osmoseanlage,
Futtermaschine, Düngermaschine) behalten wie bisher ihre eigene Welle (siehe [Create](../settings/create.md)).

## Aufstellen

* An eine **Beckenwand** oder **unter ein Becken** setzen: die Messingplatte kommt ans Glas, die Welle zeigt vom Becken
  weg. Neben ein Becken gesetzt dreht er sich von selbst dorthin (zuerst zum Becken darüber).
* Eine Create-**Welle, ein Zahnrad, ein Getriebe oder einen Motor** an seine Welle anschließen. Der Adapter gehört zum
  Netz von Create: der Wellenstummel dreht sich mit, und die Ingenieursbrille zeigt seinen Stress und was er dem Becken
  gibt.
* Das Aquarium braucht die Strom-Optionen des Servers (`power.heaterRequiresEnergy` und die anderen): ohne sie nimmt
  kein Gerät FE, und der Adapter läuft nur leer.

## Leistung

* **64 U/min** (in jede Richtung) geben einem Adapter seine volle Leistung, **256 FE pro Tick**, langsamer anteilig
  (32 U/min: 128 FE/t). Schneller gibt nicht mehr.
* Der Adapter gibt, was das Becken braucht: was seine Geräte in der letzten Sekunde genommen haben, plus was seinen
  Puffer füllt. Ein voller Puffer und wenige Geräte nehmen nur wenig.
* **Mehrere Adapter** an einem Becken teilen sich die Arbeit und addieren sich, bis höchstens **1024 FE pro Tick** je
  Becken.

## Stress

![Die Ingenieursbrille an einem Antriebsadapter: sein Stress und was er dem Becken gibt](../images/drive_adapter_goggles.png)

Der Adapter kostet **echten Stress** im Netz von Create, nach seiner Last: **2 SU für jedes FE pro Tick**, das er
gibt (ein Becken, das 64 FE/t nimmt, kostet 128 SU), mindestens **0,5 SU pro U/min**, solange er ohne Last dreht.
Während sich der Puffer eines Beckens füllt, läuft der Adapter unter voller Last. Ein **überlastetes** Netz gibt dem
Becken nichts (die Brille sagt es); die Geräte greifen dann auf das FE im Puffer und die mit der [Kurbel](crank.md)
aufgezogenen Federn zurück.

## Auch der Schrank nimmt eine Welle

![Eine Create-Welle in der Seite eines Schranks, mit Flansch](../images/cabinet_drive.png)

Ein [Schrank](cabinet.md) unter dem Becken kann dasselbe wie ein Adapter: die Seite im Schrankmenü wählen (der Knopf mit
der Welle links vom Filterplatz: keine, hinten, links, rechts, vorne oder unten) und dort eine Welle anschließen. Ein
Messingflansch markiert die Seite. Mit Aufziehen an (`features.power.winding`) **zieht, was das Becken nicht braucht,
die Feder des Schranks auf**, so läuft das Becken noch eine Weile weiter, wenn die Drehung stoppt. Die Overlays (Jade,
The One Probe, WTHIT) zeigen am Schrank und am Becken, was die Welle gibt.

## Einstellungen

Die Schalter `features.compat.create.drive` (Adapter und Schrank-Welle) und `features.compat.create.cabinetDrive` (nur
die Schrank-Welle); die Zahlen im Server-Config-Abschnitt `createDrive`: volle Drehzahl, FE je Anschluss, Obergrenze je
Becken, Stress pro FE und im Leerlauf, FE pro Feder-Tick und die Seiten, die ein Schrank nutzen darf (`cabinetSides`).

## Unterwegs

An ein Becken auf einer Create-Contraption geklebt, fährt der Adapter mit. Dort dreht sich kein Create-Netz, also dreht
er sich mit der Fahrt selbst: je schneller er sich bewegt (`createTravel.dynamoFullSpeed`, 8 Blöcke pro Sekunde für die volle
Leistung), desto mehr lädt er den Puffer des Beckens für die mitfahrenden Filter und Heizer. Auf einem Lager bewegt sich ein
Adapter an der Achse nicht und gibt nichts. Auf einem Luftschiff von Create Aeronautics arbeitet er wie am Boden, an einer
Welle, die ein Generator des Schiffs dreht. Siehe [Becken mit Create bewegen](../settings/create.md).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:drive_adapter" />
