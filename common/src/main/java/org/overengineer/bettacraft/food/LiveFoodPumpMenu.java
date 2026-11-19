package org.overengineer.bettacraft.food;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.registry.ModMenus;

/**
 * M26: the window of the live food pump: doses per day and portions per dose (- and +), "Dose now", what the pump did
 * last, the food in the breeders next to it and the tanks it reaches. Buttons are vanilla menu button clicks
 * ({@link #clickMenuButton}).
 */
public class LiveFoodPumpMenu extends AbstractContainerMenu {
    public static final int DOSES_DOWN = 0, DOSES_UP = 1, PORTIONS_DOWN = 2, PORTIONS_UP = 3, DOSE_NOW = 4;
    @Nullable
    private final LiveFoodPumpBlockEntity pump;
    private final ContainerData data;

    public LiveFoodPumpMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, null, new SimpleContainerData(LiveFoodPumpBlockEntity.DATA));
    }

    LiveFoodPumpMenu(int id, @Nullable LiveFoodPumpBlockEntity pump, ContainerData data) {
        super(ModMenus.LIVE_FOOD_PUMP.get(), id);
        this.pump = pump;
        this.data = data;
        addDataSlots(data);
    }

    public int doses() { return data.get(0); }
    public int portions() { return data.get(1); }
    public int status() { return data.get(2); }
    public int lastPumped() { return data.get(3); }
    public int food() { return data.get(4); }
    public int tanks() { return data.get(5); }
    public int pending() { return data.get(6); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pump == null) return false;
        switch (id) {
            case DOSES_DOWN -> pump.setDoses(pump.doses() - 1);
            case DOSES_UP -> pump.setDoses(pump.doses() + 1);
            case PORTIONS_DOWN -> pump.setPortions(pump.portions() - 1);
            case PORTIONS_UP -> pump.setPortions(pump.portions() + 1);
            case DOSE_NOW -> pump.doseNow();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return pump == null || (!pump.isRemoved() && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pump.getBlockPos())) <= 64.0);
    }
}
