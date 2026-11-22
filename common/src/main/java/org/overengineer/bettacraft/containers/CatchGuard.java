package org.overengineer.bettacraft.containers;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;

/**
 * M62 part 7: the catch guard. Right after a player caught an animal (or clicked one with something in the hand) no
 * container of theirs lets an animal go for {@link #TICKS} ticks: a second click of a bouncing mouse button or a held key
 * does not release the animal that was just caught (it used to come right out of a terrarium box again). The confirmation
 * before every release ({@link ContainerRules#release}) stays; this only adds the short pause after a catch.
 * <p>
 * Kept per side: the client knows it from its own click (no confirmation screen pops up), the server from the click it
 * receives and from every catch in the code ({@link #caught}). Switch {@code features.equipment.catchGuard}.
 */
public final class CatchGuard {
    /** The pause after a catch (half a second). */
    public static final int TICKS = 10;
    /** Game time of the last catch per player (client and server players are different objects, each with its own clock). */
    private static final Map<Player, Long> LAST = Collections.synchronizedMap(new WeakHashMap<>());

    private CatchGuard() {}

    /** The player just caught an animal (or clicked one): no release for the next {@link #TICKS} ticks. */
    public static void caught(@Nullable Player player) {
        if (player != null) LAST.put(player, player.level().getGameTime());
    }

    /** Whether a release of this player waits now (the guard is on and the last catch is less than {@link #TICKS} ago). */
    public static boolean guarded(@Nullable Player player) {
        if (player == null || !Features.enabled(Feature.CATCH_GUARD)) return false;
        Long at = LAST.get(player);
        if (at == null) return false;
        long d = player.level().getGameTime() - at;
        return d >= 0 && d < TICKS;
    }

    /** GameTests: forgets the last catch of the player. */
    public static void reset(@Nullable Player player) {
        if (player != null) LAST.remove(player);
    }

    /**
     * Every click on a living animal with something in the hand counts as a catch (on both sides, before any handler can
     * take the click): buckets, bags, boxes, jars, tubs and the nets of the mod, vanilla buckets and other mods' catchers.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().isEmpty() || !(event.getTarget() instanceof LivingEntity) || event.getTarget() instanceof Player) return;
        caught(event.getEntity());
    }
}
