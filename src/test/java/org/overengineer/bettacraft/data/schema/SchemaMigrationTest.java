package org.overengineer.bettacraft.data.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

/**
 * Runs without a Minecraft bootstrap: CompoundTag and TagParser are plain classes.
 */
class SchemaMigrationTest {

    private static CompoundTag snbt(String s) {
        try {
            return TagParser.parseTag(s);
        } catch (CommandSyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private static final MigrationChain DEMO = MigrationChain.builder("demo")
            .step(0, d -> { d.putString("added_in_v1", "x"); return d; })
            .step(1, d -> { d.putInt("renamed", d.getInt("old")); d.remove("old"); return d; })
            .build();

    @Test
    void missingSchemaIsVersionZero() {
        assertEquals(0, Schema.read(new CompoundTag()));
        assertEquals(3, Schema.read(snbt("{schema:3}")));
        assertEquals(0, Schema.read(snbt("{schema:\"oops\"}")));
        assertEquals(2, Schema.read(snbt("{schema:2b}")));
    }

    @Test
    void runsAllStepsInOrderAndStamps() {
        CompoundTag in = snbt("{old:7,unknown:\"keep me\"}");
        CompoundTag out = DEMO.migrate(in);
        assertEquals(2, DEMO.currentVersion());
        assertEquals(2, Schema.read(out));
        assertEquals("x", out.getString("added_in_v1"));
        assertEquals(7, out.getInt("renamed"));
        assertFalse(out.contains("old"));
        assertEquals("keep me", out.getString("unknown"), "unknown keys must be carried along");
    }

    @Test
    void startsAtStoredVersion() {
        CompoundTag out = DEMO.migrate(snbt("{schema:1,old:5}"));
        assertFalse(out.contains("added_in_v1"), "step 0 must not run for v1 data");
        assertEquals(5, out.getInt("renamed"));
    }

    @Test
    void inputIsNotModified() {
        CompoundTag in = snbt("{old:1}");
        CompoundTag out = DEMO.migrate(in);
        assertNotSame(in, out);
        assertTrue(in.contains("old"));
        assertFalse(in.contains(Schema.KEY));
    }

    @Test
    void newerDataIsLeftUntouched() {
        CompoundTag in = snbt("{schema:99,future:1b}");
        CompoundTag out = DEMO.migrate(in);
        assertEquals(in, out);
        assertFalse(DEMO.needsMigration(in));
    }

    @Test
    void stepsMustBeContiguous() {
        assertThrows(IllegalArgumentException.class, () -> MigrationChain.builder("bad").step(1, d -> d));
    }

    // ---- frozen examples of the real chains ----

    @Test
    void tankV0ControllerGetsDefaults() {
        // Frozen v0 example: controller written before the schema field existed.
        CompoundTag v0 = snbt("{glass:\"minecraft:glass\",controller:[I;0,64,0],tank:{members:[L;0L],owner:[I;1,2,3,4]}}");
        CompoundTag v1 = Schemas.TANK_BLOCK_ENTITY.migrate(v0);
        assertEquals(2, Schema.read(v1));
        assertEquals(16, v1.getCompound("tank").getInt("scale"));
        assertEquals(0L, v1.getCompound("tank").getLong("water_mb"));
        assertEquals("minecraft:glass", v1.getString("glass"));
    }

    @Test
    void tankV0KeepsExistingValues() {
        CompoundTag v0 = snbt("{tank:{scale:8,water_mb:4000L}}");
        CompoundTag v1 = Schemas.TANK_BLOCK_ENTITY.migrate(v0);
        assertEquals(8, v1.getCompound("tank").getInt("scale"));
        assertEquals(4000L, v1.getCompound("tank").getLong("water_mb"));
    }

    @Test
    void tankMemberWithoutTankDataIsUnchanged() {
        CompoundTag v0 = snbt("{glass:\"minecraft:glass\",controller:[I;1,2,3]}");
        CompoundTag v1 = Schemas.TANK_BLOCK_ENTITY.migrate(v0);
        assertFalse(v1.contains("tank"));
        assertEquals(2, Schema.read(v1));
    }

    @Test
    void tankV1GetsCareDefaults() {
        // Frozen v1 example (M0 controller).
        CompoundTag v1 = snbt("{schema:1,glass:\"minecraft:glass\",controller:[I;0,64,0],tank:{members:[L;0L],scale:16,water_mb:1000L,region:-1}}");
        CompoundTag v2 = Schemas.TANK_BLOCK_ENTITY.migrate(v1);
        CompoundTag t = v2.getCompound("tank");
        assertEquals(2, Schema.read(v2));
        assertEquals(27.0F, t.getFloat("target_temp"));
        assertEquals(27.0F, t.getFloat("water_temp"));
        assertTrue(t.getBoolean("sign_visible"));
        assertEquals("top_center", t.getString("sign_pos"));
        assertEquals("south", t.getString("front"));
        assertEquals(1000L, t.getLong("water_mb"), "existing values are kept");
    }

    @Test
    void tankV1KeepsExistingCareValues() {
        CompoundTag v2 = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:1,tank:{target_temp:30.0f,sign_visible:0b,front:\"east\"}}"));
        CompoundTag t = v2.getCompound("tank");
        assertEquals(30.0F, t.getFloat("target_temp"));
        assertEquals(30.0F, t.getFloat("water_temp"), "water starts at the target temperature");
        assertFalse(t.getBoolean("sign_visible"));
        assertEquals("east", t.getString("front"));
    }

    @Test
    void bettaV0IsStamped() {
        CompoundTag v0 = snbt("{genome:{shape:\"veil\",body_color:\"does_not_exist\"},sex:\"female\",custom:1b}");
        CompoundTag v1 = Schemas.BETTA.migrate(v0);
        assertEquals(1, Schema.read(v1));
        assertEquals("does_not_exist", v1.getCompound("genome").getString("body_color"), "unknown ids are carried along");
        assertTrue(v1.getBoolean("custom"));
    }

    @Test
    void currentVersionsAreStable() {
        // Bump these deliberately together with a new step + frozen test.
        assertEquals(2, Schemas.TANK_BLOCK_ENTITY.currentVersion());
        assertEquals(1, Schemas.BETTA.currentVersion());
        assertEquals(1, Schemas.TANK_REF.currentVersion());
        assertEquals(1, Schemas.FEEDER.currentVersion());
        assertEquals(1, Schemas.INTERIOR_REGIONS.currentVersion());
        assertEquals(1, Schemas.INTERIOR_RETURN.currentVersion());
    }
}
