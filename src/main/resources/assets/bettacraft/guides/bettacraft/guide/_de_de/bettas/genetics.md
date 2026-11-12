---
navigation:
  title: Genetik
  icon: minecraft:written_book
  parent: bettas.md
  position: 30
---

# Genetik

Jeder Betta trägt für jedes von neun Merkmalen (Loci) **zwei Allele**: eines von der Mutter, eines vom Vater. Das
dominante Allel bestimmt, was man sieht; das andere bleibt verborgen und kann bei den Nachkommen wieder auftauchen. Das
Aquarium-Menü zeigt den vollständigen Genotyp jedes Fisches (das erste Allel ist das sichtbare), der Tooltip listet alle
neun Loci auf.

| Locus | Allele, dominantes zuerst |
|---|---|
| Schwanzform | Rundschwanz > Spatenschwanz > Schleierschwanz > Delta > Halbmond > Rosenschwanz > Kronenschwanz > Doppelschwanz |
| Flossenlänge | kurz (Plakat) > lang |
| Brustflossen | normal > Dumbo (große „Elefantenohr“-Brustflossen) |
| Körper | etwa 40 Farben, dunklere Farben dominieren |
| Flossenfarbe | dieselben Farben, getrennt vom Körper (zweifarbige Fische) |
| Muster | Marmor > Drachenschuppen > Maske > Koi > Galaxy > Grizzle > Piebald > Dalmatiner > Butterfly > einfarbig > Verlauf, Querstreifen, Spitzen |
| Musterfarbe | Farbe des Musters |
| Schimmer | stark > schwach > keiner |
| Schimmerfarbe | Farbe des Schimmers |

## Vererbung

Für jeden Locus bekommt ein Jungtier ein zufälliges Allel der Mutter und eines des Vaters (klassische Mendel-Genetik).
Das Geschlecht ist zu 50 % männlich, zu 50 % weiblich. Jedes Allel kann außerdem mit kleiner Wahrscheinlichkeit zu
einem zufälligen anderen Allel **mutieren** (`breeding.mutationChance`, 1 %).

Beispiel: Zwei Halbmonde, die beide einen verborgenen Schleierschwanz tragen (Schleierschwanz / Halbmond), ergeben etwa
75 % Schleierschwänze und 25 % Halbmonde, weil Schleierschwanz über Halbmond dominiert.

Der [Zucht-Tab](../breeding.md) des Aquarium-Menüs rechnet das für dich aus: Sobald ein adultes Paar im Aquarium ist,
zeigt er die erwarteten Nachkommen pro Locus (ein Punnett-Quadrat) zusammen mit den Genotypen beider Eltern.

## Stammbaum

Jeder Betta hat eine eigene Kennung, einen Namen (im Menü umbenennen), Mutter und Vater, seine Generation und seinen
Geburtszeitpunkt. Wilde und gekaufte Fische sind Generation 0, ihre Jungen Generation 1 und so weiter.

## Datapacks

Farben, Gene und benannte Typen sind datengetrieben: `data/<namespace>/bettacraft/colors/*.json`,
`.../genes/<locus>.json` (Rang und Häufigkeit je Allel) und `.../varieties/*.json`. Datapacks können Farben hinzufügen
und Dominanz oder Häufigkeiten ändern; neue Schwanzmodelle oder Mustermasken brauchen ein Resourcepack und Code.
