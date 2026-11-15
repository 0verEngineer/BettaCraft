package org.overengineer.bettacraft.equipment;

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
import org.overengineer.bettacraft.registry.ModTags;

/** 3x3 food slots (dispenser layout) and a button that cycles 1-3 feedings per day. */
public class FeederMenu extends AbstractContainerMenu {
    public static final int BUTTON_CYCLE = 0;
    @Nullable
    private final FeederBlockEntity feeder;
    private final ContainerData data;

    public FeederMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(FeederBlockEntity.SLOTS), new SimpleContainerData(1));
    }

    FeederMenu(int id, Inventory inv, FeederBlockEntity feeder, ContainerData data) {
        this(id, inv, feeder, feeder.items(), data);
    }

    private FeederMenu(int id, Inventory inv, @Nullable FeederBlockEntity feeder, IItemHandler items, ContainerData data) {
        super(ModMenus.FEEDER.get(), id);
        this.feeder = feeder;
        this.data = data;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                addSlot(new SlotItemHandler(items, c + r * 3, 62 + c * 18, 17 + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int feedingsPerDay() {
        return Math.max(1, data.get(0));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE && feeder != null) {
            feeder.cycleFeedings();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < 9) {
            if (!moveItemStackTo(s, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            if (!(s.is(ModTags.BETTA_FOOD) || s.is(net.minecraft.tags.ItemTags.FISHES)) || !moveItemStackTo(s, 0, 9, false)) return ItemStack.EMPTY; // M30 part c: raw fish for dolphins
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return feeder == null || (!feeder.isRemoved() && player.distanceToSqr(feeder.getBlockPos().getCenter()) <= 64.0);
    }
}
