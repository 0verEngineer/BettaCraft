---
navigation:
  title: Licht
  icon: bettacraft:tank_light
  parent: equipment.md
  position: 60
item_ids:
  - bettacraft:tank_light
---

# Aquariumlicht

<GameScene zoom="4" interactive={true}>
  <Block id="bettacraft:tank_light" x="0" p:axis="x" p:pos="true" />
  <Block id="bettacraft:tank_light" x="1" p:axis="x" p:neg="true" p:pos="true" />
  <Block id="bettacraft:tank_light" x="2" p:axis="x" p:neg="true" />
</GameScene>

Das <ItemLink id="bettacraft:tank_light" /> leuchtet mit Lichtstufe 15.

* Lichter in einer Reihe verbinden sich zu **einer langen Leiste** (nur der Länge nach, nicht zu einer Fläche).
* Setze es oben auf das Aquarium, auf den Glasrand oder **hängend** unter einen Block über dem Aquarium.
* Es ist standardmäßig an. Ein **Redstone-Signal** an irgendeinem Teil der Leiste schaltet die ganze Leiste aus, zum
  Beispiel mit einem Tageslichtsensor.
* Das Aquarium-Menü zeigt, wie viele Lichter an sind („Licht: 4/4 an“).

## Drei Stufen

![Hochleistungs-LED, LED-Panel und Lichtleiste (von links)](../images/light_tiers.png)

Die Lichtleiste ist die **schwächste von drei Stufen**: [LED-Panel](led_panel.md) (Licht 15 + 2,5) und
[Hochleistungs-LED](high_power_led.md) (Licht 15 + 5) geben auf dem Teil des Aquariums, den sie abdecken, mehr Licht.
Stärkeres Licht macht rote Pflanzen **röter** (das tiefste Rot nur unter Panel oder Hochleistungs-LED), lässt die
**Starklichtpflanzen** (Monte Carlo, Alternanthera) mit voller Geschwindigkeit wachsen (unter der Leiste langsamer)
und lässt ohne CO2 und Dünger mehr **Algen** wachsen. Der Reiter **Technik** des Aquarium-Menüs zeigt die Lichter und
die Lichtstärke.

## Dimmer und Tageslicht-Timer

Im Reiter **Technik** des Aquarium-Menüs:

* **Dimmer** 0 bis 100 %: das Licht des Aquariums leuchtet schwächer (Lichtstufe und Licht für die Pflanzen).
* **Tageslicht-Timer:** an zur An-Zeit, aus zur Aus-Zeit (Standard 08:00 bis 20:00), jeweils über eine Stunde wie ein
  Sonnenauf- und -untergang. +/- verschiebt eine Zeit um 30 Minuten (Schleichen: eine Stunde).

Ein Server kann beides abschalten (`features.lightTimer`) und die stärkeren Stufen (`features.ledTiers`).

## Farben

![Aquarien unter Lichtern in sechs Farben bei Nacht](../images/light_colors.png)

Das Licht gibt es in den **16 Farbstoff-Farben**. Weiß ist das Licht wie bisher (Lichter älterer Welten bleiben weiß).

* **Rechtsklick mit einem Farbstoff** auf ein Licht: Die ganze Leiste nimmt die Farbe an, für einen einzigen Farbstoff.
* Oder herstellen: ein Licht (beliebige Farbe) mit einem Farbstoff, oder acht Lichter um einen Farbstoff. Weißer
  Farbstoff macht es wieder weiß.
* Die Farbe **tönt das Wasser** des Aquariums darunter (mehrere Farben auf einem Aquarium mischen sich, weiße machen sie
  blasser), und das Leuchtfeld der Lampe strahlt in ihr. Schwarz gibt ein violettes „Schwarzlicht“.
* Die Farbe ist **nur Optik**: Die Lichtstufe bleibt 15, Pflanzen, die Rote Wurzelflosse und die Algen bekommen genau
  so viel Licht wie unter einer weißen Lampe.
* Wie stark das Wasser getönt wird, ist eine Client-Einstellung (`render.lightTint`, 0 bis 1, Standard 0,6).
* Mit einem **Shaderpack** (Iris) bleibt das Wasser getönt. Packs mit farbigem Blocklicht (Complementary: Option
  „Colored Lighting“) geben auch dem Licht der Lampe ihre Farbe; die Lampe nimmt dafür die Lichtfarbe der brennenden
  Kerze gleicher Farbe.

![Mit Complementary und der Option „Colored Lighting“](../images/light_shader.png)

![Die 16 Lichter](../images/light_items.png)

## Bezug

Herstellung:

<RecipesFor id="bettacraft:tank_light" />
