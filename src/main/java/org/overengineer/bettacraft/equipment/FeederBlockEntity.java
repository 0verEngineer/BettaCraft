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

    /** Slots of this feeder (9; the M21.5 clip-on feeder 1). */
    protected final int slots;
    private final ItemStackHandler items;
    private ItemStackHandler handler(int n) {
        return new ItemStackHandler(n) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModTags.BETTA_FOOD);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }
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
        this(ModBlockEntities.FEEDER.get(), pos, state, SLOTS);
    }

    protected FeederBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.slots = slots;
        this.items = handler(slots);
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
        for (int i = 0; i < slots; i++) n += items.getStackInSlot(i).getCount();
        return n;
    }

    /** The tank this feeder feeds (the one below it; the clip-on feeder: the one it hangs on). */
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

    /** M21.5: whether this feeder works (the clip-on feeder can be switched off). */
    protected boolean active() {
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity be) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (!be.active()) return;
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
        for (int i = 0; i < slots; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty() || !s.is(ModTags.BETTA_FOOD)) continue;
            int n = TankCare.feed(level, tank, s, s.getCount(), fed);
            if (n > 0) {
                items.extractItem(i, n, false);
                used += n;
            }
        }
        // then food stored in the cabinets under the tank (PLAN 6.5)
        for (CabinetBlockEntity cab : org.overengineer.bettacraft.tank.TankEquipment.cabinets(level, tank)) {
            var st = cab.storage();
            for (int i = 0; i < st.getSlots(); i++) {
                ItemStack s = st.getStackInSlot(i);
                if (s.isEmpty() || !s.is(ModTags.BETTA_FOOD)) continue;
                int n = TankCare.feed(level, tank, s, s.getCount(), fed);
                if (n > 0) {
                    st.extractItem(i, n, false);
                    used += n;
                }
            }
        }
        // M12: under a dense cover of floating plants some food stays on the leaves: it costs food and rots in the water
        int stuck = org.overengineer.bettacraft.tank.plants.TankPlants.pelletsOnLeaves(tank.getTank(), used, level.getRandom()::nextLong);
        if (stuck > 0) {
            int left = stuck;
            for (int i = 0; i < slots && left > 0; i++) {
                ItemStack s = items.getStackInSlot(i);
                if (s.isEmpty() || !s.is(ModTags.BETTA_FOOD)) continue;
                left -= items.extractItem(i, left, false).getCount();
            }
            org.overengineer.bettacraft.tank.TankWater.overfed(level, tank, stuck);
            TankCare.log(level, tank, "log.bettacraft.pellets_on_leaves", String.valueOf(stuck));
        }
        if (used == 0 && !TankCare.bettas(level, tank.getTank()).isEmpty()) TankCare.log(level, tank, "log.bettacraft.feeder_empty");
        if (used > 0) org.overengineer.bettacraft.progression.Progress.awardNear(level, tank, org.overengineer.bettacraft.progression.Progress.FEEDER, used);
        return used;
    }

    /** Catch-up feeding (simulateUnloadedTime): consumes fractional portions; false if there was no food. */
    public boolean consumeVirtualPortion(double portions) {
        // M17: the credit is food already taken from the slots (one item feeds several portions); before, an empty
        // feeder still fed 3 of 4 portions because the credit was counted the other way round
        if (!(portions > 0)) return true;
        while (virtualCredit < portions - 1e-9) {
            int slot = -1;
            for (int i = 0; i < slots && slot < 0; i++) if (!items.getStackInSlot(i).isEmpty()) slot = i;
            if (slot < 0) return false;
            items.extractItem(slot, 1, false);
            virtualCredit += 1.0;
        }
        virtualCredit -= portions;
        return true;
    }

    /** Food already taken from the slots for catch-up feeding (portions of one item). */
    public double virtualCredit() {
        return virtualCredit;
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
        double credit = tag.getDouble("credit");
        virtualCredit = Double.isFinite(credit) ? Math.max(0, Math.min(1, credit)) : 0;
    }
}
