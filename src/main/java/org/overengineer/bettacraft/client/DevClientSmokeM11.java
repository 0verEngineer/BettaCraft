package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M11 part of the dev client smoke test: tank styles, connected glass, thin edges, the water seam and the mirror stick.
 * A row of 3x2x2 tanks (textured vanilla glass, textured light blue stained glass, clean glass), the light blue T with
 * raised ends of the M10 scene, and a small tank with a male for the mirror stick. Screenshots {@code bettacraft_m11_*}:
 * the styles, each style close up, a close-up of the thin edge, the water seam before (per-block glass frames and the
 * old 1/16 edge, as in M10) and after in the cuboid and in the T, the info overlay naming the glass, and the male
 * flaring at the mirror stick.
 */
final class DevClientSmokeM11 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final ResourceLocation LIGHT_BLUE = ResourceLocation.withDefaultNamespace("light_blue_stained_glass");
    private static final ResourceLocation GLASS = ResourceLocation.withDefaultNamespace("glass");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base;
    private volatile BlockPos mirrorTank;
    private int wait;

    DevClientSmokeM11() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> lookM10Style(true));
        // all styles side by side
        then(60, () -> look(base.getX() + 6.5, base.getY() + 3.6, base.getZ() - 7.0, 0F, 22F));
        then(100, () -> shot("styles"));
        // each style from the front, slightly above
        then(1, () -> look(base.getX() + 1.5, base.getY() + 2.6, base.getZ() - 3.4, 0F, 24F));
        then(50, () -> shot("textured_glass"));
        then(1, () -> look(base.getX() + 6.5, base.getY() + 2.6, base.getZ() - 3.4, 0F, 24F));
        then(50, () -> shot("textured_stained"));
        then(1, () -> look(base.getX() + 11.5, base.getY() + 2.6, base.getZ() - 3.4, 0F, 24F));
        then(50, () -> shot("clean"));
        // close-up of the thin edge: the upper front left corner of the clean tank
        then(1, () -> look(base.getX() + 10.35, base.getY() + 2.25, base.getZ() - 0.55, 25F, 35F));
        then(50, () -> shot("edge_closeup"));
        // water seam: before (M10 look: a glass frame on every block, 1/16 edges) and after, cuboid and T
        then(1, () -> lookM10Style(false));
        then(80, () -> look(base.getX() + 6.5, base.getY() + 2.2, base.getZ() - 2.3, 0F, 30F));
        then(50, () -> shot("seam_cuboid_before"));
        then(1, () -> look(base.getX() + 18.5, base.getY() + 3.2, base.getZ() - 2.0, 0F, 40F));
        then(50, () -> shot("seam_t_before"));
        then(1, () -> look(base.getX() + 6.5, base.getY() + 1.1, base.getZ() - 2.6, 20F, -8F));
        then(50, () -> shot("seam_eyelevel_before"));
        then(1, () -> lookM10Style(true));
        then(80, () -> look(base.getX() + 6.5, base.getY() + 2.2, base.getZ() - 2.3, 0F, 30F));
        then(50, () -> shot("seam_cuboid_after"));
        then(1, () -> look(base.getX() + 18.5, base.getY() + 3.2, base.getZ() - 2.0, 0F, 40F));
        then(50, () -> shot("seam_t_after"));
        then(1, () -> look(base.getX() + 6.5, base.getY() + 1.1, base.getZ() - 2.6, 20F, -8F));
        then(50, () -> shot("seam_eyelevel_after"));
        // the info overlay of a tank names its glass (Jade, The One Probe, WTHIT; with GUI); the hotbar shows the items
        then(1, () -> server(p -> {
            var inv = p.getInventory();
            inv.setItem(1, org.overengineer.bettacraft.tank.TankBlockItem.of(ModItems.TANK.get(), GLASS, TankStyle.TEXTURED, 4));
            inv.setItem(2, org.overengineer.bettacraft.tank.TankBlockItem.of(ModItems.TANK.get(), LIGHT_BLUE, TankStyle.TEXTURED, 4));
            inv.setItem(3, org.overengineer.bettacraft.tank.TankBlockItem.of(ModItems.TANK.get(), GLASS, TankStyle.CLEAN, 4));
            inv.setItem(4, new ItemStack(ModItems.MIRROR_STICK.get()));
            inv.selected = 0;
        }));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> look(base.getX() + 6.5, base.getY() + 1.5, base.getZ() - 2.2, 0F, 0F));
        then(60, () -> shot("overlay_glass"));
        then(1, () -> look(base.getX() + 11.5, base.getY() + 1.5, base.getZ() - 2.2, 0F, 0F));
        then(60, () -> shot("overlay_clean"));
        // the water tab of the tank menu names the glass and the style
        then(1, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), base.offset(5, 0, 0));
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(5);
        });
        then(30, () -> shot("menu_water"));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // the mirror stick: hold it up in front of the male
        then(1, () -> server(p -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIRROR_STICK.get()))));
        then(1, () -> lookAt(mirrorTank.getX() + 1.0 - 0.7, mirrorTank.getY() + 0.75, mirrorTank.getZ() - 0.9,
                new Vec3(mirrorTank.getX() + 1.0, mirrorTank.getY() + 0.5, mirrorTank.getZ() + 0.9)));
        then(70, () -> mc.getToasts().clear());
        then(10, () -> shot("mirror"));
        // the same male from the side while he still flares at the mirror (the flare holds 2 s after the last look)
        then(1, () -> server(p -> p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)));
        then(1, () -> {
            mc.options.hideGui = true;
            lookAt(mirrorTank.getX() + 3.4, mirrorTank.getY() + 0.62, mirrorTank.getZ() + 0.9,
                    new Vec3(mirrorTank.getX() + 1.0, mirrorTank.getY() + 0.5, mirrorTank.getZ() + 0.9));
        });
        then(12, () -> shot("mirror_side"));
        then(1, () -> server(p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (Entity e : TankInhabitants.liveInhabitants(p.serverLevel(), TankMultiblock.getController(p.serverLevel(), mirrorTank).getTank())) {
                if (e instanceof BettaEntity b) LOGGER.info("[smoketest] M11 mirror: male flaring {} trained {}", b.isFlaring(), b.breeding().isTrained());
            }
        }));
        then(20, () -> mc.options.hideGui = false);
    }

    /** {@code connectedGlass} off and 1/16 edges = the look of M10 (the "before" pictures); on with the default width = M11. */
    private void lookM10Style(boolean m11) {
        ClientConfig.CONNECTED_GLASS.set(m11);
        ClientConfig.TANK_EDGE_WIDTH.set(m11 ? 0.25 : 1.0);
        org.overengineer.bettacraft.client.tank.TankWaterRenderer.m10Look = !m11;
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        LOGGER.info("[smoketest] M11 tank look: {}", m11 ? "M11 (connected glass, 1/64 edges)" : "M10 (frame on every block, 1/16 edges)");
    }

    private static void place(ServerLevel level, List<BlockPos> blocks, ResourceLocation glass, TankStyle style) {
        for (BlockPos pos : blocks) {
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                be.setGlassId(glass);
                be.setStyle(style);
            }
        }
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
    }

    private static List<BlockPos> cuboid(BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> l = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) l.add(min.offset(x, y, z));
        return l;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        base = p.blockPosition().offset(0, 2, 150);
        String[] names = {"Glass", "Stained", "Clear"};
        ResourceLocation[] glass = {GLASS, LIGHT_BLUE, GLASS};
        TankStyle[] style = {TankStyle.TEXTURED, TankStyle.TEXTURED, TankStyle.CLEAN};
        for (int i = 0; i < 3; i++) {
            BlockPos min = base.offset(i * 5, 0, 0);
            List<BlockPos> blocks = cuboid(min, 3, 2, 2);
            place(level, blocks, glass[i], style[i]);
            TankBlockEntity c = TankMultiblock.getController(level, min);
            TankInteractions.changeWater(level, c, i == 1 ? 12000 : 10000);
            c.getTank().setName(names[i] + " tank");
            c.getTank().setFront(Direction.NORTH);
            c.getTank().setSignVisible(false);
            c.getTank().setOwner(p.getUUID());
            fish(level, p, c, new double[][]{{0.8, 0.5, 1.2}, {2.2, 1.3, 0.9}});
            c.markTankChanged();
        }
        // the light blue T with raised ends (M10 scene), partly filled: the upper layer half full
        BlockPos tb = base.offset(16, 0, 0);
        List<BlockPos> t = new ArrayList<>();
        for (int x = 0; x < 5; x++) t.add(tb.offset(x, 0, 2));
        t.add(tb.offset(2, 0, 0));
        t.add(tb.offset(2, 0, 1));
        t.add(tb.offset(0, 1, 2));
        t.add(tb.offset(4, 1, 2));
        place(level, t, LIGHT_BLUE, TankStyle.TEXTURED);
        TankBlockEntity ct = TankMultiblock.getController(level, tb.offset(2, 0, 0));
        TankInteractions.changeWater(level, ct, 8000);
        ct.getTank().setSignVisible(false);
        ct.getTank().setOwner(p.getUUID());
        fish(level, p, ct, new double[][]{{2.5, 0.5, 0.6}});
        // a small clean tank with one male for the mirror stick (in front of the others, to the right)
        BlockPos mt = base.offset(24, 0, 0);
        place(level, cuboid(mt, 2, 1, 2), GLASS, TankStyle.CLEAN);
        TankBlockEntity cm = TankMultiblock.getController(level, mt);
        TankInteractions.changeWater(level, cm, 4000);
        cm.getTank().setSignVisible(false);
        cm.getTank().setOwner(p.getUUID());
        fish(level, p, cm, new double[][]{{1.0, 0.5, 0.9}}, 0);
        mirrorTank = mt;
        LOGGER.info("[smoketest] M11 scene at {}: styles {}, T {} blocks, mirror tank {} (Jade {}, TOP {}, WTHIT {})", base,
                List.of(names), ct.getTank().memberCount(), mt, ModList.get().isLoaded("jade"), ModList.get().isLoaded("theoneprobe"),
                ModList.get().isLoaded("wthit"));
    }

    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots) {
        fish(level, p, c, spots, 1);
    }

    /** Adult bettas at fixed spots (relative to the tank's box min); {@code firstFemale} 0 = all males from index 0 on. */
    private static void fish(ServerLevel level, ServerPlayer p, TankBlockEntity c, double[][] spots, int firstFemale) {
        List<Map<String, String>> genes = List.of(
                Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red", "pattern", "butterfly",
                        "pattern_color", "white", "iridescence", "high"),
                Map.of("finform", "round", "fin_length", "short", "body_color", "yellow", "fin_color", "yellow", "pattern", "koi",
                        "pattern_color", "orange"));
        for (int i = 0; i < spots.length; i++) {
            boolean female = firstFemale > 0 && i >= firstFemale;
            BettaData d = BettaData.create(BettaGenome.of(genes.get(i % genes.size()), BettaGenome.DEFAULT), female, level.getGameTime());
            Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    ModEntities.BETTA.get(), p, false);
            if (err != null) LOGGER.warn("[smoketest] M11 insert failed: {}", err.getString());
        }
        TankData t = c.getTank();
        int i = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof BettaEntity b) || i >= spots.length) continue;
            SmokeFreeze.freeze(b, true);
            b.moveTo(t.min().getX() + spots[i][0], t.min().getY() + spots[i][1], t.min().getZ() + spots[i][2], 90F + i * 60F, 0F);
            b.setYBodyRot(b.getYRot());
            b.setYHeadRot(b.getYRot());
            i++;
        }
    }

    /** Puts the eye of the player at (x, eyeY, z). */
    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
    }

    private void lookAt(double x, double eyeY, double z, Vec3 target) {
        double dx = target.x - x, dy = target.y - eyeY, dz = target.z - z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        look(x, eyeY, z, yaw, pitch);
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
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

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m11_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
