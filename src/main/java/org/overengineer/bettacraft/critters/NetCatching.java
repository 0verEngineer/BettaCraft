package org.overengineer.bettacraft.critters;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * Catching with the aquarium net in open water (M14): a short cooldown, a little wear, and with a chance something alive
 * in the net. What depends on the biome and the water: rivers (shrimp, ramshorn and bladder snails), swamps (ramshorn,
 * bladder and trumpet snails, rarely a dwarf clawed frog), mangrove swamps (nerites, rarely the red nerite), beaches
 * (nerites in the brackish water), jungle water (apple snails, rarely a frog or a wild dwarf crayfish), lush caves and
 * deep cave water (trumpet snails); assassin snails are a rare catch in several of them. Plants nearby make shrimp and
 * ramshorns likelier, sand or stone at the bottom trumpet snails and nerites. The catch goes into an empty fish bag.
 */
public final class NetCatching {
    /** M20: WARM_OCEAN (the coral reefs) and LUKEWARM_OCEAN for the reef animals. */
    public enum Water { RIVER, SWAMP, MANGROVE, BEACH, JUNGLE, CAVE, WARM_OCEAN, LUKEWARM_OCEAN, NONE }

    /** One entry of the table: a species (null = dwarf clawed frog) and its weight. */
    public record Catch(@Nullable CritterSpecies species, double weight) {}

    private NetCatching() {}

    public static RandomGenerator random(Level level) {
        var r = level.getRandom();
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return r.nextLong();
            }

            @Override
            public double nextGaussian() {
                return r.nextGaussian();
            }
        };
    }

    /** The kind of water at {@code pos}. */
    public static Water water(ServerLevel level, BlockPos pos) {
        return water(level.getBiome(pos), pos.getY() < level.getSeaLevel() - 8 && !level.canSeeSky(pos.above()));
    }

    /**
     * The kind of water of a biome (M25.1: by the biome tags of {@link org.overengineer.bettacraft.world.BiomeWaters},
     * also the biomes of biome mods); {@code deep}: deep under ground without sky, cave water where no biome water is.
     */
    public static Water water(Holder<Biome> b, boolean deep) {
        Water w = org.overengineer.bettacraft.world.BiomeWaters.water(b, true);
        return w == Water.NONE && deep ? Water.CAVE : w;
    }

    /** What can be caught in this water (weights). */
    public static List<Catch> table(Water w, boolean plants, boolean hardBottom, boolean lush) {
        List<Catch> out = new ArrayList<>();
        double p = plants ? 1.5 : 1.0, hard = hardBottom ? 2.0 : 1.0, ner = hardBottom ? 1.3 : 1.0;
        switch (w) {
            case RIVER -> {
                out.add(new Catch(CritterSpecies.NEOCARIDINA, 40 * p));
                out.add(new Catch(CritterSpecies.RAMSHORN, 30 * p));
                out.add(new Catch(CritterSpecies.BLADDER_SNAIL, 25 * p));
                out.add(new Catch(CritterSpecies.TRUMPET_SNAIL, 4 * hard));
                out.add(new Catch(CritterSpecies.ASSASSIN_SNAIL, 3));
                // M29 part d: the marbled crayfish, set free in rivers and lakes (rare)
                if (CritterSpecies.MARBLED_CRAYFISH.available()) out.add(new Catch(CritterSpecies.MARBLED_CRAYFISH, 2));
                // M25 part c: wild bee shrimp in soft, plant-rich streams (rare; rarer over sand and stone)
                if (CritterSpecies.CARIDINA.available()) out.add(new Catch(CritterSpecies.CARIDINA, (hardBottom ? 1.5 : 5) * p));
            }
            case SWAMP -> {
                out.add(new Catch(CritterSpecies.RAMSHORN, 30 * p));
                out.add(new Catch(CritterSpecies.BLADDER_SNAIL, 30 * p));
                out.add(new Catch(CritterSpecies.TRUMPET_SNAIL, 25 * hard));
                out.add(new Catch(null, 5));
                // M29 part d: the Florida crayfish lives in the swamps of Florida (rare)
                if (CritterSpecies.FLORIDA_CRAYFISH.available()) out.add(new Catch(CritterSpecies.FLORIDA_CRAYFISH, 4 * p));
            }
            case MANGROVE -> {
                out.add(new Catch(CritterSpecies.ZEBRA_NERITE, 25 * ner));
                out.add(new Catch(CritterSpecies.HORNED_NERITE, 20 * ner));
                out.add(new Catch(CritterSpecies.OLIVE_NERITE, 25 * ner));
                out.add(new Catch(CritterSpecies.RED_NERITE, 5));
                if (CritterSpecies.YELLOW_NERITE.available()) out.add(new Catch(CritterSpecies.YELLOW_NERITE, 4)); // M21.5
                out.add(new Catch(CritterSpecies.ASSASSIN_SNAIL, 4));
            }
            case BEACH -> {
                out.add(new Catch(CritterSpecies.ZEBRA_NERITE, 30 * ner));
                out.add(new Catch(CritterSpecies.HORNED_NERITE, 25 * ner));
                out.add(new Catch(CritterSpecies.OLIVE_NERITE, 30 * ner));
                out.add(new Catch(CritterSpecies.HERMIT_CRAB, 15 * ner)); // M20
            }
            case WARM_OCEAN -> {
                out.add(new Catch(CritterSpecies.TURBO_SNAIL, 30 * ner));
                out.add(new Catch(CritterSpecies.HERMIT_CRAB, 30 * ner));
                out.add(new Catch(CritterSpecies.CLEANER_SHRIMP, 20));
            }
            case LUKEWARM_OCEAN -> {
                out.add(new Catch(CritterSpecies.TURBO_SNAIL, 30 * ner));
                out.add(new Catch(CritterSpecies.HERMIT_CRAB, 35 * ner));
                out.add(new Catch(CritterSpecies.CLEANER_SHRIMP, 5));
            }
            case JUNGLE -> {
                out.add(new Catch(CritterSpecies.APPLE_SNAIL, 30));
                out.add(new Catch(null, 8));
                out.add(new Catch(CritterSpecies.DWARF_CRAYFISH, 5));
                // M25 part c: the animals of the ancient lakes of Sulawesi over stone and sand (rare)
                if (CritterSpecies.SULAWESI_SHRIMP.available()) {
                    out.add(new Catch(CritterSpecies.SULAWESI_SHRIMP, 2 * hard));
                    out.add(new Catch(CritterSpecies.TYLOMELANIA, 2.5 * hard));
                }
            }
            case CAVE -> {
                out.add(new Catch(CritterSpecies.TRUMPET_SNAIL, 30 * hard));
                if (lush) out.add(new Catch(CritterSpecies.RAMSHORN, 10 * p));
                out.add(new Catch(CritterSpecies.ASSASSIN_SNAIL, 4));
            }
            default -> {}
        }
        return out;
    }

    /** M19: the tankmates native to this kind of water (rough analogues of their regions) with their weights. */
    public static java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, Double> fishTable(Water w) {
        java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, Double> out = new java.util.LinkedHashMap<>();
        org.overengineer.bettacraft.tankmates.TankmateSpecies.Water fw = switch (w) {
            case RIVER -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.RIVER;
            case SWAMP -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.SWAMP;
            case JUNGLE -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.JUNGLE;
            case MANGROVE -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.MANGROVE;
            case WARM_OCEAN -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.WARM_OCEAN;
            case LUKEWARM_OCEAN -> org.overengineer.bettacraft.tankmates.TankmateSpecies.Water.LUKEWARM_OCEAN;
            default -> null;
        };
        if (fw == null) return out;
        for (var s : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
            if (!s.available()) continue; // M21.5
            double wt = s.nativeWeight(fw);
            if (wt > 0) out.put(s, wt);
        }
        return out;
    }

    @Nullable
    public static org.overengineer.bettacraft.tankmates.TankmateSpecies rollFish(java.util.Map<org.overengineer.bettacraft.tankmates.TankmateSpecies, Double> table,
                                                                                 RandomGenerator rnd) {
        double total = 0;
        for (double v : table.values()) total += v;
        if (total <= 0) return null;
        double x = rnd.nextDouble() * total;
        for (var e : table.entrySet()) {
            x -= e.getValue();
            if (x < 0) return e.getKey();
        }
        return table.keySet().iterator().next();
    }

    @Nullable
    public static Catch roll(List<Catch> table, RandomGenerator rnd) {
        double total = 0;
        for (Catch c : table) total += c.weight();
        if (total <= 0) return null;
        double x = rnd.nextDouble() * total;
        for (Catch c : table) {
            x -= c.weight();
            if (x < 0) return c;
        }
        return table.getLast();
    }

    /** Wild genes: the colour lines are hidden in a few animals (bred out later). */
    public static CritterGenes wildGenes(CritterSpecies s, RandomGenerator rnd) {
        boolean colors = org.overengineer.bettacraft.config.CommonConfig.snailColors();
        if (colors && s == CritterSpecies.APPLE_SNAIL) {
            // M21.5: like in the trade, nearly every apple snail is gold; wild brown is rare, now and then another colour
            double x = rnd.nextDouble();
            if (x < 0.7) return CritterGenes.pure(0, 0.3F + rnd.nextFloat() * 0.25F);
            if (x < 0.88) return CritterGenes.wild(s.loci().length, 0.15, rnd);
            return CritterGenes.pure(1 + rnd.nextInt(3), 0.3F);
        }
        double carrier = switch (s) {
            case NEOCARIDINA -> 0.06;
            case CARIDINA -> 0.05; // M25 part c
            case DWARF_CRAYFISH -> 0.15;
            case FLORIDA_CRAYFISH -> 0.12; // M29 part d: wild ones now and then carry blue or orange
            case RAMSHORN -> 0.2;
            case APPLE_SNAIL -> 0.08;
            default -> 0;
        };
        // without M21.5's snail colours no ramshorn carries the new red
        int loci = !colors && s == CritterSpecies.RAMSHORN ? 2 : s.loci().length;
        return CritterGenes.wild(loci, carrier, rnd);
    }

    /** The filled bag for a catch (a frog, or 1-3 small animals). */
    public static ItemStack catchItem(ServerLevel level, Catch c, RandomGenerator rnd) {
        if (c.species() == null) {
            var frog = ModEntities.DWARF_FROG.get().create(level);
            if (frog == null) return ItemStack.EMPTY;
            ItemStack bag = FishBagItem.fillWith(frog);
            frog.discard();
            return bag;
        }
        CritterSpecies s = c.species();
        int n = switch (s.group()) {
            case SHRIMP -> 1 + rnd.nextInt(3);
            case CRAYFISH -> 1;
            case SNAIL -> s == CritterSpecies.APPLE_SNAIL || s == CritterSpecies.ASSASSIN_SNAIL || s == CritterSpecies.RED_NERITE
                    || s == CritterSpecies.YELLOW_NERITE || s == CritterSpecies.TYLOMELANIA ? 1 : 1 + rnd.nextInt(2);
        };
        return TankCritters.bagOf(s, n, () -> org.overengineer.bettacraft.world.Wildlife.critter(s, rnd), rnd, true); // M22.3: rarely a colour
    }

    /** Plants (seagrass, kelp, lily pads, ...) in the water around {@code pos}. */
    public static boolean plantsNear(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 1, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT) || s.is(Blocks.LILY_PAD)
                    || s.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock) return true;
        }
        return false;
    }

    /** Sand, gravel or stone at the bottom of the water. */
    public static boolean hardBottom(Level level, BlockPos pos) {
        BlockPos p = pos;
        for (int i = 0; i < 8 && level.getFluidState(p).is(FluidTags.WATER); i++) p = p.below();
        BlockState s = level.getBlockState(p);
        return s.is(net.minecraft.tags.BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD);
    }

    /**
     * The aquarium net used on open water (not a tank): cooldown, wear, maybe a catch. Returns true when the net was
     * used (also without a catch).
     */
    public static boolean tryCatch(ServerLevel level, Player player, InteractionHand hand, BlockPos water) {
        ItemStack net = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(net.getItem())) return false;
        player.getCooldowns().addCooldown(net.getItem(), ServerConfig.getOr(ServerConfig.CRITTERS_NET_COOLDOWN, 40));
        if (!player.getAbilities().instabuild) net.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, water, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5F, 1.4F);
        RandomGenerator rnd = random(level);
        Water w = water(level, water);
        boolean plants = plantsNear(level, water);
        Outcome o = decide(w, plants, hardBottom(level, water), org.overengineer.bettacraft.world.BiomeWaters.lushCave(level.getBiome(water)), netChance(),
                ServerConfig.getOr(ServerConfig.TANKMATES_NET_SHARE, 0.4), ServerConfig.getOr(ServerConfig.REEF_NET_SHARE, 0.4), plantShare(),
                org.overengineer.bettacraft.sources.PlantTables.net(w), rnd);
        // M20: on a reef the net sometimes brings up a coral frag or an anemone (soft corals that break off)
        if (o.frag() != null) {
            ItemStack frag = org.overengineer.bettacraft.plants.InteriorPlants.item(o.frag(), 0);
            player.displayClientMessage(Component.translatable("message.bettacraft.net.frag", frag.getHoverName()), true);
            if (!player.getInventory().add(frag)) player.drop(frag, false);
            return true;
        }
        // M22.4: in rivers, jungle water and swamps now and then an aquarium plant from the bottom
        if (o.plant() != null) {
            ItemStack plant = org.overengineer.bettacraft.plants.InteriorPlants.item(o.plant(), 0);
            player.displayClientMessage(Component.translatable("message.bettacraft.net.plant", plant.getHoverName()).withStyle(ChatFormatting.GREEN), true);
            if (!player.getInventory().add(plant)) player.drop(plant, false);
            level.playSound(null, water, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.5F, 1.2F);
            return true;
        }
        // M19: in jungle, swamp and river water some catches are tankmate fish of the region
        if (o.fish() != null) {
            var s = o.fish();
            int n = s.shoaling() ? 1 + rnd.nextInt(4) : 1;
            java.util.function.Supplier<org.overengineer.bettacraft.tankmates.TankmateGenes> genes = () -> org.overengineer.bettacraft.world.Wildlife.tankmate(s, rnd); // M22.3
            // M21.6: now and then a group of feral fancy guppies (escaped strains) instead of wild ones
            var feral = org.overengineer.bettacraft.tankmates.TankmateGenes.livebearer(s) && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()
                    ? org.overengineer.bettacraft.world.TankmateTrades.traderStrains(s) : java.util.List.<org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain>of();
            if (!feral.isEmpty() && rnd.nextInt(12) == 0) {
                var st = feral.get(rnd.nextInt(feral.size()));
                genes = () -> org.overengineer.bettacraft.tankmates.TankmateGenes.strain(s, st);
            }
            ItemStack bag = org.overengineer.bettacraft.tankmates.TankmateItems.groupBag(s, n, genes, rnd, false);
            give(player, bag, Component.translatable(s.translationKey()));
            return true;
        }
        Catch c = o.critter();
        if (c == null) {
            player.displayClientMessage(Component.translatable(o.nothingHere() ? "message.bettacraft.net.nothing_here" : "message.bettacraft.net.empty"), true);
            return true;
        }
        ItemStack bag = catchItem(level, c, rnd);
        if (bag.isEmpty()) return true;
        Component name = c.species() == null ? Component.translatable("entity.bettacraft.dwarf_clawed_frog") : Component.translatable(c.species().translationKey());
        give(player, bag, name);
        return true;
    }

    /** M22.2: the chance of a catch per swing in fitting water ({@code critters.netChance}). */
    public static double netChance() {
        return ServerConfig.getOr(ServerConfig.CRITTERS_NET_CHANCE, 0.3);
    }

    /** M22.4: the share of the catches that is a plant ({@code interiorPlants.netPlantShare}, 0 with {@code features.plantNetCatches} off). */
    public static double plantShare() {
        if (!org.overengineer.bettacraft.config.CommonConfig.plantNetCatches()) return 0;
        return ServerConfig.getOr(ServerConfig.AQUATIC_NET_SHARE, org.overengineer.bettacraft.sources.WildTables.NET_PLANT_SHARE);
    }

    /**
     * M22.2: what one swing of the net brings up, without side effects (the game and the handbook use the same rules):
     * first a coral frag on a reef, then (M22.4) an aquarium plant from the bottom, then a fish of the region, then a small
     * animal of the table. Nothing set = an empty net ({@code nothingHere}: this water holds nothing at all).
     */
    public record Outcome(@Nullable org.overengineer.bettacraft.plants.PlantSpecies frag, @Nullable org.overengineer.bettacraft.plants.PlantSpecies plant,
                          @Nullable org.overengineer.bettacraft.tankmates.TankmateSpecies fish, @Nullable Catch critter, boolean nothingHere) {}

    public static Outcome decide(Water w, boolean plants, boolean hardBottom, boolean lush, double netChance, double tankShare, double reefShare,
                                 double plantShare, org.overengineer.bettacraft.sources.WeightedTable<org.overengineer.bettacraft.plants.PlantSpecies> plantTable,
                                 RandomGenerator rnd) {
        List<Catch> table = table(w, plants, hardBottom, lush);
        double chance = netChance * (plants ? 1.2 : 1.0);
        var fish = fishTable(w);
        boolean ocean = w == Water.WARM_OCEAN || w == Water.LUKEWARM_OCEAN;
        if (w == Water.WARM_OCEAN && rnd.nextDouble() < chance * reefShare * org.overengineer.bettacraft.sources.WildTables.NET_FRAG_FACTOR) {
            return new Outcome(org.overengineer.bettacraft.sources.WildTables.NET_REEF_FRAGS.pick(rnd), null, null, null, false);
        }
        if (plantShare > 0 && !plantTable.isEmpty() && rnd.nextDouble() < chance * plantShare) {
            return new Outcome(null, plantTable.pick(rnd), null, null, false);
        }
        double share = ocean ? reefShare : tankShare;
        if (!fish.isEmpty() && rnd.nextDouble() < chance && rnd.nextDouble() < share) {
            var s = rollFish(fish, rnd);
            if (s != null) return new Outcome(null, null, s, null, false);
        }
        Catch c = table.isEmpty() || rnd.nextDouble() >= chance ? null : roll(table, rnd);
        return new Outcome(null, null, null, c, table.isEmpty() && plantTable.isEmpty());
    }

    /**
     * M22.2: the exact chances of {@link #decide} per swing: keys are {@link org.overengineer.bettacraft.plants.PlantSpecies}
     * (frags and, M22.4, plants), {@link org.overengineer.bettacraft.tankmates.TankmateSpecies} (fish), {@link CritterSpecies}
     * and the string {@code "frog"} for the dwarf clawed frog.
     */
    public static java.util.Map<Object, Double> odds(Water w, boolean plants, boolean hardBottom, boolean lush, double netChance, double tankShare,
                                                      double reefShare, double plantShare,
                                                      org.overengineer.bettacraft.sources.WeightedTable<org.overengineer.bettacraft.plants.PlantSpecies> plantTable) {
        java.util.Map<Object, Double> out = new java.util.LinkedHashMap<>();
        List<Catch> table = table(w, plants, hardBottom, lush);
        double chance = netChance * (plants ? 1.2 : 1.0);
        var fish = fishTable(w);
        boolean ocean = w == Water.WARM_OCEAN || w == Water.LUKEWARM_OCEAN;
        double left = 1.0;
        if (w == Water.WARM_OCEAN) {
            double pf = Math.min(1, chance * reefShare * org.overengineer.bettacraft.sources.WildTables.NET_FRAG_FACTOR);
            var frags = org.overengineer.bettacraft.sources.WildTables.NET_REEF_FRAGS;
            for (var f : frags.entries()) out.merge(f.value(), pf * frags.share(f.value()) / countOf(frags, f.value()), Double::sum);
            left -= pf;
        }
        if (plantShare > 0 && !plantTable.isEmpty()) {
            double pp = Math.min(1, chance * plantShare);
            for (var e : plantTable.entries()) out.merge(e.value(), left * pp * e.weight() / plantTable.total(), Double::sum);
            left *= 1 - pp;
        }
        double share = ocean ? reefShare : tankShare;
        if (!fish.isEmpty()) {
            double pFish = Math.min(1, chance) * Math.min(1, share);
            double total = 0;
            for (double v : fish.values()) total += v;
            if (total > 0) {
                for (var e : fish.entrySet()) out.merge(e.getKey(), left * pFish * e.getValue() / total, Double::sum);
                left *= 1 - pFish;
            }
        }
        double total = 0;
        for (Catch c : table) total += c.weight();
        if (total > 0) {
            for (Catch c : table) out.merge(c.species() == null ? "frog" : c.species(), left * Math.min(1, chance) * c.weight() / total, Double::sum);
        }
        return out;
    }

    /** Entries of a value in a table (share() sums them all; merged per entry). */
    private static <T> int countOf(org.overengineer.bettacraft.sources.WeightedTable<T> t, T v) {
        int n = 0;
        for (var e : t.entries()) if (e.value().equals(v)) n++;
        return Math.max(1, n);
    }

    /** Puts a catch into an empty fish bag of the player; without one it gets away. */
    public static void give(Player player, ItemStack filled, Component name) {
        var inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof FishBagItem && !FishBagItem.isFilled(s)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.net.no_bag", name).withStyle(ChatFormatting.GOLD), true);
            return;
        }
        if (!player.getAbilities().instabuild) inv.getItem(slot).shrink(1);
        if (!inv.add(filled)) player.drop(filled, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.bettacraft.net.caught", filled.getHoverName()).withStyle(ChatFormatting.GREEN), true);
    }
}
