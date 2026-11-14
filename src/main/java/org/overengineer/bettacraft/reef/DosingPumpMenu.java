package org.overengineer.bettacraft.reef;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModMenus;

/**
 * M25 part c: the window of the dosing pump: four canister slots (one per channel), per channel - and + (sneaking: by 10)
 * for the mL per day, what the pump does and the values of the tank it doses into. The buttons go to the server as
 * vanilla menu button clicks ({@link #clickMenuButton}: channel x 4 + 0 / 1 / 2 / 3 for -1 / +1 / -10 / +10).
 */
public class DosingPumpMenu extends AbstractContainerMenu {
    public static final int SLOT_Y = 22, INV_Y = 104;
    @Nullable
    private final DosingPumpBlockEntity pump;
    private final ContainerData data;

    public DosingPumpMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(DosingPumpBlockEntity.CHANNELS), new SimpleContainerData(DosingPumpBlockEntity.DATA));
    }

    DosingPumpMenu(int id, Inventory inv, DosingPumpBlockEntity pump, ContainerData data) {
        this(id, inv, pump, pump.items(), data);
    }

    private DosingPumpMenu(int id, Inventory inv, @Nullable DosingPumpBlockEntity pump, IItemHandler items, ContainerData data) {
        super(ModMenus.DOSING_PUMP.get(), id);
        this.pump = pump;
        this.data = data;
        for (int c = 0; c < DosingPumpBlockEntity.CHANNELS; c++) addSlot(new SlotItemHandler(items, c, slotX(c), SLOT_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    /** x of the canister slot of a channel (four columns of 43 pixels). */
    public static int slotX(int channel) {
        return 16 + channel * 43;
    }

    public int perDay(int ch) {
        return data.get(ch);
    }

    public int status() {
        return data.get(4);
    }

    public int tanks() {
        return data.get(5);
    }

    public int calcium() {
        return data.get(6);
    }

    public float kh() {
        return data.get(7) / 10F;
    }

    public int magnesium() {
        return data.get(8);
    }

    public int trace() {
        return data.get(9);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pump == null || id < 0 || id >= DosingPumpBlockEntity.CHANNELS * 4) return false;
        int ch = id / 4, k = id % 4;
        int delta = switch (k) {
            case 0 -> -1;
            case 1 -> 1;
            case 2 -> -10;
            default -> 10;
        };
        pump.setPerDay(ch, pump.perDay(ch) + delta);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int n = DosingPumpBlockEntity.CHANNELS;
        if (index < n) {
            if (!moveItemStackTo(s, n, n + 36, true)) return ItemStack.EMPTY;
        } else {
            if (!(s.getItem() instanceof DosingCanisterItem c) || c.additive() == null || !moveItemStackTo(s, 0, n, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return pump == null || (!pump.isRemoved() && player.distanceToSqr(pump.getBlockPos().getCenter()) <= 64.0);
    }
}
