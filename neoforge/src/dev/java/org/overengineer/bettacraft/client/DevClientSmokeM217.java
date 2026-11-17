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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockItem;
import org.overengineer.bettacraft.equipment.CabinetStones;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.7 dev client smoke scene ({@code -Psmoketestm21_7}), screenshots {@code bettacraft_m21_7_*}:
 * <ul>
 *     <li>a row of cabinets of every vanilla stone with slabs and of the stones of other mods in the dev runtime, each with
 *     a small tank on it: near (in parts), close up (the edges: no colours of neighbouring atlas textures) and far;</li>
 *     <li>joined counters of three cabinets (sandstone, deepslate bricks, quartz) under long tanks;</li>
 *     <li>the stone cabinets as items in the hotbar and inventory, the JEI recipe (twice, it cycles) and the handbook page
 *     (en, de).</li>
 * </ul>
 */
final class DevClientSmokeM217 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final String lang = mc.options.languageCode;
    private BlockPos base, counters;
    private volatile int stones = 40;
    private final List<ResourceLocation> hotbar = new ArrayList<>();
    private int wait;

    DevClientSmokeM217() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // the row near, in parts of four cabinets (two blocks apart)
        for (int part = 0; part < 12; part++) {
            int n = part;
            then(1, () -> {
                if (n * 4 < stones) look(base.getX() + n * 8 + 3.5, base.getY() + 1.7, base.getZ() - 4.6, 0F, 12F);
            });
            then(n == 0 ? 120 : 40, () -> {
                if (n * 4 < stones) shot("row_" + n);
            });
        }
        // close up: the edges of the first cabinets (stone, cobblestone) and the modded ones at the end
        then(1, () -> look(base.getX() + 1.5, base.getY() + 1.0, base.getZ() - 1.6, 0F, 25F));
        then(40, () -> shot("close_vanilla"));
        then(1, () -> look(base.getX() + (stones - 2) * 2 + 0.5, base.getY() + 1.0, base.getZ() - 1.6, 0F, 25F));
        then(40, () -> shot("close_modded"));
        then(1, () -> look(base.getX() + 15.5, base.getY() + 1.4, base.getZ() - 2.0, -30F, 22F));
        then(40, () -> shot("close_side"));
        // far: the whole row
        then(1, () -> look(base.getX() + stones + 0.5, base.getY() + 10.0, base.getZ() - 34.0, 0F, 12F));
        then(80, () -> shot("far"));
        then(1, () -> look(base.getX() + 20.5, base.getY() + 4.0, base.getZ() - 14.0, 0F, 10F));
        then(40, () -> shot("mid"));
        // joined counters under long tanks
        then(1, () -> look(counters.getX() + 6.0, counters.getY() + 1.8, counters.getZ() - 5.5, 0F, 12F));
        then(60, () -> shot("counters"));
        // items: hotbar and inventory
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            for (int i = 0; i < 9 && i < hotbar.size(); i++) p.getInventory().setItem(i, CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), hotbar.get(i)));
            for (int i = 9; i < 36; i++) {
                int k = i % hotbar.size();
                p.getInventory().setItem(i, CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), hotbar.get(k)));
            }
            p.getInventory().selected = 4;
            p.teleportTo(p.serverLevel(), counters.getX() + 6.0, counters.getY(), counters.getZ() - 5.5, 0F, 12F);
        }));
        then(1, () -> {
            if (mc.player != null) mc.player.getInventory().selected = 4;
        });
        then(60, () -> mc.getToasts().clear());
        then(5, () -> shot("hotbar"));
        // the menu of a stone cabinet (its name in the title) with stone cabinet items in the storage
        then(1, () -> server(p -> {
            BlockPos at = base.offset(8, 0, 0);
            if (p.serverLevel().getBlockEntity(at) instanceof CabinetBlockEntity cab) {
                for (int i = 0; i < 9 && i < hotbar.size(); i++) {
                    cab.storage().setStackInSlot(i, CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), hotbar.get(i)));
                }
                p.openMenu(cab, at);
            }
        }));
        then(20, () -> hoverMenu());
        then(10, () -> shot("menu"));
        then(1, () -> language("de_de"));
        then(10, () -> hoverMenu());
        then(10, () -> shot("menu_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        // JEI
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei")) {
            then(5, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
            then(10, () -> {
                boolean ok = org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.showStoneCabinet();
                LOGGER.info("[smoketest] M21.7 JEI stone cabinet recipe shown: {}", ok);
                if (!ok) LOGGER.error("[smoketest] M21.7 no stone cabinet recipe in JEI");
            });
            then(20, () -> shot("jei_1"));
            then(30, () -> shot("jei_2"));
            then(5, () -> mc.setScreen(null));
        }
        // the handbook page (en, de)
        then(1, () -> mc.options.hideGui = true);
        guide("en");
        then(1, () -> language("de_de"));
        guide("de");
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
    }

    private void guide(String l) {
        if (!GuideBook.available()) return;
        then(10, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("equipment/cabinet.md")));
        then(40, () -> shot("guide_" + l));
        then(5, () -> scrollGuide());
        then(20, () -> shot("guide_" + l + "_2"));
        then(5, () -> mc.setScreen(null));
    }

    private void scrollGuide() {
        if (mc.screen == null) return;
        double x = mc.getWindow().getGuiScaledWidth() / 2.0, y = mc.getWindow().getGuiScaledHeight() / 2.0;
        for (int i = 0; i < 6; i++) mc.screen.mouseScrolled(x, y, 0, -3);
    }

    /** Mouse over the fifth storage slot of the open cabinet menu (tooltip with the name of the stone). */
    private void hoverMenu() {
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> scr) || scr.getMenu().slots.size() < 6) return;
        double scale = mc.getWindow().getGuiScale();
        var slot = scr.getMenu().slots.stream().filter(sl -> sl.getItem().is(ModItems.CABINET.get())).skip(4).findFirst().orElse(null);
        if (slot == null) return;
        int x = scr.getGuiLeft() + slot.x + 8, y = scr.getGuiTop() + slot.y + 8;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * scale, y * scale);
        try {
            for (String f : new String[] {"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x * scale : y * scale);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] M21.7 mouse: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------ building

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        base = p.blockPosition().offset(-30, 3, 440);
        List<ResourceLocation> ids = new ArrayList<>();
        for (CabinetStones.Pair s : CabinetStones.VANILLA) ids.add(block(s.stone()));
        List<CabinetStones.Pair> all = CabinetStones.all(level.getRecipeManager(), level.registryAccess());
        int modded = 0;
        for (CabinetStones.Pair s : all) {
            if (!s.stoneId().getNamespace().equals("minecraft") && modded < 8) {
                ids.add(block(s.stone()));
                modded++;
            }
        }
        LOGGER.info("[smoketest] M21.7 cabinet stones: {} known to the recipes ({} of other mods in the row): {}", all.size(), modded, ids);
        if (modded == 0) LOGGER.warn("[smoketest] M21.7 no stone of another mod in this runtime");
        stones = ids.size();
        for (int i = 0; i < ids.size(); i++) {
            BlockPos at = base.offset(i * 2, 0, 0);
            cabinet(level, at, ids.get(i));
            tank(level, at.above(), 1, p);
        }
        // the counters: three joined cabinets each
        counters = base.offset(0, 0, 10);
        ResourceLocation[] joined = {block(Items.SANDSTONE), block(Items.DEEPSLATE_BRICKS), block(Items.QUARTZ_BLOCK)};
        for (int k = 0; k < joined.length; k++) {
            BlockPos at = counters.offset(k * 4, 0, 0);
            for (int x = 0; x < 3; x++) cabinet(level, at.offset(x, 0, 0), joined[k]);
            for (int x = 0; x < 3; x++) level.setBlockAndUpdate(at.offset(x, 0, 0), Block.updateFromNeighbourShapes(level.getBlockState(at.offset(x, 0, 0)), level, at.offset(x, 0, 0)));
            tank(level, at.above(), 3, p);
        }
        // the hotbar: a few vanilla stones and the modded ones
        for (var it : List.of(Items.STONE_BRICKS, Items.COBBLED_DEEPSLATE, Items.SANDSTONE, Items.BRICKS, Items.QUARTZ_BLOCK, Items.TUFF_BRICKS, Items.NETHER_BRICKS)) {
            hotbar.add(block(it));
        }
        for (CabinetStones.Pair s : all) if (!s.stoneId().getNamespace().equals("minecraft") && hotbar.size() < 9) hotbar.add(block(s.stone()));
        while (hotbar.size() < 9) hotbar.add(block(Items.POLISHED_BLACKSTONE_BRICKS));
    }

    private static ResourceLocation block(net.minecraft.world.item.Item item) {
        return BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock());
    }

    private static void cabinet(ServerLevel level, BlockPos at, ResourceLocation stone) {
        level.setBlockAndUpdate(at, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(at) instanceof CabinetBlockEntity cab) {
            cab.setWood(stone);
            level.sendBlockUpdated(at, level.getBlockState(at), level.getBlockState(at), Block.UPDATE_CLIENTS);
        }
    }

    private static void tank(ServerLevel level, BlockPos min, int sx, ServerPlayer owner) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = 0; x < sx; x++) blocks.add(min.offset(x, 0, 0));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) return;
        TankInteractions.changeWater(level, c, blocks.size() * 1000L);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(owner.getUUID());
        c.markTankChanged();
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
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_7_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
