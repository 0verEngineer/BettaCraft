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
            } else if (e instanceof LivingEntity le) {
                i.putFloat("health", le.getHealth() / le.getMaxHealth());
            }
            list.add(i);
        }
        out.put("list", list);
        var r = InteriorRegions.get(level.getServer()).get(t.regionId());
        out.putInt("filters_out", TankEquipment.filters(level, c).size());
        out.putInt("filters_in", r == null || !r.built() ? 0 : InteriorManager.equipment(level.getServer(), r).filters().size());
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
        if (attached) {
            out.putString("tank", c.getTank().name());
            if (TankPower.filterRequired()) out.putBoolean("unpowered", !c.filterPowered());
        }
        if (f instanceof CabinetBlockEntity cab) {
            int n = 0;
            for (int i = 0; i < cab.storage().getSlots(); i++) n += cab.storage().getStackInSlot(i).getCount();
            out.putInt("storage", n);
        }
        return out;
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
        out.put("genotype", b.genotype().save());
        out.putInt("generation", b.getBettaData().generation());
        out.putBoolean("flaring", b.isFlaring());
        out.putBoolean("trained", b.breeding().isTrained() && !b.isFemale());
        return out;
    }
}
