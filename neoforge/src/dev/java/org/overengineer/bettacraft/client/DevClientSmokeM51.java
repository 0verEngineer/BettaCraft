package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.aquarist.AquaristAccess;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristMenu;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.algae.TankAlgae;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M51 client smoke ({@code -Psmoketestm51}), screenshots {@code bettacraft_m51_*}: an aquarist comes to the player (his
 * offer in the chat with the buttons, his offer window), is hired and takes the breeding basin and the bed next to the
 * tanks; his menu (status, home, wage, the six tasks with levels, the rearing tank, the tanks around and whose they are),
 * en and de; him at work (fetching food at the basin, at the tanks); the button of the tank menu; the four looks side by
 * side and close; asleep in his bed at night. Checks (errors in the log): he came, was hired, found his home, worked at
 * the own tanks (log entries) and never at the foreign one.
 */
final class DevClientSmokeM51 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private volatile BlockPos base;
    private volatile AquaristEntity npc;
    private volatile BlockPos own1, own2, rearing, foreign;

    DevClientSmokeM51() {
        then(1, () -> server(p -> p.teleportTo(p.level(), p.getX() + 1300, p.getY() + 2, p.getZ(), java.util.Set.of(), 0F, 10F, false)));
        then(20, () -> server(this::build));
        then(60, () -> mc.options.hideGui = false);
        // the arrival and his offer
        then(1, () -> server(this::arrive));
        for (int i = 0; i < 12; i++) then(20, () -> server(this::faceHim));
        then(1, () -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen("")));
        then(20, () -> shot("offer_chat"));
        then(1, () -> mc.setScreen(null));
        String lang = mc.options.languageCode;
        then(1, () -> server(p -> AquaristMenu.open(p, npc)));
        then(30, () -> {
            if (!(mc.screen instanceof org.overengineer.bettacraft.client.screen.AquaristScreen)) LOGGER.error("[smoketest] M51 no offer window");
        });
        then(5, () -> shot("offer_menu"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("offer_menu_de"));
        then(1, () -> language(lang));
        then(5, () -> mc.setScreen(null));
        // hired: his menu
        then(5, () -> server(this::hire));
        then(10, () -> server(p -> AquaristMenu.open(p, npc)));
        then(30, () -> shot("menu"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_de"));
        then(1, () -> language(lang));
        then(5, () -> mc.setScreen(null));
        // the tank menu with the aquarist button
        then(5, () -> server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.level(), own1);
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> shot("tank_menu"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("tank_menu_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // at work
        then(1, () -> mc.options.hideGui = true);
        then(1, () -> look(base.getX() + 4.5, base.getY() + 5.5, base.getZ() - 9.5, -15F, 28F));
        then(60, () -> shot("working_1"));
        for (int i = 0; i < 40; i++) then(20, () -> server(this::followJob));
        then(1, () -> shot("working_2"));
        for (int i = 0; i < 40; i++) then(20, () -> server(this::followJob));
        then(1, () -> shot("working_3"));
        // the four looks
        then(1, () -> server(this::lineUp));
        then(1, () -> look(base.getX() + 1.8, base.getY() + 1.5, base.getZ() + 8.6, 0F, 4F));
        then(60, () -> shot("variants"));
        then(1, () -> look(base.getX() - 0.9, base.getY() + 1.55, base.getZ() + 10.6, -35F, 6F));
        then(40, () -> shot("npc_close"));
        // at night in his bed
        then(1, () -> server(this::night));
        then(300, () -> look(base.getX() + 2.3, base.getY() + 2.0, base.getZ() + 4.5, 90F, 35F));
        then(40, () -> shot("sleeping"));
        then(1, () -> server(this::report));
        then(5, () -> {});
    }

    // ------------------------------------------------------------------ the scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        level.setDayTime(1500);
        level.getGameRules().getRule(net.minecraft.world.level.gamerules.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.gamerules.GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS).set(false, level.getServer());
        level.setWeatherParameters(12000, 0, false, false);
        reach(p, false);
        base = p.blockPosition().offset(0, -2, 0);
        for (int x = -10; x <= 18; x++)
            for (int z = -14; z <= 16; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        // his workplace: the breeding basin, a chest with food and a wage, a reservoir with water; his bed
        level.setBlockAndUpdate(base, ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        level.setBlockAndUpdate(base.east(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(base.west(), ModBlocks.RESERVOIR.get().defaultBlockState());
        IItemHandler chest = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, base.east(), null);
        for (ItemStack s : List.of(new ItemStack(ModItems.BETTA_PELLETS.get(), 32), new ItemStack(ModItems.FISH_FLAKES.get(), 16),
                new ItemStack(Items.EMERALD, 8), new ItemStack(Items.BREAD, 12))) {
            ItemStack rest = s;
            for (int i = 0; i < chest.getSlots() && !rest.isEmpty(); i++) rest = chest.insertItem(i, rest, false);
        }
        IFluidHandler res = org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, base.west(), null);
        if (res != null) res.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
        BlockState foot = Blocks.CYAN_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
        level.setBlockAndUpdate(base.offset(0, 0, 3), foot);
        level.setBlockAndUpdate(base.offset(0, 0, 4), foot.setValue(BedBlock.PART, BedPart.HEAD));
        level.setBlockAndUpdate(base.offset(-1, 0, 3), Blocks.LANTERN.defaultBlockState());
        // the tanks: two own ones that need care, the rearing tank, a foreign one
        own1 = base.offset(6, 0, -4);
        own2 = base.offset(6, 0, 2);
        rearing = base.offset(11, 0, -1);
        foreign = base.offset(11, 0, 4);
        TankBlockEntity a = tank(level, own1, 3, 2, 2, p.getUUID());
        TankBlockEntity b = tank(level, own2, 3, 2, 2, p.getUUID());
        TankBlockEntity r = tank(level, rearing, 2, 2, 2, p.getUUID());
        TankBlockEntity f = tank(level, foreign, 2, 2, 2, UUID.nameUUIDFromBytes("m51-neighbour".getBytes()));
        a.getTank().setName("Betta Paradise");
        b.getTank().setName("Community");
        r.getTank().setName("Rearing");
        f.getTank().setName("Neighbour");
        f.getTank().setAquaristAccess(AquaristAccess.OWNER);
        RandomSource rnd = RandomSource.create(5101);
        for (int i = 0; i < 3; i++) {
            BettaEntity fish = betta(level, a, i % 2 == 0, 5100L + i);
            if (fish != null) fish.health().feedPoorly(2);
        }
        BettaEntity young = betta(level, b, false, 5110L);
        if (young != null) young.setBettaData(young.getBettaData().withStage(LifeStage.JUVENILE));
        BettaEntity young2 = betta(level, b, true, 5111L);
        if (young2 != null) young2.setBettaData(young2.getBettaData().withStage(LifeStage.JUVENILE));
        for (TankBlockEntity c : List.of(a, f)) for (AlgaeSim.Face fc : TankAlgae.wetFaces(c.getTank())) AlgaeSim.set(c.getTank().algae(), fc.pos(), fc.dir(), 0.7F);
        var min = b.getTank().min();
        for (int i = 0; i < 200 && TankPlants.coverage(b.getTank()) < 0.92; i++) {
            TankPlants.add(b, i % 2 == 0 ? FloatingPlantType.SALVINIA : FloatingPlantType.DUCKWEED, min.getX() + rnd.nextDouble() * 3, min.getZ() + rnd.nextDouble() * 2, 1F,
                    rnd.nextFloat() * 360F);
        }
        for (TankBlockEntity c : List.of(a, b, r, f)) {
            c.markTankChanged();
            org.overengineer.bettacraft.aquarist.AquaristRecords.get(level.getServer()).noteTank(level, c);
        }
        // the player owns tanks and bred something: an aquarist may come
        Progress.award(p, Progress.SPAWNED);
        p.teleportTo(level, base.getX() + 2.5, base.getY(), base.getZ() - 7.5, java.util.Set.of(), 200F, 5F, false);
        LOGGER.info("[m51] scene at {}: why not: {}", base, AquaristArrival.whyNot(p, true));
    }

    private void arrive(ServerPlayer p) {
        if (AquaristArrival.whyNot(p, true) != null) LOGGER.error("[smoketest] M51 the conditions are not met: {}", AquaristArrival.whyNot(p, true));
        npc = AquaristArrival.arrive(p);
        if (npc == null) {
            LOGGER.error("[smoketest] M51 no aquarist came (FAIL)");
            return;
        }
        npc.snapTo(base.getX() - 6.5, base.getY(), base.getZ() - 9.5, 0F, 0F);
        LOGGER.info("[m51] {} comes ({})", npc.getName().getString(), npc.getUUID());
    }

    private void faceHim(ServerPlayer p) {
        if (npc == null) return;
        Vec3 d = npc.getEyePosition().subtract(p.getEyePosition());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90F;
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), java.util.Set.of(), yaw, pitch, false);
    }

    private void hire(ServerPlayer p) {
        Component err = AquaristArrival.hire(p);
        if (err != null) LOGGER.error("[smoketest] M51 hire failed: {}", err.getString());
        npc.alwaysDay = true;
        npc.setRearingTank(TankMultiblock.getController(p.level(), rearing).getTank().id());
        if (!npc.housed()) LOGGER.error("[smoketest] M51 no home found: workplace {}, bed {}", npc.workplace(), npc.bed());
        LOGGER.info("[m51] hired: workplace {}, bed {}, tanks {}", npc.workplace(), npc.bed(), AquaristWork.tanks(p.level(), npc).size());
    }

    /** The camera follows him at a little distance. */
    private void followJob(ServerPlayer p) {
        if (npc == null || !npc.isAlive()) return;
        Vec3 at = npc.position();
        p.teleportTo(p.level(), at.x - 2.6, at.y + 1.2, at.z - 3.2, java.util.Set.of(), -40F, 22F, false);
    }

    private void lineUp(ServerPlayer p) {
        ServerLevel level = p.level();
        for (int i = 0; i < AquaristEntity.VARIANTS; i++) {
            AquaristEntity e = ModEntities.HIRED_AQUARIST.get().create(level);
            if (e == null) continue;
            e.setVariant(i);
            e.setCustomName(null);
            e.setNoAi(true);
            e.snapTo(base.getX() + i * 1.2, base.getY(), base.getZ() + 12.5, 180F, 0F);
            e.setYHeadRot(180F);
            e.setYBodyRot(180F);
            if (i == 1) e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.FISH_BAG.get()));
            if (i == 2) e.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(ModItems.BETTA_PELLETS.get()));
            level.addFreshEntity(e);
        }
    }

    private void night(ServerPlayer p) {
        p.level().setDayTime(13800);
        if (npc != null) npc.alwaysDay = false;
    }

    private void report(ServerPlayer p) {
        ServerLevel level = p.level();
        int work = 0;
        for (BlockPos t : List.of(own1, own2, rearing)) {
            TankBlockEntity c = TankMultiblock.getController(level, t);
            if (c != null) work += (int) c.getTank().log().stream().filter(e -> e.key().startsWith("log.bettacraft.aquarist")).count();
        }
        TankBlockEntity f = TankMultiblock.getController(level, foreign);
        long foreignWork = f == null ? -1 : f.getTank().log().stream().filter(e -> e.key().startsWith("log.bettacraft.aquarist")).count();
        LOGGER.info("[smoketest] M51 scene done: {} log entries of his work at the own tanks, {} at the foreign one, sleeping {}, jobs {}", work, foreignWork,
                npc != null && npc.isSleeping(), npc == null ? 0 : npc.jobsDone);
        if (work == 0) LOGGER.error("[smoketest] M51 he did no work at the own tanks");
        if (foreignWork != 0) LOGGER.error("[smoketest] M51 he worked at the foreign tank");
        if (npc == null || !npc.isSleeping()) LOGGER.error("[smoketest] M51 he does not sleep at night");
    }

    // ------------------------------------------------------------------ helpers

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, UUID owner) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setOwner(owner);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static BettaEntity betta(ServerLevel level, TankBlockEntity c, boolean female, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0);
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(level, c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) {
            LOGGER.error("[smoketest] M51 betta not inserted: {}", err.getString());
            return null;
        }
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof BettaEntity b && b.getBettaData().fishId().equals(d.fishId())) return b;
        return null;
    }

    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
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

    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m51_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
