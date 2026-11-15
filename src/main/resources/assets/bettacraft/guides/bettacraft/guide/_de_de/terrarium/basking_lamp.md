---
navigation:
  title: Wärmespot
  icon: bettacraft:basking_lamp
  parent: terrarium.md
  position: 20
item_ids:
  - bettacraft:basking_lamp
---

# Wärmespot

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:basking_lamp" p:lit="true" />
</GameScene>

Der <ItemLink id="bettacraft:basking_lamp" /> ist eine Wärme- und UV-Lampe in einem Reflektor. Stell ihn auf einen
**oberen Block** des Beckens (oder häng ihn unter einen Deckenblock direkt darüber). Er ist eine Lampe wie die
[Aquarienlampen](../equipment/light.md): Licht 15, **Dimmer und Tageslicht-Timer** des Beckens, Redstone schaltet ihn aus,
und mit `power.lightRequiresEnergy` braucht er FE des Beckens.

An einem [Terrarium oder Paludarium](../terrarium.md), solange er leuchtet:

* wird das Land darunter zum **Sonnenplatz**, 12 K wärmer als die Luft (`terrarium.baskingSpotLift`); Terrarientiere
  gehen dorthin, um sich aufzuwärmen;
* gibt er am Sonnenplatz **UV 5** (`terrarium.baskingLampUv`);
* wärmt er die Luft etwas (bis zu 4 K mit Spots über einem Viertel der Oberseite, `terrarium.baskingLampAir`).

Ein Sonnenplatz braucht **Land unter dem Spot**: Über Wasser gibt es keinen.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:basking_lamp" />
