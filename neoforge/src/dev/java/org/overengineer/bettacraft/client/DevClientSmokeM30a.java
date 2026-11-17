package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.big.BigCatching;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M30 part a client smoke ({@code -Psmoketestm30a}): a 32 x 8 x 16 sea water tank with two dolphins (interior scale 8),
 * a 12 x 4 x 6 tank with two pangasius, and the player holding a full transport tub (slower). Screenshots
 * {@code bettacraft_m30a_*}.
 */
final class DevClientSmokeM30a {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos big, small;

    DevClientSmokeM30a() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(big.getX() + 16, big.getY() + 5.5, big.getZ() - 17, 0F, 14F));
        then(260, () -> {
            int n = mc.level.getEntitiesOfClass(Dolphin.class, new net.minecraft.world.phys.AABB(big).inflate(40)).size();
            LOGGER.info("[smoketest] M30 dolphins seen by the client: {}", n);
            if (n != 2) LOGGER.error("[smoketest] M30 expected 2 dolphins on the client, got {}", n);
            shot("dolphin_tank");
        });
        then(1, () -> look(big.getX() + 16, big.getY() + 3.5, big.getZ() - 6, 0F, 6F));
        then(60, () -> shot("dolphin_tank_near"));
        then(1, () -> look(small.getX() + 6, small.getY() + 2.0, small.getZ() - 5.5, 0F, 6F));
        then(120, () -> {
            server(p -> freezePangasius(p.level().getServer().overworld()));
        });
        then(40, () -> shot("pangasius_tank"));
        // the player with a full transport tub in the hand (the slowness shows in the FOV and the attribute)
        then(1, () -> server(p -> {
            ItemStack tub = BigCatching.caught(p.level().getServer().overworld(), TankmateSpecies.PANGASIUS, NetCatching.random(p.level()));
            p.setItemInHand(InteractionHand.MAIN_HAND, tub);
            TransportTubItem.updateSlowness(p);
            LOGGER.info("[smoketest] M30 walking speed with a full tub: {}", p.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }));
        then(1, () -> {
            mc.options.hideGui = false;
            look(small.getX() + 6, small.getY() + 2.0, small.getZ() - 4.5, 0F, 20F);
        });
        then(40, () -> shot("transport_tub"));
    }

    private void freezePangasius(ServerLevel level) {
        int i = 0;
        for (TankmateEntity m : level.getEntitiesOfClass(TankmateEntity.class, new net.minecraft.world.phys.AABB(small).inflate(16), e -> e.species() == TankmateSpecies.PANGASIUS)) {
            m.snapTo(small.getX() + 3.5 + i * 5.0, small.getY() + 1.0 + i * 1.2, small.getZ() + 2.5 + i, i == 0 ? -80F : -100F, 0F);
            SmokeFreeze.freeze(m, true);
            i++;
        }
    }

    // ------------------------------------------------------------------ the scene

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, t.capacityMb(), salt);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        t.setOwner(p.getUUID());
        t.setWaterTemp(25F);
        t.setTargetTemp(25F);
        c.markTankChanged();
        return c;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        ServerLevel in = InteriorManager.interiorLevel(p.level().getServer());
        BlockPos base = p.blockPosition().offset(-16, 2, 300);
        big = base;
        small = base.offset(8, 0, -24);
        TankBlockEntity a = tank(level, p, big, 32, 8, 16, 35F);
        InteriorRegion r = InteriorManager.ensureRegion(level, a);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        for (int[] s : new int[][]{{40, 90, 10}, {200, 100, 14}, {120, 110, 8}})
            for (int x = -s[2]; x <= s[2]; x++)
                for (int z = -s[2]; z <= s[2]; z++) {
                    int h = (int) Math.round(s[2] * 1.2 * Math.max(0, 1 - (x * x + z * z) / (double) (s[2] * s[2])));
                    for (int y = 0; y < h; y++) in.setBlock(r.toWorld(s[0] + x, y, s[1] + z), Blocks.STONE.defaultBlockState(), 2);
                }
        for (int i = 0; i < 2; i++) {
            Dolphin d = EntityType.DOLPHIN.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            ItemStack tub = TransportTubItem.fillWith(d);
            d.discard();
            var err = TankInhabitants.insert(level, a, tub, EntityType.DOLPHIN, null, 0);
            if (err != null) LOGGER.error("[smoketest] M30 dolphin refused: {}", err.getString());
        }
        LOGGER.info("[smoketest] M30 dolphin tank {}x{}x{} scale {}, region {}x{}x{}", a.getTank().sizeX(), a.getTank().sizeY(), a.getTank().sizeZ(),
                a.getTank().scale(), r.sizeX(), r.sizeY(), r.sizeZ());
        if (a.getTank().scale() != 8) LOGGER.error("[smoketest] M30 the big tank should have interior scale 8");
        TankBlockEntity b = tank(level, p, small, 12, 4, 6, 0F);
        InteriorRegion rb = InteriorManager.ensureRegion(level, b);
        for (int x = 0; x < rb.sizeX(); x++)
            for (int z = 0; z < rb.sizeZ(); z++) Substrate.set(in, new BlockPos(rb.originX() + x, InteriorRegion.FLOOR_Y, rb.originZ() + z), Blocks.GRAVEL.defaultBlockState(), false);
        for (int i = 0; i < 2; i++) {
            ItemStack tub = BigCatching.caught(level, TankmateSpecies.PANGASIUS, NetCatching.random(level));
            var err = TankInhabitants.insert(level, b, tub, TankInhabitants.containedType(tub), null, 0);
            if (err != null) LOGGER.error("[smoketest] M30 pangasius refused: {}", err.getString());
        }
        LOGGER.info("[smoketest] M30 part a scene at {}", base);
    }

    // ------------------------------------------------------------------ steps

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level().getServer().overworld(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, true));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
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

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m30a_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
