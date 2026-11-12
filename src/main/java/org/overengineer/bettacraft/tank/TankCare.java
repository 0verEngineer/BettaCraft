package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.equipment.HeaterItem;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Server-side care simulation of a tank controller: water temperature, inhabitant reconciliation, feeding and the
 * tank log (PLAN 5.5, 6.1, 6.3).
 */
public final class TankCare {
    private static final int INTERVAL = 20;
    private static final int RECONCILE_INTERVAL = 40;
    /** One food item (pellet or larvae portion) feeds up to this many fish. */
    public static final int FISH_PER_FOOD = 4;

    private TankCare() {}

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !be.isController()) return;
        long now = level.getGameTime();
        if ((now + pos.asLong()) % INTERVAL != 0) return;
        TankData tank = be.getTank();
        long dt = be.lastCareTick < 0 ? INTERVAL : Math.max(0, now - be.lastCareTick);
        if (!ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) dt = Math.min(dt, INTERVAL * 5L);
        be.lastCareTick = now;
        updateTemperature(server, be, dt / 24000.0);
        if ((now / INTERVAL) % (RECONCILE_INTERVAL / INTERVAL) == 0) {
            TankInhabitants.reconcile(server, be);
        }
    }

    // ------------------------------------------------------------------ temperature

    public static float ambient(ServerLevel level, TankData tank) {
        BlockPos c = BlockPos.containing(TankInhabitants.tankBox(tank).getCenter());
        float biome = level.getBiome(c).value().getBaseTemperature();
        float t = TankTemperature.ambientFromBiome(biome);
        if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()) {
            t += TankTemperature.dayNightOffset(level.getDayTime());
        }
        return t;
    }

    public static double heaterBlocks(TankData tank) {
        double sum = 0;
        var c = tank.heaters();
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).getItem() instanceof HeaterItem h) sum += h.capacityBlocks();
        }
        return sum;
    }

    public static void updateTemperature(ServerLevel level, TankBlockEntity be, double dtDays) {
        TankData tank = be.getTank();
        float ambient = ambient(level, tank);
        float eq = TankTemperature.equilibrium(ambient, tank.targetTemp(), heaterBlocks(tank), tank.waterBlocks());
        double tau = ServerConfig.getOr(ServerConfig.TEMPERATURE_TAU_DAYS, 0.1);
        float next = TankTemperature.approach(tank.waterTemp(), eq, dtDays, tau);
        if (Math.abs(next - tank.waterTemp()) > 1e-4) {
            tank.setWaterTemp(next);
            be.setChanged();
        }
    }

    // ------------------------------------------------------------------ geometry

    /** Water volume (inside the glass) as box; height from the water volume of the cuboid. */
    public static AABB waterBox(TankData tank) {
        BlockPos mn = tank.min(), mx = tank.max();
        double area = (double) tank.sizeX() * tank.sizeZ();
        double h = area <= 0 ? 0 : tank.waterMb() / (area * TankData.MB_PER_BLOCK);
        double wall = 1.0 / 16.0;
        return new AABB(mn.getX() + wall, mn.getY() + wall, mn.getZ() + wall,
                mx.getX() + 1 - wall, mn.getY() + Math.max(wall * 2, h), mx.getZ() + 1 - wall);
    }

    /** Positions directly above the top layer of the tank. */
    public static List<BlockPos> aboveTop(TankData tank) {
        List<BlockPos> out = new ArrayList<>();
        int top = tank.max().getY();
        for (long l : tank.members()) {
            if (BlockPos.getY(l) == top) out.add(BlockPos.of(l).above());
        }
        return out;
    }

    @Nullable
    public static FeederBlockEntity findFeeder(Level level, TankData tank) {
        for (BlockPos p : aboveTop(tank)) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof FeederBlockEntity f) return f;
        }
        return null;
    }

    /** Number of lights on the tank and how many of them are lit: {total, lit}. */
    public static int[] lights(Level level, TankData tank) {
        int total = 0, lit = 0;
        for (BlockPos p : aboveTop(tank)) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankLightBlock) {
                total++;
                if (s.getValue(TankLightBlock.LIT)) lit++;
            }
        }
        return new int[]{total, lit};
    }

    // ------------------------------------------------------------------ feeding

    public static List<BettaEntity> bettas(ServerLevel level, TankData tank) {
        List<BettaEntity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, tank)) if (e instanceof BettaEntity b) out.add(b);
        return out;
    }

    /**
     * Feeds the hungriest bettas with up to {@code maxItems} items (one item per {@link #FISH_PER_FOOD} fish).
     * Returns the number of items used (0 if there is no betta).
     */
    public static int feed(ServerLevel level, TankBlockEntity controller, ItemStack food, int maxItems) {
        List<BettaEntity> fish = bettas(level, controller.getTank());
        if (fish.isEmpty() || maxItems <= 0) return 0;
        fish.sort(Comparator.comparingDouble((BettaEntity b) -> b.health().daysSinceFed()).reversed());
        int items = Math.min(maxItems, (fish.size() + FISH_PER_FOOD - 1) / FISH_PER_FOOD);
        int fed = Math.min(fish.size(), items * FISH_PER_FOOD);
        for (int i = 0; i < fed; i++) fish.get(i).health().feed();
        log(level, controller, "log.bettacraft.fed", String.valueOf(fed), "@" + food.getDescriptionId());
        return items;
    }

    /** Right-click on the tank with betta food: one item, food sinks down, bettas swim to it. */
    public static ItemInteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, Vec3 hit) {
        if (!stack.is(ModTags.BETTA_FOOD)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) return ItemInteractionResult.CONSUME;
        TankData tank = controller.getTank();
        List<BettaEntity> fish = bettas(server, tank);
        if (fish.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.no_bettas"), true);
            return ItemInteractionResult.CONSUME;
        }
        feed(server, controller, stack, 1);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        AABB water = waterBox(tank);
        Vec3 spot = new Vec3(Math.max(water.minX, Math.min(water.maxX, hit.x)), water.maxY - 0.05,
                Math.max(water.minZ, Math.min(water.maxZ, hit.z)));
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)), spot.x, spot.y, spot.z, 10, 0.15, 0.02, 0.15, 0.02);
        server.sendParticles(ParticleTypes.BUBBLE, spot.x, spot.y - 0.3, spot.z, 6, 0.2, 0.2, 0.2, 0.01);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.6F);
        long until = level.getGameTime() + 120;
        for (BettaEntity b : fish) b.setFoodTarget(spot, until);
        return ItemInteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ log

    /** Log argument for an entity: its custom name or "@" + translation key of its type. */
    public static String nameArg(Entity e) {
        return e.hasCustomName() ? e.getCustomName().getString() : "@" + e.getType().getDescriptionId();
    }

    public static void log(ServerLevel level, TankBlockEntity controller, String key, String... args) {
        controller.getTank().addLog(level.getGameTime(), key, args);
        controller.setChanged();
    }

    /** Turns a log argument back into a component (see {@link #nameArg}). */
    public static Component argComponent(String arg) {
        return arg.startsWith("@") ? Component.translatable(arg.substring(1)) : Component.literal(arg);
    }
}
