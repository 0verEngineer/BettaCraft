package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.MobCaptures;
import org.overengineer.bettacraft.mobs.GlassMobData;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.overengineer.bettacraft.mobs.MobJarItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

/**
 * M31 part d: mobs in glass. The mob jar takes only a weakened mob of the list (with a cooldown); in the tank the mob is
 * shrunk, harmless (no target, no burning in daylight under an open top, the player next to it unhurt) and marked: killed by
 * a player it drops nothing and gives no experience, while the same kind outside does. A zombie under the water of an
 * aquarium becomes a drowned, in a terrarium it stays; two fed drowned have a baby that grows up, spiders a smaller
 * spiderling; a bait on an open tank lures a zombie next to it in (a lid refuses the bait); with EnderIO (-Ppower) a soul
 * vial puts its spider in.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobsInGlassGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private MobsInGlassGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** A dry tank: a terrarium. */
    static TankBlockEntity terrarium(GameTestHelper h, int x, int sy) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 2, sy, 2, 0);
        TerrariumMode m = TankClimate.refreshMode(h.getLevel(), c);
        if (m != TerrariumMode.TERRARIUM) throw new GameTestAssertException("no terrarium: " + m);
        return c;
    }

    /** A full tank: an aquarium. */
    private static TankBlockEntity aquarium(GameTestHelper h, int x) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, 2, 2, 2, 2, 8000);
        TerrariumMode m = TankClimate.refreshMode(h.getLevel(), c);
        if (m != TerrariumMode.AQUARIUM) throw new GameTestAssertException("no aquarium: " + m);
        return c;
    }

    /** A weakened mob of this type caught in a full jar. */
    private static ItemStack jar(GameTestHelper h, EntityType<? extends Mob> type) {
        Mob m = type.create(h.getLevel());
        if (m == null) throw new GameTestAssertException("no " + type);
        m.setHealth(1F);
        ItemStack s = MobJarItem.fillWith(m);
        m.discard();
        return s;
    }

    static Mob put(GameTestHelper h, TankBlockEntity c, EntityType<? extends Mob> type) {
        List<Entity> before = TankInhabitants.liveInhabitants(h.getLevel(), c.getTank());
        var err = TankInhabitants.insert(h.getLevel(), c, jar(h, type), type, null, 0L);
        if (err != null) throw new GameTestAssertException(type + " in: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof Mob m && e.getType() == type && !before.contains(e)) return m;
        }
        throw new GameTestAssertException("no " + type + " in the tank");
    }

    private static List<Mob> mobs(GameTestHelper h, TankBlockEntity c, EntityType<?> type) {
        return TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).stream().filter(e -> e.getType() == type).map(e -> (Mob) e).toList();
    }

    // ------------------------------------------------------------------ catching and admission

    /**
     * The jar refuses a healthy zombie (and then cools down) and a creeper, catches the zombie below a quarter of its health;
     * the tank shrinks it. The drowned needs water, the spider land.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mobJarCatchesWeakMobsAndTanksShrinkThem(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity dry = terrarium(h, 2, 2);
        TankBlockEntity wet = aquarium(h, 14);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        MobJarItem item = ModItems.MOB_JAR.get();
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 8));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        h.assertTrue(item.interactLivingEntity(p.getMainHandItem(), p, z, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "a healthy zombie stays out");
        h.assertTrue(p.getCooldowns().isOnCooldown(item), "the jar cools down after a try");
        p.getCooldowns().removeCooldown(item);
        var creeper = h.spawn(EntityType.CREEPER, new BlockPos(10, 2, 8));
        creeper.setHealth(1F);
        h.assertTrue(item.interactLivingEntity(p.getMainHandItem(), p, creeper, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "no creeper in glass");
        h.assertFalse(GlassMobs.isAllowed(EntityType.CREEPER) || GlassMobs.isAllowed(EntityType.ENDERMAN) || GlassMobs.isAllowed(EntityType.WITHER_SKELETON),
                "creeper, enderman and wither skeleton are not allowed");
        z.setHealth(4F);
        h.assertTrue(item.interactLivingEntity(p.getMainHandItem(), p, z, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "a weak zombie goes in");
        ItemStack full = p.getMainHandItem();
        h.assertTrue(MobJarItem.containedType(full) == EntityType.ZOMBIE && z.isRemoved(), "the jar holds the zombie");
        h.assertTrue(TankInhabitants.insert(level, dry, full, EntityType.ZOMBIE, p, 0L) == null, "the zombie goes into the terrarium");
        QuestAdv.check(h, p, "mob_in_glass"); // M45
        Mob in = mobs(h, dry, EntityType.ZOMBIE).getFirst();
        h.assertTrue(Math.abs(in.getScale() - 0.18F) < 0.01F && in.getBbHeight() < 0.4F, "shrunk: scale " + in.getScale() + " height " + in.getBbHeight());
        h.assertTrue(GlassMobs.isGlassMob(in) && in.isPersistenceRequired(), "marked and kept");
        h.assertTrue(TankInhabitants.insert(level, dry, jar(h, EntityType.DROWNED), EntityType.DROWNED, null, 0L) != null, "a terrarium refuses the drowned");
        h.assertTrue(TankInhabitants.insert(level, wet, jar(h, EntityType.SPIDER), EntityType.SPIDER, null, 0L) != null, "an aquarium refuses the spider");
        Mob d = put(h, wet, EntityType.DROWNED);
        h.assertTrue(d.getScale() < 0.2F, "the drowned in the aquarium is small");
        Mob s = put(h, dry, EntityType.SPIDER);
        h.assertTrue(s.getBbWidth() < 0.3F, "a small spider: " + s.getBbWidth());
        h.assertFalse(ModItems.FISH_BAG.get().interactLivingEntity(new ItemStack(ModItems.FISH_BAG.get()), p, h.spawn(EntityType.DROWNED, new BlockPos(12, 2, 12)),
                InteractionHand.MAIN_HAND).consumesAction(), "the fish bag takes no drowned");
        h.succeed();
    }

    // ------------------------------------------------------------------ behaviour

    /**
     * In an open terrarium by day the zombie and the skeleton attack nothing and do not burn; the player next to the glass
     * stays unhurt; the zombie stays in its tank and walks.
     */
    @GameTest(template = EMPTY, timeoutTicks = 260)
    public static void mobsInGlassAreHarmless(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        level.setDayTime(6000);
        TankBlockEntity c = terrarium(h, 4, 1);
        Mob z = put(h, c, EntityType.ZOMBIE);
        Mob sk = put(h, c, EntityType.SKELETON);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 2, 3)));
        double x0 = z.getX(), z0 = z.getZ();
        TankData t = c.getTank();
        h.runAfterDelay(200, () -> {
            h.assertTrue(GlassMobs.openTop(level, t), "the tank is open");
            for (Mob m : List.of(z, sk)) {
                h.assertTrue(m.isAlive() && m.getTarget() == null, m.getType() + " has no target");
                h.assertFalse(m.isOnFire(), m.getType() + " does not burn in daylight in the glass");
                h.assertTrue(TankInhabitants.inside(t, m), m.getType() + " stays in the tank: " + m.position());
            }
            h.assertTrue(p.getHealth() >= p.getMaxHealth(), "the player is unhurt: " + p.getHealth());
            h.assertTrue(Math.abs(z.getX() - x0) + Math.abs(z.getZ() - z0) > 0.01 || z.getY() >= t.min().getY(), "the zombie is on its feet");
            h.succeed();
        });
    }

    /**
     * Killed by a player, a zombie outside gives experience; the same in the glass gives nothing, and a mob that left its tank
     * keeps the mark (killed by fire it drops nothing).
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mobsInGlassDropNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = terrarium(h, 14, 1);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        Zombie wild = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 20));
        wild.hurt(level.damageSources().playerAttack(p), 100F);
        Mob in = put(h, c, EntityType.ZOMBIE);
        in.hurt(level.damageSources().playerAttack(p), 100F);
        Mob out = put(h, c, EntityType.HUSK);
        TankInhabitants.release(out);
        out.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(25.5, 2, 20.5)));
        out.hurt(level.damageSources().inFire(), 100F);
        h.runAfterDelay(5, () -> {
            AABB wildBox = new AABB(h.absolutePos(new BlockPos(0, 0, 17))).inflate(5);
            h.assertTrue(!level.getEntitiesOfClass(ExperienceOrb.class, wildBox).isEmpty(), "the wild zombie gives experience");
            AABB tankBox = TankInhabitants.tankBox(c.getTank()).inflate(3);
            h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, tankBox).isEmpty(), "no experience from the glass zombie");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, tankBox).isEmpty(), "no drops from the glass zombie");
            AABB outBox = new AABB(h.absolutePos(new BlockPos(25, 2, 20))).inflate(3);
            h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, outBox).isEmpty() && level.getEntitiesOfClass(ItemEntity.class, outBox).isEmpty(),
                    "a mob that left the glass still drops nothing");
            h.assertFalse(in.isAlive() || out.isAlive(), "both died");
            h.succeed();
        });
    }

    /** A zombie in the water of an aquarium becomes a drowned (in the tank, shrunk); in a terrarium it stays a zombie. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void zombieUnderWaterBecomesDrowned(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity wet = aquarium(h, 2);
        TankBlockEntity dry = terrarium(h, 14, 2);
        Mob wz = put(h, wet, EntityType.ZOMBIE);
        Mob dz = put(h, dry, EntityType.ZOMBIE);
        wz.moveTo(wz.getX(), wet.getTank().min().getY() + 0.05, wz.getZ());
        for (int i = 0; i < GlassMobs.DROWN_TICKS / 20 + 2; i++) {
            GlassMobs.step(level, wet, 0.001);
            GlassMobs.step(level, dry, 0.001);
        }
        h.assertTrue(mobs(h, wet, EntityType.ZOMBIE).isEmpty(), "the zombie in the water is gone");
        List<Mob> drowned = mobs(h, wet, EntityType.DROWNED);
        h.assertTrue(drowned.size() == 1 && GlassMobs.isGlassMob(drowned.getFirst()) && drowned.getFirst().getScale() < 0.2F, "a small drowned instead");
        h.assertTrue(wet.getTank().inhabitants().contains(drowned.getFirst().getUUID()), "it lives in the tank");
        h.assertTrue(dz.isAlive() && mobs(h, dry, EntityType.ZOMBIE).size() == 1, "the zombie in the terrarium stays a zombie");
        h.succeed();
    }

    /** Two drowned fed rotten flesh have a baby that grows up; two spiders fed crickets a smaller spiderling. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fedPairsBreed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity wet = aquarium(h, 2);
        TankBlockEntity dry = terrarium(h, 14, 2);
        put(h, wet, EntityType.DROWNED);
        put(h, wet, EntityType.DROWNED);
        Mob s1 = put(h, dry, EntityType.SPIDER);
        put(h, dry, EntityType.SPIDER);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        BlockPos wetPos = wet.getBlockPos(), dryPos = dry.getBlockPos();
        for (int i = 0; i < 2; i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH));
            h.assertTrue(GlassMobs.handFeed(p.getMainHandItem(), level, wetPos, p, InteractionHand.MAIN_HAND).consumesAction(), "a drowned eats rotten flesh");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRICKETS.get()));
            h.assertTrue(GlassMobs.handFeed(p.getMainHandItem(), level, dryPos, p, InteractionHand.MAIN_HAND).consumesAction(), "a spider eats crickets");
        }
        GlassMobs.step(level, wet, 0.01);
        GlassMobs.step(level, dry, 0.01);
        List<Mob> drowned = mobs(h, wet, EntityType.DROWNED);
        h.assertTrue(drowned.size() == 3, "a baby drowned: " + drowned.size());
        Mob baby = drowned.stream().filter(Mob::isBaby).findFirst().orElseThrow(() -> new GameTestAssertException("no baby"));
        h.assertTrue(GlassMobs.isGlassMob(baby) && GlassMobs.data(baby).growth < 1, "the baby is a young mob in glass");
        List<Mob> spiders = mobs(h, dry, EntityType.SPIDER);
        h.assertTrue(spiders.size() == 3, "a spiderling: " + spiders.size());
        Mob young = spiders.stream().filter(m -> GlassMobs.data(m).growth < 1).findFirst().orElseThrow();
        h.assertTrue(young.getScale() < s1.getScale() * 0.7F, "the spiderling is smaller: " + young.getScale() + " / " + s1.getScale());
        GlassMobs.step(level, wet, 0.05);
        h.assertTrue(mobs(h, wet, EntityType.DROWNED).size() == 3, "no second baby right away");
        GlassMobs.step(level, wet, GlassMobs.GROW_DAYS + 0.1);
        GlassMobs.step(level, dry, GlassMobs.GROW_DAYS + 0.1);
        h.assertFalse(baby.isBaby(), "the baby grew up");
        h.assertTrue(Math.abs(young.getScale() - s1.getScale()) < 0.01F, "the spiderling grew up");
        h.assertTrue(GlassMobs.data(baby).growth >= 1 && GlassMobs.data(baby).fedAt == GlassMobData.NEVER, "grown");
        h.succeed();
    }

    /** A bait on an open one block high tank lures the zombie next to it in; on a tank with a lid the bait is refused. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void baitLuresAMobIntoAnOpenTank(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GlassMobs.clearBaits();
        TankBlockEntity open = terrarium(h, 4, 1);
        TankBlockEntity lidded = terrarium(h, 16, 1);
        for (int x = 16; x < 18; x++) for (int z = 2; z < 4; z++) h.setBlock(new BlockPos(x, 3, z), ModBlocks.TANK_LID.get().defaultBlockState());
        h.assertTrue(GlassMobs.layBait(level, lidded, new ItemStack(Items.ROTTEN_FLESH), null) != null && GlassMobs.bait(lidded) == null,
                "a lid keeps the bait off");
        h.assertTrue(GlassMobs.layBait(level, open, new ItemStack(Items.DIRT), null) == null, "dirt is no bait");
        h.assertTrue(GlassMobs.layBait(level, open, new ItemStack(Items.ROTTEN_FLESH), null) != null && GlassMobs.bait(open) != null, "rotten flesh is a bait");
        Zombie z = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        Zombie far = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 12));
        GlassMobs.lure(level, open);
        h.assertTrue(TankInhabitants.hasTankRef(z) && GlassMobs.isGlassMob(z) && z.getScale() < 0.2F, "the zombie fell in and shrank");
        h.assertTrue(TankInhabitants.inside(open.getTank(), z), "it is in the tank: " + z.position());
        h.assertTrue(GlassMobs.bait(open) == null, "one bait, one mob");
        h.assertFalse(TankInhabitants.hasTankRef(far), "the far one is still outside");
        GlassMobs.clearBaits();
        h.succeed();
    }

    /** EnderIO (-Ppower): a soul vial with a spider puts it into a terrarium and comes back empty. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void enderIoSoulVialPutsItsMobIn(GameTestHelper h) {
        if (!ModList.get().isLoaded("enderio")) {
            h.succeed();
            return;
        }
        TankBlockEntity c = terrarium(h, 4, 2);
        ItemStack vial = new ItemStack(BuiltInRegistries.ITEM.get(MobCaptures.SOUL_VIAL));
        @SuppressWarnings("unchecked")
        DataComponentType<Object> soul = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(MobCaptures.SOUL);
        if (soul == null || soul.codec() == null) throw new GameTestAssertException("no enderio:soul component");
        try {
            Object value = soul.codec().parse(NbtOps.INSTANCE, TagParser.parseTag("{entity_type:\"minecraft:spider\",entity_tag:{id:\"minecraft:spider\",Health:4.0f}}"))
                    .getOrThrow();
            vial.set(soul, value);
        } catch (Exception e) {
            throw new GameTestAssertException("soul: " + e);
        }
        h.assertTrue(TankInhabitants.containedType(vial) == EntityType.SPIDER, "the vial holds a spider");
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // creative keeps the full item
        p.setItemInHand(InteractionHand.MAIN_HAND, vial);
        var r = TankInhabitants.tryInsert(vial, h.getLevel(), c.getBlockPos(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(r.consumesAction() && mobs(h, c, EntityType.SPIDER).size() == 1, "the spider is in the tank");
        ItemStack back = p.getMainHandItem();
        h.assertTrue(BuiltInRegistries.ITEM.getKey(back.getItem()).equals(ResourceLocation.fromNamespaceAndPath("enderio", "soul_vial"))
                && MobCaptures.storedEntity(back) == null, "the vial came back empty");
        h.succeed();
    }
}
