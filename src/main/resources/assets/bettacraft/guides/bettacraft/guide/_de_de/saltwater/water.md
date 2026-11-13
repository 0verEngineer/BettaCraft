---
navigation:
  title: Salzwasser
  icon: bettacraft:sea_salt
  parent: saltwater.md
  position: 1
item_ids:
  - bettacraft:sea_water_bucket
  - bettacraft:sea_salt
  - bettacraft:calcium_additive
---

# Salzwasser

## Meerwasser bekommen

* **Meerwassereimer**: einen leeren Eimer im Ozean oder am Strand auf Wasser benutzen (Server-Config
  `saltwater.oceanWaterIsSalty`). In der Welt gießt er wie normales Wasser; ins Aquarium gegossen bringt er 35 ‰ Salz.
* **Meersalz**: aus einem Meerwassereimer (Werkbank, der Eimer bleibt) oder vom fahrenden Händler. Ein Wassereimer und
  Meersalz ergeben einen Meerwassereimer.
* Rechtsklick mit **Meersalz** auf ein Aquarium: eine Portion salzt einen Eimer (1000 mB) seines Wassers zu Meerwasser auf;
  **Schleichen** gibt so viel hinein, wie bis 35 ‰ fehlt.

## Mischen

Das Becken mischt, was man hineingießt, nach Menge: zwei Eimer Meerwasser und zwei Eimer Süßwasser ergeben 17,5 ‰
(Brackwasser). Ablassen ändert den Salzgehalt nicht, und ein Meerwasserbecken füllt die Eimer mit Meerwasser. **Beim
Verdunsten geht nur Wasser**: das Salz bleibt und das Wasser wird salziger, deshalb ein Meerwasserbecken mit **Süßwasser**
auffüllen. Rohre und Pumpen anderer Mods bringen Süßwasser. Das Logbuch vermerkt, wenn das Wasser brackig, salzig oder
wieder süß wird.

| Wasser | ‰ | Wer darin lebt |
|---|---|---|
| Süßwasser | 0–2 | Bettas, alle Beifische, Garnelen, Krebse, Schnecken, Zwergkrallenfrösche, Aquarienpflanzen |
| Brackwasser | 2–20 | Guppys, Endler und Platys bis 18 ‰, Rennschnecken bis 30 ‰; Aquarienpflanzen wachsen ab 4 ‰ nicht mehr |
| Meerwasser | 20–40 | die Rifffische (28–40 ‰, die Gelbschwanz-Demoiselle ab 20), Putzergarnelen und Turboschnecken (28–40), Einsiedlerkrebse (20–42), Korallen |

Ein Tier außerhalb seines Bereichs bekommt einen **Salzschock**: ein paar ‰ zu viel stressen es nur (wie schlechtes
Wasser), ein Süßwasserfisch im Meerwasser stirbt binnen Stunden, ebenso ein Rifffisch im Süßwasser. Das Aquarium nimmt
kein Tier, das sein Wasser töten würde (Config `saltwater.refuseDeadlyWater`). Wassertieren aus Vanilla und anderen Mods ist
das Salz standardmäßig egal (`saltwater.foreignAnimals = LENIENT`); mit `REALISTIC` brauchen Kabeljau, Tropenfisch,
Kugelfisch und Tintenfisch Salzwasser, Axolotl und Kaulquappen Süßwasser.

## Kalzium und Nitrat

* **Kalzium** (mg/L) kommt mit Meerwasser und Meersalz (Meerwasser: 420). Die Steinkorallen bauen daraus ihre Skelette und
  verbrauchen es; unter 380 wachsen sie langsamer, unter 250 gar nicht. Wasserwechsel mit Meerwasser und der
  **Kalzium-Zusatz** (aus Calcit oder Knochenmehl und Meersalz) füllen es wieder auf.
* **Nitrat** (NO₃) ist der Abfall im Wasser, wie Meerwasseraquarianer ihn messen: das Menü zeigt es neben dem Salz.
  Steinkorallen werden ab 40 mg/L langsamer und hören bei 60 auf; Eiweißabschäumer und Lebendgestein halten es niedrig.

Der Wasser-Tab im [Aquarium-Menü](../aquarium/menu.md) zeigt Salz, Kalzium und Nitrat statt der Glas-Zeile, und in der
letzten Zeile die Abschäumer, die Strömungspumpen mit ihrer Strömung, das Lebendgestein und wie viel vom Licht die Korallen
nutzen können.
