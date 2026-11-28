---
navigation:
  title: Angestellter Aquarist
  icon: minecraft:emerald
  parent: getting_started.md
  position: 40
item_ids:
  - bettacraft:hired_aquarist
feature: aquaristNpc.enabled
---

# Angestellter Aquarist

![Der angestellte Aquarist an seinem Arbeitsplatz: Zuchtbecken, Kiste, Reservoir und sein Bett](../images/hired_aquarist.png)

Hast du ein paar Aquarien und etwas gezüchtet, kommt ein **Aquarist** vorbei und bietet dir an, deine Becken zu pflegen.
Er ist eine eigene Person (kein Dorfbewohner, nicht der [Aquarianer](aquarist.md), der nur handelt): ein Mann oder eine
Frau mit türkiser Gummischürze, blauen Handschuhen und Gummistiefeln, mit einem Vornamen. Eingestellt wohnt er in einem Bett
nahe einem Zuchtbecken, nimmt jeden Morgen seinen Lohn und **läuft wirklich zu deinen Becken**, um zu füttern, Wasser zu
wechseln, zu putzen, zu schneiden und Jungtiere umzusetzen.

## Wann er kommt

* Jeder Spieler bekommt seinen eigenen Aquaristen. Er kommt, wenn **du** mindestens **3 Becken** besitzt und **etwas
  gezüchtet** hast (ein Zucht-Fortschritt: ein Betta-Laichen, ein Wurf, junge Garnelen, ein Gelege ...), bei Tag, 10 bis 20
  Blöcke von dir entfernt; er läuft zu dir.
* Er bietet seine Arbeit im Chat an, mit zwei Knöpfen **[Einstellen]** und **[Nein, danke]**; ein Rechtsklick auf ihn
  zeigt dasselbe Angebot in seinem Fenster. Er wartet einen Spieltag auf die Antwort.
* **Abgelehnt** (oder ohne Antwort) geht er. Der nächste kommt nach **5 bis 10 Spieltagen und einer weiteren Zucht**.
  Dasselbe gilt, wenn ein angestellter Aquarist geht (kein Lohn, kein Zuhause, entlassen).
* `/bettacraft aquarist status` sagt dir, warum noch keiner gekommen ist.

![Sein Angebot](../images/hired_aquarist_offer.png)

## Zuhause und Lohn

* Er braucht ein **freies Bett** und ein **freies [Zuchtbecken](aquarist.md)** (seinen Arbeitsplatz) im Umkreis von 32
  Blöcken um die Stelle, an der er beim Einstellen steht. Er nimmt sie für sich (kein Dorfbewohner benutzt sie dann).
* **Zuchtbecken, Bett und Kiste zuweisen** (Reiter **Arbeit** seines Menüs): **Zuweisen...** neben Becken, Bett oder Kiste, dann das freie Zuchtbecken
  (ein belegtes wird abgelehnt), das Bett oder die Kiste (auch Fässer und Speicher anderer Mods) rechtsklicken, höchstens 32 Blöcke von seinem Zuchtbecken; **Auto** gibt es zurück, und er sucht wieder
  selbst. Ohne zugewiesene Kiste:
* **Kisten neben dem Zuchtbecken** (an jeder Seite, auch schräg; auch Fässer und Speicher anderer Mods) halten, was er
  braucht: das **Futter** für die Becken, die **Stecklinge**, die er zurückbringt, und seinen **Lohn**.
* Das Wasser für den Wasserwechsel kommt aus einem **Reservoir**, einem Rohr, einem Tank einer anderen Mod oder einer
  Wasserquelle **neben dem Zuchtbecken** (Salzwasser für Salzbecken).
* **Lohn:** jeden Morgen **1 Smaragd**, oder **3 Stück Essen** (Brot, gebratener Fisch ...), wenn kein Smaragd da ist. Der
  erste Tag ist frei. Ohne Lohn arbeitet er **halb so schnell**; nach **3 Tagen** ohne Lohn (oder ohne Bett und
  Zuchtbecken) geht er.
* Nachts schläft er in seinem Bett. Er kann nicht verletzt werden (nur `/kill` entfernt ihn), läuft vor Monstern weg, lässt
  sich nicht schieben, anleinen oder fangen, und nur du (oder ein Operator) kannst sein Menü benutzen. Ein Namensschild
  benennt ihn um (nur deines).

## Seine Aufgaben

Rechtsklick auf ihn: Sein Menü hat fünf Reiter (den Reiter Freund nur bei dem, der Freunde empfiehlt, siehe unten). **Arbeit** zeigt sein Zuchtbecken, Bett und Kiste (mit den Knöpfen zum Zuweisen), den Lohn und die
sechs Aufgaben: ein Klick auf den Namen einer Aufgabe schaltet sie an oder aus, die Zahl stellt ihre Stufe 1 bis 3. **Aufzuchtbecken** und
**Becken** stehen weiter unten:

| Aufgabe | Was er tut | Stufe 1 / 2 / 3 |
|---|---|---|
| Füttern | nimmt Futter aus den Kisten (Lebendfutter zuerst, mehrere Sorten im Wechsel) und füttert jedes Becken; nicht Becken mit eigenem Futterautomaten | einmal / zweimal / dreimal am Tag |
| Wasserwechsel | trägt Wasser aus dem Reservoir am Zuchtbecken heran und wechselt es, wenn der Abstand des [Wasserwechsel-Reglers](../equipment/water_change.md) um ist oder das Wasser schlecht | 15 / 25 / 35 % |
| Algen putzen | kratzt die Algen von der Scheibe | ab 50 / 30 / 15 % Algen |
| Pflanzen schneiden | dünnt Schwimmpflanzen aus und schneidet ausgewachsene Aquarienpflanzen zurück; Pflanzen und Stecklinge kommen in die Kiste | ab 90 / 75 / 60 % Bedeckung, ausgewachsene / fast / halb gewachsene Pflanzen |
| Jungtiere umsetzen | trägt junge Bettas, junge Beifische und Jungfrösche in eins seiner **Aufzuchtbecken** (Reiter Aufzuchtbecken) | 2 / 4 / 8 je Gang |
| Warnungen | sagt dir im Chat, was nicht stimmt (mit einer Karten-Mod auch als Wegpunkt am Becken, wenn dein Wegpunkt-Schalter dort an ist) | kranke Tiere und zu wenig Wasser / auch Temperatur, Wasser, Hunger, Überbesatz / auch die Zuchtwarnungen |

Er pflegt bis zu **16 Becken im Umkreis von 32 Blöcken** um sein Zuchtbecken (die nächsten zuerst), und nur Becken, die
**geladen** sind. Jede Runde holt er am Zuchtbecken, was er braucht, geht von Becken zu Becken und bringt Stecklinge und
übriges Futter zurück. Alles, was er trägt, legt er zurück, wenn er geht; Jungtiere kommen zurück in ihr Becken, wenn das
Aufzuchtbecken sie nicht nimmt (voll, falsches Wasser), oder als gefüllter Fischbeutel in eine Kiste, wenn auch das nicht
geht.

![Sein Menü](../images/hired_aquarist_menu.png)

## Wessen Becken (Mehrspieler)

Er arbeitet nur an Becken, die er anfassen darf; das prüft er vor jeder Arbeit noch einmal:

* an **deinen eigenen** Becken (Besitzer ist, wer das Becken gesetzt hat; ein Becken ohne Besitzer bekommt der nächste
  Spieler, der es öffnet);
* mit **FTB Teams** an den Becken der Mitglieder deiner Gruppe (Server-Config `hiredAquarist.ftbTeams`);
* an Becken anderer Spieler nur, wenn ihr Besitzer sie **freigegeben** hat: der Knopf **A** im [Beckenmenü](../aquarium/menu.md)
  (Besitzer oder Operator) wechselt zwischen „der Aquarist des Besitzers“ (A), „die Aquaristen aller Spieler“ (A+, für
  Freunde, die die Arbeit ihres Aquaristen teilen) und „kein Aquarist“ (A-);
* **nie an gesperrten Becken** (die Sperre des Beckenmenüs) und nie an Becken Fremder.

Sein Menü listet die Becken um seinen Arbeitsplatz (ohne Arbeitsplatz keine) und ob sie dem Arbeitgeber, dem Team gehören, freigegeben, fremd,
gesperrt oder unerwünscht sind.

## Aufzuchtbecken und was er wo tut

* **Mehrere Aufzuchtbecken** (bis 5; Reiter **Aufzuchtbecken**): ein Becken mit den Pfeilen wählen und mit dem **Haken** ganz rechts aufnehmen. Die
  Jungtiere kommen in das **erste** Aufzuchtbecken der Liste, das sie nimmt (Art, Salzgehalt und Platz). Jedes Aufzuchtbecken lässt sich auf Arten
  beschränken (der Knopf in seiner Zeile öffnet eine Liste zum Anhaken; nichts angehakt: alle Arten). Das ✕ nimmt ein Becken aus der Liste.
  Das Aufzuchtbecken einer älteren Version bleibt sein Aufzuchtbecken.
* **Je Becken** (Reiter **Becken**): ein Becken in der Liste anklicken und dann die sechs Aufgaben für dieses Becken an- oder ausschalten.
  Standardmäßig tut er an jedem Becken, an dem er arbeiten darf, alles; ein Becken mit abgeschalteten Aufgaben zeigt „4 von 6 Aufgaben“.

![Das Beckenmenü mit dem Aquaristen-Knopf](../images/hired_aquarist_tank_menu.png)

## Ein Team aus Aquaristen

Du kannst **bis zu 3 Aquaristen** haben (Server-Config `hiredAquarist.maxAquarists`, bis 10; 1 = nur einer). Nur der erste
kommt von selbst. Jeder weitere ist ein **Freund**, den dein Aquarist empfiehlt: der, der am längsten für dich arbeitet
(mit ★ markiert). Der Reiter **Freund** seines Menüs zeigt jede Bedingung mit ✔ / ✘ und wie weit du bist („ein Freund in 3 Tagen“):

* er hat **7 Spieltage ohne Unterbrechung** gearbeitet (Lohn jeden Morgen bezahlt), und seit dem letzten Einstellen sind 7
  Tage vergangen;
* **genug Arbeit**: du besitzt **3 Becken pro Aquarist**, den Freund mitgezählt (der zweite ab 6 Becken, der dritte ab 9);
* **eine weitere Zucht** seit dem letzten Einstellen;
* **kein Ärger** in den letzten 3 Tagen: kein Aquarist des Teams blieb ohne Lohn und kein Becken in ihrer Pflege hatte
  schlechtes Wasser (ein Profi empfiehlt keinen Freund in einen schlecht geführten Betrieb);
* das Team ist nicht voll.

Ist alles erfüllt, sagt er es dir im Chat („Mein Freund ... sucht Arbeit – sprich mich an“) mit einem Knopf, der sein
Menü am Reiter Freund öffnet, und der Reiter leuchtet. **[Einstellen]**: der Freund kommt zu ihm und nimmt sich ein freies
Bett und ein freies Zuchtbecken in der Nähe. Ohne sie wartet er bei dem, der ihn empfohlen hat (5 Tage,
`friendHomeGraceDays`), und arbeitet noch nicht; sein Reiter Arbeit zeigt, was fehlt, so kannst du beides danach bauen.
**[Nein, danke]**: der nächste Freund nach 5 bis 10 Tagen und einer weiteren Zucht. Ist das Team voll, zeigt der Reiter
„Team voll (3/3)“.

* **Keine doppelte Arbeit:** jedes Becken pflegt **genau ein** Aquarist: der, dessen Zuchtbecken am nächsten liegt. Im
  Reiter **Becken** ein Becken anklicken: der Knopf rechts zeigt, wer es pflegt; ein Klick gibt das Becken dem nächsten
  Aquaristen des Teams (nach dem letzten wieder dem nächstgelegenen). Die Liste zeigt die Becken der anderen als „pflegt Lena“.
* **Jeder hat sein Eigenes:** Bett, Zuchtbecken, Kisten, Aufzuchtbecken und Lohn gehören ihm (zugewiesen wie oben oder automatisch).
* Der Reiter **Team** im Menü **jedes** Aquaristen listet alle: Name, Zustand, Becken, Arbeitsplatz.
* Geht der erste (entlassen, unbezahlt, `/kill`), empfiehlt von da an der, der am längsten bei dir ist, die Freunde. Erst
  wenn keiner mehr da ist, kommt wieder ein neuer erster Aquarist von selbst.
* Die Freigaben A / A+ des Beckenmenüs gelten für alle Aquaristen des Besitzers. Jeder Spieler hat sein eigenes Team und Limit.
* `/bettacraft aquarist status` listet dein Team und die Bedingungen des nächsten Freundes.

![Der Reiter Team: alle Aquaristen des Arbeitgebers](../images/hired_aquarist_team.png)

![Der Reiter Freund: die Bedingungen des nächsten Freundes, dann das Angebot](../images/hired_aquarist_friend.png)

## Einstellungen

Config `features.aquaristNpc.enabled` (standardmäßig an). Aus: es kommt kein Aquarist, angestellte warten nur (sie arbeiten
wieder, wenn es an ist), das Beckenmenü zeigt den Knopf nicht. Server-Config `hiredAquarist.*`: nötige Becken
(`minOwnTanks`, 3), die Wartezeit nach einer Ablehnung (`returnDaysMin` 5, `returnDaysMax` 10), wie lange er auf eine
Antwort wartet (`offerDays`, 1), Becken und Umkreis (`maxTanksCared` 16, `workRadius` 32, `homeRadius` 32), der Lohn
(`wage`, `wageEmeralds` 1, `wageFood` 3, `unpaidDaysUntilLeave` 3, `unpaidWorkSpeed` 0,5) und FTB Teams (`ftbTeams`); das
Team (`features.aquaristNpc.team`, standardmäßig an; aus: kein Freund wird empfohlen, die angestellten bleiben):
`maxAquarists` 3, `friendAfterDays` 7, `tanksPerAquarist` 3, `friendCalmDays` 3, `friendHomeGraceDays` 5. Operatoren:
`/bettacraft aquarist arrive [Spieler]` lässt sofort einen kommen, `/bettacraft aquarist friend [Spieler]` lässt den
ersten sofort seinen Freund empfehlen.

![Die vier Gesichter der Aquaristen](../images/hired_aquarist_looks.png)
