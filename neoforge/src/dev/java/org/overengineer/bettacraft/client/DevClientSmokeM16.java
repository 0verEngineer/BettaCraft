package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M16 part of the dev client smoke test: a tank with a mix of water animals of other mods (whatever is loaded; with
 * {@code -Paquatic} the water animal mods of ATM10) from the front and two close-ups, the animals tab of the menu, a
 * dolphin that is refused with its message, and the "no tank inside a tank" message in the interior. Screenshots
 * {@code bettacraft_m16_*}.
 */
final class DevClientSmokeM16 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    /** Preferred animals for the mixed tank, the first ones that are loaded and accepted are used. */
    private static final List<String> MIX = List.of(
            "fishofthieves:splashtail", "alexscaves:lanternfish", "crittersandcompanions:koi_fish", "minecraft:tropical_fish",
            "fintastic:guppy", "livingthings:seahorse", "crittersandcompanions:dumbo_octopus", "aquaculture:bluegill",
            "alexscaves:sweetish_fish", "undergarden:gwibling", "naturalist:bass", "dragnbettas:betta", "crittersandcompanions:sea_bunny",
            "minecraft:axolotl", "naturalist:starfish", "fishofthieves:pondie", "deeperdarker:angler_fish", "minecraft:glow_squid",
            "naturalist:giant_isopod", "shiny:shiny_tropical_fish", "minecraft:pufferfish", "aquaculture:synodontis", "fintastic:moony");
    private static final int MAX_MIX = 16;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private int wait;

    DevClientSmokeM16() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(5, () -> look(base.getX() + 2.5, base.getY() + 1.1, base.getZ() - 3.4, 0F, 8F));
        then(120, () -> server(p -> freezeAll(p.level())));
        then(30, () -> shot("mixed_tank"));
        then(1, () -> look(base.getX() + 1.3, base.getY() + 0.9, base.getZ() - 1.3, 0F, 10F));
        then(40, () -> shot("mixed_left"));
        then(1, () -> look(base.getX() + 3.7, base.getY() + 0.9, base.getZ() - 1.3, 0F, 10F));
        then(40, () -> shot("mixed_right"));
        then(1, () -> look(base.getX() - 1.2, base.getY() + 2.9, base.getZ() - 2.2, -40F, 35F));
        then(40, () -> shot("mixed_above"));
        // the menu: animals tab with the foreign animals
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            p.teleportTo(p.level(), base.getX() + 2.5, base.getY(), base.getZ() - 2.5, java.util.Set.of(), 0F, 20F, false);
            p.getInventory().add(new ItemStack(ModItems.FISH_BAG.get()));
            TankBlockEntity c = TankMultiblock.getController(p.level(), base);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(1);
        });
        then(25, () -> mc.getToastManager().clear());
        then(5, () -> shot("menu_animals"));
        then(1, () -> mc.setScreen(null));
        // a dolphin is refused (the message in the action bar)
        then(5, () -> server(p -> {
            p.teleportTo(p.level(), base.getX() + 2.5, base.getY() - 0.2, base.getZ() - 2.6, java.util.Set.of(), 0F, 12F, false);
            Entity dolphin = EntityType.DOLPHIN.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            ItemStack bag = FishBagItem.fillWith(dolphin);
            dolphin.discard();
            p.setItemInHand(InteractionHand.MAIN_HAND, bag);
            TankInhabitants.tryInsert(bag, p.level(), base.offset(2, 0, 0), p, InteractionHand.MAIN_HAND);
        }));
        then(10, () -> shot("refused_dolphin"));
        // no tank inside a tank: in the interior, try to place a tank block
        then(40, () -> server(p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TANK.get(), 8));
            InteriorManager.enter(p, base);
        }));
        then(80, () -> {
            boolean inside = mc.level != null && mc.level.dimension() == ModDimensions.TANK_INTERIOR;
            if (!inside) LOGGER.error("[smoketest] M16 the player is not in the interior");
            mc.player.setXRot(50F);
        });
        then(10, () -> {
            var hit = mc.hitResult;
            if (hit instanceof BlockHitResult bh && mc.gameMode != null) {
                var res = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bh);
                LOGGER.info("[smoketest] M16 placing a tank in the interior: {}", res);
            } else {
                LOGGER.error("[smoketest] M16 no block to click in the interior: {}", hit);
            }
        });
        then(10, () -> mc.getToastManager().clear());
        then(4, () -> shot("interior_no_tank"));
        then(20, () -> {
            var hit = mc.hitResult;
            boolean placed = hit instanceof BlockHitResult bh && mc.level.getBlockState(bh.getBlockPos().relative(bh.getDirection())).is(ModBlocks.TANK.get());
            LOGGER.info("[smoketest] M16 a tank in the interior after the click: {}", placed ? "PLACED (wrong)" : "none (ok)");
            if (placed) LOGGER.error("[smoketest] M16 a tank was placed inside a tank");
        });
        then(1, () -> server(InteriorManager::leave));
        then(40, () -> mc.options.hideGui = true);
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        base = p.blockPosition().offset(0, 2, 330);
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 3; z++) for (int x = 0; x < 5; x++) blocks.add(base.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, base);
        TankInteractions.changeWater(level, c, c.getTank().capacityMb() - 2000);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.markTankChanged();
        for (int x = 0; x < 5; x++) {
            level.setBlockAndUpdate(base.offset(x, 2, 1), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < 4).setValue(TankLightBlock.LIT, true));
        }
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        List<String> in_ = new ArrayList<>();
        for (String id : MIX) {
            if (in_.size() >= MAX_MIX) break;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
            if (type == null || TankInhabitants.accepts(type, null) != TankInhabitants.Acceptance.OK) continue;
            ItemStack stack = container(level, type);
            if (stack.isEmpty()) continue;
            Component err = TankInhabitants.insert(level, c, stack, type, p, 0);
            if (err != null) LOGGER.info("[smoketest] M16 {} not in the mixed tank: {}", id, err.getString());
            else in_.add(id);
        }
        LOGGER.info("[smoketest] M16 scene at {}: {} kinds in the mixed tank: {}", base, in_.size(), in_);
    }

    /** The bucket of this animal (dwarf gourami data for the Fintastic gourami), else our fish bag. */
    private static ItemStack container(ServerLevel level, EntityType<?> type) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.MobBucketItem && TankInhabitants.containedType(new ItemStack(item)) == type && item != Items.AIR) {
                return org.overengineer.bettacraft.command.DevSmokeM7.foreignBucket(item);
            }
        }
        Entity e = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (e == null) return ItemStack.EMPTY;
        ItemStack bag = FishBagItem.fillWith(e);
        e.discard();
        return bag;
    }

    private void freezeAll(ServerLevel level) {
        TankBlockEntity c = TankMultiblock.getController(level, base);
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (e instanceof Mob m) {
                SmokeFreeze.freeze(m, true);
                m.setDeltaMovement(Vec3.ZERO);
                n++;
            }
        }
        LOGGER.info("[smoketest] M16 {} animals alive in the mixed tank before the pictures: {}", n,
                TankInhabitants.liveInhabitants(level, c.getTank()).stream().map(e -> e.getType().toShortString()).sorted().toList());
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

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m16_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
