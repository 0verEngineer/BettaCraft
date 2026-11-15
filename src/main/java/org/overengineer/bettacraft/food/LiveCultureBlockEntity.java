package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M26: tubifex dish and daphnia barrel ({@link LiveCultureBlock}); the culture itself is a {@link CultureModel}.
 * <ul>
 *     <li><b>Tubifex dish</b> (up to 1000 mB): needs mud, water and a few worms to start (from swamp mud). Mulm from
 *     washed filter media ({@code #bettacraft:tubifex_food}) or waste water of a tank through a pipe (water with a
 *     quality below 80 %) feeds them. The water slowly goes stale and evaporates (100 mB a day); fresh water (a bucket on
 *     a full dish, or clean water through a pipe) rinses it. Worms from a dish that is half foul are of poor quality.</li>
 *     <li><b>Daphnia barrel</b> (up to 4000 mB, needs 2000): daphnia to start (they come up with swamp water), yeast or
 *     green water ({@code #bettacraft:daphnia_food}) and light 9 or more. They collapse when they stay crowded too long
 *     or the barrel stands in the sun of a hot biome (or in the Nether). Green water bottles leave their glass bottle
 *     in the barrel (hoppers take them out).</li>
 * </ul>
 * Item capability: slot 0 takes starters, food and mud (used at once), slot 1 gives the harvest (always leaving the
 * seed of the culture), slot 2 gives the empty bottles. Fluid capability: water in. The net harvests up to 4 portions.
 */
public class LiveCultureBlockEntity extends BlockEntity {
    public static final int INPUT = 0, OUTPUT = 1, BOTTLES = 2, SLOTS = 3;
    public static final int MIN_LIGHT = 9, NET_PORTIONS = 4, MAX_BOTTLES = 16;
    public static final int TUBIFEX_WATER = 1000, DAPHNIA_WATER = 4000, DAPHNIA_MIN_WATER = 2000, TUBIFEX_MIN_WATER = 250;
    /** Water a tubifex dish loses per day. */
    public static final int TUBIFEX_EVAPORATION = 100;
    /** Water at or above this quality rinses a tubifex dish; below it, it feeds the worms (waste water). */
    public static final float CLEAN_QUALITY = 0.8F;
    private static final int INTERVAL = 20;

    private final CultureModel model = new CultureModel();
    private int water, bottles;
    private boolean mud;
    private double waterCarry;
    private long lastTick = -1;

    public LiveCultureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIVE_CULTURE.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public LiveCultureBlock.Kind kind() {
        return getBlockState().getBlock() instanceof LiveCultureBlock b ? b.kind() : LiveCultureBlock.Kind.TUBIFEX;
    }

    public boolean tubifex() {
        return kind() == LiveCultureBlock.Kind.TUBIFEX;
    }

    public CultureModel model() {
        return model;
    }

    public int water() {
        return water;
    }

    public boolean mud() {
        return mud;
    }

    public int bottles() {
        return bottles;
    }

    public int capacity() {
        return tubifex() ? TUBIFEX_WATER : DAPHNIA_WATER;
    }

    public int minWater() {
        return tubifex() ? TUBIFEX_MIN_WATER : DAPHNIA_MIN_WATER;
    }

    /** Portions the full culture holds. */
    public int room() {
        return tubifex() ? ServerConfig.getOr(ServerConfig.LIVE_FOOD_TUBIFEX_ROOM, 8) : ServerConfig.getOr(ServerConfig.LIVE_FOOD_DAPHNIA_ROOM, 16);
    }

    public double growthDays() {
        return tubifex() ? ServerConfig.getOr(ServerConfig.LIVE_FOOD_TUBIFEX_DAYS, 4.0) : ServerConfig.getOr(ServerConfig.LIVE_FOOD_DAPHNIA_DAYS, 3.0);
    }

    public ItemStack animal() {
        return new ItemStack(tubifex() ? ModItems.TUBIFEX.get() : ModItems.DAPHNIA.get());
    }

    /** One portion of the harvest (tubifex of a foul dish are of poor quality). */
    public ItemStack harvestStack(int n) {
        ItemStack s = animal().copyWithCount(Math.max(1, n));
        if (tubifex() && model.poor()) s.set(ModDataComponents.POOR_CULTURE.get(), true);
        return s;
    }

    public void setWater(int mb) {
        water = Math.max(0, Math.min(capacity(), mb));
        setChanged();
        updateState();
    }

    public void setMud(boolean m) {
        mud = m;
        setChanged();
        updateState();
    }

    // ------------------------------------------------------------------ environment

    public boolean lit(Level level) {
        return level.getMaxLocalRawBrightness(worldPosition.above()) >= MIN_LIGHT;
    }

    /** Daphnia: the barrel stands in the sun of a hot biome (base temperature 1.5 or more, by day) or in the Nether. */
    public boolean hot(Level level) {
        if (level.dimensionType().ultraWarm()) return true;
        return level.getBiome(worldPosition).value().getBaseTemperature() >= 1.5F && level.isDay() && level.canSeeSky(worldPosition.above());
    }

    /** Why the culture does not grow right now (translation key suffix), or null. */
    @Nullable
    public String blocker(Level level) {
        if (!Features.enabled(Feature.LIVE_FOOD_CULTURES)) return "off";
        if (tubifex() && !mud) return "mud";
        if (water < minWater()) return "water";
        if (!model.alive()) return "animals";
        if (!tubifex() && hot(level)) return "hot";
        if (!tubifex() && !lit(level)) return "dark";
        if (model.food <= 0) return "hungry";
        if (tubifex() && model.poor()) return "foul";
        if (!tubifex() && model.population >= CultureModel.CROWDED) return "crowded";
        return null;
    }

    // ------------------------------------------------------------------ simulation

    public static void serverTick(Level level, BlockPos pos, BlockState state, LiveCultureBlockEntity be) {
        if (!(level instanceof ServerLevel server) || (level.getGameTime() + pos.asLong()) % INTERVAL != 0) return;
        long now = level.getGameTime();
        long dt = be.lastTick < 0 ? INTERVAL : Math.max(0, now - be.lastTick);
        if (!ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) dt = Math.min(dt, INTERVAL * 5L);
        be.lastTick = now;
        be.advance(server, dt / 24000.0);
    }

    /** Advances the culture by {@code days} (also used by GameTests). */
    public CultureModel.Event advance(ServerLevel level, double days) {
        if (!Features.enabled(Feature.LIVE_FOOD_CULTURES) || days <= 0) return CultureModel.Event.NONE;
        CultureModel.Event e;
        if (tubifex()) {
            e = model.stepTubifex(days, mud, water >= minWater(), growthDays());
            if (water > 0) {
                waterCarry += TUBIFEX_EVAPORATION * days;
                int lost = (int) waterCarry;
                waterCarry -= lost;
                water = Math.max(0, water - lost);
            }
        } else {
            e = model.stepDaphnia(days, water >= minWater(), lit(level), hot(level), growthDays(),
                    ServerConfig.getOr(ServerConfig.LIVE_FOOD_CROWDED_DAYS, 1.0));
        }
        if (e == CultureModel.Event.COLLAPSE_CROWDED || e == CultureModel.Event.COLLAPSE_HEAT || e == CultureModel.Event.DIED_OUT) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.6F, 0.7F);
        }
        setChanged();
        updateState();
        return e;
    }

    void updateState() {
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        if (!(s.getBlock() instanceof LiveCultureBlock)) return;
        double p = model.population;
        int stage = p < CultureModel.DEAD ? 0 : p < 0.35 ? 1 : p < CultureModel.CROWDED ? 2 : 3;
        BlockState ns = s.setValue(LiveCultureBlock.STAGE, stage).setValue(LiveCultureBlock.WATER, water > 0).setValue(LiveCultureBlock.MUD, mud);
        if (ns != s) level.setBlock(worldPosition, ns, Block.UPDATE_ALL);
    }

    public int comparatorSignal() {
        double p = model.population;
        return p < CultureModel.DEAD ? 0 : Math.max(1, (int) Math.round(15 * p));
    }

    // ------------------------------------------------------------------ inputs

    public boolean isStarter(ItemStack s) {
        return s.is(tubifex() ? ModItems.TUBIFEX.get() : ModItems.DAPHNIA.get());
    }

    public boolean isFood(ItemStack s) {
        return s.is(tubifex() ? ModTags.TUBIFEX_FOOD : ModTags.DAPHNIA_FOOD);
    }

    /** Food value of one item: green water more than yeast or mulm. */
    public static double foodValue(ItemStack s) {
        return s.is(ModItems.GREEN_WATER_BOTTLE.get()) ? 0.4 : CultureModel.FOOD_PER_ITEM;
    }

    /** Uses one item of this stack (starter, food or mud) if it fits; returns whether it was taken. */
    public boolean insertOne(ItemStack s, boolean simulate) {
        if (s.isEmpty()) return false;
        if (isStarter(s)) {
            if (model.population >= 0.5) return false;
            if (!simulate) {
                model.seed(room());
                setChanged();
                updateState();
            }
            return true;
        }
        if (isFood(s)) {
            double v = foodValue(s);
            if (model.food + v > 1.0 + 1e-9) return false;
            boolean bottle = s.getCraftingRemainingItem().is(Items.GLASS_BOTTLE);
            if (bottle && bottles >= MAX_BOTTLES) return false;
            if (!simulate) {
                model.food = Math.min(1, model.food + v);
                if (bottle) bottles++;
                setChanged();
            }
            return true;
        }
        if (tubifex() && s.is(Items.MUD)) {
            if (mud) return false;
            if (!simulate) setMud(true);
            return true;
        }
        return false;
    }

    /** Water comes in: a tubifex dish is fed by waste water and rinsed by clean water. Returns the mB taken. */
    public int addWater(int mb, @Nullable WaterValues values, boolean simulate) {
        int n = Math.max(0, Math.min(mb, capacity() - water));
        if (n <= 0 || simulate) return n;
        water += n;
        if (tubifex()) {
            float q = values == null ? 1F : values.quality();
            if (q < CLEAN_QUALITY) model.food = Math.min(1, model.food + (1 - q) * n / 1000.0);
            else model.rinse(n);
        }
        setChanged();
        updateState();
        return n;
    }

    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == OUTPUT) {
                int n = model.harvestable(room());
                return n <= 0 ? ItemStack.EMPTY : harvestStack(n);
            }
            if (slot == BOTTLES) return bottles <= 0 ? ItemStack.EMPTY : new ItemStack(Items.GLASS_BOTTLE, bottles);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != INPUT || stack.isEmpty()) return stack;
            int taken = 0;
            // one at a time: each item changes what fits next
            while (taken < stack.getCount() && insertOne(stack, true)) {
                if (simulate) {
                    taken++;
                    break; // the culture cannot be simulated further without changing it
                }
                insertOne(stack, false);
                taken++;
            }
            if (taken == 0) return stack;
            return stack.copyWithCount(stack.getCount() - taken);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;
            if (slot == OUTPUT) {
                int n = Math.min(amount, model.harvestable(room()));
                if (n <= 0) return ItemStack.EMPTY;
                ItemStack out = harvestStack(n);
                if (!simulate) {
                    model.take(n, room());
                    setChanged();
                    updateState();
                }
                return out;
            }
            if (slot == BOTTLES && bottles > 0) {
                int n = Math.min(amount, bottles);
                if (!simulate) {
                    bottles -= n;
                    setChanged();
                }
                return new ItemStack(Items.GLASS_BOTTLE, n);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && (isStarter(stack) || isFood(stack) || tubifex() && stack.is(Items.MUD));
        }
    };

    private final IFluidHandler fluid = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return water <= 0 ? FluidStack.EMPTY : new FluidStack(net.minecraft.world.level.material.Fluids.WATER, water);
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return ModFluids.isWater(stack.getFluid()) && !stack.getFluid().isSame(ModFluids.SEA_WATER.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            return addWater(resource.getAmount(), WaterValues.from(resource), action.simulate());
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

    public IItemHandler automation() {
        return automation;
    }

    public IFluidHandler fluid() {
        return fluid;
    }

    // ------------------------------------------------------------------ interaction

    /** Whether a right-click with this item does something (client prediction). */
    public boolean accepts(ItemStack s) {
        return s.is(Items.WATER_BUCKET) || s.is(ModItems.AQUARIUM_NET.get()) || isStarter(s) || isFood(s) || tubifex() && s.is(Items.MUD);
    }

    /** Right-click with an item; false if it does nothing here. */
    public boolean use(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            if (water + 1000 <= capacity()) {
                addWater(1000, null, false);
            } else if (tubifex()) {
                // a full dish: the fresh water runs through and washes out the stale water (and some mulm)
                model.rinse(1000);
                model.food = Math.max(0, model.food - 0.1);
                setChanged();
                player.displayClientMessage(Component.translatable("message.bettacraft.live_culture.rinsed"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.bettacraft.culture.full_water"), true);
                return true;
            }
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        if (stack.is(ModItems.AQUARIUM_NET.get())) {
            if (!harvest(level, player, NET_PORTIONS)) showStatus(level, player);
            return true;
        }
        if (isStarter(stack) || isFood(stack) || tubifex() && stack.is(Items.MUD)) {
            boolean bottle = stack.getCraftingRemainingItem().is(Items.GLASS_BOTTLE), isMud = stack.is(Items.MUD);
            if (!insertOne(stack, false)) {
                showStatus(level, player);
                return true;
            }
            if (bottle) bottles--; // by hand the player keeps the bottle
            if (!player.getAbilities().instabuild) {
                ItemStack rest = stack.getCraftingRemainingItem();
                stack.shrink(1);
                if (!rest.isEmpty() && !player.getInventory().add(rest)) player.drop(rest, false);
            }
            level.playSound(null, worldPosition, isMud ? SoundEvents.MUD_PLACE : SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
            return true;
        }
        return false;
    }

    /** The net scoops up to {@code max} portions. */
    public boolean harvest(ServerLevel level, Player player, int max) {
        boolean poor = tubifex() && model.poor();
        int n = model.take(max, room());
        if (n <= 0) return false;
        ItemStack out = animal().copyWithCount(n);
        if (poor) out.set(ModDataComponents.POOR_CULTURE.get(), true);
        if (!player.getInventory().add(out)) player.drop(out, false);
        setChanged();
        updateState();
        level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 0.6F, 1.3F);
        level.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + (tubifex() ? 0.35 : 0.95),
                worldPosition.getZ() + 0.5, 8, 0.25, 0.02, 0.25, 0.02);
        org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.FOOD_BRED, n);
        if (poor) player.displayClientMessage(Component.translatable("message.bettacraft.live_culture.poor_harvest").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public void showStatus(Level level, Player player) {
        player.displayClientMessage(status(level), true);
    }

    public MutableComponent status(Level level) {
        MutableComponent c = Component.translatable("message.bettacraft.live_culture.status", water, capacity(), Math.round(model.population * 100),
                Math.round(model.food * 100), model.harvestable(room()));
        if (tubifex()) c.append(" ").append(Component.translatable("message.bettacraft.live_culture.foul", Math.round(model.foul * 100)));
        String b = blocker(level);
        if (b != null) c.append(" ").append(Component.translatable("message.bettacraft.live_culture.blocked." + b).withStyle(ChatFormatting.GOLD));
        return c;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (bottles > 0) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.GLASS_BOTTLE, bottles));
        if (mud) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.MUD));
        bottles = 0;
        mud = false;
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("water", water);
        tag.putBoolean("mud", mud);
        tag.putInt("bottles", bottles);
        tag.putDouble("population", model.population);
        tag.putDouble("food", model.food);
        tag.putDouble("foul", model.foul);
        tag.putDouble("crowded", model.crowded);
        tag.putDouble("heat", model.heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = Math.max(0, tag.getInt("water"));
        mud = tag.getBoolean("mud");
        bottles = Math.max(0, Math.min(MAX_BOTTLES, tag.getInt("bottles")));
        model.population = clamp(tag.getDouble("population"));
        model.food = clamp(tag.getDouble("food"));
        model.foul = clamp(tag.getDouble("foul"));
        model.crowded = Math.max(0, finite(tag.getDouble("crowded")));
        model.heat = Math.max(0, finite(tag.getDouble("heat")));
    }

    private static double finite(double v) {
        return Double.isFinite(v) ? v : 0;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, finite(v)));
    }
}
