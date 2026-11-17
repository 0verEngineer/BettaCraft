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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M29 part a dev client smoke scene ({@code -Psmoketestm29}), screenshots {@code bettacraft_m29_*}: a dwarf cichlid tank
 * with every ram and Apistogramma variety and sex posed in two rows (a slate cave and a coconut shell behind them), a tall
 * tank with every angelfish and discus variety, close-ups, and a ram pair guarding its eggs on the slate. Checks (errors in
 * the log): the client sees every cichlid.
 * <p>
 * Part b ({@code -Psmoketestm29b}, screenshots {@code bettacraft_m29b_*}): a tank with every puffer and barb (variety and
 * sex), a tank with the loaches, the garra and the green spotted puffer (brackish), close-ups of every species.
 * <p>
 * Part c ({@code -Psmoketestm29c}, screenshots {@code bettacraft_m29c_*}): danios, rainbowfish and tetras, gouramis and
 * livebearers (every variety and sex), the wild bettas (splendens, imbellis, smaragdina, a hybrid), close-ups.
 */
final class DevClientSmokeM29 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos dwarfs, big, brood;
    /** The first posed fish of every species (for its close-up and the handbook picture). */
    private final java.util.Map<TankmateSpecies, Vec3> firstOf = new java.util.concurrent.ConcurrentHashMap<>();
    private int wait;
    private final boolean partB;
    private final String prefix;

    DevClientSmokeM29() {
        this(false);
    }

    DevClientSmokeM29(boolean partB) {
        this(partB, false);
    }

    DevClientSmokeM29(boolean partB, boolean partC) {
        this.partB = partB;
        this.prefix = partC ? "bettacraft_m29c_" : partB ? "bettacraft_m29b_" : "bettacraft_m29_";
        if (partC) {
            partC();
            return;
        }
        if (partB) {
            partB();
            return;
        }
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX(), p.getY() + 2, p.getZ() + 700, java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        then(80, () -> server(this::pose));
        then(1, () -> look(dwarfs.getX() + 3.0, dwarfs.getY() + 1.0, dwarfs.getZ() - 4.6, 0F, 6F));
        then(60, () -> {
            shot("dwarfs");
            check();
        });
        then(1, () -> look(dwarfs.getX() + 1.4, dwarfs.getY() + 0.75, dwarfs.getZ() - 1.6, 0F, 6F));
        then(40, () -> shot("dwarfs_close"));
        then(1, () -> look(dwarfs.getX() + 4.4, dwarfs.getY() + 0.75, dwarfs.getZ() - 1.6, 0F, 6F));
        then(40, () -> shot("dwarfs_close_2"));
        then(1, () -> look(big.getX() + 3.0, big.getY() + 1.5, big.getZ() - 5.2, 0F, 4F));
        then(60, () -> shot("angel_discus"));
        then(1, () -> look(big.getX() + 1.6, big.getY() + 1.4, big.getZ() - 2.2, 0F, 4F));
        then(40, () -> shot("angel_close"));
        then(1, () -> look(big.getX() + 4.4, big.getY() + 1.4, big.getZ() - 2.2, 0F, 4F));
        then(40, () -> shot("discus_close"));
        then(1, () -> look(brood.getX() + 1.0, brood.getY() + 0.9, brood.getZ() - 1.1, 0F, 25F));
        then(60, () -> shot("brood"));
        // one close-up per species (the handbook pictures)
        for (TankmateSpecies s : List.of(TankmateSpecies.RAM_CICHLID, TankmateSpecies.BOLIVIAN_RAM, TankmateSpecies.COCKATOO_CICHLID,
                TankmateSpecies.AGASSIZ_DWARF_CICHLID, TankmateSpecies.ANGELFISH, TankmateSpecies.DISCUS)) {
            then(1, () -> {
                Vec3 at = firstOf.get(s);
                if (at == null) {
                    LOGGER.error("[smoketest] M29 no posed {}", s.id());
                    return;
                }
                double back = s.length() > 0.8 ? 1.9 : 1.15;
                look(at.x, at.y + 0.05, at.z - back, 0F, 2F);
            });
            then(40, () -> shot("sp_" + s.id()));
        }
        then(1, () -> mc.options.hideGui = false);
    }


    // ------------------------------------------------------------------ part c: danios, rainbowfish, gouramis, tetras, livebearers, wild bettas

    private static final List<TankmateSpecies> PART_C_A = List.of(TankmateSpecies.ZEBRA_DANIO, TankmateSpecies.CELESTIAL_PEARL_DANIO,
            TankmateSpecies.FORKTAIL_RAINBOW, TankmateSpecies.NEON_DWARF_RAINBOW, TankmateSpecies.EMPEROR_TETRA, TankmateSpecies.RUMMY_NOSE_TETRA,
            TankmateSpecies.BLACK_PHANTOM_TETRA);
    private static final List<TankmateSpecies> PART_C_B = List.of(TankmateSpecies.HONEY_GOURAMI, TankmateSpecies.CHOCOLATE_GOURAMI,
            TankmateSpecies.PEARL_GOURAMI, TankmateSpecies.BLACK_MOLLY, TankmateSpecies.SWORDTAIL);
    private BlockPos bettaTank;
    private final java.util.Map<String, Vec3> bettaAt = new java.util.concurrent.ConcurrentHashMap<>();

    private void partC() {
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX(), p.getY() + 2, p.getZ() + 820, java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::buildC));
        then(1, () -> mc.options.hideGui = true);
        then(80, () -> server(this::poseC));
        then(1, () -> look(dwarfs.getX() + 3.0, dwarfs.getY() + 1.0, dwarfs.getZ() - 4.6, 0F, 6F));
        then(60, () -> {
            shot("community");
            int n = 0, b = 0;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof TankmateEntity m && (PART_C_A.contains(m.species()) || PART_C_B.contains(m.species()))) n++;
                if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) b++;
            }
            LOGGER.info("[smoketest] M29c client sees {} new fish, {} bettas", n, b);
            if (n < 35) LOGGER.error("[smoketest] M29c the client sees too few: {}", n);
            if (b < 4) LOGGER.error("[smoketest] M29c the client sees too few bettas: {}", b);
        });
        then(1, () -> look(big.getX() + 3.0, big.getY() + 1.0, big.getZ() - 4.6, 0F, 6F));
        then(60, () -> shot("gouramis_livebearers"));
        then(1, () -> look(bettaTank.getX() + 1.5, bettaTank.getY() + 0.9, bettaTank.getZ() - 2.6, 0F, 6F));
        then(60, () -> shot("wild_bettas"));
        List<TankmateSpecies> all = new ArrayList<>(PART_C_A);
        all.addAll(PART_C_B);
        for (TankmateSpecies s : all) {
            then(1, () -> {
                Vec3 at = firstOf.get(s);
                if (at == null) {
                    LOGGER.error("[smoketest] M29c no posed {}", s.id());
                    return;
                }
                double back = s.length() > 0.6 ? 1.7 : s.length() > 0.35 ? 1.3 : 1.15;
                look(at.x, at.y + 0.05, at.z - back, 0F, 2F);
            });
            then(40, () -> shot("sp_" + s.id()));
        }
        for (String k : List.of("imbellis", "smaragdina")) {
            then(1, () -> {
                Vec3 at = bettaAt.get(k);
                if (at == null) {
                    LOGGER.error("[smoketest] M29c no posed betta {}", k);
                    return;
                }
                look(at.x, at.y + 0.05, at.z - 1.2, 0F, 2F);
            });
            then(40, () -> shot("betta_" + k));
        }
        then(1, () -> mc.options.hideGui = false);
    }

    private void buildC(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(0, 0, -10);
        dwarfs = base;
        TankBlockEntity a = tank(level, p, dwarfs, 6, 2, 2, 26F);
        region(level, a);
        light(level, dwarfs, 6, 2, 1);
        big = base.offset(0, 0, -8);
        TankBlockEntity b = tank(level, p, big, 6, 2, 2, 27F);
        region(level, b);
        light(level, big, 6, 2, 1);
        bettaTank = base.offset(9, 0, 0);
        TankBlockEntity bt = tank(level, p, bettaTank, 3, 2, 2, 27F);
        region(level, bt);
        light(level, bettaTank, 3, 2, 1);
        for (TankmateSpecies s : PART_C_A) {
            for (String v : s.varieties()) {
                fish(level, a, s, v, false);
                if (s.dimorphic()) fish(level, a, s, v, true);
            }
        }
        for (TankmateSpecies s : PART_C_B) {
            for (String v : s.varieties()) {
                fish(level, b, s, v, false);
                if (s.dimorphic()) fish(level, b, s, v, true);
            }
        }
        // wild bettas: a male of each species and a hybrid
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(29);
        var imb = org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(org.overengineer.bettacraft.genetics.BettaSpecies.IMBELLIS, r);
        var sma = org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(org.overengineer.bettacraft.genetics.BettaSpecies.SMARAGDINA, r);
        var spl = org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(org.overengineer.bettacraft.genetics.BettaSpecies.SPLENDENS, r);
        for (var g : List.of(spl, imb, sma, org.overengineer.bettacraft.genetics.BettaGenotype.cross(imb, spl, r, 0))) {
            var d = org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0);
            var bucket = org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new net.minecraft.world.item.ItemStack(
                    org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()), d);
            var err = org.overengineer.bettacraft.tank.TankInhabitants.insert(level, bt, bucket,
                    org.overengineer.bettacraft.registry.ModEntities.BETTA.get(), null, false);
            if (err != null) LOGGER.error("[smoketest] M29c betta insert: {}", err.getString());
        }
    }

    private void poseC(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity a = TankMultiblock.getController(level, dwarfs);
        if (a != null) row(level, a, 7, new double[]{1.35, 0.85, 0.35}, 0.8, firstOf);
        TankBlockEntity b = TankMultiblock.getController(level, big);
        if (b != null) row(level, b, 7, new double[]{1.4, 0.9, 0.4}, 0.82, firstOf);
        TankBlockEntity bt = TankMultiblock.getController(level, bettaTank);
        if (bt == null) return;
        TankWaterVolume w = new TankWaterVolume(bt.getTank());
        int i = 0;
        for (Entity e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, bt.getTank())) {
            if (!(e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity f)) continue;
            Vec3 at = new Vec3(w.box().minX + 0.45 + (i % 2) * 1.4, w.box().minY + (i < 2 ? 1.25 : 0.55), w.box().minZ + 0.6);
            f.snapTo(at.x, at.y, at.z, -90F, 0F);
            SmokeFreeze.freeze(f, true);
            f.setDeltaMovement(Vec3.ZERO);
            f.setYRot(-90F);
            f.yBodyRot = -90F;
            f.yHeadRot = -90F;
            var sp = f.genotype().ancestry().species();
            if (sp != null) bettaAt.putIfAbsent(sp.key(), at);
            LOGGER.info("[smoketest] M29c betta {} at {}", f.genome().species(), at);
            i++;
        }
    }

    // ------------------------------------------------------------------ part b: puffers, barbs, loaches

    private static final List<TankmateSpecies> PART_B_A = List.of(TankmateSpecies.DWARF_PUFFER, TankmateSpecies.RED_EYE_PUFFER,
            TankmateSpecies.TIGER_BARB, TankmateSpecies.ODESSA_BARB, TankmateSpecies.CHERRY_BARB);
    private static final List<TankmateSpecies> PART_B_B = List.of(TankmateSpecies.DWARF_CHAIN_LOACH, TankmateSpecies.RETICULATED_LOACH,
            TankmateSpecies.GARRA_RUFA, TankmateSpecies.GREEN_SPOTTED_PUFFER);

    private void partB() {
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX(), p.getY() + 2, p.getZ() + 760, java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::buildB));
        then(1, () -> mc.options.hideGui = true);
        then(80, () -> server(this::poseB));
        then(1, () -> look(dwarfs.getX() + 3.0, dwarfs.getY() + 1.0, dwarfs.getZ() - 4.6, 0F, 6F));
        then(60, () -> {
            shot("puffers_barbs");
            int n = 0;
            for (Entity e : mc.level.entitiesForRendering()) if (e instanceof TankmateEntity m && (PART_B_A.contains(m.species()) || PART_B_B.contains(m.species()))) n++;
            LOGGER.info("[smoketest] M29b client sees {} puffers, barbs and loaches", n);
            if (n < 15) LOGGER.error("[smoketest] M29b the client sees too few: {}", n);
        });
        then(1, () -> look(big.getX() + 3.0, big.getY() + 1.0, big.getZ() - 4.6, 0F, 6F));
        then(60, () -> shot("loaches"));
        List<TankmateSpecies> all = new ArrayList<>(PART_B_A);
        all.addAll(PART_B_B);
        for (TankmateSpecies s : all) {
            then(1, () -> {
                Vec3 at = firstOf.get(s);
                if (at == null) {
                    LOGGER.error("[smoketest] M29b no posed {}", s.id());
                    return;
                }
                double back = s.length() > 0.6 ? 1.7 : s.length() > 0.35 ? 1.3 : 1.15;
                look(at.x, at.y + 0.05, at.z - back, 0F, 2F);
            });
            then(40, () -> shot("sp_" + s.id()));
        }
        then(1, () -> mc.options.hideGui = false);
    }

    private void buildB(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(0, 0, -10);
        dwarfs = base;
        TankBlockEntity a = tank(level, p, dwarfs, 6, 2, 2, 26F);
        region(level, a);
        light(level, dwarfs, 6, 2, 1);
        big = base.offset(0, 0, -8);
        TankBlockEntity b = tank(level, p, big, 6, 2, 2, 26F);
        b.getTank().setSalinity(12F); // the green spotted puffer lives in brackish water
        region(level, b);
        light(level, big, 6, 2, 1);
        for (TankmateSpecies s : PART_B_A) {
            for (String v : s.varieties()) {
                fish(level, a, s, v, false);
                if (s.dimorphic()) fish(level, a, s, v, true);
            }
        }
        for (TankmateSpecies s : PART_B_B) fish(level, b, s, "wild", false);
    }

    private void poseB(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity a = TankMultiblock.getController(level, dwarfs);
        if (a != null) row(level, a, 6, new double[]{1.3, 0.55}, 0.95, firstOf);
        TankBlockEntity b = TankMultiblock.getController(level, big);
        if (b != null) row(level, b, 4, new double[]{0.3}, 1.4, firstOf);
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        BlockPos base = p.blockPosition().offset(0, 0, -10);
        dwarfs = base;
        TankBlockEntity d = tank(level, p, dwarfs, 6, 2, 2, 27F);
        InteriorRegion dr = region(level, d);
        decor(level, dr, DecorType.SLATE_CAVE, 12, 20);
        decor(level, dr, DecorType.COCONUT_CAVE, 70, 22);
        light(level, dwarfs, 6, 2, 1);
        big = base.offset(0, 0, -8);
        TankBlockEntity b = tank(level, p, big, 6, 3, 2, 29F);
        region(level, b);
        light(level, big, 6, 3, 1);
        brood = base.offset(9, 0, 0);
        TankBlockEntity bt = tank(level, p, brood, 2, 2, 2, 28F);
        InteriorRegion br = region(level, bt);
        decor(level, br, DecorType.SLATE_CAVE, 12, 12);
        light(level, brood, 2, 2, 0);
        // a first look at the ground loads the interior chunks of the brood tank (the pose looks again)
        TankmateSpots.openSpot(level.getServer(), bt.getTank(), TankmateSpecies.RAM_CICHLID, Vec3.atCenterOf(brood), level.getRandom());
        for (TankmateSpecies s : List.of(TankmateSpecies.RAM_CICHLID, TankmateSpecies.BOLIVIAN_RAM, TankmateSpecies.COCKATOO_CICHLID,
                TankmateSpecies.AGASSIZ_DWARF_CICHLID)) {
            for (String v : s.varieties()) {
                fish(level, d, s, v, false);
                if (s.dimorphic()) fish(level, d, s, v, true);
            }
        }
        for (TankmateSpecies s : List.of(TankmateSpecies.ANGELFISH, TankmateSpecies.DISCUS)) for (String v : s.varieties()) fish(level, b, s, v, false);
        fish(level, bt, TankmateSpecies.RAM_CICHLID, "wild", false);
        fish(level, bt, TankmateSpecies.RAM_CICHLID, "wild", true);
    }

    /** Every fish frozen in a row (side view from the north), the brood pair at its eggs on the slate. */
    private void pose(ServerPlayer p) {
        ServerLevel level = p.level();
        TankBlockEntity d = TankMultiblock.getController(level, dwarfs);
        if (d != null) row(level, d, 7, new double[]{1.35, 0.85, 0.35}, 0.8, firstOf);
        TankBlockEntity b = TankMultiblock.getController(level, big);
        if (b != null) row(level, b, 5, new double[]{1.6, 0.5}, 1.05, firstOf);
        TankBlockEntity bt = TankMultiblock.getController(level, brood);
        if (bt != null) {
            List<TankmateEntity> pair = TankmateBreeding.tankmates(level, bt.getTank());
            if (pair.size() < 2) return;
            TankmateEntity male = pair.stream().filter(m -> !m.isFemale()).findFirst().orElse(pair.get(0));
            TankmateEntity female = pair.stream().filter(TankmateEntity::isFemale).findFirst().orElse(pair.get(1));
            double[] sp = TankmateSpots.openSpot(level.getServer(), bt.getTank(), TankmateSpecies.RAM_CICHLID, Vec3.atCenterOf(brood.offset(0, 0, 1)), level.getRandom());
            LOGGER.info("[smoketest] M29 ram spot {} {} {} (stone: {})", sp[0], sp[1], sp[2], sp[4] > 0);
            if (sp[4] <= 0) LOGGER.error("[smoketest] M29 the rams found no flat stone on the slate cave");
            Clutch k = new Clutch(bt.getTank().nextClutchId(), TankmateSpecies.RAM_CICHLID, Clutch.Kind.OPEN, sp[0], sp[1], sp[2], 40,
                    female.genes(), male.genes());
            k.guardian = male.getUUID();
            k.partner = female.getUUID();
            bt.getTank().clutches().add(k);
            bt.markTankChanged();
            freezeAt(male, new Vec3(sp[0] - 0.16, sp[1] + 0.12, sp[2]), -90F);
            freezeAt(female, new Vec3(sp[0] + 0.18, sp[1] + 0.14, sp[2] + 0.05), 90F);
        }
    }

    private static void row(ServerLevel level, TankBlockEntity c, int perRow, double[] ys, double step, java.util.Map<TankmateSpecies, Vec3> firstOf) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        List<TankmateEntity> fish = new ArrayList<>(TankmateBreeding.tankmates(level, c.getTank()));
        fish.sort((a, b) -> a.species() != b.species() ? Integer.compare(a.species().ordinal(), b.species().ordinal())
                : a.variety().equals(b.variety()) ? Boolean.compare(a.isFemale(), b.isFemale()) : Integer.compare(a.species().varietyIndex(a.variety()),
                b.species().varietyIndex(b.variety())));
        for (int i = 0; i < fish.size(); i++) {
            int col = i % perRow, rowN = i / perRow;
            Vec3 at = new Vec3(w.box().minX + 0.4 + col * step, w.box().minY + ys[Math.min(rowN, ys.length - 1)], w.box().minZ + 0.6);
            freezeAt(fish.get(i), at, -90F);
            firstOf.putIfAbsent(fish.get(i).species(), at);
        }
    }

    private static void freezeAt(TankmateEntity m, Vec3 at, float yaw) {
        m.snapTo(at.x, at.y, at.z, yaw, 0F);
        SmokeFreeze.freeze(m, true);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
    }

    private void check() {
        int n = 0;
        for (Entity e : mc.level.entitiesForRendering()) if (e instanceof TankmateEntity m && m.species().cichlid()) n++;
        LOGGER.info("[smoketest] M29 client sees {} cichlids", n);
        if (n < 30) LOGGER.error("[smoketest] M29 the client sees too few cichlids: {}", n);
    }

    // ------------------------------------------------------------------ helpers

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(p.getUUID());
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion region(ServerLevel level, TankBlockEntity c) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        return r;
    }

    private static void decor(ServerLevel level, InteriorRegion r, DecorType type, int lx, int lz) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        var err = DecorItem.place(in, type, new BlockPos(r.originX() + lx, InteriorRegion.BASE_Y, r.originZ() + lz), Rotation.NONE, null);
        if (err != null) LOGGER.error("[smoketest] M29 decor {}: {}", type, err.getString());
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, DyeColor.WHITE));
        }
    }

    private static void fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, String variety, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3(w.box().minX + 0.5, w.box().minY + 0.5, w.box().minZ + 0.5), 0.3, 0.1, 0.3);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, variety), female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M29 {} {} not spawned", s.id(), variety);
        else e.health().feed();
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
        Screenshot.grab(mc.gameDirectory, prefix + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
