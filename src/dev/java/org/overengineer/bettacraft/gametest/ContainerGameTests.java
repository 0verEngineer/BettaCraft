package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.containers.ContainerRules;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.1 GameTests: every container empties everywhere ({@link ContainerRules}), container by container and target by
 * target. The sea water bucket into the world (the reported bug: into the sea, where a bucket finds no bottom in reach),
 * onto the ground, into a cauldron, a tank and other mods' tanks (fluid capability); the water bucket into a tank; the
 * animal buckets (betta, tankmate, seahorse, frog) and the bags (fish, critters) into the world only after the
 * confirmation, into a tank without; the empty bag shaken out without water; the bottles poured out anywhere without a
 * water block and working in a tank as before.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContainerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ContainerGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 0;
        return sp;
    }

    /** The player stands at {@code rel} (relative) looking straight down (or up). */
    private static void stand(GameTestHelper h, ServerPlayer sp, BlockPos rel, float pitch) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(rel));
        sp.moveTo(at.x, at.y, at.z, 0F, pitch);
        sp.setYHeadRot(0F);
    }

    /** A right-click with the stack in the main hand into the air or the world (the item's own use). */
    private static ItemStack use(ServerPlayer sp, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        sp.gameMode.useItem(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND);
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** A right-click on a block (blocks like tanks and cauldrons take the item first). */
    private static ItemStack useOn(ServerPlayer sp, GameTestHelper h, BlockPos rel, ItemStack stack) {
        sp.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = h.absolutePos(rel);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** A pool of water 8 deep: from its surface the bottom is out of reach (the sea from the shore or a boat). */
    private static void pool(GameTestHelper h, int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 3; x++)
            for (int z = z0 - 1; z <= z0 + 3; z++)
                for (int y = 1; y <= 8; y++) {
                    boolean wall = x == x0 - 1 || x == x0 + 3 || z == z0 - 1 || z == z0 + 3;
                    h.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                }
    }

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, long water, float salt) {
        for (int a = 0; a < 2; a++)
            for (int b = 0; b < 2; b++)
                for (int c = 0; c < 2; c++) {
                    BlockPos p = new BlockPos(x + a, y + b, z + c);
                    h.setBlock(p, ModBlocks.TANK.get().defaultBlockState());
                    TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(p), null);
                }
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(new BlockPos(x, y, z)));
        if (c == null) throw new GameTestAssertException("tank did not form");
        if (water > 0) TankInteractions.changeWater(h.getLevel(), c, water, salt);
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        return c;
    }

    private static <T extends Entity> List<T> around(GameTestHelper h, Class<T> type, BlockPos rel) {
        return h.getLevel().getEntitiesOfClass(type, new AABB(h.absolutePos(rel)).inflate(4));
    }

    private static ItemStack bettaBucket(long seed) {
        BettaGenome g = BettaGenome.randomDomestic(RandomSource.create(seed));
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), BettaData.create(g, seed % 2 == 0, 0));
    }

    private static ItemStack tankmateBucket(TankmateSpecies s) {
        return TankmateBucketItem.withFish(new ItemStack(ModItems.TANKMATE_BUCKETS.get(s).get()), s, TankmateGenes.wild(s), false, LifeStage.ADULT);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    // ------------------------------------------------------------------ the sea water bucket

    /**
     * The reported bug: a bucket of sea water used on the sea (from the shore or a boat the bottom is out of reach, the
     * vanilla bucket finds nothing) could not be emptied. Now it pours into the water the player looks at.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void seaWaterBucketIntoTheSea(GameTestHelper h) {
        ServerPlayer sp = player(h);
        pool(h, 4, 4);
        stand(h, sp, new BlockPos(5, 9, 5), 90F);
        // the cause: a vanilla bucket looks through the water for the bottom; out of reach nothing happens (the sea water
        // bucket was such a vanilla bucket)
        ItemStack vanilla = use(sp, new ItemStack(Items.WATER_BUCKET));
        check(vanilla.is(Items.WATER_BUCKET), "the vanilla water bucket behaves differently than assumed: " + vanilla);
        ItemStack after = use(sp, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        check(after.is(Items.BUCKET), "the sea water bucket did not empty into the sea: " + after);
        check(h.getLevel().getFluidState(h.absolutePos(new BlockPos(5, 8, 5))).is(net.minecraft.tags.FluidTags.WATER), "the sea is gone");
        h.succeed();
    }

    /** Onto the ground: a water source like a water bucket. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void seaWaterBucketOntoTheGround(GameTestHelper h) {
        ServerPlayer sp = player(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.STONE.defaultBlockState());
        stand(h, sp, new BlockPos(5, 4, 5), 90F);
        ItemStack after = use(sp, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        check(after.is(Items.BUCKET), "the sea water bucket did not empty onto the ground: " + after);
        check(h.getLevel().getFluidState(h.absolutePos(new BlockPos(5, 2, 5))).isSource(), "no water on the ground");
        h.succeed();
    }

    /** Into a cauldron: a full water cauldron, the empty bucket back. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void seaWaterBucketIntoACauldron(GameTestHelper h) {
        ServerPlayer sp = player(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.CAULDRON.defaultBlockState());
        ItemStack after = useOn(sp, h, new BlockPos(5, 1, 5), new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        var state = h.getBlockState(new BlockPos(5, 1, 5));
        check(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3, "the cauldron is not full of water: " + state);
        check(after.is(Items.BUCKET), "no empty bucket back: " + after);
        h.succeed();
    }

    /** Into a tank: water with the salt of the sea; the water bucket brings fresh water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bucketsIntoATank(GameTestHelper h) {
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 4, 1, 4, 2000, 0F);
        long before = c.getTank().waterMb();
        ItemStack after = useOn(sp, h, new BlockPos(4, 2, 4), new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        check(after.is(Items.BUCKET), "the sea water bucket did not empty into the tank: " + after);
        check(c.getTank().waterMb() == before + 1000 && c.getTank().salinity() > 5, "no salt water in the tank: " + c.getTank().waterMb() + " mB, "
                + c.getTank().salinity() + " ppt");
        float salt = c.getTank().salinity();
        after = useOn(sp, h, new BlockPos(4, 2, 4), new ItemStack(Items.WATER_BUCKET));
        check(after.is(Items.BUCKET) && c.getTank().waterMb() == before + 2000 && c.getTank().salinity() < salt, "the water bucket did not dilute the tank");
        h.succeed();
    }

    /** Into another mod's tank or pipe: the fluid capability holds 1000 mB of water and leaves an empty bucket. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void seaWaterBucketIntoAForeignTank(GameTestHelper h) {
        ItemStack stack = new ItemStack(ModItems.SEA_WATER_BUCKET.get());
        var handler = FluidUtil.getFluidHandler(stack).orElse(null);
        check(handler != null, "no fluid capability");
        check(handler.getFluidInTank(0).is(Fluids.WATER) && handler.getFluidInTank(0).getAmount() == 1000, "not 1000 mB of water: " + handler.getFluidInTank(0));
        FluidTank foreign = new FluidTank(8000);
        var result = FluidUtil.tryEmptyContainer(stack, foreign, 1000, null, true);
        check(result.isSuccess() && result.getResult().is(Items.BUCKET), "the bucket did not empty into the foreign tank");
        check(foreign.getFluidAmount() == 1000 && foreign.getFluid().is(Fluids.WATER), "the foreign tank has no water: " + foreign.getFluid());
        // the other way round an empty bucket fills with water (plain water: the foreign tank knows no salt)
        var back = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), foreign, 1000, null, true);
        check(back.isSuccess() && back.getResult().is(Items.WATER_BUCKET), "the empty bucket did not fill from the foreign tank");
        check(foreign.getFluid().isEmpty() || foreign.getFluid().getAmount() == 0, "water left in the foreign tank");
        check(foreign.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.SIMULATE) == 1, "foreign tank broken");
        h.succeed();
    }

    // ------------------------------------------------------------------ animals

    /** Animal buckets let their animal go into the world only after the confirmation, onto the ground and into the sea. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void animalBucketsNeedTheConfirmation(GameTestHelper h) {
        ServerPlayer sp = player(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.STONE.defaultBlockState());
        stand(h, sp, new BlockPos(5, 4, 5), 90F);
        ItemStack after = use(sp, bettaBucket(7));
        check(after.is(ModItems.BETTA_BUCKET.get()), "the betta bucket emptied without asking: " + after);
        check(around(h, BettaEntity.class, new BlockPos(5, 2, 5)).isEmpty(), "a betta was let go without asking");
        check(!h.getLevel().getFluidState(h.absolutePos(new BlockPos(5, 2, 5))).isSource(), "water placed without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the confirmed betta bucket did not empty: " + sp.getItemInHand(InteractionHand.MAIN_HAND));
        check(around(h, BettaEntity.class, new BlockPos(5, 2, 5)).size() == 1, "the confirmed betta was not let go");
        // into the sea: tankmates, a seahorse and the frog go into the water the player looks at
        pool(h, 14, 4);
        int i = 0;
        for (ItemStack bucket : new ItemStack[] {tankmateBucket(TankmateSpecies.NEON_TETRA), tankmateBucket(TankmateSpecies.LINED_SEAHORSE),
                new ItemStack(ModItems.FROG_BUCKET.get())}) {
            stand(h, sp, new BlockPos(14 + i, 9, 5), 90F);
            ItemStack before = bucket.copy();
            after = use(sp, bucket);
            check(ItemStack.isSameItemSameComponents(after, before), "emptied without asking: " + before);
            ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
            check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the confirmed bucket did not empty into the sea: " + before);
            i++;
        }
        check(around(h, LivingEntity.class, new BlockPos(16, 7, 5)).size() >= 3, "the animals are not in the sea: "
                + around(h, LivingEntity.class, new BlockPos(16, 7, 5)));
        h.succeed();
    }

    /** Into a tank an animal bucket needs no confirmation: the animal becomes an inhabitant. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void animalBucketsIntoATankWithoutAsking(GameTestHelper h) {
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 4, 1, 4, 8000, 0F);
        ItemStack after = useOn(sp, h, new BlockPos(4, 2, 4), bettaBucket(3));
        check(!after.is(ModItems.BETTA_BUCKET.get()), "the betta bucket did not go into the tank: " + after);
        check(TankInhabitants.liveInhabitants(h.getLevel(), c.getTank()).size() == 1, "no inhabitant in the tank");
        h.succeed();
    }

    /** A filled bag: into the water only after the confirmation; an empty bag shakes out anywhere without water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bagsNeedTheConfirmationAndShakeOut(GameTestHelper h) {
        ServerPlayer sp = player(h);
        pool(h, 4, 4);
        // a bag with a neon tetra (like the net or the menu fills it)
        ItemStack bag = TankmateBucketItem.withFish(new ItemStack(ModItems.FISH_BAG.get()), TankmateSpecies.NEON_TETRA,
                TankmateGenes.wild(TankmateSpecies.NEON_TETRA), false, LifeStage.ADULT);
        bag.set(org.overengineer.bettacraft.registry.ModDataComponents.BAG_ENTITY.get(),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(org.overengineer.bettacraft.registry.ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get()));
        bag.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
        stand(h, sp, new BlockPos(5, 9, 5), 90F);
        ItemStack after = use(sp, bag);
        check(FishBagItem.isFilled(after), "the bag emptied without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        ItemStack done = sp.getItemInHand(InteractionHand.MAIN_HAND);
        check(done.is(ModItems.FISH_BAG.get()) && !FishBagItem.isFilled(done), "the confirmed bag did not empty: " + done);
        check(!around(h, TankmateEntity.class, new BlockPos(5, 7, 5)).isEmpty(), "the fish is not in the water");
        // a bag of snails: the same
        ItemStack snails = TankCritters.bagOf(CritterSpecies.ZEBRA_NERITE, 3, () -> CritterGenes.pure(0, 0), new java.util.Random(1), false);
        after = use(sp, snails);
        check(FishBagItem.isFilled(after), "the critter bag emptied without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(!FishBagItem.isFilled(sp.getItemInHand(InteractionHand.MAIN_HAND)), "the confirmed critter bag did not empty");
        // the empty bag, sneaking, on dry ground: a few drops, still an empty bag, no water
        h.setBlock(new BlockPos(20, 1, 20), Blocks.DIRT.defaultBlockState());
        stand(h, sp, new BlockPos(20, 3, 20), 90F);
        sp.setShiftKeyDown(true);
        after = use(sp, new ItemStack(ModItems.FISH_BAG.get()));
        sp.setShiftKeyDown(false);
        check(after.is(ModItems.FISH_BAG.get()) && !FishBagItem.isFilled(after), "the empty bag changed: " + after);
        check(h.getBlockState(new BlockPos(20, 2, 20)).isAir() && h.getBlockState(new BlockPos(20, 1, 20)).is(Blocks.DIRT), "shaking the bag made water");
        h.succeed();
    }

    /** A bag of animals into a tank needs no confirmation. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void critterBagIntoATankWithoutAsking(GameTestHelper h) {
        ServerPlayer sp = player(h);
        TankBlockEntity c = tank(h, 4, 1, 4, 8000, 0F);
        ItemStack snails = TankCritters.bagOf(CritterSpecies.ZEBRA_NERITE, 3, () -> CritterGenes.pure(0, 0), new java.util.Random(1), false);
        ItemStack after = useOn(sp, h, new BlockPos(4, 2, 4), snails);
        check(!FishBagItem.isFilled(after), "the snails did not go into the tank: " + after);
        check(c.getTank().critters().size() == 3, "not three snails in the tank: " + c.getTank().critters().size());
        h.succeed();
    }

    // ------------------------------------------------------------------ bottles

    /** Every bottle pours out anywhere (sneaking, or into the air): an empty glass bottle, no water block, no mud. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bottlesPourOutAnywhere(GameTestHelper h) {
        ServerPlayer sp = player(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.DIRT.defaultBlockState());
        h.setBlock(new BlockPos(7, 1, 5), Blocks.FARMLAND.defaultBlockState());
        for (var item : List.of(ModItems.SALT_WATER_BOTTLE, ModItems.SWAMP_WATER_BOTTLE, ModItems.INFUSORIA, ModItems.ARTEMIA_NAUPLII)) {
            // not sneaking on the ground: nothing (the bottle may be meant for a block)
            stand(h, sp, new BlockPos(5, 3, 5), 90F);
            ItemStack after = use(sp, new ItemStack(item.get(), 2));
            check(after.is(item.get()) && after.getCount() == 2, item.getId() + " poured without sneaking");
            // sneaking onto dirt and farmland
            sp.setShiftKeyDown(true);
            after = use(sp, new ItemStack(item.get()));
            check(after.is(Items.GLASS_BOTTLE), item.getId() + " did not pour out onto dirt: " + after);
            stand(h, sp, new BlockPos(7, 3, 5), 90F);
            after = use(sp, new ItemStack(item.get()));
            check(after.is(Items.GLASS_BOTTLE), item.getId() + " did not pour out onto farmland: " + after);
            sp.setShiftKeyDown(false);
            check(h.getBlockState(new BlockPos(5, 1, 5)).is(Blocks.DIRT) && h.getBlockState(new BlockPos(5, 2, 5)).isAir(), "pouring changed the dirt");
            check(h.getBlockState(new BlockPos(7, 1, 5)).is(Blocks.FARMLAND) && h.getBlockState(new BlockPos(7, 2, 5)).isAir(), "pouring changed the farmland");
            // into the air (looking up)
            stand(h, sp, new BlockPos(12, 1, 12), -90F);
            after = use(sp, new ItemStack(item.get(), 3));
            check(after.is(item.get()) && after.getCount() == 2 && sp.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)),
                    item.getId() + " did not pour out into the air: " + after);
            sp.getInventory().clearContent();
        }
        h.succeed();
    }

    /** In a tank a bottle works as before (infusoria: no fry to feed, the bottle stays full), not poured out. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void bottlesInATankAsBefore(GameTestHelper h) {
        ServerPlayer sp = player(h);
        tank(h, 4, 1, 4, 8000, 0F);
        ItemStack after = useOn(sp, h, new BlockPos(4, 2, 4), new ItemStack(ModItems.INFUSORIA.get()));
        check(after.is(ModItems.INFUSORIA.get()), "the infusoria were poured out on the tank: " + after);
        h.succeed();
    }
}
