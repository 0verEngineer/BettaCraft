---
navigation:
  title: Rifftechnik
  icon: bettacraft:protein_skimmer
  parent: saltwater.md
  position: 2
item_ids:
  - bettacraft:protein_skimmer
  - bettacraft:flow_pump
  - bettacraft:reef_rock
  - bettacraft:live_rock
---

# Rifftechnik

![Eiweißabschäumer und aktinisches Licht an einem Meerwasserbecken](../images/reef_technique.png)

## Eiweißabschäumer

Wie den [Außenfilter](../equipment.md) an eine Seite des Aquariums hängen. Im Salzwasser schäumt er den Abfall aus: ein
Zusatzfilter für 48 Wasserblöcke (Config `saltwater.skimmerBlocks`). Im Süßwasser schäumt nichts, dort ist er
wirkungslos. Mit `power.filterRequiresEnergy` braucht er Strom wie ein Filter.

## Strömungspumpe

Korallen brauchen bewegtes Wasser. Strömungspumpen im Innenraum unter Wasser aufstellen wie den Innenfilter (ein
2x2x2-Kasten bei Maßstab 16, der Auslass zeigt von dir weg). Eine Pumpe bewegt 24 Wasserblöcke genug für Korallen mit
starkem Strömungsbedarf (`saltwater.pumpFlowBlocks`); die Auslässe von Außenfiltern zählen ein Drittel einer Pumpe,
Innenfilter ein Sechstel. Das Menü zeigt die **Strömung** in %: Scheibenanemonen und Krustenanemonen sind ab 15 %
zufrieden, die meisten Korallen ab 35 %, Acropora will 70 %; weit über 160 bis 300 % reißt das Gewebe.

## Licht

Korallen leben vom Licht ihrer Algen: unter einer [Aquariumleuchte](../equipment.md) wachsen sie, im Dunkeln gar nicht.
Die Farbe zählt (die farbigen Lichter aus M18): **aktinisches** Blaulicht (Blau, Hellblau, Türkis oder Lila) gemischt mit
Weiß ist am besten (100 %), nur Blau 85 %, nur Weiß 70 %, andere Farben 40 %, Sonnenlicht 60 %. Unter aktinischem Licht
leuchten die Korallen in ihren kräftigsten Farben.

## Riffgestein und Lebendgestein

* **Riffgestein** (4 aus Calcit und Sand): poröser Stein für den Riffaufbau im Innenraum. Korallen und die Anemone werden
  darauf geklebt (auch auf Lebendgestein und Korallenblöcke).
* **Lebendgestein** (8 Riffgestein um eine Koralle oder vom fahrenden Händler): Riffgestein voller Kalkrotalgen und
  Bakterien. Im Salzwasser reinigt es das Wasser ohne Strom: ein Tankblock Lebendgestein (16x16x16 Innenraumblöcke) hält
  24 Wasserblöcke sauber (`saltwater.liveRockBlocks`).
