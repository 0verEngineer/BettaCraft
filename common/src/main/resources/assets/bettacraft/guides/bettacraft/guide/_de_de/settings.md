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
| `care.mode` | NORMAL | NO_DEATH = Bedürfnisse und Krankheit, aber niemand stirbt; DECOR = gar keine Bedürfnisse (siehe Voreinstellungen unter Feature-Schalter) |
| `health.daysUntilSick` | 7 | Tage Hunger oder falsche Temperatur bis krank |
| `health.daysSickUntilDeath` | 14 | Krankheitstage bis zum Tod |
| `health.bothBadMultiplier` | 2 | schnellerer Stress bei zwei schlechten Bedingungen |
| `health.recoveryPerDay` | 2 | geheilte Krankheitstage pro gutem Tag |
| `health.hungryAfterDays` | 1 | Tage nach einer Mahlzeit, bis ein Betta wieder hungrig ist |
| `health.oldAge.enabled` | false | Tod durch Altersschwäche von Bettas und Beifischen (`years` 2, `daysPerYear` 96) |
| `temperature.tauDays` | 0.1 | wie schnell das Wasser seine Temperatur ändert |
| `temperature.toleratedMin`, `toleratedMax` | 24, 30 | Wassertemperaturen, die Bettas vertragen (°C); am besten ist `breeding.minTemp` bis `maxTemp` |
| `breeding.*` | | Temperaturen, Nestbau, Eier, Überlebensrate, Pause und Schaden der Weibchen, Männchen fressen Jungfische, Mutation, Spiegeltraining (`mirrorReadinessBonus` 1,25), Tage, die Lebendfutter einen Fisch konditioniert hält (`conditioningDays` 2) |
| `combat.*` | | `malesFight`, `fightDays`, `predatorAttacksPerDay`, `healPerDay` |
| `food.fryPerItem`, `food.cultureDays` | 20, 1 | Jungfische pro Flasche, Tage für einen Ansatz |
| `stages.*Days` | | Dauer der Lebensstufen |
| `sizes.betta` | NORMAL | wie groß die [Bettas](bettas.md) dargestellt werden, mit ihrer Hitbox: NORMAL oder LARGE (Groß, die Größe vor 1.28); nur das Aussehen, wirkt sofort für alle Spieler |
| `interior.defaultScale` | 16 | 4, 8 oder 16 Innenraum-Blöcke pro Aquarium-Block (neue Aquarien) |
| `interior.substrateCostsBlock` | true | Bodengrund kostet einen Block |
| `interior.maxRenderedBlocks` | 262144 | Limit der Miniatur |
| `interior.leaveSneakTicks` | 60 | wie lange man zum Verlassen schleichen (und nach oben schauen) muss |
| `interior.leaveLookUpDegrees` | 60 | wie steil man beim Schleichen nach oben schauen muss, um hinauszukommen (0 = Schleichen allein) |
| `interior.syncRange`, `preparationBudgetMs` | 96, 8 | Spieler so nah bekommen die Miniatur; Serverzeit pro Tick für den Bau neuer Innenräume |
| `drops.artemiaFromSandChance` | 0.04 | eine Dose Artemia-Zysten aus Sand (eine Dose ist ein Ansatz) |
| `waterQuality.qualityEnabled` | true | Wasserqualität an (Verlust pro Tag 0.01, pro Tier 0.04, pro Überfütterung 0.01, Filter entfernen 0.8, schlecht unter 0.35) |
| `waterQuality.externalFilterBlocks`, `internalFilterBlocks` | 32, 16 | Wasserblöcke, die ein Filter sauber hält |
| `evaporation.evaporationEnabled` | true | offene Aquarien verlieren Wasser (`mbPerBlockPerDay` 20) |
| `waterchange.difficulty` | NORMAL | [Nitrat](aquarium/nitrate.md): OFF, EASY (25 % alle 20 Tage), NORMAL (14 Tage), REALISTIC (7 Tage) |
| `waterchange.nitrateStressFrom`, `nitrateLethalFrom`, `plantUptake`, `referenceFishPerBlock` | 50, 120, 1,2, 0,5 | weniger Zucht und mehr Algen ab, schlechtes Wasser ab, Aufnahme dichter Pflanzen, ein normal besetztes Becken |
| `chemistry.tdsPerDay`, `remineraliserPpm`, `leafBlocks`, `leafHalfLifeDays` | 0.6, 400, 1.0, 7 | [Wasserchemie](aquarium/water_chemistry.md): TDS aus Futter und Abfall pro Tag, TDS eines Aufhärtesalzes je Eimer, Wasserblöcke, die ein Seemandelbaumblatt färbt, Tage, bis die Hälfte seiner Gerbstoffe weg ist |
| `co2Dosing.co2PerBubble`, `co2StressFrom`, `co2LethalFrom`, `co2Breath`, `co2PlantUptake` | 150, 35, 50, 2, 30 | [CO2-Düngung](equipment/co2.md): mg/L am Tag, die eine Blase pro Sekunde einem Wasserblock gibt, CO2, das stresst und das den Tieren schadet, Atmung je Tier, Aufnahme eines dicht bepflanzten Aquariums |
| `co2Dosing.bioCo2Bubbles`, `bioCo2Days`, `cylinderBubbleDays`, `stationTicksPerCylinder`, `stationFe`, `foreignCo2Mb` | 1, 4, 60, 24000, 20, 1000 | Blasen und Tage des Bio-CO2, Tage, die eine Flasche bei einer Blase pro Sekunde reicht, Ticks Brennen für eine Füllung, FE des Kompressors, mB CO2 einer anderen Mod pro Flasche |
| `osmosis.roRate`, `roPumpBoost`, `roPumpFe`, `roWastePerPart`, `roMembraneBuckets`, `roPrefilterBuckets` | 1, 4, 20, 3, 128, 32 | [Osmoseanlage](equipment/reverse_osmosis.md): mB pro Tick, wie viel schneller mit FE, FE pro Tick, Abwasser je Teil, Lebensdauer von Membran und Vorfilter |
| `dosing.calciumPerMl`, `alkalinityPerMl`, `magnesiumPerMl`, `tracePerMl`, `magnesiumDropPerDay`, `traceUsePerDay`, `canisterMl`, `maxMlPerDay` | 2, 0.07, 4, 0.15, 1, 0.04, 500, 100 | [Dosierpumpe](saltwater/dosing_pump.md) und [Riffchemie](saltwater/reef_chemistry.md): was ein mL einem Wasserblock zugibt, Magnesiumverlust und Spurenelement-Verbrauch pro Tag, mL eines Kanisters, höchstens mL pro Tag eines Kanals |
| `pipes.glassPipeRate`, `steelPipeRate`, `pumpRate` | 50, 250, 200 | [Rohre und Wasserpumpe](equipment/pipes.md) in mB pro Tick |
| `pipes.pumpsRequireEnergy`, `pumpFePerTick` | false, 8 | Wasserpumpen, Filter-Pumpenköpfe, Wasserwechsel-Steuerungen und Niveausensoren brauchen FE |
| `crucible.meltTicks`, `fePerTick` | 200, 20 | [Schmelztiegel](equipment/crucible.md): Dauer eines Schmelzvorgangs, FE statt Brennstoff |
| `multiblockFilter.spongeBlocks`, `ceramicBlocks`, `carbonBlocks`, `peatBlocks`, `dirtPerDay`, `maxChambers` | 12, 24, 16, 6, 0,04, 16 | [Multiblock-Filter](equipment/multiblock_filter.md): Wasserblöcke je Medium, wie schnell Medien verschmutzen, höchstens Kammern |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy` | false | Ausrüstung braucht FE |
| `power.heaterFePerBlock`, `coolerFePerBlock`, `filterFePerTick`, `lightFePerTick`, `bufferFe` | 8, 8, 10, 4, 100000 | FE-Verbrauch und -Speicher |
| `foodBreeding.*` | | Larvenbottich (`larvaTubDays` 2, `larvaTubYield` 3) und Artemia-Zuchtbecken (`hatcheryDays` 1, `hatcheryYield` 2); höchstens 16 pro Ansatz |
| `liveFood.*` | | [Tubifex-Zuchtschale](food/tubifex_dish.md) (`tubifexDays` 4, `tubifexRoom` 8, `tubifexConditioning` 2, `poorTubifexStress` 0,5), [Daphnien-Fass](food/daphnia_barrel.md) (`daphniaDays` 3, `daphniaRoom` 16, `crowdedDays` 1, `bottleDaphniaChance` 0,08), [Lebendfutter-Pumpe](food/live_food_pump.md) (`pumpFePerPortion` 200) |
| `foodMaking.*` | | [Futtermaschine](food/food_maker.md): `ticksPerBatch` 200, `springTicks` 24000 (eine volle Feder), `windsToFull` 25 (Kurbelklicks), `makerFePerTick` 10 |
| `winding.*` | | [Kurbel](equipment/crank.md): `ticksPerCharge` 24000 (eine Ladung = ein Tag eines Geräts), `clicksPerCharge` 25 (5 Sekunden), die Ladungen jedes Geräts (`externalFilterCharges` 1, `multiblockFilterCharges` 6, `cabinetCharges` 6, `osmosisCharges`, `waterChangerCharges`, `waterPumpCharges`, `fertilizerMakerCharges` 1) |
| `fertilizer.*` | | [Flüssigdünger](plants/fertilizer.md): `portionWaterBlocks` 4, `growthBoost` 0.5, `plantUsePerDay` 0.5, `fertilizerDecayPerDay` 0.05, `overdoseAt` 1.5, `overdoseAlgae` 3, `pipeUpTo` 1; [Düngermaschine](plants/fertilizer_maker.md) `fertilizerTicksPerBatch` 200, `fertilizerFePerTick` 10 |
| `monitor.*` | | [Monitore](equipment/monitor.md): `range` 32 (Blöcke bis zu den Becken), `fePerBlock` 1 (FE pro Tick und Block, solange eine Strom-Option an ist) |
| `diet.*` | | [Futtervielfalt](food/food_variety.md): `varietyDays` 3, `varietyGroups` 3, `varietyRecovery` 1,5, `poorFoodFedDays` 0,5, `shrimpFoodFactor` 2 |
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
| `foreign.foreignBreedingSpeed`, `foreignHealthSpeed` | 1, 1 | [Tiere anderer Mods](settings/foreign_profiles.md) mit Profil: ihre Zuchtgeschwindigkeit (0 = nie), wie schnell Hunger und Krankheit laufen |
| `guide.giveBook` | ON_FIRST_CATCH_OR_CRAFT | wann Spieler dieses Handbuch bekommen |
| `simulateUnloadedTime` | false | Zeit läuft in nicht geladenen Chunks weiter |

Die **Common-Config** legt Spawn-Gewicht und Gruppengröße der wilden Bettas fest, das Rezept der Fischtüte
(`recipes.fishBag`: AUTO, KELP oder PLASTIC), welche Overlay-Mod die Bettacraft-Zeilen zeigt (`compat.overlayProvider`:
AUTO = nur die erste vorhandene von Jade, WTHIT und The One Probe, damit zwei Overlay-Mods nicht jedes Aquarium doppelt
zeigen; JADE, TOP, WTHIT oder ALL) und die Feature-Schalter (`features.*`, siehe Feature-Schalter unten).
Die Common-Config `sounds.useVanilla` schaltet die Gerätesounds auf Minecrafts eigene zurück; die Client-Config hat eine
Lautstärke je Geräteart (`sound.filterVolume`, `airPumpVolume`, `airStoneVolume`, `flowPumpVolume`, `feederVolume`); das Summen
der Luftpumpen und das Surren der Strömungspumpen sind aus, bis `sound.airPumpSound` bzw. `sound.flowPumpSound` eingeschaltet
wird. Der Client-Befehl `/bettacraft sounds` spielt alle Gerätesounds nacheinander einmal ab, mit ihrem Namen im Chat. Die Server-Config `tank.protectFromExplosionsAndPistons` legt fest, ob der Bruchschutz
auch Explosionen und Kolben abhält. Die **Client-Config** enthält Animations- und Miniatur-Distanz, die Entfernung, bis zu
der die Miniatur diese Pflanzen mit echten Blättern zeigt (`render.miniaturePlantMeshDistance`, 24; weiter weg Pixel,
0 = immer Pixel), die Pflanzen-Optik und die wippenden Wasserpflanzen nur für diesen Spieler (`render.plantMeshes`,
`render.swayingPlants`; beide in den Mod-Einstellungen, auch auf einem Server), das
Glitzern der Wasseroberfläche, das Filtergeräusch, die verbundenen Glastexturen (`render.connectedGlass`) und die Breite
der Kantenlinien (`render.tankEdgeWidth`, in Pixeln zu 1/16 Block, 0,1 bis 1, Standard 0,25; 0 = keine), die Größe des
Namensschilds (`render.signScale`, 0,5- bis 2-fache der Standardhöhe von 0,12 Blöcken), die Entfernung, bis zu der
Namensschild, Schaumnest, Schwimmpflanzen, Kleintiere und Algen gezeichnet werden (`render.tankDetailDistance`, 48), wie
stark farbige Lichter das Wasser tönen (`render.lightTint`, 0 bis 1, Standard 0,6; 0 = die Farbe zeigt sich nur an der
Lampe), die Beinbewegung der Mobs und Haustiere, die im Becken gehen (`render.walkAnimationBoost`: `auto` passt sie an
ihre Größe an, ein Faktor von 0,1 bis 10, 0 = Vanilla), den Verlassen-Hinweis im Innenraum (`hud.interiorLeaveHint`) und die Alarm-Wegpunkte aller Becken auf der eigenen Karte (`map.showAlarmWaypoints`, je Karten-Mod `map.journeyMap`, `map.xaerosMinimap`, `map.ftbChunks`; auch hinter dem Zahnrad im Becken-Menü).

## Feature-Schalter

Jeder Teil von Bettacraft lässt sich in der **Common-Config** unter `features` abschalten (auch in den Mod-Einstellungen),
damit ein Modpack nur behält, was es will. Jede Gruppe hat ihren Schalter `enabled`, ihre Unterschalter liegen daneben
(`features.tankmates.enabled`, `features.tankmates.cichlids`). Ein Unterschalter ist aus, wenn seine Gruppe aus ist, und
manche brauchen eine andere Gruppe (Rifffische und Seepferdchen brauchen die Beifische, Korallen das Meerwasser, jeder
Zucht-Schalter `breeding.enabled`, Schildkröten die Terrarien). Aus heißt:

* **Kein neuer Inhalt:** seine Rezepte (auch der Futtermaschine und des Schmelztiegels), Händler-Angebote, Loot, Kescher-
  und Angelfänge und Wild-Spawns entfallen. Datapacks können dieselbe Bedingung nutzen: `{"type": "bettacraft:feature", "feature": "corals.enabled"}`
  in Rezepten und Loot-Tabellen.
* **Ausgeblendet:** Kreativ-Tab, JEI (mit den Kategorien von Futtermaschine und Schmelztiegel) und dieses Handbuch lassen
  es weg; eine Seite, die man über einen Link erreicht, sagt, dass es aus ist. Ein Client auf einem Server folgt den
  Schaltern des Servers.
* **Seine Logik ruht:** nichts davon wird gelöscht und nichts stirbt. Tiere, Pflanzen, Blöcke und Items bleiben in der Welt
  und warten; wieder an, geht alles weiter, wo es stand.

| Schalter | Aus: was ruht |
|---|---|
| `bettas.enabled` | die Pflege der Bettas (Hunger, Krankheit, Wachstum), Nester und Kämpfe |
| `breeding.enabled` | jede Zucht: Nester, Laichen, Trächtigkeit, Gelege, Junge der Garnelen und Schnecken |
| `interior.enabled` | niemand kann in ein Becken (wer drin ist, kann hinaus); die Pflanzen drinnen wachsen nicht |
| `floatingPlants.enabled`, `aquaticPlants.enabled`, `corals.enabled` | diese Pflanzen wachsen, wuchern und zehren nicht |
| `invertebrates.enabled` | Garnelen, Schnecken und Krebse stehen still (kein Fressen, Altern, Vermehren, Sterben) |
| `algae.enabled` | Algen wachsen nicht und werden nicht abgeweidet |
| `frogs.enabled` | Krallenfrösche bekommen keinen Hunger und brauchen keine Luft |
| `tankmates.enabled` (und jede Gruppe, `saltwater.reefFish`) | ihre Pflege, Schwärme, Kämpfe und Zucht |
| `saltwater.enabled` | der Salzgehalt schadet niemandem |
| `power.enabled` | kein Gerät braucht FE (wie mit allen Strom-Optionen aus) |
| `power.winding` | mit Strom-Optionen laufen Futtermaschine und Düngermaschine nur mit FE, und kein anderes Gerät nimmt die Kurbel |
| `fertilizer.enabled` (`fertilizer.dosing`) | der Dünger wirkt nicht (der Spiegel bleibt), die Düngermaschine steht still (der Dosierer überspringt Düngerkanister) |
| `liveFood.enabled` | Zuchtansätze und Kulturen stehen still |
| `sounds.enabled` | die Gerätesounds schweigen |
| `foreignAnimals.enabled`, `decor.enabled`, `compat.create.enabled` | ihre Unterschalter (Pflege, Zucht, Laub, Contraptions ...) |
| `compat.jei`, `compat.overlays`, `compat.guideme`, `compat.betterFishTanks` | die Anbindung an diese Mod (keine JEI-Seiten, keine Overlay-Zeilen, kein Handbuch-Item, kein Aussetzen in BFT-Becken) |
| `compat.mekanism`, `compat.enderio`, `compat.thermal` | das Edelstahl-Rezept der Maschine dieser Mod |
| `aquaristNpc.enabled` | kein [angestellter Aquarist](getting_started/hired_aquarist.md) kommt; angestellte warten, ohne zu arbeiten |

Jeder Schalter hat in den Mod-Einstellungen seine eigene Zeile mit Erklärung. Eine Config von vor 1.26 wird beim Start von
selbst auf diese Schlüssel umgestellt (die alte Datei bleibt als `bettacraft-common.toml.pre-m33`); nach einer Änderung in der
Datei bringt `/reload` Rezepte und Loot auf den Stand.

### Voreinstellungen (Presets)

Statt jeden Schalter einzeln zu setzen, wählt ein Modpack oder ein Server ein fertiges Profil mit **einem Schlüssel** oben
in der Common-Config: `preset = "SIMPLE"`, `"REALISTIC"` oder `"DECOR"` (`"CUSTOM"` = keins, der Standard). Operatoren
können auch `/bettacraft preset <custom|simple|realistic|decor>` eingeben: Das speichert den Schlüssel und schickt ihn an
alle Spieler; `/bettacraft preset` zeigt die aktive Voreinstellung und jeden Wert, den sie gerade setzt. Eine Voreinstellung
setzt Feature-Schalter hier und Spielwerte in der Server-Config jeder Welt:

* **Einfach:** keine Wasserchemie, kein CO2 und keine Umkehrosmose, kein Wasserwechsel-Zwang, keine Verdunstung, kein
  Strom; Tiere werden später krank und erholen sich schneller, ein breiterer Temperaturbereich, ein sanfterer Salzschock,
  einzelne Schwarmfische haben keinen Stress.
* **Realistisch:** alles an, strenge Werte: Wasserwechsel jede Woche, früher krank, ein engerer Temperaturbereich, die
  Geräte brauchen FE (eine Energie-Mod mit Kabeln und Generatoren ist nötig), Tiere anderer Mods achten auf das Salz,
  Vanilla-Korallen brauchen Meerwasser, tödliches Wasser wird nicht verweigert.
* **Nur Deko:** keine Pflege. `care.mode = DECOR`: kein Tier wird je hungrig oder krank und keins stirbt; keine
  Wasserchemie, Wasserqualität, Wasserwechsel, Verdunstung, kein Strom und keine Algen; keine Zucht (es stirbt ja nichts,
  die Becken würden nur voll), keine Kämpfe.

| Schlüssel | Standard | Einfach | Realistisch | Nur Deko |
|---|---|---|---|---|
| `features.waterChemistry.enabled`, `features.co2.enabled`, `features.osmosis.enabled` | an | aus |  | aus |
| `features.power.enabled` | an | aus |  | aus |
| `features.algae.enabled`, `features.breeding.enabled` | an |  |  | aus |
| `care.mode` | NORMAL |  |  | DECOR |
| `waterchange.difficulty` | NORMAL | aus | REALISTIC | aus |
| `waterQuality.qualityEnabled` | true |  |  | false |
| `evaporation.evaporationEnabled` | true | false |  | false |
| `health.daysUntilSick` / `daysSickUntilDeath` | 7 / 14 | 14 / 28 | 5 / 10 |  |
| `health.hungryAfterDays` / `recoveryPerDay` | 1 / 2 | 2 / 3 |  |  |
| `temperature.toleratedMin` / `toleratedMax` | 24 / 30 | 22 / 32 | 25 / 29 |  |
| `saltwater.salinityShock` | 1.0 | 0.5 |  |  |
| `saltwater.foreignAnimals` | LENIENT |  | REALISTIC |  |
| `saltwater.vanillaCoralsNeedSalt` | false |  | true |  |
| `saltwater.refuseDeadlyWater` | true |  | false |  |
| `power.heaterRequiresEnergy`, `filterRequiresEnergy`, `lightRequiresEnergy`, `aeration.airPumpRequiresEnergy`, `pipes.pumpsRequireEnergy`, `terrarium.misterRequiresEnergy` | false |  | true |  |
| `tankmates.behaviour.shoalStress` | true | false |  |  |
| `frogs.frogStarveDays` | 4 | 8 |  |  |
| `bigAnimalRules.dolphinHungryDays` | 2 | 4 |  |  |
| `terrarium.harmAfterDays` | 1 | 2 |  |  |
| `foreign.foreignHealthSpeed` | 1.0 | 0.5 |  |  |
| `combat.malesFight` | true |  |  | false |
| `invertebrates.bettasHuntShrimp` | true |  |  | false |

* Ein Wert, der noch auf seinem Standard steht, folgt der Voreinstellung; ein in der Datei geänderter Wert gewinnt. Um einen
  Standard zu behalten, den die Voreinstellung ändern würde, trägt man seinen Pfad in `presetKeep` ein:
  `presetKeep = ["features.breeding.enabled"]` ist Nur Deko mit Zucht.
* In die Dateien wird nichts geschrieben: zurück auf `CUSTOM` ist jeder Wert wieder der aus der Datei. Die
  Mod-Einstellungen zeigen die Werte der Dateien, nicht die der Voreinstellung.
* Im laufenden Spiel geändert, folgen die Clients sofort; `/reload` bringt Rezepte und Loot auf den Stand.
* `care.mode` geht auch ohne Voreinstellung: `NO_DEATH` behält die Bedürfnisse und die Krankheit, aber kein Tier in einem
  Becken oder Terrarium stirbt daran, wird von einem anderen gefressen oder stirbt an Schaden, der nicht von einem Spieler
  kommt (es wartet mit einem halben Herz); `DECOR` hat gar keine Bedürfnisse. Ein Spieler und `/kill` töten weiterhin.

## Dieses Handbuch

Das Handbuch braucht die Mod **GuideME**. Jeder Spieler bekommt es einmal, je nach `guide.giveBook`:

* `ON_JOIN`: beim ersten Betreten der Welt,
* `ON_FIRST_CATCH_OR_CRAFT`: mit dem ersten gefangenen Betta oder dem ersten hergestellten Bettacraft-Item,
* `NEVER`: gar nicht.

Ist das Inventar voll, landet das Buch vor deinen Füßen, und 60 Sekunden lang kannst nur du es aufheben. Ein neues
Exemplar: Stelle es aus einem Buch und Betta-Pellets her, oder bitte einen Operator um
`/guideme give <Spieler> bettacraft:guide`.

Ohne GuideME erklärt sich jedes Bettacraft-Item im Tooltip (Umschalt halten) und in JEI.

Auf **Fabric** ist das Handbuch ein Buch einer anderen Mod mit denselben Seiten: **Patchouli** auf Minecraft 1.21.1,
**Modonomicon** auf 1.21.11, 26.1.2 und 26.2 (Patchouli gibt es dafür nicht als Release). Dort erscheinen die Szenen dieses
Handbuchs (Beckenformen, Tiere) als Items, und nach einem Sprachwechsel zeigt das Buch die neue Sprache, sobald du die
Welt neu betrittst.

Auf **Minecraft 26.1.2** (Fabric auch 26.1 und 26.1.1) funktioniert alles wie auf 1.21.1, mit zwei Unterschieden von Minecraft selbst: Die Angebote
des fahrenden Händlers und des Aquarianers sind Daten (`villager_trade`, in den Handels-Tags
`minecraft:wandering_trader/common` / `uncommon` und `bettacraft:aquarist/level_1` ... `level_5`), Datenpakete können also
einzelne Angebote entfernen oder hinzufügen; und Mods ohne 26.1.2-Version (Create, Mekanism, Flux Networks, Industrial
Foregoing, Farmer's Delight, Fintastic, Naturalist, Critters and Companions, Alex's Caves, Cataclysm, Deeper
and Darker, Oh The Biomes We've Gone, EMI) haben dort keine Anbindung. Das FTB-Quests-Kapitel heißt dort `bettacraft.json5`
(FTB Quests 26.1 liest JSON5).

Auf **Minecraft 26.2** funktioniert alles wie auf 26.1.2 (auch dort ist der Handel Daten). Mods ohne 26.2-Version haben dort
keine Anbindung: FTB Quests (daher gibt es kein Quest-Kapitel), KubeJS, Create, Mekanism, Flux Networks, EnderIO, Oritech,
Industrial Foregoing, Refined Storage, LaserIO, Cable Facades, Fintastic, Critters and Companions, The
Undergarden, Ice and Fire, Twilight Forest, Regions Unexplored, Alex's Caves, Cataclysm, Deeper and Darker, Oh The Biomes
We've Gone und EMI. Naturalist gibt es auf 26.2 wieder (NeoForge und Fabric). In einer **friedlichen** Welt gibt es auf 26.2 gar keine
Monster: Ein Monsterglas lässt sich dort weder öffnen noch in ein Terrarium setzen, und ein Monster im Terrarium ist weg, sobald
sein Chunk im friedlichen Modus geladen wird (wie jedes andere Monster).

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
* **Unsere Pflege und Zucht für ihre Tiere:** Guppy, Gurami, Wels (Pleco), kleiner Wels, Elritze, Moony, Kabeljau und
  Lachs von Fintastic, die kleinen Fische von Aquaculture, der Koi von Critters and Companions sowie Barsch, Wels und
  Piranha von Naturalist haben ein Profil: Im Bettacraft-Aquarium bekommen sie Hunger, achten auf das Wasser und vermehren
  sich nach unseren Regeln, ihre eigene Zucht ist dort unterdrückt. Modpacks können für jedes Tier Profile ergänzen, siehe
  [Profile für Tiere anderer Mods](settings/foreign_profiles.md).
* **Create und Create Aeronautics:** Aquarien fahren auf Kolben, Lagern, Seilzügen, Portalkränen, Loren, Zügen und
  Luftschiffen mit, unterwegs gepflegt (auf Luftschiffen schwimmen die Tiere weiter), mit schwappendem Wasser; Creates Rotation treibt Pumpen, Osmoseanlage und
  Futtermaschine an, Display Links zeigen Beckenwerte, Create-Rohre füllen Aquarien, siehe
  [Becken mit Create bewegen](settings/create.md).
* **Applied Energistics 2 und Refined Storage:** Busse, Interfaces, Importer und Exporter bewegen Futter, Zutaten und
  Erzeugnisse über dieselben Item- und Flüssigkeitsanschlüsse wie Trichter und Rohre (Futterautomat, Futtermaschine,
  Schmelztiegel, Kulturen, Osmoseanlage). Für **Autocrafting** ein Verarbeitungsmuster
  kodieren (die JEI-Rezeptübertragung geht für Futtermaschine und Schmelztiegel) und den Pattern Provider oder RS-Autocrafter
  neben die Maschine stellen; er legt die Zutaten in die Eingabefelder (in den Schmelztiegel von der Seite oder von
  oben, nicht von unten), ein Import-Bus oder Importer an der Maschine holt das Erzeugnis zurück.
* **Mekanism:** seine Tanks, Rohre und Maschinen nehmen und geben die Bettacraft-Flüssigkeiten (Osmosewasser,
  Meerwasser, Lebendfutter) wie jede Flüssigkeit. Mekanism hat kein Kohlendioxid-Gas, seine Chemikalien versorgen die
  CO2-Füllstation also nicht; die CO2-Flüssigkeit anderer Mods (`#c:carbon_dioxide`, etwa aus Create: The Factory Must
  Grow) schon.
* **FTB Quests:** ein Quest-Kapitel „Bettacraft“ für Modpacks, **KubeJS:** Events, wenn Tiere geboren werden oder
  sterben und wenn ein Monitor-Alarm auslöst, siehe [Modpacks: Quests und Skripte](settings/modpacks.md).
* **JourneyMap, Xaero's Minimap / World Map, FTB Chunks:** ein Wegpunkt an einem Becken, dessen
  [Monitor-Alarm](equipment/monitor.md) ausgelöst hat, bis er wieder aus ist (nur Besitzer und eingetragene Nutzer).
* **Jede andere Mod mit Wassertieren** (Alex's Caves, Critters and Companions, Living Things, Deeper Darker, The
  Undergarden, Shiny, Sushi Go Crafting, DragN's Just Bettas, …): Ihre kleinen Wassertiere gehen ins Aquarium, große und
  feindliche werden abgelehnt. Siehe [Tiere anderer Mods](settings/other_animals.md).
* **Biome-Mods** (Regions Unexplored, Oh The Biomes We've Gone, Biomes O' Plenty, Terralith, Twilight Forest …): Die
  Wildtiere, der Kescher, das Angeln und die wilden Korallen richten sich nach Biom-Tags von Bettacraft, daher zählen
  Sümpfe, Flüsse, Mangroven, Riffe und üppige Höhlen von Biome-Mods mit. Wildbettas erscheinen in
  `#bettacraft:betta_spawn_biomes` (`#c:is_swamp`).

### Biom-Tags für Modpacks

Biome kommen per Datapack in diese Tags (oder mit `"replace": true` als eigene Liste). Geprüft wird in dieser
Reihenfolge; der erste passende Tag entscheidet.

| Tag | Gewässer | Standard |
|---|---|---|
| `#bettacraft:mangrove_waters` | Mangrovensumpf | `minecraft:mangrove_swamp`, die White Mangrove Marshes von Oh The Biomes We've Gone |
| `#bettacraft:swamp_waters` | Sumpf | `minecraft:swamp`, `#c:is_swamp`, der Sumpf des Twilight Forest |
| `#bettacraft:lush_cave_waters` | Üppige Höhlen (Kescher) | `minecraft:lush_caves`; außerdem jedes Biom, das in `#c:is_lush` und in `#c:is_cave` ist |
| `#bettacraft:cold_waters` | Kalte Seen (wilde Marimo, Nadelsimse, Hornkraut) | `minecraft:frozen_river`, die Taigas und verschneiten Ebenen; außerdem jedes Landbiom in `#c:is_cold/overworld` |
| `#bettacraft:river_waters` | Fluss | `#minecraft:is_river`, der Bach des Twilight Forest |
| `#bettacraft:beach_waters` | Strand | `#minecraft:is_beach` |
| `#bettacraft:warm_ocean_waters` | Warmer Ozean (Riff) | `minecraft:warm_ocean`, das Rocky Reef von Regions Unexplored; außerdem jedes Biom, das in `#c:is_ocean` und in `#c:is_hot` ist |
| `#bettacraft:lukewarm_ocean_waters` | Lauwarmer Ozean | `minecraft:lukewarm_ocean`, `minecraft:deep_lukewarm_ocean` |
| `#bettacraft:jungle_waters` | Dschungelgewässer | `#minecraft:is_jungle`, die Tropics von Regions Unexplored und Biomes O' Plenty |
| `#bettacraft:coral_reef_biomes` | Wilde Korallen (neue Chunks) | `#bettacraft:warm_ocean_waters`; außerdem `#c:is_ocean` und `#c:is_hot` |

Pflanzen werden in `#bettacraft:swamp_waters` und `#bettacraft:betta_spawn_biomes` geangelt. Spawns und Korallen
werden beim Laden der Welt festgelegt, ein geänderter Tag braucht einen Neustart. Einträge anderer Mods sind optional:
Ohne die Mod werden sie übersprungen.

### Wildtiere in der Welt

Die wilden Fische, Frösche und Kleintiere gehören zur Vanilla-Spawngruppe *Wasser-Umgebung*, wie Kabeljau, Lachs und
Tropenfische. Deren Obergrenze liegt bei 20 pro Spieler, und mit vielen Fisch-Mods (Fish of Thieves, Naturalist, …)
füllen die Fische der Ozeane in der Nähe sie oft ganz. Darum hat Bettacraft zusätzlich einen eigenen, sparsamen
Spawn-Durchlauf (Common-Config `features.wildSpawnPass.enabled`, an): alle `wildlife.spawnPassInterval` Ticks (200 = 10 Sekunden)
bekommt jeder Spieler einen Versuch in passendem Wasser 24 bis 48 Blöcke entfernt, solange weniger als
`wildlife.spawnPassPerPlayer` (10) wilde Fische, Frösche und Schildkröten der Mod im Umkreis von 48 Blöcken leben. Die
Kleintiere (Garnelen, Schnecken, Krebse, Einsiedlerkrebse) haben eine eigene Grenze und einen eigenen Versuch pro Durchlauf:
`wildlife.spawnPassCrittersPerPlayer` (12) im Umkreis von 48 Blöcken, so nehmen Fische und Kleintiere einander nie den Platz
weg. Ihr Versuch bevorzugt flaches Wasser (1 bis 6 Blöcke tief) über Sand, Kies oder Stein oder an Wasserpflanzen, in
kleinen Gruppen, damit man sie vom Ufer aus sieht; tiefer Grund bleibt möglich. Beim Spawnen wird die Apfelschnecke nur ein
Viertel so oft gewählt, wie der Kescher sie fängt, damit auch die anderen Tiere des Dschungels vorkommen. Die üblichen
Regeln bleiben: wenige pro 32 Blöcke (`wildlife.wildFishNearby` 6, `wildCrittersNearby` 8), nie in einem Aquarium, und weit weg vom Spieler
verschwinden sie wieder wie Vanilla-Fische. Die Gewichte sind `spawn.wildFishWeight` und `spawn.wildCritterWeight`
(Common-Config).

Alle Tiere der Mod stehen im Entity-Tag `#bettacraft:all_animals`, die von selbst spawnenden in
`#bettacraft:wild_animals`, zum Beispiel `/effect give @e[type=#bettacraft:all_animals,distance=..64] minecraft:glowing 30`.

### Wilde Pflanzen in der Welt

Jede Aquarien- und Schwimmpflanze wächst in neuen Chunks wild (Common-Config `features.aquaticPlants.wildPlants`, an): höchstens ein
Versuch pro Chunk (drei in den üppigen Höhlen), eine Gruppe einer Art am Grund oder ein Fleck Schwimmpflanzen auf ruhigem
Wasser. Die Chance pro Versuch ist 50 % in Dschungelgewässern, 40 % in Sümpfen und Mangroven, 35 % in Flüssen und den
üppigen Höhlen, 15 % in kalten Gewässern, mal `spawn.wildPlantFrequency` (1.0; 0 = keine, bis 10). Der Biom-Modifier
`bettacraft:wild_plants` hängt die Features an die Tags der Gewässer oben, damit die Biome von Biom-Mods mitzählen; welche
Pflanze wo wächst, steht unter [Pflanzen](plants.md).

## Befehle

* `/bettacraft interior leave`: einen Innenraum verlassen.
* Operatoren: `/bettacraft interior resync` (alle Miniaturen neu senden), `/bettacraft interior bench` (Messung),
  `/bettacraft wateranimals` (jedes Wassertier der geladenen Mods und ob Aquarien es nehmen, als Tabelle in
  `bettacraft-water-animals.tsv`).
* Operatoren: `/bettacraft wildlife` zeigt für deine Position das Biom, seine Gewässerart, die Tiere der Mod, die dort
  spawnen, mit ihren Gewichten, unsere Wildtiere in der Nähe, wie voll die Obergrenze der Wasser-Umgebung ist (und welche
  Mods sie füllen) und ob Vanilla oder der eigene Spawn-Durchlauf gerade spawnen kann. `/bettacraft wildlife list [Radius]`
  listet unsere Tiere in der Nähe mit Koordinaten (64 Blöcke; Beckenbewohner nicht mitgezählt),
  `/bettacraft wildlife glow [Radius]` lässt sie 30 Sekunden leuchten.
* `/bettacraft preset` zeigt die aktive Voreinstellung und die Werte, die sie setzt; Operatoren (Stufe 3):
  `/bettacraft preset <Name>` wechselt sie (siehe Voreinstellungen oben).
* Operatoren: `/bettacraft foreign list [Radius]` listet die Tiere aus Vanilla und anderen Mods im angeschauten Becken (oder in
  den Becken in der Nähe) mit ihrem Profil und die Typen anderer Mods ohne Profil ([Profile](settings/foreign_profiles.md)).
* `/bettacraft aquarist hire` / `decline`: die Antwort auf das Angebot eines Aquaristen (die Knöpfe im Chat);
  `/bettacraft aquarist status`: warum noch kein [angestellter Aquarist](getting_started/hired_aquarist.md) gekommen ist.
  Operatoren: `/bettacraft aquarist arrive [Spieler]` lässt sofort einen kommen.

## In diesem Kapitel

<SubPages icons={true} />
