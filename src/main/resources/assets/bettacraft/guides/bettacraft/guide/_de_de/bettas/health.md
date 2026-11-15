---
navigation:
  title: Gesundheit
  icon: minecraft:glistering_melon_slice
  parent: bettas.md
  position: 20
---

# Gesundheit

Bettas in einem Bettacraft-Aquarium haben vier Bedürfnisse: **Futter**, die **richtige Temperatur**, **genug Platz**
und **sauberes Wasser**. Das Aquarium-Menü zeigt für jeden Fisch einen Gesundheits- und einen „Satt“-Balken, Jade zeigt beides, wenn du
einen Fisch ansiehst.

## Was einen Betta krank macht

| Belastung | Ursache |
|---|---|
| Hunger | 7 Tage ohne Futter (hungrig ist ein Fisch ab einem Tag nach der letzten Fütterung) |
| Temperatur | 7 Tage außerhalb von 24 bis 30 °C |
| Überbesatz | 7 Tage mit 2 oder mehr adulten Tieren pro Wasserblock |
| Wasserqualität | 7 Tage mit schlechtem Wasser (unter 35 %, siehe [Wasserqualität](../aquarium/water_quality.md)) |

Temperatur-, Überbesatz- und Wasser-Belastung wachsen nur, solange die Bedingung schlecht ist, und sinken pro gutem Tag
um 2 Tage. Sind zwei oder mehr Bedingungen gleichzeitig schlecht, wachsen alle Belastungen **doppelt so schnell**.

## Kranke Fische

Ein kranker Betta ist blass, legt die Flossen an und schwimmt langsamer. Er bleibt krank, solange etwas nicht stimmt;
jeder gute Tag baut 2 Krankheitstage ab. Nach **14 Krankheitstagen** stirbt er. Der Gesundheitsbalken geht von 100 %
(gesund) über 50 % (gerade erkrankt) bis 0 %.

Stirbt ein Betta, verschwindet er, Spieler im Umkreis von 32 Blöcken bekommen eine Chat-Nachricht und das Log des
Aquariums nennt die Ursache. Es gibt keinen Drop.

## Verletzungen

Bettas haben 6 Lebenspunkte. Kämpfe, ein Männchen, das sein Nest bewacht, und Raubfische verletzen sie. Ein satter,
gesunder Betta heilt 2 Punkte pro Tag.

## Altersschwäche

Altersschwäche ist standardmäßig aus. Schaltet ein Server sie ein (`health.oldAge.enabled`), sterben Bettas und Beifische
nach `health.oldAge.years` Ingame-Jahren (2) zu je `health.oldAge.daysPerYear` Tagen (96), also nach 192 Tagen im Aquarium.
Das Alter zählt wie jeder andere Zähler: solange der Fisch in einem Aquarium lebt, dessen Chunk geladen ist. Ein Fisch von
vor 1.26.0 beginnt mit dem Alter seiner Lebensstufe (ein ausgewachsener mit dem Alter, in dem er ausgewachsen ist). Der Tod
steht im Becken-Logbuch mit der Ursache „Altersschwäche“. Mit `care.mode` NO_DEATH oder DECOR stirbt niemand an
Altersschwäche. Garnelen, Schnecken, Amphibien, Reptilien, Schildkröten, Tiere anderer Mods und die Mobs im Glas sterben
nicht an Altersschwäche.

## Zeit, während du weg bist

Standardmäßig steht die Zeit für Aquarien in nicht geladenen Chunks still: Niemand wird hungrig oder krank, solange
keiner in der Nähe ist. Mit `simulateUnloadedTime = true` wird die Zeit beim Laden nachgeholt, samt Futterautomat.
