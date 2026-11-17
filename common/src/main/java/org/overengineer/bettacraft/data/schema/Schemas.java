package org.overengineer.bettacraft.data.schema;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * All migration chains of Bettacraft. Every persisted object type has exactly one chain here.
 * <p>
 * Rule: when the stored format of a type changes, add a new {@code step(n, ...)} with a unit test using a frozen
 * example NBT of version {@code n} (see {@code SchemaMigrationTest}). Never edit an existing step.
 */
public final class Schemas {
    private Schemas() {}

    /** Tank block entity (all tank blocks; the controller additionally carries the tank data). */
    public static final MigrationChain TANK_BLOCK_ENTITY = MigrationChain.builder("tank_block_entity")
            .step(0, Schemas::tankV0toV1)
            .step(1, Schemas::tankV1toV2)
            .step(2, Schemas::tankV2toV3)
            .step(3, Schemas::tankV3toV4)
            .step(4, Schemas::tankV4toV5)
            .step(5, Schemas::tankV5toV6)
            .step(6, Schemas::tankV6toV7)
            .step(7, Schemas::tankV7toV8)
            .step(8, Schemas::tankV8toV9)
            .step(9, Schemas::tankV9toV10)
            .step(10, Schemas::tankV10toV11)
            .build();

    /** Betta data (entity NBT key "betta", item component bettacraft:fish_data, bucket_entity_data key "betta"). */
    public static final MigrationChain BETTA = MigrationChain.builder("betta")
            .step(0, data -> data) // v0 = written before versioning; identical layout
            .step(1, Schemas::bettaV1toV2)
            .step(2, Schemas::bettaV2toV3)
            .step(3, Schemas::bettaV3toV4)
            .build();

    /** Loci of the M2 genotype (and the M1 key each one was stored under). */
    private static final String[][] BETTA_LOCI = {{"finform", "shape"}, {"pectoral", "pectoral"}, {"body_color", "body_color"},
            {"fin_color", "fin_color"}, {"pattern", "pattern"}, {"pattern_color", "pattern_color"}, {"iridescence", "iridescence"},
            {"iridescence_color", "iridescence_color"}};

    /** Entity attachment bettacraft:tank_ref (controller position + tank id). */
    public static final MigrationChain TANK_REF = MigrationChain.builder("tank_ref")
            .step(0, data -> data)
            .build();

    /** Feeder block entity. */
    public static final MigrationChain FEEDER = MigrationChain.builder("feeder")
            .step(0, data -> data)
            .build();

    /** External filter block entity (M4): two heater slots. */
    public static final MigrationChain EXTERNAL_FILTER = MigrationChain.builder("external_filter")
            .step(0, data -> data)
            .step(1, Schemas::springV1toV2) // M37: the spring of the crank
            .build();

    /**
     * Pump head of the multiblock filter (M24; versioned from M40). v0 -> v1 (M40, D2): a pump head from before M40 is
     * marked {@code retrofit}: its filter now needs guards on every connection in the tanks, and its warnings say so.
     */
    public static final MigrationChain FILTER_PUMP_HEAD = MigrationChain.builder("filter_pump_head")
            .step(0, Schemas::pumpHeadV0toV1)
            .build();

    /** Larva tub and artemia hatchery (M9): water, slots and progress. */
    public static final MigrationChain CULTURE_TUB = MigrationChain.builder("culture_tub")
            .step(0, data -> data)
            .build();

    /** Cabinet block entity (M4): planks, filter slot and storage. */
    public static final MigrationChain CABINET = MigrationChain.builder("cabinet")
            .step(0, data -> data)
            .step(1, Schemas::springV1toV2) // M37: the spring of the crank
            .step(2, Schemas::cabinetDriveV2toV3) // M41: the side that takes a Create shaft
            .build();

    /** Tank interior region SavedData (one per server, in the overworld data folder). */
    public static final MigrationChain INTERIOR_REGIONS = MigrationChain.builder("interior_regions")
            .step(0, data -> data) // v0 = written before versioning; identical layout
            .step(1, Schemas::regionsV1toV2)
            .step(2, Schemas::regionsV2toV3)
            .step(3, Schemas::regionsV3toV4)
            .build();

    /** Substrate block entity of an interior floor block (M3). */
    public static final MigrationChain INTERIOR_FLOOR = MigrationChain.builder("interior_floor")
            .step(0, data -> data)
            .build();

    /** Player attachment holding the position to return to when leaving an interior. */
    public static final MigrationChain INTERIOR_RETURN = MigrationChain.builder("interior_return")
            .step(0, data -> data)
            .build();

    /** Entity attachment bettacraft:glass_mob (M31 part d): a mob that lives (or lived) shrunk in a tank. */
    public static final MigrationChain GLASS_MOB = MigrationChain.builder("glass_mob")
            .step(0, data -> data)
            .build();

    /** Player attachment bettacraft:guide_book (M6): whether the handbook was given. */
    public static final MigrationChain GUIDE_BOOK = MigrationChain.builder("guide_book")
            .step(0, data -> data)
            .build();

    /**
     * Betta v1 -> v2 (M2): the genome stored one value per locus (the phenotype). Every value becomes a homozygous allele
     * pair ({@code body_color:"red"} -> {@code body_color:["red","red"]}); {@code shape} is renamed to {@code finform}.
     * Missing loci stay missing (defaults apply on load), unknown keys and unknown ids are kept.
     */
    static CompoundTag bettaV1toV2(CompoundTag data) {
        if (!NbtCompat.has(data, "genome", Tag.TAG_COMPOUND)) return data;
        CompoundTag g = data.getCompoundOrEmpty("genome");
        for (String[] locus : BETTA_LOCI) {
            String now = locus[0], old = locus[1];
            if (NbtCompat.has(g, old, Tag.TAG_STRING)) {
                String v = g.getStringOr(old, "");
                g.remove(old);
                net.minecraft.nbt.ListTag pair = new net.minecraft.nbt.ListTag();
                pair.add(net.minecraft.nbt.StringTag.valueOf(v));
                pair.add(net.minecraft.nbt.StringTag.valueOf(v));
                g.put(now, pair);
            }
        }
        return data;
    }

    /**
     * Betta v3 -> v4 (M22.3): the hidden genes need the magnifier now; every betta of an older world counts as analysed
     * (nobody loses what the menu showed before). New bettas start without the flag (= not analysed).
     */
    static CompoundTag bettaV3toV4(CompoundTag data) {
        if (!data.contains("analysed")) data.putBoolean("analysed", true);
        return data;
    }

    /**
     * Betta v2 -> v3 (M5): the fin form is split into the tail shape ({@code finform}) and the new {@code fin_length}
     * locus (short = plakat types, dominant). Per allele: {@code plakat} becomes the tail {@code round} with a
     * {@code short} fin-length allele, every other tail gets a {@code long} one. Allele order is kept, so carriers stay
     * carriers and every fish keeps its phenotype ((plakat, veil) -> finform (round, veil), fin_length (short, long)).
     * A genome without finform gets no fin_length (defaults apply on load); an existing fin_length is left alone.
     */
    static CompoundTag bettaV2toV3(CompoundTag data) {
        if (NbtCompat.has(data, "genome", Tag.TAG_COMPOUND)) genomeV2toV3(data.getCompoundOrEmpty("genome"));
        return data;
    }

    /**
     * Tank v3 -> v4 (M5): the genotypes kept in the bubble nest (eggs, larvae and both parents) get the same fin-form
     * split as the betta data ({@link #bettaV2toV3}).
     */
    static CompoundTag tankV3toV4(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "nest", Tag.TAG_COMPOUND)) return data;
        CompoundTag nest = tank.getCompoundOrEmpty("nest");
        for (String k : new String[]{"father_genome", "mother_genome"}) {
            if (NbtCompat.has(nest, k, Tag.TAG_COMPOUND)) genomeV2toV3(nest.getCompoundOrEmpty(k));
        }
        net.minecraft.nbt.ListTag eggs = nest.getListOrEmpty("eggs");
        for (int i = 0; i < eggs.size(); i++) {
            CompoundTag e = eggs.getCompoundOrEmpty(i);
            if (NbtCompat.has(e, "genome", Tag.TAG_COMPOUND)) genomeV2toV3(e.getCompoundOrEmpty("genome"));
        }
        return data;
    }

    /**
     * Tank v4 -> v5 (M9): water quality, evaporation and power get logic. The M0 placeholder {@code evaporation} (a float
     * that was always 0) is dropped in favour of {@code evaporation_carry} (evaporated mB not yet taken from the water, starts
     * at 0); a missing or invalid {@code water_quality} becomes 1.0 (clean, so existing tanks do not start dirty), a
     * negative {@code power} buffer 0, and {@code last_fresh_water} -1 (never). Everything else is kept.
     */
    static CompoundTag tankV4toV5(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        tank.remove("evaporation");
        if (!NbtCompat.has(tank, "evaporation_carry", NbtCompat.ANY_NUMERIC)) tank.putDouble("evaporation_carry", 0.0);
        float q = NbtCompat.has(tank, "water_quality", NbtCompat.ANY_NUMERIC) ? tank.getFloatOr("water_quality", 0f) : 1.0F;
        if (Float.isNaN(q) || q < 0 || q > 1) q = Float.isNaN(q) || q > 1 ? 1.0F : 0.0F;
        tank.putFloat("water_quality", q);
        if (tank.getLongOr("power", 0L) < 0) tank.putLong("power", 0L);
        if (!NbtCompat.has(tank, "last_fresh_water", NbtCompat.ANY_NUMERIC)) tank.putLong("last_fresh_water", -1L);
        return data;
    }

    /**
     * M11: tank variants. Every block written before M11 (controller or member) shows the texture of its glass, so it
     * becomes {@code style: "textured"}; the glass id stays as it is (also ids of removed mods).
     */
    static CompoundTag tankV5toV6(CompoundTag data) {
        if (!NbtCompat.has(data, "style", Tag.TAG_STRING)) data.putString("style", "textured");
        return data;
    }

    /**
     * M12: floating plants on the water surface. A controller written before M12 gets an empty {@code plants} list and
     * {@code next_plant_id} 1; members (no tank data) are only stamped. Lily pads or Fintastic duckweed placed in the
     * interior stay blocks of the interior and are not converted.
     */
    static CompoundTag tankV6toV7(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "plants", Tag.TAG_LIST)) tank.put("plants", new net.minecraft.nbt.ListTag());
        if (!NbtCompat.has(tank, "next_plant_id", NbtCompat.ANY_NUMERIC)) tank.putInt("next_plant_id", 1);
        return data;
    }

    /**
     * M14: the controller gets empty lists of small animals (shrimp, crayfish, snails), of their clutches and moults, an
     * empty food pool and no algae.
     */
    static CompoundTag tankV7toV8(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "critters", Tag.TAG_LIST)) tank.put("critters", new net.minecraft.nbt.ListTag());
        if (!NbtCompat.has(tank, "critter_objects", Tag.TAG_LIST)) tank.put("critter_objects", new net.minecraft.nbt.ListTag());
        if (!NbtCompat.has(tank, "next_critter_id", NbtCompat.ANY_NUMERIC)) tank.putInt("next_critter_id", 1);
        if (!NbtCompat.has(tank, "critter_food", NbtCompat.ANY_NUMERIC)) tank.putDouble("critter_food", 0.0);
        if (!NbtCompat.has(tank, "algae", Tag.TAG_LIST)) tank.put("algae", new net.minecraft.nbt.ListTag());
        return data;
    }

    /** M19: tankmate clutches, the breeding box and the water change bookkeeping (empty / none). */
    static CompoundTag tankV8toV9(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "clutches", Tag.TAG_LIST)) tank.put("clutches", new net.minecraft.nbt.ListTag());
        if (!NbtCompat.has(tank, "next_clutch_id", NbtCompat.ANY_NUMERIC)) tank.putInt("next_clutch_id", 1);
        if (!NbtCompat.has(tank, "breeding_box", NbtCompat.ANY_NUMERIC)) tank.putBoolean("breeding_box", false);
        if (!NbtCompat.has(tank, "last_cool_change", NbtCompat.ANY_NUMERIC)) tank.putLong("last_cool_change", -1L);
        return data;
    }

    /** M20: salt water: every tank of an older world is a freshwater tank (salinity 0, no calcium). */
    static CompoundTag tankV9toV10(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "salinity", NbtCompat.ANY_NUMERIC)) tank.putFloat("salinity", 0.0F);
        if (!NbtCompat.has(tank, "calcium", NbtCompat.ANY_NUMERIC)) tank.putFloat("calcium", 0.0F);
        return data;
    }

    /** M37: liquid fertilizer: every tank of an older world has none in its water. */
    static CompoundTag tankV10toV11(CompoundTag data) {
        if (!NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) return data;
        CompoundTag tank = data.getCompoundOrEmpty("tank");
        if (!NbtCompat.has(tank, "fertilizer", NbtCompat.ANY_NUMERIC)) tank.putFloat("fertilizer", 0.0F);
        return data;
    }

    /** M37: external filters and cabinets of an older world have an unwound spring. */
    static CompoundTag pumpHeadV0toV1(CompoundTag data) {
        if (!NbtCompat.has(data, "retrofit", NbtCompat.ANY_NUMERIC)) data.putBoolean("retrofit", true);
        return data;
    }

    /** M41: cabinets from before M41 take no Create shaft ({@code drive: none}). */
    static CompoundTag cabinetDriveV2toV3(CompoundTag data) {
        if (!NbtCompat.has(data, "drive", Tag.TAG_STRING)) data.putString("drive", "none");
        return data;
    }

    static CompoundTag springV1toV2(CompoundTag data) {
        if (!NbtCompat.has(data, "spring", NbtCompat.ANY_NUMERIC)) data.putLong("spring", 0L);
        return data;
    }

    /** M5 fin-form split of one genotype compound (in place). */
    static void genomeV2toV3(CompoundTag g) {
        if (!NbtCompat.has(g, "finform", Tag.TAG_LIST)) return;
        net.minecraft.nbt.ListTag fin = g.getListOrEmpty("finform");
        net.minecraft.nbt.ListTag tails = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.ListTag lengths = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < fin.size(); i++) {
            String a = fin.getStringOr(i, "");
            boolean plakat = "plakat".equals(a);
            tails.add(net.minecraft.nbt.StringTag.valueOf(plakat ? "round" : a));
            lengths.add(net.minecraft.nbt.StringTag.valueOf(plakat ? "short" : "long"));
        }
        g.put("finform", tails);
        if (!g.contains("fin_length")) g.put("fin_length", lengths);
    }

    /**
     * Interior regions v1 -> v2 (M3): regions gain an {@code equipment} compound (heaters, filters, floating plants,
     * substrate count). Regions written before M3 get an empty one flagged {@code rescan}, so the equipment already
     * standing in them is counted from the world the first time it is needed. Everything else is kept.
     */
    static CompoundTag regionsV1toV2(CompoundTag data) {
        net.minecraft.nbt.ListTag list = data.getListOrEmpty("regions");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompoundOrEmpty(i);
            if (!NbtCompat.has(r, "equipment", Tag.TAG_COMPOUND)) {
                CompoundTag eq = new CompoundTag();
                eq.putBoolean("rescan", true);
                r.put("equipment", eq);
            }
        }
        return data;
    }

    /**
     * Interior regions v2 -> v3 (M10): regions store the tank's shape as {@code cells}, a bit set over the grid of tank
     * blocks of the bounding box ({@code size / scale} per axis, index {@code (y * cz + z) * cx + x}). Every region
     * written before M10 belongs to a cuboid tank, so it gets all cells set. Everything else is kept.
     */
    static CompoundTag regionsV2toV3(CompoundTag data) {
        net.minecraft.nbt.ListTag list = data.getListOrEmpty("regions");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompoundOrEmpty(i);
            if (NbtCompat.has(r, "cells", Tag.TAG_BYTE_ARRAY)) continue;
            int[] size = r.getIntArray("size").orElse(new int[0]);
            int scale = Math.max(1, r.getIntOr("scale", 0));
            int cx = size.length == 3 ? Math.max(1, size[0] / scale) : 1;
            int cy = size.length == 3 ? Math.max(1, size[1] / scale) : 1;
            int cz = size.length == 3 ? Math.max(1, size[2] / scale) : 1;
            r.putByteArray("cells", org.overengineer.bettacraft.tank.TankShape.fullBox(cx, cy, cz));
        }
        return data;
    }

    /**
     * Interior regions v3 -> v4 (M13): regions get a {@code plants} index (aquarium plants: species, root, stage, seed,
     * growth progress). Older regions have no aquarium plants, so they get an empty index. Everything else is kept.
     */
    static CompoundTag regionsV3toV4(CompoundTag data) {
        net.minecraft.nbt.ListTag list = data.getListOrEmpty("regions");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompoundOrEmpty(i);
            if (NbtCompat.has(r, "plants", Tag.TAG_COMPOUND)) continue;
            CompoundTag plants = new CompoundTag();
            plants.put("list", new net.minecraft.nbt.ListTag());
            plants.putInt("next_id", 1);
            r.put("plants", plants);
        }
        return data;
    }

    /**
     * v2 -> v3 (M4): the four heater slots of the tank menu (M1-M3) are given up; heaters belong into the external
     * filter now. Their contents move to {@code legacy_heaters} unchanged (ids of removed mods included), so nothing
     * is lost: the tank moves them into an attached filter by itself, and until then they keep heating and can be
     * taken out in the tank menu.
     */
    static CompoundTag tankV2toV3(CompoundTag data) {
        if (NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) {
            CompoundTag tank = data.getCompoundOrEmpty("tank");
            if (NbtCompat.has(tank, "heaters", Tag.TAG_LIST)) {
                net.minecraft.nbt.ListTag old = tank.getListOrEmpty("heaters");
                tank.remove("heaters");
                if (!NbtCompat.has(tank, "legacy_heaters", Tag.TAG_LIST)) tank.put("legacy_heaters", old);
            }
        }
        return data;
    }

    /**
     * v0 -> v1: tank data written without schema. A controller (has "members") without a fixed interior scale
     * gets the historical default 16, and a missing water volume becomes 0 mB. Everything else is kept.
     */
    static CompoundTag tankV0toV1(CompoundTag data) {
        if (NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) {
            CompoundTag tank = data.getCompoundOrEmpty("tank");
            if (!NbtCompat.has(tank, "scale", NbtCompat.ANY_NUMERIC)) {
                tank.putInt("scale", 16);
            }
            if (!NbtCompat.has(tank, "water_mb", NbtCompat.ANY_NUMERIC)) {
                tank.putLong("water_mb", 0L);
            }
        }
        return data;
    }

    /**
     * v1 -> v2 (M1): tanks gain care data (temperature, equipment, sign, lock, inhabitants, log). Missing values get
     * their defaults: target 27 C, water at the target temperature, sign visible at top centre, front south, unlocked.
     */
    static CompoundTag tankV1toV2(CompoundTag data) {
        if (NbtCompat.has(data, "tank", Tag.TAG_COMPOUND)) {
            CompoundTag tank = data.getCompoundOrEmpty("tank");
            if (!NbtCompat.has(tank, "target_temp", NbtCompat.ANY_NUMERIC)) tank.putFloat("target_temp", 27.0F);
            if (!NbtCompat.has(tank, "water_temp", NbtCompat.ANY_NUMERIC)) tank.putFloat("water_temp", tank.getFloatOr("target_temp", 0f));
            if (!tank.contains("sign_visible")) tank.putBoolean("sign_visible", true);
            if (!NbtCompat.has(tank, "sign_pos", Tag.TAG_STRING)) tank.putString("sign_pos", "top_center");
            if (!NbtCompat.has(tank, "front", Tag.TAG_STRING)) tank.putString("front", "south");
        }
        return data;
    }
}
