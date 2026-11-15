package org.overengineer.bettacraft.crucible;

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

/** M24: crucible menu: four inputs (2x2), fuel, output and the player inventory; progress and heat as data slots. */
public class CrucibleMenu extends AbstractContainerMenu {
    public static final int[][] INPUT_XY = {{38, 17}, {56, 17}, {38, 35}, {56, 35}};
    public static final int FUEL_X = 47, FUEL_Y = 53, OUT_X = 116, OUT_Y = 26;
    @Nullable
    private final CrucibleBlockEntity be;
    private final ContainerData data;

    public CrucibleMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(CrucibleBlockEntity.SLOTS), new SimpleContainerData(5));
    }

    CrucibleMenu(int id, Inventory inv, CrucibleBlockEntity be, ContainerData data) {
        this(id, inv, be, be.items(), data);
    }

    private CrucibleMenu(int id, Inventory inv, @Nullable CrucibleBlockEntity be, IItemHandler items, ContainerData data) {
        super(ModMenus.CRUCIBLE.get(), id);
        this.be = be;
        this.data = data;
        for (int i = 0; i < CrucibleBlockEntity.INPUTS; i++) addSlot(new SlotItemHandler(items, i, INPUT_XY[i][0], INPUT_XY[i][1]));
        addSlot(new SlotItemHandler(items, CrucibleBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CrucibleBlockEntity.fuelTicks(stack) > 0;
            }
        });
        addSlot(new SlotItemHandler(items, CrucibleBlockEntity.OUTPUT, OUT_X + 4, OUT_Y + 4) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public int meltTicks() { return Math.max(1, data.get(1)); }
    public int heat() { return data.get(2); }
    public int heatMax() { return Math.max(1, data.get(3)); }
    public CrucibleBlockEntity.Heat source() {
        int i = data.get(4);
        return CrucibleBlockEntity.Heat.values()[Math.max(0, Math.min(CrucibleBlockEntity.Heat.values().length - 1, i))];
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int end = CrucibleBlockEntity.SLOTS;
        if (index < end) {
            if (!moveItemStackTo(s, end, slots.size(), true)) return ItemStack.EMPTY;
        } else if (CrucibleBlockEntity.fuelTicks(s) > 0) {
            if (!moveItemStackTo(s, CrucibleBlockEntity.FUEL, CrucibleBlockEntity.FUEL + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, 0, CrucibleBlockEntity.INPUTS, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0);
    }
}
