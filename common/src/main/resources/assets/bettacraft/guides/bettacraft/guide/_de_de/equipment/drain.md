---
navigation:
  title: Abfluss
  icon: bettacraft:drain
  parent: equipment/water_system.md
  position: 8
feature: pipes.drain
item_ids:
  - bettacraft:drain
---

# Abfluss

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:water_change_controller" p:facing="south" />
  <Block id="bettacraft:drain" x="1" />
</GameScene>

Der <ItemLink id="bettacraft:drain" /> ist das **Ende einer Abwasserleitung**: ein [Rohr](pipes.md) verbindet sich an jeder
Seite mit ihm, und er nimmt alles Wasser, das durch das Rohr kommt, und **entsorgt** es. Er hält nichts und gibt nichts.

* An den **Abwasser-Ausgang** einer [Wasserwechsel-Steuerung](water_change.md) setzen (rechts, von vorne gesehen): direkt an
  den Anschluss, wie in der Szene oben, oder ans Ende eines Rohrs. Mehrere Steuerungen dürfen sich eine Abflussleitung
  teilen.
* Das alte Wasser darf stattdessen auch in einen [Vorratsbehälter](reservoir.md) oder den Tank einer anderen Mod gehen; der
  Abfluss ist der einfache Weg, es loszuwerden.
* Rohre anderer Mods dürfen ihn auch als Senke nutzen. Er nimmt Wasser (mit seinen Werten), keine andere Flüssigkeit.

![Ein Abfluss am Ende des Abwasserrohrs einer Steuerung](../images/drain.png)

Mit `features.pipes.drain = false` nimmt er kein Wasser (Rohre verbinden sich trotzdem).

## Bezug

Herstellung:

<RecipesFor id="bettacraft:drain" />
