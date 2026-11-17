package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetMenu;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.OutletSide;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCurrents;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.Seahorses;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.4: the outlet side of the filter in a cabinet. The menu button sets back / left / right / front; the hose goes up
 * that wall, the jet and the current come from it (floating plant jets, {@link TankCurrents}); a seahorse in front of the
 * outlet feels the stronger flow, one on the other side does not; the setting survives saving, breaking and placing
 * (item component), old cabinets keep the back wall, and with {@code features.equipment.cabinetOutletSide} off every cabinet uses
 * the back wall.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CabinetOutletGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CabinetOutletGameTests() {}

    /** A 4x2x2 salt water tank (16 water blocks) on a north facing cabinet with a filter under its block {@code at} (0..3). */
    private static CabinetBlockEntity cabinetUnder(GameTestHelper h, TankBlockEntity[] tankOut, int x, int z, int at) {
        BlockPos cp = new BlockPos(x + at, 1, z);
        h.setBlock(cp, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
        TankBlockEntity c = SeahorseGameTests.tank(h, x, 2, z, 4, 2, 2, 16000, 33F, 24F);
        CabinetBlockEntity cab = (CabinetBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(cp));
        cab.items().setStackInSlot(CabinetBlockEntity.FILTER_SLOT, new ItemStack(ModItems.EXTERNAL_FILTER.get()));
        TankEquipment.invalidate(c);
        tankOut[0] = c;
        return cab;
    }

    /** The current factor at the inset corners of the tank: the smallest one (far from the outlet). */
    private static double calmest(List<TankCurrents.Current> cs, TankData t) {
        double best = Double.MAX_VALUE;
        for (double fx : new double[]{0.3, t.sizeX() - 0.3}) {
            for (double fz : new double[]{0.3, t.sizeZ() - 0.3}) best = Math.min(best, TankCurrents.factor(cs, t.min().getX() + fx, t.min().getZ() + fz));
        }
        return best;
    }

    @GameTest(template = EMPTY, batch = "bettacraft_outlet", timeoutTicks = 200)
    public static void outletSideMovesHoseJetAndCurrent(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity[] tk = new TankBlockEntity[1];
        CabinetBlockEntity cab = cabinetUnder(h, tk, 4, 4, 1);
        TankBlockEntity c = tk[0];
        TankData t = c.getTank();
        h.assertTrue(TankEquipment.filters(level, c).size() == 1, "the cabinet filter belongs to the tank");
        h.assertTrue(cab.outletSide() == OutletSide.BACK, "a new cabinet: back");
        // facing north (the door to the north): back = south wall, left = east, right = west, front = north
        Direction[] walls = {Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH};
        for (OutletSide side : OutletSide.values()) {
            cab.setOutletSide(side);
            Direction wall = walls[side.ordinal()];
            h.assertTrue(side.wall(Direction.NORTH) == wall, side + ": wall " + side.wall(Direction.NORTH));
            FilterFlow.Path p = FilterFlow.forCabinet(t, cab);
            h.assertTrue(p != null && p.inward() == wall.getOpposite(), side + ": the jet points into the tank from the " + wall + " wall: " + (p == null ? null : p.inward()));
            // the hose stands outside that wall
            double out = wall.getAxis() == Direction.Axis.Z ? p.bottom().z : p.bottom().x;
            double edge = switch (wall) {
                case SOUTH -> t.max().getZ() + 1;
                case NORTH -> t.min().getZ();
                case EAST -> t.max().getX() + 1;
                default -> t.min().getX();
            };
            h.assertTrue(Math.abs(out - edge) < 0.2, side + ": the hose at the " + wall + " glass (" + out + " vs " + edge + ")");
            // floating plants drift with the jet of that side
            var jets = TankPlants.jets(level, c);
            h.assertTrue(jets.size() == 1 && Math.round(jets.getFirst().dx()) == wall.getOpposite().getStepX()
                    && Math.round(jets.getFirst().dz()) == wall.getOpposite().getStepZ(), side + ": floating plant jet " + jets);
            // the current: strong in front of the outlet, the far corner calm
            List<TankCurrents.Current> cs = TankCurrents.of(level, c);
            h.assertTrue(cs.size() == 1, side + ": one current");
            TankCurrents.Current cur = cs.getFirst();
            double near = TankCurrents.factor(cs, cur.x() + cur.dx() * 0.5, cur.z() + cur.dz() * 0.5);
            h.assertTrue(near > 1.4, side + ": strong in front of the outlet (a submerged outlet: 0.6 of a jet) " + near);
            h.assertTrue(Math.abs(calmest(cs, t) - 1.0) < 1e-9, side + ": a calm corner " + calmest(cs, t));
        }
        h.succeed();
    }

    /** A seahorse in front of the outlet is stressed by the flow; turned to the other side, the outlet leaves it alone. */
    @GameTest(template = EMPTY, batch = "bettacraft_outlet", timeoutTicks = 200)
    public static void seahorseFeelsTheOutletSide(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity[] tk = new TankBlockEntity[1];
        CabinetBlockEntity cab = cabinetUnder(h, tk, 4, 10, 3); // the cabinet under the east end: a back outlet stays clear of the west end
        TankBlockEntity c = tk[0];
        TankmateEntity sh = SeahorseGameTests.add(h, c, TankmateSpecies.LINED_SEAHORSE, false, LifeStage.ADULT);
        h.runAfterDelay(5, () -> {
            Seahorses.forget();
            double base = Seahorses.flow(level, c);
            h.assertTrue(base > 0 && !Seahorses.flowTooStrong(base), "one filter in 16 blocks: gentle on average " + base);
            // outlet on the right (west wall): the seahorse right in front of it
            cab.setOutletSide(OutletSide.RIGHT);
            Seahorses.forget();
            TankCurrents.Current cur = TankCurrents.of(level, c).getFirst();
            Vec3 front = new Vec3(cur.x() + cur.dx() * 0.6, sh.getY(), cur.z() + cur.dz() * 0.6);
            sh.setPos(front);
            double there = Seahorses.flowAt(level, c, front.x, front.z);
            h.assertTrue(Seahorses.flowTooStrong(there), "in front of the outlet too strong: " + there);
            TankmateCare.advance(level, c, sh, 0.05);
            h.assertTrue((sh.stressBits() & TankmateCare.STRESS_FLOW) != 0, "stressed in front of the outlet: " + sh.stressBits());
            // the outlet to the back wall at the east end: the same spot is calm now
            cab.setOutletSide(OutletSide.BACK);
            Seahorses.forget();
            sh.setPos(front);
            double after = Seahorses.flowAt(level, c, front.x, front.z);
            h.assertTrue(!Seahorses.flowTooStrong(after), "outlet on the other side: calm " + after);
            TankmateCare.advance(level, c, sh, 0.05);
            h.assertTrue((sh.stressBits() & TankmateCare.STRESS_FLOW) == 0, "no flow stress any more: " + sh.stressBits());
            // switched off: even flow like before, also in front of the outlet
            boolean feature = Features.own(Feature.CABINET_OUTLET_SIDE);
            try {
                Features.set(Feature.CABINET_OUTLET_SIDE, false);
                Seahorses.forget();
                h.assertTrue(TankCurrents.of(level, c).isEmpty(), "off: no directed currents");
                h.assertTrue(Math.abs(Seahorses.flowAt(level, c, front.x, front.z) - base) < 1e-9, "off: the average flow everywhere");
                cab.setOutletSide(OutletSide.RIGHT);
                h.assertTrue(cab.outletSide() == OutletSide.BACK && cab.outletSetting() == OutletSide.RIGHT, "off: the back wall, the setting kept");
                FilterFlow.Path p = FilterFlow.forCabinet(c.getTank(), cab);
                h.assertTrue(p != null && p.inward() == Direction.NORTH, "off: the jet from the back wall " + (p == null ? null : p.inward()));
            } finally {
                Features.set(Feature.CABINET_OUTLET_SIDE, feature);
                Seahorses.forget();
            }
            h.succeed();
        });
    }

    /** The menu buttons set the side (and are refused with the feature off); NBT, the update tag, breaking and placing keep it. */
    @GameTest(template = EMPTY, batch = "bettacraft_outlet_keep", timeoutTicks = 200)
    public static void outletSideIsKept(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos cp = new BlockPos(3, 1, 3);
        BlockState state = ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.EAST);
        h.setBlock(cp, state);
        CabinetBlockEntity cab = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(cp));
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        boolean feature = Features.own(Feature.CABINET_OUTLET_SIDE);
        try {
            sp.teleportTo(h.absolutePos(cp).getX() + 0.5, h.absolutePos(cp).getY() + 1, h.absolutePos(cp).getZ() - 1.5);
            CabinetMenu menu = (CabinetMenu) cab.createMenu(1, sp.getInventory(), sp);
            h.assertTrue(menu.outletSide() == OutletSide.BACK, "menu shows back");
            h.assertTrue(menu.clickMenuButton(sp, CabinetMenu.BUTTON_OUTLET0 + OutletSide.FRONT.ordinal()), "button front");
            h.assertTrue(cab.outletSide() == OutletSide.FRONT && menu.outletSide() == OutletSide.FRONT, "front set");
            h.assertTrue(!menu.clickMenuButton(sp, 9), "unknown button refused");
            Features.set(Feature.CABINET_OUTLET_SIDE, false);
            h.assertTrue(menu.outletSide() == null, "off: the menu hides the setting");
            h.assertTrue(!menu.clickMenuButton(sp, CabinetMenu.BUTTON_OUTLET0 + OutletSide.LEFT.ordinal()) && cab.outletSetting() == OutletSide.FRONT,
                    "off: the button does nothing");
            Features.set(Feature.CABINET_OUTLET_SIDE, feature);
            menu.clickMenuButton(sp, CabinetMenu.BUTTON_OUTLET0 + OutletSide.LEFT.ordinal());
            h.assertTrue(cab.outletSide() == OutletSide.LEFT, "left set");
            // the update tag (network) carries the side
            CompoundTag upd = cab.getUpdateTag(level.registryAccess());
            h.assertTrue("left".equals(upd.getString("outlet")), "update tag: " + upd);
            // saved and loaded
            CompoundTag saved = cab.saveWithFullMetadata(level.registryAccess());
            CabinetBlockEntity copy = new CabinetBlockEntity(h.absolutePos(cp), state);
            copy.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(copy.outletSetting() == OutletSide.LEFT, "saved and loaded: " + copy.outletSetting());
            // an old cabinet (no "outlet" in its data) keeps the back wall
            saved.remove("outlet");
            CabinetBlockEntity old = new CabinetBlockEntity(h.absolutePos(cp), state);
            old.loadWithComponents(saved, level.registryAccess());
            h.assertTrue(old.outletSetting() == OutletSide.BACK, "old cabinet: back");
            // broken: the drop remembers the side, placed again it is back
            List<ItemStack> drops = Block.getDrops(state, level, h.absolutePos(cp), cab);
            ItemStack item = drops.stream().filter(s -> s.is(ModItems.CABINET.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(!item.isEmpty() && "left".equals(item.get(ModDataComponents.OUTLET_SIDE.get())), "the dropped cabinet keeps left: " + drops);
            BlockPos cp2 = new BlockPos(6, 1, 3);
            h.setBlock(cp2, state);
            CabinetBlockEntity placed = (CabinetBlockEntity) level.getBlockEntity(h.absolutePos(cp2));
            placed.applyComponentsFromItemStack(item);
            h.assertTrue(placed.outletSetting() == OutletSide.LEFT, "placed again: left");
            // a cabinet on the back wall drops a plain item (stacks with new ones)
            CabinetBlockEntity fresh = new CabinetBlockEntity(h.absolutePos(cp), state);
            List<ItemStack> backDrops = Block.getDrops(state, level, h.absolutePos(cp), fresh);
            h.assertTrue(backDrops.stream().filter(s -> s.is(ModItems.CABINET.get())).allMatch(s -> s.get(ModDataComponents.OUTLET_SIDE.get()) == null),
                    "back: no component " + backDrops);
        } finally {
            Features.set(Feature.CABINET_OUTLET_SIDE, feature);
            sp.getServer().getPlayerList().remove(sp);
        }
        h.succeed();
    }
}
