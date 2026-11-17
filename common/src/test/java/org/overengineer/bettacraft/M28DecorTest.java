package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.decor.DecorType;

/** M28 part c: the new decorations (structure files and sizes) and the tannins of the leaf litter. */
class M28DecorTest {
    private static final Path STRUCTURES = Path.of(System.getProperty("bettacraft.projectDir", "."))
            .resolve("src/main/resources/data/bettacraft/structure/decor");

    private static CompoundTag read(String name) throws Exception {
        return NbtIo.readCompressed(STRUCTURES.resolve(name + ".nbt"), NbtAccounter.unlimitedHeap());
    }

    /** Every decoration has its structure; the size in the enum (tooltip, handbook) is the size of the file. */
    @Test
    void sizesMatchTheStructures() throws Exception {
        for (DecorType t : DecorType.values()) {
            assertTrue(Files.exists(STRUCTURES.resolve(t.id() + ".nbt")), "structure of " + t);
            ListTag size = read(t.id()).getListOrEmpty("size");
            assertEquals(t.sizeX, size.getInt(0), t + " x");
            assertEquals(t.sizeY, size.getInt(1), t + " y");
            assertEquals(t.sizeZ, size.getInt(2), t + " z");
        }
    }

    private static int count(CompoundTag nbt, String block) {
        ListTag palette = nbt.getListOrEmpty("palette");
        int n = 0;
        ListTag blocks = nbt.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            if (palette.getCompound(blocks.getCompoundOrEmpty(i).getIntOr("state", 0)).getStringOr("Name", "").equals(block)) n++;
        }
        return n;
    }

    /** The caves for plecos are tubes of spawning cave blocks; the treasure chest has its air stone; no valuable blocks. */
    @Test
    void cavesAndOutletsAreInTheStructures() throws Exception {
        assertTrue(count(read("ship_bow"), "bettacraft:spawning_cave") >= 2 * 20, "ship bow: clay pipe");
        assertTrue(count(read("clay_tubes"), "bettacraft:spawning_cave") >= 2 * 2 * 20, "clay tubes");
        assertEquals(1, count(read("treasure_chest"), "bettacraft:air_stone"), "treasure chest: one air stone");
        for (DecorType t : DecorType.values()) {
            if (!t.hasHint()) continue;
            CompoundTag nbt = read(t.id());
            for (String rich : new String[]{"minecraft:gold_block", "minecraft:raw_gold_block", "minecraft:copper_block", "minecraft:iron_block",
                    "minecraft:diamond_block", "minecraft:emerald_block", "minecraft:waxed_copper_block", "minecraft:chest"}) {
                assertEquals(0, count(nbt, rich), t + " has " + rich);
            }
        }
    }

    /** Litter in the water settles at half of what as many leaves put in give (a cone: a third of a leaf). */
    @Test
    void litterSettlesAtHalfTheLeaves() {
        double half = 7.0, per = 1.0;
        long water = 8000;
        double rate = WaterChemistry.litterTanninPerDay(4, 0, water, per, half);
        float tannin = 0;
        for (int day = 0; day < 200; day++) {
            for (int s = 0; s < 20; s++) {
                tannin = (float) Math.min(1.0, tannin + rate * 0.05);
                tannin = WaterChemistry.fadeTannin(tannin, 0.05, half);
            }
        }
        double leaves = WaterChemistry.addLeaves(0F, water, 4, per);
        assertEquals(WaterChemistry.LITTER_SHARE * leaves, tannin, 0.01);
        assertEquals(WaterChemistry.litterTanninPerDay(0, 3, water, per, half), WaterChemistry.litterTanninPerDay(1, 0, water, per, half), 1e-9);
        assertEquals(0, WaterChemistry.litterTanninPerDay(0, 0, water, per, half));
        assertEquals(0, WaterChemistry.litterTanninPerDay(5, 5, 0, per, half));
    }

    /** New decorations are appended (the item ids come from the names). */
    @Test
    void newDecorationsAreAppended() {
        assertEquals(DecorType.SPAWNING_MOP.ordinal() + 1, DecorType.ROWING_BOAT.ordinal());
        for (DecorType t : DecorType.values()) assertEquals(t.ordinal() >= DecorType.ROWING_BOAT.ordinal(), t.hasHint(), t.name());
        for (var e : Map.of(DecorType.SHIP_BOW, 14, DecorType.CLAY_TUBES, 13).entrySet()) assertTrue(e.getKey().sizeX <= e.getValue());
    }
}
