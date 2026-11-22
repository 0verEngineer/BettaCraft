---
navigation:
  title: Fehler melden
  icon: minecraft:spyglass
  parent: settings.md
  position: 50
---

# Fehler melden

Etwas funktioniert nicht, ein Tier ist ohne sichtbaren Grund gestorben oder eine andere Mod macht etwas Seltsames? Das
Formular für Fehlerberichte auf GitHub ([github.com/0verEngineer/BettaCraft/issues](https://github.com/0verEngineer/BettaCraft/issues/new/choose))
hilft beim Beschreiben; kein Feld ist Pflicht. Bettacraft kann aufschreiben, was es tut, damit der Bericht die Ursache
zeigt.

## 1. Debug-Log einschalten

`/bettacraft debug all on` (Operatoren oder der Besitzer einer Einzelspielerwelt, auch ohne Cheats). Ab jetzt bis zum
Neustart des Spiels schreibt Bettacraft in `logs/bettacraft-debug.log`, was passiert und warum:

* `care`: Tode mit ihrem Grund (Hunger, Temperatur, Wasserwerte, Alter, Räuber ...), das Beckenprotokoll
* `breeding`: Nester, Eier, überlebende Larven, geborene Jungtiere
* `tank`: eingesetzte oder abgelehnte Tiere (mit Grund), die Beckenblöcke
* `interior`, `network`: der Innenraum, die Miniatur-Abgleiche und ihre Größe, die Pakete
* `compat`: andere Mods (Create, Better Fish Tanks, Karten-Mods ...) und ihre Fehler
* `render`: die Miniaturen auf deinem Bildschirm
* `aquarist`: die Arbeiten des angestellten Aquaristen
* `world`: wilde Spawns

`/bettacraft debug care on` schaltet einen Bereich, `... off` schaltet ihn aus, `... trace` schreibt noch mehr (jeden
Pflegeschritt, jedes Paket). `/bettacraft debug` zeigt, was an ist, `/bettacraft debug reset` geht zurück zur Config.

Dauerhaft über einen Neustart hinaus: `debug.logLevel` (INFO, DEBUG oder TRACE) und `debug.areas` (`all` oder eine Liste
von Bereichen) in `config/bettacraft-common.toml`, auch im Config-Bildschirm. Das normale Log bleibt sauber: es bekommt
nur Infos und Warnungen, und eine Warnung, die sich wiederholt, kommt höchstens einmal pro Minute mit einem Zähler. Die
Debug-Datei hat höchstens 10 MB, die vorige bleibt als `bettacraft-debug.1.log`. Ausgeschaltet (der Standard) kostet das
Debug-Log keine Leistung.

## 2. Fehler nachstellen

Mach noch einmal, was schiefging, oder warte, bis es passiert.

## 3. Bericht schreiben

Schau auf das Becken, um das es geht, und tippe `/bettacraft report`. Die Datei `logs/bettacraft-report-<Datum>.txt`
enthält die Versionen (Bettacraft, Loader, Minecraft, Java), die Integrationen und alle geladenen Mods, die geänderten
Einstellungen, die Daten des Beckens mit seinen Tieren, das angeschaute Tier und das Ende des Debug-Logs. Ein Klick auf
den Pfad im Chat kopiert ihn; der Link daneben öffnet das Formular. Zieh die Datei in sein Feld **Diagnosis**, dazu einen
Absturzbericht aus `crash-reports/`, falls das Spiel abgestürzt ist.

Ideen und Wünsche haben auf derselben Seite ein eigenes Formular.
