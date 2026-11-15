package org.overengineer.bettacraft.fertilizer;

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
 * M37: the fertilizer maker window: kelp meal and nutrient salt, the progress, the water tank and the fertilizer tank,
 * the bottle slot and the filled bottle, the spring (or the FE) and the status.
 */
public class FertilizerMakerMenu extends AbstractContainerMenu {
    public static final int MEAL_X = 30, MEAL_Y = 24, SALT_X = 30, SALT_Y = 46, BOTTLE_X = 134, BOTTLE_Y = 20, OUT_X = 134, OUT_Y = 50;
    @Nullable
    private final FertilizerMakerBlockEntity be;
    private final ContainerData data;

    public FertilizerMakerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(FertilizerMakerBlockEntity.SLOTS), new SimpleContainerData(FertilizerMakerBlockEntity.DATA));
    }

    FertilizerMakerMenu(int id, Inventory inv, FertilizerMakerBlockEntity be, ContainerData data) {
        this(id, inv, be, be.items(), data);
    }

    private FertilizerMakerMenu(int id, Inventory inv, @Nullable FertilizerMakerBlockEntity be, IItemHandler items, ContainerData data) {
        super(ModMenus.FERTILIZER_MAKER.get(), id);
        this.be = be;
        this.data = data;
        addSlot(new SlotItemHandler(items, FertilizerMakerBlockEntity.MEAL, MEAL_X, MEAL_Y));
        addSlot(new SlotItemHandler(items, FertilizerMakerBlockEntity.SALT, SALT_X, SALT_Y));
        addSlot(new SlotItemHandler(items, FertilizerMakerBlockEntity.BOTTLES, BOTTLE_X, BOTTLE_Y));
        addSlot(new SlotItemHandler(items, FertilizerMakerBlockEntity.OUTPUT, OUT_X, OUT_Y) {
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
    public boolean powered() { return data.get(5) != 0; }
    /** The spring in percent while it may be wound (-1 otherwise). */
    public int springPercent() { return data.get(6); }
    public int water() { return data.get(7); }
    public int fertilizer() { return data.get(8); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int end = FertilizerMakerBlockEntity.SLOTS;
        if (index < end) {
            if (!moveItemStackTo(s, end, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, 0, FertilizerMakerBlockEntity.OUTPUT, false)) {
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
