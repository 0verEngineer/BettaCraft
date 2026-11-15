package org.overengineer.bettacraft.terrarium;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M31 part b: greens for the turtles ({@code #bettacraft:turtle_greens}). Hermann's tortoise eats herbs, dandelion and
 * clover in the wild; Minecraft has dandelions, the clover-like pink petals and short grass and ferns for the herbs (the
 * tag holds them and the oxeye daisy; packs can add more). Sliders and Reeves' turtles eat greens too. One item feeds one
 * animal: by hand on the tank (the hungriest one), or from a feeder (every animal that ate more than {@link #HUNGRY_DAYS}
 * days ago).
 */
public final class TurtleGreens {
    /** From this many days after its last meal a turtle eats greens from the feeder. */
    public static final double HUNGRY_DAYS = 0.33;

    private TurtleGreens() {}

    public static boolean isGreens(ItemStack stack) {
        return stack.is(ModTags.TURTLE_GREENS);
    }

    /** The greens eaters of the tank, hungriest first. */
    public static List<Entity> eaters(ServerLevel level, TankData t) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof TerrariumAnimal a && a.eatsGreens()) out.add(e);
        out.sort(Comparator.comparingDouble((Entity e) -> ((TerrariumAnimal) e).daysSinceFed()).reversed());
        return out;
    }

    /** Greens on a tank: the hungriest greens eater eats one. PASS if it is no greens or nobody eats them. */
    public static ItemInteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!isGreens(stack) || !(level instanceof ServerLevel server)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        List<Entity> list = eaters(server, c.getTank());
        if (list.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Entity pick = list.getFirst();
        ((TerrariumAnimal) pick).feed();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)), pick.getX(), pick.getY() + 0.2, pick.getZ(), 6, 0.1, 0.05, 0.1, 0.02);
        level.playSound(null, pick.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5F, 1.4F);
        TankCare.log(server, c, "log.bettacraft.turtle.greens", TankCare.nameArg(pick), "@" + stack.getDescriptionId());
        player.displayClientMessage(Component.translatable("message.bettacraft.turtle.greens", pick.getDisplayName()), true);
        return ItemInteractionResult.CONSUME;
    }

    /** The feeder: greens for every greens eater that is hungry. Returns the items used. */
    public static int feedFromFeeder(ServerLevel level, TankBlockEntity c, net.neoforged.neoforge.items.IItemHandler items) {
        int used = 0;
        for (Entity e : eaters(level, c.getTank())) {
            if (((TerrariumAnimal) e).daysSinceFed() < HUNGRY_DAYS) continue;
            for (int i = 0; i < items.getSlots(); i++) {
                if (!isGreens(items.getStackInSlot(i))) continue;
                if (items.extractItem(i, 1, false).isEmpty()) continue;
                ((TerrariumAnimal) e).feed();
                used++;
                break;
            }
        }
        return used;
    }
}
