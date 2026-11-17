package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M15 part of the dev client smoke test: the smaller name sign (default, signScale 2 and 0.5), the domed bubble nest
 * with eggs and with larvae from above and from the side through the glass, shrimp lying flat on the floor seen from the
 * side, and the interior leave hint at the bottom left (English, German, while holding sneak + looking up, then out in
 * front of the tank). Screenshots {@code bettacraft_m15_*}.
 */
final class DevClientSmokeM15 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;
    private boolean inside;
    private Vec3 enteredAt;

    DevClientSmokeM15() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // 1. the name sign: default size, twice, half
        then(20, () -> look(base.getX() + 1.5, base.getY() + 1.6, base.getZ() - 2.6, 0F, 10F));
        then(80, () -> shot("sign"));
        then(1, () -> ClientConfig.SIGN_SCALE.set(2.0));
        then(10, () -> shot("sign_large"));
        then(1, () -> ClientConfig.SIGN_SCALE.set(0.5));
        then(10, () -> shot("sign_small"));
        then(1, () -> ClientConfig.SIGN_SCALE.set(1.0));
        // 3. the domed nest with eggs: from above, from the side at the surface, a little from below
        then(1, () -> look(base.getX() + 7.0, base.getY() + 3.3, base.getZ() + 0.6, 0F, 75F));
        then(60, () -> shot("nest_top"));
        then(1, () -> look(base.getX() + 7.0, base.getY() + 1.88, base.getZ() - 0.5, 0F, 4F));
        then(40, () -> shot("nest_side"));
        then(1, () -> look(base.getX() + 7.0, base.getY() + 1.55, base.getZ() - 0.7, 0F, -12F));
        then(40, () -> shot("nest_below"));
        then(1, () -> look(base.getX() + 6.2, base.getY() + 1.9, base.getZ() - 1.6, -15F, 6F));
        then(40, () -> shot("nest_side_far"));
        // and with larvae
        then(1, () -> look(base.getX() + 11.0, base.getY() + 1.9, base.getZ() - 0.9, 0F, 4F));
        then(40, () -> shot("nest_larvae_side"));
        then(1, () -> look(base.getX() + 11.0, base.getY() + 1.55, base.getZ() - 0.7, 0F, -12F));
        then(40, () -> shot("nest_larvae_below"));
        // shrimp on the floor seen from the side (they lie flat; head-on they only look upright through perspective)
        then(1, () -> look(base.getX() + 15.0, base.getY() + 0.3, base.getZ() - 0.9, 0F, 4F));
        then(40, () -> shot("shrimp_side"));
        then(1, () -> look(base.getX() + 15.0, base.getY() + 0.55, base.getZ() - 0.55, 0F, 22F));
        then(20, () -> shot("shrimp_closeup"));
        // 4. the interior with the leave hint at the bottom left
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            p.teleportTo(p.level(), base.getX() + 1.5, base.getY(), base.getZ() - 2.0, java.util.Set.of(), 0F, 20F, false);
            enteredAt = p.position();
            InteriorManager.enter(p, base);
        }));
        then(60, () -> {
            inside = mc.level != null && mc.level.dimension() == ModDimensions.TANK_INTERIOR;
            if (!inside) LOGGER.error("[smoketest] M15 the player is not in the interior");
            mc.player.setXRot(25F);
        });
        then(30, () -> mc.getToastManager().clear());
        then(10, () -> shot("interior_hint"));
        String lang = mc.options.languageCode;
        then(1, () -> language("de_de"));
        then(20, () -> shot("interior_hint_de"));
        then(1, () -> language(lang));
        then(1, () -> ClientConfig.INTERIOR_LEAVE_HINT.set(false));
        then(10, () -> shot("interior_nohint"));
        then(1, () -> ClientConfig.INTERIOR_LEAVE_HINT.set(true));
        // sneaking while looking at the floor: stays inside
        then(1, () -> mc.options.keyShift.setDown(true));
        then(90, () -> {
            boolean still = mc.level.dimension() == ModDimensions.TANK_INTERIOR;
            LOGGER.info("[smoketest] M15 sneaking while looking down for 90 ticks: {}", still ? "still inside (ok)" : "LEFT");
            if (!still) LOGGER.error("[smoketest] M15 sneaking while looking down took the player out");
            mc.player.setXRot(-75F);
        });
        then(30, () -> {
            mc.player.setXRot(-75F);
            shot("interior_leaving");
        });
        then(50, () -> mc.options.keyShift.setDown(false));
        then(20, () -> {
            boolean out = mc.level.dimension() != ModDimensions.TANK_INTERIOR;
            double d = enteredAt == null || mc.player == null ? -1 : mc.player.position().distanceTo(enteredAt);
            LOGGER.info("[smoketest] M15 after sneaking + looking up: {} ({} blocks from where the player entered)", out ? "out" : "STILL INSIDE", d);
            if (!out || d > 0.6) LOGGER.error("[smoketest] M15 the player did not come out in front of the tank");
            mc.player.setXRot(10F);
        });
        then(20, () -> shot("interior_left"));
        then(1, () -> mc.options.hideGui = true);
    }

    private static void place(ServerLevel level, List<BlockPos> blocks) {
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private static List<BlockPos> cuboid(BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) l.add(min.offset(x, y, z));
        return l;
    }

    private static TankBlockEntity setup(ServerLevel level, ServerPlayer p, BlockPos min, long water) {
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, water);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(27F);
        c.getTank().setTargetTemp(27F);
        return c;
    }

    private static void lights(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true));
        }
    }

    private static void sand(ServerLevel level, TankBlockEntity c) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
    }

    /** A nest with 22 eggs (or larvae) in the middle of the tank, as if a pair had spawned. */
    private static void nest(ServerLevel level, TankBlockEntity c, boolean larvae) {
        TankData t = c.getTank();
        Vec3 spot = TankBreeding.nestSpot(level, t, new Vec3(t.min().getX() + 1.0, t.max().getY() + 1, t.min().getZ() + 0.8));
        CompoundTag n = new CompoundTag();
        n.putString("phase", "eggs");
        n.putDouble("x", spot.x);
        n.putDouble("y", spot.y);
        n.putDouble("z", spot.z);
        n.putDouble("progress", larvae ? LifeStage.EGG.days() + 0.05 : 0.02);
        ListTag eggs = new ListTag();
        RandomSource r = RandomSource.create(15);
        for (int i = 0; i < 22; i++) {
            CompoundTag e = new CompoundTag();
            e.put("genome", BettaGenotype.randomDomestic(r).save());
            e.putBoolean("female", i % 2 == 0);
            eggs.add(e);
        }
        n.put("eggs", eggs);
        t.setNest(BubbleNest.load(n));
        c.markTankChanged();
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        base = p.blockPosition().offset(0, 2, 290);
        // A: the name sign (and the interior with the leave hint)
        place(level, cuboid(base, 3, 2, 1));
        TankBlockEntity a = setup(level, p, base, 5000);
        a.getTank().setName("Betta Palace");
        a.getTank().setSignVisible(true);
        a.markTankChanged();
        sand(level, a);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion ra = InteriorManager.ensureRegion(level, a);
        for (int x = 18; x <= 22; x++) for (int y = 0; y < 3; y++) in.setBlock(ra.toWorld(x, y, 10), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        // B: nest with eggs, C: nest with larvae (full tanks: the surface sits 1/16 under the top)
        BlockPos bb = base.offset(6, 0, 0), cb = base.offset(10, 0, 0);
        place(level, cuboid(bb, 2, 2, 2));
        place(level, cuboid(cb, 2, 2, 2));
        TankBlockEntity b = setup(level, p, bb, 16000), c = setup(level, p, cb, 16000);
        lights(level, bb, 2, 2, 1);
        lights(level, cb, 2, 2, 1);
        sand(level, b);
        sand(level, c);
        nest(level, b, false);
        nest(level, c, true);
        // D: shrimp on the floor in all directions
        BlockPos db = base.offset(14, 0, 0);
        place(level, cuboid(db, 2, 1, 1));
        TankBlockEntity d = setup(level, p, db, 2000);
        sand(level, d);
        TankData dt = d.getTank();
        var terrain = TankCritters.terrain(level, dt);
        float[] yaws = {0F, 45F, 90F, 135F, 180F, -135F, -90F, -45F};
        for (int i = 0; i < yaws.length; i++) {
            double x = db.getX() + 0.3 + (i % 4) * 0.45, z = db.getZ() + 0.35 + (i / 4) * 0.35;
            double y = terrain.ground(x, z);
            Critter k = new Critter(dt.nextCritterId(), CritterSpecies.NEOCARIDINA, i % 2 == 0, CritterGenes.pure(0, 0.8F), x,
                    Double.isFinite(y) ? y : db.getY() + 0.05, z);
            k.age = CritterSpecies.NEOCARIDINA.juvenileDays() + 2;
            k.yaw = yaws[i];
            k.idle = 1_000_000;
            dt.critters().add(k);
        }
        d.markTankChanged();
        TankCritters.sync(level, d);
        LOGGER.info("[smoketest] M15 scene at {}: sign tank {}, nest {} / {}, {} shrimp", base, a.getTank().name(), b.getTank().nest() != null,
                c.getTank().nest() != null, dt.critters().size());
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty() && (mc.level == null || mc.level.dimension() != ModDimensions.TANK_INTERIOR)) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m15_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
