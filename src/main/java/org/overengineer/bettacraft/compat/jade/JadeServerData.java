package org.overengineer.bettacraft.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server side of the Jade integration (no client classes); the data comes from {@link InfoData}. */
final class JadeServerData {
    private JadeServerData() {}

    private static void put(CompoundTag data, CompoundTag out) {
        if (out != null) data.put(InfoData.KEY, out);
    }

    static final class Tank implements IServerDataProvider<BlockAccessor> {
        static final Tank INSTANCE = new Tank();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getLevel() instanceof ServerLevel level) put(data, InfoData.tank(level, accessor.getPosition()));
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
            if (accessor.getLevel() instanceof ServerLevel level) put(data, InfoData.filter(level, accessor.getBlockEntity()));
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.FILTER;
        }
    }

    /** Larva tub and artemia hatchery (M9). */
    static final class Culture implements IServerDataProvider<BlockAccessor> {
        static final Culture INSTANCE = new Culture();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getLevel() instanceof ServerLevel level) put(data, InfoData.culture(level, accessor.getBlockEntity()));
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.CULTURE;
        }
    }

    static final class Betta implements IServerDataProvider<EntityAccessor> {
        static final Betta INSTANCE = new Betta();

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (accessor.getEntity() instanceof BettaEntity b) put(data, InfoData.betta(b));
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.BETTA;
        }
    }
}
