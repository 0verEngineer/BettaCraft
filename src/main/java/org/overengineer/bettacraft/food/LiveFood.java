package org.overengineer.bettacraft.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaBreeding;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;

/**
 * M26: live food beyond the mosquito larvae. Tubifex worms condition for breeding twice as well
 * ({@code #bettacraft:conditioning_food}, {@code liveFood.tubifexConditioning}); worms from a foul dish
 * ({@link ModDataComponents#POOR_CULTURE}) stress the fish a little (they bring germs). Live food in a pipe is the fluid
 * {@code bettacraft:live_food} ({@link #PORTION_MB} mB = one portion of the food named by {@link Content}); a tank
 * feeds it to its animals when it comes in ({@link #fillTank}).
 */
public final class LiveFood {
    /** mB of suspension per portion (one food item). */
    public static final int PORTION_MB = 250;

    private LiveFood() {}

    /** What a portion of suspension is: the food item and whether it came from a poor culture. */
    public record Content(ResourceLocation item, boolean poor) {
        public static final Codec<Content> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Content::item),
                Codec.BOOL.optionalFieldOf("poor", false).forGetter(Content::poor)).apply(i, Content::new));
        public static final StreamCodec<ByteBuf, Content> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Content::item, ByteBufCodecs.BOOL, Content::poor, Content::new);
    }

    // ------------------------------------------------------------------ the food items

    public static boolean poor(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.POOR_CULTURE.get()));
    }

    /** Food a pump may send: live food and fry food (not pellets or wafers). */
    public static boolean pumpable(ItemStack stack) {
        BettaFood.Kind k = BettaFood.kind(stack);
        return k == BettaFood.Kind.LIVE || BettaFood.isFryFood(k);
    }

    /** How much longer than usual this food conditions a breeding fish. */
    public static double conditioning(ItemStack food) {
        return food.is(ModTags.CONDITIONING_FOOD) ? ServerConfig.getOr(ServerConfig.LIVE_FOOD_TUBIFEX_CONDITIONING, 2.0) : 1.0;
    }

    /** A fish ate this food: live food conditions an adult, food of a poor culture stresses it a little. */
    public static void eaten(ItemStack food, BettaHealth health, BettaBreeding breeding, boolean adult) {
        if (adult && BettaFood.kind(food) == BettaFood.Kind.LIVE) breeding.condition(conditioning(food));
        if (poor(food)) health.addWaterStress(ServerConfig.getOr(ServerConfig.LIVE_FOOD_POOR_STRESS, 0.5));
    }

    // ------------------------------------------------------------------ the suspension

    public static boolean isSuspension(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().isSame(ModFluids.LIVE_FOOD.get());
    }

    /** {@code portions} of suspension of this food. */
    public static FluidStack suspension(ItemStack food, int portions) {
        FluidStack f = new FluidStack(ModFluids.LIVE_FOOD.get(), portions * PORTION_MB);
        f.set(ModDataComponents.LIVE_FOOD.get(), new Content(BuiltInRegistries.ITEM.getKey(food.getItem()), poor(food)));
        return f;
    }

    /** The food one portion of this suspension stands for (EMPTY if it names nothing known). */
    public static ItemStack food(FluidStack stack) {
        Content c = stack.get(ModDataComponents.LIVE_FOOD.get());
        if (c == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(c.item());
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item);
        if (c.poor()) s.set(ModDataComponents.POOR_CULTURE.get(), true);
        return s;
    }

    /**
     * Suspension flows into a tank: whole portions only, and only if somebody there eats this food. The animals eat what
     * they need (the hungriest first); the rest sinks to the shrimp and snails. The water of the suspension overflows.
     */
    public static int fillTank(ServerLevel level, TankBlockEntity c, FluidStack resource, IFluidHandler.FluidAction action) {
        if (!CommonConfig.liveFoodPump()) return 0;
        int portions = resource.getAmount() / PORTION_MB;
        ItemStack food = food(resource);
        if (portions <= 0 || food.isEmpty() || BettaFood.kind(food) == null) return 0;
        if (!TankCare.anyoneEats(level, c.getTank(), food)) return 0;
        if (action.execute()) {
            int used = TankCare.feed(level, c, food, portions);
            if (used < portions) org.overengineer.bettacraft.critters.TankCritters.addFood(c.getTank(), portions - used);
            TankCare.log(level, c, "log.bettacraft.live_food_pumped", String.valueOf(portions), "@" + food.getDescriptionId());
            c.setChanged();
        }
        return portions * PORTION_MB;
    }
}
