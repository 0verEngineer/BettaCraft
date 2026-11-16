---
navigation:
  title: Landtiere anderer Mods
  icon: bettacraft:terrarium_box
  parent: terrarium.md
  position: 75
feature: foreignAnimals.terrarium
---

# Landtiere anderer Mods

Land- und Sumpftiere anderer Mods können in unseren Terrarien und Paludarien leben, wenn sie ein **Terrarium-Profil**
haben ([Profile für Tiere anderer Mods](../settings/foreign_profiles.md)). Bettacraft bringt Profile mit für:

| Tier | Mod | Becken | Größenregel |
|---|---|---|---|
| [Landschildkröte](naturalist_tortoise.md) | Naturalist | trockenes Terrarium | 1 m: 12 × 6 × 3 Blöcke, 100 Blöcke Land |
| [Alligator](naturalist_alligator.md) | Naturalist | großes Paludarium | 2 m: 24 × 12 × 6 Blöcke, 800 Blöcke Becken |
| Echse (Bartagame, Leopardgecko, grün, braun) | Naturalist | trockenes Terrarium, Sonnenplatz, UV | seine Hitbox |
| Schlange (Grasnatter, Korallenotter, Klapperschlange) | Naturalist | trockenes Terrarium, Sonnenplatz | seine Hitbox |
| Wüstenskorpion | Naturalist | trockenes Terrarium, trockene Luft (10 bis 40 %) | seine Hitbox |
| Dschungelskorpion | Naturalist | trockenes Terrarium, feuchte Luft (60 bis 90 %) | 0,2 m: 3 × 2 × 1 Blöcke |
| Schnecke | Naturalist | trockenes Terrarium, feuchte Luft (70 bis 100 %), kühl | seine Hitbox |
| Krabbe | Naturalist | Paludarium, Süß- oder Meerwasser | seine Hitbox |
| Libelle | Naturalist, Critters and Companions | Paludarium, fliegt (**Deckel**) | 0,1 m (für die breiten Flügel) |
| Ente | Naturalist | großes Paludarium | 0,6 m: 8 × 4 × 2 Blöcke, 22 Blöcke Becken |
| Capybara | Naturalist | großes Paludarium | 1,2 m: 15 × 8 × 4 Blöcke, 173 Blöcke Becken |
| Wandelndes Blatt | Critters and Companions | trockenes Terrarium, feuchte Luft (60 bis 90 %) | seine Hitbox |
| Otter | Critters and Companions | großes Paludarium, kühles Wasser | 1 m: 12 × 6 × 3 Blöcke, 100 Blöcke Becken |

Mit einem Profil bekommt ein Tier:

* **Sein Becken:** ein trockenes Terrarium, ein Paludarium oder beides, mit Luft, Luftfeuchte, UV und Sonnenplatz wie unsere
  [Terrarientiere](../terrarium.md). Ein großes hat eine Länge und folgt der [Größenregel der Großtiere](../big_tanks.md),
  gemessen über das **Land** (eine Landschildkröte), das **Wasser** oder das **ganze Becken** (ein Alligator, in beidem zu Hause).
* **Unsere Pflege:** Es bekommt Hunger, frisst sein Futter von Hand auf das Becken und aus dem
  [Futterautomaten](../equipment/feeder.md) (auch Grünzeug und rohes Fleisch), achtet auf sein Klima, wird ohne Pflege krank
  und stirbt.
* **Unseren Läufer:** Im Becken läuft es über das Land des Innenraums, schwimmt im Wasser und geht zum Sonnenplatz, statt der
  KI seiner Mod (die das Land der Miniatur nicht sieht).
* **Unsere Zucht:** Ein gefüttertes Paar vermehrt sich ab und zu; ein Eierleger legt seine Eier in Sand oder Erde auf dem
  Land, die Nesttemperatur brütet sie aus (wärmer schneller). Seine eigene Zucht ist im Becken unterdrückt.
* **Die Terrarienbox:** Eine leere <ItemLink id="bettacraft:terrarium_box" /> fängt es (Rechtsklick aufs Tier), eine volle
  setzt es in ein Becken oder lässt es frei. Tiere, die zu groß für eine Box sind (der Alligator), kommen mit dem
  [großen Kescher](../big_tanks/large_net.md) in eine [Transportwanne](../big_tanks/transport_tub.md). Tiere ohne Profil lehnt
  die Box ab, außer der Server nennt sie in `terrarium.boxAllow` (sie leben dann ohne unsere Pflege in Terrarien und
  Paludarien). Ein zahmes Tier kommt nur für seinen Besitzer in die Box.

## Die anderen Tiere

| Tier | Futter (von Hand und aus dem Automaten) | Zucht | Im Becken |
|---|---|---|---|
| Echse | Heimchen, Grünzeug, täglich | Eier in Sand oder Erde (29 °C) | läuft, sonnt sich |
| Schlange | Beute (rohes Kaninchen, Huhn), alle 5 Tage | Eier in Sand oder Erde (29 °C) | schlängelt, sonnt sich |
| Skorpione | Heimchen, alle 3 Tage | lebende Junge | läuft langsam, versteckt sich eingegraben |
| Schnecke | Grünzeug, Karotten, Rote Bete | Eier in Sand oder Erde (22 °C) | kriecht, klettert am Glas |
| Krabbe | Pellets, roher Fisch | keine (ihre Larven brauchen das Meer) | läuft an Land und auf dem Grund des Wassers |
| Libelle | Fruchtfliegen, täglich | keine (ihre Larven leben jahrelang im Wasser) | fliegt über Wasser und Land, ruht an Land |
| Ente | Pellets, Samen, Grünzeug, roher Fisch | Eier in Sand oder Erde (26 °C) | schwimmt auf dem Wasser, läuft an Land |
| Capybara | Grünzeug, Melone, Zuckerrohr | lebende Junge | grast, schwimmt |
| Wandelndes Blatt | Laub | Eier in Sand oder Erde (25 °C) | sitzt still, klettert am Glas |
| Otter | roher Fisch, Muscheln | lebende Junge | schwimmt schnell, **jagt kleine Fische** (bis 0,4 Blöcke) |

* Die **Skorpione** sind in der Welt nachts feindlich; im Becken bewegt sie unser Läufer und sie greifen niemanden an (wie die
  [Mobs im Glas](mobs_in_glass.md)). Fang sie bei Tag.
* Die **Libelle** fliegt (unser Läufer hat dafür einen Flugstil): gerade durch die Luft des Beckens, nie ins Land, ins Wasser
  oder in die Deko, ab und zu dicht über dem Wasser, und sie ruht an Land.
* Das **Capybara** kommt mit dem [großen Kescher](../big_tanks/large_net.md) in eine [Transportwanne](../big_tanks/transport_tub.md);
  die anderen kommen in die Terrarienbox.
* Der **Seehase** von Critters and Companions hat ein Aquarien-Profil: ein Meerwasseraquarium (18 bis 28 °C, Pellets), keine Zucht.

## Insekten, Kleintiere, Säugetiere und Vögel

Bettacraft bringt auch Profile für die Insekten und Krabbeltiere, die Säugetiere und die Vögel von Naturalist, Critters and
Companions und Living Things mit: für jedes, das von der Größe her in ein Becken passt. Die
[Liste der Tiere anderer Mods](../settings/foreign_animal_list.md) nennt jedes mit Becken, Länge und Futter.

| Gruppe | Tiere | Becken | Zucht |
|---|---|---|---|
| Insekten | Raupe, Schmetterling, Glühwürmchen, Ameise (Naturalist); Hirschkäfer, Stabschrecke, Marienkäfer, Eichelbohrer (Critters and Companions) | trockenes Terrarium (das Glühwürmchen auch Paludarium) | keine (ihr eigenes Leben bleibt ihrer Mod) |
| Spinnen | Springspinne (Critters and Companions) | trockenes Terrarium | keine |
| Aufräumtruppe | Assel, Schnecke (Critters and Companions), Schnecke (Living Things) | feuchtes Trockenterrarium | Asseln lebende Junge, Schnecken Eier in Sand oder Erde |
| Kleine Säugetiere | Igel, Maulwurf, Ratte (Naturalist); Frettchen, Roter Panda (Critters and Companions); Koala, Affe, Waschbär (Living Things) | trockenes Terrarium (der Waschbär auch Paludarium) | lebende Junge, ein Weibchen und ein Männchen |
| Vögel | Vogel (Naturalist), Shima Enaga (Critters and Companions), Eule (Living Things) | trockenes Terrarium mit **Deckel** | Eier, ein Weibchen und ein Männchen |
| Bodenvögel | Truthahn (Naturalist), Pfau (Living Things) | großes Trockenterrarium mit **Deckel** (sie flattern hoch) | Eier in Sand oder Erde |
| Pinguin und Flamingo | Living Things | großes Paludarium: der Pinguin kalt (−10 bis 12 °C) und jagt kleine Fische, der Flamingo warm und frisst Lebendfutter | Eier in Sand oder Erde |
| Komodowaran | Naturalist | großes Trockenterrarium, Sonnenplatz, UV (2,5 m: 30 × 15 × 8 Blöcke) | keine im Becken |
| Große Säugetiere | Bär, Schwarzbär, Wildschwein, Hirsch, Zebra, Löwe, Tiger, Strauß, Geier (Naturalist); Löwe, Strauß (Living Things) | großes Trockenterrarium nach ihrer Länge (1,4 bis 2,4 m) | keine im Becken |

* **Ein Deckel für die Flieger.** Schmetterlinge, Glühwürmchen, Marienkäfer, Libellen und die Vögel brauchen eine geschlossene
  Oberseite: jeder obere Block des Beckens mit einem <ItemLink id="bettacraft:tank_lid" />, einem Licht oder einem
  Futterautomaten bedeckt. Bei offener Oberseite lehnen unsere Becken sie ab, und eins, dessen Deckel abgenommen wird, fliegt
  innerhalb von etwa einer Stunde aus dem Becken (es kommt lebend in die Welt über dem Becken).
* **Die Aufräumtruppe.** Asseln und Schnecken fressen die Futterreste (die Portionen, die niemand brauchte, verrotten nicht)
  und leben vom Abfall der anderen Tiere: Solange ein anderes Tier des Beckens frisst, brauchen sie kein eigenes Futter.
* **Glühwürmchen** leuchten nachts in ihrem Becken, alle paar Sekunden ein Aufblitzen, und das Tageslicht durchs Glas vertreibt
  sie nicht (in der Welt verstecken sie sich vor der Sonne).
* **Die Raupe** wird bei Naturalist nur auf ihrem eigenen Weg zum Schmetterling: Sie braucht einen echten Stamm neben sich in
  der Welt. Das Land eines Beckens ist sein Innenraum in Miniatur, darum bleibt sie in unseren Becken eine Raupe (Bettacraft
  unterdrückt das nicht).
* **Große** (ab 1,2 m) kommen mit dem [großen Kescher](../big_tanks/large_net.md) in eine
  [Transportwanne](../big_tanks/transport_tub.md), die anderen in die Terrarienbox. Eine volle Wanne lässt ein Landtier auf
  dem Boden frei, ein Wassertier ins Wasser. Die Großen vermehren sich im Becken nicht (die Jungen würden herauswachsen), wie
  die Großfische.
* **Zu groß** sind Elefant, Mammut, Giraffe, Flusspferd und Nashorn (Naturalist, Living Things); der Netherritter und die
  uralte Lohe von Living Things sind feindlich und der junge Enderdrache kein Tier fürs Becken: Alle werden mit Grund abgelehnt.

## Fische im Wasserteil eines Paludariums

Unsere Fische, die Fische anderer Mods mit Profil und die Wassertiere, die die Bewohnerregeln zulassen, leben auch im
**Wasserteil** eines Paludariums. Dort entscheidet der Wasserteil, nicht die Füllung des ganzen Beckens (ein Paludarium hat
sein Wasser in einem Drittel oder der Hälfte seiner Höhe): Das Wasser muss tief genug für den Fisch sein (ein halber Block, ein
hoher Fisch seine Höhe und etwas mehr), Besatz und Größenregel der Großfische messen den Wasserteil. Ein Fisch kommt über dem
offenen Wasser hinein und bleibt dort: unsere Fische halten sich selbst vom Land fern, ein Fisch einer anderen Mod, der ins
Land schwimmt, kommt zurück ins Wasser.

## Eier und die Mutter

Verlässt eine Mutter ihr Becken (Box, Wanne, Menü), nimmt sie ihr gelegtes Gelege mit: Die Eier verschwinden aus dem Nest und
sie legt sie im nächsten Becken wieder (es braucht Sand oder Erde), so weit, wie sie waren. Stirbt eine Mutter im Becken, sind
ihre Eier verloren.

Schalter: `features.foreignAnimals.terrarium` (aus: keins davon kommt in Becken, die Box nimmt keins; die im Becken bleiben).
