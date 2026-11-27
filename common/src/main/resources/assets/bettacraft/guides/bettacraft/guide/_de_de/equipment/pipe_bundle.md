---
navigation:
  title: Rohrbündel
  icon: bettacraft:stainless_steel_pipe
  parent: equipment/water_system.md
  position: 3
---

# Rohrbündel

![Eine Reihe Rohrbündel: vier Zulaufleitungen in einem Block, die nacheinander abzweigen](../images/pipe_bundle.png)

Bis zu **vier Rohre laufen in einem Block**, wie die Leitungen anderer Mods:

* **Schleichen + Rechtsklick** mit einem Glas- oder Edelstahlrohr auf ein Rohr: beide laufen in einem **Rohrbündel** weiter.
  Weitere kommen genauso hinein, bis zu vier.
* Jedes Rohr des Bündels ist **seine eigene Leitung** mit eigenen Seiten, Glas oder Stahl, und eigener Aufgabe und eigenem
  Farbring (von den Anschlüssen, die seine Leitung erreicht, siehe [Rohre](pipes.md)). Die Rohre **mischen sich nie**, auch
  zwei Leitungen derselben Aufgabe (zwei Zulaufleitungen nebeneinander gehen).
* **Schleichen + Rechtsklick mit leerer Hand** nimmt das Rohr, das du ansiehst, wieder heraus; bleibt eines übrig, ist der
  Block wieder ein normales Rohr (es behält Material, Seiten, Ring und Verkleidung). Ein abgebautes Bündel gibt jedes Rohr
  zurück.

## Verbinden

* **Bündel an Bündel:** jedes Rohr bleibt in seiner Ecke des Stahlverteilers und verbindet sich mit dem Rohr in derselben Ecke
  des nächsten Bündels, so trägt eine Reihe Bündel ihre Leitungen geradeaus weiter. Schleichen + Rechtsklick mit der
  [Rohrzange](pipe_wrench.md) schiebt ein Rohr in die nächste Ecke, damit es passt.
* **Bündel an ein einzelnes Rohr, einen Anschluss oder ein Gerät:** es trifft **eines** der Rohre des Bündels: das, das schon
  dorthin führt, sonst das mit einem offenen Ende. Sein Arm kommt aus der Mitte des Verteilers, in Linie mit diesem Rohr.
* **Bündel an ein Aquarium:** **alle** seine Rohre docken nebeneinander an, jedes mit seinem eigenen kleinen Flansch am Glas.
  Die Zange schließt die Seite eines einzelnen Rohrs wie bei einem normalen Rohr.

## Im Aquarium: ein Anschluss je Leitung

Wo ein Bündel andockt, hat innen jede seiner Leitungen ihren **eigenen Rohranschluss**, hinter ihrem Flansch, mit dem
Farbring ihrer Leitung (ein Bündel mit nur einer Leitung zu dieser Fläche dockt wie ein einzelnes Rohr in der Mitte an). So
können eine Zulauf- und eine Rücklaufleitung, oder die Frischwasser- und Abwasserleitung einer
[Wasserwechsel-Steuerung](water_change.md), durch **einen Beckenblock** hineinführen, und jede Leitung bekommt ihren eigenen
Schutz: innen an ihrem Anschluss, oder von außen, indem man den Schutz auf den Flansch dieser Leitung anwendet (siehe
[Schutzaufsätze](guards.md)). Von den Anschlüssen gehen die Leitungen innen als Innenraum-Rohre weiter, auch als
[Innenraum-Rohrbündel](bulkhead.md).

![Ein Bündel mit zwei Leitungen (blau und orange) und eines mit vier Leitungen an einem Beckenblock, jede Leitung mit eigenem Flansch](../images/bundle_flanges.png)

Anschlüsse von Bündeln aus älteren Versionen, bei denen ein Anschluss in der Mitte alle Leitungen bediente, teilen sich beim
Laden der Welt von selbst in einen Anschluss je Leitung; ihre Schutzaufsätze wandern zu der Leitung, zu der sie gehören. Wo das
nicht eindeutig ist (Innenraum-Rohre hinter dem Anschluss führen zu beiden Arten von Auslässen), bleibt der alte Anschluss
gemeinsam und arbeitet wie bisher.
