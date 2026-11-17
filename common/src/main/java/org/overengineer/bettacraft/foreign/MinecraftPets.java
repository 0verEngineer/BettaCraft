package org.overengineer.bettacraft.foreign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;

/**
 * M39 part c (optional feature #25, question H3): the peaceful animals of Minecraft as pets in terraria and paludaria
 * ({@code features.foreignAnimals.vanillaPets}). They are foreign animals with a terrarium profile ({@code minecraft_*.json}):
 * our walker moves them instead of their AI, our care feeds them, our breeding breeds them, the terrarium box (or the large net
 * and the tub for the big ones) catches them - at their real size, as the land animals of the other mods (part b), the big
 * ones by the size rule of the big animals. This class adds what is special to them:
 * <ul>
 *     <li><b>Needs</b> ({@link ForeignProfile.Need}, any profile can use them): flowers, grass and a beehive standing on the land of
 *     the interior (scanned every 10 s while such an animal lives in the tank). A missing one is a problem in the menu and the log
 *     and stops the breeding; a grazer ({@code grazes}) lives from its grass or flowers (fed while they are there).</li>
 *     <li><b>No farm</b> (as the mobs in glass of M31, question C4): every Minecraft pet that lives in a tank is marked
 *     ({@link #PET}, in its save data, so the mark goes with it out of the tank): it drops nothing and gives no experience when it
 *     dies, whatever kills it; shears, buckets, bowls and brushes do nothing on it; a chicken lays no eggs in its tank (only the
 *     eggs of our breeding in the sand, which hatch). Their young are born in the tank and so marked too.</li>
 *     <li><b>Bait</b>: their food by hand on a tank with an open top that no animal in it eats is laid as bait for a day (not saved);
 *     every 5 s a pet within 12 blocks that eats it walks there, and one that reaches an open top within a jump goes in (by the
 *     same rules as from a box). The fliers (a lid) and the big ones (net and tub) do not come by bait.</li>
 * </ul>
 */
public final class MinecraftPets {
    /** The mark of a Minecraft animal that lived in one of our tanks (persistent data): no drops, no experience. */
    public static final String PET = "bettacraft:pet";
    /** Ticks between two scans of the land for the needs. */
    public static final long SCAN_TICKS = 200;
    /** Land columns (interior blocks) with flowers / a beehive the needs want; the share of the land columns with grass. */
    public static final int FLOWERS_MIN = 3, HIVES_MIN = 1;
    public static final double GRASS_SHARE = 0.1;
    /** A chicken in a tank lays no eggs: its egg timer is held at least this high. */
    public static final int NO_EGG_TICKS = 6000;
    /** Ticks a bait lasts, blocks a pet looks for it. */
    public static final long BAIT_TICKS = 24000L;
    public static final double LURE_RANGE = 12.0;

    private static final Map<TankBlockEntity, Scan> SCANS = new WeakHashMap<>();
    private static final Map<TankBlockEntity, Bait> BAITS = new WeakHashMap<>();

    /**
     * What stands on the land of a tank: land columns, columns with flowers, with grass, with a beehive (interior blocks).
     * {@code complete} is false while some interior chunks were not loaded (then the needs count as met).
     */
    public record Scan(int land, int flowers, int grass, int hives, long time, boolean complete) {
        public static final Scan NONE = new Scan(0, 0, 0, 0, Long.MIN_VALUE, false);

        public boolean has(ForeignProfile.Need n) {
            if (!complete) return true; // not known yet: nothing is missing
            return switch (n) {
                case FLOWERS -> flowers >= FLOWERS_MIN;
                case GRASS -> land > 0 && grass >= Math.max(1, Math.ceil(land * GRASS_SHARE));
                case HIVE -> hives >= HIVES_MIN;
            };
        }
    }

    /** A bait on an open tank: the item and until when it lures. */
    public record Bait(ItemStack item, long until) {}

    private MinecraftPets() {}

    /** The switch of the Minecraft pets (it needs the land animals of other mods and the terrariums). */
    public static boolean enabled() {
        return Features.enabled(Feature.VANILLA_PETS) && ForeignTerrarium.enabled();
    }

    /** Whether this animal is marked as a Minecraft pet (it lived in a tank or was born there). */
    public static boolean marked(Entity e) {
        return org.overengineer.bettacraft.platform.Platform.get().persistentData(e).getBooleanOr(PET, false);
    }

    public static void mark(Entity e) {
        if (!marked(e)) org.overengineer.bettacraft.platform.Platform.get().persistentData(e).putBoolean(PET, true);
    }

    // ------------------------------------------------------------------ needs

    /** The scan of the land of the tank, at most {@link #SCAN_TICKS} old (scans now if it is older). */
    public static Scan scan(ServerLevel level, TankBlockEntity c) {
        long now = level.getGameTime();
        Scan s = SCANS.get(c);
        if (s == null || now - s.time() >= SCAN_TICKS || now < s.time()) {
            Scan fresh = scanNow(level.getServer(), c.getTank(), now);
            if (fresh.complete() || s == null) s = fresh;
            else s = new Scan(s.land(), s.flowers(), s.grass(), s.hives(), now - SCAN_TICKS + 20, s.complete()); // again in a second
            SCANS.put(c, s);
        }
        return s;
    }

    /** Forgets the scan of a tank (GameTests after changing its interior). */
    public static void rescan(TankBlockEntity c) {
        SCANS.remove(c);
    }

    /**
     * Every land column of the interior (above the water): flowers on it (or on the block above its top), grass (a grass block
     * as its top, grass or ferns on it) and a beehive or bee nest (its top, or standing on it).
     */
    static Scan scanNow(MinecraftServer server, TankData t, long now) {
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (r == null || !r.built() || interior == null) return new Scan(0, 0, 0, 0, now, false);
        double water = t.waterMb() <= 0 ? -1 : TankCare.waterLocalHeight(t, r.scale());
        int land = 0, flowers = 0, grass = 0, hives = 0;
        boolean complete = true;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < r.sizeZ(); lz++) {
            for (int lx = 0; lx < r.sizeX(); lx++) {
                if (!r.inShape(lx, 0, lz)) continue;
                int wx = r.originX() + lx, wz = r.originZ() + lz;
                if (interior.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) {
                    complete = false;
                    continue;
                }
                int top16 = InteriorGround.top16(server, r, lx, lz);
                if (water >= 0 && top16 / 16.0 <= water + 0.05) continue;
                land++;
                // the block it stands on (the floor of the interior for an empty column) and the one on that ground
                int ly = Math.floorDiv(top16 - 1, 16);
                p.set(wx, InteriorRegion.BASE_Y + ly, wz);
                BlockState top = interior.getBlockState(p);
                BlockState above = ly + 1 < r.sizeY() ? interior.getBlockState(p.move(0, 1, 0)) : Blocks.AIR.defaultBlockState();
                if (top.is(BlockTags.FLOWERS) || above.is(BlockTags.FLOWERS)) flowers++;
                if (top.is(Blocks.GRASS_BLOCK) || grass(above) || grass(top)) grass++;
                if (top.is(BlockTags.BEEHIVES) || above.is(BlockTags.BEEHIVES)) hives++;
            }
        }
        return new Scan(land, flowers, grass, hives, now, complete);
    }

    private static boolean grass(BlockState s) {
        return s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN);
    }

    /** The needs of this land part its tank does not meet now (none while the scan is not complete). */
    public static List<ForeignProfile.Need> missing(ServerLevel level, TankBlockEntity c, ForeignProfile.Land l) {
        if (l.needs().isEmpty()) return List.of();
        Scan s = scan(level, c);
        List<ForeignProfile.Need> out = new ArrayList<>();
        for (ForeignProfile.Need n : l.needs()) if (!s.has(n)) out.add(n);
        return out;
    }

    // ------------------------------------------------------------------ the care step

    /**
     * One care step (every second): the Minecraft pets are marked and the chickens lay no eggs; every animal with needs is
     * told in the log what it misses (once a day), a grazer is fed while its grass or flowers are there; the bait lures.
     */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !ForeignTerrarium.enabled()) return;
        if (level.getGameTime() % 100 < 20) lure(level, c);
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty() || ForeignProfiles.all().isEmpty()) return;
        long now = level.getGameTime();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            EntityType<?> type = e.getType();
            if (ForeignProfiles.pet(type)) {
                mark(e);
                if (e instanceof Chicken ch && ch.eggTime < NO_EGG_TICKS) ch.eggTime = NO_EGG_TICKS; // no egg farm
            }
            ForeignProfile p = ForeignProfiles.cared(e);
            ForeignProfile.Land l = p != null ? p.land() : null;
            if (l == null || l.needs().isEmpty() || !(e instanceof LivingEntity le)) continue;
            List<ForeignProfile.Need> missing = missing(level, c, l);
            for (ForeignProfile.Need n : missing) {
                if (c.logDue("pet_need:" + type.getDescriptionId() + ":" + n.key(), now, 24000)) {
                    TankCare.log(level, c, "log.bettacraft.terrarium.problem", TankCare.nameArg(e), "@" + n.problemKey());
                }
            }
            if (l.grazes() && grazed(l, missing)) {
                BettaHealth h = ForeignCare.health(le);
                if (h.daysSinceFed() >= 0.25) {
                    h.feed();
                    ForeignCare.setHealth(le, h);
                }
            }
        }
    }

    /** Whether a grazer finds its food (the grass or flowers of its needs) in the tank. */
    static boolean grazed(ForeignProfile.Land l, List<ForeignProfile.Need> missing) {
        for (ForeignProfile.Need n : l.needs()) {
            if ((n == ForeignProfile.Need.GRASS || n == ForeignProfile.Need.FLOWERS) && !missing.contains(n)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ no farm

    /** Whether this animal counts as a pet for the drops: marked, or a Minecraft pet in a tank right now. */
    public static boolean noDrops(Entity e) {
        return marked(e) || TankInhabitants.hasTankRef(e) && ForeignProfiles.pet(e.getType());
    }




    /** The items that would take something from an animal (wool, milk, stew, a scute). */
    public static boolean farmTool(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.ShearsItem || stack.is(Items.BUCKET) || stack.is(Items.BOWL)
                || stack.getItem() instanceof net.minecraft.world.item.BrushItem;
    }

    // ------------------------------------------------------------------ bait

    /**
     * Food by hand on a tank that no animal in it ate ({@code ForeignFood} comes first): on a tank with an open top the item is laid
     * as bait if some Minecraft pet eats it (a message). PASS otherwise.
     */
    public static InteractionResult handBait(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server) || !enabled() || !lures(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!org.overengineer.bettacraft.mobs.GlassMobs.openTop(server, c.getTank())) {
            player.displayClientMessage(Component.translatable("message.bettacraft.glass.bait_lid"), true);
            return InteractionResult.CONSUME;
        }
        BAITS.put(c, new Bait(stack.copyWithCount(1), server.getGameTime() + BAIT_TICKS));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        TankCare.log(server, c, "log.bettacraft.pet.bait", "@" + stack.getItem().getDescriptionId());
        player.displayClientMessage(Component.translatable("message.bettacraft.pet.bait", stack.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    /** Lays a bait without a player (GameTests). */
    public static void layBait(ServerLevel level, TankBlockEntity c, ItemStack stack) {
        BAITS.put(c, new Bait(stack.copyWithCount(1), level.getGameTime() + BAIT_TICKS));
    }

    @Nullable
    public static Bait bait(TankBlockEntity c) {
        return BAITS.get(c);
    }

    /** GameTests: forget the baits and scans. */
    public static void clear() {
        BAITS.clear();
        SCANS.clear();
    }

    /** Whether some Minecraft pet that comes by bait eats this item. */
    public static boolean lures(ItemStack stack) {
        for (var e : ForeignProfiles.all().entrySet()) {
            if (luredBy(e.getKey(), stack)) return true;
        }
        return false;
    }

    /** Whether a pet of this type comes to this bait: a Minecraft pet for the box (no flier, no big one) whose profile food it is. */
    static boolean luredBy(EntityType<?> type, ItemStack stack) {
        if (!ForeignProfiles.pet(type)) return false;
        ForeignProfile p = ForeignProfiles.of(type);
        if (p == null || p.land() == null || !p.land().box() || p.land().lid() || p.land().big()) return false;
        return ForeignCare.eats(p, null, stack, null);
    }

    /**
     * Luring (every 5 s while a bait lies on an open tank): the pets that bait lures within {@link #LURE_RANGE} walk to the tank; one
     * that reaches an open top (within a jump) goes in by the rules of the box. One bait, one animal.
     */
    public static void lure(ServerLevel level, TankBlockEntity c) {
        Bait b = BAITS.get(c);
        if (b == null) return;
        if (level.getGameTime() > b.until() || !enabled()) {
            BAITS.remove(c);
            return;
        }
        TankData t = c.getTank();
        List<BlockPos> open = new ArrayList<>();
        for (BlockPos top : t.openTops()) if (!TankWater.covers(level, top) && level.isLoaded(top)) open.add(top);
        if (open.isEmpty()) return;
        AABB box = TankInhabitants.tankBox(t);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box.inflate(LURE_RANGE), x -> x.isAlive() && !TankInhabitants.hasTankRef(x) && !x.isPassenger())) {
            if (!luredBy(m.getType(), b.item())) continue;
            BlockPos top = nearest(open, m.position());
            double dx = m.getX() - (top.getX() + 0.5), dz = m.getZ() - (top.getZ() + 0.5);
            boolean reach = dx * dx + dz * dz <= 1.6 * 1.6 && m.getY() + 1.25 >= top.getY();
            if (reach) {
                if (admit(level, c, m) == null) {
                    BAITS.remove(c);
                    level.playSound(null, top, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 0.6F, 1.4F);
                    TankCare.log(level, c, "log.bettacraft.pet.lured", TankCare.nameArg(m));
                    return;
                }
                continue;
            }
            m.getNavigation().moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 1.0);
        }
    }

    /**
     * Puts a live pet into a tank by the rules of the terrarium box (the tank's mode, the climate, the stocking, the entity limit):
     * the animal goes into a box and from the box into the tank. Returns the error, or null when it went in. On an error it stays.
     */
    @Nullable
    public static Component admit(ServerLevel level, TankBlockEntity c, Mob m) {
        if (TankInhabitants.accepts(m.getType(), m) != TankInhabitants.Acceptance.OK) return TankInhabitants.rejection(TankInhabitants.accepts(m.getType(), m), m.getType());
        Component no = TerrariumAnimals.refusal(level, c, m.getType());
        if (no != null) return no;
        ItemStack boxed = TerrariumBoxItem.fillWith(m);
        CompoundTag saved = TerrariumBoxItem.storedEntity(boxed);
        double x = m.getX(), y = m.getY(), z = m.getZ();
        m.discard(); // the same animal (UUID) comes in from the box
        Component err = TankInhabitants.insert(level, c, boxed, m.getType(), null, 0);
        if (err != null && saved != null) { // back where it stood
            Entity back = EntityType.loadEntityRecursive(saved, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, e -> {
                e.snapTo(x, y, z, e.getYRot(), e.getXRot());
                return e;
            });
            if (back != null) level.addFreshEntity(back);
        }
        return err;
    }

    private static BlockPos nearest(List<BlockPos> list, net.minecraft.world.phys.Vec3 p) {
        BlockPos best = list.getFirst();
        double bd = Double.MAX_VALUE;
        for (BlockPos q : list) {
            double d = q.distToCenterSqr(p);
            if (d < bd) {
                bd = d;
                best = q;
            }
        }
        return best;
    }

    /** The registry id of a type (logs, tests). */
    static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }
}
