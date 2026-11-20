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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.compat.FishContainers;
import org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M7 part of the dev client smoke test: fish of the aquarium mods in a Bettacraft tank (with Fintastic plants in the
 * interior), a betta in a Better Fish Tanks tank, a betta on the Fish of Thieves plaque and the tank menu with a Better
 * Fish Tanks bag. Steps of mods that are not installed are left out. Screenshots: run/screenshots/bettacraft_m7_*.png.
 */
final class DevClientSmokeM7 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final String[] FOREIGN = {"fishofthieves:splashtail_bucket", "fintastic:guppy_bucket", "aquaculture:bluegill_bucket",
            "fintastic:gourami_bucket", "naturalist:bass_bucket", "fishofthieves:pondie_bucket", "fintastic:pleco_bucket",
            "aquaculture:minnow_bucket"};

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private BlockPos mixMin, bftMin, plaquePos;

    DevClientSmokeM7() {
        then(1, () -> server(this::build));
        then(30, () -> server(this::fillBft));
        then(1, () -> mc.options.hideGui = true);
        // our tank with the fish of other mods (and Fintastic plants in the miniature)
        then(40, () -> server(p -> p.teleportTo(p.level(), mixMin.getX() + 2.5, mixMin.getY() + 0.1, mixMin.getZ() - 2.0, java.util.Set.of(), 0F, 8F, false)));
        then(60, () -> shot("bettacraft_m7_foreign_fish"));
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(BetterFishTanksCompat.MOD_ID)) {
            then(1, () -> server(p -> p.teleportTo(p.level(), bftMin.getX() + 1.5, bftMin.getY() + 0.1, bftMin.getZ() - 1.9, java.util.Set.of(), 0F, 10F, false)));
            then(60, () -> {
                LOGGER.info("[smoketest] M7 BFT client state {} fluid {}", mc.level.getBlockState(bftMin), mc.level.getFluidState(bftMin));
                server(p -> LOGGER.info("[smoketest] M7 BFT server state {} fluid {}", p.level().getBlockState(bftMin), p.level().getFluidState(bftMin)));
                shot("bettacraft_m7_bft_tank");
            });
        }
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fishofthieves")) {
            then(1, () -> server(p -> p.teleportTo(p.level(), plaquePos.getX() + 0.5, plaquePos.getY() - 1.12, plaquePos.getZ() - 0.85, java.util.Set.of(), 0F, 0F, false)));
            then(60, () -> shot("bettacraft_m7_plaque"));
        }
        // tank menu, animals tab, with Better Fish Tanks bags in the inventory: M52: one of them filled through the menu's take-out
        // (their full bag, beside an empty one: their empty bag looks like a water bottle, the screenshots seemed to show none)
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            p.teleportTo(p.level(), mixMin.getX() + 2.5, mixMin.getY() + 0.5, mixMin.getZ() - 2.5, java.util.Set.of(), 0F, 20F, false);
            TankBlockEntity c = TankMultiblock.getController(p.level(), mixMin);
            if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(BetterFishTanksCompat.MOD_ID)) {
                p.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.getValue(FishContainers.BFT_FISH_BAG), 2));
                Entity betta = null;
                for (Entity e : TankInhabitants.liveInhabitants(p.level(), c.getTank())) if (e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity) betta = e;
                Component err = betta == null ? Component.literal("no betta") : TankInhabitants.takeOut(p, c, betta.getUUID(), true);
                int full = 0, empty = 0;
                for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                    if (FishContainers.kind(s) != FishContainers.Foreign.BFT_BAG) continue;
                    if (FishContainers.storedEntity(s) != null) full += s.getCount();
                    else empty += s.getCount();
                }
                LOGGER.info("[smoketest] M7 betta taken out into a Better Fish Tanks bag: {}, their bags in the inventory: {} full, {} empty",
                        err == null ? "ok" : err.getString(), full, empty);
            }
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(1);
        });
        then(30, () -> shot("bettacraft_m7_menu_animals"));
        // the interior with the Fintastic plants, at full size
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fintastic")) {
            then(1, () -> server(p -> {
                p.closeContainer();
                InteriorManager.enter(p, mixMin);
            }));
            then(80, () -> server(p -> {
                InteriorRegion r = InteriorManager.regionAt(p.level().getServer(), p.blockPosition());
                if (r == null) return;
                BlockPos eye = r.toWorld(30, 2, 14);
                p.teleportTo(p.level(), eye.getX() + 0.5, eye.getY(), eye.getZ() + 0.5, java.util.Set.of(), 0F, 12F, false);
            }));
            then(1, () -> mc.options.hideGui = true);
            then(50, () -> shot("bettacraft_m7_interior_plants"));
            then(1, () -> {
                mc.options.hideGui = false;
                server(InteriorManager::leave);
            });
            then(40, () -> {});
        }
    }

    /** M52-final: a copy of DevSmokeM7#foreignBucket (that class needs NeoForge's fake players; this scene also runs on Fabric). */
    private static ItemStack foreignBucket(Item item) {
        ItemStack stack = new ItemStack(item);
        if ("fintastic:gourami_bucket".equals(BuiltInRegistries.ITEM.getKey(item).toString())) {
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putInt("VariantModel", 1);
            t.putInt("VariantSkin", 11);
            stack.set(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(t));
        }
        return stack;
    }

    private void build(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos feet = p.blockPosition();
        BlockPos base = feet.offset(0, 0, 40);
        p.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() - 3.0, java.util.Set.of(), 0F, 0F, false);
        mixMin = base.offset(-8, 0, 0);
        for (int x = 0; x < 5; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            BlockPos pos = mixMin.offset(x, y, z);
            level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
            TankMultiblock.onTankBlockAdded(level, pos, null);
        }
        TankBlockEntity c = TankMultiblock.getController(level, mixMin);
        TankInteractions.changeWater(level, c, 20000);
        c.getTank().setName("Community tank");
        c.getTank().setSignVisible(false);
        c.getTank().setFront(Direction.NORTH);
        List<String> in = new ArrayList<>();
        for (String id : FOREIGN) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item == Items.AIR) continue;
            ItemStack stack = foreignBucket(item);
            var err = TankInhabitants.insert(level, c, stack, TankInhabitants.containedType(stack), p, 0L);
            if (err != null) LOGGER.warn("[smoketest] M7 {}: {}", id, err.getString());
            else in.add(id);
        }
        for (int i = 0; i < 2; i++) {
            var d = BettaData.create(BettaGenome.randomDomestic(level.getRandom()), i == 1, level.getGameTime());
            TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d),
                    TankInhabitants.containedType(new ItemStack(ModItems.BETTA_BUCKET.get())), p, 0L);
        }
        // line them up (motionless, side view) so every species is visible
        List<Entity> live = TankInhabitants.liveInhabitants(level, c.getTank());
        for (int i = 0; i < live.size(); i++) {
            Entity e = live.get(i);
            if (e instanceof Mob m) SmokeFreeze.freeze(m, false);
            double x = mixMin.getX() + 0.5 + (i % 5);
            double y = mixMin.getY() + 0.35 + (i / 5) * 0.8;
            e.snapTo(x, y, mixMin.getZ() + 0.9, i % 2 == 0 ? -90F : 90F, 0F);
            e.setDeltaMovement(Vec3.ZERO);
        }
        LOGGER.info("[smoketest] M7 community tank: {} + 2 bettas ({} animals)", in, live.size());
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fintastic")) plants(level, c);
        bftMin = base.offset(0, 0, 0);
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(BetterFishTanksCompat.MOD_ID)) {
            Block bft = BuiltInRegistries.BLOCK.getValue(BetterFishTanksCompat.TANK_BLOCK);
            for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
                level.setBlockAndUpdate(bftMin.offset(x, y, z), bft.defaultBlockState());
        }
        plaquePos = base.offset(6, 1, 0);
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("fishofthieves")) {
            level.setBlockAndUpdate(plaquePos.south(), Blocks.STONE_BRICKS.defaultBlockState());
            Block oak = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fishofthieves", "oak_fish_plaque"));
            BlockState s = oak.defaultBlockState();
            Property<?> facing = oak.getStateDefinition().getProperty("facing");
            if (facing != null) s = setNorth(s, facing);
            level.setBlockAndUpdate(plaquePos, s);
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(
                    BettaGenome.of(java.util.Map.of("finform", "halfmoon", "fin_length", "long", "body_color", "royal_blue", "fin_color", "red",
                            "pattern", "butterfly", "pattern_color", "white", "iridescence", "high"), BettaGenome.DEFAULT), false, level.getGameTime()));
            p.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            level.getBlockState(plaquePos).useItemOn(bucket, level, p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(plaquePos), Direction.NORTH, plaquePos, false));
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setNorth(BlockState s, Property facing) {
        return facing.getPossibleValues().contains(Direction.NORTH) ? s.setValue(facing, Direction.NORTH) : s;
    }

    /** Fintastic plants in the interior of the community tank: hornwort, anubias, amazon swords and duckweed on top. */
    private void plants(ServerLevel level, TankBlockEntity c) {
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        String[] floor = {"hornwort", "anubias", "amazon_sword", "hornwort", "blue_hypnea", "anubias", "mermaid_fan", "hornwort"};
        int placed = 0;
        for (int i = 0; i < 40; i++) {
            Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fintastic", floor[i % floor.length]));
            if (b == Blocks.AIR) continue;
            BlockPos pos = r.toWorld(2 + (i * 2) % 76, 0, 22 + (i * 7) % 9);
            in.setBlockAndUpdate(pos, b.defaultBlockState());
            placed++;
        }
        Block duckweed = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("fintastic", "duckweed"));
        for (int i = 0; i < 24; i++) in.setBlockAndUpdate(r.toWorld(4 + (i * 3) % 72, r.sizeY() - 2, 8 + (i * 5) % 20), duckweed.defaultBlockState());
        InteriorSync.markAllDirty(r.id());
        LOGGER.info("[smoketest] M7 {} Fintastic plants and 24 duckweed in the interior", placed);
    }

    private void fillBft(ServerPlayer p) {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(BetterFishTanksCompat.MOD_ID)) return;
        ServerLevel level = p.level();
        var be = level.getBlockEntity(bftMin);
        try {
            Object cap = be.getClass().getMethod("getCapacity").invoke(be);
            BetterFishTanksCompat.fillWater(be, 10000);
            LOGGER.info("[smoketest] M7 BFT tank: capacity {}, water {}", cap, BetterFishTanksCompat.waterAmount(be));
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M7 BFT addWater: {}", e.toString());
        }
        for (int i = 0; i < 2; i++) {
            ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                    BettaData.create(BettaGenome.randomDomestic(level.getRandom()), i == 1, level.getGameTime()));
            p.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            var err = BetterFishTanksCompat.release(level, bftMin, p, InteractionHand.MAIN_HAND, bucket);
            LOGGER.info("[smoketest] M7 betta into the Better Fish Tanks tank: {}", err == null ? "ok" : err.getString());
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
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
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
