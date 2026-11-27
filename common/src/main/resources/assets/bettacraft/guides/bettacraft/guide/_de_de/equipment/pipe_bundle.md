---
navigation:
  title: Rohrbündel
  icon: bettacraft:glass_pipe
  parent: equipment/multiblock_filter.md
  position: 2
---

# Rohrbündel

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:stainless_steel_pipe" p:west="true" p:east="true" p:ring="intake" />
  <Block id="bettacraft:glass_pipe" z="1" p:west="true" p:east="true" p:ring="return" />
  <Block id="bettacraft:filter_chamber" x="2" p:medium="ceramic" />
</GameScene>

![Eine Reihe Rohrbündel trägt mehrere Leitungen zwischen einem Aquarium und dem Filter](../images/pipe_bundle.png)

Bis zu **vier Rohre laufen in einem Block**, wie die Conduits anderer Mods: **Schleichen + Rechtsklick** mit einem Glas- oder
Edelstahlrohr auf ein Rohr, und beide laufen in einem **Rohrbündel** weiter; weitere kommen genauso hinein, bis zu vier.
Jedes Rohr des Bündels ist seine eigene Leitung mit eigenen Verbindungen, Glas oder Edelstahl, und eigener Aufgabe und
eigenem Ring (von den [Anschlüssen](filter_lines.md), die seine Leitung erreicht): die Rohre **mischen sich nie**, auch zwei
Leitungen derselben Aufgabe (zwei Zulaufleitungen nebeneinander). Schleichen + Rechtsklick mit leerer Hand auf ein Bündel
nimmt das Rohr, auf das du schaust, wieder heraus; mit einem Rohr übrig ist der Block wieder ein normales Rohr. Wer ein
Bündel abbaut, bekommt jedes Rohr zurück.

Zwischen zwei Bündeln bleibt jedes Rohr in seiner Ecke des Stahlverteilers und verbindet sich mit dem Rohr in derselben Ecke
des nächsten Bündels, so passen die Bündel zueinander. Ein einzelnes Rohr, ein Filteranschluss oder ein anderes Gerät neben
einem Bündel trifft **eines** seiner Rohre: das, das schon dorthin führt, sonst das mit einem offenen Ende. Aquariumglas
treffen alle nebeneinander. Die <ItemLink id="bettacraft:pipe_wrench" /> wirkt auf das Rohr des Bündels, auf das du schaust;
**Schleichen + Rechtsklick** damit setzt dieses Rohr in die nächste Ecke, damit es zum nächsten Bündel passt.

Wo ein Rohr das Bündel in ein einzelnes Rohr verlässt (oder einen Filteranschluss, ein Aquarium), kommt sein Arm aus der
**Mitte** des Verteilers, in einer Linie mit diesem Rohr.

Die Frisch- und Abwasseranschlüsse des Filters gibt es nicht mehr: ihre Rohre sind jetzt normale Edelstahlrohre.
