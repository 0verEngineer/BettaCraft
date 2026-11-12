package org.overengineer.bettacraft.tank.menu;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.equipment.HeaterItem;
import org.overengineer.bettacraft.network.TankViewPayload;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * Tank menu (PLAN 8): heater equipment slots (M1; heaters move into the interior in M3) plus the player inventory.
 * Everything else is shown from a {@link TankView} that is synced only while the menu is open and only when it
 * changed. All actions go through {@link TankActions} and are validated on the server.
 */
public class TankMenu extends AbstractContainerMenu {
    public static final int HEATER_X = 12, SLOT_Y = 148, INV_X = 44, HOTBAR_Y = 206;
    private static final int SYNC_INTERVAL = 10;

    private final BlockPos controllerPos;
    private final Player player;
    @Nullable
    private final TankBlockEntity controller;
    @Nullable
    private final TankData tankAtOpen;
    private final Container heaters;
    @Nullable
    private CompoundTag lastSent;
    private int syncCooldown;
    /** Client: latest view from the server. */
    @Nullable
    private TankView view;

    /** Client constructor. */
    public TankMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), null, new SimpleContainer(TankData.HEATER_SLOTS));
    }

    /** Server constructor. */
    public TankMenu(int id, Inventory inv, TankBlockEntity controller) {
        this(id, inv, controller.getBlockPos(), controller, controller.getTank().heaters());
    }

    private TankMenu(int id, Inventory inv, BlockPos pos, @Nullable TankBlockEntity controller, Container heaters) {
        super(ModMenus.TANK.get(), id);
        this.controllerPos = pos;
        this.player = inv.player;
        this.controller = controller;
        this.tankAtOpen = controller == null ? null : controller.getTank();
        this.heaters = heaters;
        for (int i = 0; i < TankData.HEATER_SLOTS; i++) addSlot(new HeaterSlot(heaters, i, HEATER_X, SLOT_Y + i * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, SLOT_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, HOTBAR_Y));
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    @Nullable
    public TankBlockEntity controller() {
        return controller;
    }

    @Nullable
    public TankView view() {
        return view;
    }

    public void acceptView(TankView v) {
        this.view = v;
    }

    boolean canModify() {
        if (controller != null) return TankInhabitants.canModify(player, controller.getTank());
        return view == null || view.canModify;
    }

    /** Forces a view update in the next tick (after an action). */
    public void requestSync() {
        syncCooldown = 0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (controller == null || !(player instanceof ServerPlayer sp)) return;
        if (--syncCooldown > 0) return;
        syncCooldown = SYNC_INTERVAL;
        if (!stillValid(player)) return;
        CompoundTag tag = TankView.build(sp, controller);
        if (!tag.equals(lastSent)) {
            lastSent = tag;
            PacketDistributor.sendToPlayer(sp, new TankViewPayload(containerId, tag));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller == null) return true;
        if (controller.isRemoved() || !controller.isController() || controller.getTank() != tankAtOpen) return false;
        TankBlockEntity c = TankMultiblock.getController(player.level(), controllerPos);
        if (c != controller) return false;
        return TankInhabitants.tankBox(controller.getTank()).inflate(8).contains(player.position());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int heaterEnd = TankData.HEATER_SLOTS;
        if (index < heaterEnd) {
            if (!moveItemStackTo(s, heaterEnd, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!(s.getItem() instanceof HeaterItem) || !canModify() || !moveItemStackTo(s, 0, heaterEnd, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    private final class HeaterSlot extends Slot {
        HeaterSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof HeaterItem && canModify();
        }

        @Override
        public boolean mayPickup(Player p) {
            return canModify();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (controller != null) controller.setChanged();
        }
    }
}
