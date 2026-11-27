package org.overengineer.bettacraft.crucible;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModRecipes;

/**
 * M24: the crucible. Four input slots, a fuel slot and the output. It melts while it is hot: lava (or a lava cauldron
 * or magma block) right under it heats it for free, FE (power mods: {@code crucible.fePerTick}) or fuel otherwise: a
 * lava bucket (100 s of heat, the bucket stays), a blaze rod (12 s) or blaze powder (6 s). Hoppers and pipes of other
 * mods put things in from the top and the sides (fuel only into the fuel slot) and take the result from below.
 */
public class CrucibleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUTS = CrucibleRecipe.INPUTS, FUEL = 4, OUTPUT = 5, SLOTS = 6;
    public enum Heat { NONE, LAVA, FE, FUEL }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == OUTPUT) return false;
            if (slot == FUEL) return fuelTicks(stack) > 0;
            return true;
        }
    };
    private final DeviceEnergy energy = new DeviceEnergy(50000, this::setChanged);
    private int progress, heat, heatMax;
    private Heat source = Heat.NONE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> meltTicks();
                case 2 -> heat;
                case 3 -> heatMax;
                default -> source.ordinal();
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public DeviceEnergy energy() {
        return energy;
    }

    public int progress() {
        return progress;
    }

    public Heat source() {
        return source;
    }

    public static int meltTicks() {
        return ServerConfig.getOr(ServerConfig.CRUCIBLE_TICKS, 200);
    }

    /** Heat of one fuel item in ticks (0 = no fuel for the crucible). */
    public static int fuelTicks(ItemStack s) {
        if (s.is(Items.LAVA_BUCKET)) return 2000;
        if (s.is(Items.BLAZE_ROD)) return 240;
        if (s.is(Items.BLAZE_POWDER)) return 120;
        return 0;
    }

    /** Whether lava, a lava cauldron or a magma block is right under the crucible. */
    public static boolean lavaBelow(Level level, BlockPos pos) {
        BlockState b = level.getBlockState(pos.below());
        return b.getFluidState().is(FluidTags.LAVA) || b.is(Blocks.LAVA_CAULDRON) || b.is(Blocks.MAGMA_BLOCK);
    }

    /** Hoppers and pipes: inputs from the top and the sides (fuel only into the fuel slot), the result from below. */
    public IItemHandler automation(@Nullable Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (side == Direction.DOWN || slot == OUTPUT) return stack;
                if (fuelTicks(stack) > 0 && slot != FUEL) return stack;
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != OUTPUT && items.isItemValid(slot, stack);
            }
        };
    }

    private List<ItemStack> inputs() {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) l.add(items.getStackInSlot(i));
        return l;
    }

    @Nullable
    private RecipeHolder<CrucibleRecipe> recipe(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return null; // M52b: the recipes are on the server
        Optional<RecipeHolder<CrucibleRecipe>> r = sl.recipeAccess().getRecipeFor(ModRecipes.CRUCIBLE_TYPE.get(),
                new CrucibleRecipe.Input(CrucibleRecipe.copy(inputs())), level);
        return r.orElse(null);
    }

    private boolean fits(ItemStack result) {
        ItemStack out = items.getStackInSlot(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** One tick (public for the GameTests). */
    public void tick(Level level, BlockPos pos, BlockState state) {
        RecipeHolder<CrucibleRecipe> r = Features.enabled(Feature.STAINLESS_STEEL) ? recipe(level) : null;
        boolean ready = r != null && fits(r.value().result());
        Heat now = Heat.NONE;
        if (ready) {
            int fe = org.overengineer.bettacraft.tank.TankPower.feInt(ServerConfig.getOr(ServerConfig.CRUCIBLE_FE, 20)); // M67: power.costs
            if (lavaBelow(level, pos)) now = Heat.LAVA;
            else if (energyOffered() && energy.getEnergyStored() >= fe) {
                energy.use(fe);
                now = Heat.FE;
            } else {
                if (heat <= 0) ignite();
                if (heat > 0) now = Heat.FUEL;
            }
        }
        if (heat > 0 && (now == Heat.FUEL || !ready)) heat--; // fuel burns down once lit (like a furnace)
        if (now != Heat.NONE) {
            progress++;
            if (progress >= meltTicks()) {
                melt(r.value());
                progress = 0;
            }
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2); // it cools down
        }
        if (now != source) {
            source = now;
            setChanged();
        }
        boolean lit = now != Heat.NONE;
        if (state.getValue(CrucibleBlock.LIT) != lit) level.setBlock(pos, state.setValue(CrucibleBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private void ignite() {
        ItemStack f = items.getStackInSlot(FUEL);
        int t = fuelTicks(f);
        if (t <= 0) return;
        heat = heatMax = t;
        if (f.is(Items.LAVA_BUCKET)) items.setStackInSlot(FUEL, new ItemStack(Items.BUCKET));
        else f.shrink(1);
        setChanged();
    }

    private void melt(CrucibleRecipe r) {
        int[] take = r.take(inputs());
        if (take == null) return;
        for (int i = 0; i < INPUTS; i++) {
            if (take[i] <= 0) continue;
            ItemStack s = items.getStackInSlot(i);
            ItemStack rem = org.overengineer.bettacraft.platform.Platform.get().craftingRemainder(s);
            s.shrink(take[i]);
            if (s.isEmpty() && !rem.isEmpty()) items.setStackInSlot(i, rem);
        }
        ItemStack out = items.getStackInSlot(OUTPUT);
        if (out.isEmpty()) items.setStackInSlot(OUTPUT, r.result().copy());
        else out.grow(r.result().getCount());
        setChanged();
    }

    /** FE is taken while any power option is on (also the tank power of M9). */
    public static boolean energyOffered() {
        return DeviceEnergy.required() || org.overengineer.bettacraft.tank.TankPower.anyRequired();
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.crucible");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrucibleMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
        tag.putInt("heat_max", heatMax);
        energy.save(tag);
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag tag = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompoundOrEmpty("items"));
        progress = tag.getIntOr("progress", 0);
        heat = tag.getIntOr("heat", 0);
        heatMax = tag.getIntOr("heat_max", 0);
        energy.load(tag);
    }
}
