---
navigation:
  title: Amphibien und Reptilien
  icon: bettacraft:dart_frog_spawn_egg
  parent: terrarium.md
  position: 60
item_ids:
  - bettacraft:frog_eggs
---

# Amphibien und Reptilien

Neben den Schildkröten leben fünf weitere Terrarientiere in den Becken: zwei Frösche, zwei Geckos und eine Schlange. Sie
brauchen dieselben Klimawerte wie die Schildkröten (Luft, Luftfeuchte, UV, Sonnenplatz, Land), bewegen sich aber auf ihre
eigene Art: die Frösche **hüpfen**, der Hornfrosch **sitzt eingegraben** im Boden, der Kronengecko **klettert am Glas**
und die Kornnatter **schlängelt** sich. Alle reisen in einer [Terrarienbox](../terrarium.md) (Rechtsklick mit einer
leeren <ItemLink id="bettacraft:terrarium_box" /> auf das Tier).

| Tier | Becken | Luft, Luftfeuchte | Futter | Zucht |
|---|---|---|---|---|
| [Pfeilgiftfrösche](dart_frog.md) (drei Arten) | Paludarium, viel Land | 21-28 °C, 75-100 % | Fruchtfliegen | Eier auf Moos, Laub oder Erde; Kaulquappen ins Wasser |
| [Schmuckhornfrosch](horned_frog.md) | feuchtes Terrarium | 23-30 °C, 60-95 % | Heimchen | keine im Becken |
| [Leopardgecko](leopard_gecko.md) | trockenes Terrarium, Sonnenplatz | 24-33 °C, 20-55 % | Heimchen | zwei Eier, Geschlecht nach Temperatur |
| [Kronengecko](crested_gecko.md) | Terrarium mit Höhe | 19-27 °C, 55-95 % | Obst, Heimchen, Fruchtfliegen | zwei Eier |
| [Kornnatter](corn_snake.md) | trockenes Terrarium, Sonnenplatz, 8 Blöcke | 21-31 °C, 35-70 % | ein Beutetier pro Woche | Eier; Winterstarre bei Kälte |

Ihr Futter: [Futterinsekten](feeder_insects.md) (Heimchen und Fruchtfliegen aus Zuchten), weiches Obst für den Kronengecko
und rohes Kaninchen oder Huhn für die Schlange.

## Zucht

Ein ausgewachsenes, gefüttertes Weibchen (zwei Tage), ohne schädliches Problem und mit wenig Stress, und ein ausgewachsenes
Männchen derselben Art legen Eier auf dem Land wie die Schildkröten ([Schildkrötenzucht](turtle_breeding.md)): Das Weibchen
läuft zu einem Nistplatz und legt dort.

* **Pfeilgiftfrösche** legen ein kleines Gelege aus Gallerte mit dunklen Embryonen (3-6 Eier) auf **Moos, Laub oder Erde**
  (`#bettacraft:frog_nest_ground`). Nur Frösche **derselben Art** paaren sich (die drei Arten kreuzen sich nicht). Die Jungen
  schlüpfen als **Kaulquappen** und kommen direkt ins Wasser (ein Paludarium ohne Wasser: sie sterben). Eine Kaulquappe
  schwimmt, frisst Pellets oder Jungfischfutter und wird bei 40 % ihres Wachstums zum Fröschchen; dann klettert sie an Land.
* **Geckos** legen zwei Eier in Sand oder Erde. Beim **Leopardgecko** bestimmt die Temperatur das Geschlecht, mit Weibchen
  an beiden Enden: kühle Nester (26-29 °C) geben **Weibchen**, etwa 32-33 °C meist **Männchen**, sehr heiße Nester (ab
  35 °C) wieder Weibchen. Beim Kronengecko ist das Geschlecht genetisch (halb und halb).
* Die **Kornnatter** legt 4-8 Eier in Sand oder Erde; ihr Geschlecht ist genetisch.
* Der **Hornfrosch** züchtet nicht im Becken: In Gefangenschaft laicht er nur nach einer nachgestellten Regenzeit und mit
  Hormonen.

Die Eier entwickeln sich mit der Wärme des Bodens ([Heizmatten](heat_mat.md)); der Klima-Reiter zeigt sie. Morphe: Ab und zu
zeigt ein Schlüpfling von Leopardgecko, Kronengecko oder Kornnatter einen eigenen Zucht-Morph (4 %; das Logbuch nennt ihn).
Die Besatzgrenzen zählen jedes Jungtier wie die Schlüpflinge der Schildkröten.

## Wild

| Tier | Wo |
|---|---|
| Pfeilgiftfrösche, Kronengecko | auf dem Boden von Dschungeln |
| Hornfrosch | Savannen und trockenes Buschland, selten Sümpfe |
| Leopardgecko | Wüsten und Tafelberge (`#bettacraft:arid_lands`) |
| Kornnatter | Wälder und Ebenen (`#bettacraft:snake_lands`) |

Sie spawnen auf Erde, Sand oder Terrakotta bei Tageslicht (auch unter dem Laub des Dschungels, nicht in Höhlen), höchstens
zwei einer Art in der Nähe; die wilden sind die Wildform (die Morphe sind gezüchtet). Schalter: `features.wildHerps`.

## Einstellungen

Common-Config: `features.herps` (aus: keine kommen ins Becken), `features.herpBreeding`, `features.wildHerps`. Brutgeschwindigkeit,
Tage zwischen den Gelegen und Winterstarre sind die der Schildkröten (`terrarium.*`).
