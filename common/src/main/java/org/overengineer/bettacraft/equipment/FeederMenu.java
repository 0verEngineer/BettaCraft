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
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.platform.item.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Food slots and a button that cycles 1-3 feedings per day. The feeder on top of a tank has 3x3 slots (dispenser layout);
 * M66: the clip-on feeder has this menu too, with two slots. The panel is 176 x 190 (room for the hint that the feeder takes
 * food from a cabinet under the tank, too).
 */
public class FeederMenu extends AbstractContainerMenu {
    public static final int BUTTON_CYCLE = 0;
    /** The inventory (M66: 22 px under the vanilla layout) and the size of the panel. */
    public static final int INV_X = 8, INV_Y = 108, WIDTH = 176, HEIGHT = 190;
    @Nullable
    private final FeederBlockEntity feeder;
    private final ContainerData data;
    private final int slotCount;

    public FeederMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(ModMenus.FEEDER.get(), FeederBlockEntity.SLOTS, id, inv, null, new ItemStackHandler(FeederBlockEntity.SLOTS), new SimpleContainerData(1));
    }

    /** M66: the client side of the clip-on feeder's menu (two slots). */
    public static FeederMenu clip(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return new FeederMenu(ModMenus.CLIP_FEEDER.get(), ClipFeederBlockEntity.SLOTS, id, inv, null, new ItemStackHandler(ClipFeederBlockEntity.SLOTS), new SimpleContainerData(1));
    }

    FeederMenu(int id, Inventory inv, FeederBlockEntity feeder, ContainerData data) {
        this(feeder.menuType(), feeder.items().getSlots(), id, inv, feeder, feeder.items(), data);
    }

    private FeederMenu(net.minecraft.world.inventory.MenuType<FeederMenu> type, int slots, int id, Inventory inv, @Nullable FeederBlockEntity feeder, IItemHandler items,
                       ContainerData data) {
        super(type, id);
        this.feeder = feeder;
        this.data = data;
        this.slotCount = slots;
        for (int i = 0; i < slots; i++) {
            int[] at = slotAt(slots, i);
            addSlot(new SlotItemHandler(items, i, at[0], at[1]));
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    /** Where slot {@code i} of {@code n} sits: the 3x3 grid (dispenser layout), or two side by side in the middle row. */
    public static int[] slotAt(int n, int i) {
        return n == 2 ? new int[]{71 + i * 18, 35} : new int[]{62 + i % 3 * 18, 17 + i / 3 * 18};
    }

    public int slotCount() {
        return slotCount;
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
        if (index < slotCount) {
            if (!moveItemStackTo(s, slotCount, slotCount + 36, true)) return ItemStack.EMPTY;
        } else {
            if (!(s.is(ModTags.BETTA_FOOD) || s.is(net.minecraft.tags.ItemTags.FISHES)) || !moveItemStackTo(s, 0, slotCount, false)) return ItemStack.EMPTY; // M30 part c: raw fish for dolphins
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return feeder == null || (!feeder.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(feeder.getBlockPos())) <= 64.0);
    }
}
