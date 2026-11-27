---
navigation:
  title: Rohrzange
  icon: bettacraft:pipe_wrench
  parent: equipment/water_system.md
  position: 2
feature: pipes.pipeWrench
item_ids:
  - bettacraft:pipe_wrench
---

# Rohrzange

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:stainless_steel_pipe" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="1" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:stainless_steel_pipe" x="2" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:glass_pipe" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:glass_pipe" x="1" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:glass_pipe" x="2" z="1" p:west="true" p:east="true" p:ring="return" />
</GameScene>

Ein [Rohr](pipes.md) verbindet sich mit jedem passenden Nachbarn. Die <ItemLink id="bettacraft:pipe_wrench" /> bestimmt, wo
nicht:

* **Rechtsklick auf eine Seite** eines Rohrs (einen Arm des Rohrs, oder die Fläche seiner Mitte, wo eine Seite zu ist): diese
  Seite ist **zu**. Das Rohr verbindet sich nicht mehr mit dem, was dort ist, und eine **Kappe** aus Stahl zeigt es. Noch ein
  Rechtsklick **öffnet** sie.
* Eine geschlossene Seite an einem von zwei Rohren reicht: sie bleiben getrennt.
* An einem [Rohrbündel](pipe_bundle.md) wirkt die Zange auf das Rohr, das du ansiehst. **Schleichen + Rechtsklick** schiebt
  dieses Rohr auf den nächsten Platz im Bündel, damit es zum nächsten Bündel passt.
* Die Zange nutzt sich nicht ab.

Wofür sie da ist:

* **Zwei Leitungen nebeneinander**, wie in der Szene oben: eine Zulauf- und eine Rücklaufleitung in benachbarten Blöcken
  würden sich verbinden und einen Kurzschluss geben; die Seiten dazwischen schließen, und beide behalten ihre Aufgabe.
* **Eine Verbindung umlegen:** hier eine Seite schließen und dort eine öffnen, und ein Abzweig geht, wohin du willst.
* **Vorbeiführen:** eine normale Wasserleitung, die an einer Filterkammer, einem Gerät oder einem Aquarium vorbeiläuft,
  verbindet sich damit; diese Seite schließen, und die Leitung läuft vorbei.

![Eine Zulauf- und eine Rücklaufleitung übereinander: die Kappen der Zange halten sie getrennt](../images/pipe_wrench_caps.png)

Mit `features.pipes.pipeWrench = false` tut die Zange nichts; geschlossene Seiten bleiben zu.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:pipe_wrench" />
