package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetFront;
import org.overengineer.bettacraft.equipment.CabinetFrontSetting;
import org.overengineer.bettacraft.equipment.CabinetMenu;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M44: the fronts of cabinet rows. Rows of 1, 2, 4, 6, 8 and 12 get the layout of {@link CabinetFront#layout}; placing and
 * breaking in the middle or at an end changes the whole row; two rows meeting at a corner keep their own fronts; the menu
 * fixes the front and the joints of one cabinet and the dropped item keeps them; an old cabinet becomes Auto with joints;
 * with {@code features.equipment.cabinetFronts} off every cabinet shows the old two doors and the setting is kept.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CabinetFrontGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CabinetFrontGameTests() {}

    private static BlockState cabinet(Direction facing) {
        return ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, facing);
    }

    /** A row of {@code n} north facing cabinets at z, from x0 eastwards (the viewer in the north: his left is the east). */
    private static void row(GameTestHelper h, int x0, int z, int n) {
        for (int x = 0; x < n; x++) h.setBlock(new BlockPos(x0 + x, 1, z), cabinet(Direction.NORTH));
    }

    /** The fronts of the row from the viewer's left (east) to his right (west) as letters (T L R S D). */
    private static String fronts(GameTestHelper h, int x0, int z, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(letter(h.getBlockState(new BlockPos(x0 + n - 1 - i, 1, z)).getValue(CabinetBlock.FRONT)));
        return b.toString();
    }

    private static char letter(CabinetFront f) {
        return switch (f) {
            case TWO -> 'T';
            case PAIR_L -> 'L';
            case PAIR_R -> 'R';
            case SINGLE -> 'S';
            case DRAWERS -> 'D';
        };
    }

    private static String expected(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(letter(CabinetFront.layout(n, i)));
        return b.toString();
    }

    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts", timeoutTicks = 100)
    public static void rowsGetTheirFronts(GameTestHelper h) {
        int[] lengths = {1, 2, 4, 6, 8, 12};
        int z = 2;
        for (int n : lengths) {
            row(h, 2, z, n);
            z += 2;
        }
        // placed from the west end towards the east the other way round as well (the row does not care)
        for (int x = 11; x >= 0; x--) h.setBlock(new BlockPos(16 + x, 1, 2), cabinet(Direction.NORTH));
        h.runAfterDelay(2, () -> {
            int zz = 2;
            for (int n : lengths) {
                String got = fronts(h, 2, zz, n);
                h.assertTrue(got.equals(expected(n)), n + " blocks: " + got + " instead of " + expected(n));
                for (int x = 0; x < n; x++) {
                    BlockState s = h.getBlockState(new BlockPos(2 + x, 1, zz));
                    h.assertTrue(s.getValue(CabinetBlock.JOINTS), "new cabinets have joints");
                }
                zz += 2;
            }
            h.assertTrue("DLRLRLRLRLRD".equals(fronts(h, 16, 2, 12)), "built backwards: " + fronts(h, 16, 2, 12));
            h.assertTrue(expected(6).equals("DLRLRD") && expected(12).equals("DLRLRLRLRLRD"), "the layout E");
            h.succeed();
        });
    }

    /** Placing and breaking in the middle and at an end changes the fronts of the whole row (also of blocks far away). */
    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts", timeoutTicks = 100)
    public static void placingAndBreakingUpdatesTheRow(GameTestHelper h) {
        row(h, 2, 4, 4);
        h.assertTrue("DLRD".equals(fronts(h, 2, 4, 4)), "4: " + fronts(h, 2, 4, 4));
        // one more at the west end: 5 blocks, the middle of the row changes too (not only the neighbour)
        h.setBlock(new BlockPos(1, 1, 4), cabinet(Direction.NORTH));
        h.assertTrue(expected(5).equals(fronts(h, 1, 4, 5)), "5: " + fronts(h, 1, 4, 5));
        // and at the east end: 6
        h.setBlock(new BlockPos(6, 1, 4), cabinet(Direction.NORTH));
        h.assertTrue("DLRLRD".equals(fronts(h, 1, 4, 6)), "6: " + fronts(h, 1, 4, 6));
        // broken in the middle: two rows of 2 and 3
        h.setBlock(new BlockPos(3, 1, 4), Blocks.AIR.defaultBlockState());
        h.assertTrue("LR".equals(fronts(h, 1, 4, 2)) && expected(3).equals(fronts(h, 4, 4, 3)), "split: " + fronts(h, 1, 4, 2) + " | " + fronts(h, 4, 4, 3));
        // the end broken: 2 left of the gap stay, the 3 become 2
        h.setBlock(new BlockPos(6, 1, 4), Blocks.AIR.defaultBlockState());
        h.assertTrue("LR".equals(fronts(h, 4, 4, 2)), "end broken: " + fronts(h, 4, 4, 2));
        // the gap filled again: 5
        h.setBlock(new BlockPos(3, 1, 4), cabinet(Direction.NORTH));
        h.assertTrue(expected(5).equals(fronts(h, 1, 4, 5)), "gap filled: " + fronts(h, 1, 4, 5));
        // a turned cabinet leaves the row
        h.setBlock(new BlockPos(3, 1, 4), cabinet(Direction.SOUTH));
        h.assertTrue("LR".equals(fronts(h, 1, 4, 2)) && "LR".equals(fronts(h, 4, 4, 2)), "turned: " + fronts(h, 1, 4, 2) + " | " + fronts(h, 4, 4, 2));
        h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)).getValue(CabinetBlock.FRONT) == CabinetFront.TWO, "the turned one alone: two doors");
        // a long row of 32: the layout; 33: double doors by the position, never a half one inside
        row(h, 0, 8, 32);
        h.assertTrue(expected(32).equals(fronts(h, 0, 8, 32)), "32: " + fronts(h, 0, 8, 32));
        h.succeed();
    }

    /** Two rows meeting at a corner (one facing north, one facing west) keep their own fronts. */
    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts", timeoutTicks = 100)
    public static void cornerOfTwoRows(GameTestHelper h) {
        // the north facing row x 2..5 at z 2; the west facing row at x 6, z 2..5 (its viewer stands in the west: his left is the north)
        row(h, 2, 2, 4);
        for (int z = 2; z <= 5; z++) h.setBlock(new BlockPos(6, 1, z), cabinet(Direction.WEST));
        h.assertTrue("DLRD".equals(fronts(h, 2, 2, 4)), "north row: " + fronts(h, 2, 2, 4));
        StringBuilder west = new StringBuilder();
        for (int z = 2; z <= 5; z++) west.append(letter(h.getBlockState(new BlockPos(6, 1, z)).getValue(CabinetBlock.FRONT)));
        h.assertTrue("DLRD".contentEquals(west), "west row (from the viewer's left, north): " + west);
        // the corner blocks are not joined: both keep their counter-top ends
        h.assertTrue(!h.getBlockState(new BlockPos(5, 1, 2)).getValue(CabinetBlock.LEFT) && !h.getBlockState(new BlockPos(6, 1, 2)).getValue(CabinetBlock.LEFT),
                "the corner is two row ends");
        h.succeed();
    }

    /** The menu fixes the front and the joints of one cabinet; NBT, the update tag and the dropped item keep them. */
    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts_menu", timeoutTicks = 100)
    public static void menuOverridesAutoAndItemKeepsIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        row(h, 2, 3, 4);
        BlockPos mid = new BlockPos(4, 1, 3); // the second from the east (the viewer's left): the left leaf of the double door
        CabinetBlockEntity cab = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(mid));
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        try {
            sp.teleportTo(h.absolutePos(mid).getX() + 0.5, h.absolutePos(mid).getY() + 1, h.absolutePos(mid).getZ() - 1.5);
            CabinetMenu menu = (CabinetMenu) cab.createMenu(1, sp.getInventory(), sp);
            h.assertTrue(menu.frontSetting() == CabinetFrontSetting.AUTO && menu.joints(), "menu: auto, joints");
            h.assertTrue(menu.clickMenuButton(sp, CabinetMenu.BUTTON_FRONT0 + CabinetFrontSetting.DRAWERS.ordinal()), "button drawers");
            h.assertTrue(h.getBlockState(mid).getValue(CabinetBlock.FRONT) == CabinetFront.DRAWERS, "this block: drawers");
            h.assertTrue("DDRD".equals(fronts(h, 2, 3, 4)), "the others stay auto: " + fronts(h, 2, 3, 4));
            h.assertTrue(menu.clickMenuButton(sp, CabinetMenu.BUTTON_JOINTS) && !cab.joints() && !menu.joints(), "joints off");
            h.assertTrue(!h.getBlockState(mid).getValue(CabinetBlock.JOINTS), "the state without joints");
            h.assertTrue(menu.clickMenuButton(sp, CabinetMenu.BUTTON_FRONT0 + CabinetFrontSetting.SINGLE.ordinal()) && cab.frontSetting() == CabinetFrontSetting.SINGLE, "single");
            h.assertTrue(h.getBlockState(mid).getValue(CabinetBlock.FRONT) == CabinetFront.SINGLE, "this block: single door");
            CompoundTag upd = cab.getUpdateTag(level.registryAccess());
            h.assertTrue("single".equals(upd.getString("front")) && !upd.getBoolean("joints"), "update tag: " + upd);
            CompoundTag saved = cab.saveWithFullMetadata(level.registryAccess());
            CabinetBlockEntity copy = new CabinetBlockEntity(h.absolutePos(mid), h.getBlockState(mid));
            copy.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(copy.frontSetting() == CabinetFrontSetting.SINGLE && !copy.joints(), "saved and loaded");
            // an old cabinet (no front, no joints in its data): auto with joints
            saved.remove("front");
            saved.remove("joints");
            CabinetBlockEntity old = new CabinetBlockEntity(h.absolutePos(mid), h.getBlockState(mid));
            old.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(old.frontSetting() == CabinetFrontSetting.AUTO && old.joints(), "old cabinet: auto, joints");
            // broken: the drop keeps the setting, placed again (alone) the block shows it
            List<ItemStack> drops = Block.getDrops(h.getBlockState(mid), level, h.absolutePos(mid), cab);
            ItemStack item = drops.stream().filter(s -> s.is(ModItems.CABINET.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue("single".equals(item.get(ModDataComponents.CABINET_FRONT.get())) && Boolean.FALSE.equals(item.get(ModDataComponents.CABINET_JOINTS.get())),
                    "the dropped cabinet keeps single, no joints: " + drops);
            ItemStack picked = ((CabinetBlock) ModBlocks.CABINET.get()).getCloneItemStack(h.getBlockState(mid), null, level, h.absolutePos(mid), sp);
            h.assertTrue(CabinetFrontSetting.byId(picked.get(ModDataComponents.CABINET_FRONT.get())) == CabinetFrontSetting.SINGLE, "pick block keeps it");
            BlockPos lone = new BlockPos(10, 1, 10);
            h.setBlock(lone, cabinet(Direction.NORTH));
            CabinetBlockEntity placed = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(lone));
            placed.applyComponentsFromItemStack(item);
            CabinetBlock.refresh(level, h.absolutePos(lone));
            h.assertTrue(placed.frontSetting() == CabinetFrontSetting.SINGLE && h.getBlockState(lone).getValue(CabinetBlock.FRONT) == CabinetFront.SINGLE
                    && !h.getBlockState(lone).getValue(CabinetBlock.JOINTS), "placed again: single door, no joints");
            // an auto cabinet with joints drops a plain item (stacks with new ones)
            BlockPos end = new BlockPos(2, 1, 3);
            List<ItemStack> plain = Block.getDrops(h.getBlockState(end), level, h.absolutePos(end), level.getBlockEntity(h.absolutePos(end)));
            h.assertTrue(plain.stream().filter(s -> s.is(ModItems.CABINET.get())).allMatch(s -> s.get(ModDataComponents.CABINET_FRONT.get()) == null
                    && s.get(ModDataComponents.CABINET_JOINTS.get()) == null), "auto: no components " + plain);
            // back to auto: the double door again
            menu.clickMenuButton(sp, CabinetMenu.BUTTON_FRONT0 + CabinetFrontSetting.AUTO.ordinal());
            menu.clickMenuButton(sp, CabinetMenu.BUTTON_JOINTS);
            h.assertTrue("DLRD".equals(fronts(h, 2, 3, 4)) && h.getBlockState(mid).getValue(CabinetBlock.JOINTS), "auto again: " + fronts(h, 2, 3, 4));
        } finally {
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }

    /** The switch off: the old two doors without joints everywhere, the menu hides the button, the setting is kept. */
    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts_off", timeoutTicks = 100)
    public static void cabinetFrontsSwitchedOff(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        row(h, 2, 3, 6);
        BlockPos at = new BlockPos(3, 1, 3);
        CabinetBlockEntity cab = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(at));
        cab.setFrontSetting(CabinetFrontSetting.SINGLE);
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        boolean feature = Features.own(Feature.CABINET_FRONTS);
        try {
            Features.set(Feature.CABINET_FRONTS, false);
            for (int x = 0; x < 6; x++) CabinetBlock.refresh(level, h.absolutePos(new BlockPos(2 + x, 1, 3)));
            h.assertTrue("TTTTTT".equals(fronts(h, 2, 3, 6)), "off: two doors everywhere: " + fronts(h, 2, 3, 6));
            for (int x = 0; x < 6; x++) h.assertTrue(!h.getBlockState(new BlockPos(2 + x, 1, 3)).getValue(CabinetBlock.JOINTS), "off: no joints");
            // new cabinets as well
            h.setBlock(new BlockPos(8, 1, 3), cabinet(Direction.NORTH));
            h.assertTrue("TTTTTTT".equals(fronts(h, 2, 3, 7)), "off: a new one: " + fronts(h, 2, 3, 7));
            CabinetMenu menu = (CabinetMenu) cab.createMenu(1, sp.getInventory(), sp);
            h.assertTrue(menu.frontSetting() == null && !menu.joints(), "off: the menu hides the front");
            h.assertTrue(!menu.clickMenuButton(sp, CabinetMenu.BUTTON_FRONT0 + CabinetFrontSetting.DRAWERS.ordinal())
                    && !menu.clickMenuButton(sp, CabinetMenu.BUTTON_JOINTS), "off: the buttons do nothing");
            h.assertTrue(cab.frontSetting() == CabinetFrontSetting.SINGLE && cab.joints(), "off: the setting is kept");
        } finally {
            Features.set(Feature.CABINET_FRONTS, feature);
            sp.getServer().getPlayerList().remove(sp);
        }
        for (int x = 0; x < 7; x++) CabinetBlock.refresh(level, h.absolutePos(new BlockPos(2 + x, 1, 3)));
        h.assertTrue(h.getBlockState(at).getValue(CabinetBlock.FRONT) == CabinetFront.SINGLE, "on again: the single door");
        h.assertTrue(fronts(h, 2, 3, 7).equals(expected(7).substring(0, 5) + "S" + expected(7).substring(6)), "on again: " + fronts(h, 2, 3, 7));
        h.succeed();
    }

    /** A cabinet loaded with the default state (as saved before M44) gets its front a tick after its block entity loads. */
    @GameTest(template = EMPTY, batch = "bettacraft_cabinet_fronts", timeoutTicks = 100)
    public static void oldCabinetsGetTheirFrontWhenLoaded(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        row(h, 2, 6, 4);
        // the state of an old save: the default front, no joints (set without updates, like loading a chunk)
        for (int x = 0; x < 4; x++) {
            BlockPos p = h.absolutePos(new BlockPos(2 + x, 1, 6));
            level.setBlock(p, level.getBlockState(p).setValue(CabinetBlock.FRONT, CabinetFront.TWO).setValue(CabinetBlock.JOINTS, false),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        h.assertTrue("TTTT".equals(fronts(h, 2, 6, 4)), "the old state: " + fronts(h, 2, 6, 4));
        for (int x = 0; x < 4; x++) {
            BlockPos p = h.absolutePos(new BlockPos(2 + x, 1, 6));
            ((CabinetBlockEntity) level.getBlockEntity(p)).onLoad();
        }
        h.succeedWhen(() -> h.assertTrue("DLRD".equals(fronts(h, 2, 6, 4)) && h.getBlockState(new BlockPos(2, 1, 6)).getValue(CabinetBlock.JOINTS),
                "loaded: " + fronts(h, 2, 6, 4)));
    }
}
