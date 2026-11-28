package org.overengineer.bettacraft.command;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.filter.FilterChamberBlockEntity;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.FilterPipeBlock;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M68 upgrade test, OLD side for the reference world of the M67 line (1.56.0-beta.1, the last version with the M40 pipe system).
 * NOT part of the mod: {@code tools/upgrade/build_reference.sh <commit> <world> DevUpgradeReferenceM67} copies it into a git
 * worktree of that commit and hooks it into its {@code DevShowcase} (whose row "Rohre, Filter und Wasserwechsel" already holds the M40
 * station: one multiblock filter for two tanks through old filter pipes, a four-line bundle with covers, T-pieces, the fresh and
 * waste water ports with their lines to reservoirs). After the showcase rows it builds an extra row with what the showcase lacks:
 * <ul>
 *   <li>a filter whose intake and return lines end in one old bundle on one tank face: ONE shared interior dock with both guards
 *   (the intake guard in front of it, the outlet guard beside it);</li>
 *   <li>the same with interior pipes from the shared dock down to a nozzle (the return's outlet) and the intake guard from outside;</li>
 *   <li>a water line of stainless steel and glass pipes between two reservoirs;</li>
 *   <li>a chest, a barrel and a shulker box with every old pipe item (each filter pipe, both ports, the T-piece, the interior pipe),
 *   one shulker box item with old pipe items inside the chest.</li>
 * </ul>
 * After the world is saved it writes {@code bettacraft_reference.json} (the tanks and chests in the format of
 * {@code DevSmokeUpgrade}, plus a {@code pipes} section: every old pipe block of the world with its connections, every multiblock
 * filter with its tanks and their status after a run, every shared dock with its guards, the item counts of every container above).
 * The new version's {@code DevSmokeUpgrade} checks the converted world against it.
 */
public final class DevUpgradeReferenceM67 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/upgrade-reference");
    public static final String FILE = "bettacraft_reference.json";
    /** The old pipe items (their ids), each in every container with its own count. */
    static final String[] OLD_ITEMS = {"filter_inlet_pipe", "filter_outlet_pipe", "filter_fresh_pipe", "filter_waste_pipe", "filter_fresh_port",
            "filter_waste_port", "pipe_tee", "interior_pipe"};
    private static BlockPos extrasStart;
    private static final List<BlockPos> containers = new ArrayList<>();
    private static final List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
    private static final List<String> problems = new ArrayList<>();

    private DevUpgradeReferenceM67() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("bettacraft", id));
    }

    // ------------------------------------------------------------------ extra row

    public static void extras(ServerLevel level, BlockPos rowStart) {
        // well south of the last showcase row (its tanks reach into the next row's space; the world is flat, nothing to clear)
        BlockPos start = rowStart.offset(0, 0, 16);
        extrasStart = start;
        LOGGER.info("[upgrade-reference] M67 pipe row at {}: chunks", start);
        for (int cx = (start.getX() - 8) >> 4; cx <= (start.getX() + 56) >> 4; cx++)
            for (int cz = (start.getZ() - 8) >> 4; cz <= (start.getZ() + 16) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
                level.getChunk(cx, cz);
            }
        for (int x = -2; x <= 44; x++)
            for (int z = -2; z <= 5; z++) {
                level.setBlockAndUpdate(start.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 0; y <= 5; y++) if (!level.getBlockState(start.offset(x, y, z)).isAir()) problems.add("the pipe row is not free at " + start.offset(x, y, z));
            }
        LOGGER.info("[upgrade-reference] station A");
        // A: intake and return in one old bundle on one tank face -> one shared dock, both guards
        Rig a = rig(level, start, "Ref geteilter Anschluss");
        guard(level, a, Plumb.INTAKE_GUARD);
        guard(level, a, Plumb.OUTLET_GUARD);
        expectShared(level, a, "A");
        LOGGER.info("[upgrade-reference] station B");
        // B: the same with interior pipes from the shared dock down to a nozzle (the return's outlet), the intake guard from outside
        Rig b = rig(level, start.offset(10, 0, 0), "Ref geteilter Anschluss mit Rohren");
        InteriorRegion r = InteriorManager.ensureRegion(level, b.tank);
        long k = TankAnchors.ensure(level, b.tank, b.member, Direction.WEST);
        if (r == null || k == Long.MIN_VALUE) problems.add("B: no dock");
        else {
            ServerLevel in = InteriorManager.interiorLevel(level.getServer());
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            in.setBlock(r.toWorld(x, y - 1, z), ModBlocks.INTERIOR_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
            in.setBlock(r.toWorld(x, y - 2, z), ModBlocks.INTERIOR_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
            in.setBlock(r.toWorld(x, y - 3, z), ModBlocks.NOZZLE.get().defaultBlockState().setValue(PlumbingBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            guard(level, b, Plumb.INTAKE_GUARD);
        }
        expectShared(level, b, "B");
        LOGGER.info("[upgrade-reference] station C");
        // C: stainless steel and glass pipes between two reservoirs
        BlockPos c = start.offset(20, 0, 1);
        level.setBlockAndUpdate(c, ModBlocks.RESERVOIR.get().defaultBlockState());
        level.setBlockAndUpdate(c.east(7), ModBlocks.RESERVOIR.get().defaultBlockState());
        if (level.getBlockEntity(c) instanceof org.overengineer.bettacraft.plumbing.ReservoirBlockEntity res)
            res.water().fill(new org.overengineer.bettacraft.platform.fluid.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 8000),
                    org.overengineer.bettacraft.platform.fluid.IFluidHandler.FluidAction.EXECUTE);
        for (int x = 1; x <= 6; x++) level.setBlockAndUpdate(c.east(x), (x <= 3 ? ModBlocks.STAINLESS_STEEL_PIPE : ModBlocks.GLASS_PIPE).get().defaultBlockState());
        for (int x = 0; x <= 7; x++) level.setBlockAndUpdate(c.east(x), Block.updateFromNeighbourShapes(level.getBlockState(c.east(x)), level, c.east(x)));
        LOGGER.info("[upgrade-reference] station D");
        // D: containers with every old pipe item
        BlockPos d = start.offset(32, 0, 1);
        level.setBlockAndUpdate(d, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(d.east(2), Blocks.BARREL.defaultBlockState());
        level.setBlockAndUpdate(d.east(4), Blocks.SHULKER_BOX.defaultBlockState());
        List<ItemStack> chest = new ArrayList<>();
        for (int i = 0; i < OLD_ITEMS.length; i++) chest.add(new ItemStack(item(OLD_ITEMS[i]), 5 + i));
        chest.add(new ItemStack(item("filter_inlet_pipe"), 64));
        chest.add(new ItemStack(item("interior_pipe"), 64));
        chest.add(new ItemStack(ModItems.GLASS_PIPE.get(), 3));
        chest.add(new ItemStack(ModItems.STAINLESS_STEEL_PIPE.get(), 4));
        chest.add(new ItemStack(ModItems.INTAKE_GUARD.get(), 2));
        chest.add(new ItemStack(ModItems.OUTLET_GUARD.get(), 2));
        chest.add(new ItemStack(ModItems.NOZZLE.get(), 1));
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(item("filter_outlet_pipe"), 9),
                new ItemStack(item("pipe_tee"), 8), new ItemStack(item("interior_pipe"), 10), new ItemStack(item("filter_waste_port"), 3))));
        chest.add(box);
        fill(level, d, chest);
        List<ItemStack> barrel = new ArrayList<>();
        for (int i = 0; i < OLD_ITEMS.length; i++) barrel.add(new ItemStack(item(OLD_ITEMS[i]), 20 + i));
        fill(level, d.east(2), barrel);
        List<ItemStack> shulker = new ArrayList<>();
        for (int i = 0; i < OLD_ITEMS.length; i++) shulker.add(new ItemStack(item(OLD_ITEMS[i]), 40 - i));
        fill(level, d.east(4), shulker);
        LOGGER.info("[upgrade-reference] M67 pipe row built at {}: shared docks, interior pipes, steel and glass pipes, {} containers", start, containers.size());
    }

    private static void fill(ServerLevel level, BlockPos p, List<ItemStack> items) {
        if (!(level.getBlockEntity(p) instanceof Container c)) {
            problems.add("no container at " + p);
            return;
        }
        for (int i = 0; i < items.size(); i++) c.setItem(i, items.get(i));
        level.getBlockEntity(p).setChanged();
        containers.add(p);
    }

    record Rig(BlockPos head, TankBlockEntity tank, BlockPos member) {}

    /**
     * A filter (chambers at {@code at}+(0,0,1), facing east), a tank 2x1x2 at {@code at}+(4,0,1); the intake pipes (2,0,1), (3,0,1),
     * the return pipes (2,1,1), (3,1,1), then the return line into (3,0,1): an old bundle (intake west-east, return up-east) on the west
     * face of the tank block (4,0,1). That is the world of {@code PipeLinesGameTests#oldWorld} built by the old version itself.
     */
    private static Rig rig(ServerLevel level, BlockPos at, String name) {
        BlockPos ch = at.offset(0, 0, 1);
        level.setBlockAndUpdate(ch, ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(ch.above(), ModBlocks.FILTER_CHAMBER.get().defaultBlockState());
        level.setBlockAndUpdate(ch.above(2), ModBlocks.FILTER_PUMP_HEAD.get().defaultBlockState());
        level.setBlockAndUpdate(ch.east(), ModBlocks.FILTER_INLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(ch.above().east(), ModBlocks.FILTER_OUTLET.get().defaultBlockState().setValue(FilterPartBlock.FACING, Direction.EAST));
        ((FilterChamberBlockEntity) level.getBlockEntity(ch)).setMedium(new ItemStack(ModItems.CERAMIC_RINGS.get()));
        ((FilterChamberBlockEntity) level.getBlockEntity(ch.above())).setMedium(new ItemStack(ModItems.FILTER_FOAM.get()));
        LOGGER.info("[upgrade-reference] {}: filter", name);
        TankBlockEntity t = tank(level, at.offset(4, 0, 1), 2, 1, 2);
        LOGGER.info("[upgrade-reference] {}: tank, pipes", name);
        Block in = ModBlocks.FILTER_INLET_PIPE.get(), out = ModBlocks.FILTER_OUTLET_PIPE.get();
        level.setBlockAndUpdate(at.offset(2, 0, 1), in.defaultBlockState());
        level.setBlockAndUpdate(at.offset(3, 0, 1), in.defaultBlockState());
        level.setBlockAndUpdate(at.offset(2, 1, 1), out.defaultBlockState());
        level.setBlockAndUpdate(at.offset(3, 1, 1), out.defaultBlockState());
        LOGGER.info("[upgrade-reference] {}: bundle", name);
        if (!FilterPipeBundleBlock.addLine(level, at.offset(3, 0, 1), PipeLine.RETURN, null, null)) problems.add(name + ": no bundle");
        t.getTank().setName(name);
        t.markTankChanged();
        return new Rig(ch.above(2), t, at.offset(4, 0, 1));
    }

    private static void guard(ServerLevel level, Rig k, Plumb kind) {
        ItemStack s = new ItemStack(kind == Plumb.INTAKE_GUARD ? ModItems.INTAKE_GUARD.get() : ModItems.OUTLET_GUARD.get());
        if (!TankAnchors.installGuard(level, k.member, Direction.WEST, null, s, kind).consumesAction()) problems.add(k.tank.getTank().name() + ": no " + kind);
    }

    /** The filter reaches the tank with its intake and return line through the same face: one dock with an intake and an outlet behind it. */
    private static void expectShared(ServerLevel level, Rig k, String what) {
        LOGGER.info("[upgrade-reference] {}: check", what);
        MultiblockFilter.Structure st = MultiblockFilter.scan(level, k.head);
        List<MultiblockFilter.Link> links = MultiblockFilter.links(level, st);
        FilterLines.Dock dock = new FilterLines.Dock(k.member, Direction.WEST);
        if (links.size() != 1 || !links.get(0).intakes().equals(List.of(dock)) || !links.get(0).returns().equals(List.of(dock))) {
            problems.add(what + ": the lines do not share the dock: " + links.stream().map(l -> l.intakes() + "/" + l.returns()).toList());
            return;
        }
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(k.tank.getTank().regionId());
        long key = TankAnchors.existing(level, k.tank, k.member, Direction.WEST);
        InteriorPlumbing.Net n = r == null || key == Long.MIN_VALUE ? null : InteriorPlumbing.net(r, key);
        if (n == null || n.intakes().size() != 1 || n.outflows().size() != 1) problems.add(what + ": the shared dock holds " + n);
        FilterPumpHeadBlockEntity h = (FilterPumpHeadBlockEntity) level.getBlockEntity(k.head);
        int moved = h.run(level, true);
        if (moved <= 0 || h.tanks().size() != 1 || h.tanks().get(0).status() != MultiblockFilter.LinkStatus.OK) problems.add(what + ": the filter does not filter: " + moved + " " + h.tanks());
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos p : blocks) level.setBlockAndUpdate(p, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos p : blocks) TankMultiblock.onTankBlockAdded(level, p, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        if (c == null) throw new IllegalStateException("no tank at " + min);
        TankInteractions.changeWater(level, c, (long) blocks.size() * 1000, 0F);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setWaterTemp(25F);
        c.getTank().setWaterQuality(1F);
        TankEquipment.invalidate(c);
        c.markTankChanged();
        return c;
    }

    // ------------------------------------------------------------------ manifest

    private static int mask(java.util.function.Predicate<Direction> joined) {
        int m = 0;
        for (Direction d : Direction.values()) if (joined.test(d)) m |= 1 << d.get3DDataValue();
        return m;
    }

    public static void manifest(MinecraftServer server, ServerLevel level) {
        JsonObject root = new JsonObject();
        root.addProperty("version", org.overengineer.bettacraft.platform.Platform.get().modVersion("bettacraft"));
        root.addProperty("gameTime", level.getGameTime());
        JsonArray tanks = new JsonArray(), chests = new JsonArray(), pipes = new JsonArray(), filters = new JsonArray(), shared = new JsonArray(), boxes = new JsonArray();
        BlockPos spawn = level.getRespawnData().pos();
        BlockPos end = extrasStart == null ? spawn.offset(0, 0, 220) : extrasStart.offset(0, 0, 24);
        int y0 = spawn.getY() - 6, y1 = spawn.getY() + 20;
        for (int cx = (spawn.getX() - 16) >> 4; cx <= (spawn.getX() + 190) >> 4; cx++)
            for (int cz = (spawn.getZ() - 16) >> 4; cz <= end.getZ() >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (be instanceof TankBlockEntity t && t.isController() && t.getTank() != null) tanks.add(tank(server, level, t));
                    if (be instanceof ChestBlockEntity cb) chests.add(chest(cb));
                    if (be instanceof FilterPumpHeadBlockEntity h) filters.add(filter(level, h, shared));
                }
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        for (int y = y0; y <= y1; y++) {
                            BlockPos p = new BlockPos(cx * 16 + x, y, cz * 16 + z);
                            JsonObject o = pipe(level, p, chunk.getBlockState(p));
                            if (o != null) pipes.add(o);
                        }
            }
        for (BlockPos p : containers) boxes.add(container(level, p));
        JsonObject pj = new JsonObject();
        pj.add("blocks", pipes);
        pj.add("filters", filters);
        pj.add("sharedDocks", shared);
        pj.add("containers", boxes);
        root.add("tanks", tanks);
        root.add("chests", chests);
        root.add("pipes", pj);
        int oldPipes = 0, bundles = 0, tees = 0, ports = 0, plain = 0;
        for (var e : pipes) {
            String k = e.getAsJsonObject().get("kind").getAsString();
            switch (k) {
                case "filter" -> {
                    oldPipes++;
                    if (e.getAsJsonObject().get("junction").getAsBoolean()) tees++;
                }
                case "bundle" -> bundles++;
                case "port" -> ports++;
                default -> plain++;
            }
        }
        if (oldPipes == 0 || bundles < 3 || tees == 0 || ports < 2 || plain == 0 || shared.size() < 2 || filters.isEmpty())
            problems.add("the world lacks old pipes: " + oldPipes + " filter pipes, " + bundles + " bundles, " + tees + " T-pieces, " + ports + " ports, "
                    + plain + " glass/steel pipes, " + shared.size() + " shared docks, " + filters.size() + " filters");
        for (var cp : forced) level.setChunkForced(cp.x(), cp.z(), false);
        if (!problems.isEmpty()) {
            for (String p : problems) LOGGER.error("[upgrade-reference] PROBLEM {}", p);
            return; // no manifest: build_reference.sh fails
        }
        try {
            Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(FILE), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("[upgrade-reference] {} old filter pipes ({} T-pieces), {} bundles, {} ports, {} glass/steel pipes, {} filters, {} shared docks, {} containers",
                oldPipes, tees, bundles, ports, plain, filters.size(), shared.size(), boxes.size());
        LOGGER.info("[upgrade-reference] MANIFEST WRITTEN: {} tanks, {} chests", tanks.size(), chests.size());
    }

    /** An old pipe block: its kind, line and the sides it is joined on (bit 1 << 3D data value); null for anything else. */
    private static JsonObject pipe(ServerLevel level, BlockPos p, BlockState s) {
        Block b = s.getBlock();
        String kind;
        JsonObject o = new JsonObject();
        if (b instanceof FilterPipeBundleBlock) {
            kind = "bundle";
            int lines = FilterPipes.packed(level, p, s);
            JsonArray ls = new JsonArray();
            for (PipeLine l : FilterPipes.lines(lines)) {
                int slot = FilterPipes.slot(lines, l);
                JsonObject lo = new JsonObject();
                lo.addProperty("line", l.ordinal());
                lo.addProperty("name", l.getSerializedName());
                lo.addProperty("sides", mask(d -> FilterPipes.joined(slot, d)));
                lo.addProperty("junction", FilterPipes.junction(slot));
                ls.add(lo);
            }
            o.add("lines", ls);
        } else if (b instanceof FilterPipeBlock f) {
            kind = "filter";
            o.addProperty("line", f.line().ordinal());
            o.addProperty("name", f.line().getSerializedName());
            o.addProperty("sides", mask(d -> FilterPipeBlock.joined(s, d)));
            o.addProperty("junction", s.getValue(FilterPipeBlock.JUNCTION));
        } else if (b instanceof FilterPartBlock f && (f.part() == FilterPartBlock.Part.FRESH || f.part() == FilterPartBlock.Part.WASTE)) {
            kind = "port";
            o.addProperty("facing", s.getValue(FilterPartBlock.FACING).getName());
        } else if (b instanceof PipeBlock) {
            kind = "pipe";
            o.addProperty("sides", mask(d -> s.getValue(PipeBlock.SIDES.get(d))));
        } else {
            return null;
        }
        o.addProperty("pos", p.asLong());
        o.addProperty("id", BuiltInRegistries.BLOCK.getKey(b).toString());
        o.addProperty("kind", kind);
        if (level.getBlockEntity(p) instanceof org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity be && be.cover() != null)
            o.addProperty("cover", BuiltInRegistries.BLOCK.getKey(be.cover().getBlock()).toString());
        return o;
    }

    /** A multiblock filter after a run: its tanks and their status; the docks both of its lines share. */
    private static JsonObject filter(ServerLevel level, FilterPumpHeadBlockEntity h, JsonArray shared) {
        int moved = h.run(level, true);
        JsonObject o = new JsonObject();
        o.addProperty("pos", h.getBlockPos().asLong());
        o.addProperty("moved", moved);
        o.addProperty("problem", h.problem().name());
        JsonArray links = new JsonArray();
        for (var t : h.tanks()) {
            JsonObject l = new JsonObject();
            l.addProperty("tank", t.tank().asLong());
            l.addProperty("status", t.status().name());
            links.add(l);
        }
        o.add("links", links);
        for (MultiblockFilter.Link l : MultiblockFilter.links(level, MultiblockFilter.scan(level, h.getBlockPos()))) {
            for (FilterLines.Dock d : l.intakes()) {
                if (!l.returns().contains(d)) continue;
                JsonObject s = new JsonObject();
                s.addProperty("filter", h.getBlockPos().asLong());
                s.addProperty("tank", l.tank().getBlockPos().asLong());
                s.addProperty("member", d.member().asLong());
                s.addProperty("face", d.face().getName());
                InteriorRegion r = InteriorRegions.get(level.getServer()).get(l.tank().getTank().regionId());
                long key = TankAnchors.existing(level, l.tank(), d.member(), d.face());
                if (r != null && key != Long.MIN_VALUE) {
                    InteriorPlumbing.Net n = InteriorPlumbing.net(r, key);
                    s.addProperty("key", key);
                    s.addProperty("intakes", n.intakes().size());
                    s.addProperty("outflows", n.outflows().size());
                }
                shared.add(s);
            }
        }
        return o;
    }

    /** Every item of a container by id, the contents of shulker box items in it included. */
    static void count(ItemStack s, Map<String, Integer> out) {
        if (s.isEmpty()) return;
        out.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        ItemContainerContents in = s.get(DataComponents.CONTAINER);
        if (in == null) return;
        // 26.1: the contents hold item templates; copied out as stacks
        net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(64, ItemStack.EMPTY);
        in.copyInto(list);
        list.forEach(i -> count(i, out));
    }

    private static JsonObject container(ServerLevel level, BlockPos p) {
        JsonObject o = new JsonObject();
        o.addProperty("pos", p.asLong());
        o.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString());
        Map<String, Integer> counts = new TreeMap<>();
        if (level.getBlockEntity(p) instanceof Container c) for (int i = 0; i < c.getContainerSize(); i++) count(c.getItem(i), counts);
        JsonObject co = new JsonObject();
        counts.forEach(co::addProperty);
        o.add("counts", co);
        return o;
    }

    // ------------------------------------------------------------------ tanks and chests (the format of DevSmokeUpgrade)

    private static JsonObject tank(MinecraftServer server, ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        JsonObject o = new JsonObject();
        o.addProperty("pos", c.getBlockPos().asLong());
        o.addProperty("name", t.name());
        o.addProperty("water", t.waterMb());
        o.addProperty("salinity", t.salinity());
        o.addProperty("terra_mode", t.terrariumMode().id());
        o.addProperty("tank_mode", t.tankMode().id());
        o.addProperty("cap", t.capacityMb());
        o.addProperty("land", t.landShare());
        o.addProperty("members", t.memberCount());
        o.addProperty("front", t.front().getName());
        o.addProperty("locked", t.locked());
        o.addProperty("owner", t.owner() == null ? "" : t.owner().toString());
        o.addProperty("signVisible", t.signVisible());
        o.addProperty("floating", t.plants().size());
        o.addProperty("regionId", t.regionId());
        JsonArray inh = new JsonArray();
        for (UUID u : t.inhabitants()) {
            JsonObject a = new JsonObject();
            a.addProperty("uuid", u.toString());
            Entity e = level.getEntity(u);
            if (e != null) {
                a.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
                if (e instanceof TankmateEntity m) {
                    a.addProperty("variety", m.variety());
                    a.addProperty("female", m.isFemale());
                    a.addProperty("stage", m.lifeStage().name());
                    var lb = m.genes().lb();
                    if (lb != null) {
                        a.addProperty("lb", lb.toString());
                        var strain = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(lb.express(m.isFemale()));
                        a.addProperty("strain", strain == null ? "" : strain.id());
                    }
                } else if (e instanceof BettaEntity b) {
                    a.addProperty("genome", b.genome().toString());
                    a.addProperty("female", b.isFemale());
                    a.addProperty("stage", b.lifeStage().name());
                }
            }
            inh.add(a);
        }
        o.add("inhabitants", inh);
        JsonArray crit = new JsonArray();
        List<String> cs = new ArrayList<>();
        for (Critter k : t.critters()) cs.add(k.species.id() + "|" + k.genes.variety(k.species) + "|" + k.female + "|" + k.genes.a() + "/" + k.genes.b());
        java.util.Collections.sort(cs);
        cs.forEach(crit::add);
        o.add("critters", crit);
        if (t.regionId() >= 0) {
            InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
            ServerLevel in = InteriorManager.interiorLevel(server);
            if (r != null && in != null) {
                Map<String, Integer> counts = new TreeMap<>();
                for (int y = 0; y < r.sizeY(); y++)
                    for (int z = 0; z < r.sizeZ(); z++)
                        for (int x = 0; x < r.sizeX(); x++) {
                            BlockState s = in.getBlockState(r.toWorld(x, y, z));
                            if (s.isAir()) continue;
                            counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                        }
                JsonObject co = new JsonObject();
                counts.forEach(co::addProperty);
                o.add("interior", co);
            }
        }
        JsonArray eq = new JsonArray();
        BlockPos lo = t.min().offset(-1, -1, -1), hi = t.max().offset(1, 1, 1);
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            BlockState s = level.getBlockState(p);
            Identifier id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
            if (!id.getNamespace().equals("bettacraft") || s.is(ModBlocks.TANK.get())) continue;
            JsonObject e = new JsonObject();
            e.addProperty("pos", p.asLong());
            e.addProperty("block", id.toString());
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof CabinetBlockEntity cab) {
                e.addProperty("wood", cab.woodId().toString());
                e.addProperty("filter", BuiltInRegistries.ITEM.getKey(cab.filterItem().getItem()).toString());
                e.addProperty("heater0", BuiltInRegistries.ITEM.getKey(cab.heater(0).getItem()).toString());
                e.addProperty("items", items(cab.items()));
            } else if (be instanceof FeederBlockEntity f) {
                e.addProperty("items", items(f.items()));
                e.addProperty("feedings", f.feedingsPerDay());
            }
            eq.add(e);
        }
        o.add("equipment", eq);
        return o;
    }

    private static String items(org.overengineer.bettacraft.platform.item.IItemHandler h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            sb.append(i).append(':').append(BuiltInRegistries.ITEM.getKey(s.getItem())).append('x').append(s.getCount()).append(' ');
        }
        return sb.toString().trim();
    }

    private static JsonObject chest(ChestBlockEntity cb) {
        JsonObject o = new JsonObject();
        o.addProperty("pos", cb.getBlockPos().asLong());
        JsonArray a = new JsonArray();
        for (int i = 0; i < cb.getContainerSize(); i++) {
            ItemStack s = cb.getItem(i);
            if (s.isEmpty()) continue;
            JsonObject it = new JsonObject();
            it.addProperty("slot", i);
            it.addProperty("id", BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            it.addProperty("count", s.getCount());
            List<String> comps = new ArrayList<>();
            for (var c : s.getComponentsPatch().entrySet()) comps.add(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.getKey())));
            java.util.Collections.sort(comps);
            it.addProperty("components", String.join(",", comps));
            a.add(it);
        }
        o.add("items", a);
        return o;
    }
}
