package org.overengineer.bettacraft.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.DriveSide;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M41 client smoke ({@code -Pcreate -Psmoketestm41}): a 2 x 2 x 1 tank with the power options on and no FE cable, on two
 * cabinets; the right cabinet takes a Create shaft on its right side from a creative motor, a drive adapter on the left wall
 * of the tank has its own motor. Screenshots {@code bettacraft_m41_drive} (the scene), {@code _adapter} (close-up of the
 * adapter), {@code _goggles} (Create's goggles on the adapter: stress and what it gives the tank), {@code _cabinet} (the
 * flange on the cabinet) and {@code _cabinet_menu} (the menu with the shaft button).
 */
final class DevClientSmokeM41 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos base;

    DevClientSmokeM41() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 0.9, base.getY() + 1.6, base.getZ() - 4.2, 0F, 12F));
        then(100, () -> shot("drive"));
        then(1, () -> look(base.getX() - 1.6, base.getY() + 1.0, base.getZ() - 1.4, -30F, 14F));
        then(40, () -> shot("adapter"));
        then(1, () -> look(base.getX() + 3.4, base.getY() + 0.2, base.getZ() - 1.6, 34F, 16F));
        then(40, () -> shot("cabinet"));
        // Create's goggles on the adapter: the overlay needs the GUI
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:goggles"))))));
        then(1, () -> look(base.getX() - 1.6, base.getY() + 1.0, base.getZ() - 1.4, -30F, 14F));
        then(60, () -> shot("goggles"));
        then(1, () -> server(p -> {
            if (p.serverLevel().getBlockEntity(base.below().east()) instanceof CabinetBlockEntity cab) p.openMenu(cab, cab.getBlockPos());
        }));
        then(30, () -> shot("cabinet_menu"));
        then(1, () -> mc.setScreen(null));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState s, String property, Comparable value) {
        for (Property<?> p : s.getProperties()) if (p.getName().equals(property)) return s.setValue((Property) p, value);
        return s;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    /** A creative motor at {@code pos} whose shaft points {@code facing}, at {@code rpm}. */
    private static void motor(ServerLevel level, BlockPos pos, Direction facing, int rpm) {
        level.setBlock(pos, with(block("create:creative_motor").defaultBlockState(), "facing", facing), 3);
        BlockEntity be = level.getBlockEntity(pos);
        try {
            Field f = null;
            for (Class<?> k = be.getClass(); k != null && f == null; k = k.getSuperclass()) {
                try {
                    f = k.getDeclaredField("generatedSpeed");
                } catch (NoSuchFieldException ignored) {
                }
            }
            f.setAccessible(true);
            Object speed = f.get(be);
            for (Class<?> k = speed.getClass(); k != null; k = k.getSuperclass()) {
                for (Method m : k.getDeclaredMethods()) {
                    if (m.getName().equals("setValue") && m.getParameterCount() == 1) {
                        m.setAccessible(true);
                        m.invoke(speed, rpm);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[smoketest] M41 motor speed", e);
        }
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        base = p.blockPosition().offset(0, 3, 40);
        Block adapter = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("bettacraft", "drive_adapter"));
        if (block("create:creative_motor") == Blocks.AIR || adapter == Blocks.AIR) {
            LOGGER.error("[smoketest] M41 needs Create (-Pcreate)");
            return;
        }
        ServerConfig.POWER_HEATER.set(true);
        ServerConfig.POWER_FILTER.set(true);
        ServerConfig.POWER_LIGHT.set(true);
        Features.set(Feature.POWER, true);
        for (int x = -4; x <= 6; x++) for (int z = -2; z <= 3; z++) level.setBlock(base.offset(x, -2, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        // the cabinets, a tank on them, a filter in the right cabinet, a light on top
        for (int x = 0; x <= 1; x++) {
            level.setBlock(base.offset(x, -1, 0), ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH), 3);
        }
        List<BlockPos> blocks = List.of(base, base.east(), base.above(), base.above().east());
        for (BlockPos q : blocks) level.setBlock(q, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, base);
        t.setFront(Direction.NORTH);
        t.setOwner(p.getUUID());
        t.setWaterTemp(26F);
        t.setTargetTemp(28F);
        t.setWaterQuality(0.92F);
        t.setPowerBuffer(0);
        TankInteractions.changeWater(level, c, t.capacityMb());
        c.markTankChanged();
        if (level.getBlockEntity(base.below().east()) instanceof CabinetBlockEntity cab) {
            cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
            cab.setDriveSide(DriveSide.LEFT); // seen from the door (north) the left side is east
        }
        level.setBlock(base.above(2), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true), 3);
        level.setBlock(base.above(2).east(), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true), 3);
        level.setBlock(base.south().east(), ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.NORTH), 3);
        // the right cabinet's shaft: two shafts to a motor
        level.setBlock(base.offset(2, -1, 0), with(block("create:shaft").defaultBlockState(), "axis", Direction.Axis.X), 3);
        level.setBlock(base.offset(3, -1, 0), with(block("create:shaft").defaultBlockState(), "axis", Direction.Axis.X), 3);
        motor(level, base.offset(4, -1, 0), Direction.WEST, 64);
        // the drive adapter on the left wall, a cogwheel shaft line to its motor
        level.setBlock(base.west(), with(adapter.defaultBlockState(), "facing", Direction.WEST), 3);
        level.setBlock(base.offset(-2, 0, 0), with(block("create:shaft").defaultBlockState(), "axis", Direction.Axis.X), 3);
        motor(level, base.offset(-3, 0, 0), Direction.EAST, 64);
        LOGGER.info("[smoketest] M41 tank at {} with a drive adapter and a cabinet shaft", c.getBlockPos());
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m41_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
