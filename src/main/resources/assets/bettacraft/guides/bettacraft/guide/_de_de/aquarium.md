---
navigation:
  title: Aquarium
  icon: bettacraft:tank
  parent: index.md
  position: 30
item_ids:
  - bettacraft:tank
---

# Aquarium

Das <ItemLink id="bettacraft:tank" /> ist das Herz von Bettacraft: ein Glasblock, der sich mit seinen Nachbarn zu einem
Aquarium mit echtem Wasser verbindet.

<RecipeFor id="bettacraft:tank" />

Alle sechs Glasblöcke im Rezept müssen gleich sein; das Aquarium übernimmt dieses Glas. **Jedes Glas geht**, auch
gefärbtes Glas, getöntes Glas und das Glas anderer Mods (`#c:glass_blocks`).

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:tank" x="0" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="1" p:west="true" p:east="true" p:fill="12" />
  <Block id="bettacraft:tank" x="2" p:west="true" p:fill="12" />
</GameScene>

## Bauen

* Setze Aquarium-Blöcke nebeneinander: Blöcke aus **demselben Glas** verbinden sich zu einem Aquarium.
* Ein Aquarium ist ein **Quader**, von 1x1x1 bis 16x16x16 Blöcke (Server können bis 64 erlauben). Ein Block, der keinen
  Quader ergibt, bildet ein eigenes Aquarium.
* Zwei Aquarien verbinden sich nur, wenn sie aus demselben Glas sind und denselben [Innenraum-Maßstab](interior.md)
  haben.
* Dunkle Kantenlinien laufen nur an den Außenkanten des ganzen Aquariums, nie zwischen seinen Blöcken.
* Ein neues Aquarium zeigt mit der Vorderseite zu dem Spieler, der es gesetzt hat; die Vorderseite (für das
  Namensschild) lässt sich im Menü ändern.
* Kolben können Aquarium-Blöcke nicht verschieben; gegen Explosionen halten sie so viel aus wie Glas.

## Abbauen

Aquarium-Blöcke baust du wie Glas ab, **außer solange Tiere darin leben** oder das Schaumnest noch Eier oder Larven hat:
Die Meldung sagt dir, was noch drin ist. Nimm die Tiere zuerst heraus, an Land würden sie sterben.

Das Wasser des abgebauten Blocks verschwindet einfach (nichts läuft aus), der Rest des Aquariums behält seinen Anteil.
Die Innenraum-Blöcke hinter dem abgebauten Block droppen als Items (mehr als 27 Stapel kommen in Truhen verpackt). Wer
einen Block in der Mitte abbaut, teilt das Aquarium in mehrere kleinere.

## Mehr

* [Wasser](aquarium/water.md)
* [Bewohner](aquarium/inhabitants.md)
* [Temperatur](aquarium/temperature.md)
* [Aquarium-Menü, Namensschild und Sperre](aquarium/menu.md)
