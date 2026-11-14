---
navigation:
  title: Lebendfutter züchten
  icon: bettacraft:larva_tub
  parent: food.md
  position: 20
item_ids:
  - bettacraft:larva_tub
  - bettacraft:artemia_hatchery
---

# Lebendfutter züchten

![Ein Artemia-Zuchtbecken mit Nauplien und ein Larvenbottich mit Mückenlarven](../images/live_food.png)

Statt jede Portion aus einer Flasche auszubrüten, kannst du Lebendfutter in zwei Blöcken züchten. Beide funktionieren mit
Trichtern und Leitungen: Zutaten kommen von oben oder von der Seite hinein, ein Trichter darunter holt das Futter heraus,
ein Komparator zeigt, wie voll es ist.

## Larvenbottich

<ItemImage id="bettacraft:larva_tub" scale="2" />

Der <ItemLink id="bettacraft:larva_tub" /> züchtet [Mückenlarven](../food.md) unter **freiem Himmel**.

1. Mit einem **Wassereimer** füllen (fasst 1000 mB, Leitungen können ihn ebenfalls füllen).
2. **Organisches Material** hineingeben: Laub, Weizen, verrottetes Fleisch oder Knochenmehl
   (`#bettacraft:larva_tub_food`). Beim Schleichen kommt ein ganzer Stapel hinein.
3. Nach etwa **2 Ingame-Tagen** ergibt ein Ansatz 3 Mückenlarven und verbraucht ein Stück organisches Material und
   250 mB Wasser. **Nachts** geht es 1,5-mal so schnell, im **Sumpf** 1,3-mal und bei **Regen** 1,3-mal.
4. Mit einer **Glasflasche** ernten (Rechtsklick; die Flasche bleibt leer) oder einen Trichter darunter die Larven
   holen lassen.

<RecipeFor id="bettacraft:larva_tub" />

## Artemia-Zuchtbecken

<ItemImage id="bettacraft:artemia_hatchery" scale="2" />

Das <ItemLink id="bettacraft:artemia_hatchery" /> brütet [Artemia-Nauplien](fry_food.md) für Jungfische aus. Es braucht
**Licht** (Stufe 9 oder mehr).

1. **Salzwasser-Flaschen** hineingießen (je 250 mB, bis zu 1000 mB). Die leeren Flaschen bleiben im Zuchtbecken.
2. **Artemia-Eier** hineinlegen; jeder Ansatz verbraucht eine ihrer 16 Anwendungen.
   Aus einem Stapel öffnet das Zuchtbecken immer eine Dose und braucht sie auf, bevor es die nächste öffnet (beim Abbauen
   fällt die offene Dose mit ihren restlichen Anwendungen heraus).
3. Jede Flasche Nauplien braucht eine **Glasflasche**: mit Glasflaschen rechtsklicken oder per Trichter hineingeben.
4. Nach **1 Ingame-Tag** ergibt ein Ansatz 2 Flaschen Nauplien. Mit leerer Hand oder einem Trichter darunter
   herausnehmen.

<RecipeFor id="bettacraft:artemia_hatchery" />

Ein Rechtsklick mit leerer Hand zeigt den Zustand (Wasser, Zutat, Ansatz, was fehlt); Jade, The One Probe und WTHIT
zeigen dasselbe. Zeiten und Mengen lassen sich in der Server-Config ändern (`foodBreeding.*`).
