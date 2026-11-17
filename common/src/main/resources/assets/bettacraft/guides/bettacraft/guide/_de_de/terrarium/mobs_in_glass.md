---
navigation:
  title: Mobs im Glas
  icon: bettacraft:mob_jar
  parent: terrarium.md
  position: 70
item_ids:
  - bettacraft:mob_jar
---

# Mobs im Glas

Ein paar Minecraft-Mobs können auch in einem Becken leben: **verkleinert** auf ein Drittel Block (das Attribut
`generic.scale`) und **harmlos**. Ein Ertrunkener läuft über den Grund eines Aquariums, eine Spinne klettert am Glas eines
Terrariums, ein kleiner Magmawürfel hüpft durch ein trockenes.

## Welche Mobs

| Mob | Becken | Vermehrt sich (Futter) | Köder |
|---|---|---|---|
| Ertrunkener | Aquarium oder Paludarium | ja, ein Baby (verrottetes Fleisch) | verrottetes Fleisch |
| Zombie | jedes Becken; unter Wasser wird er nach 45 s zum Ertrunkenen | ja, ein Baby (verrottetes Fleisch) | verrottetes Fleisch |
| Wüstenzombie | Terrarium oder Paludarium | ja, ein Baby (verrottetes Fleisch) | verrottetes Fleisch |
| Skelett, Eiswanderer | Terrarium oder Paludarium | nein | Knochen |
| Spinne, Höhlenspinne | Terrarium oder Paludarium; klettert am Glas | ja, Jungspinnen (Heimchen) | Heimchen |
| Silberfischchen, Endermite | trockenes Terrarium | nein | keiner |
| Schleim (klein) | Terrarium oder Paludarium | ja, ein neuer kleiner (Schleimball) | Schleimball |
| Magmawürfel (klein) | trockenes Terrarium, mindestens 15 °C | ja, ein neuer kleiner (Magmacreme) | Magmacreme |

Nicht erlaubt: Creeper (sie explodieren), Endermen (sie teleportieren), Hexen, Witherskelette und alle Bosse. Ein Server
kann mit der Liste `inhabitants.allow` weitere feindliche Mobs zulassen und mit `inhabitants.deny` Mobs ausschließen.

## Fangen

* **Mobglas** <ItemLink id="bettacraft:mob_jar" />: den Mob unter ein Viertel seines Lebens schwächen
  (`terrarium.mobCatchHealth`), dann mit einem leeren Glas rechtsklicken. Nach jedem Versuch braucht das Glas drei
  Sekunden. Dann mit dem vollen Glas aufs Becken rechtsklicken; der Mob schrumpft darin. Auf jedem anderen Block kommt er
  in voller Größe heraus.
* **Köder**: ein Becken mit **offener Oberseite** (mindestens ein oberer Block ohne Deckel, Lampe oder Futterautomat) mit
  einem Köder aus der Tabelle rechtsklicken. Einen Tag lang laufen Mobs dieser Art im Umkreis von 12 Blöcken zum Becken,
  und der erste, der die offene Oberseite erreicht, fällt hinein und schrumpft. Er muss hinkommen: die Oberseite in
  Sprunghöhe von dort, wo er steht (ein Becken von einem Block Höhe oder eines, das im Boden eingelassen ist); Spinnen
  klettern an jedem Becken hoch. Ein Köder, ein Mob; ein Neustart löscht den Köder.
* **Andere Mods**: die **Seelenphiole** (Soul Vial) von EnderIO und das **Mob Imprisonment Tool** von Industrial Foregoing
  mit einem Mob aus der Tabelle gehen auch: mit dem vollen Item aufs Becken rechtsklicken, es kommt leer zurück. Modpacks
  können andere Fang-Items über den Tag `#bettacraft:mob_capture_items` ergänzen.

Ein Mob im Becken kommt mit jedem Leben heraus: mit einem leeren Mobglas rechtsklicken, oder über das Beckenmenü mit einem
leeren Glas im Inventar.

## Verhalten

Im Becken greift ein Mob nichts an, weder durchs Glas noch sonst: keine Ziele, keine Pfeile, Schleime verletzen bei
Berührung nicht, Zombies rufen keine Verstärkung, und nichts draußen geht auf ihn los. Untote verbrennen nicht im
Tageslicht, das Glas schützt sie (`terrarium.mobUndeadBurn`: unter einer offenen Oberseite doch). Er ertrinkt nicht und
verschwindet nicht, auch nicht im Friedlich-Modus. Sein Becken wird wie bei den Terrarientieren gewählt: das falsche
Becken kostet nach einer Weile Leben.

Ein **Zombie** in einem Aquarium (oder einer, der im Paludarium unter Wasser gerät) wird nach 45 Sekunden zum
**Ertrunkenen**, ein Wüstenzombie zum Zombie, wie in der offenen Welt. Im Terrarium bleibt er ein Zombie.

**Zucht**: zwei ausgewachsenen Mobs einer Art ihr Futter von Hand geben (Rechtsklick aufs Becken). Innerhalb eines Tages
bekommen sie ein Junges: ein Zombie-, Wüstenzombie- oder Ertrunkenen-Baby, eine Jungspinne, einen neuen kleinen Schleim
oder Magmawürfel. Es wächst in zwei Tagen heran. Ein Mob, der Junge bekommen hat, wartet zwei Tage, bevor er sich wieder
vermehrt (`terrarium.mobBreeding` schaltet das ab). Der Besatz des Beckens begrenzt sie.

## Keine Drops

Ein Mob, der im Glas gelebt hat, **droppt nie etwas und gibt keine Erfahrung**, egal wer oder was ihn tötet (ein Spieler,
ein anderer Mob, Feuer, ein Sturz), im Becken und nachdem er es verlassen hat. Gezüchtete Mobs sind Deko und Sammlung,
keine Farm. Ein gefangener Mob mit Gegenständen in den Händen behält sie, sie gehen aber verloren, wenn er stirbt.

## Einstellungen

Common-Config `features.mobsInGlass.enabled` (aus: kein Mob kommt hinein; die in Becken bleiben). Server-Config
`terrarium.mobCatchHealth`, `terrarium.mobUndeadBurn`, `terrarium.mobBreeding` und die Listen `inhabitants.allow` und
`inhabitants.deny`.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:mob_jar" />
