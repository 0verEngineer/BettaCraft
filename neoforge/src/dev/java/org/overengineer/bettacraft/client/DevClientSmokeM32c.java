package org.overengineer.bettacraft.client;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M32 part c client smoke ({@code -Pcreate -Psmoketestm32c}): a 2 x 2 x 1 tank with a Create Display Link on its back
 * that writes its values onto an oak sign next to it. Screenshot {@code bettacraft_m32c_display_link}. (A display board
 * would need Create's rotation set up as well: manual test.)
 */
final class DevClientSmokeM32c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base;
    private final List<BlockEntity> links = new ArrayList<>();

    DevClientSmokeM32c() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 2.3, base.getY() + 1.5, base.getZ() - 2.6, 0F, 8F));
        then(40, () -> server(p -> refresh()));
        then(120, () -> shot("display_link"));
        then(1, () -> mc.options.hideGui = false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        return s;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        base = p.blockPosition().offset(0, 2, 40);
        if (block("create:display_link") == Blocks.AIR) {
            LOGGER.error("[smoketest] M32c needs Create (-Pcreate)");
            return;
        }
        for (int x = -2; x <= 4; x++) for (int z = -1; z <= 3; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        List<BlockPos> blocks = List.of(base, base.east(), base.above(), base.above().east());
        for (BlockPos q : blocks) level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, base);
        t.setFront(Direction.NORTH);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(26F);
        t.setWaterQuality(0.92F);
        TankInteractions.changeWater(level, c, t.capacityMb());
        c.markTankChanged();
        // a link behind the tank writes its values onto a sign next to it
        level.setBlock(base.east().south(), with(block("create:display_link").defaultBlockState(), "facing", Direction.SOUTH), 3);
        level.setBlock(base.offset(3, 0, 0), Blocks.OAK_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, 8), 3);
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Object holder = Class.forName("org.overengineer.bettacraft.compat.create.CreateDisplays").getField("TANK_VALUES").get(null);
            Object source = holder.getClass().getMethod("get").invoke(holder);
            BlockEntity link = level.getBlockEntity(base.east().south());
            lookup.findVirtual(link.getClass(), "target", MethodType.methodType(void.class, BlockPos.class)).invoke(link, base.offset(3, 0, 0));
            link.getClass().getField("activeSource").set(link, source);
            links.add(link);
            LOGGER.info("[smoketest] M32c tank at {} with a display link", c.getBlockPos());
        } catch (Throwable e) {
            LOGGER.error("[smoketest] M32c display links", e);
        }
    }

    private void refresh() {
        try {
            for (BlockEntity link : links) {
                MethodHandles.publicLookup().findVirtual(link.getClass(), "updateGatheredData", MethodType.methodType(void.class)).invoke(link);
            }
        } catch (Throwable e) {
            LOGGER.error("[smoketest] M32c refresh", e);
        }
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m32c_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
