package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.sources.PlantTables;
import org.overengineer.bettacraft.sources.Source;
import org.overengineer.bettacraft.sources.Sources;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.sources.WildTables;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.world.AquaticPlantFishing;

/**
 * M22.4: no plant only at the wandering trader. The aquarium net brings up the plants of its water (rivers: Vallisneria,
 * Monte Carlo, Java moss, Alternanthera, the sword; jungle water: the Amazon sword, Alternanthera, the tiger lotus, Java
 * moss; swamps: Monte Carlo, Alternanthera, the tiger lotus, the crypts and the rare Flamingo), not in other water, not
 * with {@code features.aquaticPlants.netCatches} off; the config lists change the tables; every plant species of the game (also
 * floating plants and corals) is in a net or fishing table the server really uses.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlantSourceGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PlantSourceGameTests() {}

    private static void biome(ServerLevel level, BlockPos at, String biome) {
        for (int x = at.getX() - 4; x <= at.getX() + 4; x += 4) for (int z = at.getZ() - 4; z <= at.getZ() + 4; z += 4) level.getChunk(x >> 4, z >> 4);
        var src = level.getServer().createCommandSourceStack().withSuppressedOutput().withLevel(level);
        level.getServer().getCommands().performPrefixedCommand(src, "fillbiome " + (at.getX() - 4) + " " + (at.getY() - 3) + " " + (at.getZ() - 4) + " "
                + (at.getX() + 4) + " " + (at.getY() + 4) + " " + (at.getZ() + 4) + " " + biome);
    }

    /** Swings the net {@code n} times at {@code water} and counts the aquarium plants that came up (by species). */
    private static Map<PlantSpecies, Integer> swing(GameTestHelper h, BlockPos water, int n) {
        ServerLevel level = h.getLevel();
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        Map<PlantSpecies, Integer> out = new EnumMap<>(PlantSpecies.class);
        try {
            sp.gameMode.changeGameModeForPlayer(GameType.CREATIVE); // the net does not wear out, bags are not used up
            sp.getInventory().clearContent();
            sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get(), 4));
            sp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.AQUARIUM_NET.get()));
            for (int i = 0; i < n; i++) {
                sp.getCooldowns().removeCooldown(ModItems.AQUARIUM_NET.get());
                NetCatching.tryCatch(level, sp, InteractionHand.OFF_HAND, water);
                for (int s = 0; s < sp.getInventory().getContainerSize(); s++) {
                    ItemStack st = sp.getInventory().getItem(s);
                    if (st.getItem() instanceof AquaticPlantItem a) {
                        out.merge(a.species(), st.getCount(), Integer::sum);
                        sp.getInventory().setItem(s, ItemStack.EMPTY);
                    }
                }
            }
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        return out;
    }

    /** Each new plant comes up in the net in its waters (with the net always catching a plant), and only the plants of that water. */
    @GameTest(template = EMPTY, batch = "bettacraft_plant_net", timeoutTicks = 400)
    public static void netBringsUpThePlantsOfItsWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double chance = ServerConfig.CRITTERS_NET_CHANCE.get(), share = ServerConfig.AQUATIC_NET_SHARE.get();
        try {
            ServerConfig.CRITTERS_NET_CHANCE.set(1.0);
            ServerConfig.AQUATIC_NET_SHARE.set(1.0);
            BlockPos rel = new BlockPos(16, 2, 16), water = h.absolutePos(rel);
            h.setBlock(rel, Blocks.WATER);
            Map<String, NetCatching.Water> biomes = Map.of("minecraft:river", NetCatching.Water.RIVER, "minecraft:jungle", NetCatching.Water.JUNGLE,
                    "minecraft:swamp", NetCatching.Water.SWAMP);
            Set<PlantSpecies> all = EnumSet.noneOf(PlantSpecies.class);
            for (var e : biomes.entrySet()) {
                biome(level, water, e.getKey());
                h.assertTrue(NetCatching.water(level, water) == e.getValue(), e.getKey() + ": water " + NetCatching.water(level, water));
                Map<PlantSpecies, Integer> got = swing(h, water, 1500); // M28c: rare plants (weight 2 of about 300) come up for sure
                int plants = got.values().stream().mapToInt(Integer::intValue).sum();
                h.assertTrue(plants == 1500, e.getKey() + ": every swing a plant: " + plants);
                var table = WildTables.netPlants(e.getValue());
                for (PlantSpecies p : got.keySet()) h.assertTrue(table.contains(p), e.getKey() + ": " + p + " is not a plant of this water");
                for (var en : table.entries()) h.assertTrue(got.containsKey(en.value()), e.getKey() + ": " + en.value() + " never came up: " + got);
                all.addAll(got.keySet());
            }
            for (PlantSpecies p : new PlantSpecies[]{PlantSpecies.AMAZON_SWORD, PlantSpecies.ALTERNANTHERA, PlantSpecies.VALLISNERIA, PlantSpecies.MONTE_CARLO,
                    PlantSpecies.CRYPT_FLAMINGO, PlantSpecies.TIGER_LOTUS}) {
                h.assertTrue(all.contains(p), "the former trader-only plant " + p + " comes up in the net");
            }
            // a plain beach and the lukewarm ocean have no plants
            biome(level, water, "minecraft:beach");
            h.assertTrue(swing(h, water, 60).isEmpty(), "no plants on a beach");
        } finally {
            ServerConfig.CRITTERS_NET_CHANCE.set(chance);
            ServerConfig.AQUATIC_NET_SHARE.set(share);
        }
        h.succeed();
    }

    /** {@code features.aquaticPlants.netCatches} off: the net brings up no plants; the config list replaces a table (an empty one: none). */
    @GameTest(template = EMPTY, batch = "bettacraft_plant_net_config", timeoutTicks = 400)
    public static void netPlantsSwitchAndConfigList(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        double chance = ServerConfig.CRITTERS_NET_CHANCE.get(), share = ServerConfig.AQUATIC_NET_SHARE.get();
        boolean feature = Features.own(Feature.PLANT_NET_CATCHES);
        var river = ServerConfig.NET_PLANTS_RIVER.get();
        try {
            ServerConfig.CRITTERS_NET_CHANCE.set(1.0);
            ServerConfig.AQUATIC_NET_SHARE.set(1.0);
            BlockPos rel = new BlockPos(16, 2, 16), water = h.absolutePos(rel);
            h.setBlock(rel, Blocks.WATER);
            biome(level, water, "minecraft:river");
            Features.set(Feature.PLANT_NET_CATCHES, false);
            h.assertTrue(swing(h, water, 60).isEmpty(), "switched off: no plants");
            Features.set(Feature.PLANT_NET_CATCHES, true);
            ServerConfig.NET_PLANTS_RIVER.set(java.util.List.of("red_tiger_lotus=1", "no_such_plant=5"));
            Map<PlantSpecies, Integer> got = swing(h, water, 40);
            h.assertTrue(got.keySet().equals(EnumSet.of(PlantSpecies.TIGER_LOTUS)) && got.get(PlantSpecies.TIGER_LOTUS) == 40,
                    "the config list: only the tiger lotus " + got);
            ServerConfig.NET_PLANTS_RIVER.set(java.util.List.of());
            h.assertTrue(PlantTables.net(NetCatching.Water.RIVER).isEmpty(), "an empty list");
            h.assertTrue(swing(h, water, 40).isEmpty(), "an empty list: no plants");
        } finally {
            ServerConfig.CRITTERS_NET_CHANCE.set(chance);
            ServerConfig.AQUATIC_NET_SHARE.set(share);
            Features.set(Feature.PLANT_NET_CATCHES, feature);
            ServerConfig.NET_PLANTS_RIVER.set(river);
        }
        h.succeed();
    }

    /**
     * The rule "never only at the trader" over every plant species the game has, with the tables the server uses: each
     * aquarium plant and coral is in a net table (plants or reef frags) or a fishing table, each floating plant in a fishing
     * table; and the handbook's sources say the same.
     */
    @GameTest(template = EMPTY)
    public static void noPlantOnlyAtTheTrader(GameTestHelper h) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        for (PlantSpecies p : PlantSpecies.values()) {
            if (p.land()) continue; // M56: the terrarium plants grow wild on the land (shears), see TerrariumPlantGameTests
            boolean wild = WildTables.NET_REEF_FRAGS.contains(p);
            for (NetCatching.Water w : NetCatching.Water.values()) wild |= PlantTables.net(w).contains(p);
            for (AquaticPlantFishing.Water w : AquaticPlantFishing.Water.values()) wild |= PlantTables.fishing(w).contains(p);
            if (!wild) problems.add(p.id() + ": in no net or fishing table");
            boolean book = Sources.of(Target.plant(p)).stream().anyMatch(s -> s.kind() == Source.Kind.NET || s.kind() == Source.Kind.FISHING);
            if (!book) problems.add(p.id() + ": the handbook names no net or fishing source");
        }
        for (FloatingPlantType f : FloatingPlantType.values()) {
            if (!f.grows() || f.plantItem()) continue; // M28: floating Riccia is the aquarium plant (checked above)
            boolean wild = WildTables.FISHING_FLOATING_SWAMP.contains(f) || WildTables.FISHING_FLOATING_MANGROVE.contains(f);
            if (!wild) problems.add(f.id() + ": in no fishing table");
        }
        h.assertTrue(problems.isEmpty(), "plants only at the trader: " + problems);
        h.succeed();
    }
}
