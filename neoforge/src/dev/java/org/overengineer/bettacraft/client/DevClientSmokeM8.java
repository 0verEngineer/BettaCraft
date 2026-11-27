package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M8 part of the dev client smoke test: the overlays of the installed info mods (Jade, The One Probe, WTHIT) on a tank
 * (normal and sneaking/extended), on a betta behind the glass, on an external filter and on a cabinet. The screenshot
 * names carry the loaded info mods, e.g. {@code bettacraft_m8_top_tank.png}; run once per mod with the other two
 * switched off ({@code -PnoJade -PnoWthit} etc.) for clean pictures.
 */
final class DevClientSmokeM8 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final String tag;
    private int wait;
    private BlockPos tankMin;

    DevClientSmokeM8() {
        StringBuilder sb = new StringBuilder();
        for (String m : List.of("jade", "theoneprobe", "wthit")) {
            if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)) sb.append(sb.isEmpty() ? "" : "_").append(m.equals("theoneprobe") ? "top" : m);
        }
        tag = sb.isEmpty() ? "none" : sb.toString();
        LOGGER.info("[smoketest] M8 info overlays with: {}", tag);
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
        // tank block: front left column, upper row (the bettas swim at the back right)
        then(30, () -> look(tankMin.getX() + 0.5, tankMin.getY() + 1.5, tankMin.getZ() - 2.2, 0F, 0F));
        then(60, () -> shot("tank"));
        then(1, () -> sneak(true));
        then(8, () -> look(tankMin.getX() + 0.5, tankMin.getY() + 1.5, tankMin.getZ() - 2.2, 0F, 0F));
        then(40, () -> shot("tank_details"));
        then(1, () -> sneak(false));
        // betta behind the glass (the plugins retarget from the tank block to the fish)
        then(1, () -> look(tankMin.getX() + 2.5, tankMin.getY() + 0.45, tankMin.getZ() - 1.6, 0F, 0F));
        then(60, () -> shot("betta"));
        then(1, () -> sneak(true));
        then(8, () -> look(tankMin.getX() + 2.5, tankMin.getY() + 0.45, tankMin.getZ() - 1.6, 0F, 0F));
        then(40, () -> shot("betta_details"));
        then(1, () -> sneak(false));
        // external filter on the west side, then the cabinet below the tank (with a filter, attached)
        then(1, () -> look(tankMin.getX() - 0.5, tankMin.getY() + 0.5, tankMin.getZ() - 1.8, 0F, 0F));
        then(50, () -> shot("filter"));
        then(1, () -> look(tankMin.getX() + 1.5, tankMin.getY() - 0.5, tankMin.getZ() - 1.8, 0F, 0F));
        then(50, () -> shot("cabinet"));
        then(1, () -> server(p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setNoGravity(false);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos base = p.blockPosition().offset(0, 2, 70);
        // a row of cabinets, the tank on top (3x2x2), an external filter on the west side
        for (int x = 0; x < 3; x++) {
            BlockPos cp = base.offset(x, 0, 0);
            level.setBlockAndUpdate(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            if (x == 1 && level.getBlockEntity(cp) instanceof CabinetBlockEntity cab) {
                ItemStack f = new ItemStack(ModItems.EXTERNAL_FILTER.get());
                FilterHeaters.write(f, List.of(new ItemStack(ModItems.HEATER_MEDIUM.get()), ItemStack.EMPTY));
                cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
                cab.storage().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 24), false);
                cab.setChanged();
            }
        }
        for (int x = 0; x < 3; x++) {
            BlockPos cp = base.offset(x, 0, 0);
            level.setBlockAndUpdate(cp, Block.updateFromNeighbourShapes(level.getBlockState(cp), level, cp));
        }
        tankMin = base.above();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) for (int x = 0; x < 3; x++) {
            BlockPos pos = tankMin.offset(x, y, z);
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, pos, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, tankMin);
        TankInteractions.changeWater(level, c, 11000);
        c.getTank().setName("Probe tank");
        c.getTank().setSignVisible(false);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setOwner(p.getUUID());
        BlockPos efp = tankMin.offset(-1, 0, 0);
        level.setBlockAndUpdate(efp, ModBlocks.EXTERNAL_FILTER.get().defaultBlockState().setValue(ExternalFilterBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(efp) instanceof ExternalFilterBlockEntity ef) ef.insertHeater(new ItemStack(ModItems.HEATER_LARGE.get()));
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "crowntail", "fin_length", "long", "body_color", "red", "fin_color", "red"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"));
        for (int i = 0; i < genes.size(); i++) {
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i), BettaGenome.DEFAULT), i == 2, level.getGameTime());
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
            Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M8 insert failed: {}", err.getString());
        }
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!(e instanceof BettaEntity b)) continue;
            SmokeFreeze.freeze(b, true);
            if (i == 0) b.setCustomName(Component.literal("Indigo"));
            // the first one right on the view ray of the betta shot, the others further back
            b.snapTo(tankMin.getX() + 2.5, tankMin.getY() + 0.3 + i * 0.5, tankMin.getZ() + 0.5 + i * 0.6, i == 0 ? 90F : -90F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
        c.markTankChanged();
        // M67.5: no probe in the hand (TOP's default needsProbe = 3 gives its extended mode only with one): sneaking alone must
        // show our details, as the hint says
        // hover without flying: sneaking crouches (details in Jade/WTHIT) instead of flying down
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        p.setNoGravity(true);
        LOGGER.info("[smoketest] M8 scene: tank {} with {} animals, filter {}", tankMin, i, efp);
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void sneak(boolean on) {
        mc.options.keyShift.setDown(on);
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = false;
            mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
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

    private void shot(String name) {
        mc.gui.toastManager().clear(); // advancement toasts would cover the overlay
        String file = "bettacraft_m8_" + tag + "_" + name;
        Screenshot.grab(mc.gameDirectory, file + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
