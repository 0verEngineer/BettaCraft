package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockItem;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BettaCraft.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bettacraft"))
            .icon(() -> new ItemStack(ModItems.BETTA_BUCKET.get()))
            .displayItems((params, out) -> {
                // the handbook first (only with GuideME, M6)
                ItemStack book = org.overengineer.bettacraft.progression.GuideBook.createBook();
                if (!book.isEmpty()) out.accept(book);
                // catching: fish bag and a wild betta
                out.accept(ModItems.FISH_BAG.get());
                net.minecraft.util.RandomSource wildRandom = net.minecraft.util.RandomSource.create(7);
                out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                        org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(wildRandom), true, 0)));
                // One tank per glass type of #c:glass_blocks (tags are bound when the tab is built).
                boolean any = false;
                for (Holder<Block> glass : BuiltInRegistries.BLOCK.getTagOrEmpty(ModTags.TANK_GLASS_BLOCKS)) {
                    if (glass.value().asItem() == net.minecraft.world.item.Items.AIR) continue;
                    ItemStack s = new ItemStack(ModItems.TANK.get());
                    TankBlockItem.setGlass(s, BuiltInRegistries.BLOCK.getKey(glass.value()));
                    TankBlockItem.setStyle(s, org.overengineer.bettacraft.tank.TankStyle.TEXTURED);
                    out.accept(s);
                    any = true;
                }
                if (!any) out.accept(ModItems.TANK.get());
                // M11: the clean tank (clear vanilla glass without texture)
                out.accept(TankBlockItem.of(ModItems.TANK.get(), org.overengineer.bettacraft.tank.TankBlockEntity.DEFAULT_GLASS,
                        org.overengineer.bettacraft.tank.TankStyle.CLEAN, 1));
                // equipment
                out.accept(ModItems.HEATER_SMALL.get());
                out.accept(ModItems.HEATER_MEDIUM.get());
                out.accept(ModItems.HEATER_LARGE.get());
                out.accept(ModItems.COOLER_SMALL.get());
                out.accept(ModItems.COOLER_MEDIUM.get());
                out.accept(ModItems.COOLER_LARGE.get());
                out.accept(ModItems.INTERNAL_FILTER.get());
                out.accept(ModItems.EXTERNAL_FILTER.get());
                // cabinets: one per vanilla planks (modded planks work the same, see the cabinet recipe)
                for (Holder<net.minecraft.world.item.Item> planks : BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.ItemTags.PLANKS)) {
                    if (!(planks.value() instanceof net.minecraft.world.item.BlockItem bi)) continue;
                    var id = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                    if (!"minecraft".equals(id.getNamespace())) continue;
                    out.accept(org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), id));
                }
                // M21.7: a selection of stone cabinets (every stone with its own slabs works, see the stone cabinet recipe)
                if (org.overengineer.bettacraft.config.CommonConfig.stoneCabinets()) {
                    for (var stone : org.overengineer.bettacraft.equipment.CabinetStones.SELECTION) {
                        if (!(stone instanceof net.minecraft.world.item.BlockItem bi)) continue;
                        out.accept(org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()),
                                BuiltInRegistries.BLOCK.getKey(bi.getBlock())));
                    }
                }
                out.accept(ModItems.FEEDER.get());
                out.accept(ModItems.CLIP_FEEDER.get());
                out.accept(ModItems.TANK_LIGHT.get());
                // M18: the coloured lights
                for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) {
                    if (c != net.minecraft.world.item.DyeColor.WHITE) out.accept(org.overengineer.bettacraft.equipment.TankLightItem.of(ModItems.TANK_LIGHT.get(), c, 1));
                }
                // M23: the stronger lights, aeration and the other internal filters
                for (var led : java.util.List.of(ModItems.LED_PANEL, ModItems.HIGH_POWER_LED)) {
                    out.accept(led.get());
                    for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) {
                        if (c != net.minecraft.world.item.DyeColor.WHITE) out.accept(org.overengineer.bettacraft.equipment.TankLightItem.of(led.get(), c, 1));
                    }
                }
                out.accept(ModItems.AIR_PUMP.get());
                out.accept(ModItems.AIR_HOSE.get());
                out.accept(ModItems.AIR_STONE.get());
                out.accept(ModItems.SPONGE_FILTER.get());
                out.accept(ModItems.CORNER_FILTER.get());
                out.accept(ModItems.INTAKE_SPONGE.get());
                // M24: stainless steel, pipes, the multiblock filter and the water devices
                for (var it : java.util.List.of(ModItems.STAINLESS_STEEL_INGOT, ModItems.CRUCIBLE, ModItems.GLASS_PIPE, ModItems.STAINLESS_STEEL_PIPE,
                        ModItems.WATER_PUMP, ModItems.BULKHEAD, ModItems.INTERIOR_PIPE, ModItems.INTAKE_STRAINER, ModItems.LILY_PIPE, ModItems.NOZZLE,
                        ModItems.SPRAY_BAR, ModItems.FILTER_CHAMBER, ModItems.FILTER_PUMP_HEAD, ModItems.FILTER_INLET, ModItems.FILTER_OUTLET,
                        ModItems.FILTER_FOAM, ModItems.CERAMIC_RINGS, ModItems.ACTIVATED_CARBON, ModItems.FILTER_PEAT, ModItems.RESERVOIR,
                        ModItems.LEVEL_SENSOR, ModItems.WATER_CHANGE_CONTROLLER)) out.accept(it.get());
                // M25: water chemistry
                for (var it : java.util.List.of(ModItems.TDS_METER, ModItems.PH_TEST_KIT, ModItems.REMINERALISING_SALT, ModItems.ALMOND_LEAF)) out.accept(it.get());
                // M25 part b: CO2 and reverse osmosis
                for (var it : java.util.List.of(ModItems.YEAST, ModItems.BIO_CO2, ModItems.CO2_STATION, ModItems.CO2_SOLENOID, ModItems.RO_UNIT,
                        ModItems.RO_MEMBRANE, ModItems.RO_PREFILTER, ModItems.RO_WATER_BUCKET)) out.accept(it.get());
                // M25 part c: reef dosing
                for (var it : java.util.List.of(ModItems.DOSING_PUMP, ModItems.DOSING_CANISTER, ModItems.CALCIUM_CANISTER, ModItems.ALKALINITY_CANISTER,
                        ModItems.MAGNESIUM_CANISTER, ModItems.TRACE_CANISTER)) out.accept(it.get());
                out.accept(ModItems.CO2_CYLINDER.get());
                ItemStack fullCylinder = new ItemStack(ModItems.CO2_CYLINDER.get());
                org.overengineer.bettacraft.co2.Co2CylinderItem.setGas(fullCylinder, 1e9F);
                out.accept(fullCylinder);
                out.accept(ModItems.TANK_LID.get());
                out.accept(ModItems.MIRROR_STICK.get());
                out.accept(ModItems.AQUARIUM_NET.get());
                out.accept(ModItems.ALGAE_SCRAPER.get());
                // floating plants (M12)
                for (var p : ModItems.FLOATING_PLANTS.values()) out.accept(p.get());
                for (var p : ModItems.AQUATIC_PLANTS.values()) out.accept(p.get());
                // food chain (in crafting order)
                out.accept(ModItems.SWAMP_WATER_BOTTLE.get());
                out.accept(ModItems.MOSQUITO_LARVAE.get());
                out.accept(ModItems.BETTA_PELLETS.get());
                out.accept(ModItems.SALT_WATER_BOTTLE.get());
                out.accept(ModItems.ARTEMIA_CYSTS.get());
                out.accept(ModItems.INFUSORIA_CULTURE.get());
                out.accept(ModItems.INFUSORIA.get());
                out.accept(ModItems.ARTEMIA_CULTURE.get());
                out.accept(ModItems.ARTEMIA_NAUPLII.get());
                out.accept(ModItems.LARVA_TUB.get());
                out.accept(ModItems.ARTEMIA_HATCHERY.get());
                // M20: salt water and the reef technique
                out.accept(ModItems.SEA_WATER_BUCKET.get());
                out.accept(ModItems.SEA_SALT.get());
                out.accept(ModItems.CALCIUM_ADDITIVE.get());
                out.accept(ModItems.PROTEIN_SKIMMER.get());
                out.accept(ModItems.FLOW_PUMP.get());
                out.accept(ModItems.REEF_ROCK.get());
                out.accept(ModItems.LIVE_ROCK.get());
                // M22.3: the magnifier, the wild corals of the reefs
                out.accept(ModItems.MAGNIFIER.get());
                for (var c : ModItems.WILD_CORALS.values()) out.accept(c.get());
                // decoration
                for (var d : ModItems.DECOR.values()) out.accept(d.get());
                // one bucket per fin form (long-finned tails and the plakat forms), then one per named variety (its example fish)
                net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(42);
                for (String length : java.util.List.of(org.overengineer.bettacraft.genetics.BettaGenome.LONG, org.overengineer.bettacraft.genetics.BettaGenome.SHORT)) {
                    for (String shape : org.overengineer.bettacraft.genetics.BettaGenome.SHAPES) {
                        var g = org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(r)
                                .with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM, new org.overengineer.bettacraft.genetics.BettaGenotype.AllelePair(shape, shape))
                                .with(org.overengineer.bettacraft.genetics.BettaLoci.FIN_LENGTH, new org.overengineer.bettacraft.genetics.BettaGenotype.AllelePair(length, length));
                        out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                                org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0)));
                    }
                }
                for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
                    if (v.example().isEmpty()) continue;
                    var ph = org.overengineer.bettacraft.genetics.BettaGenome.of(v.example(), org.overengineer.bettacraft.genetics.BettaGenome.DEFAULT);
                    out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                            org.overengineer.bettacraft.entity.betta.BettaData.create(ph, false, 0)));
                }
                out.accept(ModItems.BETTA_SPAWN_EGG.get());
                // M14: small animals in fish bags (one bag per species and colour), the frog
                java.util.random.RandomGenerator cr = new java.util.Random(14);
                for (var sp : org.overengineer.bettacraft.critters.CritterSpecies.values()) {
                    out.accept(org.overengineer.bettacraft.critters.TankCritters.bagOf(sp, sp.breeds() ? 2 : 1,
                            () -> org.overengineer.bettacraft.critters.CritterGenes.WILD, cr, true));
                    for (int l = 0; l < sp.loci().length; l++) {
                        int locus = l;
                        out.accept(org.overengineer.bettacraft.critters.TankCritters.bagOf(sp, 2,
                                () -> org.overengineer.bettacraft.critters.CritterGenes.pure(locus, 0.5F), cr, true));
                    }
                }
                out.accept(org.overengineer.bettacraft.critters.TankCritters.bagOf(org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA, 2,
                        () -> org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.9F), cr, true)); // Painted Fire Red
                // M22.3: shrimp in a plain bucket
                out.accept(org.overengineer.bettacraft.containers.AnimalContainers.critterBucket(org.overengineer.bettacraft.critters.TankCritters.bagOf(
                        org.overengineer.bettacraft.critters.CritterSpecies.NEOCARIDINA, 10, () -> org.overengineer.bettacraft.critters.CritterGenes.pure(0, 0.4F), cr, true)
                        .get(org.overengineer.bettacraft.registry.ModDataComponents.CRITTERS.get())));
                out.accept(ModItems.FROG_BUCKET.get());
                out.accept(ModItems.FROG_SPAWN_EGG.get());
                // M19: tankmates (a shoal or a pair in a fish bag, every variety; bucket and spawn egg), their breeding items
                out.accept(ModItems.BREEDING_BOX.get());
                out.accept(ModItems.FRY_POWDER.get());
                out.accept(ModItems.ALGAE_WAFER.get());
                out.accept(ModItems.SPAWNING_CAVE.get());
                out.accept(ModItems.SPAWNING_MOP.get());
                java.util.random.RandomGenerator tr = new java.util.Random(19);
                for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
                    int n = sp.shoaling() ? Math.max(6, sp.shoal()) : 2;
                    if (org.overengineer.bettacraft.tankmates.TankmateGenes.livebearer(sp) && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) {
                        // M21.6: a pair of every named strain
                        for (var st : org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(sp == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY)) {
                            out.accept(org.overengineer.bettacraft.tankmates.TankmateItems.groupBag(sp, 2,
                                    () -> org.overengineer.bettacraft.tankmates.TankmateGenes.strain(sp, st), tr, true));
                        }
                    } else {
                        for (String v : sp.varieties()) {
                            out.accept(org.overengineer.bettacraft.tankmates.TankmateItems.groupBag(sp, n,
                                    () -> org.overengineer.bettacraft.tankmates.TankmateGenes.pure(sp, v), tr, true));
                        }
                    }
                    out.accept(org.overengineer.bettacraft.tankmates.TankmateBucketItem.withFish(new ItemStack(ModItems.TANKMATE_BUCKETS.get(sp).get()), sp,
                            org.overengineer.bettacraft.tankmates.TankmateGenes.wild(sp), false, org.overengineer.bettacraft.entity.betta.LifeStage.ADULT));
                    out.accept(ModItems.TANKMATE_SPAWN_EGGS.get(sp).get());
                }
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
