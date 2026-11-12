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
            data.put("bettacraft", out);
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.TANK;
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
