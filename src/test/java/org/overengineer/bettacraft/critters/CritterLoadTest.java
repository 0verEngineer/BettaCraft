package org.overengineer.bettacraft.critters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** M17 review: corrupt numbers in saved small animals fall back to sane values (NaN broke the sort and hunger death). */
class CritterLoadTest {
    @Test
    void nanAndNegativeNumbersAreRepaired() {
        Critter c = new Critter(7, CritterSpecies.values()[0], true, CritterGenes.WILD, 1.5, 0.1, 1.5);
        CompoundTag t = c.save();
        t.putDouble("hunger", Double.NaN);
        t.putDouble("age", Double.POSITIVE_INFINITY);
        t.putDouble("molt", Double.NaN);
        t.putDouble("dead", Double.NaN);
        t.putInt("gen", -5);
        Critter l = Critter.load(t);
        assertNotNull(l);
        assertEquals(0, l.hunger);
        assertTrue(Double.isFinite(l.age) && l.age >= 0, "age " + l.age);
        assertTrue(Double.isFinite(l.sinceMolt));
        assertEquals(-1, l.dead);
        assertEquals(0, l.generation);
    }

    @Test
    void objectCountIsBounded() {
        CritterObject o = new CritterObject(3, CritterObject.Kind.CLUTCH, 1, 1, 1);
        o.species = CritterSpecies.values()[0];
        CompoundTag t = o.save();
        t.putInt("count", 1_000_000);
        t.putDouble("age", Double.NaN);
        CritterObject l = CritterObject.load(t);
        assertNotNull(l);
        assertTrue(l.count <= 1000, "count " + l.count);
        assertEquals(0, l.age);
    }
}
