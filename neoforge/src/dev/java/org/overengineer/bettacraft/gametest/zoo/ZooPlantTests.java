package org.overengineer.bettacraft.gametest.zoo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.gametest.ZooAccess;
import org.overengineer.bettacraft.plants.foreign.ForeignPlants;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.interior.InteriorDrops;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M42 part b (zoo suite, {@link ZooSuite}): the water and wetland plants of other mods without a plant profile of ours (M38)
 * in the interior of a tank: placed on the sand floor (waterlogged where they can be), whether they stay (the interior is
 * dry), whether our plant effects take them (they should not: no profile), the items our interior drops would give for
 * them, and breaking the tank: the plant must come out as an item (else it is lost). Table {@code zoo/plants.tsv}.
 */
@PrefixGameTestTemplate(false)
public final class ZooPlantTests {
    /** Names of water and wetland plants (the plant classes alone would take every flower and sapling). */
    static final Pattern AQUATIC = Pattern.compile("kelp|seagrass|sea_grass|coral|lily|lilies|lilypad|algae|reed|cattail|duckweed|anemone|lotus|hornwort"
            + "|moss|weed|bulrush|papyrus|tube_worm|sponge|pickle|hyacinth|rush|marsh|swamp|water|aqua|sea_|frogspawn|glitter|shimmer");

    /** The grounds tried for a plant: the sand floor, then a block on it (dirt, mud, grass, clay, gravel, stone, ice). */
    public static final List<BlockState> FLOORS = List.of(Blocks.SAND.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.MUD.defaultBlockState(),
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.CLAY.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState(),
            Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.ICE.defaultBlockState(), Blocks.SCULK.defaultBlockState(), Blocks.NETHERRACK.defaultBlockState(),
            Blocks.SOUL_SOIL.defaultBlockState(), Blocks.END_STONE.defaultBlockState());
    static final String FLOOR_NAMES = "sand floor, dirt, mud, grass, clay, gravel, stone, moss, ice, sculk, netherrack, soul soil, end stone";

    private ZooPlantTests() {}

    public static List<Block> candidates() {
        List<Identifier> ids = new ArrayList<>(BuiltInRegistries.BLOCK.keySet());
        ids.sort(java.util.Comparator.comparing(Identifier::toString));
        List<Block> out = new ArrayList<>();
        for (Identifier rl : ids) {
            if (ZooSuite.OWN.contains(rl.getNamespace()) || rl.getPath().startsWith("potted_")) continue;
            Block b = BuiltInRegistries.BLOCK.getValue(rl);
            boolean plant = b instanceof BushBlock || b instanceof GrowingPlantBlock || b instanceof BaseCoralPlantTypeBlock
                    || b instanceof LiquidBlockContainer && !(b instanceof net.minecraft.world.level.block.SimpleWaterloggedBlock)
                    && !(b instanceof net.minecraft.world.level.block.EntityBlock);
            if (!plant || !AQUATIC.matcher(rl.getPath()).find()) continue;
            if (ForeignPlants.of(b.defaultBlockState()) != null) continue; // M38 profile: integrated
            if (b.asItem() == net.minecraft.world.item.Items.AIR && !(b instanceof GrowingPlantBlock)) continue; // technical blocks
            out.add(b);
        }
        return out;
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_plants", timeoutTicks = 400)
    public static void zooPlants0(GameTestHelper h) {
        plants(h, 0);
    }

    @GameTest(templateNamespace = BettaCraft.MOD_ID, template = ZooSuite.EMPTY, batch = "bettacraft_zoo_plants", timeoutTicks = 400)
    public static void zooPlants1(GameTestHelper h) {
        plants(h, 1);
    }

    private record Placed(Block block, TankBlockEntity tank, InteriorRegion region, BlockPos at, String in) {}

    private static void plants(GameTestHelper h, int part) {
        ServerLevel level = h.getLevel();
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        List<Block> all = candidates();
        int from = part * ZooAnimalTests.SLOTS, to = Math.min(all.size(), from + ZooAnimalTests.SLOTS);
        if (from >= to) {
            h.succeed();
            return;
        }
        List<String> violations = new ArrayList<>();
        List<Placed> placed = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Block b = all.get(i);
            int k = i - from, x = 1 + 3 * (k % 10), z = 1 + 3 * (k / 10);
            try {
                TankBlockEntity c = ZooAccess.waterTank(h, x, z, 2, 2, 2, 25F);
                InteriorRegion r = ZooAccess.sandFloor(h, c);
                BlockPos at = r.toWorld(16, 0, 16);
                BlockState s = b.defaultBlockState();
                if (s.hasProperty(BlockStateProperties.WATERLOGGED)) s = s.setValue(BlockStateProperties.WATERLOGGED, true);
                // a player can only place it where it survives: the floor substrates of the interior one after the other, then
                // on a block of the interior (the interior has no water blocks: M29.1)
                String ground = null;
                for (BlockState floor : FLOORS) {
                    if (floor.isAir()) continue;
                    if (floor.is(Blocks.SAND)) {
                        if (s.canSurvive(in, at)) { ground = "sand floor"; break; }
                        continue;
                    }
                    in.setBlock(at, floor, 3);
                    BlockPos up = at.above();
                    if (s.canSurvive(in, up)) { ground = BuiltInRegistries.BLOCK.getKey(floor.getBlock()).getPath() + " block"; at = up; break; }
                    in.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                }
                if (ground == null) {
                    placed.add(new Placed(b, null, null, null, "cannot be planted: survives on none of " + FLOOR_NAMES));
                    continue;
                }
                if (b instanceof DoublePlantBlock) DoublePlantBlock.placeAt(in, s, at, 3);
                else in.setBlock(at, s, 3);
                placed.add(new Placed(b, c, r, at, "on " + ground + ", placed " + (in.getBlockState(at).is(b) ? "yes" : "NO")));
            } catch (Throwable t) {
                placed.add(new Placed(b, null, null, null, "EXCEPTION " + ZooSuite.brief(t)));
                if (ZooSuite.ours(t)) violations.add(BuiltInRegistries.BLOCK.getKey(b) + ": " + ZooSuite.brief(t));
            }
        }
        h.runAfterDelay(80, () -> {
            List<List<String>> rows = new ArrayList<>();
            for (Placed p : placed) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(p.block());
                if (p.tank() == null) {
                    rows.add(ZooSuite.row(id.getNamespace(), id, p.block().getClass().getSimpleName(), p.in(), "-", "-", "-"));
                    continue;
                }
                String stay = "", drops = "", broken = "";
                try {
                    BlockState now = in.getBlockState(p.at());
                    stay = now.is(p.block()) ? "stays" : "GONE (now " + BuiltInRegistries.BLOCK.getKey(now.getBlock()) + ")";
                    var effect = ForeignPlants.effect(level.getServer(), p.region());
                    stay += ", plant effect " + (effect.equals(ForeignPlants.Effect.NONE) || effect.mass() <= 0 ? "none" : "YES " + effect);
                    List<ItemStack> out = new ArrayList<>();
                    if (now.is(p.block())) InteriorDrops.collect(in, p.at(), now, out);
                    drops = out.stream().map(st -> st.getCount() + " " + BuiltInRegistries.ITEM.getKey(st.getItem())).toList().toString();
                    // break the tank: its interior comes out as items in front of it
                    AABB box = org.overengineer.bettacraft.tank.TankInhabitants.tankBox(p.tank().getTank()).inflate(3);
                    List<BlockPos> members = new ArrayList<>();
                    for (long l : p.tank().getTank().members()) members.add(BlockPos.of(l));
                    boolean wasThere = now.is(p.block());
                    for (BlockPos q : members) level.destroyBlock(q, false);
                    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
                    boolean got = false;
                    for (ItemEntity ie : items) for (ItemStack want : out) if (ItemStack.isSameItem(ie.getItem(), want)) got = true;
                    broken = items.size() + " item(s): " + items.stream().map(ie -> BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).toString()).distinct().toList();
                    if (wasThere && !out.isEmpty() && !got) violations.add(id + ": the tank broke without the plant's item");
                    if (wasThere && out.isEmpty()) violations.add(id + ": our interior drops give nothing for it (lost when the tank breaks)");
                    for (ItemEntity ie : items) ie.discard();
                } catch (Throwable t) {
                    broken += " EXCEPTION " + ZooSuite.brief(t);
                    if (ZooSuite.ours(t)) violations.add(id + ": " + ZooSuite.brief(t));
                }
                rows.add(ZooSuite.row(id.getNamespace(), id, p.block().getClass().getSimpleName(), p.in(), stay, drops, broken));
            }
            ZooSuite.table("plants" + part, List.of("mod", "block", "class", "placed", "after 80 ticks", "interior drops", "tank broken"), rows);
            ZooSuite.verdict("zoo plants part " + part, violations);
            h.succeed();
        });
    }
}
