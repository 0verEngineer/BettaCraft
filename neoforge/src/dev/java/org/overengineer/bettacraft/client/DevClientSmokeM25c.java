package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.reef.DosingPumpBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M25 part c client smoke ({@code -Psmoketestm25c}): a soft water tank with the Caridina lines (crystal red A to SSS,
 * crystal black, Blue Bolt, Wine Red, Orange Eye Blue Tiger, the wild form), a hard water tank with cardinal shrimp and
 * Tylomelania, a reef tank with the dosing pump. Screenshots {@code bettacraft_m25_caridina}, {@code _sulawesi},
 * {@code _dosing_pump} (+ {@code _de}). Checks (errors in the log): the pump doses the reef tank; part d: the soft water tank
 * stands on active soil.
 */
final class DevClientSmokeM25c {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final BlockPos base;
    private final String lang = Minecraft.getInstance().options.languageCode;
    private int wait;

    DevClientSmokeM25c() {
        base = mc.player.blockPosition().offset(-10, 0, 700);
        then(1, () -> server(this::build));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
        then(20, () -> look(base.getX() + 2.0, base.getY() + 2.2, base.getZ() - 3.0, 0F, 18F));
        then(80, () -> server(this::arrange));
        // the bee shrimp close up, eye at the glass
        then(1, () -> look(base.getX() + 1.9, base.getY() + 0.62, base.getZ() - 0.6, 0F, 24F));
        then(80, () -> shot("caridina"));
        // the Sulawesi tank
        then(1, () -> look(base.getX() + 7.4, base.getY() + 0.75, base.getZ() - 0.75, 0F, 22F));
        then(60, () -> shot("sulawesi"));
        then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
        // the dosing pump next to the reef tank: its window en + de
        then(1, () -> server(p -> {
            p.teleportTo(p.level(), base.getX() + 14.5, base.getY() + 0.2, base.getZ() - 1.5, java.util.Set.of(), 0F, 20F, false);
            if (p.level().getBlockEntity(base.offset(15, 0, 0)) instanceof DosingPumpBlockEntity pump) {
                pump.step(1.0);
                if (pump.status() != DosingPumpBlockEntity.RUNNING) LOGGER.error("[smoketest] M25c the dosing pump does not run: {}", pump.status());
                org.overengineer.bettacraft.platform.Platform.get().openMenu(p, pump, base.offset(15, 0, 0));
            } else LOGGER.error("[smoketest] M25c no dosing pump");
        }));
        then(30, () -> {
            if (!(mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.DosingPumpScreen)) LOGGER.error("[smoketest] M25c no dosing pump screen");
            shot("dosing_pump");
        });
        then(1, () -> quickLanguage("de_de"));
        then(20, () -> shot("dosing_pump_de"));
        then(1, () -> quickLanguage(lang));
        then(1, () -> mc.gui.setScreen(null));
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        for (int x = -4; x <= 22; x++)
            for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y < 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        TankBlockEntity soft = tank(level, p, base, 4, 2, 2);
        soft.getTank().setTds(150F);
        soft.getTank().setPh(6.3F);
        // M25 part d: the soft water tank stands on active soil
        {
            ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(level.getServer());
            var r = org.overengineer.bettacraft.tank.interior.InteriorManager.ensureRegion(level, soft);
            for (int x = 0; x < r.sizeX(); x++)
                for (int z = 0; z < r.sizeZ(); z++)
                    org.overengineer.bettacraft.tank.interior.Substrate.set(in, new BlockPos(r.originX() + x, org.overengineer.bettacraft.tank.interior.InteriorRegion.FLOOR_Y,
                            r.originZ() + z), ModBlocks.ACTIVE_SOIL.get().defaultBlockState(), false);
            var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
            if (eq == null || eq.activeSoil() != r.sizeX() * r.sizeZ()) LOGGER.error("[smoketest] M25d active soil not counted: {}", eq == null ? "-" : eq.activeSoil());
        }
        TankBlockEntity hard = tank(level, p, base.offset(6, 0, 0), 3, 2, 2);
        hard.getTank().setTds(260F);
        hard.getTank().setPh(8.0F);
        hard.getTank().setWaterTemp(27F);
        hard.getTank().setTargetTemp(27F);
        TankBlockEntity reef = tank(level, p, base.offset(12, 0, 0), 3, 2, 2);
        reef.getTank().setSalinity(35F);
        reef.getTank().setCalcium(400F);
        level.setBlockAndUpdate(base.offset(15, 0, 0), ModBlocks.DOSING_PUMP.get().defaultBlockState().setValue(DosingPumpBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(base.offset(15, 0, 0)) instanceof DosingPumpBlockEntity pump) {
            pump.items().setStackInSlot(0, new ItemStack(ModItems.CALCIUM_CANISTER.get()));
            pump.items().setStackInSlot(1, new ItemStack(ModItems.ALKALINITY_CANISTER.get()));
            ItemStack mg = new ItemStack(ModItems.MAGNESIUM_CANISTER.get());
            mg.setDamageValue(mg.getMaxDamage() / 2);
            pump.items().setStackInSlot(2, mg);
            pump.items().setStackInSlot(3, new ItemStack(ModItems.TRACE_CANISTER.get()));
            pump.setPerDay(0, 12);
            pump.setPerDay(1, 12);
            pump.setPerDay(2, 4);
            pump.setPerDay(3, 8);
        }
        for (TankBlockEntity c : List.of(soft, hard, reef)) c.markTankChanged();
    }

    private void arrange(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity soft = TankMultiblock.getController(level, base), hard = TankMultiblock.getController(level, base.offset(6, 0, 0));
        if (soft == null || hard == null) {
            LOGGER.error("[smoketest] M25c tanks missing");
            return;
        }
        double bx = base.getX(), bz = base.getZ();
        TankData t = soft.getTank();
        t.critters().clear();
        var terrain = TankCritters.terrain(level, t);
        Object[][] bees = { // locus (100 = Wine Red), grade, x, z, yaw
                {CritterGenes.CARIDINA_RED, 0.2F, 0.75, 0.42, 30F}, {CritterGenes.CARIDINA_RED, 0.5F, 1.1, 0.55, -40F},
                {CritterGenes.CARIDINA_RED, 0.7F, 1.45, 0.4, 80F}, {CritterGenes.CARIDINA_RED, 0.92F, 1.8, 0.5, 10F},
                {CritterGenes.CARIDINA_BLACK, 0.7F, 2.15, 0.42, -100F}, {CritterGenes.CARIDINA_BLACK, 0.92F, 2.5, 0.58, 60F},
                {CritterGenes.CARIDINA_TAIWAN, 0.7F, 2.85, 0.4, -20F}, {100, 0.7F, 3.15, 0.55, 150F},
                {CritterGenes.CARIDINA_TIGER, 0.7F, 1.3, 0.85, -60F}, {-1, 0.1F, 2.0, 0.9, 20F}, {CritterGenes.CARIDINA_RED, 0.85F, 2.7, 0.9, -150F}};
        int i = 0;
        for (Object[] b : bees) {
            int l = (Integer) b[0];
            int wine = (1 << CritterGenes.CARIDINA_TAIWAN) | (1 << CritterGenes.CARIDINA_RED);
            CritterGenes g = l == 100 ? new CritterGenes(wine, wine, (Float) b[1]) : l < 0 ? CritterGenes.WILD : CritterGenes.pure(l, (Float) b[1]);
            put(t, terrain, CritterSpecies.CARIDINA, g, i++ % 2 == 0, bx + (Double) b[2], bz + (Double) b[3], (Float) b[4]);
        }
        TankData h = hard.getTank();
        h.critters().clear();
        var ht = TankCritters.terrain(level, h);
        double hx = bx + 6;
        put(h, ht, CritterSpecies.SULAWESI_SHRIMP, CritterGenes.WILD, true, hx + 0.7, bz + 0.45, 20F);
        put(h, ht, CritterSpecies.SULAWESI_SHRIMP, CritterGenes.WILD, false, hx + 1.05, bz + 0.6, -60F);
        put(h, ht, CritterSpecies.SULAWESI_SHRIMP, CritterGenes.WILD, true, hx + 1.9, bz + 0.45, 120F);
        put(h, ht, CritterSpecies.TYLOMELANIA, CritterGenes.WILD, true, hx + 1.45, bz + 0.75, 70F);
        put(h, ht, CritterSpecies.TYLOMELANIA, CritterGenes.WILD, false, hx + 2.3, bz + 0.9, -30F);
        for (TankBlockEntity c : List.of(soft, hard)) {
            c.markTankChanged();
            TankCritters.sync(level, c);
        }
        LOGGER.info("[smoketest] M25c arranged {} bee shrimp, {} Sulawesi animals", t.critters().size(), h.critters().size());
    }

    private static Critter put(TankData t, CritterMovement.Terrain terrain, CritterSpecies s, CritterGenes g, boolean female, double x, double z, float yaw) {
        double y = terrain.ground(x, z);
        Critter k = new Critter(t.nextCritterId(), s, s.hasSexes() && female, g, x, Double.isFinite(y) ? y : t.min().getY() + 0.05, z);
        k.age = s.juvenileDays() + 2;
        k.yaw = yaw;
        k.idle = 1_000_000;
        t.critters().add(k);
        return k;
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 900, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(22F);
        c.getTank().setTargetTemp(22F);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void quickLanguage(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.gui.screen() != null) mc.gui.screen().resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m25_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
