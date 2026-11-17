package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockItem;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.Seahorses;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.8 dev client smoke scene ({@code -Psmoketestm21_8}), screenshots {@code bettacraft_m21_8_*}:
 * <ul>
 *     <li>a seahorse reef tank (seagrass, kelp, corals on reef rock, a rock pillar; lined seahorses of every colour and
 *     dwarf seahorses) left to swim freely twice, then posed at their posts close up; a pair with a full brood pouch and
 *     fry in a small tank;</li>
 *     <li>a placed clear glass tank next to a textured one (the block unchanged), the clear glass tank items in the hotbar,
 *     the inventory and JEI, stacks of cyst jars (M35.1: one jar, one culture; an old one) with their tooltips;</li>
 *     <li>the animals and breeding tabs of the menu with a seahorse, and the handbook page (en, de).</li>
 * </ul>
 * Checks (errors in the log): the client sees the seahorses, some of them hold on to a post while they swim freely, the
 * brood pouch is synced, and the cyst jars stack to 64 on the client (M35.1).
 */
final class DevClientSmokeM218 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final String lang = mc.options.languageCode;
    private BlockPos base, reef, pouch, glass;
    private volatile int serverStack = -1;
    private int wait;

    DevClientSmokeM218() {
        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 2, p.getZ() + 620, 0F, 10F)));
        then(20, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // 1. free swimming: the seahorses find their posts themselves
        then(60, () -> look(reef.getX() + 2.5, reef.getY() + 1.4, reef.getZ() - 4.2, 0F, 8F));
        then(600, () -> {
            shot("tank_free_1");
            checkFree();
        });
        then(300, () -> {
            shot("tank_free_2");
            checkFree();
        });
        then(1, () -> look(reef.getX() + 1.5, reef.getY() + 0.9, reef.getZ() - 1.7, 0F, 12F));
        then(40, () -> shot("free_close"));
        // 2. posed at their posts
        then(1, () -> server(this::pose));
        then(1, () -> look(reef.getX() + 2.5, reef.getY() + 1.4, reef.getZ() - 4.2, 0F, 8F));
        then(60, () -> shot("tank"));
        then(1, () -> look(reef.getX() + 1.6, reef.getY() + 0.8, reef.getZ() - 1.5, 0F, 10F));
        then(40, () -> shot("hitched"));
        then(1, () -> look(reef.getX() + 3.6, reef.getY() + 0.8, reef.getZ() - 1.5, 0F, 10F));
        then(40, () -> shot("hitched_2"));
        // 3. the brood pouch and the fry
        then(1, () -> look(pouch.getX() + 1.0, pouch.getY() + 0.8, pouch.getZ() - 1.4, 0F, 10F));
        then(60, () -> {
            shot("pouch");
            checkPouch();
        });
        // 4. clear glass: placed (unchanged) and as items
        then(1, () -> look(glass.getX() + 1.5, glass.getY() + 1.6, glass.getZ() - 3.0, 0F, 18F));
        then(40, () -> shot("glass_placed"));
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(this::fillInventory));
        then(1, () -> {
            if (mc.player != null) mc.player.getInventory().selected = 8;
        });
        then(40, () -> {
            mc.getToasts().clear();
            checkStack();
        });
        then(5, () -> shot("hotbar"));
        then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        then(10, () -> {
            boolean jei = org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei") && org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.filterForTest("glass tank");
            LOGGER.info("[smoketest] M21.8 JEI filter set: {}", jei);
        });
        then(20, () -> shot("inventory"));
        then(1, () -> hover(s -> s.is(ModItems.ARTEMIA_CYSTS.get()) && s.getCount() == 16));
        then(10, () -> shot("cysts_tooltip"));
        then(1, () -> hover(s -> s.is(ModItems.ARTEMIA_CYSTS.get()) && s.getCount() == 3));
        then(10, () -> shot("cysts_old_tooltip"));
        then(1, () -> hover(s -> s.getItem() instanceof TankBlockItem && TankBlockItem.getStyle(s) == TankStyle.CLEAN));
        then(10, () -> shot("glass_tooltip"));
        then(1, () -> language("de_de"));
        then(10, () -> hover(s -> s.is(ModItems.ARTEMIA_CYSTS.get()) && s.getCount() == 16));
        then(10, () -> shot("cysts_tooltip_de"));
        then(1, () -> language(lang));
        then(1, () -> {
            if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei")) org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.filterForTest("");
        });
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> p.setGameMode(net.minecraft.world.level.GameType.CREATIVE)));
        // 5. menus
        then(1, () -> server(p -> p.teleportTo(p.serverLevel(), reef.getX() + 2.5, reef.getY() + 0.5, reef.getZ() - 2.5, 0F, 0F)));
        menu(1, "menu_seahorse", s -> s.showTankmateForTest("lined_seahorse"));
        menu(2, "menu_breeding", null);
        // 6. handbook
        then(1, () -> mc.options.hideGui = true);
        guide("en");
        then(1, () -> language("de_de"));
        guide("de");
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ scene

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        serverStack = new ItemStack(ModItems.ARTEMIA_CYSTS.get()).getMaxStackSize();
        base = p.blockPosition().offset(0, 0, -10);
        reef = base;
        TankBlockEntity c = tank(level, p, reef, 5, 2, 3, 24F, 33F);
        InteriorRegion r = region(level, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        // seagrass meadow and kelp in front, a rock pillar and corals on reef rock behind
        for (int x = 4; x < 76; x++) {
            for (int z = 4; z < 22; z++) {
                int k = (x * 7 + z * 13) % 11;
                if (k == 0) in.setBlock(r.toWorld(x, 0, z), Blocks.TALL_SEAGRASS.defaultBlockState(), 2);
                else if (k == 3 || k == 6) in.setBlock(r.toWorld(x, 0, z), Blocks.SEAGRASS.defaultBlockState(), 2);
            }
        }
        for (int[] kp : new int[][]{{10, 30}, {28, 26}, {62, 28}}) {
            for (int y = 0; y < 22; y++) in.setBlock(r.toWorld(kp[0], y, kp[1]), (y == 21 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        }
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) for (int y = 0; y < 18; y++) in.setBlock(r.toWorld(44 + x, y, 30 + z), Blocks.STONE.defaultBlockState(), 2);
        rock(in, r, 20, 38, 7, 5);
        rock(in, r, 58, 38, 7, 6);
        coral(in, r, PlantSpecies.LEATHER_CORAL, 20, 38, 2181L);
        coral(in, r, PlantSpecies.HAMMER_CORAL, 58, 38, 2182L);
        light(level, reef, 5, 2, 1);
        // the seahorses
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, "wild", true, LifeStage.ADULT);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, "wild", false, LifeStage.ADULT);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, "yellow", false, LifeStage.ADULT);
        fish(level, c, TankmateSpecies.LINED_SEAHORSE, "orange", true, LifeStage.ADULT);
        fish(level, c, TankmateSpecies.DWARF_SEAHORSE, "wild", true, LifeStage.ADULT);
        fish(level, c, TankmateSpecies.DWARF_SEAHORSE, "wild", false, LifeStage.ADULT);
        // the pouch tank: a pair, the male with a full brood pouch, and fry
        pouch = base.offset(8, 0, 0);
        TankBlockEntity pc = tank(level, p, pouch, 2, 2, 2, 23F, 33F);
        InteriorRegion pr = region(level, pc);
        for (int y = 0; y < 20; y++) in.setBlock(pr.toWorld(16, y, 20), (y == 19 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
        light(level, pouch, 2, 2, 0);
        TankmateEntity male = fish(level, pc, TankmateSpecies.LINED_SEAHORSE, "yellow", false, LifeStage.ADULT);
        TankmateEntity female = fish(level, pc, TankmateSpecies.LINED_SEAHORSE, "orange", true, LifeStage.ADULT);
        if (male != null && female != null) male.setGravid(1.0, female.genes());
        for (int i = 0; i < 5; i++) fish(level, pc, TankmateSpecies.LINED_SEAHORSE, i % 2 == 0 ? "yellow" : "orange", i % 2 == 0, LifeStage.FRY);
        // clear glass next to textured glass
        glass = base.offset(-6, 0, 0);
        tank(level, p, glass, 1, 1, 1, 24F, 0F);
        tank(level, p, glass.offset(2, 0, 0), 1, 1, 1, 24F, 0F);
        TankBlockEntity clear = TankMultiblock.getController(level, glass);
        if (clear != null) {
            clear.setStyle(TankStyle.CLEAN);
            level.sendBlockUpdated(glass, level.getBlockState(glass), level.getBlockState(glass), 3);
        }
        for (int x = -1; x < 4; x++) for (int y = -1; y < 3; y++) level.setBlockAndUpdate(glass.offset(x, y, 2), Blocks.WHITE_CONCRETE.defaultBlockState());
        Seahorses.forget();
        LOGGER.info("[smoketest] M21.8 hitching spots in the reef tank: {}", Seahorses.spots(level, c.getTank()).size());
        if (Seahorses.spots(level, c.getTank()).isEmpty()) LOGGER.warn("[smoketest] M21.8 no hitching posts found yet (interior chunks loading)");
    }

    /** Every seahorse frozen at a post of its own, tail curled; the pouch tank posed close up. */
    private void pose(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        TankBlockEntity c = TankMultiblock.getController(level, reef);
        if (c == null) return;
        Seahorses.forget();
        var spots = new ArrayList<>(Seahorses.spots(level, c.getTank()));
        LOGGER.info("[smoketest] M21.8 hitching spots: {}", spots.size());
        if (spots.isEmpty()) LOGGER.error("[smoketest] M21.8 no hitching posts in the reef tank");
        // front spots first (the camera is in the north)
        spots.sort((a, b) -> Double.compare(a.pos().z, b.pos().z));
        int i = 0;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, c.getTank())) {
            if (spots.isEmpty()) break;
            var s = spots.get(Math.min(spots.size() - 1, (i * 7) % Math.max(1, Math.min(spots.size(), 24))));
            Vec3 d = s.post().subtract(s.pos());
            float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90F;
            freezeAt(m, s.pos(), yaw, TankmateEntity.FLAG_REST);
            i++;
        }
        TankBlockEntity pc = TankMultiblock.getController(level, pouch);
        if (pc != null) {
            TankWaterVolume w = new TankWaterVolume(pc.getTank());
            int fry = 0;
            for (TankmateEntity m : TankmateBreeding.tankmates(level, pc.getTank())) {
                double cx = w.box().minX + 1.0, y0 = w.box().minY + 0.35;
                if (!m.isAdult()) {
                    freezeAt(m, new Vec3(cx - 0.5 + fry * 0.22, y0 + 0.3 + (fry % 2) * 0.12, w.box().minZ + 0.45), 0F, 0);
                    fry++;
                } else if (m.isFemale()) {
                    freezeAt(m, new Vec3(cx + 0.32, y0, w.box().minZ + 0.55), -90F, 0);
                } else {
                    freezeAt(m, new Vec3(cx - 0.25, y0, w.box().minZ + 0.55), -90F, TankmateEntity.FLAG_REST | TankmateEntity.FLAG_GRAVID);
                }
            }
        }
    }

    private static void freezeAt(TankmateEntity m, Vec3 at, float yaw, int flags) {
        m.moveTo(at.x, at.y, at.z, yaw, 0F);
        SmokeFreeze.freeze(m, true);
        m.setDeltaMovement(Vec3.ZERO);
        m.setYRot(yaw);
        m.yBodyRot = yaw;
        m.yHeadRot = yaw;
        m.setPoseForSmoke(flags);
    }

    private void fillInventory(ServerPlayer p) {
        // survival: the plain inventory screen (the creative one has other slots) and the hotbar with counts
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.getInventory().clearContent();
        ResourceLocation glassId = ResourceLocation.withDefaultNamespace("glass");
        ItemStack old = new ItemStack(ModItems.ARTEMIA_CYSTS.get(), 3);
        old.set(DataComponents.DAMAGE, 5); // M35.1: an old jar; it reaches the client without its old uses
        ItemStack[] hotbar = {
                TankBlockItem.of(ModItems.TANK.get(), glassId, TankStyle.CLEAN, 1),
                TankBlockItem.of(ModItems.TANK.get(), glassId, TankStyle.CLEAN, 12),
                TankBlockItem.of(ModItems.TANK.get(), glassId, TankStyle.TEXTURED, 4),
                TankBlockItem.of(ModItems.TANK.get(), ResourceLocation.withDefaultNamespace("light_blue_stained_glass"), TankStyle.TEXTURED, 1),
                new ItemStack(ModItems.ARTEMIA_CYSTS.get(), 16),
                new ItemStack(ModItems.ARTEMIA_CYSTS.get()),
                old,
                new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.LINED_SEAHORSE).get()),
                new ItemStack(ModItems.TANKMATE_BUCKETS.get(TankmateSpecies.DWARF_SEAHORSE).get()),
        };
        for (int i = 0; i < hotbar.length; i++) p.getInventory().setItem(i, hotbar[i]);
        p.getInventory().setItem(9, new ItemStack(ModItems.ARTEMIA_CYSTS.get(), 7));
        p.getInventory().setItem(10, new ItemStack(ModItems.ARTEMIA_NAUPLII.get(), 4));
        p.getInventory().setItem(11, TankBlockItem.of(ModItems.TANK.get(), glassId, TankStyle.CLEAN, 64));
        p.getInventory().selected = 8;
        p.teleportTo(p.serverLevel(), glass.getX() + 1.5, glass.getY() - 0.2, glass.getZ() - 3.0, 0F, 18F);
    }

    // ------------------------------------------------------------------ checks

    private void checkFree() {
        int seahorses = 0, hitched = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof TankmateEntity m && m.species().seahorse() && m.isAdult() && m.getY() < reef.getY() + 3 && Math.abs(m.getZ() - reef.getZ()) < 4
                    && m.getX() < reef.getX() + 6) {
                seahorses++;
                if (m.flag(TankmateEntity.FLAG_REST)) hitched++;
            }
        }
        LOGGER.info("[smoketest] M21.8 client: {} seahorses in the reef tank, {} hold on to a post", seahorses, hitched);
        if (seahorses < 6) LOGGER.error("[smoketest] M21.8 the client sees too few seahorses: {}", seahorses);
        if (hitched == 0) LOGGER.error("[smoketest] M21.8 no seahorse holds on to a post");
    }

    private void checkPouch() {
        int gravid = 0, fry = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof TankmateEntity m) || !m.species().seahorse() || e.distanceToSqr(Vec3.atCenterOf(pouch)) > 9) continue;
            if (m.isGravid()) gravid++;
            if (!m.isAdult()) fry++;
        }
        LOGGER.info("[smoketest] M21.8 client: {} full brood pouch(es), {} fry", gravid, fry);
        if (gravid != 1) LOGGER.error("[smoketest] M21.8 the brood pouch is not synced: {}", gravid);
        if (fry < 5) LOGGER.error("[smoketest] M21.8 too few seahorse fry on the client: {}", fry);
    }

    private void checkStack() {
        int client = new ItemStack(ModItems.ARTEMIA_CYSTS.get()).getMaxStackSize();
        LOGGER.info("[smoketest] M21.8 cyst jars per stack: client {}, server {}", client, serverStack);
        if (client != serverStack || client != 64) LOGGER.error("[smoketest] M21.8 cyst stack size differs: client {}, server {}", client, serverStack);
        if (mc.player != null) {
            ItemStack s = mc.player.getInventory().getItem(4);
            if (!s.is(ModItems.ARTEMIA_CYSTS.get()) || s.getCount() != 16) LOGGER.error("[smoketest] M21.8 the client has no stack of 16 jars: {}", s);
        }
    }

    // ------------------------------------------------------------------ helpers

    private void menu(int tab, String name, java.util.function.Consumer<org.overengineer.bettacraft.client.screen.TankScreen> page) {
        then(10, () -> openMenu(reef));
        then(30, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen ts) {
                ts.selectTabForTest(tab);
                if (page != null) page.accept(ts);
            } else {
                LOGGER.error("[smoketest] M21.8 no tank menu for {}", name);
            }
        });
        then(20, () -> shot(name));
        then(1, () -> language("de_de"));
        then(20, () -> shot(name + "_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
            if (c == null) return;
            org.overengineer.bettacraft.platform.Platform.get().openMenu(p, new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void guide(String l) {
        if (!GuideBook.available()) return;
        then(10, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("saltwater/seahorses.md")));
        then(40, () -> shot("guide_" + l));
        then(5, () -> {
            if (mc.screen == null) return;
            double x = mc.getWindow().getGuiScaledWidth() / 2.0, y = mc.getWindow().getGuiScaledHeight() / 2.0;
            for (int i = 0; i < 8; i++) mc.screen.mouseScrolled(x, y, 0, -3);
        });
        then(20, () -> shot("guide_" + l + "_2"));
        then(5, () -> mc.setScreen(null));
    }

    private void hover(java.util.function.Predicate<ItemStack> which) {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr)) return;
        for (var s : scr.getMenu().slots) {
            if (which.test(s.getItem())) {
                mouseTo(scr.getGuiLeft() + s.x + 8, scr.getGuiTop() + s.y + 8);
                return;
            }
        }
        LOGGER.warn("[smoketest] M21.8 item to hover not found");
    }

    private void mouseTo(int x, int y) {
        double scale = mc.getWindow().getGuiScale();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * scale, y * scale);
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

    private TankBlockEntity tank(ServerLevel level, ServerPlayer p, BlockPos min, int sx, int sy, int sz, float temp, float salt) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, (long) sx * sy * sz * 1000, salt);
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

    private static void rock(ServerLevel in, InteriorRegion r, int cx, int cz, int rad, int h) {
        for (int x = -rad; x <= rad; x++)
            for (int z = -rad; z <= rad; z++) {
                double d = Math.sqrt(x * x + z * z) / rad;
                if (d > 1.0) continue;
                int top = (int) Math.round(h * Math.max(0.2, 1 - d * d * 0.8));
                for (int y = 0; y < top; y++) in.setBlock(r.toWorld(cx + x, y, cz + z), ModBlocks.REEF_ROCK.get().defaultBlockState(), 2);
            }
    }

    private static void coral(ServerLevel in, InteriorRegion r, PlantSpecies s, int x, int z, long seed) {
        int y = 0;
        while (y < r.sizeY() && !in.getBlockState(r.toWorld(x, y, z)).isAir()) y++;
        InteriorPlant p = InteriorPlants.plantAt(in, r, s, x, y, z, s.stages(), seed);
        if (p == null) {
            LOGGER.error("[smoketest] M21.8 {} not placed", s.id());
            return;
        }
        p.tint = 2;
        InteriorPlants.apply(in, r, p);
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true)
                    .setValue(TankLightBlock.COLOR, x % 2 == 0 ? DyeColor.WHITE : DyeColor.LIGHT_BLUE));
        }
    }

    private static TankmateEntity fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, String variety, boolean female, LifeStage stage) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = level.getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.3, 0.1, 0.7);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, TankmateGenes.pure(s, variety), female, stage, 0, p);
        if (e == null) LOGGER.error("[smoketest] M21.8 {} not spawned", s.id());
        else e.health().feed();
        return e;
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch));
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_8_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
