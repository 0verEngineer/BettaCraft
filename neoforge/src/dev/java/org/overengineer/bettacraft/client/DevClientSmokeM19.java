package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M19 part of the dev client smoke test: a planted community tank (cardinals, embers, harlequins, sterbai corys, otos,
 * an L134 in its cave, kuhli loaches, a betta), a gallery of all 20 species (one small tank each, a close-up each, also
 * the handbook images), the breeding (corydoras eggs on the glass, a bristlenose guarding its cave, the breeding box with
 * young guppies, a gourami foam nest, neon eggs in a spawning mop), the animals and breeding tabs of the menu and the
 * items (English, German). Screenshots {@code bettacraft_m19_*}. Checks (errors in the log): the client sees the
 * tankmates with their variety and the clutches of the tanks.
 */
final class DevClientSmokeM19 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int GALLERY_SPACING = 4;

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base, community, breeding;
    private final List<BlockPos> gallery = new ArrayList<>();
    private final List<BlockPos> breedTanks = new ArrayList<>();
    private int wait;

    DevClientSmokeM19() {
        // stand near the scene first: its chunks must stay loaded until the fish are posed
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX(), p.getY() + 2, p.getZ() + 440, java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // let them swim into place, then freeze them in their poses
        then(160, () -> server(this::pose));
        // 1. the community tank
        then(40, () -> look(community.getX() + 2.5, community.getY() + 1.7, community.getZ() - 4.2, 0F, 10F));
        then(60, () -> {
            shot("community");
            check();
        });
        then(1, () -> look(community.getX() + 1.4, community.getY() + 0.9, community.getZ() - 1.6, 0F, 8F));
        then(40, () -> shot("community_close"));
        then(1, () -> look(community.getX() + 3.6, community.getY() + 0.7, community.getZ() - 1.4, -10F, 14F));
        then(40, () -> shot("community_bottom"));
        // 2. the gallery: a close-up of each species (a narrow field of view: the fish fill the picture)
        int[] fov = new int[1];
        then(1, () -> {
            fov[0] = mc.options.fov().get();
            mc.options.fov().set(40);
        });
        for (int i = 0; i < TankmateSpecies.values().length; i++) {
            final int k = i;
            TankmateSpecies s = TankmateSpecies.values()[i];
            double d = s.length() < 0.25 ? 1.3 : s.length() < 0.5 ? 1.7 : 2.3;
            then(1, () -> {
                BlockPos t = gallery.get(k);
                Vec3 f = focus.getOrDefault(t, new Vec3(t.getX() + 1.0, t.getY() + 0.9, t.getZ()));
                boolean bottom = s.zone().bottom();
                look(f.x, f.y + (bottom ? 0.38 : 0.02), t.getZ() - d, 0F, bottom ? 13F : 1F);
            });
            then(30, () -> shot("tm_" + s.id()));
        }
        then(1, () -> mc.options.fov().set(55));
        // 3. breeding
        then(1, () -> lookAt(0, 1.2, 0.75, 1.7, 8F));
        then(40, () -> shot("eggs_glass"));
        then(1, () -> lookAt(1, 1.0, 0.45, 1.25, 12F));
        then(40, () -> shot("cave"));
        then(1, () -> lookAt(2, 1.5, 1.45, 1.5, 6F));
        then(40, () -> shot("breeding_box"));
        then(1, () -> lookAt(3, 1.0, 1.85, 1.6, -6F));
        then(40, () -> shot("foam_nest"));
        then(1, () -> lookAt(4, 1.0, 0.6, 1.4, 12F));
        then(40, () -> shot("mop"));
        then(1, () -> mc.options.fov().set(fov[0]));
        then(1, () -> look(breeding.getX() + 8.5, breeding.getY() + 2.4, breeding.getZ() - 6.5, 0F, 14F));
        then(40, () -> shot("breeding_row"));
        // 4. menus
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.level(), community.getX() + 2.5, community.getY() + 0.5, community.getZ() - 2.5, java.util.Set.of(), 0F, 0F, false);
        }));
        then(10, () -> openMenu(community));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts && !ts.showTankmateForTest("cardinal_tetra")) {
                LOGGER.error("[smoketest] M19 no cardinal in the menu");
            }
        });
        then(20, () -> shot("menu_animals"));
        String lang = mc.options.languageCode;
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_animals_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(5, () -> server(p -> p.teleportTo(p.level(), breedTanks.getFirst().getX() + 1.5, breedTanks.getFirst().getY() + 0.5, breedTanks.getFirst().getZ() - 2.5, java.util.Set.of(), 0F, 0F, false)));
        then(10, () -> openMenu(breedTanks.get(2)));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(2);
        });
        then(20, () -> shot("menu_breeding"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_breeding_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(10, () -> openMenu(breedTanks.getFirst()));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) ts.selectTabForTest(2);
        });
        then(20, () -> shot("menu_breeding_cory"));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // 5. items
        then(10, this::openCreativeTab);
        then(10, this::hoverBag);
        then(20, () -> shot("items"));
        then(1, () -> language("de_de"));
        then(5, this::hoverBag);
        then(20, () -> shot("items_de"));
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
        then(1, () -> mc.options.hideGui = true);
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        reach(p, false);
        level.setDayTime(6000);
        base = p.blockPosition().offset(0, 0, -10);
        // community tank 5x3x3, planted, with a cave
        community = base;
        TankBlockEntity c = tank(level, p, community, 5, 3, 3, 27F);
        InteriorRegion r = region(level, c);
        plants(level, r, true);
        decor(level, r, DecorType.SPAWNING_CAVE, 62, 34, Rotation.CLOCKWISE_90);
        decor(level, r, DecorType.DRIFTWOOD, 20, 30, Rotation.NONE);
        light(level, community, 5, 3, 1);
        fish(level, c, TankmateSpecies.CARDINAL_TETRA, 8, null);
        fish(level, c, TankmateSpecies.EMBER_TETRA, 8, null);
        fish(level, c, TankmateSpecies.HARLEQUIN_RASBORA, 6, null);
        fish(level, c, TankmateSpecies.STERBAI_CORY, 6, null);
        fish(level, c, TankmateSpecies.OTOCINCLUS, 5, null);
        fish(level, c, TankmateSpecies.KUHLI_LOACH, 5, null);
        fish(level, c, TankmateSpecies.LEOPARD_FROG_PLECO, 1, null);
        betta(level, c, p, 7);
        // the gallery
        int i = 0;
        for (TankmateSpecies s : TankmateSpecies.values()) {
            BlockPos min = base.offset(-20 + i * GALLERY_SPACING, 0, 14);
            TankBlockEntity g = tank(level, p, min, 2, 2, 2, (float) Mth.clamp((s.tempMin() + s.tempMax()) / 2, 22, 28));
            if (s.marine()) g.getTank().setSalinity(35F); // M20: the reef fish in sea water
            InteriorRegion gr = region(level, g);
            plants(level, gr, false);
            if (s.breeding() == TankmateSpecies.Breeding.CAVE) decor(level, gr, DecorType.SPAWNING_CAVE, 10, 14, Rotation.CLOCKWISE_90);
            light(level, min, 2, 2, 1);
            int n = s.shoaling() ? 4 : s.dimorphic() ? 2 : 1;
            if (s.varieties().size() > 1) {
                // one fish of each variety (both sexes for the livebearers and the gourami)
                for (String v : s.varieties()) {
                    fishOf(level, g, s, TankmateGenes.pure(s, v), false);
                    if (s.dimorphic()) fishOf(level, g, s, TankmateGenes.pure(s, v), true);
                }
            } else {
                fish(level, g, s, Math.max(2, n), null);
            }
            gallery.add(min);
            i++;
        }
        // breeding tanks
        breeding = base.offset(-20, 0, 26);
        for (int k = 0; k < 5; k++) {
            BlockPos min = breeding.offset(k * 4, 0, 0);
            TankBlockEntity b = tank(level, p, min, 2, 2, 2, 25F);
            InteriorRegion br = region(level, b);
            plants(level, br, false);
            light(level, min, 2, 2, 1);
            breedTanks.add(min);
            switch (k) {
                case 0 -> { // corydoras eggs on the inside of the front glass
                    fish(level, b, TankmateSpecies.PANDA_CORY, 6, null);
                    var t = b.getTank();
                    var rnd = RandomSource.create(19);
                    for (int e = 0; e < 5; e++) {
                        Clutch cl = new Clutch(t.nextClutchId(), TankmateSpecies.PANDA_CORY, Clutch.Kind.GLASS, min.getX() + 0.35 + e * 0.32,
                                min.getY() + 0.35 + rnd.nextDouble() * 0.7, min.getZ() + 1.0 / 16 + 0.012, 4 + rnd.nextInt(4),
                                TankmateGenes.wild(TankmateSpecies.PANDA_CORY), TankmateGenes.wild(TankmateSpecies.PANDA_CORY));
                        cl.face = 0;
                        cl.cover = 0;
                        t.clutches().add(cl);
                    }
                    t.setLastCoolChange(level.getGameTime());
                }
                case 1 -> { // a bristlenose guarding his cave with eggs
                    decor(level, br, DecorType.SPAWNING_CAVE, 16, 10, Rotation.CLOCKWISE_90);
                    fishOf(level, b, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO), false);
                    fishOf(level, b, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.pure(TankmateSpecies.BRISTLENOSE_PLECO, "albino"), true);
                    var caves = org.overengineer.bettacraft.tankmates.TankmateSpots.caves(level.getServer(), b.getTank());
                    if (caves.isEmpty()) {
                        LOGGER.error("[smoketest] M19 the spawning cave was not found");
                    } else {
                        Vec3 cp = caves.getFirst().pos();
                        LOGGER.info("[smoketest] M19 cave at {} facing {}", cp, caves.getFirst().dir());
                        Clutch cl = new Clutch(b.getTank().nextClutchId(), TankmateSpecies.BRISTLENOSE_PLECO, Clutch.Kind.CAVE, cp.x, cp.y + 0.01, cp.z, 24,
                                TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO), TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO));
                        TankmateEntity male = TankmateBreeding.tankmates(level, b.getTank()).stream().filter(m -> !m.isFemale()).findFirst().orElse(null);
                        if (male != null) cl.guardian = male.getUUID();
                        b.getTank().clutches().add(cl);
                    }
                }
                case 2 -> { // guppies with a breeding box and young in it
                    b.getTank().setBreedingBox(true);
                    fishOf(level, b, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "red"), false);
                    fishOf(level, b, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "red"), true);
                    fishOf(level, b, TankmateSpecies.GUPPY, TankmateGenes.pure(TankmateSpecies.GUPPY, "blue_grass"), true);
                    Vec3 bp = TankmateBreeding.boxPos(b.getTank());
                    Clutch cl = new Clutch(b.getTank().nextClutchId(), TankmateSpecies.GUPPY, Clutch.Kind.BOX, bp.x, bp.y, bp.z, 9,
                            TankmateGenes.pure(TankmateSpecies.GUPPY, "red"), TankmateGenes.pure(TankmateSpecies.GUPPY, "red"));
                    cl.cover = 1;
                    b.getTank().clutches().add(cl);
                    for (TankmateEntity m : TankmateBreeding.tankmates(level, b.getTank())) if (m.isFemale()) m.setGravid(1.6, m.genes());
                }
                case 3 -> { // a dwarf gourami foam nest with eggs
                    TankmateEntity male = fishOf(level, b, TankmateSpecies.DWARF_GOURAMI, TankmateGenes.wild(TankmateSpecies.DWARF_GOURAMI), false);
                    fishOf(level, b, TankmateSpecies.DWARF_GOURAMI, TankmateGenes.wild(TankmateSpecies.DWARF_GOURAMI), true);
                    Vec3 np = new TankWaterVolume(b.getTank()).surfacePoint(new Vec3(min.getX() + 1.0, 0, min.getZ() + 0.8), 0.35, 0.06);
                    Clutch cl = new Clutch(b.getTank().nextClutchId(), TankmateSpecies.DWARF_GOURAMI, Clutch.Kind.FOAM, np.x, np.y, np.z, 30,
                            TankmateGenes.wild(TankmateSpecies.DWARF_GOURAMI), TankmateGenes.wild(TankmateSpecies.DWARF_GOURAMI));
                    if (male != null) cl.guardian = male.getUUID();
                    b.getTank().clutches().add(cl);
                }
                default -> { // neon eggs in a spawning mop
                    decor(level, br, DecorType.SPAWNING_MOP, 10, 10, Rotation.NONE);
                    fish(level, b, TankmateSpecies.NEON_TETRA, 6, null);
                    var mops = org.overengineer.bettacraft.tankmates.TankmateSpots.mops(level.getServer(), b.getTank());
                    if (mops.isEmpty()) {
                        LOGGER.error("[smoketest] M19 the spawning mop was not found");
                    } else {
                        for (int e = 0; e < 3; e++) {
                            Vec3 mp = mops.getFirst().pos().add((e - 1) * 0.06, (e - 1) * 0.05, 0);
                            Clutch cl = new Clutch(b.getTank().nextClutchId(), TankmateSpecies.NEON_TETRA, Clutch.Kind.SCATTER, mp.x, mp.y, mp.z, 14,
                                    TankmateGenes.wild(TankmateSpecies.NEON_TETRA), TankmateGenes.wild(TankmateSpecies.NEON_TETRA));
                            cl.cover = 0.8;
                            b.getTank().clutches().add(cl);
                        }
                    }
                }
            }
            b.markTankChanged();
        }
        LOGGER.info("[smoketest] M19 scene at {}: community, {} gallery tanks, {} breeding tanks", base, gallery.size(), breedTanks.size());
    }

    /** Lines the fish of every tank up in their zone in front, side-on to the camera, and freezes them. */
    private void pose(ServerPlayer p) {
        ServerLevel level = p.level();
        List<BlockPos> all = new ArrayList<>(gallery);
        all.addAll(breedTanks);
        for (BlockPos min : all) {
            TankBlockEntity c = TankMultiblock.getController(level, min);
            if (c == null) continue;
            List<TankmateEntity> list = TankmateBreeding.tankmates(level, c.getTank());
            TankWaterVolume w = new TankWaterVolume(c.getTank());
            var region = InteriorRegions.get(level.getServer()).get(c.getTank().regionId());
            TankTerrain terrain = new TankTerrain(c.getTank(), level.getServer(), region, true);
            int n = list.size();
            int front = Math.min(n, 6);
            double fx = 0, fy = 0;
            int fn = 0;
            for (int k = 0; k < n; k++) {
                TankmateEntity m = list.get(k);
                TankmateSpecies s = m.species();
                if (m.isGuarding() || m.holdSpot() != null) {
                    freeze(m, m.getYRot(), TankmateEntity.FLAG_REST);
                    continue;
                }
                boolean back = k >= front;
                int slot = back ? k - front : k;
                int slots = back ? Math.max(1, n - front) : front;
                double span = Math.min(1.1, 0.25 + slots * 0.2);
                double cx = (w.box().minX + w.box().maxX) / 2;
                double x = cx - span / 2 + span * (slots == 1 ? 0.5 : slot / (double) (slots - 1));
                double z = back ? w.box().maxZ - m.sideMargin() - 0.3 : w.box().minZ + m.sideMargin() + 0.04 + (slot % 2) * 0.1;
                double y;
                int flags = 0;
                if (s.zone() == TankmateSpecies.Zone.GLASS && k % 2 == 1) {
                    // on the back glass (shows its back to the camera)
                    z = w.box().maxZ - m.sideMargin() - 0.005;
                    y = w.box().minY + 0.3 + 0.2 * slot / Math.max(1, slots);
                    m.snapTo(x, y, z, 0F, 0F); // facing south: the back glass
                    freeze(m, 0F, TankmateEntity.FLAG_GLASS);
                    continue;
                }
                if (s.zone().bottom()) {
                    double g = terrain.ground(x, z);
                    y = (Double.isFinite(g) ? g : w.box().minY) + 0.01;
                    flags = TankmateEntity.FLAG_REST;
                } else {
                    double mid = w.box().minY + (w.surface() - w.box().minY) * (s.zone().lo + s.zone().hi) / 2;
                    y = mid + (slot % 2 == 0 ? -0.08 : 0.08) * Math.max(1, s.length() * 3);
                }
                float yaw = slot % 2 == 0 ? -90F : 90F;
                m.snapTo(x, y, z, yaw, 0F);
                freeze(m, yaw, flags);
                if (!back) {
                    fx += x;
                    fy += y + m.getBbHeight() * 0.5;
                    fn++;
                }
            }
            if (fn > 0) focus.put(min, new Vec3(fx / fn, fy / fn, min.getZ()));
            for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
                if (e instanceof Mob mob && !(e instanceof TankmateEntity)) SmokeFreeze.freeze(mob, true);
            }
            if (min.equals(gallery.getFirst())) {
                for (TankmateEntity m : list) LOGGER.info("[smoketest] M19 posed {} at {} (tank min {}), noAi {}", m.species().id(), m.position(), min, m.isNoAi());
            }
        }
        posedAt = level.getGameTime();
    }

    private long posedAt;
    /** Middle of the front row of posed fish per gallery tank (where the camera looks). */
    private final java.util.Map<BlockPos, Vec3> focus = new java.util.HashMap<>();

    private static void freeze(TankmateEntity m, float yaw, int flags) {
        SmokeFreeze.freeze(m, true);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
        m.setPoseForSmoke(flags);
    }

    private void check() {
        int seen = 0, varieties = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof TankmateEntity m) {
                seen++;
                if (!"wild".equals(m.variety())) varieties++;
            }
        }
        LOGGER.info("[smoketest] M19 client sees {} tankmates ({} of a bred variety)", seen, varieties);
        if (seen < 60) LOGGER.error("[smoketest] M19 the client sees too few tankmates: {}", seen);
        if (varieties == 0) LOGGER.error("[smoketest] M19 the client sees no bred variety");
        int clutches = 0;
        for (BlockPos b : breedTanks) {
            TankBlockEntity c = TankMultiblock.getController(mc.level, b);
            if (c != null && c.getTank() != null) clutches += c.getTank().clutches().size();
        }
        LOGGER.info("[smoketest] M19 client sees {} clutches", clutches);
        BlockPos g0 = gallery.getFirst();
        for (Entity e : mc.level.getEntitiesOfClass(TankmateEntity.class, new net.minecraft.world.phys.AABB(g0).expandTowards(2, 2, 2))) {
            LOGGER.info("[smoketest] M19 client: {} at {} (tank min {})", ((TankmateEntity) e).species().id(), e.position(), g0);
        }
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), g0);
            if (c == null) return;
            for (TankmateEntity m : TankmateBreeding.tankmates(p.level(), c.getTank())) {
                LOGGER.info("[smoketest] M19 server now: {} at {} noAi {} ({} ticks after posing)", m.species().id(), m.position(), m.isNoAi(),
                        p.level().getGameTime() - posedAt);
            }
        });
        if (clutches < 5) LOGGER.error("[smoketest] M19 the client sees too few clutches: {}", clutches);
    }

    // ------------------------------------------------------------------ helpers

    private static final class Mth {
        static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        return c;
    }

    private static InteriorRegion region(ServerLevel level, TankBlockEntity c) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        return r;
    }

    /** Plants at the back: stem plants and a sword plant; {@code rich}: more of them and java moss. */
    private static void plants(ServerLevel level, InteriorRegion r, boolean rich) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        long seed = 1919;
        int back = r.sizeZ() - 4;
        for (int x = 3; x < r.sizeX() - 2; x += rich ? 5 : 9) {
            PlantSpecies s = (x / 5) % 3 == 0 ? PlantSpecies.ROTALA : (x / 5) % 3 == 1 ? PlantSpecies.VALLISNERIA : PlantSpecies.LUDWIGIA;
            InteriorPlants.plantAt(in, r, s, x, 0, back - (x % 3), s.stages(), seed++);
        }
        if (rich) {
            InteriorPlants.plantAt(in, r, PlantSpecies.AMAZON_SWORD, r.sizeX() / 2, 0, r.sizeZ() / 2 + 6, PlantSpecies.AMAZON_SWORD.stages(), seed++);
            InteriorPlants.plantAt(in, r, PlantSpecies.JAVA_MOSS, 10, 0, 12, PlantSpecies.JAVA_MOSS.stages(), seed++);
            InteriorPlants.plantAt(in, r, PlantSpecies.CRYPT_GREEN, 30, 0, 10, PlantSpecies.CRYPT_GREEN.stages(), seed);
        }
    }

    private static void decor(ServerLevel level, InteriorRegion r, DecorType t, int lx, int lz, Rotation rot) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        var err = DecorItem.place(in, t, new BlockPos(r.originX() + lx, InteriorRegion.BASE_Y, r.originZ() + lz), rot, null);
        if (err != null) LOGGER.error("[smoketest] M19 decor {} not placed: {}", t, err.getString());
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true));
        }
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, int n, String variety) {
        for (int i = 0; i < n; i++) fishOf(level, c, s, variety == null ? TankmateGenes.wild(s) : TankmateGenes.pure(s, variety), i % 2 == 0);
    }

    private static TankmateEntity fishOf(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = level.getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, g, female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M19 {} not spawned", s.id());
        else e.health().feed();
        return e;
    }

    private static void betta(ServerLevel level, TankBlockEntity c, ServerPlayer p, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), false, 0);
        Component err = TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), p, 0L);
        if (err != null) LOGGER.error("[smoketest] M19 betta not inserted: {}", err.getString());
    }

    private void lookAt(int breedTank, double dx, double dy, double dist, float pitch) {
        BlockPos t = breedTanks.get(breedTank);
        look(t.getX() + dx, t.getY() + dy, t.getZ() - dist, 0F, pitch);
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), at);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    /** No info overlay (Jade) in the pictures: no reach while the scene runs. */
    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    /** The creative inventory on the Bettacraft tab, scrolled to the tankmates. */
    private void openCreativeTab() {
        var scr = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(scr);
        try {
            var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            m.setAccessible(true);
            m.invoke(scr, org.overengineer.bettacraft.registry.ModCreativeTabs.MAIN.get());
            var menu = (net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu) scr.getMenu();
            int idx = 0;
            while (idx < menu.items.size() && !menu.items.get(idx).is(ModItems.BREEDING_BOX.get())) idx++;
            int rows = Math.max(1, (menu.items.size() + 8) / 9 - 5);
            float pos = Math.min(1F, (idx / 9) / (float) rows);
            menu.scrollTo(pos);
            var f = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
            f.setAccessible(true);
            f.setFloat(scr, pos);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[smoketest] M19 cannot select the creative tab: {}", ex.toString());
        }
    }

    /** Over a fish bag with a shoal of neons. */
    private void hoverBag() {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            ItemStack st = s.getItem();
            if (org.overengineer.bettacraft.tankmates.TankmateItems.bagSpecies(st) == TankmateSpecies.CARDINAL_TETRA) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.warn("[smoketest] M19 no cardinal bag visible in the creative tab");
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

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.level(), x, eyeY - p.getEyeHeight(), z, java.util.Set.of(), yaw, pitch, false));
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void mouseTo(int x, int y) {
        double scale = mc.getWindow().getGuiScale();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), x * scale, y * scale);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x * scale : y * scale);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] cannot move the mouse: {}", e.toString());
        }
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        if (mc.player != null) LOGGER.info("[smoketest] M19 shot {} from {} (fov {})", name, mc.player.getEyePosition(), mc.options.fov().get());
        Screenshot.grab(mc.gameDirectory, "bettacraft_m19_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] " + c.getString()));
    }
}
