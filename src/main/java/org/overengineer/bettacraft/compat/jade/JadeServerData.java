package org.overengineer.bettacraft.compat.jade;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the Jade integration (no client classes). */
final class JadeServerData {
    private JadeServerData() {}

    static final class Tank implements IServerDataProvider<BlockAccessor> {
        static final Tank INSTANCE = new Tank();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel level)) return;
            TankBlockEntity c = TankMultiblock.getController(level, accessor.getPosition());
            if (c == null) return;
            TankData t = c.getTank();
            CompoundTag out = new CompoundTag();
            out.putString("name", t.name());
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
                if (list.size() >= 12) break;
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
            int outside = org.overengineer.bettacraft.tank.TankEquipment.filters(level, c).size();
            var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId());
            int inside = r == null || !r.built() ? 0 : org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r).filters().size();
            out.putInt("filters_out", outside);
            out.putInt("filters_in", inside);
            data.put("bettacraft", out);
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.TANK;
        }
    }

    /** External filter block and cabinet (M4): attached tank, heaters, storage. */
    static final class Filter implements IServerDataProvider<BlockAccessor> {
        static final Filter INSTANCE = new Filter();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel level)) return;
            if (!(accessor.getBlockEntity() instanceof org.overengineer.bettacraft.equipment.FilterHost f)) return;
            CompoundTag out = new CompoundTag();
            out.putBoolean("has_filter", f.hasFilter());
            out.putInt("heaters", f.heaterCount());
            out.putInt("heat", f.heaterBlocks());
            TankBlockEntity c = org.overengineer.bettacraft.equipment.FilterHost.tankOf(level, f.tankBlockPos());
            boolean attached = c != null && org.overengineer.bettacraft.tank.TankEquipment.filters(level, c).contains(f);
            out.putBoolean("attached", attached);
            if (attached) out.putString("tank", c.getTank().name());
            if (f instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab) {
                int n = 0;
                for (int i = 0; i < cab.storage().getSlots(); i++) n += cab.storage().getStackInSlot(i).getCount();
                out.putInt("storage", n);
            }
            data.put("bettacraft", out);
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.FILTER;
        }
    }

    static final class Betta implements IServerDataProvider<EntityAccessor> {
        static final Betta INSTANCE = new Betta();

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof BettaEntity b)) return;
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
            data.put("bettacraft", out);
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.BETTA;
        }
    }
}
