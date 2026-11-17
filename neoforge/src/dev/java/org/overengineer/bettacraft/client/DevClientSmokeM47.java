package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.frog.FrogSpawn;
import org.overengineer.bettacraft.entity.frog.Tadpole;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.villager.Aquarists;
import org.overengineer.bettacraft.villager.ModVillagers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M47 client smoke ({@code -Psmoketestm47}, a normal world with structures, seed 42): the aquarist villager in a village
 * (an unemployed villager takes the breeding basin; close-ups from the front and the side, the basin alone), his trade
 * menu at the master level with a bred betta in the trade slot (the price by its genes), the ram forms Black and German
 * Blue next to the wild form in a tank, and a frog tank with a pair in amplexus, a clump of spawn among floating plants,
 * tadpoles of every age and a froglet. Screenshots {@code bettacraft_m47_*}.
 */
final class DevClientSmokeM47 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private volatile BlockPos village, basin, home;
    private volatile Villager aquarist;
    private int waited;

    DevClientSmokeM47() {
        then(1, () -> server(this::setup));
        then(1, () -> mc.options.hideGui = true);
        // part 1: the aquarist in a village
        then(40, () -> server(this::village));
        then(200, () -> server(this::hire));
        for (int i = 0; i < 30; i++) then(20, () -> server(p -> {
            if (aquarist != null && !Aquarists.isAquarist(aquarist)) waited += 20;
        }));
        then(1, () -> server(this::pose));
        then(80, () -> lookAt(basin.getX() + 0.5, basin.getY() + 1.6, basin.getZ() - 3.2, 0F, 12F));
        then(60, () -> shot("aquarist_village"));
        then(1, () -> lookAt(basin.getX() + 1.6, basin.getY() + 1.7, basin.getZ() - 1.8, 30F, 18F));
        then(40, () -> shot("aquarist_close"));
        then(1, () -> lookAt(basin.getX() + 0.5, basin.getY() + 1.3, basin.getZ() - 1.5, 0F, 35F));
        then(40, () -> shot("basin"));
        // part 2: the trade menu (master level, a bred betta in the trade slot)
        then(1, () -> mc.options.hideGui = false);
        then(5, () -> server(this::trade));
        then(40, () -> shot("trade_menu"));
        then(5, () -> server(this::sellBetta));
        then(30, () -> shot("trade_betta"));
        then(1, () -> {
            if (mc.player != null) mc.player.closeContainer();
            mc.setScreen(null);
        });
        then(1, () -> mc.options.hideGui = true);
        // part 3: the rams, part 4: the frogs
        then(5, () -> server(this::tanks));
        then(80, () -> lookAt(home.getX() + 1.5, home.getY() + 1.2, home.getZ() - 1.9, 0F, 8F));
        then(60, () -> shot("rams"));
        then(1, () -> lookAt(home.getX() + 6.5, home.getY() + 1.4, home.getZ() - 1.9, 0F, 14F));
        then(60, () -> shot("frogs"));
        then(1, () -> lookAt(home.getX() + 6.5, home.getY() + 3.3, home.getZ() - 0.9, 0F, 55F));
        then(40, () -> shot("frog_spawn_top"));
        then(1, () -> lookAt(home.getX() + 6.2, home.getY() + 1.1, home.getZ() - 0.9, 0F, 10F));
        then(40, () -> shot("frogs_close"));
        then(1, () -> lookAt(home.getX() + 5.75, home.getY() + 1.62, home.getZ() + 0.7, -90F, 8F));
        then(40, () -> shot("amplexus"));
        then(1, () -> server(this::report));
        then(5, () -> {});
    }

    // ------------------------------------------------------------------ setup

    private void setup(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(6000);
        level.getGameRules().set(GameRules.ADVANCE_TIME, false, level.getServer());
        level.getGameRules().set(GameRules.ADVANCE_WEATHER, false, level.getServer());
        level.setWeatherParameters(12000, 0, false, false);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    /** The nearest village: the player goes there; a basin on the ground at the edge of the village street. */
    private void village(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, p.blockPosition(), 100, false);
        LOGGER.info("[m47] village at {}", found);
        if (found == null) {
            LOGGER.error("[smoketest] M47 no village found (FAIL)");
            found = p.blockPosition().offset(200, 0, 0);
        }
        level.getChunk(found);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
        village = new BlockPos(found.getX(), y, found.getZ());
        p.teleportTo(level, village.getX() + 0.5, village.getY() + 6, village.getZ() - 6, java.util.Set.of(), 0F, 30F, false);
    }

    /** A basin next to the village centre and an unemployed villager beside it; the villager takes the job by himself. */
    private void hire(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos b = village.offset(3, 0, 3);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ());
        basin = new BlockPos(b.getX(), y, b.getZ());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -3; dz <= 2; dz++) {
            for (int dy = 0; dy < 3; dy++) level.setBlock(basin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(basin.offset(dx, -1, dz), Blocks.DIRT_PATH.defaultBlockState(), 2);
        }
        level.setBlock(basin, ModBlocks.BREEDING_BASIN.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                Direction.NORTH), 3);
        Villager v = EntityType.VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        v.snapTo(basin.getX() + 1.5, basin.getY(), basin.getZ() + 0.5, 90F, 0F);
        v.setPersistenceRequired();
        level.addFreshEntity(v);
        aquarist = v;
        LOGGER.info("[m47] basin at {}, villager {}", basin, v.getVillagerData().profession());
    }

    /** The aquarist stands still beside his basin, facing the camera. */
    private void pose(ServerPlayer p) {
        Villager v = aquarist;
        // ours or a villager of the village (the first unemployed one to reach the basin claims it)
        List<Villager> near = p.level().getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(basin).inflate(24), Aquarists::isAquarist);
        boolean hired = !near.isEmpty();
        LOGGER.info("[smoketest] M47 an unemployed villager took the breeding basin: {} ({} aquarists near, ours {}) {}", hired, near.size(),
                Aquarists.isAquarist(v), hired ? "PASS" : "FAIL");
        for (Villager o : near) if (o != v) o.discard(); // the scene shows ours
        if (!Aquarists.isAquarist(v)) v.setVillagerData(v.getVillagerData().withProfession(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(ModVillagers.AQUARIST.get())));
        v.setVillagerData(v.getVillagerData().withLevel(3));
        v.snapTo(basin.getX() - 0.4, basin.getY(), basin.getZ() - 0.9, 180F, 0F);
        v.setYHeadRot(180F);
        v.setYBodyRot(180F);
        SmokeFreeze.freeze(v, false);
    }

    private static List<MerchantOffer> allOffers(Villager v) {
        List<MerchantOffer> out = new ArrayList<>();
        var trades = VillagerTrades.TRADES.get(ModVillagers.AQUARIST.get());
        var r = v.getRandom();
        for (int level = 1; level <= 5; level++) {
            for (var l : trades.get(level)) {
                MerchantOffer o = l.getOffer((net.minecraft.server.level.ServerLevel) v.level(), v, r);
                if (o != null) out.add(o);
            }
        }
        return out;
    }

    private void trade(ServerPlayer p) {
        Villager v = aquarist;
        v.setVillagerData(v.getVillagerData().withLevel(5));
        MerchantOffers offers = new MerchantOffers();
        offers.addAll(allOffers(v));
        v.setOffers(offers);
        LOGGER.info("[m47] aquarist offers: {}", offers.size());
        v.setTradingPlayer(p);
        v.openTradingScreen(p, v.getDisplayName(), 5);
    }

    /** A bred, long-finned dumbo dalmatian in a bucket in the trade slot: the result shows its price. */
    private void sellBetta(ServerPlayer p) {
        if (!(p.containerMenu instanceof MerchantMenu menu)) {
            LOGGER.error("[smoketest] M47 no trade menu open (FAIL)");
            return;
        }
        BettaGenome fancy = new BettaGenome(BettaGenome.SPECIES_BETTA, "halfmoon", BettaGenome.LONG, "dumbo", "white", "red", "dalmatian", "black",
                "high", "turquoise");
        BettaData d = new BettaData(BettaGenotype.homozygous(fancy), false, "adult", 0, UUID.randomUUID(), Optional.empty(), Optional.empty(), 4, 0L,
                new BettaHealth().save(), new CompoundTag(), new CompoundTag());
        menu.setSelectionHint(1);
        menu.getSlot(0).set(BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d));
        menu.slotsChanged(menu.getSlot(0).container);
        menu.broadcastChanges();
        ItemStack r = menu.getSlot(2).getItem();
        LOGGER.info("[smoketest] M47 the aquarist pays {} for the bred betta {}", r, r.getCount() >= 6 ? "PASS" : "FAIL");
    }

    // ------------------------------------------------------------------ the tanks

    private TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, float temp) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlock(pos, ModBlocks.TANK.get().defaultBlockState(), 2);
        TankData t = TankMultiblock.formAll(level, blocks);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        t.setFront(Direction.NORTH);
        t.setSignVisible(false);
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, c, t.capacityMb());
        t.setWaterTemp(temp);
        t.setTargetTemp(temp);
        return c;
    }

    private void tanks(ServerPlayer p) {
        ServerLevel level = p.level();
        home = p.blockPosition().offset(0, 30, 12);
        for (int x = -3; x < 13; x++) for (int z = -4; z < 5; z++) {
            for (int y = 0; y < 6; y++) level.setBlock(home.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(home.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        }
        // part 3: rams: black and German blue pairs and a wild one
        TankBlockEntity rams = tank(level, home, 3, 2, 2, 28F);
        TankWaterVolume w = new TankWaterVolume(rams.getTank());
        TankmateSpecies s = TankmateSpecies.RAM_CICHLID;
        String[][] fish = {{"black", "m"}, {"black", "f"}, {"german_blue", "m"}, {"german_blue", "f"}, {"wild", "m"}};
        for (int i = 0; i < fish.length; i++) {
            Vec3 at = w.clamp(new Vec3(home.getX() + 0.45 + i * 0.52, home.getY() + 0.55 + (i % 2) * 0.55, home.getZ() + 0.6), 0.3, 0.1, 0.3);
            TankmateEntity e = TankmateBreeding.spawn(level, rams, s, TankmateGenes.pure(s, fish[i][0]), fish[i][1].equals("f"), LifeStage.ADULT, 0, at);
            if (e == null) continue;
            e.setYRot(90F);
            e.setYBodyRot(90F);
            e.setYHeadRot(90F);
            SmokeFreeze.freeze(e, true);
        }
        // part 4: frogs: a pair in amplexus, a froglet, spawn among floating plants, tadpoles of every age
        BlockPos fm = home.offset(5, 0, 0);
        TankBlockEntity frogs = tank(level, fm, 3, 2, 2, 26F);
        TankData t = frogs.getTank();
        org.overengineer.bettacraft.tank.TankInteractions.changeWater(level, frogs, -1000); // air for the frogs
        TankWaterVolume fw = new TankWaterVolume(t);
        double pairY = fw.surface() - 0.3;
        DwarfFrogEntity male = frog(level, frogs, false, new Vec3(fm.getX() + 1.4, pairY, fm.getZ() + 0.7));
        DwarfFrogEntity female = frog(level, frogs, true, new Vec3(fm.getX() + 1.4, pairY, fm.getZ() + 0.7));
        DwarfFrogEntity young = frog(level, frogs, true, new Vec3(fm.getX() + 0.5, fm.getY() + 0.05, fm.getZ() + 0.6));
        if (young != null) young.makeFroglet();
        if (male != null && female != null) {
            SmokeFreeze.freeze(female, true);
            male.clasp(female);
        }
        double surf = fw.surface();
        for (int i = 0; i < 4; i++) {
            t.plants().add(new FloatingPlant(t.nextPlantId(), i % 2 == 0 ? FloatingPlantType.FROGBIT : FloatingPlantType.SALVINIA,
                    fm.getX() + 0.4 + i * 0.6, fm.getZ() + 1.4 - (i % 2) * 0.3, 1F, i * 40F));
        }
        t.frogSpawn().add(new FrogSpawn(t.nextFrogBroodId(), fm.getX() + 2.2, surf, fm.getZ() + 0.8, 36));
        t.frogSpawn().add(new FrogSpawn(t.nextFrogBroodId(), fm.getX() + 0.8, surf, fm.getZ() + 0.6, 18));
        for (int i = 0; i < 14; i++) {
            Vec3 at = fw.clamp(new Vec3(fm.getX() + 0.4 + (i % 7) * 0.33, fm.getY() + 0.5 + (i / 7) * 0.4, fm.getZ() + 0.45 + (i % 3) * 0.2), 0.1, 0.1, 0.1);
            Tadpole k = new Tadpole(t.nextFrogBroodId(), at.x, at.y, at.z);
            k.age = org.overengineer.bettacraft.entity.frog.FrogBreeding.tadpoleDays() * (i / 13.0) * 0.98;
            t.tadpoles().add(k);
        }
        frogs.markTankChanged();
        rams.markTankChanged();
        LOGGER.info("[m47] tanks at {}: rams, frogs (amplexus {}), spawn {}, tadpoles {}", home, male != null && male.inAmplexus(), t.frogSpawn().size(),
                t.tadpoles().size());
    }

    private static DwarfFrogEntity frog(ServerLevel level, TankBlockEntity c, boolean female, Vec3 at) {
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (f == null) return null;
        f.setFemale(female);
        f.snapTo(at.x, at.y, at.z, 0F, 0F);
        f.setYRot(0F);
        TankInhabitants.adopt(f, c);
        level.addFreshEntity(f);
        c.getTank().inhabitants().add(f.getUUID());
        return f;
    }

    private void report(ServerPlayer p) {
        LOGGER.info("[smoketest] M47 scene done: aquarist {}, tanks at {}", aquarist == null ? null : aquarist.getVillagerData(), home);
    }

    // ------------------------------------------------------------------ steps

    private void lookAt(double x, double eyeY, double z, float yaw, float pitch) {
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m47_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
