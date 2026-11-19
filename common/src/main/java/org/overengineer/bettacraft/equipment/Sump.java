package org.overengineer.bettacraft.equipment;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.filter.FilterMediaItem;
import org.overengineer.bettacraft.filter.FilterMedium;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankNitrate;
import org.overengineer.bettacraft.tank.TankPower;

/**
 * M49 part a: the sump, a small tank in the cabinet under a tank ({@link CabinetBlockEntity#SUMP_SLOT}). The return pump moves
 * the water of the tank through it, so it works like an external filter ({@link SumpHost}); its water adds to the tank
 * ({@code sump.sumpWaterBlocks}: the waste, the nitrate and the dosed values spread over more water, and it counts for the
 * stocking, {@link TankData#systemBlocks()}). Its chambers hold what would else take room in the tank:
 * <ul>
 *     <li>two heaters or coolers (like the external filter's),</li>
 *     <li>a protein skimmer (cleans salt water like one on the wall),</li>
 *     <li>two filter media (clean like a chamber of the multiblock filter and clog the same way),</li>
 *     <li>the refugium ({@code features.sump.refugium}): Chaetomorpha (up to 16) under a tank light. The algae take up nitrate
 *     ({@code sump.refugiumNitratePerAlgae}), starve the algae on the glass of the nutrients and grow
 *     ({@code sump.chaetoDoublingDays}); take the extra ones out as food or for the composter.</li>
 * </ul>
 * A level sensor or a dosing pump next to the cabinet serves the tank above ({@link #tankBeside}). The chambers are kept in the
 * sump item ({@code minecraft:container}), the growth of the algae in its custom data, so they move with it.
 */
public final class Sump {
    public static final int HEATER0 = 0, SKIMMER = 2, MEDIA0 = 3, ALGAE = 5, LIGHT = 6, CHAMBERS = 7;
    public static final int MAX_ALGAE = 16;

    private Sump() {}

    public static boolean enabled() {
        return Features.enabled(Feature.SUMP);
    }

    public static boolean refugiumEnabled() {
        return Features.enabled(Feature.REFUGIUM);
    }

    public static boolean isSump(ItemStack s) {
        return !s.isEmpty() && s.is(ModItems.SUMP.get());
    }

    // ------------------------------------------------------------------ the chambers of the item

    public static NonNullList<ItemStack> read(ItemStack sump) {
        NonNullList<ItemStack> list = NonNullList.withSize(CHAMBERS, ItemStack.EMPTY);
        ItemContainerContents c = sump.get(DataComponents.CONTAINER);
        if (c != null) c.copyInto(list);
        return list;
    }

    public static void write(ItemStack sump, List<ItemStack> chambers) {
        boolean any = false;
        for (ItemStack s : chambers) any |= !s.isEmpty();
        if (!any) sump.remove(DataComponents.CONTAINER);
        else sump.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(new ArrayList<>(chambers)));
    }

    /** Whether {@code stack} fits into a chamber. */
    public static boolean fits(int chamber, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return switch (chamber) {
            case HEATER0, HEATER0 + 1 -> FilterHeaters.isHeater(stack);
            case SKIMMER -> stack.is(ModItems.PROTEIN_SKIMMER.get());
            case MEDIA0, MEDIA0 + 1 -> stack.getItem() instanceof FilterMediaItem;
            case ALGAE -> stack.is(ModItems.CHAETOMORPHA.get());
            case LIGHT -> stack.getItem() instanceof TankLightItem;
            default -> false;
        };
    }

    public static int limit(int chamber) {
        return chamber == ALGAE ? MAX_ALGAE : 1;
    }

    /** The growth of the algae towards the next one (0..1). */
    public static float growth(ItemStack sump) {
        CustomData d = sump.get(DataComponents.CUSTOM_DATA);
        if (d == null) return 0;
        float g = d.copyTag().getFloatOr("chaeto_growth", 0f);
        return Float.isFinite(g) ? Math.max(0, Math.min(1, g)) : 0;
    }

    static void setGrowth(ItemStack sump, float g) {
        CompoundTag t = sump.has(DataComponents.CUSTOM_DATA) ? sump.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        if (g <= 0) t.remove("chaeto_growth");
        else t.putFloat("chaeto_growth", g);
        if (t.isEmpty()) sump.remove(DataComponents.CUSTOM_DATA);
        else sump.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    // ------------------------------------------------------------------ the sumps of a tank

    /** The sumps in the cabinets under this tank (empty while {@code features.sump.enabled} is off). */
    public static List<SumpHost> hosts(Level level, TankBlockEntity c) {
        if (!enabled()) return List.of();
        List<SumpHost> out = new ArrayList<>();
        for (CabinetBlockEntity cab : TankEquipment.cabinets(level, c)) if (cab.hasSump()) out.add(new SumpHost(cab));
        return out;
    }

    /** Water blocks the sumps add to the tank (0 without water in the tank). */
    public static double waterBlocks(Level level, TankBlockEntity c) {
        if (c.getTank().waterMb() <= 0) return 0;
        return hosts(level, c).size() * (double) ServerConfig.getOr(ServerConfig.SUMP_WATER_BLOCKS, 0.5);
    }

    /** Protein skimmers in the sumps (they count like the ones on the wall). */
    public static int skimmers(Level level, TankBlockEntity c) {
        int n = 0;
        for (SumpHost h : hosts(level, c)) if (!h.chamber(SKIMMER).isEmpty()) n++;
        return n;
    }

    /** Water blocks the filter media in the sumps keep clean while their return pumps run. */
    public static double mediaBlocks(Level level, TankBlockEntity c) {
        double n = 0;
        for (SumpHost h : hosts(level, c)) {
            if (!TankPower.filterRuns(c, h)) continue;
            for (int i = 0; i < 2; i++) {
                ItemStack m = h.chamber(MEDIA0 + i);
                FilterMedium kind = FilterMediaItem.mediumOf(m);
                n += kind.blocks() * kind.efficiency(FilterMediaItem.dirt(m)) * h.powerShare();
            }
        }
        return n;
    }

    /** Chaetomorpha in lit refugia under this tank (0 while the refugium is switched off). */
    public static int refugiumAlgae(Level level, TankBlockEntity c) {
        if (!refugiumEnabled() || c.getTank().waterMb() <= 0) return 0;
        int n = 0;
        for (SumpHost h : hosts(level, c)) if (h.refugiumLit(c)) n += h.chamber(ALGAE).getCount();
        return n;
    }

    /** Nitrate (mg/L) the refugia take out of the water per day. */
    public static double nitrateUptakePerDay(Level level, TankBlockEntity c) {
        int algae = refugiumAlgae(level, c);
        if (algae <= 0) return 0;
        return algae * (double) ServerConfig.getOr(ServerConfig.REFUGIUM_NITRATE, 0.6) / Math.max(1.0, c.getTank().systemBlocks());
    }

    /** Pure: the factor on the growth of the glass algae from the Chaetomorpha (it takes their nutrients): down to half. */
    public static double algaeFactor(int chaeto, double systemBlocks) {
        if (chaeto <= 0) return 1;
        return 1 - 0.5 * Math.min(1.0, chaeto * 2.0 / Math.max(1.0, systemBlocks) / MAX_ALGAE);
    }

    public static double algaeFactor(Level level, TankBlockEntity c) {
        return algaeFactor(refugiumAlgae(level, c), c.getTank().systemBlocks());
    }

    /** Pure: the algae of a refugium after {@code days}: logistic growth to a full chamber; returns {count, growth}. */
    public static double[] grow(int count, double growth, double days, double doublingDays) {
        if (count <= 0 || count >= MAX_ALGAE || days <= 0) return new double[]{count, count >= MAX_ALGAE ? 0 : growth};
        double g = growth + count * Math.log(2) / Math.max(0.1, doublingDays) * (1 - count / (double) MAX_ALGAE) * days;
        int n = count;
        while (g >= 1 && n < MAX_ALGAE) {
            g -= 1;
            n++;
        }
        return new double[]{n, n >= MAX_ALGAE ? 0 : g};
    }

    /**
     * The care step (every second): the water of the sumps for the tank, the media clog, the Chaetomorpha grows and takes up
     * nitrate.
     */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        List<SumpHost> hosts = hosts(level, c);
        t.setSumpBlocks(t.waterMb() <= 0 ? 0 : hosts.size() * (double) ServerConfig.getOr(ServerConfig.SUMP_WATER_BLOCKS, 0.5));
        if (hosts.isEmpty() || days <= 0 || t.waterMb() <= 0) return;
        double load = Math.max(0.25, Math.min(4.0, TankNitrate.load(level, t)));
        double dirt = ServerConfig.getOr(ServerConfig.MEDIA_DIRT_PER_DAY, 0.04) * load * days;
        double doubling = ServerConfig.getOr(ServerConfig.CHAETO_DOUBLING_DAYS, 4.0);
        for (SumpHost h : hosts) {
            ItemStack sump = h.cabinet().sumpItem();
            NonNullList<ItemStack> ch = read(sump);
            boolean changed = false;
            if (TankPower.filterRuns(c, h)) {
                for (int i = 0; i < 2; i++) {
                    ItemStack m = ch.get(MEDIA0 + i);
                    if (!(m.getItem() instanceof FilterMediaItem fm) || !m.isDamageableItem()) continue;
                    double add = dirt * fm.medium().dirtRate() * FilterMediaItem.MAX_DIRT;
                    int whole = (int) Math.floor(add);
                    if (level.getRandom().nextDouble() < add - whole) whole++; // the fraction by chance (one step is a few hundredths)
                    if (whole <= 0 || m.getDamageValue() >= m.getMaxDamage()) continue;
                    m.setDamageValue(Math.min(m.getMaxDamage(), m.getDamageValue() + whole));
                    changed = true;
                }
            }
            float growth = growth(sump);
            float newGrowth = growth;
            if (refugiumEnabled() && h.refugiumLit(c)) {
                ItemStack a = ch.get(ALGAE);
                double[] r = grow(a.getCount(), growth, days, doubling);
                if ((int) r[0] != a.getCount()) {
                    ch.set(ALGAE, a.copyWithCount((int) r[0]));
                    changed = true;
                }
                newGrowth = (float) r[1];
            }
            if (changed || Math.abs(newGrowth - growth) > 1e-6) {
                ItemStack ns = sump.copy();
                if (changed) write(ns, ch);
                setGrowth(ns, newGrowth);
                h.cabinet().setSumpItem(ns);
            }
        }
        double uptake = nitrateUptakePerDay(level, c);
        if (uptake > 0 && TankNitrate.enabled()) {
            t.setNitrate((float) Math.max(0, t.nitrate() - uptake * days));
            c.setChanged();
        }
    }

    /** The tank above a cabinet with a sump next to {@code pos} (level sensors and dosing pumps beside the cabinet), or null. */
    @Nullable
    public static TankBlockEntity tankBeside(Level level, BlockPos pos) {
        if (!enabled()) return null;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockEntity(n) instanceof CabinetBlockEntity cab) || !cab.hasSump()) continue;
            BlockPos up = cab.tankBlockPos();
            if (up == null || !level.isLoaded(up)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, up);
            if (c != null) return c;
        }
        return null;
    }

    /** The sump line of the tank menu (the tab "Sump"; empty without a sump). */
    public static CompoundTag view(Level level, TankBlockEntity c) {
        CompoundTag out = new CompoundTag();
        List<SumpHost> hosts = hosts(level, c);
        if (hosts.isEmpty()) return out;
        out.putInt("count", hosts.size());
        out.putDouble("blocks", c.getTank().sumpBlocks());
        out.putDouble("tank_blocks", c.getTank().waterBlocks());
        int heaters = 0, skimmers = 0, media = 0, algae = 0, lit = 0;
        double dirt = 0;
        boolean running = false;
        for (SumpHost h : hosts) {
            heaters += h.heaterCount();
            if (!h.chamber(SKIMMER).isEmpty()) skimmers++;
            for (int i = 0; i < 2; i++) {
                ItemStack m = h.chamber(MEDIA0 + i);
                if (m.isEmpty()) continue;
                media++;
                dirt = Math.max(dirt, FilterMediaItem.dirt(m));
            }
            algae += h.chamber(ALGAE).getCount();
            if (h.refugiumLit(c)) lit++;
            running |= TankPower.filterRuns(c, h);
        }
        out.putInt("heaters", heaters);
        out.putInt("skimmers", skimmers);
        out.putInt("media", media);
        out.putDouble("dirt", dirt);
        out.putDouble("media_blocks", level instanceof ServerLevel ? mediaBlocks(level, c) : 0);
        out.putInt("algae", algae);
        out.putInt("lit", lit);
        out.putBoolean("refugium_on", refugiumEnabled());
        out.putDouble("uptake", nitrateUptakePerDay(level, c));
        out.putDouble("algae_factor", algaeFactor(level, c));
        out.putBoolean("running", running);
        // the devices beside the cabinets
        int sensors = 0, pumps = 0;
        for (SumpHost h : hosts) {
            for (Direction d : Direction.values()) {
                BlockPos n = h.cabinet().getBlockPos().relative(d);
                if (!level.isLoaded(n)) continue;
                var be = level.getBlockEntity(n);
                if (be instanceof org.overengineer.bettacraft.plumbing.LevelSensorBlockEntity) sensors++;
                else if (be instanceof org.overengineer.bettacraft.reef.DosingPumpBlockEntity) pumps++;
                else if (be instanceof org.overengineer.bettacraft.reef.CalciumReactorBlockEntity) out.putBoolean("reactor", true);
            }
        }
        out.putInt("sensors", sensors);
        out.putInt("dosing", pumps);
        return out;
    }
}
