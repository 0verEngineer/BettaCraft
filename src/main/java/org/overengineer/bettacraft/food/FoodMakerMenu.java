package org.overengineer.bettacraft.food;

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
 * M26 part b: the food maker window: four inputs (2x2), the output, the product (automatic or one recipe, &lt; and &gt;),
 * the progress, the spring or the FE and the status. Buttons are vanilla menu button clicks ({@link #clickMenuButton}).
 */
public class FoodMakerMenu extends AbstractContainerMenu {
    public static final int PREV = 0, NEXT = 1;
    public static final int[][] INPUT_XY = {{26, 32}, {44, 32}, {26, 50}, {44, 50}};
    public static final int OUT_X = 112, OUT_Y = 41;
    @Nullable
    private final FoodMakerBlockEntity be;
    private final ContainerData data;

    public FoodMakerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(FoodMakerBlockEntity.SLOTS), new SimpleContainerData(FoodMakerBlockEntity.DATA));
    }

    FoodMakerMenu(int id, Inventory inv, FoodMakerBlockEntity be, ContainerData data) {
        this(id, inv, be, be.items(), data);
    }

    private FoodMakerMenu(int id, Inventory inv, @Nullable FoodMakerBlockEntity be, IItemHandler items, ContainerData data) {
        super(ModMenus.FOOD_MAKER.get(), id);
        this.be = be;
        this.data = data;
        for (int i = 0; i < FoodMakerBlockEntity.INPUTS; i++) addSlot(new SlotItemHandler(items, i, INPUT_XY[i][0], INPUT_XY[i][1]));
        addSlot(new SlotItemHandler(items, FoodMakerBlockEntity.OUTPUT, OUT_X + 4, OUT_Y + 4) {
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
    public int batchTicks() { return Math.max(1, data.get(1)); }
    public int drive() { return data.get(2); }
    public int driveMax() { return Math.max(1, data.get(3)); }
    public int status() { return data.get(4); }
    /** 0 = automatic, else 1 + index in {@link FoodMakerBlockEntity#recipes}. */
    public int selected() { return data.get(5); }
    public boolean powered() { return data.get(6) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return false;
        switch (id) {
            case PREV -> be.cycle(-1);
            case NEXT -> be.cycle(1);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int end = FoodMakerBlockEntity.SLOTS;
        if (index < end) {
            if (!moveItemStackTo(s, end, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, 0, FoodMakerBlockEntity.INPUTS, false)) {
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
