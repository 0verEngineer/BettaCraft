package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.platform.item.IItemHandlerModifiable;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.platform.item.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;

/**
 * Cabinet menu: the filter slot, two heater slots that edit the heaters inside that filter item (only active while a
 * filter is in the slot), nine storage slots and the player inventory.
 */
public class CabinetMenu extends AbstractContainerMenu {
    /** M66: the panel is 216 x 188; the storage sits at x 124, the inventory 20 px right and 22 px down of the vanilla layout. */
    public static final int FILTER_X = 26, FILTER_Y = 18, HEATER_X = 17, HEATER_Y = 44, STORAGE_X = 124, STORAGE_Y = 17,
            INV_X = 28, INV_Y = 106, WIDTH = 216, HEIGHT = 188;
    /** M29.1: the air pump slot, right of the heaters. */
    public static final int AIR_PUMP_X = 74, AIR_PUMP_Y = 44;
    /** Menu slot indices. */
    public static final int FILTER = 0, HEATER0 = 1, AIR_PUMP = 3, STORAGE0 = 4, PLAYER0 = 4 + CabinetBlockEntity.STORAGE;
    /** M49: the sump slot between the heaters and the air pump (after the player inventory in the menu), and the button that opens its chambers. */
    public static final int SUMP_X = 55, SUMP_Y = 44, SUMP = PLAYER0 + 36, BUTTON_SUMP = 60;
    /** M22.4: menu button ids 0..3 set the outlet side ({@link OutletSide} ordinal). */
    public static final int BUTTON_OUTLET0 = 0;
    /** M41: menu buttons: the next / previous side of the Create shaft that the server allows. */
    public static final int BUTTON_DRIVE_NEXT = 30, BUTTON_DRIVE_PREVIOUS = 31;
    /** M44: menu button ids 40..45 set the front ({@link CabinetFrontSetting} ordinal), 50 switches the joints. */
    public static final int BUTTON_FRONT0 = 40, BUTTON_JOINTS = 50;
    @Nullable
    private final CabinetBlockEntity cabinet;
    private final IItemHandlerModifiable items;
    /** M22.4: the outlet side (ordinal), -1 while {@code features.equipment.cabinetOutletSide} is off on the server. */
    private final net.minecraft.world.inventory.DataSlot outlet = net.minecraft.world.inventory.DataSlot.standalone();

    public CabinetMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(CabinetBlockEntity.SLOTS));
    }

    CabinetMenu(int id, Inventory inv, CabinetBlockEntity cabinet) {
        this(id, inv, cabinet, cabinet.items());
    }

    private CabinetMenu(int id, Inventory inv, @Nullable CabinetBlockEntity cabinet, IItemHandlerModifiable items) {
        super(ModMenus.CABINET.get(), id);
        this.cabinet = cabinet;
        this.items = items;
        addSlot(new SlotItemHandler(items, CabinetBlockEntity.FILTER_SLOT, FILTER_X, FILTER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.EXTERNAL_FILTER.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        FilterItemHeaters heaters = new FilterItemHeaters();
        for (int i = 0; i < 2; i++) addSlot(new HeaterSlot(heaters, i, HEATER_X + i * 18, HEATER_Y));
        addSlot(new SlotItemHandler(items, CabinetBlockEntity.AIR_PUMP_SLOT, AIR_PUMP_X, AIR_PUMP_Y) { // M29.1
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.AIR_PUMP.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                addSlot(new SlotItemHandler(items, 1 + c + r * 3, STORAGE_X + c * 18, STORAGE_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
        addSlot(new SlotItemHandler(items, CabinetBlockEntity.SUMP_SLOT, SUMP_X, SUMP_Y) { // M49
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Sump.isSump(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPowerSlot(cabinet);
        energy = new MenuEnergy(cabinet); // M66: the tank's store
        for (var s : energy.slots()) addDataSlot(s);
        outlet.set(cabinet == null ? -1 : outletValue(cabinet));
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return cabinet == null ? outlet.get() : outletValue(cabinet);
            }

            @Override
            public void set(int v) {
                outlet.set(v);
            }
        });
        addDataSlot(spring = org.overengineer.bettacraft.winding.Winding.slot(cabinet)); // M37
        addDataSlot(springShared = new net.minecraft.world.inventory.DataSlot() { // M62: cabinets sharing the store
            private int client = 1;

            @Override
            public int get() {
                return cabinet == null ? client : cabinet.windStore() instanceof CabinetSprings.Store s ? s.size() : 1;
            }

            @Override
            public void set(int v) {
                client = v;
            }
        });
        frontSlot.set(cabinet == null ? -1 : frontValue(cabinet)); // M44
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return cabinet == null ? frontSlot.get() : frontValue(cabinet);
            }

            @Override
            public void set(int v) {
                frontSlot.set(v);
            }
        });
        drive.set(cabinet == null ? -1 : driveValue(cabinet)); // M41
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return cabinet == null ? drive.get() : driveValue(cabinet);
            }

            @Override
            public void set(int v) {
                drive.set(v);
            }
        });
    }

    // ------------------------------------------------------------------ M44 front

    /** M44: the front setting (ordinal) + 16 with joints, -1 while {@code features.equipment.cabinetFronts} is off on the server. */
    private final net.minecraft.world.inventory.DataSlot frontSlot = net.minecraft.world.inventory.DataSlot.standalone();

    private static int frontValue(CabinetBlockEntity c) {
        return Features.enabled(Feature.CABINET_FRONTS) ? c.frontSetting().ordinal() + (c.joints() ? 16 : 0) : -1;
    }

    /** M44: the front setting shown in the menu, or null while the fronts are switched off. */
    @Nullable
    public CabinetFrontSetting frontSetting() {
        int v = cabinet == null ? frontSlot.get() : frontValue(cabinet);
        return v < 0 ? null : CabinetFrontSetting.byIndex(v & 15);
    }

    /** M44: whether the front shows its joints (false while the fronts are switched off). */
    public boolean joints() {
        int v = cabinet == null ? frontSlot.get() : frontValue(cabinet);
        return v >= 0 && (v & 16) != 0;
    }

    // ------------------------------------------------------------------ M41 Create shaft

    /** M41: the side of the Create shaft (ordinal), -1 while the server has no Create or the shaft on cabinets is off. */
    private final net.minecraft.world.inventory.DataSlot drive = net.minecraft.world.inventory.DataSlot.standalone();

    /** The setting's ordinal, + 16 when the server does not allow that side ({@code createDrive.cabinetSides}); -1: not available. */
    private static int driveValue(CabinetBlockEntity c) {
        if (!org.overengineer.bettacraft.tank.TankDrive.cabinetsEnabled()) return -1;
        DriveSide s = c.driveSetting();
        return s.ordinal() + (org.overengineer.bettacraft.tank.TankDrive.sideAllowed(s) ? 0 : 16);
    }

    /** M41: the drive side shown in the menu, or null while the setting is not available. */
    @Nullable
    public DriveSide driveSide() {
        int v = cabinet == null ? drive.get() : driveValue(cabinet);
        return v < 0 ? null : DriveSide.byIndex(v & 15);
    }

    /** M41: whether the server allows the side shown. */
    public boolean driveAllowed() {
        int v = cabinet == null ? drive.get() : driveValue(cabinet);
        return v >= 0 && v < 16;
    }

    /** The next side from {@code from} the server allows (none is always allowed). */
    static DriveSide nextAllowed(DriveSide from, boolean forward) {
        DriveSide s = from;
        for (int i = 0; i < DriveSide.values().length; i++) {
            s = forward ? s.next() : s.previous();
            if (org.overengineer.bettacraft.tank.TankDrive.sideAllowed(s)) return s;
        }
        return DriveSide.NONE;
    }

    /** M37: the spring of the cabinet in percent (-1: not windable now). */
    private net.minecraft.world.inventory.DataSlot spring;

    public int springPercent() {
        return spring.get();
    }

    /** M62: the cabinets that share the spring store of this one (1: it stands alone). */
    private net.minecraft.world.inventory.DataSlot springShared;

    public int springShared() {
        return springShared.get();
    }

    private static int outletValue(CabinetBlockEntity c) {
        return Features.enabled(Feature.CABINET_OUTLET_SIDE) ? c.outletSetting().ordinal() : -1;
    }

    /** M22.4: the outlet side shown in the menu, or null while the setting is switched off. */
    @Nullable
    public OutletSide outletSide() {
        int v = cabinet == null ? outlet.get() : outletValue(cabinet);
        return v < 0 ? null : OutletSide.byIndex(v);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (powerButton(player, id, cabinet)) return true;
        if (cabinet != null && id == BUTTON_SUMP) { // M49: the chambers of the sump in their own window
            if (!Sump.enabled() || !cabinet.hasSump() || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
            net.minecraft.core.BlockPos pos = cabinet.getBlockPos();
            org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, new net.minecraft.world.SimpleMenuProvider((cid, pinv, p) -> new SumpMenu(cid, pinv, cabinet),
                    net.minecraft.network.chat.Component.translatable("gui.bettacraft.sump.title")), b -> b.writeBlockPos(pos));
            return true;
        }
        if (cabinet != null && (id == BUTTON_DRIVE_NEXT || id == BUTTON_DRIVE_PREVIOUS)) { // M41
            if (!org.overengineer.bettacraft.tank.TankDrive.cabinetsEnabled()) return false;
            cabinet.setDriveSide(nextAllowed(cabinet.driveSetting(), id == BUTTON_DRIVE_NEXT));
            return true;
        }
        if (cabinet != null && (id == BUTTON_JOINTS || (id >= BUTTON_FRONT0 && id < BUTTON_FRONT0 + CabinetFrontSetting.values().length))) { // M44
            if (!Features.enabled(Feature.CABINET_FRONTS)) return false;
            if (id == BUTTON_JOINTS) cabinet.setJoints(!cabinet.joints());
            else cabinet.setFrontSetting(CabinetFrontSetting.byIndex(id - BUTTON_FRONT0));
            return true;
        }
        if (cabinet == null || id < BUTTON_OUTLET0 || id >= BUTTON_OUTLET0 + OutletSide.values().length) return false;
        if (!Features.enabled(Feature.CABINET_OUTLET_SIDE)) return false;
        cabinet.setOutletSide(OutletSide.byIndex(id - BUTTON_OUTLET0));
        return true;
    }

    // ------------------------------------------------------------------ M23 power regulator

    /** Button ids of the power regulator: up / down by 10 %. */
    public static final int BUTTON_POWER_UP = 20, BUTTON_POWER_DOWN = 21;
    private final net.minecraft.world.inventory.DataSlot power = net.minecraft.world.inventory.DataSlot.standalone();

    private void addPowerSlot(@Nullable FilterHost host) {
        power.set(host == null ? 10 : host.regulator());
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return host == null ? power.get() : host.regulator();
            }

            @Override
            public void set(int v) {
                power.set(v);
            }
        });
    }

    private final MenuEnergy energy;

    /** M66: the energy of the tank this cabinet feeds. */
    public MenuEnergy energy() {
        return energy;
    }

    /** Regulator 0..10 as the menu sees it. */
    public int power() {
        return power.get();
    }

    private boolean powerButton(Player player, int id, @Nullable FilterHost host) {
        if (host == null || (id != BUTTON_POWER_UP && id != BUTTON_POWER_DOWN)) return false;
        if (!Features.enabled(Feature.POWER_REGULATOR)) return false;
        host.setRegulator(Math.max(0, Math.min(10, host.regulator() + (id == BUTTON_POWER_UP ? 1 : -1))));
        return true;
    }

    public boolean hasFilter() {
        return !items.getStackInSlot(CabinetBlockEntity.FILTER_SLOT).isEmpty();
    }

    /** M49: a sump sits in its slot. */
    public boolean hasSump() {
        return !items.getStackInSlot(CabinetBlockEntity.SUMP_SLOT).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < PLAYER0 || index == SUMP) {
            if (!moveItemStackTo(s, PLAYER0, PLAYER0 + 36, true)) return ItemStack.EMPTY;
        } else if (Sump.isSump(s) && !slots.get(SUMP).hasItem()) { // M49
            if (!moveItemStackTo(s, SUMP, SUMP + 1, false)) return ItemStack.EMPTY;
        } else if (s.is(ModItems.EXTERNAL_FILTER.get()) && !slots.get(FILTER).hasItem()) {
            if (!moveItemStackTo(s, FILTER, FILTER + 1, false)) return ItemStack.EMPTY;
        } else if (s.is(ModItems.AIR_PUMP.get()) && !slots.get(AIR_PUMP).hasItem()) { // M29.1
            if (!moveItemStackTo(s, AIR_PUMP, AIR_PUMP + 1, false)) return ItemStack.EMPTY;
        } else if (FilterHeaters.isHeater(s) && hasFilter() && (!slots.get(HEATER0).hasItem() || !slots.get(HEATER0 + 1).hasItem())) {
            if (!moveItemStackTo(s, HEATER0, HEATER0 + 2, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, STORAGE0, PLAYER0, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return cabinet == null || (!cabinet.isRemoved() && player.distanceToSqr(cabinet.getBlockPos().getCenter()) <= 64.0);
    }

    /** The heaters inside the filter item in the filter slot, as a two-slot container (component in and out). */
    private final class FilterItemHeaters implements Container {
        private ItemStack filter() {
            return items.getStackInSlot(CabinetBlockEntity.FILTER_SLOT);
        }

        private void store(NonNullList<ItemStack> list) {
            ItemStack f = filter().copy();
            FilterHeaters.write(f, list);
            items.setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : FilterHeaters.read(filter())) if (!s.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack f = filter();
            return f.isEmpty() ? ItemStack.EMPTY : FilterHeaters.read(f).get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack f = filter();
            if (f.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            NonNullList<ItemStack> list = FilterHeaters.read(f);
            ItemStack out = list.get(slot).split(amount);
            if (!out.isEmpty()) store(list);
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 64);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (filter().isEmpty()) return;
            NonNullList<ItemStack> list = FilterHeaters.read(filter());
            list.set(slot, stack);
            store(list);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            if (cabinet != null) cabinet.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !filter().isEmpty() && FilterHeaters.isHeater(stack);
        }

        @Override
        public void clearContent() {
            if (!filter().isEmpty()) store(NonNullList.withSize(2, ItemStack.EMPTY));
        }
    }

    private final class HeaterSlot extends Slot {
        HeaterSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return hasFilter() && FilterHeaters.isHeater(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return hasFilter();
        }
    }
}
