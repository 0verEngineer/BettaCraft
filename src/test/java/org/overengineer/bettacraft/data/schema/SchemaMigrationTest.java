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
        assertEquals(10, Schema.read(v1));
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
        assertEquals(10, Schema.read(v1));
    }

    @Test
    void tankV1GetsCareDefaults() {
        // Frozen v1 example (M0 controller).
        CompoundTag v1 = snbt("{schema:1,glass:\"minecraft:glass\",controller:[I;0,64,0],tank:{members:[L;0L],scale:16,water_mb:1000L,region:-1}}");
        CompoundTag v2 = Schemas.TANK_BLOCK_ENTITY.migrate(v1);
        CompoundTag t = v2.getCompound("tank");
        assertEquals(10, Schema.read(v2));
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

    /** M22.3: every betta of an older world counts as analysed (magnifier); a new one (v4 without the flag) does not. */
    @Test
    void bettaV3CountsAsAnalysed() {
        CompoundTag v3 = snbt("{schema:3,genome:{finform:[\"halfmoon\",\"veil\"]},sex:\"male\",stage:\"adult\",generation:2}");
        CompoundTag v4 = Schemas.BETTA.migrate(v3);
        assertEquals(4, Schema.read(v4));
        assertTrue(v4.getBoolean("analysed"));
        assertTrue(org.overengineer.bettacraft.entity.betta.BettaData.load(v3).analysed());
        assertFalse(org.overengineer.bettacraft.entity.betta.BettaData.load(snbt("{schema:4,sex:\"male\"}")).analysed());
        var fresh = org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenome.DEFAULT, false, 0);
        assertFalse(org.overengineer.bettacraft.entity.betta.BettaData.load(fresh.save()).analysed(), "a new betta keeps its genes hidden");
        assertTrue(org.overengineer.bettacraft.entity.betta.BettaData.load(fresh.withAnalysed(true).save()).analysed(), "the analysis is kept");
    }

    @Test
    void bettaV0IsStamped() {
        CompoundTag v0 = snbt("{genome:{shape:\"veil\",body_color:\"does_not_exist\"},sex:\"female\",custom:1b}");
        CompoundTag v2 = Schemas.BETTA.migrate(v0);
        assertEquals(4, Schema.read(v2));
        net.minecraft.nbt.ListTag body = v2.getCompound("genome").getList("body_color", net.minecraft.nbt.Tag.TAG_STRING);
        assertEquals("does_not_exist", body.getString(0), "unknown ids are carried along");
        assertEquals("does_not_exist", body.getString(1));
        assertTrue(v2.getBoolean("custom"));
    }

    @Test
    void bettaV1GenomeBecomesHomozygousPairs() {
        // Frozen v1 example (M1 bucket / entity data): one value per locus.
        CompoundTag v1 = snbt("{schema:1,genome:{species:\"bettacraft:betta\",shape:\"halfmoon\",pectoral:\"dumbo\",body_color:\"red\","
                + "fin_color:\"royal_blue\",pattern:\"marble\",pattern_color:\"black\",iridescence:\"high\",iridescence_color:\"turquoise\","
                + "future_gene:\"x\"},sex:\"male\",stage:\"adult\",generation:0,born:12L,health:{sick:0b}}");
        CompoundTag v2 = Schemas.BETTA.migrate(v1);
        assertEquals(4, Schema.read(v2));
        CompoundTag g = v2.getCompound("genome");
        assertFalse(g.contains("shape"), "shape is renamed to finform");
        net.minecraft.nbt.ListTag fin = g.getList("finform", net.minecraft.nbt.Tag.TAG_STRING);
        assertEquals(2, fin.size());
        assertEquals("halfmoon", fin.getString(0));
        assertEquals("halfmoon", fin.getString(1));
        for (String k : new String[]{"pectoral", "body_color", "fin_color", "pattern", "pattern_color", "iridescence", "iridescence_color"}) {
            net.minecraft.nbt.ListTag l = g.getList(k, net.minecraft.nbt.Tag.TAG_STRING);
            assertEquals(2, l.size(), k);
            assertEquals(l.getString(0), l.getString(1), k + " is homozygous");
        }
        assertEquals("x", g.getString("future_gene"), "unknown genome keys are kept");
        assertEquals("bettacraft:betta", g.getString("species"));
        assertEquals(12L, v2.getLong("born"));
        // and the genotype shows exactly the old phenotype
        var gt = org.overengineer.bettacraft.genetics.BettaGenotype.load(g);
        var ph = gt.express();
        assertEquals("halfmoon", ph.shape());
        assertEquals("dumbo", ph.pectoral());
        assertEquals("royal_blue", ph.finColor());
        assertEquals("marble", ph.pattern());
        assertEquals("x", gt.save().getString("future_gene"), "unknown keys survive a save");
    }

    /** Frozen M2-M4 (v2) betta: "plakat" becomes the roundtail with short fins, all other tails get long fins. */
    @Test
    void bettaV2FinformSplitsIntoTailAndFinLength() {
        CompoundTag v2 = snbt("{schema:2,genome:{species:\"bettacraft:betta\",finform:[\"plakat\",\"halfmoon\"],pectoral:[\"normal\",\"dumbo\"],"
                + "body_color:[\"red\",\"red\"],fin_color:[\"red\",\"royal_blue\"],pattern:[\"none\",\"marble\"],pattern_color:[\"black\",\"black\"],"
                + "iridescence:[\"low\",\"high\"],iridescence_color:[\"turquoise\",\"steel\"],future_gene:[\"x\",\"y\"]},sex:\"female\","
                + "stage:\"juvenile\",stage_days:1.5d,generation:3,born:99L,health:{sick:0b},custom:\"keep\"}");
        CompoundTag v3 = Schemas.BETTA.migrate(v2);
        assertEquals(4, Schema.read(v3));
        CompoundTag g = v3.getCompound("genome");
        net.minecraft.nbt.ListTag fin = g.getList("finform", net.minecraft.nbt.Tag.TAG_STRING);
        net.minecraft.nbt.ListTag len = g.getList("fin_length", net.minecraft.nbt.Tag.TAG_STRING);
        assertEquals("round", fin.getString(0));
        assertEquals("halfmoon", fin.getString(1));
        assertEquals("short", len.getString(0), "plakat allele -> short");
        assertEquals("long", len.getString(1), "halfmoon allele -> long");
        assertEquals("x", g.getList("future_gene", net.minecraft.nbt.Tag.TAG_STRING).getString(0), "unknown loci are kept");
        assertEquals("keep", v3.getString("custom"));
        assertEquals(3, v3.getInt("generation"));
        // phenotype unchanged: a plakat carrying halfmoon is still a plakat (short-finned roundtail)
        var ph = org.overengineer.bettacraft.genetics.BettaGenotype.load(g).express();
        assertEquals("round", ph.shape());
        assertEquals("short", ph.finLength());
        // homozygous long-finned fish stay long-finned
        CompoundTag veil = Schemas.BETTA.migrate(snbt("{schema:2,genome:{finform:[\"veil\",\"veil\"]}}"));
        var pv = org.overengineer.bettacraft.genetics.BettaGenotype.load(veil.getCompound("genome")).express();
        assertEquals("veil", pv.shape());
        assertEquals("long", pv.finLength());
        // a v2 genome without finform keeps the defaults (wild-type plakat) and gets nothing added
        CompoundTag none = Schemas.BETTA.migrate(snbt("{schema:2,genome:{body_color:[\"red\",\"red\"]}}"));
        assertFalse(none.getCompound("genome").contains("fin_length"));
        // v3 data is not touched again
        CompoundTag again = Schemas.BETTA.migrate(v3.copy());
        assertEquals(v3, again);
    }

    /** Frozen M4 (v3) controller with a bubble nest: the egg and parent genotypes get the fin-form split too. */
    @Test
    void tankV3NestGenotypesAreMigrated() {
        CompoundTag v3 = snbt("{schema:3,tank:{members:[L;0L],scale:16,water_mb:1000L,nest:{pos:[I;0,1,0],"
                + "father_genome:{finform:[\"plakat\",\"veil\"]},mother_genome:{finform:[\"crowntail\",\"crowntail\"]},"
                + "eggs:[{genome:{finform:[\"plakat\",\"plakat\"],body_color:[\"red\",\"red\"]},female:1b},{genome:{},female:0b}]}}}");
        CompoundTag v4 = Schemas.TANK_BLOCK_ENTITY.migrate(v3);
        assertEquals(10, Schema.read(v4));
        CompoundTag nest = v4.getCompound("tank").getCompound("nest");
        assertEquals("round", nest.getCompound("father_genome").getList("finform", 8).getString(0));
        assertEquals("long", nest.getCompound("father_genome").getList("fin_length", 8).getString(1));
        assertEquals("long", nest.getCompound("mother_genome").getList("fin_length", 8).getString(0));
        CompoundTag egg = nest.getList("eggs", 10).getCompound(0).getCompound("genome");
        assertEquals("short", egg.getList("fin_length", 8).getString(0));
        assertEquals("red", egg.getList("body_color", 8).getString(0));
        assertTrue(nest.getList("eggs", 10).getCompound(0).getBoolean("female"));
        assertFalse(nest.getList("eggs", 10).getCompound(1).getCompound("genome").contains("fin_length"));
        // a tank without nest is untouched apart from the stamp
        CompoundTag plain = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:3,tank:{water_mb:5L}}"));
        assertEquals(5L, plain.getCompound("tank").getLong("water_mb"));
    }

    @Test
    void currentVersionsAreStable() {
        // Bump these deliberately together with a new step + frozen test.
        assertEquals(10, Schemas.TANK_BLOCK_ENTITY.currentVersion());
        assertEquals(4, Schemas.BETTA.currentVersion());
        assertEquals(1, Schemas.TANK_REF.currentVersion());
        assertEquals(1, Schemas.FEEDER.currentVersion());
        assertEquals(4, Schemas.INTERIOR_REGIONS.currentVersion());
        assertEquals(1, Schemas.INTERIOR_FLOOR.currentVersion());
        assertEquals(1, Schemas.INTERIOR_RETURN.currentVersion());
        assertEquals(1, Schemas.EXTERNAL_FILTER.currentVersion());
        assertEquals(1, Schemas.CABINET.currentVersion());
        assertEquals(1, Schemas.GUIDE_BOOK.currentVersion());
        assertEquals(1, Schemas.CULTURE_TUB.currentVersion());
    }

    /** Frozen M8 (v4) controller: the evaporation placeholder goes, water quality and power stay or get defaults. */
    @Test
    void tankV4GetsWaterCareFields() {
        CompoundTag v4 = snbt("{schema:4,glass:\"minecraft:glass\",controller:[I;0,64,0],tank:{members:[L;0L,1L],scale:16,water_mb:2000L,"
                + "region:-1,water_quality:1.0f,evaporation:0.0f,zones:{},power:0L,target_temp:27.0f,water_temp:26.0f,legacy_heaters:[\"\",\"\",\"\",\"\"],"
                + "name:\"M8 tank\",sign_visible:1b,sign_pos:\"top_center\",front:\"south\",locked:0b,inhabitants:[],log:[],custom:\"keep\"}}");
        CompoundTag v5 = Schemas.TANK_BLOCK_ENTITY.migrate(v4);
        assertEquals(10, Schema.read(v5));
        CompoundTag t = v5.getCompound("tank");
        assertFalse(t.contains("evaporation"), "placeholder dropped");
        assertEquals(0.0, t.getDouble("evaporation_carry"));
        assertEquals(1.0F, t.getFloat("water_quality"), "existing tanks start with clean water");
        assertEquals(0L, t.getLong("power"));
        assertEquals(-1L, t.getLong("last_fresh_water"));
        assertEquals(2000L, t.getLong("water_mb"));
        assertEquals("M8 tank", t.getString("name"));
        assertEquals("keep", t.getString("custom"), "unknown keys kept");
        // without the M0 fields (older saves migrated step by step) and with broken values
        CompoundTag bare = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:4,tank:{members:[L;0L],water_mb:5L}}")).getCompound("tank");
        assertEquals(1.0F, bare.getFloat("water_quality"));
        CompoundTag broken = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:4,tank:{water_quality:7.0f,power:-5L}}")).getCompound("tank");
        assertEquals(1.0F, broken.getFloat("water_quality"));
        assertEquals(0L, broken.getLong("power"));
        CompoundTag low = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:4,tank:{water_quality:-1.0f}}")).getCompound("tank");
        assertEquals(0.0F, low.getFloat("water_quality"));
        // a member block (no tank data) is only stamped
        CompoundTag member = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:4,glass:\"minecraft:glass\",controller:[I;0,64,0]}"));
        assertEquals(10, Schema.read(member));
        assertFalse(member.contains("tank"));
    }

    /** Frozen M3 (v2) controller with heaters in the old menu slots: they move to legacy_heaters unchanged. */
    @Test
    void tankV2HeaterSlotsBecomeLegacyHeaters() {
        CompoundTag v2 = snbt("{schema:2,glass:\"minecraft:glass\",controller:[I;0,64,0],tank:{members:[L;0L],scale:16,water_mb:1000L,"
                + "region:-1,target_temp:27.0f,water_temp:26.5f,heaters:[\"bettacraft:heater_large\",\"\",\"othermod:gone_heater\","
                + "\"bettacraft:heater_small\"],name:\"Old tank\",custom:\"keep\"}}");
        CompoundTag v3 = Schemas.TANK_BLOCK_ENTITY.migrate(v2);
        assertEquals(10, Schema.read(v3));
        CompoundTag t = v3.getCompound("tank");
        assertFalse(t.contains("heaters"), "old key is gone");
        net.minecraft.nbt.ListTag l = t.getList("legacy_heaters", net.minecraft.nbt.Tag.TAG_STRING);
        assertEquals(4, l.size(), "all four slots are kept");
        assertEquals("bettacraft:heater_large", l.getString(0));
        assertEquals("", l.getString(1));
        assertEquals("othermod:gone_heater", l.getString(2), "ids of removed mods are kept");
        assertEquals("bettacraft:heater_small", l.getString(3));
        assertEquals("Old tank", t.getString("name"));
        assertEquals("keep", t.getString("custom"));
        assertEquals(26.5F, t.getFloat("water_temp"));
    }

    @Test
    void tankV2WithoutHeatersIsOnlyStamped() {
        CompoundTag v3 = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:2,glass:\"minecraft:glass\",controller:[I;1,2,3]}"));
        assertEquals(10, Schema.read(v3));
        assertFalse(v3.contains("tank"));
        CompoundTag v3b = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:2,tank:{members:[L;0L]}}"));
        assertFalse(v3b.getCompound("tank").contains("legacy_heaters"));
    }

    @Test
    void newEquipmentChainsStartAtV1() {
        CompoundTag f = Schemas.EXTERNAL_FILTER.migrate(snbt("{heaters:{Size:2,Items:[]}}"));
        assertEquals(1, Schema.read(f));
        assertTrue(f.contains("heaters"));
        CompoundTag c = Schemas.CABINET.migrate(snbt("{wood:\"biomesoplenty:fir_planks\"}"));
        assertEquals(1, Schema.read(c));
        assertEquals("biomesoplenty:fir_planks", c.getString("wood"), "modded wood ids are kept");
    }

    /** Frozen M2 (v1) interior regions SavedData: regions without equipment get a flagged empty one. */
    @Test
    void interiorRegionsV1ToV2AddsEquipmentRescan() {
        CompoundTag v1 = snbt("{schema:1,next_id:3,regions:[{id:1,size:[I;16,16,16],scale:16,tank_dim:\"minecraft:overworld\","
                + "tank_min:[I;0,64,0],built:1b,custom:\"keep\"},{id:2,size:[I;32,16,16],scale:16,tank_dim:\"minecraft:overworld\","
                + "tank_min:[I;5,64,0],built:0b,equipment:{substrate:4}}]}");
        CompoundTag out = Schemas.INTERIOR_REGIONS.migrate(v1);
        assertEquals(Schemas.INTERIOR_REGIONS.currentVersion(), Schema.read(out));
        CompoundTag r1 = out.getList("regions", 10).getCompound(0);
        assertTrue(r1.getCompound("equipment").getBoolean("rescan"), "old region is rescanned");
        assertEquals("keep", r1.getString("custom"), "unknown keys kept");
        assertEquals(16, r1.getInt("scale"));
        CompoundTag r2 = out.getList("regions", 10).getCompound(1);
        assertFalse(r2.getCompound("equipment").getBoolean("rescan"), "existing equipment untouched");
        assertEquals(4, r2.getCompound("equipment").getInt("substrate"));
        assertEquals(3, out.getInt("next_id"));
    }

    /** Frozen M12 (v3) interior regions SavedData: regions get an empty aquarium plant index (M13), the rest stays. */
    @Test
    void interiorRegionsV3ToV4AddsEmptyPlantIndex() {
        CompoundTag v3 = snbt("{schema:3,next_id:2,regions:[{id:1,size:[I;32,16,16],scale:16,tank_dim:\"minecraft:overworld\","
                + "tank_min:[I;0,64,0],built:1b,equipment:{substrate:2,floating:[L;5L]},cells:[B;3B],custom:\"keep\"}]}");
        CompoundTag out = Schemas.INTERIOR_REGIONS.migrate(v3);
        assertEquals(4, Schema.read(out));
        CompoundTag r1 = out.getList("regions", 10).getCompound(0);
        assertTrue(r1.contains("plants", 10), "plant index added");
        assertEquals(0, r1.getCompound("plants").getList("list", 10).size(), "no aquarium plants in old regions");
        assertEquals("keep", r1.getString("custom"));
        assertEquals(3, r1.getByteArray("cells")[0]);
        var region = org.overengineer.bettacraft.tank.interior.InteriorRegion.load(r1);
        assertTrue(region.plants().isEmpty());
        assertEquals(2, region.equipment().substrateCount());
        // a region with plants survives save / load
        region.plants().add(org.overengineer.bettacraft.plants.PlantSpecies.ROTALA, 3, 0, 4, 99L, 5).room = 14;
        var again = org.overengineer.bettacraft.tank.interior.InteriorRegion.load(region.save());
        assertEquals(1, again.plants().size());
        var p = again.plants().all().getFirst();
        assertEquals(org.overengineer.bettacraft.plants.PlantSpecies.ROTALA, p.species);
        assertEquals(5, p.stage);
        assertEquals(14, p.room);
        assertEquals(99L, p.seed);
    }

    /** Frozen M9 (v2) interior regions SavedData: every old region belongs to a cuboid tank and gets all shape cells. */
    @Test
    void interiorRegionsV2ToV3AddsFullShapeCells() {
        CompoundTag v2 = snbt("{schema:2,next_id:3,regions:[{id:1,size:[I;32,16,48],scale:16,tank_dim:\"minecraft:overworld\","
                + "tank_min:[I;0,64,0],built:1b,equipment:{substrate:2},custom:\"keep\"},{id:2,size:[I;24,8,8],scale:8,"
                + "tank_dim:\"minecraft:the_nether\",tank_min:[I;5,64,0],built:0b,equipment:{}}]}");
        CompoundTag out = Schemas.INTERIOR_REGIONS.migrate(v2);
        assertEquals(Schemas.INTERIOR_REGIONS.currentVersion(), Schema.read(out));
        CompoundTag r1 = out.getList("regions", 10).getCompound(0);
        byte[] cells = r1.getByteArray("cells");
        assertEquals(1, cells.length, "2x1x3 = 6 cells in one byte");
        assertEquals(0b111111, cells[0] & 0xFF, "all cells of the box are part of the tank");
        assertEquals("keep", r1.getString("custom"));
        assertEquals(2, r1.getCompound("equipment").getInt("substrate"));
        CompoundTag r2 = out.getList("regions", 10).getCompound(1);
        assertEquals(0b111, r2.getByteArray("cells")[0] & 0xFF, "scale 8: 3x1x1 cells");
        // regions loaded from the migrated data are full boxes
        var region = org.overengineer.bettacraft.tank.interior.InteriorRegion.load(r1);
        assertTrue(region.fullBox());
        assertTrue(region.hasCell(1, 0, 2));
        assertFalse(region.hasCell(2, 0, 0));
    }

    @Test
    void interiorFloorStartsAtV1() {
        assertEquals(1, Schemas.INTERIOR_FLOOR.currentVersion());
        CompoundTag out = Schemas.INTERIOR_FLOOR.migrate(snbt("{paid:1b}"));
        assertEquals(1, Schema.read(out));
        assertTrue(out.getBoolean("paid"));
    }

    /** Frozen M10 (v5) controller and member of a free-shape tank: both become textured, everything else stays. */
    @Test
    void tankV5BecomesTextured() {
        CompoundTag v5 = snbt("{schema:5,glass:\"minecraft:light_blue_stained_glass\",controller:[I;0,64,0],tank:{members:[L;0L,1L,2L],scale:16,"
                + "water_mb:3000L,region:-1,water_quality:0.9f,evaporation_carry:0.0d,power:0L,last_fresh_water:-1L,target_temp:27.0f,"
                + "water_temp:26.0f,name:\"T\",sign_visible:1b,sign_pos:\"top_center\",front:\"north\",locked:0b,inhabitants:[],log:[],custom:\"keep\"}}");
        CompoundTag v6 = Schemas.TANK_BLOCK_ENTITY.migrate(v5);
        assertEquals(10, Schema.read(v6));
        assertEquals("textured", v6.getString("style"));
        assertEquals("minecraft:light_blue_stained_glass", v6.getString("glass"), "glass id kept");
        assertEquals(3000L, v6.getCompound("tank").getLong("water_mb"));
        assertEquals("keep", v6.getCompound("tank").getString("custom"));
        CompoundTag member = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:5,glass:\"othermod:gone_glass\",controller:[I;0,64,0]}"));
        assertEquals("textured", member.getString("style"));
        assertEquals("othermod:gone_glass", member.getString("glass"), "ids of removed mods are kept");
        assertFalse(member.contains("tank"));
        // an explicit style (written by M11) is never touched
        CompoundTag clean = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:5,glass:\"minecraft:glass\",style:\"clean\"}"));
        assertEquals("clean", clean.getString("style"));
        assertEquals(org.overengineer.bettacraft.tank.TankStyle.TEXTURED, org.overengineer.bettacraft.tank.TankStyle.byId(""));
        assertEquals(org.overengineer.bettacraft.tank.TankStyle.CLEAN, org.overengineer.bettacraft.tank.TankStyle.byId("clean"));
        assertEquals(org.overengineer.bettacraft.tank.TankStyle.TEXTURED, org.overengineer.bettacraft.tank.TankStyle.byId("futuristic"));
    }

    /** Frozen M11 (v6) controller and member: the controller gets an empty list of floating plants, the rest stays. */
    @Test
    void tankV6GetsFloatingPlants() {
        CompoundTag v6 = snbt("{schema:6,glass:\"minecraft:glass\",style:\"clean\",controller:[I;0,64,0],tank:{members:[L;0L,1L],scale:16,"
                + "water_mb:2000L,region:-1,water_quality:0.8f,evaporation_carry:0.0d,power:0L,last_fresh_water:-1L,target_temp:27.0f,"
                + "water_temp:26.0f,name:\"T\",sign_visible:1b,sign_pos:\"top_center\",front:\"north\",locked:0b,inhabitants:[],log:[],"
                + "nest:{phase:\"building\"},custom:\"keep\"}}");
        CompoundTag v7 = Schemas.TANK_BLOCK_ENTITY.migrate(v6);
        assertEquals(10, Schema.read(v7));
        CompoundTag t = v7.getCompound("tank");
        assertTrue(t.contains("plants", 9), "plants list added");
        assertEquals(0, t.getList("plants", 10).size());
        assertEquals(1, t.getInt("next_plant_id"));
        assertEquals("keep", t.getString("custom"));
        assertEquals(2000L, t.getLong("water_mb"));
        assertEquals("clean", v7.getString("style"));
        assertTrue(t.contains("nest"));
        // the tank data loads with no plants
        assertTrue(org.overengineer.bettacraft.tank.TankData.load(t).plants().isEmpty());
        // a member has no tank data: only stamped
        CompoundTag member = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:6,glass:\"minecraft:glass\",style:\"textured\",controller:[I;0,64,0]}"));
        assertEquals(10, Schema.read(member));
        assertFalse(member.contains("tank"));
        // plants already there (written by M12) are kept
        CompoundTag withPlants = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:6,tank:{plants:[{id:3,type:\"salvinia\",x:0.5d,z:0.5d}],next_plant_id:4}}"));
        assertEquals(1, withPlants.getCompound("tank").getList("plants", 10).size());
        assertEquals(4, withPlants.getCompound("tank").getInt("next_plant_id"));
    }

    /** Frozen M13 (v7) controller: gets empty lists of small animals, clutches, moults and algae; everything else stays. */
    @Test
    void tankV7GetsCrittersAndAlgae() {
        CompoundTag v7 = snbt("{schema:7,glass:\"minecraft:glass\",style:\"textured\",controller:[I;0,64,0],tank:{members:[L;0L,1L],scale:16,"
                + "water_mb:2000L,region:3,water_quality:0.7f,evaporation_carry:0.0d,power:0L,last_fresh_water:-1L,target_temp:26.0f,"
                + "water_temp:26.0f,name:\"Shrimp\",sign_visible:1b,sign_pos:\"top_center\",front:\"north\",locked:0b,inhabitants:[],log:[],"
                + "plants:[{id:1,type:\"duckweed\",x:0.5d,z:0.5d,size:0.5f,rot:0.0f}],next_plant_id:2,custom:\"keep\"}}");
        CompoundTag v8 = Schemas.TANK_BLOCK_ENTITY.migrate(v7);
        assertEquals(10, Schema.read(v8));
        CompoundTag t = v8.getCompound("tank");
        assertTrue(t.contains("critters", 9), "critter list added");
        assertEquals(0, t.getList("critters", 10).size());
        assertEquals(0, t.getList("critter_objects", 10).size());
        assertEquals(0, t.getList("algae", 10).size());
        assertEquals(1, t.getInt("next_critter_id"));
        assertEquals("keep", t.getString("custom"));
        assertEquals(1, t.getList("plants", 10).size(), "floating plants kept");
        org.overengineer.bettacraft.tank.TankData d = org.overengineer.bettacraft.tank.TankData.load(t);
        assertTrue(d.critters().isEmpty());
        assertTrue(d.algae().isEmpty());
        assertEquals(1, d.plants().size());
        assertEquals("Shrimp", d.name());
        // a member is only stamped
        CompoundTag member = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:7,glass:\"minecraft:glass\",controller:[I;0,64,0]}"));
        assertEquals(10, Schema.read(member));
        assertFalse(member.contains("tank"));
        // critters written by M14 are kept
        CompoundTag with = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:7,tank:{members:[L;0L],critters:[{id:5,species:\"ramshorn\",x:0.5d,y:0.1d,z:0.5d,"
                + "genes:{a:1,b:1,grade:0.5f},age:9.0d}],next_critter_id:6}}"));
        assertEquals(1, with.getCompound("tank").getList("critters", 10).size());
        var loaded = org.overengineer.bettacraft.tank.TankData.load(with.getCompound("tank"));
        assertEquals(1, loaded.critters().size());
        assertEquals("pink", loaded.critters().getFirst().genes.variety(org.overengineer.bettacraft.critters.CritterSpecies.RAMSHORN));
    }

    /** Frozen M18 (v8) controller: gets an empty list of tankmate clutches, no breeding box, no cool water change; the rest stays. */
    @Test
    void tankV8GetsTankmateClutches() {
        CompoundTag v8 = snbt("{schema:8,glass:\"minecraft:glass\",style:\"textured\",controller:[I;0,64,0],tank:{members:[L;0L,1L],scale:16,"
                + "water_mb:2000L,region:3,water_quality:0.9f,target_temp:26.0f,water_temp:26.0f,name:\"Neons\",inhabitants:[],log:[],"
                + "critters:[],critter_objects:[],next_critter_id:1,critter_food:0.0d,algae:[],custom:\"keep\"}}");
        CompoundTag v9 = Schemas.TANK_BLOCK_ENTITY.migrate(v8);
        assertEquals(10, Schema.read(v9));
        CompoundTag t = v9.getCompound("tank");
        assertTrue(t.contains("clutches", 9), "clutch list added");
        assertEquals(0, t.getList("clutches", 10).size());
        assertEquals(1, t.getInt("next_clutch_id"));
        assertFalse(t.getBoolean("breeding_box"));
        assertEquals(-1L, t.getLong("last_cool_change"));
        assertEquals("keep", t.getString("custom"));
        org.overengineer.bettacraft.tank.TankData d = org.overengineer.bettacraft.tank.TankData.load(t);
        assertTrue(d.clutches().isEmpty());
        assertFalse(d.breedingBox());
        assertEquals("Neons", d.name());
        // clutches written by M19 are kept (unknown species are dropped on load, not by the migration)
        CompoundTag with = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:8,tank:{members:[L;0L],clutches:[{id:2,species:\"panda_cory\",kind:\"glass\","
                + "x:0.5d,y:0.2d,z:0.06d,face:0,count:12,age:0.4d,mother:{a:\"wild\",b:\"wild\"},father:{a:\"wild\",b:\"wild\"}},"
                + "{id:3,species:\"unknown_fish\",kind:\"glass\",x:0.5d,y:0.2d,z:0.06d,count:3}],next_clutch_id:4,breeding_box:1b}}"));
        assertEquals(2, with.getCompound("tank").getList("clutches", 10).size());
        org.overengineer.bettacraft.tank.TankData dw = org.overengineer.bettacraft.tank.TankData.load(with.getCompound("tank"));
        assertEquals(1, dw.clutches().size());
        assertEquals(12, dw.clutches().getFirst().count);
        assertTrue(dw.breedingBox());
        assertEquals(4, dw.nextClutchId());
        // a member is only stamped
        CompoundTag member = Schemas.TANK_BLOCK_ENTITY.migrate(snbt("{schema:8,glass:\"minecraft:glass\",controller:[I;0,64,0]}"));
        assertEquals(10, Schema.read(member));
        assertFalse(member.contains("tank"));
    }

    /** Frozen M19 (v9) controller: becomes a freshwater tank (salinity 0, calcium 0); everything else stays. */
    @Test
    void tankV9BecomesFreshwater() {
        CompoundTag v9 = snbt("{schema:9,glass:\"minecraft:glass\",style:\"textured\",controller:[I;0,64,0],tank:{members:[L;0L,1L],scale:16,"
                + "water_mb:2000L,region:3,water_quality:0.8f,target_temp:26.0f,water_temp:26.0f,name:\"Neons\",inhabitants:[],log:[],"
                + "critters:[],critter_objects:[],next_critter_id:1,critter_food:0.0d,algae:[],clutches:[],next_clutch_id:1,breeding_box:0b,"
                + "last_cool_change:-1L,custom:\"keep\"}}");
        CompoundTag v10 = Schemas.TANK_BLOCK_ENTITY.migrate(v9);
        assertEquals(10, Schema.read(v10));
        CompoundTag t = v10.getCompound("tank");
        assertEquals(0.0F, t.getFloat("salinity"));
        assertEquals(0.0F, t.getFloat("calcium"));
        assertEquals("keep", t.getString("custom"));
        org.overengineer.bettacraft.tank.TankData d = org.overengineer.bettacraft.tank.TankData.load(t);
        assertEquals(0.0F, d.salinity());
        assertFalse(d.marine());
        assertEquals(0.8F, d.waterQuality(), 1e-6);
        assertEquals("Neons", d.name());
        // a salt water tank written by M20 keeps its salt; saving and loading round-trips
        CompoundTag salty = t.copy();
        salty.putFloat("salinity", 34.5F);
        salty.putFloat("calcium", 400F);
        org.overengineer.bettacraft.tank.TankData back = org.overengineer.bettacraft.tank.TankData.load(salty);
        assertEquals(34.5F, back.salinity(), 1e-4);
        assertEquals(400F, back.calcium(), 1e-3);
        assertTrue(back.marine());
        assertEquals(34.5F, org.overengineer.bettacraft.tank.TankData.loadClientView(back.saveClientView()).salinity(), 1e-4);
        // corrupt values are clamped
        CompoundTag bad = salty.copy();
        bad.putFloat("salinity", Float.NaN);
        bad.putFloat("calcium", -5F);
        org.overengineer.bettacraft.tank.TankData b = org.overengineer.bettacraft.tank.TankData.load(bad);
        assertEquals(0F, b.salinity());
        assertEquals(0F, b.calcium());
    }
}
