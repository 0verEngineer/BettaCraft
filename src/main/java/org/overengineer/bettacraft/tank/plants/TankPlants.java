package org.overengineer.bettacraft.tank.plants;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.network.TankPlantsPayload;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * Floating plants of a tank on the server (M12): the care step (drift, growth, waste uptake, sync), placing plants
 * from items, taking them out (at a spot, thinning), and what they do for the tank (nest, breeding, feeder). The
 * plants live in {@link TankData#plants()}; the pure rules are in {@link FloatingPlantSim}.
 */
public final class TankPlants {
    /** Clients glide the plants to their new place over this many ticks (= one care step). */
    public static final int SYNC_TICKS = 20;
    /** Leaf area that counts as "plants in the tank" for the breeding bonus. */
    public static final double BONUS_FROM = 0.1;
    /** Reach of a hand taking plants out (blocks around the spot looked at, plus the leaf radius). */
    public static final double TAKE_REACH = 0.2;
    /** Reach of the aquarium net around the spot looked at (a bit wider than the hand). */
    public static final double NET_REACH = 0.3;
    /** A plant put in from an item is half grown. */
    public static final float ITEM_SIZE = 0.5F;
    /** Most leaf cover a player can still put a plant into. */
    public static final double PLACE_MAX_COVERAGE = 0.95;

    private TankPlants() {}

    // ------------------------------------------------------------------ config

    public static FloatingPlantSim.Params params() {
        return new FloatingPlantSim.Params(
                ServerConfig.getOr(ServerConfig.PLANTS_GROWTH_SPEED, 1.0),
                ServerConfig.getOr(ServerConfig.PLANTS_MAX_COVERAGE, 0.6),
                ServerConfig.getOr(ServerConfig.PLANTS_DENSE_FROM, 0.5),
                ServerConfig.getOr(ServerConfig.PLANTS_MAX_PER_TANK, 256));
    }

    // ------------------------------------------------------------------ items

    /** The item of a species (the vanilla lily pad for {@link FloatingPlantType#LILY_PAD}). */
    public static ItemStack item(FloatingPlantType type) {
        if (type == FloatingPlantType.LILY_PAD) return new ItemStack(Items.LILY_PAD);
        var item = ModItems.FLOATING_PLANTS.get(type);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    /** The species an item puts onto a tank, or null. */
    @Nullable
    public static FloatingPlantType typeOf(ItemStack stack) {
        if (stack.getItem() instanceof FloatingPlantItem p) return p.type();
        if (stack.is(Items.LILY_PAD)) return FloatingPlantType.LILY_PAD;
        return null;
    }

    // ------------------------------------------------------------------ geometry

    /** Layer that holds the surface (the bottom layer when the tank is dry: the plants lie on the floor). */
    public static int surfaceLayer(TankData t) {
        if (t.waterMb() <= 0) return t.min().getY();
        return Mth.floor(t.waterSurfaceY() - 1e-4);
    }

    public static PlantSurface surface(TankData t) {
        return PlantSurface.of(t.members(), surfaceLayer(t));
    }

    public static double coverage(TankData t) {
        return FloatingPlantSim.coverage(t.plants(), surface(t).area());
    }

    /** Dense cover (server config floatingPlants.denseCoverFrom): warning, shading, pellets on the leaves. */
    public static boolean dense(TankData t) {
        return !t.plants().isEmpty() && coverage(t) >= ServerConfig.getOr(ServerConfig.PLANTS_DENSE_FROM, 0.5);
    }

    /**
     * Where the view ray of a player meets the water surface of the tank (x, surface, z), or null when it does not (looking
     * up, from below the surface, or beside the tank's surface).
     */
    @Nullable
    public static Vec3 surfaceHit(TankData t, Vec3 eye, Vec3 look) {
        double y = t.waterMb() <= 0 ? t.min().getY() + 1.0 / 16 : t.waterSurfaceY();
        if (look.y > -1e-4 || eye.y < y) return null;
        double k = (y - eye.y) / look.y;
        if (k < 0 || k > 12) return null;
        Vec3 p = eye.add(look.scale(k));
        return surface(t).contains(p.x, p.z) ? p : null;
    }

    /**
     * Where the aquarium net takes plants out (x, surface, z): where the view ray meets the surface when it does (looking
     * down from above), otherwise the first plant the ray passes over or under inside the tank (from the side through
     * the glass, from below). Null when the ray meets no plant.
     */
    @Nullable
    public static Vec3 netTarget(TankData t, Vec3 eye, Vec3 look) {
        double y = t.waterMb() <= 0 ? t.min().getY() + 1.0 / 16 : t.waterSurfaceY();
        Vec3 at = surfaceHit(t, eye, look);
        if (at != null) {
            for (FloatingPlant p : t.plants()) if (Math.hypot(p.x - at.x, p.z - at.z) <= NET_REACH + p.radius()) return at;
        }
        FloatingPlant p = FloatingPlantSim.firstAlongRay(t.plants(), eye.x, eye.y, eye.z, look.x, look.y, look.z,
                t.min().getY(), y + 0.5, 12, NET_REACH);
        return p == null ? null : new Vec3(p.x, y, p.z);
    }

    // ------------------------------------------------------------------ light

    /**
     * Light at the surface (0..15): the brightest surface block (sky and block light), 15 under a lit tank light. A lid
     * or a full block over the tank keeps the sky out; the glass does not.
     */
    public static int light(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        int[] lights = TankCare.lights(level, t);
        if (lights[1] > 0 && c.lightPowered()) return 15;
        int layer = surfaceLayer(t), best = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long l : t.members()) {
            if (BlockPos.getY(l) != layer) continue;
            p.set(l);
            if (!level.isLoaded(p)) continue;
            best = Math.max(best, level.getMaxLocalRawBrightness(p));
            if (best >= 15) break;
        }
        return best;
    }

    // ------------------------------------------------------------------ filter jets

    /** Where the jets of the attached filters hit the surface (only while the filters run). */
    public static List<FloatingPlantSim.Jet> jets(Level level, TankBlockEntity c) {
        List<FloatingPlantSim.Jet> out = new ArrayList<>();
        if (!c.filterPowered()) return out;
        TankData t = c.getTank();
        for (FilterHost f : TankEquipment.filters(level, c)) {
            FilterFlow.Path path = null;
            if (f instanceof ExternalFilterBlockEntity e) {
                BlockState s = e.getBlockState();
                if (s.getBlock() instanceof ExternalFilterBlock) path = FilterFlow.forFilterBlock(t, e.getBlockPos(), s.getValue(ExternalFilterBlock.FACING));
            } else if (f instanceof CabinetBlockEntity cab) {
                BlockState s = cab.getBlockState();
                if (s.getBlock() instanceof CabinetBlock) path = FilterFlow.forCabinet(t, cab); // M22.4: its outlet side
            }
            if (path == null || !path.wet()) continue;
            Vec3 hit = path.submerged() ? path.nozzle() : path.impact();
            out.add(new FloatingPlantSim.Jet(hit.x, hit.z, path.inward().getStepX(), path.inward().getStepZ(), path.submerged() ? 0.6 : 1.0));
        }
        return out;
    }

    // ------------------------------------------------------------------ care step

    private static RandomGenerator random(Level level) {
        var r = level.getRandom();
        return r::nextLong;
    }

    /** One care step (every 20 ticks, {@code days} since the last one): drift, growth, waste uptake and the sync. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        List<FloatingPlant> plants = t.plants();
        if (plants.isEmpty()) return;
        if (t.waterMb() <= 0) return; // stranded on the floor: no drift, no growth
        PlantSurface surface = surface(t);
        RandomGenerator rnd = random(level);
        FloatingPlantSim.move(plants, surface, jets(level, c), rnd);
        grow(level, c, days, surface, rnd);
        c.setChanged();
        sync(level, c);
    }

    /** Growth and waste uptake for {@code days} (also used by GameTests with long steps). */
    public static FloatingPlantSim.Growth grow(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (t.plants().isEmpty() || t.waterMb() <= 0) return new FloatingPlantSim.Growth(0, 0);
        FloatingPlantSim.Growth g = grow(level, c, days, surface(t), random(level));
        c.markTankChanged();
        return g;
    }

    private static FloatingPlantSim.Growth grow(ServerLevel level, TankBlockEntity c, double days, PlantSurface surface, RandomGenerator rnd) {
        TankData t = c.getTank();
        int light = light(level, c);
        FloatingPlantSim.Env env = new FloatingPlantSim.Env(FloatingPlantSim.lightFactor(light),
                FloatingPlantSim.nutrientFactor(TankWater.qualityEnabled() ? t.waterQuality() : 0.9F), FloatingPlantSim.redTarget(light));
        int before = t.plants().size();
        FloatingPlantSim.Growth g = FloatingPlantSim.grow(t.plants(), surface, env, params(), days, rnd, t::nextPlantId);
        if (g.offshoots() > 0 && before > 0) {
            // the same measure that stops the offshoots (every plant counted full-grown): logged once when it reaches the cap
            double full = FloatingPlantSim.fullCoverage(t.plants(), surface.area());
            double cap = ServerConfig.getOr(ServerConfig.PLANTS_MAX_COVERAGE, 0.6);
            if (full >= cap && FloatingPlantSim.fullCoverage(t.plants().subList(0, before), surface.area()) < cap) {
                TankCare.log(level, c, "log.bettacraft.plants_full", String.valueOf(Math.round(Math.min(1.0, full) * 100)));
            }
        }
        double uptake = ServerConfig.getOr(ServerConfig.PLANTS_WASTE_UPTAKE, 0.04);
        if (TankWater.qualityEnabled() && uptake > 0 && g.activeArea() > 0 && t.waterQuality() < 1.0F) {
            double dq = uptake * g.activeArea() * days / Math.max(1.0, t.waterBlocks());
            t.setWaterQuality((float) Math.min(1.0, t.waterQuality() + dq));
        }
        return g;
    }

    /** Sends the plants to every player near the tank (positions glide on the client over {@link #SYNC_TICKS}). */
    public static void sync(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        byte[] data = FloatingPlantSim.encode(t.plants(), t.min().getX(), t.min().getZ());
        if (!c.plantSync.changed(data, level.getGameTime())) return; // M17: plants at rest send nothing
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(c.getBlockPos()), new TankPlantsPayload(c.getBlockPos(), data));
    }

    // ------------------------------------------------------------------ adding / removing

    /**
     * Adds a plant at (x, z) (moved inside the glass of the surface). Null when the tank is full (plant limit, or the
     * surface is covered) or has no surface.
     */
    @Nullable
    public static FloatingPlant add(TankBlockEntity c, FloatingPlantType type, double x, double z, float size, float rot) {
        TankData t = c.getTank();
        PlantSurface surface = surface(t);
        if (surface.isEmpty() || t.plants().size() >= params().maxPlants()
                || FloatingPlantSim.coverage(t.plants(), surface.area()) >= PLACE_MAX_COVERAGE) return null;
        FloatingPlant p = new FloatingPlant(t.nextPlantId(), type, x, z, size, rot);
        double[] cl = surface.clamp(x, z, FloatingPlantSim.margin(p));
        p.x = cl[0];
        p.z = cl[1];
        t.plants().add(p);
        c.markTankChanged();
        return p;
    }

    /**
     * A player uses a plant item (or a lily pad) on the tank: the plant floats where the player looks at the surface
     * (or the surface point nearest to the clicked spot). Returns false when nothing was placed (message shown).
     */
    public static boolean place(ServerLevel level, TankBlockEntity c, Player player, ItemStack stack, Vec3 clicked) {
        FloatingPlantType type = typeOf(stack);
        if (type == null) return false;
        TankData t = c.getTank();
        if (t.waterMb() <= 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.plants.no_water"), true);
            return false;
        }
        Vec3 at = surfaceHit(t, player.getEyePosition(), player.getViewVector(1.0F));
        if (at == null) at = new org.overengineer.bettacraft.tank.TankWaterVolume(t).surfacePoint(clicked, 0.3, 0.0);
        float rot = level.getRandom().nextFloat() * Mth.TWO_PI;
        double[] xz = type == FloatingPlantType.LILY_PAD ? FloatingPlantSim.awayFromPads(t.plants(), at.x, at.z, type.radius(1.0)) : new double[] {at.x, at.z};
        FloatingPlant p = add(c, type, xz[0], xz[1], type.grows() ? ITEM_SIZE : 1.0F, rot);
        if (p == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.plants.full"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, p.x, t.waterSurfaceY(), p.z, SoundEvents.LILY_PAD_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        TankCare.log(level, c, "log.bettacraft.plant_added", TankCare.nameArg(player), "@" + item(type).getDescriptionId());
        sync(level, c);
        return true;
    }

    /** Takes the plants out whose leaves are at (x, z); returns their items. */
    public static List<ItemStack> removeAt(ServerLevel level, TankBlockEntity c, double x, double z) {
        return removeAt(level, c, x, z, TAKE_REACH);
    }

    /** Takes the plants out whose leaves are within {@code reach} of (x, z); returns their items. */
    public static List<ItemStack> removeAt(ServerLevel level, TankBlockEntity c, double x, double z, double reach) {
        List<FloatingPlant> removed = FloatingPlantSim.removeNear(c.getTank().plants(), x, z, reach);
        if (removed.isEmpty()) return List.of();
        c.markTankChanged();
        sync(level, c);
        return items(removed);
    }

    /** Thins the plants out by half (lily pads stay); returns their items. Also the menu button. */
    public static List<ItemStack> thin(ServerLevel level, TankBlockEntity c) {
        List<FloatingPlant> removed = FloatingPlantSim.thin(c.getTank().plants(), random(level));
        if (removed.isEmpty()) return List.of();
        c.markTankChanged();
        sync(level, c);
        return items(removed);
    }

    /** One item per plant, stacked per species. */
    public static List<ItemStack> items(List<FloatingPlant> plants) {
        Map<FloatingPlantType, Integer> n = new EnumMap<>(FloatingPlantType.class);
        for (FloatingPlant p : plants) n.merge(p.type, 1, Integer::sum);
        List<ItemStack> out = new ArrayList<>();
        for (var e : n.entrySet()) {
            int left = e.getValue();
            ItemStack proto = item(e.getKey());
            if (proto.isEmpty()) continue;
            while (left > 0) {
                int k = Math.min(left, proto.getMaxStackSize());
                out.add(proto.copyWithCount(k));
                left -= k;
            }
        }
        return out;
    }

    /** Into the inventory, the rest is dropped at the player's feet. */
    public static void give(Player player, List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            ItemStack copy = s.copy();
            if (!player.getInventory().add(copy) && !copy.isEmpty()) player.drop(copy, false);
        }
    }

    /** Player took plants out: items, sound, message. */
    public static void taken(ServerLevel level, TankBlockEntity c, Player player, List<ItemStack> stacks, boolean thinned) {
        int n = 0;
        for (ItemStack s : stacks) n += s.getCount();
        give(player, stacks);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        player.displayClientMessage(Component.translatable(thinned ? "message.bettacraft.plants.thinned" : "message.bettacraft.plants.taken", n,
                c.getTank().plants().size()).withStyle(ChatFormatting.GREEN), true);
        if (thinned) TankCare.log(level, c, "log.bettacraft.plants_thinned", TankCare.nameArg(player), String.valueOf(n));
    }

    /** All plants of a tank that is gone (last block broken) drop as items. */
    public static void dropAll(ServerLevel level, List<FloatingPlant> plants, BlockPos at) {
        for (ItemStack s : items(plants)) {
            net.minecraft.world.Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, s);
        }
    }

    // ------------------------------------------------------------------ effects on the tank

    /** Breeding readiness bonus of the plants (server config floatingPlants.breedingBonus, from 10 % leaf cover). */
    public static double breedingBonus(TankData t) {
        if (t.plants().isEmpty()) return 1.0;
        return coverage(t) >= BONUS_FROM ? Math.max(1.0, ServerConfig.getOr(ServerConfig.PLANTS_BREEDING_BONUS, 1.15)) : 1.0;
    }

    /**
     * Food that stays on the leaves when the feeder feeds {@code used} portions under a dense cover: from denseCoverFrom
     * up to a full cover the share grows to floatingPlants.pelletsOnLeaves. Rounded at random.
     */
    public static int pelletsOnLeaves(TankData t, int used, RandomGenerator rnd) {
        if (used <= 0 || t.plants().isEmpty()) return 0;
        double from = ServerConfig.getOr(ServerConfig.PLANTS_DENSE_FROM, 0.5);
        double share = ServerConfig.getOr(ServerConfig.PLANTS_PELLETS_ON_LEAVES, 0.25);
        double cov = Math.min(1.0, coverage(t));
        if (share <= 0 || cov < from) return 0;
        double f = from >= 1 ? 1 : (cov - from) / (1 - from);
        double expected = used * share * Math.max(0.2, f);
        int whole = (int) Math.floor(expected);
        return whole + (rnd.nextDouble() < expected - whole ? 1 : 0);
    }

    /** Centre of the plant cluster the male likes best (at the surface), or null without plants. */
    @Nullable
    public static Vec3 nestCluster(TankData t, Vec3 near) {
        double[] c = FloatingPlantSim.nestCluster(t.plants(), near.x, near.z);
        return c == null ? null : new Vec3(c[0], near.y, c[1]);
    }

    /** Light quality for the menu: 0 dark, 1 weak, 2 good, 3 strong (red root floater turns red). */
    public static int lightClass(int light) {
        return light < 9 ? 0 : light < 12 ? 1 : light < 14 ? 2 : 3;
    }
}
