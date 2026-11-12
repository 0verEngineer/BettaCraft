package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * Feeder on top of a tank (PLAN 6.3): 9 food slots (hoppers and item capabilities can insert), feeds 1-3 times per
 * in-game day. Feeding times are day time 1000 (morning), plus 11000 for two and 7000/13000 for three feedings.
 */
public class FeederBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 9;
    private static final long[][] SCHEDULE = {{1000}, {1000, 11000}, {1000, 7000, 13000}};

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModTags.BETTA_FOOD);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int feedingsPerDay = 1;
    private long lastEvent = Long.MIN_VALUE;
    private double virtualCredit;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return feedingsPerDay;
        }

        @Override
        public void set(int index, int value) {
            feedingsPerDay = Math.max(1, Math.min(3, value));
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public int feedingsPerDay() {
        return feedingsPerDay;
    }

    public void cycleFeedings() {
        feedingsPerDay = feedingsPerDay % 3 + 1;
        setChanged();
    }

    public int foodCount() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) n += items.getStackInSlot(i).getCount();
        return n;
    }

    @Nullable
    public TankBlockEntity tankBelow() {
        return level == null ? null : TankMultiblock.getController(level, worldPosition.below());
    }

    /** Key of the latest scheduled feeding at or before {@code dayTime}. */
    static long eventKey(long dayTime, int feedings) {
        long[] times = SCHEDULE[Math.max(1, Math.min(3, feedings)) - 1];
        long day = Math.floorDiv(dayTime, 24000L);
        long tod = Math.floorMod(dayTime, 24000L);
        for (int i = times.length - 1; i >= 0; i--) {
            if (tod >= times[i]) return day * 4 + i;
        }
        return (day - 1) * 4 + (times.length - 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity be) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.asLong()) % 20 != 0) return;
        long key = eventKey(level.getDayTime(), be.feedingsPerDay);
        if (be.lastEvent == Long.MIN_VALUE || key < be.lastEvent) {
            be.lastEvent = key; // fresh feeder or time set backwards: wait for the next feeding time
            be.setChanged();
            return;
        }
        if (key == be.lastEvent) return;
        be.lastEvent = key;
        be.setChanged();
        TankBlockEntity tank = be.tankBelow();
        if (tank != null) be.feedNow(server, tank);
    }

    /**
     * Feeds the tank: every fish gets the first suitable food in the slots (fry food for fry, pellets / larvae for the
     * others, PLAN 6.3). Returns the items used.
     */
    public int feedNow(ServerLevel level, TankBlockEntity tank) {
        java.util.Set<java.util.UUID> fed = new java.util.HashSet<>();
        int used = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty() || !s.is(ModTags.BETTA_FOOD)) continue;
            int n = TankCare.feed(level, tank, s, s.getCount(), fed);
            if (n > 0) {
                items.extractItem(i, n, false);
                used += n;
            }
        }
        if (used == 0 && !TankCare.bettas(level, tank.getTank()).isEmpty()) TankCare.log(level, tank, "log.bettacraft.feeder_empty");
        return used;
    }

    /** Catch-up feeding (simulateUnloadedTime): consumes fractional portions; false if there was no food. */
    public boolean consumeVirtualPortion(double portions) {
        virtualCredit += portions;
        while (virtualCredit >= 1.0) {
            int slot = -1;
            for (int i = 0; i < SLOTS && slot < 0; i++) if (!items.getStackInSlot(i).isEmpty()) slot = i;
            if (slot < 0) {
                virtualCredit = 0;
                return false;
            }
            items.extractItem(slot, 1, false);
            virtualCredit -= 1.0;
        }
        return foodCount() > 0 || virtualCredit > 0;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.feeder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FeederMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Schemas.FEEDER.stamp(tag);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("feedings", feedingsPerDay);
        tag.putLong("last_event", lastEvent);
        tag.putDouble("credit", virtualCredit);
    }

    @Override
    protected void loadAdditional(CompoundTag raw, HolderLookup.Provider registries) {
        super.loadAdditional(raw, registries);
        CompoundTag tag = Schemas.FEEDER.migrate(raw);
        items.deserializeNBT(registries, tag.getCompound("items"));
        feedingsPerDay = Math.max(1, Math.min(3, tag.getInt("feedings")));
        lastEvent = tag.contains("last_event") ? tag.getLong("last_event") : Long.MIN_VALUE;
        virtualCredit = tag.getDouble("credit");
    }
}
