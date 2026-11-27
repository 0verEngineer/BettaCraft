---
navigation:
  title: Vorratsbehälter
  icon: bettacraft:reservoir
  parent: equipment/water_system.md
  position: 9
item_ids:
  - bettacraft:reservoir
---

# Vorratsbehälter

<GameScene zoom="3" interactive={true}>
  <Block id="bettacraft:reservoir" p:fill="6" />
  <Block id="bettacraft:glass_pipe" x="1" p:west="true" p:east="true" p:ring="fresh" />
  <Block id="bettacraft:water_change_controller" x="2" p:facing="south" />
</GameScene>

Der <ItemLink id="bettacraft:reservoir" /> fasst **16 Eimer** Wasser. Mit Eimern oder [Rohren](pipes.md) füllen; jedes
Wasser mischt sich (normales Wasser, Umkehrosmose-Wasser, Meerwasser). Rechtsklick zeigt, wie voll er ist und welches Wasser
er hält; ein Komparator liest ihn.

* **Frischwasser für die Steuerung:** am Ende der Frischwasserleitung einer [Wasserwechsel-Steuerung](water_change.md) ist
  er die Quelle für Wasserwechsel und Nachfüllen; die Steuerung holt das Wasser selbst, ohne Pumpe. Mehrere Steuerungen
  dürfen sich einen Vorratsbehälter teilen.
* **Abwasser:** er kann auch das alte Wasser am Ende einer Abwasserleitung aufnehmen. Dafür einen anderen Vorratsbehälter
  als den für das Frischwasser nehmen, sonst mischt sich das Abwasser hinein; ein [Abfluss](drain.md) entsorgt es einfach.
* Der [angestellte Aquarist](../getting_started/hired_aquarist.md) nimmt das Wasser für seine Wasserwechsel aus einem
  Vorratsbehälter, und eine [Sprühanlage](../terrarium/mister.md) füllt sich aus einem daneben.

## Bezug

Herstellung:

<RecipesFor id="bettacraft:reservoir" />
