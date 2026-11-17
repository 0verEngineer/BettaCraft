package org.overengineer.bettacraft.client;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.render.BettaSkins;
import org.overengineer.bettacraft.client.render.GuppyLook;
import org.overengineer.bettacraft.client.render.TankmateRenderer;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.livebearer.LbLocus;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.6 dev client smoke scene ({@code -Psmoketestm21_6}), screenshots {@code bettacraft_m21_6_*}: guppy and endler
 * genetics.
 * <ul>
 *     <li>every named strain as a male, side by side in 3x2 tanks (frozen), without and with name tags;</li>
 *     <li>females of several strains (one pregnant), pairs, hybrids (1/4, 1/2, 3/4 endler), the nine tail shapes;</li>
 *     <li>the old fixed varieties of an older world as converted genomes, next to the old textures (genetics off);</li>
 *     <li>a community tank with free-swimming guppies and endlers;</li>
 *     <li>the tank menu with the gene tooltip (en, de) and the genetics page of the handbook (en, de).</li>
 * </ul>
 * Every fish of the scene must get a composited skin (no missing mask).
 */
final class DevClientSmokeM216 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    /** One fish of a display tank. */
    private record Show(String label, TankmateSpecies species, TankmateGenes genes, boolean female, boolean gravid) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final List<BlockPos> tanks = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private BlockPos base, community, menuTank, legacy;
    private int wait;
    private final String lang = mc.options.languageCode;
    private final List<List<Show>> displays = new ArrayList<>();
    private final List<TankmateEntity> legacyFish = new ArrayList<>();

    DevClientSmokeM216() {
        plan();
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(120, () -> {});
        for (int i = 0; i < displays.size(); i++) {
            int n = i;
            then(1, () -> look(tanks.get(n).getX() + 1.0, tanks.get(n).getY() + 0.92, tanks.get(n).getZ() - 0.8, 0F, 0F));
            then(50, () -> shot(names.get(n)));
            then(1, () -> mc.options.hideGui = false);
            then(10, () -> shot(names.get(n) + "_labels"));
            then(1, () -> mc.options.hideGui = true);
        }
        then(1, this::checkSkins);
        // old varieties converted, then the same fish with the genetics switched off (old textures)
        then(1, () -> look(legacy.getX() + 2.0, legacy.getY() + 0.93, legacy.getZ() - 1.9, 0F, 0F));
        then(50, () -> shot("legacy_genetics"));
        then(1, () -> server(p -> setGenetics(false)));
        then(30, () -> shot("legacy_old_textures"));
        then(1, () -> server(p -> setGenetics(true)));
        then(20, () -> {});
        // a community tank
        then(1, () -> look(community.getX() + 2.0, community.getY() + 1.2, community.getZ() - 2.6, 0F, 5F));
        then(100, () -> shot("community"));
        then(1, () -> look(community.getX() + 1.0, community.getY() + 0.9, community.getZ() - 1.2, 0F, 8F));
        then(40, () -> shot("community_close"));
        // the menu with the gene tooltip
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.level(), menuTank.getX() + 0.5, menuTank.getY(), menuTank.getZ() - 2.0, java.util.Set.of(), 0F, 0F, false);
        }));
        then(10, () -> openMenu(menuTank));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
                if (!ts.showTankmateForTest("guppy")) LOGGER.error("[smoketest] M21.6 no guppy in the menu");
                ts.showGeneTooltipForTest(true);
            }
        });
        then(20, () -> shot("menu_genes"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_genes_de"));
        then(1, () -> language(lang));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.showGeneTooltipForTest(false);
        });
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> reach(p, false)));
        // the handbook page
        then(1, () -> mc.options.hideGui = true);
        guide("en");
        then(1, () -> language("de_de"));
        guide("de");
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
    }

    private void guide(String l) {
        if (!GuideBook.available()) return;
        then(10, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("tankmates/guppy_genetics.md")));
        then(40, () -> shot("guide_genetics_" + l));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ what is shown

    private void plan() {
        Random r = new Random(216);
        List<Show> all = new ArrayList<>();
        for (LivebearerStrain s : LivebearerStrain.of(false)) all.add(new Show(s.en, TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, s), false, false));
        display("strains_guppy", all);
        List<Show> endlers = new ArrayList<>();
        for (LivebearerStrain s : LivebearerStrain.of(true)) endlers.add(new Show(s.en, TankmateSpecies.ENDLER_GUPPY, TankmateGenes.strain(TankmateSpecies.ENDLER_GUPPY, s), false, false));
        LivebearerGenome cobra = LivebearerStrain.RED_COBRA.genome(), tiger = LivebearerStrain.ENDLER_TIGER.genome();
        LivebearerGenome h50 = LivebearerGenome.cross(cobra, tiger, false, r, 0);
        endlers.add(new Show("Hybrid 50% (Red Cobra x Endler Tiger)", TankmateSpecies.GUPPY, new TankmateGenes("wild", "wild", h50), false, false));
        display("strains_endler", endlers);
        List<Show> hybrids = new ArrayList<>();
        LivebearerGenome h50f = LivebearerGenome.cross(cobra, tiger, true, r, 0);
        hybrids.add(new Show("Hybrid 25%", TankmateSpecies.GUPPY, new TankmateGenes("wild", "wild", LivebearerGenome.cross(cobra, h50, false, r, 0)), false, false));
        hybrids.add(new Show("Hybrid 50%", TankmateSpecies.GUPPY, new TankmateGenes("wild", "wild", h50), false, false));
        hybrids.add(new Show("Hybrid 75%", TankmateSpecies.ENDLER_GUPPY, new TankmateGenes("wild", "wild", LivebearerGenome.cross(tiger.toBuilder().y("none").build(), h50, false, r, 0)), false, false));
        hybrids.add(new Show("Red Cobra (father)", TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.RED_COBRA), false, false));
        hybrids.add(new Show("Hybrid 50% female", TankmateSpecies.GUPPY, new TankmateGenes("wild", "wild", h50f), true, false));
        hybrids.add(new Show("Endler Tiger (father)", TankmateSpecies.ENDLER_GUPPY, TankmateGenes.strain(TankmateSpecies.ENDLER_GUPPY, LivebearerStrain.ENDLER_TIGER), false, false));
        display("hybrids", hybrids);
        List<Show> females = new ArrayList<>();
        for (LivebearerStrain s : List.of(LivebearerStrain.WILD_GUPPY, LivebearerStrain.FULL_GOLD, LivebearerStrain.ALBINO_FULL_RED, LivebearerStrain.TUXEDO,
                LivebearerStrain.BLUE_GRASS, LivebearerStrain.ENDLER_WILD)) {
            TankmateSpecies sp = s.endler ? TankmateSpecies.ENDLER_GUPPY : TankmateSpecies.GUPPY;
            females.add(new Show(s.en + " female", sp, TankmateGenes.strain(sp, s), true, s == LivebearerStrain.FULL_GOLD));
        }
        display("females", females);
        List<Show> pairs = new ArrayList<>();
        for (LivebearerStrain s : List.of(LivebearerStrain.RED_COBRA, LivebearerStrain.BLUE_MOSCOW, LivebearerStrain.ENDLER_TIGER)) {
            TankmateSpecies sp = s.endler ? TankmateSpecies.ENDLER_GUPPY : TankmateSpecies.GUPPY;
            pairs.add(new Show(s.en + " male", sp, TankmateGenes.strain(sp, s), false, false));
        }
        for (LivebearerStrain s : List.of(LivebearerStrain.RED_COBRA, LivebearerStrain.BLUE_MOSCOW, LivebearerStrain.ENDLER_TIGER)) {
            TankmateSpecies sp = s.endler ? TankmateSpecies.ENDLER_GUPPY : TankmateSpecies.GUPPY;
            pairs.add(new Show(s.en + " female", sp, TankmateGenes.strain(sp, s), true, false));
        }
        display("pairs", pairs);
        List<Show> shapes = new ArrayList<>();
        for (String t : LbLocus.TAIL.alleles) {
            LivebearerGenome g = LivebearerStrain.FULL_RED.genome().toBuilder().pure(LbLocus.TAIL, t).build();
            shapes.add(new Show(t, TankmateSpecies.GUPPY, new TankmateGenes("red", "red", g), false, false));
        }
        display("tails", shapes);
    }

    /** Tanks of four (2 x 2), named name_1, name_2, ... when there are more. */
    private void display(String name, List<Show> fish) {
        for (int i = 0; i < fish.size(); i += 4) {
            names.add(fish.size() > 4 ? name + "_" + (i / 4 + 1) : name);
            displays.add(new ArrayList<>(fish.subList(i, Math.min(fish.size(), i + 4))));
        }
    }

    // ------------------------------------------------------------------ building

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        base = p.blockPosition().offset(-30, 3, 440);
        for (int i = 0; i < displays.size(); i++) {
            BlockPos at = base.offset(i * 4, 0, 0);
            tanks.add(at);
            TankBlockEntity c = tank(level, at, 2, 2, 1, p);
            floor(level, c, Blocks.SAND.defaultBlockState());
            light(level, at, 2, 2, 0);
            List<Show> fish = displays.get(i);
            StringBuilder log = new StringBuilder();
            for (int k = 0; k < fish.size(); k++) {
                Show s = fish.get(k);
                int col = k % 2, row = 1 - k / 2;
                TankmateEntity e = fish(level, c, s.species(), s.genes(), s.female());
                if (e == null) continue;
                if (s.gravid()) e.setGravid(1.0, s.genes());
                e.setCustomName(Component.literal(s.label()));
                e.setCustomNameVisible(true);
                // seen from the front (north) the first column is on the right: x falls from left to right
                place(e, at.getX() + 1.45 - col * 0.9, at.getY() + 0.45 + row * 0.92, at.getZ() + 0.5);
                log.append(col).append(',').append(row).append('=').append(s.label()).append("; ");
            }
            LOGGER.info("[smoketest] M21.6 {}: {}", names.get(i), log);
        }
        // the old fixed varieties of an older world (genes without a genome: converted when loaded)
        legacy = base.offset(0, 0, 6);
        TankBlockEntity lc = tank(level, legacy, 4, 2, 1, p);
        floor(level, lc, Blocks.SAND.defaultBlockState());
        light(level, legacy, 4, 2, 0);
        String[][] old = {{"guppy", "wild"}, {"guppy", "red"}, {"guppy", "blue_grass"}, {"guppy", "green_cobra"}, {"guppy", "tuxedo"},
                {"endler_guppy", "wild"}, {"endler_guppy", "tiger"}, {"endler_guppy", "black_bar"}};
        for (int k = 0; k < old.length; k++) {
            TankmateSpecies sp = TankmateSpecies.byId(old[k][0]);
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putString("a", old[k][1]);
            t.putString("b", old[k][1]);
            TankmateGenes g = TankmateGenes.load(t, sp); // as an M21.5 fish would load
            TankmateEntity e = fish(level, lc, sp, g, false);
            if (e == null) continue;
            place(e, legacy.getX() + 3.5 - (k % 4) * 1.0, legacy.getY() + 0.45 + (1 - k / 4) * 0.92, legacy.getZ() + 0.5);
            legacyFish.add(e);
        }
        // a community tank: guppies and endlers swimming
        community = base.offset(0, 0, 12);
        TankBlockEntity cc = tank(level, community, 4, 2, 2, p);
        floor(level, cc, Blocks.GRAVEL.defaultBlockState());
        light(level, community, 4, 2, 1);
        Random r = new Random(7);
        for (LivebearerStrain s : List.of(LivebearerStrain.RED_COBRA, LivebearerStrain.BLUE_MOSCOW, LivebearerStrain.FULL_GOLD, LivebearerStrain.TUXEDO_KOI,
                LivebearerStrain.JAPAN_BLUE, LivebearerStrain.ENDLER_TIGER, LivebearerStrain.ENDLER_WILD, LivebearerStrain.BLUE_GRASS)) {
            TankmateSpecies sp = s.endler ? TankmateSpecies.ENDLER_GUPPY : TankmateSpecies.GUPPY;
            fish(level, cc, sp, TankmateGenes.strain(sp, s), false);
            if (r.nextBoolean()) fish(level, cc, sp, TankmateGenes.strain(sp, s), true);
        }
        // the menu tank: one Tuxedo Koi male
        menuTank = base.offset(0, 0, 18);
        TankBlockEntity mt = tank(level, menuTank, 1, 1, 1, p);
        fish(level, mt, TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.TUXEDO_KOI), false);
        reach(p, false);
    }

    private static void place(TankmateEntity e, double x, double y, double z) {
        e.snapTo(x, y, z, 90F, 0F);
        e.setYBodyRot(90F);
        e.setYHeadRot(90F);
        e.setDeltaMovement(Vec3.ZERO);
        e.freezeBehaviour();
        SmokeFreeze.freeze(e, true);
    }

    /** Switches the genetics (server config value) and re-sends the look of the old-variety fish. */
    private void setGenetics(boolean on) {
        Features.set(Feature.LIVEBEARER_GENETICS, on);
        for (TankmateEntity e : legacyFish) e.setup(e.genes(), e.isFemale(), e.lifeStage(), e.generation());
        LOGGER.info("[smoketest] M21.6 genetics {} ({}), {} old-variety fish re-sent", on ? "on" : "off",
                Features.enabled(Feature.LIVEBEARER_GENETICS), legacyFish.size());
    }

    /** Every guppy and endler near the scene has the genetics look and a composited skin. */
    private void checkSkins() {
        int fish = 0, skins = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof TankmateEntity f) || !TankmateGenes.livebearer(f.species()) || f.distanceToSqr(Vec3.atCenterOf(base)) > 80 * 80) continue;
            var p = f.livebearerPhenotype();
            if (p == null) continue;
            fish++;
            if (BettaSkins.get(GuppyLook.layers(p, TankmateRenderer.adultLook(f), f.isGravid())) != null) skins++;
        }
        LOGGER.info("[smoketest] M21.6 genetics look: {} fish, {} composited skins", fish, skins);
        if (fish < 40) LOGGER.error("[smoketest] M21.6 too few guppies with the genetics look ({})", fish);
        if (skins != fish) LOGGER.error("[smoketest] M21.6 {} fish without a skin (missing mask?)", fish - skins);
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, ServerPlayer owner) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, blocks.size() * 1000L);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(owner.getUUID());
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(1F);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion floor(ServerLevel level, TankBlockEntity c, BlockState s) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
        return r;
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true));
        }
    }

    private static TankmateEntity fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = level.getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, g, female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M21.6 {} not spawned", s.id());
        else e.health().feed();
        return e;
    }

    /** No info overlay (Jade) in the pictures: no reach while the scene runs. */
    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
            else p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false);
        });
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

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_6_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
