package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.platform.item.ItemStackHandler;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * Larva tub / artemia hatchery (M9). Slots: 0 = ingredient (organic matter, or artemia cysts), 1 = empty glass bottles
 * (hatchery: the nauplii are filled into them), 2 = food ready to take. Water in mB (larva tub: water bucket or any
 * water pipe; hatchery: salt water bottles, the empty bottle goes to slot 1).
 * <p>
 * One batch takes {@code foodBreeding.larvaTubDays} (larva tub: needs the open sky; 1.5x as fast at night, 1.3x in a
 * betta biome and 1.3x in the rain) or {@code foodBreeding.hatcheryDays} (hatchery: needs light 9+). It uses one
 * ingredient (one jar of cysts) and {@link #WATER_PER_BATCH} mB and yields the configured amount of food. Time stands
 * still while unloaded unless {@code simulateUnloadedTime} is on.
 */
public class CultureTubBlockEntity extends BlockEntity {
    public static final int INPUT = 0, BOTTLES = 1, OUTPUT = 2, SLOTS = 3;
    public static final int WATER_CAPACITY = 1000, WATER_PER_BATCH = 250, SALT_WATER_BOTTLE_MB = 250, OUTPUT_MAX = 16;
    public static final int MIN_LIGHT = 9;
    private static final int INTERVAL = 20;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT -> isIngredient(stack);
                case BOTTLES -> artemia() && stack.is(Items.GLASS_BOTTLE);
                default -> true;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == OUTPUT ? OUTPUT_MAX : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == OUTPUT) updateState();
        }
    };
    private int water;
    /**
     * M21.8: a jar of cysts split off a stack in the input slot and opened. M35.1: jars have no uses any more; an open jar of
     * an old world goes back onto the input slot on load, or (if it does not fit) waits here and is used up by the next batch.
     */
    private ItemStack opened = ItemStack.EMPTY;
    private double progress;
    private long lastTick = -1;

    /** Automation view: ingredients and bottles in (salt water bottles become water), food out (slot 2 only). */
    private final IItemHandler automation = new IItemHandler() {
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
            if (stack.isEmpty()) return stack;
            if (slot == INPUT && artemia() && stack.is(ModItems.SALT_WATER_BOTTLE.get())) {
                if (water + SALT_WATER_BOTTLE_MB > WATER_CAPACITY) return stack;
                ItemStack bottles = items.getStackInSlot(BOTTLES);
                if (!bottles.isEmpty() && bottles.getCount() >= 64) return stack;
                if (!simulate) addSaltWaterBottle();
                ItemStack rest = stack.copy();
                rest.shrink(1);
                return rest;
            }
            if (slot == OUTPUT) return stack;
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
            if (slot == INPUT && artemia() && stack.is(ModItems.SALT_WATER_BOTTLE.get())) return true;
            return slot != OUTPUT && items.isItemValid(slot, stack);
        }
    };

    /** Water from pipes (larva tub only; there is no salt water fluid). */
    private final IFluidHandler fluid = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return water <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, water);
        }

        @Override
        public int getTankCapacity(int tank) {
            return WATER_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int n = Math.min(resource.getAmount(), WATER_CAPACITY - water);
            if (n > 0 && action.execute()) {
                water += n;
                setChanged();
                updateState();
            }
            return Math.max(0, n);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public CultureTubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CULTURE_TUB.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public CultureTubBlock.Kind kind() {
        return getBlockState().getBlock() instanceof CultureTubBlock b ? b.kind() : CultureTubBlock.Kind.LARVAE;
    }

    public boolean artemia() {
        return kind() == CultureTubBlock.Kind.ARTEMIA;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automation() {
        return automation;
    }

    @Nullable
    public IFluidHandler fluid() {
        return artemia() ? null : fluid;
    }

    public int water() {
        return water;
    }

    public void setWater(int mb) {
        water = Math.max(0, Math.min(WATER_CAPACITY, mb));
        setChanged();
        updateState();
    }

    /** Progress of the current batch (0..1). */
    public double progress() {
        return progress;
    }

    public boolean isIngredient(ItemStack stack) {
        return artemia() ? stack.is(ModItems.ARTEMIA_CYSTS.get()) : stack.is(ModTags.LARVA_TUB_FOOD);
    }

    public ItemStack result() {
        return new ItemStack(artemia() ? ModItems.ARTEMIA_NAUPLII.get() : ModItems.MOSQUITO_LARVAE.get());
    }

    public int batchYield() {
        return artemia() ? ServerConfig.getOr(ServerConfig.CULTURE_ARTEMIA_YIELD, 2) : ServerConfig.getOr(ServerConfig.CULTURE_LARVA_YIELD, 3);
    }

    public double batchDays() {
        return artemia() ? ServerConfig.getOr(ServerConfig.CULTURE_ARTEMIA_DAYS, 1.0) : ServerConfig.getOr(ServerConfig.CULTURE_LARVA_DAYS, 2.0);
    }

    /** The jar of an old world still waiting in the hatchery (EMPTY if none; GameTests). */
    public ItemStack openedJar() {
        return opened;
    }

    /** Batches the ingredients are enough for (M35.1: one jar of cysts is one batch). */
    public int ingredientUses() {
        return items.getStackInSlot(INPUT).getCount() + opened.getCount();
    }

    // ------------------------------------------------------------------ environment

    /** Why the batch does not grow right now (translation key), or null if it does. */
    @Nullable
    public String blocker(Level level) {
        if (water < WATER_PER_BATCH) return artemia() ? "salt_water" : "water";
        if (items.getStackInSlot(INPUT).isEmpty() && opened.isEmpty()) return artemia() ? "cysts" : "organic";
        ItemStack out = items.getStackInSlot(OUTPUT);
        // M17: a whole batch must fit (before, a batch near the full mark used its food and water for fewer items)
        int y = Math.min(batchYield(), OUTPUT_MAX);
        if (!out.isEmpty() && out.getCount() + y > OUTPUT_MAX) return "full";
        if (artemia()) {
            if (items.getStackInSlot(BOTTLES).getCount() < y) return "bottles";
            if (level.getMaxLocalRawBrightness(worldPosition.above()) < MIN_LIGHT) return "dark";
        } else if (!level.canSeeSky(worldPosition.above())) {
            return "sky";
        }
        return null;
    }

    /** Speed multiplier of the larva tub (night, betta biome, rain); 1 for the hatchery. */
    public double speed(Level level) {
        if (artemia()) return 1.0;
        double f = 1.0;
        if (level.dimensionType().hasSkyLight() && level.isDarkOutside()) f *= 1.5;
        if (level.getBiome(worldPosition).is(ModTags.BETTA_SPAWN_BIOMES)) f *= 1.3;
        if (level.isRainingAt(worldPosition.above())) f *= 1.3;
        return f;
    }

    // ------------------------------------------------------------------ simulation

    public static void serverTick(Level level, BlockPos pos, BlockState state, CultureTubBlockEntity be) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        long now = level.getGameTime();
        long dt = be.lastTick < 0 ? INTERVAL : Math.max(0, now - be.lastTick);
        if (!ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) dt = Math.min(dt, INTERVAL * 5L);
        be.lastTick = now;
        if (!org.overengineer.bettacraft.config.FeatureGate.item(state.getBlock().asItem())) return; // M33 part b: live food off: the batch rests
        be.advance(server, dt / 24000.0);
    }

    /** Advances the batch by {@code days} (also used by GameTests). Returns the number of batches finished. */
    public int advance(ServerLevel level, double days) {
        int done = 0;
        double left = days;
        while (left > 1e-9 && blocker(level) == null) {
            double rate = speed(level) / Math.max(1e-6, batchDays());
            double need = (1 - progress) / rate;
            if (need > left) {
                progress += left * rate;
                left = 0;
            } else {
                left -= need;
                progress = 0;
                finishBatch();
                done++;
            }
            setChanged();
        }
        if (done > 0) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.4F);
            updateState();
        }
        return done;
    }

    private void finishBatch() {
        ItemStack in = items.getStackInSlot(INPUT);
        if (!opened.isEmpty()) {
            // an open jar of an old world first (M35.1: one jar, one batch)
            opened = ItemStack.EMPTY;
        } else {
            // one jar of cysts or one piece of organic matter
            items.extractItem(INPUT, 1, false);
        }
        water -= WATER_PER_BATCH;
        int n = batchYield();
        ItemStack out = items.getStackInSlot(OUTPUT);
        int room = OUTPUT_MAX - out.getCount();
        n = Math.min(n, room);
        if (artemia()) {
            n = Math.min(n, items.getStackInSlot(BOTTLES).getCount());
            items.extractItem(BOTTLES, n, false);
        }
        if (n > 0) {
            ItemStack add = out.isEmpty() ? result().copyWithCount(n) : out.copyWithCount(out.getCount() + n);
            items.setStackInSlot(OUTPUT, add);
        }
    }

    private void addSaltWaterBottle() {
        water = Math.min(WATER_CAPACITY, water + SALT_WATER_BOTTLE_MB);
        ItemStack b = items.getStackInSlot(BOTTLES);
        items.setStackInSlot(BOTTLES, b.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : b.copyWithCount(b.getCount() + 1));
        setChanged();
        updateState();
    }

    void updateState() {
        if (level == null || level.isClientSide()) return;
        BlockState s = getBlockState();
        if (!(s.getBlock() instanceof CultureTubBlock)) return;
        int w = water <= 0 ? 0 : Math.min(4, (water + WATER_PER_BATCH - 1) / WATER_PER_BATCH);
        BlockState ns = s.setValue(CultureTubBlock.WATER, w).setValue(CultureTubBlock.READY, !items.getStackInSlot(OUTPUT).isEmpty());
        if (ns != s) level.setBlock(worldPosition, ns, Block.UPDATE_ALL);
    }

    public int comparatorSignal() {
        ItemStack out = items.getStackInSlot(OUTPUT);
        return out.isEmpty() ? 0 : Math.max(1, Math.round(15F * out.getCount() / OUTPUT_MAX));
    }

    // ------------------------------------------------------------------ interaction

    /** Whether a right-click with this item does something (client prediction). */
    public boolean accepts(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(Items.GLASS_BOTTLE) || stack.is(ModItems.SALT_WATER_BOTTLE.get()) || isIngredient(stack);
    }

    /** Right-click with an item. Returns false if the item does nothing here. */
    public boolean use(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            if (artemia()) {
                player.sendOverlayMessage(Component.translatable("message.bettacraft.culture.needs_salt_water").withStyle(ChatFormatting.GOLD));
            } else if (water + 1000 > WATER_CAPACITY) {
                player.sendOverlayMessage(Component.translatable("message.bettacraft.culture.full_water"));
            } else {
                setWater(water + 1000);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (stack.is(ModItems.SALT_WATER_BOTTLE.get())) {
            if (!artemia()) return false;
            if (water + SALT_WATER_BOTTLE_MB > WATER_CAPACITY) {
                player.sendOverlayMessage(Component.translatable("message.bettacraft.culture.full_water"));
                return true;
            }
            water = Math.min(WATER_CAPACITY, water + SALT_WATER_BOTTLE_MB);
            setChanged();
            updateState();
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!artemia()) {
                // harvest the larvae with the bottle (PLAN 6.3); the bottle is a tool here
                if (!take(level, player)) showStatus(level, player);
                return true;
            }
            ItemStack rest = items.insertItem(BOTTLES, stack.copy(), false);
            int moved = stack.getCount() - rest.getCount();
            if (moved > 0 && !player.getAbilities().instabuild) stack.shrink(moved);
            if (moved == 0) showStatus(level, player);
            return true;
        }
        if (isIngredient(stack)) {
            ItemStack one = stack.copyWithCount(player.isShiftKeyDown() ? stack.getCount() : 1);
            ItemStack rest = items.insertItem(INPUT, one, false);
            int moved = one.getCount() - rest.getCount();
            if (moved > 0) {
                if (!player.getAbilities().instabuild) stack.shrink(moved);
                level.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                showStatus(level, player);
            }
            return true;
        }
        return false;
    }

    /** Right-click with an empty hand: take the food (hatchery), or show the status. */
    public void takeOrShow(ServerLevel level, Player player) {
        if (artemia() && take(level, player)) return;
        showStatus(level, player);
    }

    /** Gives the ready food to the player. */
    public boolean take(ServerLevel level, Player player) {
        ItemStack out = items.extractItem(OUTPUT, OUTPUT_MAX, false);
        if (out.isEmpty()) return false;
        int n = out.getCount();
        if (!player.getInventory().add(out)) player.drop(out, false, net.minecraft.util.Prediction.SERVER_ONLY);
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.FOOD_BRED, n);
        return true;
    }

    public void showStatus(Level level, Player player) {
        player.sendOverlayMessage(status(level));
    }

    public MutableComponent status(Level level) {
        String b = blocker(level);
        MutableComponent c = Component.translatable("message.bettacraft.culture.status", water, WATER_CAPACITY, ingredientUses(),
                Math.round(progress * 100), items.getStackInSlot(OUTPUT).getCount());
        if (b != null) c.append(" ").append(Component.translatable("message.bettacraft.culture.blocked." + b).withStyle(ChatFormatting.GOLD));
        return c;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (!opened.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, opened.copy());
            opened = ItemStack.EMPTY;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s.copy());
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        Schemas.CULTURE_TUB.stamp(tag);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("water", water);
        tag.putDouble("progress", progress);
        if (!opened.isEmpty()) tag.put("opened_jar", NbtCompat.saveStack(registries, opened));
        NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag raw = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        CompoundTag tag = Schemas.CULTURE_TUB.migrate(raw);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompoundOrEmpty("items"));
            if (items.getSlots() != SLOTS) items.setSize(SLOTS);
        }
        water = Math.max(0, Math.min(WATER_CAPACITY, tag.getIntOr("water", 0)));
        progress = Math.max(0, Math.min(1, tag.getDoubleOr("progress", 0d)));
        opened = tag.contains("opened_jar") ? NbtCompat.parseOptionalStack(registries, tag.getCompoundOrEmpty("opened_jar")) : ItemStack.EMPTY;
        if (!opened.isEmpty()) {
            // M35.1: the open jar of an old world (its old uses are gone on load) goes back onto the jars in the input slot
            ItemStack in = items.getStackInSlot(INPUT);
            if (in.isEmpty()) {
                items.setStackInSlot(INPUT, opened);
                opened = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(in, opened) && in.getCount() + opened.getCount() <= in.getMaxStackSize()) {
                items.setStackInSlot(INPUT, in.copyWithCount(in.getCount() + opened.getCount()));
                opened = ItemStack.EMPTY;
            }
        }
    }
}
