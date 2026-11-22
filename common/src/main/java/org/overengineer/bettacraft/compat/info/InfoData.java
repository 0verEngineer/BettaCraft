package org.overengineer.bettacraft.compat.info;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankPower;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * Server side of the info-mod integrations (Jade, The One Probe, WTHIT): collects what the overlays show into a plain
 * {@link CompoundTag}. Uses no class of any info mod, so every plugin shares it; {@link InfoLines} turns the tag into
 * text lines. Jade and WTHIT send the tag to the client, The One Probe uses it directly on the server.
 */
public final class InfoData {
    /** Key under which the plugins store the tag in their server data. */
    public static final String KEY = "bettacraft";
    private static final int MAX_LISTED = 12;

    private InfoData() {}

    /** Tank under {@code pos} (any block of it): name, water, animals, nest, warnings, filters; null if none. */
    public static CompoundTag tank(ServerLevel level, BlockPos pos) {
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return null;
        TankData t = c.getTank();
        CompoundTag out = new CompoundTag();
        out.putString("name", t.name());
        // M11: the glass the tank is made of and its style (textured / clean)
        out.putString("glass", c.getGlassId().toString());
        out.putString("style", c.getStyle().getSerializedName());
        out.putFloat("temp", t.waterTemp());
        out.putLong("water", t.waterMb());
        out.putLong("cap", t.capacityMb());
        // M31: a terrarium or paludarium and its climate
        // M33.1: a fixed tank type shows even for an aquarium
        boolean fixed = org.overengineer.bettacraft.terrarium.WaterLimit.setting(t) != org.overengineer.bettacraft.terrarium.TankModeSetting.AUTO;
        if (fixed) out.putBoolean("terra_fixed", true);
        if (t.terrariumMode().hasLand() || fixed) {
            out.putString("terra_mode", t.terrariumMode().translationKey());
            if (t.hasClimate()) {
                out.putFloat("air", t.airTemp());
                out.putFloat("rh", t.humidity());
                out.putFloat("uv", t.uv());
                out.putFloat("bask", t.baskTemp());
                out.putFloat("land", t.landShare());
            }
        }
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        out.putInt("count", live.size());
        out.putInt("adults", TankInhabitants.countAdults(live));
        out.putBoolean("crowded", c.isCrowded());
        BubbleNest n = t.nest();
        out.putString("nest", TankBreeding.nestState(t));
        out.putInt("eggs", n == null ? 0 : n.eggCount());
        ListTag warn = new ListTag();
        for (String w : TankBreeding.warnings(level, c)) warn.add(StringTag.valueOf(w));
        out.put("warnings", warn);
        ListTag fw = new ListTag(); // M40: multiblock filters that reach the tank without guards
        for (var e : org.overengineer.bettacraft.filter.MultiblockFilter.warnings(level, c).entrySet())
            fw.add(StringTag.valueOf(e.getKey().getX() + " " + e.getKey().getY() + " " + e.getKey().getZ() + "|" + e.getValue().key()));
        out.put("filter_warn", fw);
        BettaHealth.Params p = BettaCare.params();
        ListTag list = new ListTag();
        for (Entity e : live) {
            if (list.size() >= MAX_LISTED) break;
            CompoundTag i = new CompoundTag();
            i.putString("name", TankCare.nameArg(e));
            if (e instanceof BettaEntity b) {
                i.putString("sex", b.isFemale() ? "female" : "male");
                i.putString("stage", b.stage());
                i.putFloat("health", Math.min(b.health().healthFraction(p), b.getHealth() / b.getMaxHealth()));
                i.putBoolean("sick", b.health().isSick());
            } else if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) {
                i.putString("sex", m.isFemale() ? "female" : "male");
                i.putString("stage", m.lifeStage().id());
                i.putFloat("health", Math.min(m.health().healthFraction(p), m.getHealth() / m.getMaxHealth()));
                i.putBoolean("sick", m.health().isSick());
                // M21.6: guppies and endlers with their genes: the genome, the client names the strain
                var lb = m.livebearerGenome();
                if (lb != null) {
                    i.put("lb", lb.save());
                    i.putBoolean("endler", m.species() == org.overengineer.bettacraft.tankmates.TankmateSpecies.ENDLER_GUPPY);
                }
            } else if (e instanceof LivingEntity le) {
                i.putFloat("health", le.getHealth() / le.getMaxHealth());
            }
            list.add(i);
        }
        out.put("list", list);
        // M19: the tankmates by species, their eggs and young in clutches and in the breeding box
        java.util.Map<String, Integer> mates = new java.util.LinkedHashMap<>();
        for (Entity e : live) if (e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m) mates.merge(m.species().translationKey(), 1, Integer::sum);
        ListTag tm = new ListTag();
        for (var e : mates.entrySet()) {
            CompoundTag x = new CompoundTag();
            x.putString("k", e.getKey());
            x.putInt("n", e.getValue());
            tm.add(x);
        }
        if (!tm.isEmpty()) out.put("tankmates", tm);
        int eggs = 0;
        for (var k : t.clutches()) if (k.kind != org.overengineer.bettacraft.tankmates.Clutch.Kind.BOX && k.hasYoung()) eggs += k.count;
        out.putInt("tm_eggs", eggs);
        out.putInt("tm_box", t.breedingBox() ? org.overengineer.bettacraft.tankmates.TankmateBreeding.boxFry(t) : -1);
        var r = InteriorRegions.get(level.getServer()).get(t.regionId());
        out.putInt("filters_out", TankEquipment.filters(level, c).size());
        out.putInt("filters_in", r == null || !r.built() ? 0 : InteriorManager.equipment(level.getServer(), r).filters().size());
        // M12: floating plants
        out.putInt("plants", t.plants().size());
        out.putFloat("plant_cover", (float) org.overengineer.bettacraft.tank.plants.TankPlants.coverage(t));
        // M14: shrimp, crayfish, snails (adults and young) and the algae on the glass
        out.putIntArray("critters", org.overengineer.bettacraft.critters.TankCritters.counts(t));
        out.putFloat("algae", (float) org.overengineer.bettacraft.tank.algae.TankAlgae.mean(t));
        if (t.salinity() > 0) out.putFloat("salinity", t.salinity()); // M20
        if (t.calcium() > 0) out.putFloat("calcium", t.calcium());
        out.putFloat("stock", (float) org.overengineer.bettacraft.tank.TankInhabitants.stocking(level, t));
        // M9: water quality, evaporation, power
        if (TankWater.qualityEnabled()) {
            out.putFloat("quality", t.waterQuality());
            out.putString("quality_state", TankWater.state(t).name().toLowerCase(java.util.Locale.ROOT));
        }
        out.putDouble("evaporation", TankWater.evaporationPerDay(level, t));
        if (TankPower.anyRequired()) {
            out.putLong("power", t.powerBuffer());
            out.putLong("power_cap", TankPower.capacity());
            out.putInt("demand", c.lastDemand());
            if (c.driveInputs() > 0) { // M41: Create's rotation charges the buffer
                out.putInt("drive_fe", c.driveFe());
                out.putInt("drive_inputs", c.driveInputs());
                out.putBoolean("drive_overstressed", c.driveOverstressed());
            }
            out.putBoolean("unpowered", (TankPower.heaterRequired() && !c.heatPowered()) || (TankPower.filterRequired() && !c.filterPowered())
                    || (TankPower.lightRequired() && !c.lightPowered()));
        }
        return out;
    }

    /** Larva tub / artemia hatchery (M9): water, ingredient, progress, food, what is missing. */
    public static CompoundTag culture(ServerLevel level, BlockEntity be) {
        if (!(be instanceof org.overengineer.bettacraft.food.CultureTubBlockEntity tub)) return null;
        CompoundTag out = new CompoundTag();
        out.putBoolean("artemia", tub.artemia());
        out.putInt("water", tub.water());
        out.putInt("uses", tub.ingredientUses());
        out.putInt("bottles", tub.items().getStackInSlot(org.overengineer.bettacraft.food.CultureTubBlockEntity.BOTTLES).getCount());
        out.putFloat("progress", (float) tub.progress());
        out.putInt("ready", tub.items().getStackInSlot(org.overengineer.bettacraft.food.CultureTubBlockEntity.OUTPUT).getCount());
        String b = tub.blocker(level);
        if (b != null) out.putString("blocked", b);
        return out;
    }

    /** External filter block or cabinet: heaters, attached tank, storage; null for other block entities. */
    public static CompoundTag filter(ServerLevel level, BlockEntity be) {
        if (!(be instanceof FilterHost f)) return null;
        CompoundTag out = new CompoundTag();
        out.putBoolean("has_filter", f.hasFilter());
        out.putInt("heaters", f.heaterCount() - f.coolerCount());
        out.putInt("heat", f.heaterBlocks());
        out.putInt("coolers", f.coolerCount());
        out.putInt("cool", f.coolerBlocks());
        TankBlockEntity c = FilterHost.tankOf(level, f.tankBlockPos());
        boolean attached = c != null && TankEquipment.filters(level, c).contains(f);
        out.putBoolean("attached", attached);
        if (be instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity ef) {
            out.putBoolean("filter_block", true); // M62: a filter block that feeds no tank explains the sneak-click
            if (attached && ef.below() > 0) out.putInt("below", ef.below());
        }
        if (attached) {
            out.putString("tank", c.getTank().name());
            if (TankPower.filterRequired()) out.putBoolean("unpowered", !c.filterPowered());
        }
        if (f instanceof org.overengineer.bettacraft.winding.Windable w) putSpring(out, w); // M37
        if (f instanceof CabinetBlockEntity cab) {
            // M41: the Create shaft on its drive side
            org.overengineer.bettacraft.tank.TankDrive.Bridge drive = org.overengineer.bettacraft.tank.TankDrive.BRIDGE;
            if (drive != null && cab.driveSide() != org.overengineer.bettacraft.equipment.DriveSide.NONE) {
                out.putString("drive", cab.driveSide().id());
                double fe = drive.cabinetFe(cab);
                out.putInt("drive_fe", fe < 0 ? -1 : (int) Math.round(fe));
                out.putBoolean("drive_overstressed", drive.cabinetOverstressed(cab));
            }
            int n = 0;
            for (int i = 0; i < cab.storage().getSlots(); i++) n += cab.storage().getStackInSlot(i).getCount();
            out.putInt("storage", n);
            if (cab.hasSump() && org.overengineer.bettacraft.equipment.Sump.enabled()) { // M49: the sump and its chambers
                var ch = org.overengineer.bettacraft.equipment.Sump.read(cab.sumpItem());
                int h = 0;
                for (int i = 0; i < 2; i++) if (!ch.get(org.overengineer.bettacraft.equipment.Sump.HEATER0 + i).isEmpty()) h++;
                int m = 0;
                for (int i = 0; i < 2; i++) if (!ch.get(org.overengineer.bettacraft.equipment.Sump.MEDIA0 + i).isEmpty()) m++;
                out.putIntArray("sump", new int[]{h, ch.get(org.overengineer.bettacraft.equipment.Sump.SKIMMER).isEmpty() ? 0 : 1, m,
                        ch.get(org.overengineer.bettacraft.equipment.Sump.ALGAE).getCount(), ch.get(org.overengineer.bettacraft.equipment.Sump.LIGHT).isEmpty() ? 0 : 1});
            }
        }
        return out;
    }

    /** M37: a device with a spring (food maker, fertilizer maker, osmosis, water devices, filter pump head); null otherwise. */
    public static CompoundTag spring(ServerLevel level, BlockEntity be) {
        org.overengineer.bettacraft.winding.Windable w = be instanceof org.overengineer.bettacraft.winding.Windable x ? x
                : level == null || be == null ? null : org.overengineer.bettacraft.winding.Winding.find(level, be.getBlockPos());
        if (w == null) return null;
        CompoundTag out = new CompoundTag();
        putSpring(out, w);
        return out.isEmpty() ? null : out;
    }

    private static void putSpring(CompoundTag out, org.overengineer.bettacraft.winding.Windable w) {
        int pct = org.overengineer.bettacraft.winding.Winding.shown(w);
        if (pct < 0) return;
        out.putInt("spring", pct);
        org.overengineer.bettacraft.winding.WindSpring s = w.windStore(); // M62: the store of the whole under-cabinet
        out.putString("spring_hours", org.overengineer.bettacraft.winding.Winding.hours(s));
        if (s instanceof org.overengineer.bettacraft.equipment.CabinetSprings.Store store) out.putInt("spring_shared", store.size());
    }

    /** Betta: care state (only meaningful in a tank), stage progress, genotype, generation. */
    public static CompoundTag betta(BettaEntity b) {
        BettaHealth.Params p = BettaCare.params();
        CompoundTag out = new CompoundTag();
        out.putBoolean("in_tank", b.isInTank() || TankInhabitants.hasTankRef(b));
        out.putFloat("progress", (float) b.stageProgress());
        out.putFloat("health", Math.min(b.health().healthFraction(p), b.getHealth() / b.getMaxHealth()));
        out.putFloat("satiation", b.health().satiation(p));
        out.putBoolean("hungry", b.health().isHungry(p));
        out.putBoolean("sick", b.health().isSick());
        // M22.3: the genotype only once analysed with the magnifier
        if (org.overengineer.bettacraft.genetics.GeneVisibility.visible(b.getBettaData().analysed())) out.put("genotype", b.genotype().save());
        else out.putBoolean("genes_unknown", true);
        out.putInt("generation", b.getBettaData().generation());
        out.putBoolean("flaring", b.isFlaring());
        out.putBoolean("trained", b.breeding().isTrained() && !b.isFemale());
        return out;
    }
}
