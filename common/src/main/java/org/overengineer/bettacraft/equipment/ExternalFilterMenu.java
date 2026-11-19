package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.platform.item.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModMenus;

/** Two heater slots of the external filter plus the player inventory (vanilla 3-row container layout). */
public class ExternalFilterMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 71, SLOT_Y = 35;
    @Nullable
    private final ExternalFilterBlockEntity filter;

    public ExternalFilterMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(FilterHeaters.SLOTS));
    }

    ExternalFilterMenu(int id, Inventory inv, ExternalFilterBlockEntity filter) {
        this(id, inv, filter, filter.heaters());
    }

    private ExternalFilterMenu(int id, Inventory inv, @Nullable ExternalFilterBlockEntity filter, IItemHandler heaters) {
        super(ModMenus.EXTERNAL_FILTER.get(), id);
        this.filter = filter;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            addSlot(new SlotItemHandler(heaters, i, SLOT_X + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FilterHeaters.isHeater(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addPowerSlot(filter);
        addDataSlot(spring = org.overengineer.bettacraft.winding.Winding.slot(filter)); // M37
    }

    /** M37: the spring of the filter in percent (-1: not windable now). */
    private net.minecraft.world.inventory.DataSlot spring;

    public int springPercent() {
        return spring.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return powerButton(player, id, filter);
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


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int end = FilterHeaters.SLOTS;
        if (index < end) {
            if (!moveItemStackTo(s, end, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!FilterHeaters.isHeater(s) || !moveItemStackTo(s, 0, end, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return filter == null || (!filter.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(filter.getBlockPos())) <= 64.0);
    }
}
