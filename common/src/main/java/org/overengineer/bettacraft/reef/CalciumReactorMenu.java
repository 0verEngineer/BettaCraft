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
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.platform.item.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;

/** M49 part a: the window of the calcium reactor: the coral rubble slot, what it does and the values of the tank it doses into. */
public class CalciumReactorMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 80, SLOT_Y = 24, INV_Y = 84;
    @Nullable
    private final CalciumReactorBlockEntity reactor;
    private final ContainerData data;

    public CalciumReactorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(1), new SimpleContainerData(CalciumReactorBlockEntity.DATA));
    }

    CalciumReactorMenu(int id, Inventory inv, CalciumReactorBlockEntity reactor, ContainerData data) {
        this(id, inv, reactor, reactor.items(), data);
    }

    private CalciumReactorMenu(int id, Inventory inv, @Nullable CalciumReactorBlockEntity reactor, IItemHandler items, ContainerData data) {
        super(ModMenus.CALCIUM_REACTOR.get(), id);
        this.reactor = reactor;
        this.data = data;
        addSlot(new SlotItemHandler(items, 0, SLOT_X, SLOT_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public int status() {
        return data.get(0);
    }

    public int tanks() {
        return data.get(1);
    }

    public int calcium() {
        return data.get(2);
    }

    public float kh() {
        return data.get(3) / 10F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index == 0) {
            if (!moveItemStackTo(s, 1, 37, true)) return ItemStack.EMPTY;
        } else if (!s.is(ModItems.CORAL_RUBBLE.get()) || !moveItemStackTo(s, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return reactor == null || (!reactor.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(reactor.getBlockPos())) <= 64.0);
    }
}
