---
navigation:
  title: Aquarium
  icon: bettacraft:tank
  position: 20
item_ids:
  - bettacraft:tank
---

# Aquarium

Das <ItemLink id="bettacraft:tank" /> ist das Herz von Bettacraft: ein Glasblock, der sich mit seinen Nachbarn zu einem
Aquarium mit echtem Wasser verbindet.

Alle sechs Glasblöcke im Rezept müssen gleich sein; das Aquarium übernimmt dieses Glas. **Jedes Glas geht**, auch
gefärbtes Glas, getöntes Glas und das Glas anderer Mods (`#c:glass_blocks`).

## Zwei Ausführungen

* **Mit Glastextur**: Das Aquarium zeigt die Textur seines Glases wie ein Fenster, und die Texturen sind **verbunden**:
  Der Rahmen der Glastextur läuft nur an den Außenkanten des ganzen Aquariums, nicht zwischen seinen Blöcken. Das
  klappt auch mit dem Glas anderer Mods.
* **Klarglas**: vollkommen klares Glas ohne Textur, nur mit den dünnen dunklen Kantenlinien. Stelle es aus 6
  Vanilla-Glas mit einem **Eisennugget** in der Mitte statt des Eisenbarrens her (4 Aquarien).
* Ein einzelnes Aquarium aus Vanilla-Glas lässt sich im Werkbankgitter zwischen beiden Ausführungen umwandeln (es wird
  zur jeweils anderen Ausführung).
* Die beiden Ausführungen verbinden sich nie: Ein Klarglas-Aquarium neben einem mit Glastextur bleibt ein eigenes
  Aquarium.
* Aquarien aus älteren Versionen haben die Glastextur. Ausführung und Glas zeigen Jade, The One Probe, WTHIT und der
  Wasser-Tab des Aquarium-Menüs.

![Aquarien mit Glastextur und aus Klarglas](images/tank_styles.png)

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="1" p:west="true" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="2" p:west="true" p:fill="12" />
</GameScene>

## Bauen

* Setze Aquarium-Blöcke nebeneinander: Blöcke aus **demselben Glas** verbinden sich zu einem Aquarium.
* Ein Aquarium kann **jede Form** haben (L, T, Stufen …), deren Blöcke sich mit einer Fläche berühren, von 1 Block bis
  zu einem umschließenden Quader von 16x16x16 Blöcken (Server können bis 64 erlauben). Siehe [Formen](aquarium/shapes.md).
* Zwei Aquarien verbinden sich nur, wenn sie aus demselben Glas in derselben Ausführung sind und denselben
  [Innenraum-Maßstab](interior.md) haben.
* Dünne dunkle Kantenlinien laufen nur an den Außenkanten des ganzen Aquariums, nie zwischen seinen Blöcken (ihre
  Breite und die verbundenen Texturen lassen sich in den Client-Einstellungen ändern).
* Ein neues Aquarium zeigt mit der Vorderseite zu dem Spieler, der es gesetzt hat; die Vorderseite (für das
  Namensschild) lässt sich im Menü ändern.
* Kolben können Aquarium-Blöcke nicht verschieben; gegen Explosionen halten sie so viel aus wie Glas.

## Abbauen

Aquarium-Blöcke baust du wie Glas ab, **außer solange Tiere darin leben**, das Schaumnest noch Eier oder Larven hat
oder ein Spieler in seinem [Innenraum](interior.md) ist: Die Meldung sagt dir, was noch drin ist. Nimm die Tiere zuerst heraus, an Land würden sie sterben.

Das Wasser des abgebauten Blocks verschwindet einfach (nichts läuft aus), der Rest des Aquariums behält seinen Anteil.
Die Innenraum-Blöcke hinter dem abgebauten Block droppen als Items (mehr als 27 Stapel kommen in Truhen verpackt). Wer
einen Block in der Mitte abbaut, kann das Aquarium in mehrere kleinere teilen; jedes behält das Wasser und den Innenraum
seiner eigenen Blöcke.

## In diesem Kapitel

<SubPages icons={true} />

## Bezug

Herstellung:

<RecipesFor id="bettacraft:tank" />
