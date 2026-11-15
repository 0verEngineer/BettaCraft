package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.DeviceEnergy;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModRecipes;
import org.overengineer.bettacraft.registry.ModSounds;

/**
 * M26 part b: the food maker. Four input slots and the output; it makes one batch of a {@link FoodMakerRecipe} in
 * {@code foodMaker.ticksPerBatch} ticks. The product is chosen in the window, or "automatic": the recipe with the most
 * ingredients that the inputs hold (so a pattern of AE2 / RS only has to put the ingredients in).
 * <p>
 * Drive: a spring that the crank winds (M37, {@link org.overengineer.bettacraft.winding.Winding}; {@code foodMaking.windsToFull}
 * clicks, {@code foodMaking.springTicks} ticks of work when full: one in-game day). While a power option is on (like the
 * crucible) it takes {@code foodMaking.makerFePerTick} FE while it works, and its spring when the FE is short
 * ({@code features.power.winding}). Redstone pauses it.
 * <p>
 * Hoppers, pipes of other mods, AE2 / RS buses and interfaces put ingredients into the inputs (from every side) and take
 * the food from the output ({@link #automation()}).
 */
public class FoodMakerBlockEntity extends BlockEntity implements MenuProvider, org.overengineer.bettacraft.winding.Windable {
    public static final int INPUTS = FoodMakerRecipe.INPUTS, OUTPUT = 4, SLOTS = 5, DATA = 8;
    public static final int IDLE = 0, WORKING = 1, NO_RECIPE = 2, OUTPUT_FULL = 3, WIND_IT = 4, NO_POWER = 5, PAUSED = 6, OFF = 7,
            /** M37: no FE and the spring is down: connect FE or wind it. */
            FE_OR_WIND = 8;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != OUTPUT;
        }
    };
    private final DeviceEnergy energy = new DeviceEnergy(20000, this::setChanged);
    /** The chosen recipe (null = automatic). */
    @Nullable
    private ResourceLocation selected;
    private int progress, status = IDLE;
    /** M37: the spring (device-ticks; the crank winds it). */
    private final org.overengineer.bettacraft.winding.WindSpring spring =
            org.overengineer.bettacraft.winding.WindSpring.of(org.overengineer.bettacraft.winding.Winding.Device.FOOD_MAKER, this::setChanged);
    /** M32 part c: share of full work Create's rotation gives (looked at once a second; transient). */
    private float turned;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> batchTicks();
                case 2 -> powered() ? energy.getEnergyStored() : (int) Math.round(spring.fill() * 1000);
                case 3 -> powered() ? energy.getMaxEnergyStored() : 1000;
                case 4 -> status;
                case 5 -> level == null ? 0 : selectedIndex(level.getRecipeManager());
                case 6 -> powered() ? 1 : 0;
                case 7 -> org.overengineer.bettacraft.winding.Winding.Device.FOOD_MAKER.windable() ? spring.percent() : -1; // M37
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {}

        @Override
        public int getCount() {
            return DATA;
        }
    };

    public FoodMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_MAKER.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public DeviceEnergy energy() {
        return energy;
    }

    public int progress() { return progress; }
    public int spring() { return (int) Math.min(Integer.MAX_VALUE, spring.ticks()); }

    @Override
    public org.overengineer.bettacraft.winding.WindSpring windSpring() {
        return spring;
    }

    @Override
    public org.overengineer.bettacraft.winding.Winding.Device windDevice() {
        return org.overengineer.bettacraft.winding.Winding.Device.FOOD_MAKER;
    }
    public int status() { return status; }

    public static int batchTicks() {
        return ServerConfig.getOr(ServerConfig.FOOD_MAKER_TICKS, 200);
    }

    public static int springMax() {
        return (int) org.overengineer.bettacraft.winding.Winding.Device.FOOD_MAKER.capacity();
    }

    /** Runs on FE (no spring) while any power option is on. */
    public static boolean powered() {
        return org.overengineer.bettacraft.crucible.CrucibleBlockEntity.energyOffered();
    }

    public void setSpring(int ticks) {
        spring.set(ticks);
    }

    // ------------------------------------------------------------------ recipes

    /** Every recipe of the food maker in a fixed order (by id): the list the window steps through. */
    public static List<RecipeHolder<FoodMakerRecipe>> recipes(RecipeManager rm) {
        List<RecipeHolder<FoodMakerRecipe>> l = new ArrayList<>(rm.getAllRecipesFor(ModRecipes.FOOD_MAKER_TYPE.get()));
        l.sort(Comparator.comparing(h -> h.id().toString()));
        return l;
    }

    /** 0 = automatic, else 1 + the index in {@link #recipes}. */
    public int selectedIndex(RecipeManager rm) {
        if (selected == null) return 0;
        List<RecipeHolder<FoodMakerRecipe>> l = recipes(rm);
        for (int i = 0; i < l.size(); i++) if (l.get(i).id().equals(selected)) return i + 1;
        return 0;
    }

    /** Steps the choice of product (-1 / +1), through "automatic" and every recipe. */
    public void cycle(int dir) {
        if (level == null) return;
        List<RecipeHolder<FoodMakerRecipe>> l = recipes(level.getRecipeManager());
        int n = l.size() + 1;
        int i = Math.floorMod(selectedIndex(level.getRecipeManager()) + dir, n);
        select(i == 0 ? null : l.get(i - 1).id());
    }

    public void select(@Nullable ResourceLocation id) {
        if (java.util.Objects.equals(id, selected)) return;
        selected = id;
        progress = 0;
        setChanged();
    }

    @Nullable
    public ResourceLocation selected() {
        return selected;
    }

    private List<ItemStack> inputs() {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) l.add(items.getStackInSlot(i));
        return l;
    }

    /** The recipe it makes now: the chosen one if the inputs hold it, or (automatic) the one with the most ingredients. */
    @Nullable
    public FoodMakerRecipe current(Level level) {
        List<ItemStack> in = FoodMakerRecipe.copy(inputs());
        FoodMakerRecipe best = null;
        for (RecipeHolder<FoodMakerRecipe> h : recipes(level.getRecipeManager())) {
            if (selected != null && !h.id().equals(selected)) continue;
            if (h.value().take(in) == null) continue;
            if (best == null || weight(h.value()) > weight(best)) best = h.value();
        }
        return best;
    }

    private static int weight(FoodMakerRecipe r) {
        int n = 0;
        for (var si : r.ingredients()) n += 100 + si.count();
        return n;
    }

    private boolean fits(ItemStack result) {
        ItemStack out = items.getStackInSlot(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    // ------------------------------------------------------------------ work

    static void serverTick(Level level, BlockPos pos, BlockState state, FoodMakerBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** One tick (public for the GameTests). */
    public void tick(Level level, BlockPos pos, BlockState state) {
        int now = step(level, pos);
        if (now != status) {
            status = now;
            setChanged();
        }
        boolean working = now == WORKING;
        if (state.hasProperty(FoodMakerBlock.WORKING) && state.getValue(FoodMakerBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(FoodMakerBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    private int step(Level level, BlockPos pos) {
        if (!Features.enabled(Feature.FOOD_MACHINE)) return OFF;
        if (level.hasNeighborSignal(pos)) return PAUSED;
        FoodMakerRecipe r = current(level);
        if (r == null) {
            if (progress > 0) progress = 0;
            return NO_RECIPE;
        }
        if (!fits(r.result())) return OUTPUT_FULL;
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) turned = org.overengineer.bettacraft.compat.create.CreateKinetics.drive(level, pos);
        if (powered()) {
            int fe = ServerConfig.getOr(ServerConfig.FOOD_MAKER_FE, 10);
            if (turned > 0F) energy.drive(level, pos, fe); // M32 part c: Create's rotation instead of FE
            if (!energy.use(fe)) {
                // M37: FE first, then the spring (features.power.winding)
                if (!org.overengineer.bettacraft.winding.Winding.enabled()) return NO_POWER;
                if (!spring.use(1)) return FE_OR_WIND;
            }
        } else {
            // M32 part c: Create's rotation winds the spring (at full speed twice as fast as the machine uses it)
            if (turned > 0F) spring.set(spring.ticks() + (int) Math.ceil(2F * turned));
            if (!spring.use(1)) return WIND_IT;
        }
        progress++;
        if (progress >= batchTicks()) {
            make(r);
            progress = 0;
        }
        setChanged();
        return WORKING;
    }

    private void make(FoodMakerRecipe r) {
        int[] take = r.take(inputs());
        if (take == null) return;
        for (int i = 0; i < INPUTS; i++) {
            if (take[i] <= 0) continue;
            ItemStack s = items.getStackInSlot(i);
            ItemStack rem = s.getCraftingRemainingItem();
            items.extractItem(i, take[i], false);
            if (items.getStackInSlot(i).isEmpty() && !rem.isEmpty()) items.setStackInSlot(i, rem);
        }
        ItemStack out = items.getStackInSlot(OUTPUT);
        if (out.isEmpty()) items.setStackInSlot(OUTPUT, r.result().copy());
        else {
            ItemStack grown = out.copy();
            grown.grow(r.result().getCount());
            items.setStackInSlot(OUTPUT, grown);
        }
    }

    /**
     * M37: one click of the crank ({@link org.overengineer.bettacraft.winding.Winding#wind}): winds the spring a step (the
     * crank turns a quarter, the ratchet clicks). False when it may not be wound now or is full.
     */
    public boolean wind(@Nullable Player player) {
        if (level == null) return false;
        return org.overengineer.bettacraft.winding.Winding.wind(level, worldPosition, player) == org.overengineer.bettacraft.winding.Winding.Result.WOUND;
    }

    @Override
    public void onWound() {
        BlockState st = getBlockState();
        if (level != null && st.hasProperty(FoodMakerBlock.CRANK)) {
            level.setBlock(worldPosition, st.setValue(FoodMakerBlock.CRANK, (st.getValue(FoodMakerBlock.CRANK) + 1) % 4), Block.UPDATE_CLIENTS);
        }
    }

    /** Comparator: how full the output slot is. */
    public int comparator() {
        ItemStack out = items.getStackInSlot(OUTPUT);
        return out.isEmpty() ? 0 : 1 + Math.round(14F * out.getCount() / out.getMaxStackSize());
    }

    /** For hoppers, pipes and storage mods: ingredients in from every side, the food out of the output slot. */
    public IItemHandler automation() {
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
                return slot == OUTPUT ? stack : items.insertItem(slot, stack, simulate);
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
                return slot != OUTPUT;
            }
        };
    }

    public static Component statusText(int status) {
        return Component.translatable("gui.bettacraft.food_maker.status." + switch (status) {
            case WORKING -> "working";
            case NO_RECIPE -> "no_recipe";
            case OUTPUT_FULL -> "output_full";
            case WIND_IT -> "wind";
            case NO_POWER -> "no_power";
            case FE_OR_WIND -> "fe_or_wind";
            case PAUSED -> "paused";
            case OFF -> "off";
            default -> "idle";
        });
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bettacraft.food_maker");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FoodMakerMenu(id, inv, this, data);
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
        spring.save(tag, "spring");
        if (selected != null) tag.putString("recipe", selected.toString());
        energy.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompound("items"));
        progress = Math.max(0, tag.getInt("progress"));
        spring.load(tag, "spring");
        selected = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        energy.load(tag);
    }
}
