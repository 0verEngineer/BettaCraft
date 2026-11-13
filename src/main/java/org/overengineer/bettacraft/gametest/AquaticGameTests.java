package org.overengineer.bettacraft.gametest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.AquaticSpecies;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M16: water animals of vanilla and every loaded mod in our tanks; no tank inside a tank.
 * <p>
 * The species tests go over every water animal type found at runtime ({@link AquaticSpecies#discover}): without other
 * mods these are the vanilla ones (and the aquarium mods of the dev setup), with {@code -Paquatic} every water animal mod
 * of the ATM10 pack. Each type gets its own tank: accepted ones are put in (with their bucket, or with our fish bag),
 * live 200 ticks in it, must still be alive and inside and are taken out again; refused ones must be refused with a
 * message and leave the tank empty.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticGameTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String EMPTY = "empty_32x10x32";
    /** The species are spread over this many tests of at most {@link #PER_TEST} tanks each. */
    private static final int SLICES = 12;
    private static final int PER_TEST = 25;
    private static final int LIVE_TICKS = 200;

    private AquaticGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, long water) {
        for (int a = 0; a < sx; a++)
            for (int b = 0; b < sy; b++)
                for (int c = 0; c < sz; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        TankInteractions.changeWater(h.getLevel(), c, water);
        return c;
    }

    /** A filled container for this species: its own bucket if it has one, else our fish bag. */
    private static ItemStack container(GameTestHelper h, AquaticSpecies.Species s) {
        if (!s.buckets().isEmpty()) return new ItemStack(s.buckets().getFirst());
        Entity e = s.type().create(h.getLevel());
        if (e == null) throw new GameTestAssertException("cannot create " + s.id());
        ItemStack bag = FishBagItem.fillWith(e);
        e.discard();
        return bag;
    }

    // ------------------------------------------------------------------ inventory

    /** Lists every water animal type with size, buckets and acceptance (log and aquatic-inventory.tsv in the game dir). */
    @GameTest(template = EMPTY)
    public static void aquaticInventory(GameTestHelper h) {
        List<AquaticSpecies.Species> all = AquaticSpecies.discover(h.getLevel());
        List<String> lines = AquaticSpecies.table(all);
        for (String l : lines) LOGGER.info("[aquatic] {}", l);
        try {
            Files.write(Path.of("aquatic-inventory.tsv"), lines, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            LOGGER.warn("cannot write aquatic-inventory.tsv: {}", e.toString());
        }
        h.assertTrue(all.size() <= SLICES * PER_TEST, all.size() + " water animals: raise SLICES in AquaticGameTests");
        for (String v : List.of("cod", "salmon", "tropical_fish", "pufferfish", "axolotl", "tadpole", "squid", "glow_squid")) {
            AquaticSpecies.Species s = all.stream().filter(x -> x.id().getNamespace().equals("minecraft") && x.id().getPath().equals(v))
                    .findFirst().orElseThrow(() -> new GameTestAssertException("vanilla " + v + " not found"));
            h.assertTrue(s.accepted(), v + " goes into tanks, got " + s.acceptance());
        }
        for (String v : List.of("dolphin", "turtle", "guardian", "elder_guardian", "frog")) {
            all.stream().filter(x -> x.id().getNamespace().equals("minecraft") && x.id().getPath().equals(v)).findFirst()
                    .ifPresent(s -> h.assertTrue(!s.accepted(), v + " is refused"));
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ every species

    @GameTestGenerator
    public static Collection<TestFunction> everyWaterAnimal() {
        List<TestFunction> out = new ArrayList<>();
        for (int i = 0; i < SLICES; i++) {
            int slice = i;
            out.add(new TestFunction("bettacraft_aquatic_species", "aquaticspecies_" + i, BettaCraft.MOD_ID + ":" + EMPTY,
                    LIVE_TICKS + 200, 0, true, h -> species(h, slice)));
        }
        return out;
    }

    private record Placed(AquaticSpecies.Species species, TankBlockEntity tank, @Nullable UUID entity) {}

    private static void species(GameTestHelper h, int slice) {
        List<AquaticSpecies.Species> all = AquaticSpecies.discover(h.getLevel());
        List<AquaticSpecies.Species> mine = new ArrayList<>();
        for (int i = slice; i < all.size(); i += SLICES) mine.add(all.get(i));
        if (mine.isEmpty()) {
            h.succeed();
            return;
        }
        if (mine.size() > PER_TEST) throw new GameTestAssertException(mine.size() + " species in one test: raise SLICES");
        List<Placed> placed = new ArrayList<>();
        for (int i = 0; i < mine.size(); i++) {
            AquaticSpecies.Species s = mine.get(i);
            TankBlockEntity c = tank(h, 1 + (i % 5) * 6, 1, 1 + (i / 5) * 6, 3, 2, 3, 16000);
            ItemStack stack = container(h, s);
            EntityType<?> type = TankInhabitants.containedType(stack);
            h.assertTrue(type == s.type(), s.id() + ": container holds " + type);
            Component err = TankInhabitants.insert(h.getLevel(), c, stack, type, null, TankInhabitants.carriedWater(stack));
            List<Entity> live = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
            if (s.accepted()) {
                if (err != null) throw new GameTestAssertException(s.id() + " should go in: " + err.getString());
                h.assertTrue(live.size() == 1 && live.getFirst().getType() == s.type(), s.id() + ": one inside, got " + live);
                placed.add(new Placed(s, c, live.getFirst().getUUID()));
            } else {
                h.assertTrue(err != null, s.id() + " (" + s.acceptance() + ") must be refused");
                h.assertTrue(!err.getString().isBlank(), s.id() + ": refusal has a message");
                h.assertTrue(live.isEmpty(), s.id() + ": nothing inside after the refusal");
                LOGGER.info("[aquatic] {} refused: {}", s.id(), err.getString());
            }
        }
        h.runAfterDelay(LIVE_TICKS / 2, () -> checkInside(h, placed, false));
        h.runAfterDelay(LIVE_TICKS, () -> {
            checkInside(h, placed, true);
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            try {
                for (Placed p : placed) {
                    Entity e = h.getLevel().getEntity(p.entity());
                    boolean bag = !(e instanceof Bucketable) || p.species().buckets().isEmpty();
                    sp.getInventory().clearContent();
                    sp.getInventory().add(bag ? new ItemStack(ModItems.FISH_BAG.get()) : new ItemStack(Items.BUCKET));
                    Component err = TankInhabitants.takeOut(sp, p.tank(), p.entity(), bag);
                    if (err != null) throw new GameTestAssertException(p.species().id() + ": take out failed: " + err.getString());
                    h.assertTrue(h.getLevel().getEntity(p.entity()) == null, p.species().id() + ": gone from the tank");
                    boolean filled = false;
                    for (ItemStack s : sp.getInventory().items) filled |= TankInhabitants.containedType(s) == p.species().type();
                    h.assertTrue(filled, p.species().id() + ": in the " + (bag ? "bag" : "bucket") + " now");
                    LOGGER.info("[aquatic] {} lived {} ticks in a tank and was taken out with a {}", p.species().id(), LIVE_TICKS, bag ? "bag" : "bucket");
                }
            } finally {
                sp.getServer().getPlayerList().remove(sp);
            }
            h.succeed();
        });
    }

    private static void checkInside(GameTestHelper h, List<Placed> placed, boolean last) {
        for (Placed p : placed) {
            Entity e = h.getLevel().getEntity(p.entity());
            if (e == null || !e.isAlive()) throw new GameTestAssertException(p.species().id() + " died in the tank" + (e == null ? "" : ": " + e));
            if (!TankInhabitants.inside(p.tank().getTank(), e)) {
                throw new GameTestAssertException(p.species().id() + " left the tank: " + e.position() + " tank " + p.tank().getTank().min());
            }
            h.assertTrue(TankInhabitants.hasTankRef(e), p.species().id() + " keeps its tank reference");
            if (last) h.assertTrue(p.tank().getTank().inhabitants().contains(p.entity()), p.species().id() + " is on the inhabitant list");
        }
    }

    // ------------------------------------------------------------------ rules

    /** Refusals have their own messages; the config lists win over tags and size. */
    @GameTest(template = EMPTY, batch = "bettacraft_aquatic_config")
    public static void rulesAndConfigLists(GameTestHelper h) {
        var allow = ServerConfig.INHABITANTS_ALLOW.get();
        var deny = ServerConfig.INHABITANTS_DENY.get();
        try {
            h.assertTrue(TankInhabitants.accepts(EntityType.DOLPHIN, null) == TankInhabitants.Acceptance.TOO_BIG, "dolphin too big");
            h.assertTrue(TankInhabitants.accepts(EntityType.FROG, null) == TankInhabitants.Acceptance.BLACKLISTED, "frog blacklisted");
            h.assertTrue(TankInhabitants.accepts(EntityType.WITHER, null) == TankInhabitants.Acceptance.BOSS, "wither is a boss");
            h.assertTrue(TankInhabitants.accepts(EntityType.ENDERMITE, null) == TankInhabitants.Acceptance.HOSTILE, "endermite hostile");
            Entity bee = EntityType.BEE.create(h.getLevel());
            h.assertTrue(TankInhabitants.accepts(EntityType.BEE, bee) == TankInhabitants.Acceptance.NOT_AQUATIC, "a bee is no water animal");
            bee.discard();
            Entity squid = EntityType.SQUID.create(h.getLevel());
            h.assertTrue(TankInhabitants.accepts(EntityType.SQUID, squid) == TankInhabitants.Acceptance.OK, "squids fit (0.8)");
            squid.discard();
            h.assertTrue(EntityType.AXOLOTL.is(ModTags.PREDATORS), "axolotls are predators");
            ServerConfig.INHABITANTS_DENY.set(List.of("minecraft:cod"));
            ServerConfig.INHABITANTS_ALLOW.set(List.of("minecraft:*"));
            h.assertTrue(TankInhabitants.accepts(EntityType.COD, null) == TankInhabitants.Acceptance.BLACKLISTED, "deny wins");
            h.assertTrue(TankInhabitants.accepts(EntityType.DOLPHIN, null) == TankInhabitants.Acceptance.OK, "allow minecraft:* takes dolphins");
            ServerConfig.INHABITANTS_ALLOW.set(List.of());
            ServerConfig.INHABITANTS_DENY.set(List.of());
            TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 16000);
            Component tooBig = TankInhabitants.insert(h.getLevel(), c, FishBagItem.fillWith(EntityType.DOLPHIN.create(h.getLevel())), EntityType.DOLPHIN, null, 100);
            h.assertTrue(tooBig != null && tooBig.getString().contains("too big"), "message names the size: " + tooBig);
        } finally {
            ServerConfig.INHABITANTS_ALLOW.set(allow);
            ServerConfig.INHABITANTS_DENY.set(deny);
        }
        h.succeed();
    }

    /** A squid (no bucket) goes in with the fish bag, keeps its data and comes out with a bag again. */
    @GameTest(template = EMPTY)
    public static void squidInFishBag(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 3, 2, 3, 16000);
        Entity squid = EntityType.GLOW_SQUID.create(h.getLevel());
        squid.setCustomName(Component.literal("Inky"));
        ItemStack bag = FishBagItem.fillWith(squid);
        squid.discard();
        h.assertTrue(FishBagItem.storedEntity(bag) != null, "full entity data in the bag");
        Component err = TankInhabitants.insert(h.getLevel(), c, bag, EntityType.GLOW_SQUID, null, 100);
        if (err != null) throw new GameTestAssertException(err.getString());
        Entity in = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
        h.assertTrue(in.getType() == EntityType.GLOW_SQUID && "Inky".equals(in.getCustomName().getString()), "the named glow squid is inside");
        h.runAfterDelay(100, () -> {
            h.assertTrue(in.isAlive() && TankInhabitants.inside(c.getTank(), in), "still inside");
            ServerPlayer sp = MockPlayers.serverPlayer(h);
            try {
                sp.getInventory().add(new ItemStack(Items.BUCKET));
                h.assertTrue(TankInhabitants.takeOut(sp, c, in.getUUID(), false) != null, "a squid does not fit into a bucket");
                sp.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
                Component out = TankInhabitants.takeOut(sp, c, in.getUUID(), true);
                h.assertTrue(out == null, "taken out with the bag: " + (out == null ? "" : out.getString()));
                ItemStack filled = sp.getInventory().items.stream().filter(s -> FishBagItem.storedEntity(s) != null).findFirst()
                        .orElseThrow(() -> new GameTestAssertException("no filled bag"));
                CompoundTag t = FishBagItem.storedEntity(filled);
                h.assertTrue("minecraft:glow_squid".equals(t.getString("id")) && t.getString("CustomName").contains("Inky"), "data kept: " + t);
            } finally {
                sp.getServer().getPlayerList().remove(sp);
            }
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ tadpoles

    private static Tadpole tadpoleIn(GameTestHelper h, TankBlockEntity c) {
        Component err = TankInhabitants.insert(h.getLevel(), c, new ItemStack(Items.TADPOLE_BUCKET), EntityType.TADPOLE, null, 1000);
        if (err != null) throw new GameTestAssertException(err.getString());
        return (Tadpole) TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).getFirst();
    }

    private static void growUp(Tadpole t) {
        CompoundTag age = new CompoundTag();
        age.putInt("Age", Tadpole.ticksToBeFrog);
        t.loadFromBucketTag(age); // reaches the frog age: converts right away
    }

    /** A grown-up tadpole leaves the tank as a frog in front of it (default). */
    @GameTest(template = EMPTY)
    public static void tadpoleLeavesAsFrog(GameTestHelper h) {
        TankBlockEntity c = tank(h, 4, 1, 4, 3, 2, 3, 16000);
        c.getTank().setFront(Direction.SOUTH);
        Tadpole t = tadpoleIn(h, c);
        growUp(t);
        h.runAfterDelay(2, () -> {
            h.assertTrue(!t.isAlive(), "the tadpole is gone");
            AABB around = new AABB(h.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
            List<Frog> frogs = h.getLevel().getEntitiesOfClass(Frog.class, around);
            h.assertTrue(frogs.size() == 1, "one frog, got " + frogs.size());
            Frog f = frogs.getFirst();
            h.assertTrue(!c.getTank().contains(f.blockPosition()) && !TankInhabitants.hasTankRef(f), "the frog is outside: " + f.position());
            h.assertTrue(f.blockPosition().getZ() == c.getTank().max().getZ() + 1, "in front (south) of the tank: " + f.position());
            h.assertTrue(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).isEmpty(), "tank empty");
            h.assertTrue(c.getTank().log().stream().anyMatch(l -> l.key().equals("log.bettacraft.tadpole_left")), "log entry");
            h.succeed();
        });
    }

    /** With inhabitants.tadpoles = STAY_TADPOLE it does not grow up in a tank. */
    @GameTest(template = EMPTY, batch = "bettacraft_aquatic_tadpole_config")
    public static void tadpoleStaysTadpole(GameTestHelper h) {
        var saved = ServerConfig.INHABITANTS_TADPOLES.get();
        ServerConfig.INHABITANTS_TADPOLES.set(ServerConfig.Tadpoles.STAY_TADPOLE);
        try {
            TankBlockEntity c = tank(h, 4, 1, 4, 3, 2, 3, 16000);
            Tadpole t = tadpoleIn(h, c);
            growUp(t);
            h.assertTrue(t.isAlive() && TankInhabitants.inside(c.getTank(), t), "still a tadpole in the tank");
            CompoundTag tag = new CompoundTag();
            t.addAdditionalSaveData(tag);
            h.assertTrue(tag.getInt("Age") < Tadpole.ticksToBeFrog, "age set back: " + tag.getInt("Age"));
        } finally {
            ServerConfig.INHABITANTS_TADPOLES.set(saved);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ no tank inside a tank

    /** In the interior a tank block can neither be placed with the item nor survive being set there; outside it can. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void noTankInsideATank(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 2, 2, 2, 0);
        ServerLevel interior = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (interior == null) throw new GameTestAssertException("no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        BlockPos floor = r.toWorld(5, 0, 5).below();
        BlockPos target = floor.above();
        // entities (the drop) are only seen in loaded entity sections: keep the chunk loaded for the test
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(target);
        interior.setChunkForced(chunk.x, chunk.z, true);
        h.assertTrue(interior.getBlockState(target).isAir(), "free spot in the interior");
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            ItemStack stack = new ItemStack(ModItems.TANK.get(), 4);
            sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            UseOnContext ctx = new UseOnContext(interior, sp, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
            InteractionResult res = stack.useOn(ctx);
            h.assertTrue(!res.consumesAction() && interior.getBlockState(target).isAir(), "placing a tank in the interior fails: " + res);
            h.assertTrue(stack.getCount() == 4, "the item is kept");
            h.assertTrue(ModBlocks.TANK.get().defaultBlockState().is(ModTags.NOT_IN_TANK_INTERIOR), "tank in #bettacraft:not_in_tank_interior");
            // the same item works outside
            h.setBlock(new BlockPos(10, 0, 10), net.minecraft.world.level.block.Blocks.STONE);
            BlockPos outside = h.absolutePos(new BlockPos(10, 1, 10));
            UseOnContext ok = new UseOnContext(h.getLevel(), sp, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(outside.below()).add(0, 0.5, 0), Direction.UP, outside.below(), false));
            h.assertTrue(stack.useOn(ok).consumesAction() && h.getLevel().getBlockState(outside).getBlock() instanceof TankBlock, "outside it is placed");
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        // set without the item (commands, structure tools): broken again and dropped
        interior.setBlockAndUpdate(target, ModBlocks.TANK.get().defaultBlockState());
        h.runAfterDelay(3, () -> {
            h.assertTrue(interior.getBlockState(target).isAir(), "a tank set into the interior is removed: " + interior.getBlockState(target));
            List<ItemEntity> drops = interior.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(8));
            h.assertTrue(drops.stream().anyMatch(i -> i.getItem().is(ModItems.TANK.get())), "and dropped as an item: "
                    + interior.getEntities((Entity) null, new AABB(target).inflate(8), x -> true));
            drops.forEach(Entity::discard);
            interior.setChunkForced(chunk.x, chunk.z, false);
            h.succeed();
        });
    }
}
