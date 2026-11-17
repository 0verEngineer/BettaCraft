package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.registry.ModMenus;

/**
 * M49 part a: the window of the sump in a cabinet: its seven chambers (two heaters, the protein skimmer, two filter media, the
 * refugium with Chaetomorpha and its light) over the player inventory. The chambers live in the sump item of the cabinet
 * ({@link Sump#read}); this menu edits them in place, like the heaters of the filter in the cabinet menu.
 */
public class SumpMenu extends AbstractContainerMenu {
    public static final int INV_Y = 84, PLAYER0 = Sump.CHAMBERS;
    /** x, y of each chamber slot. */
    public static final int[][] POS = {{17, 22}, {35, 22}, {71, 22}, {17, 52}, {35, 52}, {107, 52}, {143, 52}};
    @Nullable
    private final CabinetBlockEntity cabinet;

    public SumpMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new SimpleContainer(Sump.CHAMBERS));
        buf.readBlockPos();
    }

    SumpMenu(int id, Inventory inv, CabinetBlockEntity cabinet) {
        this(id, inv, cabinet, null);
    }

    private SumpMenu(int id, Inventory inv, @Nullable CabinetBlockEntity cabinet, @Nullable Container client) {
        super(ModMenus.SUMP.get(), id);
        this.cabinet = cabinet;
        Container chambers = client != null ? client : new Chambers();
        for (int i = 0; i < Sump.CHAMBERS; i++) addSlot(new ChamberSlot(chambers, i, POS[i][0], POS[i][1]));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < PLAYER0) {
            if (!moveItemStackTo(s, PLAYER0, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < Sump.CHAMBERS && !moved; i++) {
                if (Sump.fits(i, s) && (!slots.get(i).hasItem() || ItemStack.isSameItemSameComponents(slots.get(i).getItem(), s))) {
                    moved = moveItemStackTo(s, i, i + 1, false);
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return cabinet == null || (!cabinet.isRemoved() && cabinet.hasSump() && player.distanceToSqr(cabinet.getBlockPos().getCenter()) <= 64.0);
    }

    private static final class ChamberSlot extends Slot {
        ChamberSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return Sump.fits(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return Sump.limit(getContainerSlot());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(stack.getMaxStackSize(), Sump.limit(getContainerSlot()));
        }
    }

    /** The chambers of the sump item in the cabinet (component in and out). */
    private final class Chambers implements Container {
        private ItemStack sump() {
            return cabinet == null ? ItemStack.EMPTY : cabinet.sumpItem();
        }

        private void store(NonNullList<ItemStack> list) {
            ItemStack s = sump();
            if (s.isEmpty() || cabinet == null) return;
            ItemStack n = s.copy();
            Sump.write(n, list);
            cabinet.setSumpItem(n);
        }

        @Override
        public int getContainerSize() {
            return Sump.CHAMBERS;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : Sump.read(sump())) if (!s.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack s = sump();
            return s.isEmpty() ? ItemStack.EMPTY : Sump.read(s).get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = sump();
            if (s.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            NonNullList<ItemStack> list = Sump.read(s);
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
            if (sump().isEmpty()) return;
            NonNullList<ItemStack> list = Sump.read(sump());
            list.set(slot, stack);
            store(list);
        }

        @Override
        public int getMaxStackSize() {
            return Sump.MAX_ALGAE;
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
            return !sump().isEmpty() && Sump.fits(slot, stack);
        }

        @Override
        public void clearContent() {
            if (!sump().isEmpty()) store(NonNullList.withSize(Sump.CHAMBERS, ItemStack.EMPTY));
        }
    }
}
