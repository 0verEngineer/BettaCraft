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
import org.overengineer.bettacraft.network.TankViewPayload;
import org.overengineer.bettacraft.registry.ModMenus;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * Tank menu (PLAN 8): the player inventory and the four old heater slots (M1-M3). Since M4 heaters belong into the
 * interior or an external filter; heaters still left in the old slots can only be taken out (and are moved into an
 * attached filter automatically, see {@code TankEquipment#moveLegacyHeaters}).
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
        this(id, inv, buf.readBlockPos(), null, new SimpleContainer(TankData.LEGACY_HEATER_SLOTS));
    }

    /** Server constructor. */
    public TankMenu(int id, Inventory inv, TankBlockEntity controller) {
        this(id, inv, controller.getBlockPos(), controller, controller.getTank().legacyHeaters());
    }

    private TankMenu(int id, Inventory inv, BlockPos pos, @Nullable TankBlockEntity controller, Container heaters) {
        super(ModMenus.TANK.get(), id);
        this.controllerPos = pos;
        this.player = inv.player;
        this.controller = controller;
        this.tankAtOpen = controller == null ? null : controller.getTank();
        this.heaters = heaters;
        for (int i = 0; i < TankData.LEGACY_HEATER_SLOTS; i++) addSlot(new LegacyHeaterSlot(heaters, i, HEATER_X, SLOT_Y + i * 18));
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
        // M17: level and distance first; looking the controller up in another dimension could load a chunk there
        if (player.level() != controller.getLevel()) return false;
        if (!TankInhabitants.tankBox(controller.getTank()).inflate(8).contains(player.position())) return false;
        TankBlockEntity c = TankMultiblock.getController(player.level(), controllerPos);
        return c == controller;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int heaterEnd = TankData.LEGACY_HEATER_SLOTS;
        if (index < heaterEnd) {
            if (!canModify() || !moveItemStackTo(s, heaterEnd, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY; // nothing goes into the old heater slots any more
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Old heater slot: take-out only, shown only while it holds a heater. */
    private final class LegacyHeaterSlot extends Slot {
        LegacyHeaterSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return hasItem();
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
