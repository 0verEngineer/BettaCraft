package org.overengineer.bettacraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.TransportTubItem;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.containers.CatchGuard;
import org.overengineer.bettacraft.containers.ContainerRules;
import org.overengineer.bettacraft.mobs.MobJarItem;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TerrariumBoxItem;

/**
 * M62 part 7 GameTests: the catch guard ({@link CatchGuard}) and the confirmation for every catcher. A leopard gecko caught
 * with the terrarium box stays in its box when the box is used (and even confirmed) right after the catch, and comes out only
 * after the pause and the confirmation; the box of a foreign land animal, the transport tub and the mob jar ask first like a
 * bucket; with the switch off the confirmed box empties at once.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatchGuardGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CatchGuardGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer sp = MockPlayers.serverPlayer(h);
        sp.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        sp.getInventory().selected = 0;
        CatchGuard.reset(sp);
        return sp;
    }

    /** The player stands at {@code rel} looking straight down. */
    private static void stand(GameTestHelper h, ServerPlayer sp, BlockPos rel) {
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(rel));
        sp.moveTo(at.x, at.y, at.z, 0F, 90F);
        sp.setYHeadRot(0F);
    }

    private static ItemStack use(ServerPlayer sp) {
        sp.gameMode.useItem(sp, sp.serverLevel(), sp.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static ItemStack useOn(ServerPlayer sp, GameTestHelper h, BlockPos rel) {
        BlockPos pos = h.absolutePos(rel);
        sp.gameMode.useItemOn(sp, sp.serverLevel(), sp.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false));
        return sp.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static <T extends Entity> List<T> around(GameTestHelper h, Class<T> type, BlockPos rel) {
        return h.getLevel().getEntitiesOfClass(type, new AABB(h.absolutePos(rel)).inflate(3));
    }

    private static List<? extends Entity> ofType(GameTestHelper h, EntityType<?> type, BlockPos rel) {
        return h.getLevel().getEntities(type, new AABB(h.absolutePos(rel)).inflate(3), e -> true);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    /** A leopard gecko on stone at {@code rel} (x, 2, z), caught by the player with an empty terrarium box. */
    private static ItemStack catchGecko(GameTestHelper h, ServerPlayer sp, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.STONE.defaultBlockState());
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> type = (EntityType<? extends Mob>) ModEntities.herp(HerpSpecies.LEOPARD_GECKO);
        Mob gecko = h.spawn(type, rel);
        gecko.setNoAi(true);
        stand(h, sp, rel.above(2));
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TERRARIUM_BOX.get()));
        sp.interactOn(gecko, InteractionHand.MAIN_HAND);
        ItemStack held = sp.getItemInHand(InteractionHand.MAIN_HAND);
        check(held.is(ModItems.herpContainer(HerpSpecies.LEOPARD_GECKO)), "the box did not catch the gecko: " + held);
        check(gecko.isRemoved(), "the caught gecko is still in the world");
        return held;
    }

    /** The reported bug: catch, use at once (a bouncing double click): the gecko stays in its box. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aCaughtAnimalStaysInTheBoxRightAfterTheCatch(GameTestHelper h) {
        ServerPlayer sp = player(h);
        BlockPos at = new BlockPos(5, 2, 5);
        catchGecko(h, sp, at);
        ItemStack after = use(sp);
        check(after.is(ModItems.herpContainer(HerpSpecies.LEOPARD_GECKO)), "the second click let the gecko go: " + after);
        // even a confirmation right after the catch waits (the double click hit "Empty")
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.herpContainer(HerpSpecies.LEOPARD_GECKO)), "released right after the catch");
        check(around(h, HerpEntity.class, at).isEmpty(), "a gecko is back in the world right after the catch");
        h.runAfterDelay(CatchGuard.TICKS + 2, () -> {
            // after the pause still not without asking ...
            ItemStack later = use(sp);
            check(later.is(ModItems.herpContainer(HerpSpecies.LEOPARD_GECKO)), "the box emptied without asking: " + later);
            check(around(h, HerpEntity.class, at).isEmpty(), "a gecko was let go without asking");
            // ... and with the confirmation it comes out
            ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
            check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.TERRARIUM_BOX.get()), "the confirmed box did not empty: "
                    + sp.getItemInHand(InteractionHand.MAIN_HAND));
            check(around(h, HerpEntity.class, at).size() == 1, "the confirmed gecko was not let go");
            h.succeed();
        });
    }

    /** Every other catcher of the mod holding an animal asks first: the box of a foreign animal, the transport tub, the mob jar. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void everyCatcherNeedsTheConfirmation(GameTestHelper h) {
        ServerPlayer sp = player(h);
        // the terrarium box with a land animal of another mod (here a vanilla rabbit)
        h.setBlock(new BlockPos(5, 1, 5), Blocks.STONE.defaultBlockState());
        stand(h, sp, new BlockPos(5, 4, 5));
        Entity rabbit = EntityType.RABBIT.create(h.getLevel());
        sp.setItemInHand(InteractionHand.MAIN_HAND, TerrariumBoxItem.fillWith(rabbit));
        rabbit.discard();
        ItemStack after = use(sp);
        check(TerrariumBoxItem.isFilled(after), "the box of a foreign animal emptied without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(!TerrariumBoxItem.isFilled(sp.getItemInHand(InteractionHand.MAIN_HAND)), "the confirmed box did not empty");
        check(ofType(h, EntityType.RABBIT, new BlockPos(5, 2, 5)).size() == 1, "the rabbit is not out");
        // the transport tub into water
        for (int x = 11; x <= 15; x++) for (int z = 4; z <= 8; z++) for (int y = 1; y <= 3; y++) {
            boolean wall = x == 11 || x == 15 || z == 4 || z == 8 || y == 1;
            h.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState());
        }
        stand(h, sp, new BlockPos(13, 5, 6));
        Entity cod = EntityType.COD.create(h.getLevel());
        sp.setItemInHand(InteractionHand.MAIN_HAND, TransportTubItem.fillWith(cod));
        cod.discard();
        after = use(sp);
        check(TransportTubItem.isFilled(after), "the tub emptied without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(!TransportTubItem.isFilled(sp.getItemInHand(InteractionHand.MAIN_HAND)), "the confirmed tub did not empty: " + sp.getItemInHand(InteractionHand.MAIN_HAND));
        check(ofType(h, EntityType.COD, new BlockPos(13, 3, 6)).size() == 1, "the cod is not in the water");
        // the mob jar onto a block
        h.setBlock(new BlockPos(20, 1, 5), Blocks.STONE.defaultBlockState());
        stand(h, sp, new BlockPos(20, 4, 5));
        Mob zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.setHealth(1F);
        sp.setItemInHand(InteractionHand.MAIN_HAND, MobJarItem.fillWith(zombie));
        zombie.discard();
        after = useOn(sp, h, new BlockPos(20, 1, 5));
        check(MobJarItem.isFilled(after), "the mob jar emptied without asking");
        ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
        check(!MobJarItem.isFilled(sp.getItemInHand(InteractionHand.MAIN_HAND)), "the confirmed jar did not empty: " + sp.getItemInHand(InteractionHand.MAIN_HAND));
        check(ofType(h, EntityType.ZOMBIE, new BlockPos(20, 2, 5)).size() == 1, "the zombie is not out");
        ofType(h, EntityType.ZOMBIE, new BlockPos(20, 2, 5)).forEach(Entity::discard);
        h.succeed();
    }

    /** The switch off: no pause, the confirmed box empties right after the catch (the confirmation itself stays). */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_catch_guard", timeoutTicks = 100)
    public static void catchGuardSwitchOff(GameTestHelper h) {
        boolean saved = Features.enabled(Feature.CATCH_GUARD);
        Features.set(Feature.CATCH_GUARD, false);
        try {
            ServerPlayer sp = player(h);
            BlockPos at = new BlockPos(5, 2, 5);
            catchGecko(h, sp, at);
            ItemStack after = use(sp);
            check(after.is(ModItems.herpContainer(HerpSpecies.LEOPARD_GECKO)), "without the guard the box still asks first: " + after);
            ContainerRules.confirm(sp, InteractionHand.MAIN_HAND);
            check(sp.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.TERRARIUM_BOX.get()), "without the guard the confirmed box did not empty at once");
            check(around(h, HerpEntity.class, at).size() == 1, "the gecko is not out");
        } finally {
            Features.set(Feature.CATCH_GUARD, saved);
        }
        h.succeed();
    }
}
