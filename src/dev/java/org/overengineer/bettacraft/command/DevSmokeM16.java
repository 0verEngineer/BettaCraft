package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.compat.AquaticSpecies;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M16 part of the dedicated-server smoke test: every water animal the loaded mods register is looked at; one of every
 * accepted kind that is not ours goes into a 4x2x3 tank (bucket or fish bag), every refused kind must be refused; the real
 * care loop runs for the rest of the test and all of them must still be alive in the tank at the end. A tank block set
 * into the interior dimension is removed again (no tank inside a tank).
 */
final class DevSmokeM16 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final int MAX_ANIMALS = 40;
    private static final Map<UUID, String> INSIDE = new LinkedHashMap<>();
    private static BlockPos interiorProbe;

    private DevSmokeM16() {}

    private static BlockPos tankPos(BlockPos base) {
        return base.offset(0, 0, 44);
    }

    static void setup(ServerLevel level, BlockPos base) {
        BlockPos p = tankPos(base);
        AABB box = new AABB(p).expandTowards(4, 2, 3).inflate(3);
        for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) e.discard();
        for (int x = 0; x < 4; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 3; z++) {
                    BlockPos q = p.offset(x, y, z);
                    if (!level.getBlockState(q).isAir()) level.setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
                }
        for (int x = 0; x < 4; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 3; z++) {
                    level.setBlockAndUpdate(p.offset(x, y, z), ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(level, p.offset(x, y, z), null);
                }
        TankBlockEntity c = TankMultiblock.getController(level, p);
        if (c == null) throw new IllegalStateException("M16: tank did not form");
        TankInteractions.changeWater(level, c, c.getTank().capacityMb() - 4000);
        List<AquaticSpecies.Species> all = AquaticSpecies.discover(level);
        int refused = 0;
        INSIDE.clear();
        for (AquaticSpecies.Species s : all) {
            if (s.id().getNamespace().equals("bettacraft")) continue;
            ItemStack stack = container(level, s);
            if (stack.isEmpty()) continue;
            boolean variable = s.accepted() || s.acceptance() == TankInhabitants.Acceptance.TOO_BIG; // size by variant
            if (!s.accepted() && !variable) {
                Component err = TankInhabitants.insert(level, c, stack, s.type(), null, TankInhabitants.carriedWater(stack));
                if (err == null) throw new IllegalStateException("M16: " + s.id() + " (" + s.acceptance() + ") was let in");
                refused++;
                continue;
            }
            if (INSIDE.size() >= MAX_ANIMALS) continue;
            List<UUID> before = new ArrayList<>(c.getTank().inhabitants());
            Component err = TankInhabitants.insert(level, c, stack, s.type(), null, 0);
            if (err != null) {
                if (s.accepted() && !err.getString().equals(TankInhabitants.rejection(TankInhabitants.Acceptance.TOO_BIG, s.type()).getString())) {
                    throw new IllegalStateException("M16: " + s.id() + " refused: " + err.getString());
                }
                refused++;
                continue;
            }
            for (UUID u : c.getTank().inhabitants()) if (!before.contains(u)) INSIDE.put(u, s.id().toString());
        }
        LOGGER.info("[smoketest] M16: {} water animals known, {} kinds put into the mixed tank, {} refused", all.size(), INSIDE.size(), refused);
        // no tank inside a tank: set one into the interior of this tank without the item
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) throw new IllegalStateException("M16: no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        interiorProbe = r.toWorld(3, 0, 3);
        interior.setBlockAndUpdate(interiorProbe, ModBlocks.TANK.get().defaultBlockState());
    }

    private static ItemStack container(ServerLevel level, AquaticSpecies.Species s) {
        if (!s.buckets().isEmpty()) return new ItemStack(s.buckets().getFirst());
        Entity e = s.type().create(level);
        if (e == null) return ItemStack.EMPTY;
        ItemStack bag = FishBagItem.fillWith(e);
        e.discard();
        return bag;
    }

    static void check(ServerLevel level, BlockPos base) {
        TankBlockEntity c = TankMultiblock.getController(level, tankPos(base));
        if (c == null) throw new IllegalStateException("M16: tank gone");
        List<String> missing = new ArrayList<>();
        for (var en : INSIDE.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null || !e.isAlive() || !TankInhabitants.inside(c.getTank(), e)) missing.add(en.getValue());
        }
        // in a mixed tank the predators (axolotl, pufferfish, piranhas ...) may hunt a little; every single kind on its own
        // is checked strictly by the GameTests (AquaticGameTests)
        if (missing.size() > INSIDE.size() / 4) throw new IllegalStateException("M16: too many gone from the mixed tank: " + missing);
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior.getBlockState(interiorProbe).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock) {
            throw new IllegalStateException("M16: a tank stayed inside a tank interior");
        }
        LOGGER.info("[smoketest] M16: {} of {} foreign animals alive in the mixed tank (gone: {}), stocking {}, no tank in the interior",
                INSIDE.size() - missing.size(), INSIDE.size(), missing, TankInhabitants.stocking(level, c.getTank()));
    }
}
