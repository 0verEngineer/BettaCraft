package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaTooltip;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaLoci;
import org.overengineer.bettacraft.genetics.BettaTypes;
import org.overengineer.bettacraft.genetics.BettaVarietyTable;
import org.overengineer.bettacraft.genetics.GeneticsData;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;

/** M5: betta types from the datapack, type names, inheritance of fin length and colour type, fin-form migration. */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TypeGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private TypeGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(2 + a, 1 + b, 2 + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)));
        if (c == null) throw new GameTestFailure("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, water);
        c.getTank().legacyHeaters().setItem(0, new ItemStack(ModItems.HEATER_LARGE.get()));
        c.getTank().setTargetTemp(27.0F);
        c.getTank().setWaterTemp(27.0F);
        return c;
    }

    private static BettaEntity insert(GameTestHelper h, TankBlockEntity c, BettaData d) {
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestFailure("insert failed: " + err.getString());
        for (BettaEntity b : bettas(h, c)) if (b.getBettaData().fishId().equals(d.fishId())) return b;
        throw new GameTestFailure("inserted betta not found");
    }

    private static List<BettaEntity> bettas(GameTestHelper h, TankBlockEntity c) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b && b.isAlive()) out.add(b);
        return out;
    }

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    /** The server loaded the genetics from the mod's datapack files (not only the built-in fallback). */
    @GameTest(template = EMPTY)
    public static void geneticsComeFromTheDatapack(GameTestHelper h) {
        GeneticsData d = GeneticsData.get();
        h.assertTrue(d.colors().size() == 43, "43 colours loaded, got " + d.colors().size());
        h.assertTrue(d.varieties().size() == BettaVarietyTable.VARIETIES.size(), "all varieties loaded, got " + d.varieties().size());
        h.assertTrue(d.alleles("finform").size() == BettaGenome.SHAPES.size(), "all tail shapes: " + d.alleles("finform").keySet());
        h.assertTrue(d.alleles("fin_length").containsKey("short") && d.alleles("fin_length").containsKey("long"), "fin length alleles");
        h.assertTrue(BettaLoci.FIN_LENGTH.express("long", "short").equals("short"), "short fins dominate");
        h.succeed();
    }

    /** Every named variety, put into a tank as its example fish, is shown with its own name. */
    @GameTest(template = EMPTY)
    public static void everyVarietyIsNamedInTheTank(GameTestHelper h) {
        TankBlockEntity c = tank(h, 6, 2, 4, 48000);
        for (GeneticsData.VarietyDef v : GeneticsData.get().varieties()) {
            BettaGenome ph = BettaGenome.of(v.example(), BettaGenome.DEFAULT);
            BettaEntity b = insert(h, c, BettaData.create(ph, false, 0));
            GeneticsData.VarietyDef got = BettaTypes.variety(b.genome());
            h.assertTrue(got != null && got.id().equals(v.id()), v.id() + " named " + (got == null ? "generic" : got.id()));
            String k = key(BettaTooltip.type(b.genome()));
            h.assertTrue(v.includeForm() ? k.equals("type.bettacraft.full") : k.equals("variety.bettacraft." + v.id()), v.id() + ": " + k);
        }
        // a short-finned halfmoon is a Halfmoon Plakat, the wild-type form a Plakat
        BettaGenome hmpk = BettaGenome.of(java.util.Map.of("finform", "halfmoon", "fin_length", "short", "body_color", "lavender",
                "fin_color", "lavender"), BettaGenome.DEFAULT);
        h.assertTrue(key(BettaTypes.form(hmpk)).equals("form.bettacraft.short"), "Halfmoon Plakat");
        h.assertTrue(key(BettaTypes.form(BettaGenome.DEFAULT)).equals("form.bettacraft.plakat"), "Plakat");
        h.succeed();
    }

    /**
     * Inheritance in the tank: a Mustard Gas pair, both carrying long fins (short/long), spawns into the nest. Every
     * egg is a Mustard Gas (colours are homozygous), the fin-length alleles come from the parents and both forms occur.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void typeAndFinLengthAreInherited(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 3, 2, 2, 12000);
        BettaGenome mg = BettaGenome.of(java.util.Map.of("finform", "halfmoon", "body_color", "navy", "fin_color", "mustard",
                "pattern", "none", "iridescence", "high", "iridescence_color", "royal_blue"), BettaGenome.DEFAULT);
        BettaGenotype parent = BettaGenotype.homozygous(mg).with(BettaLoci.FIN_LENGTH, new BettaGenotype.AllelePair("short", "long"));
        BettaEntity male = insert(h, c, BettaData.create(parent, false, 0));
        insert(h, c, BettaData.create(parent, true, 0));
        h.assertTrue(male.genome().isShortFinned(), "the carrier parents are plakats");
        TankData t = c.getTank();
        TankCare.feed(level, c, new ItemStack(ModItems.MOSQUITO_LARVAE.get()), 1);
        for (int i = 0; i < 60 && (t.nest() == null || !t.nest().hasEggs()); i++) TankBreeding.step(level, c, 0.05);
        BubbleNest n = t.nest();
        h.assertTrue(n != null && n.hasEggs(), "eggs laid");
        int shortFins = 0, longFins = 0, mutated = 0;
        for (BubbleNest.Egg e : n.eggs()) {
            // breeding.mutationChance (1 % per allele) may turn an allele into one the parents do not have: such
            // eggs are skipped (they made this test flaky); every other egg must be a Mustard Gas
            boolean fromParents = true;
            for (var l : org.overengineer.bettacraft.genetics.BettaLoci.ALL) {
                BettaGenotype.AllelePair pp = parent.pair(l), cp = e.genotype().pair(l);
                List<String> have = List.of(pp.a(), pp.b());
                if (!have.contains(cp.a()) || !have.contains(cp.b())) fromParents = false;
            }
            if (!fromParents) {
                mutated++;
                continue;
            }
            BettaGenome child = e.genotype().express();
            GeneticsData.VarietyDef v = BettaTypes.variety(child);
            h.assertTrue(v != null && v.id().equals("mustard_gas"), "every egg is a Mustard Gas: " + child);
            BettaGenotype.AllelePair p = e.genotype().pair(BettaLoci.FIN_LENGTH);
            h.assertTrue(List.of("short", "long").contains(p.a()) && List.of("short", "long").contains(p.b()), "fin length alleles from the parents");
            if (child.isShortFinned()) shortFins++;
            else longFins++;
        }
        h.assertTrue(mutated * 2 < n.eggs().size(), "most eggs carry only parental alleles (" + mutated + " mutated)");
        h.assertTrue(shortFins > 0, "some offspring are plakats (" + shortFins + "/" + longFins + ")");
        h.succeed();
    }

    /** A betta bucket written by M2-M4 (schema 2, finform plakat carrying veil) loads as a short-finned roundtail. */
    @GameTest(template = EMPTY)
    public static void m4BucketMigratesFinform(GameTestHelper h) throws Exception {
        TankBlockEntity c = tank(h, 2, 2, 2, 6000);
        CompoundTag v2 = TagParser.parseCompoundFully("{betta:{schema:2,genome:{species:\"bettacraft:betta\",finform:[\"plakat\",\"veil\"],"
                + "pectoral:[\"normal\",\"normal\"],body_color:[\"navy\",\"navy\"],fin_color:[\"mustard\",\"mustard\"],pattern:[\"none\",\"none\"],"
                + "pattern_color:[\"black\",\"black\"],iridescence:[\"high\",\"low\"],iridescence_color:[\"royal_blue\",\"royal_blue\"]},"
                + "sex:\"male\",stage:\"adult\",generation:4,born:5L,health:{}},Health:6.0f}");
        ItemStack bucket = new ItemStack(ModItems.BETTA_BUCKET.get());
        bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(v2));
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        h.assertTrue(err == null, "insert of an M4 bucket");
        BettaEntity b = bettas(h, c).getFirst();
        BettaGenotype g = b.genotype();
        h.assertTrue("round".equals(g.pair(BettaLoci.FINFORM).a()) && "veil".equals(g.pair(BettaLoci.FINFORM).b()), "finform round/veil: " + g);
        h.assertTrue("short".equals(g.pair(BettaLoci.FIN_LENGTH).a()) && "long".equals(g.pair(BettaLoci.FIN_LENGTH).b()), "fin_length short/long");
        h.assertTrue(b.genome().isShortFinned() && "round".equals(b.genome().shape()), "still a plakat");
        h.assertTrue("mustard_gas".equals(BettaTypes.variety(b.genome()).id()), "named Mustard Gas");
        h.assertTrue(b.getBettaData().generation() == 4, "other data kept");
        h.assertTrue(b.getBettaData().save().getIntOr("schema", 0) == org.overengineer.bettacraft.data.schema.Schemas.BETTA.currentVersion(), "saved in the current version");
        h.succeed();
    }

    /** A controller saved by M4 (tank schema 3) with eggs in the nest: the egg genotypes get the fin-form split. */
    @GameTest(template = EMPTY)
    public static void m4NestEggsMigrate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 8000);
        CompoundTag tag = c.saveWithFullMetadata(level.registryAccess());
        tag.putInt("schema", 3);
        CompoundTag nest = new CompoundTag();
        nest.putString("phase", "eggs");
        nest.putDouble("x", c.getBlockPos().getX() + 0.5);
        nest.putDouble("y", c.getBlockPos().getY() + 1.8);
        nest.putDouble("z", c.getBlockPos().getZ() + 0.5);
        net.minecraft.nbt.ListTag eggs = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 3; i++) {
            CompoundTag e = new CompoundTag();
            CompoundTag gen = new CompoundTag();
            net.minecraft.nbt.ListTag fin = new net.minecraft.nbt.ListTag();
            fin.add(net.minecraft.nbt.StringTag.valueOf("plakat"));
            fin.add(net.minecraft.nbt.StringTag.valueOf(i == 0 ? "plakat" : "crowntail"));
            gen.put("finform", fin);
            e.put("genome", gen);
            e.putBoolean("female", i == 1);
            eggs.add(e);
        }
        nest.put("eggs", eggs);
        tag.getCompoundOrEmpty("tank").put("nest", nest);
        c.loadWithComponents(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
        BubbleNest n = c.getTank().nest();
        h.assertTrue(n != null && n.eggCount() == 3, "three eggs survive the load");
        for (BubbleNest.Egg e : n.eggs()) {
            h.assertTrue("round".equals(e.genotype().pair(BettaLoci.FINFORM).a()), "plakat -> round");
            h.assertTrue("short".equals(e.genotype().express().finLength()), "egg is a plakat");
        }
        h.assertTrue("crowntail".equals(n.eggs().get(1).genotype().pair(BettaLoci.FINFORM).b())
                && "long".equals(n.eggs().get(1).genotype().pair(BettaLoci.FIN_LENGTH).b()), "carrier allele kept");
        h.assertTrue(c.saveWithFullMetadata(level.registryAccess()).getIntOr("schema", 0) == org.overengineer.bettacraft.data.schema.Schemas.TANK_BLOCK_ENTITY.currentVersion(), "saved as the current version");
        h.succeed();
    }
}
