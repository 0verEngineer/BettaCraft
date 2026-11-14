---
navigation:
  title: Einstellungen und andere Mods
  icon: minecraft:comparator
  position: 120
---

# Einstellungen und andere Mods

## Server-Config (pro Welt)

| Option | Standard | |
|---|---|---|
| `tank.maxSizeX/Y/Z` | 16 | größtes Aquarium (bis 64), der umschließende Quader bei freien Formen |
| `tank.freeShapes` | true | jede zusammenhängende Form (L, T, Stufen); false = nur Quader |
| `tank.maxEntities` | 64 | Tiere pro Aquarium |
| `tank.hardCapPerWaterBlock` | 6 | höchstens so viele Adulte pro Wasserblock |
| `tank.overcrowdedPerWaterBlock` | 2 | ab so vielen Adulten pro Wasserblock gibt es Stress |
| `tank.minFillToInsert` | 0.5 | Füllung, ab der Tiere eingesetzt werden dürfen |
| `inhabitants.allow`, `inhabitants.deny` | leer | Tiere, die immer / nie in Aquarien dürfen (`modid:name`, `modid:*`) |
| `inhabitants.maxHitbox` | 0.8 | größtes Tier (Hitbox-Breite und -Höhe in Blöcken) |
| `inhabitants.refuseHostile` | true | keine feindlichen Mobs im Aquarium |
| `inhabitants.tadpoles` | LEAVE_AS_FROG | eine große Kaulquappe hüpft als Frosch hinaus; STAY_TADPOLE = sie bleibt Kaulquappe |
| `inhabitants.outgrown` | BAG_IN_FRONT | ein Tier, das im Aquarium größer als `maxHitbox` geworden ist, wartet in einer Fischtüte davor; KEEP = es bleibt |
| `health.daysUntilSick` | 7 | Tage Hunger oder falsche Temperatur bis krank |
| `health.daysSickUntilDeath` | 14 | Krankheitstage bis zum Tod |
| `health.bothBadMultiplier` | 2 | schnellerer Stress bei zwei schlechten Bedingungen |
| `health.recoveryPerDay` | 2 | geheilte Krankheitstage pro gutem Tag |
| `health.hungryAfterDays` | 1 | Tage nach einer Mahlzeit, bis ein Betta wieder hungrig ist |
| `health.oldAge.enabled` | false | Tod durch Altersschwäche (`years` 2, `daysPerYear` 96) |
| `temperature.tauDays` | 0.1 | wie schnell das Wasser seine Temperatur ändert |
| `temperature.toleratedMin`, `toleratedMax` | 24, 30 | Wassertemperaturen, die Bettas vertragen (°C); am besten ist `breeding.minTemp` bis `maxTemp` |
| `breeding.*` | | Temperaturen, Nestbau, Eier, Überlebensrate, Pause und Schaden der Weibchen, Männchen fressen Jungfische, Mutation, Spiegeltraining (`mirrorReadinessBonus` 1,25), Tage, die Lebendfutter einen Fisch konditioniert hält (`conditioningDays` 2) |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | Jungfische pro Flasche, Tage für einen Ansatz |
| `food.artemiaCystStackSize` | 16 | Dosen Artemia-Zysten pro Stapel: 16, 32 oder 64 (jede behält ihre 16 Anwendungen) |
| `stages.*Days` | | Dauer der Lebensstufen |
| `interior.defaultScale` | 16 | 4, 8 oder 16 Innenraum-Blöcke pro Aquarium-Block (neue Aquarien) |
| `interior.substrateCostsBlock` | true | Bodengrund kostet einen Block |
| `interior.maxRenderedBlocks` | 262144 | Limit der Miniatur |
| `interior.leaveSneakTicks` | 60 | wie lange man zum Verlassen schleichen (und nach oben schauen) muss |
| `interior.leaveLookUpDegrees` | 60 | wie steil man beim Schleichen nach oben schauen muss, um hinauszukommen (0 = Schleichen allein) |
| `interior.syncRange`, `preparationBudgetMs` | 96, 8 | Spieler so nah bekommen die Miniatur; Serverzeit pro Tick für den Bau neuer Innenräume |
| `drops.artemiaFromSandChance` | 0.02 | Artemia-Zysten aus Sand |
| `waterQuality.qualityEnabled` | true | Wasserqualität an (Verlust pro Tag 0.01, pro Tier 0.04, pro Überfütterung 0.01, Filter entfernen 0.8, schlecht unter 0.35) |
| `waterQuality.externalFilterBlocks`, `internalFilterBlocks` | 32, 16 | Wasserblöcke, die ein Filter sauber hält |
| `evaporation.evaporationEnabled` | true | offene Aquarien verlieren Wasser (`mbPerBlockPerDay` 20) |
| `waterchange.difficulty` | NORMAL | [Nitrat](aquarium/nitrate.md): OFF, EASY (25 % alle 20 Tage), NORMAL (14 Tage), REALISTIC (7 Tage) |
| `waterchange.nitrateStressFrom`, `nitrateLethalFrom`, `plantUptake`, `referenceFishPerBlock` | 50, 120, 1,2, 0,5 | weniger Zucht und mehr Algen ab, schlechtes Wasser ab, Aufnahme dichter Pflanzen, ein normal besetztes Becken |
| `chemistry.tdsPerDay`, `remineraliserPpm`, `leafBlocks`, `leafHalfLifeDays` | 1.5, 400, 1.0, 7 | [Wasserchemie](aquarium/water_chemistry.md): TDS aus Futter und Abfall pro Tag, TDS eines Aufhärtesalzes je Eimer, Wasserblöcke, die ein Seemandelbaumblatt färbt, Tage, bis die Hälfte seiner Gerbstoffe weg ist |
| `co2Dosing.co2PerBubble`, `co2StressFrom`, `co2LethalFrom`, `co2Breath`, `co2PlantUptake` | 150, 35, 50, 2, 30 | [CO2-Düngung](equipment/co2.md): mg/L am Tag, die eine Blase pro Sekunde einem Wasserblock gibt, CO2, das stresst und das den Tieren schadet, Atmung je Tier, Aufnahme eines dicht bepflanzten Aquariums |
| `co2Dosing.bioCo2Bubbles`, `bioCo2Days`, `cylinderBubbleDays`, `stationTicksPerCylinder`, `stationFe`, `foreignCo2Mb` | 1, 4, 60, 24000, 20, 1000 | Blasen und Tage des Bio-CO2, Tage, die eine Flasche bei einer Blase pro Sekunde reicht, Ticks Brennen für eine Füllung, FE des Kompressors, mB CO2 einer anderen Mod pro Flasche |
| `osmosis.roRate`, `roPumpBoost`, `roPumpFe`, `roWastePerPart`, `roMembraneBuckets`, `roPrefilterBuckets` | 1, 4, 20, 3, 128, 32 | [Osmoseanlage](equipment/reverse_osmosis.md): mB pro Tick, wie viel schneller mit FE, FE pro Tick, Abwasser je Teil, Lebensdauer von Membran und Vorfilter |
| `pipes.glassPipeRate`, `steelPipeRate`, `pumpRate` | 50, 250, 200 | [Rohre und Wasserpumpe](equipment/pipes.md) in mB pro Tick |
| `pipes.pumpsRequireEnergy`, `pumpFePerTick` | false, 8 | Wasserpumpen, Filter-Pumpenköpfe, Wasserwechsel-Steuerungen und Niveausensoren brauchen FE |
| `crucible.meltTicks`, `fePerTick` | 200, 20 | [Schmelztiegel](equipment/crucible.md): Dauer eines Schmelzvorgangs, FE statt Brennstoff |
| `multiblockFilter.spongeBlocks`, `ceramicBlocks`, `carbonBlocks`, `peatBlocks`, `dirtPerDay`, `maxChambers` | 12, 24, 16, 6, 0,04, 16 | [Multiblock-Filter](equipment/multiblock_filter.md): Wasserblöcke je Medium, wie schnell Medien verschmutzen, höchstens Kammern |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | Ausrüstung braucht FE |
| `power.heaterFePerBlock`, `coolerFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 8, 8, 10, 4, 100000 | FE-Verbrauch und -Speicher |
| `foodBreeding.*` | | Larvenbottich (`larvaTubDays` 2, `larvaTubYield` 3) und Artemia-Zuchtbecken (`hatcheryDays` 1, `hatcheryYield` 2); höchstens 16 pro Ansatz |
| `floatingPlants.growthSpeed`, `maxCoverage`, `denseCoverFrom`, `maxPerTank` | 1, 0.6, 0.5, 256 | [Schwimmpflanzen](plants/floating_plants.md): Wachstum (0 = keins), größte bedeckte Oberfläche, dichte Decke ab, Pflanzen pro Aquarium |
| `floatingPlants.wasteUptake`, `breedingBonus`, `pelletsOnLeaves` | 0.04, 1.15, 0.25 | aufgenommener Abfall, schnelleres Nest darunter, Anteil der Pellets, der auf den Blättern liegen bleibt |
| `floatingPlants.bottleDuckweedChance`, `fishingChance` | 0.15, 0.06 | Wasserlinse in einer Flasche Sumpfwasser, eine Schwimmpflanze beim Angeln im Sumpf (siehe [Was gibt es wo](getting_started/sources.md)) |
| `interiorPlants.plantGrowthSpeed`, `plantWasteUptake`, `plantBreedingBonus`, `hidingSpots` | 1, 0.5, 1.1, 0.6 | [Aquarienpflanzen](plants.md): Wachstum (0 = keins), Abfallaufnahme, Nestbonus im bepflanzten Becken, Verstecke |
| `interiorPlants.maxPlantsPerTank`, `plantsSpread` | 48, true | Pflanzen pro Aquarium, Seitentriebe und Ausläufer |
| `interiorPlants.plantFishingChance`, `bottleCuttingChance`, `mossChance` | 0.05, 0.05, 0.5 | eine Pflanze beim Angeln, ein Ableger im Sumpfwasser, Javamoos von der Schere am Moos |
| `interiorPlants.netPlantShare`, `netPlantsRiver`, `netPlantsJungle`, `netPlantsSwamp` | 0,2, Listen | Anteil der [Kescher](equipment/tank_net.md)-Fänge in Flüssen, Dschungelgewässern und Sümpfen, der eine Pflanze ist, und die Pflanzen je Gewässer (`pflanzen_id=Gewicht`) |
| `interiorPlants.fishingPlantsJungle`, `fishingPlantsRiver`, `fishingPlantsSwamp`, `fishingPlantsMangrove` | Listen | die Pflanzen beim Angeln je Gewässer (`pflanzen_id=Gewicht`; siehe [Was gibt es wo](getting_started/sources.md)) |
| `invertebrates.maxCrittersPerTank` | 200 | Garnelen, Krebse und Schnecken pro Aquarium |
| `invertebrates.shrimpBreeding`, `crayfishBreeding`, `snailBreeding` | 1 | Vermehrungstempo (0 = nie) |
| `invertebrates.bettasHuntShrimp` | true | Bettas jagen junge Garnelen und Krebse |
| `invertebrates.biofilmPerWaterBlock` | 0.03 | Futter, das ein Wasserblock von selbst bildet |
| `invertebrates.netCatchChance`, `netCooldownTicks`, `mudSnailChance` | 0.3, 40, 0.08 | Fangen in offenem Wasser, Schnecken im Schlamm |
| `frogs.frogFoodShareWithBettas`, `frogAirMinutes`, `frogStarveDays` | 0.4, 2, 4 | Frösche: ihr Anteil am Futter, Luft unter Wasser, Tage ohne Futter bis sie hungern |
| `stocking.snailsPerFish`, `shrimpPerFish`, `crayfishPerFish`, `frogsPerFish` | 50, 25, 12, 1 | Kleintiere pro Fisch |
| `stocking.eggStocking`, `larvaStocking`, `fryStocking`, `juvenileStocking`, `subadultStocking` | 0, 0, 0,02, 0,1, 0,3 | wie viel eines Fischs ein Jungtier zählt (Juvenil auch für Jungtiere anderer Mods und junge Kleintiere) |
| `algae.algaeGrowthSpeed`, `cleanerStrength` | 0.25, 1 | Algen am Glas (0 = keine), wie viel die Putzkolonne frisst |
| `tankmates.breeding.tankmateBreedingSpeed`, `clutchSpeed`, `gestationDays` | 1, 1, 3 | [Beifische](tankmates.md): wie bereitwillig sie laichen, wie schnell Eier sich entwickeln, Tragzeit der Lebendgebärenden |
| `tankmates.breeding.maxFryPerClutch`, `breedingBoxCapacity` | 20, 20 | Jungfische, die aus einem Gelege frei schwimmen, Junge im Ablaichkasten |
| `tankmates.breeding.waterChangeCools`, `coolChangeShare`, `coolChangeDrop` | true, 0,25, 1,5 | frisches Wasser kühlt das Becken; der kühle Wasserwechsel, der die Panzerwelse laichen lässt |
| `tankmates.behaviour.fryEatenPerDay`, `eggsEatenPerDay` | 0,5, 1,5 | wie schnell die Erwachsenen Jungfische jagen und Eier fressen (Pflanzen und Mops schützen) |
| `tankmates.behaviour.shoalStress`, `bettaAggression`, `clownLoachSnailsPerDay` | true, 1, 0,5 | Stress im zu kleinen Schwarm, Betta-Männchen gegen Guppy- und Fadenfisch-Männchen, Schnecken je Prachtschmerle |
| `tankmates.behaviour.bottomFoodShare`, `tankmateStockingFactor`, `netFishShare` | 0,6, 1, 0,4 | Futter für die Bodenfische, Besatz-Anteil der Beifische, Anteil Fische im Kescher |
| `saltwater.salinityShock`, `refuseDeadlyWater` | 1, true | [Meerwasserbecken](saltwater.md): wie schnell der falsche Salzgehalt Tieren schadet (0 = nie); Tiere ablehnen, die das Wasser töten würde |
| `saltwater.oceanWaterIsSalty`, `foreignAnimals`, `vanillaCoralsNeedSalt` | true, LENIENT, false | Meerwassereimer aus dem Ozean; Salz und Tiere aus Vanilla / anderen Mods (LENIENT oder REALISTIC); Vanilla-Korallen sterben im Süßwasser |
| `saltwater.skimmerBlocks`, `liveRockBlocks`, `pumpFlowBlocks` | 48, 24, 24 | Wasserblöcke, die ein Eiweißabschäumer und ein Tankblock Lebendgestein im Salzwasser sauber halten, Wasserblöcke, die eine Strömungspumpe bewegt |
| `saltwater.coralGrowthSpeed`, `calciumPerStage`, `calciumPerDose`, `reefNetFishShare` | 1, 40, 200, 0,4 | Wachstum der Korallen, Kalzium, das eine Steinkoralle je Stufe verbraucht und ein Zusatz hinzufügt (mg/L in einem Wasserblock), Anteil Rifffische im Kescher |
| `seahorses.maxFlow`, `needHitchingPosts`, `feedingsPerDay`, `foodShareWithFastFish`, `harassedByFastFish`, `pouchDays` | 0,6, an, 2, 0,35, an, 4 | [Seepferdchen](saltwater/seahorses.md): höchste Strömung, Stress ohne Haltepunkt, Fütterungen pro Tag, Futteranteil neben schnellen Fischen, Doktorfische und Riffbarsche bedrängen sie, Tage in der Bruttasche |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | wann Spieler dieses Handbuch bekommen |
| `simulateUnloadedTime` | false | Zeit läuft in nicht geladenen Chunks weiter |

Die **Common-Config** legt Spawn-Gewicht und Gruppengröße der wilden Bettas fest, das Rezept der Fischtüte
(`recipes.fishBag`: AUTO, KELP oder PLASTIC), welche Overlay-Mod die Bettacraft-Zeilen zeigt (`compat.overlayProvider`:
AUTO = nur die erste vorhandene von Jade, WTHIT und The One Probe, damit zwei Overlay-Mods nicht jedes Aquarium doppelt
zeigen; JADE, TOP, WTHIT oder ALL) und Schalter für einzelne Funktionen (`features.*`): `interiorPathfinding` (Frösche
laufen um die Deko herum, Kleintiere gehen unter Überhänge und in Höhlen, Harnischwelse schwimmen in ihre Laichhöhle; aus
= geradeaus) und `plantMeshes` (die neue Optik der Pflanzen und Korallen im Innenraum und in der Miniatur; aus = die
Blöcke, für alle), seit 1.14.2 `swayingForeignPlants` (Kelp, Seegras, Korallen und Wasserpflanzen anderer Mods wippen in
der Miniatur; der Tag `#bettacraft:swaying_plants`, andere Unterwasserpflanzen werden selbst erkannt: `swaying.autoDetect`,
nie wippen: `swaying.exclude` mit Block-IDs, `#Tags` oder `modid:*`), seit 1.13.2 `smoothSwimming` (Fische wenden im Bogen und neigen sich weich, Dornaugen
schlängeln sich und verstecken sich tagsüber, Harnischwelse stellen sich erst an der Scheibe auf), `greenNeonTetra` (der
Grüne Neon beim Händler und im Kescher), `snailColors` (eigene Farbtöne der Rennschnecken, meist goldene Apfelschnecken
mit Bändern, rote Posthörner), `detailedDecor` (der große Schädel und das große Wrack), `breakProtection` (der Schalter
im Becken-Menü), `clipOnFeeder` und seit 1.13.3 `livebearerGenetics` (die [Guppy- und Endler-Genetik](tankmates/guppy_genetics.md); aus = die alten festen Farbformen) und seit 1.13.4 `stoneCabinets` ([Schränke aus Stein](equipment/cabinet.md); aus = nur Holzschränke herstellbar) und seit 1.14.1 `seahorses` ([Seepferdchen](saltwater/seahorses.md); aus = keine aus Kescher und Händler) und seit 1.15.1 `plantNetCatches` (Aquarienpflanzen aus dem [Kescher](equipment/tank_net.md); aus = Pflanzen nur vom Angeln, aus Flaschen, mit der Schere und vom Händler) und `cabinetOutletSide` (die [Auslass-Seite](equipment/cabinet.md) des Schrankfilters; aus = immer die Rückwand, gleichmäßige Strömung) und seit 1.16.0 `oxygen` ([Sauerstoff](aquarium/oxygen.md)),
`aeration` ([Luftpumpe und Ausströmer](equipment/air_pump.md)), `moreInternalFilters` (Schwamm- und Eckfilter,
Ansaugschutz), `fryIntake` (Motorfilter saugen Jungfische ein), `powerRegulator` ([Leistungsregler](equipment/regulator.md)),
`ledTiers` ([LED-Panel und Hochleistungs-LED](equipment/light.md)) und `lightTimer` (Dimmer und Tageslicht-Timer) und seit 1.17.0 `stainlessSteel` ([Schmelztiegel](equipment/crucible.md)), `pipes` ([Rohre und Wasserpumpe](equipment/pipes.md)), `interiorPipes` ([Bodendurchführung und Innenraum-Rohre](equipment/bulkhead.md)), `multiblockFilter` ([Multiblock-Filter](equipment/multiblock_filter.md)), `autoWaterChange` ([Wasserwechsel-Steuerung](equipment/water_change.md)) und `topOff` ([Niveausensor](equipment/top_off.md)) und seit 1.18.0 `waterChemistry` ([Wasserchemie](aquarium/water_chemistry.md); aus = die Werte stehen still und kein Tier stört sich daran), `co2` ([CO2-Düngung](equipment/co2.md); aus = die Geräte geben kein CO2) und `reverseOsmosis` ([Osmoseanlage](equipment/reverse_osmosis.md); aus = sie läuft nicht).
Die Common-Config `sounds.useVanilla` schaltet die Gerätesounds auf Minecrafts eigene zurück; die Client-Config hat eine
Lautstärke je Geräteart (`sound.filterVolume`, `airPumpVolume`, `airStoneVolume`, `flowPumpVolume`, `feederVolume`). Die Server-Config `tank.protectFromExplosionsAndPistons` legt fest, ob der Bruchschutz
auch Explosionen und Kolben abhält. Die **Client-Config** enthält Animations- und Miniatur-Distanz, die Entfernung, bis zu
der die Miniatur diese Pflanzen mit echten Blättern zeigt (`render.miniaturePlantMeshDistance`, 24; weiter weg Pixel,
0 = immer Pixel), die Pflanzen-Optik und die wippenden Wasserpflanzen nur für diesen Spieler (`render.plantMeshes`,
`render.swayingPlants`; beide in den Mod-Einstellungen, auch auf einem Server), das
Glitzern der Wasseroberfläche, das Filtergeräusch, die verbundenen Glastexturen (`render.connectedGlass`) und die Breite
der Kantenlinien (`render.tankEdgeWidth`, in Pixeln zu 1/16 Block, 0,1 bis 1, Standard 0,25; 0 = keine), die Größe des
Namensschilds (`render.signScale`, 0,5- bis 2-fache der Standardhöhe von 0,12 Blöcken), die Entfernung, bis zu der
Namensschild, Schaumnest, Schwimmpflanzen, Kleintiere und Algen gezeichnet werden (`render.tankDetailDistance`, 48), wie
stark farbige Lichter das Wasser tönen (`render.lightTint`, 0 bis 1, Standard 0,6; 0 = die Farbe zeigt sich nur an der
Lampe), und den Verlassen-Hinweis im Innenraum (`hud.interiorLeaveHint`).

## Dieses Handbuch

Das Handbuch braucht die Mod **GuideME**. Jeder Spieler bekommt es einmal, je nach `guide.giveBook`:

* `ON_JOIN`: beim ersten Betreten der Welt,
* `ON_FIRST_CATCH_OR_CRAFT`: mit dem ersten gefangenen Betta oder dem ersten hergestellten Bettacraft-Item,
* `NEVER`: gar nicht.

Ist das Inventar voll, landet das Buch vor deinen Füßen, und 60 Sekunden lang kannst nur du es aufheben. Ein neues
Exemplar: Stelle es aus einem Buch und Betta-Pellets her, oder bitte einen Operator um
`/guideme give <Spieler> bettacraft:guide`.

Ohne GuideME erklärt sich jedes Bettacraft-Item im Tooltip (Umschalt halten) und in JEI.

## Andere Mods

* **Jade, The One Probe und WTHIT:** (bei mehreren nur das erste, siehe `compat.overlayProvider`) Der Blick auf ein Aquarium zeigt Name, Temperatur, Wasser und Tiere (schleichen
  für die Liste; The One Probe braucht dafür seine Sonde), das Nest, die Filter und Warnungen; der Blick durch das Glas
  auf einen Betta zeigt Typ, Geschlecht, Gesundheit, Hunger, Stufe und Gene. Filter und Schränke zeigen ihre
  Heizstäbe und Kühler und das Aquarium, in das sie pumpen; Aquarien zeigen die Wasserqualität und den gespeicherten
  Strom, Larvenbottiche und Zuchtbecken ihren Ansatz.
* **Energie-Mods:** Jede Mod mit Forge-Energy-Kabeln (FE) kann Aquarien versorgen, wenn ein Server `power.*`
  einschaltet; Wasserleitungen können Aquarien füllen und leeren und den Larvenbottich füllen, Trichter und
  Item-Leitungen funktionieren mit Futterautomat, Schrank, Larvenbottich und Zuchtbecken.
* **JEI:** Info-Seiten für alle Bettacraft-Items.
* **Sodium und Iris:** Aquarium-Wasser und Miniaturen funktionieren mit beiden.
* **Better Fish Tanks:** Seine *Fish Bag* setzt Tiere in Bettacraft-Aquarien ein und nimmt sie über das Menü wieder
  heraus. Umgekehrt passen Bettas in seine Tüte und seine Aquarien; eine gefüllte Bettacraft-Fischtüte oder ein
  Betta-Eimer setzt das Tier per Rechtsklick in ein Better-Fish-Tanks-Aquarium.
* **Fintastic:** Seine Fische und das *Fischnetz* funktionieren in beide Richtungen; sein Fischfutter füttert Bettas,
  Feenkrebse, Daphnien und Ruderfußkrebse sind lebende Beute, und seine Wasserpflanzen (auch Wasserlinsen und
  Lotusblätter) lassen sich im trockenen Innenraum setzen. Arapaima und Quastenflosser sind zu groß für ein Aquarium.
* **Fish of Thieves:** Seine Fische leben in Bettacraft-Aquarien (Battlegill, Wrecker und Devilfish sind Raubfische),
  und ein Betta-Eimer setzt einen Betta auf seine *Fish Plaque*. Regenwürmer sind Lebendfutter.
* **Aquaculture 2:** Seine kleinen Fische sind willkommen, die großen (Thunfisch, Arapaima, Heilbutt, Wels, Karpfen …)
  nicht; der Piranha ist ein Raubfisch. Würmer sind Lebendfutter, seine rohen Fische und Filets ergeben Betta-Pellets.
* **Naturalist:** Barsch, Wels und andere kleine Tiere werden angenommen; Piranha und Anglerfisch sind Raubfische.
* **Jede andere Mod mit Wassertieren** (Alex's Caves, Critters and Companions, Living Things, Deeper Darker, The
  Undergarden, Shiny, Sushi Go Crafting, DragN's Just Bettas, …): Ihre kleinen Wassertiere gehen ins Aquarium, große und
  feindliche werden abgelehnt. Siehe [Tiere anderer Mods](settings/other_animals.md).
* **Sümpfe anderer Mods** (Biomes O' Plenty, Regions Unexplored …): Wildbettas erscheinen in jedem Biom aus
  `#c:is_swamp` über `#bettacraft:betta_spawn_biomes`.

## Befehle

* `/bettacraft interior leave`: einen Innenraum verlassen.
* Operatoren: `/bettacraft interior resync` (alle Miniaturen neu senden), `/bettacraft interior bench` (Messung),
  `/bettacraft wateranimals` (jedes Wassertier der geladenen Mods und ob Aquarien es nehmen, als Tabelle in
  `bettacraft-water-animals.tsv`).

## In diesem Kapitel

<SubPages icons={true} />
