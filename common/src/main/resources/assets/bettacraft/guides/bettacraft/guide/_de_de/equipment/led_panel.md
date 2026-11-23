---
navigation:
  title: LED-Panel
  icon: bettacraft:led_panel
  parent: equipment/light.md
  position: 10
item_ids:
  - bettacraft:led_panel
---

# LED-Panel

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:led_panel" x="0" p:axis="x" p:pos="true" p:partner_pos="true" />
  <Block id="bettacraft:led_panel" x="1" p:axis="x" p:neg="true" p:partner_pos="true" />
  <Block id="bettacraft:led_panel" x="0" z="1" p:axis="x" p:pos="true" p:partner_pos="false" />
  <Block id="bettacraft:led_panel" x="1" z="1" p:axis="x" p:neg="true" p:partner_pos="false" />
</GameScene>

Das <ItemLink id="bettacraft:led_panel" /> ist die **mittlere Lichtstufe**, stärker als die [Lichtleiste](light.md).

* Ein Gegenstand legt ein Panel **zwei Blöcke breit**: die zweite Hälfte liegt von dir weg. Panels in einer Reihe
  entlang ihrer Länge verbinden sich zu **einem langen Panel**.
* Es gibt Licht 15 **plus 2,5** auf dem Teil des Aquariums, den es abdeckt: rote Pflanzen werden **röter**, die
  Starklichtpflanzen (Monte Carlo, Alternanthera) wachsen schneller, und ohne CO2 und Dünger wachsen auch die
  **Algen** schneller.
* Färben wie die Lichtleiste (Rechtsklick mit Farbstoff färbt das ganze Panel); Redstone schaltet es aus; Dimmer und
  Tageslicht-Timer im Aquarium-Menü.
* Direkt auf einem Aquarium **deckt** es die Blöcke darunter ab wie eine [Abdeckung](tank_lid.md): dort verdunstet nichts,
  nichts fliegt heraus oder klettert hinein.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:led_panel" />
