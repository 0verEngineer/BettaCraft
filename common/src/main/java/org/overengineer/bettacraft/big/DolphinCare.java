package org.overengineer.bettacraft.big;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M30: the basic care of a vanilla dolphin in a big sea water tank (no dolphin of our own: the vanilla model and AI stay).
 * <ul>
 *     <li><b>Breathing</b>: a dolphin breathes air at the surface. In the tank its air and its moisture stay full (the tank
 *     is open at the top for it; the glass keeps it in), so it does not drown or dry out.</li>
 *     <li><b>Food</b>: raw fish ({@code #minecraft:fishes}) by hand: right-click the tank with it; M30 part c: or from a
 *     feeder on the tank (raw fish in its slots, one per dolphin and feeding), and a hungry dolphin (a day without fish) hunts
 *     one of the small fish of its tank a day (tankmates, bettas, frogs; not the big animals). After
 *     {@code bigAnimalRules.dolphinHungryDays} without fish it loses health (2 hearts a day), fish heals it a little.</li>
 *     <li><b>Water</b>: sea water only ({@link BigAnimals#DOLPHIN_MIN_SALT}); in too fresh water it loses health too.</li>
 * </ul>
 * It only goes into a tank by the size rule (a 2.6 m dolphin from 32 x 16 x 8 blocks, see {@link BigAnimals}).
 */
public final class DolphinCare {
    public static final String FED = "bettacraft:dolphin_fed", HARM = "bettacraft:dolphin_harm";
    /** Health lost per day while hungry or in too fresh water. */
    public static final double HARM_PER_DAY = 4.0;
    private static final long LOG_TICKS = 6000;

    private DolphinCare() {}

    public static List<Dolphin> dolphins(ServerLevel level, TankData t) {
        List<Dolphin> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (e instanceof Dolphin d) out.add(d);
        return out;
    }

    /** Days since the dolphin last got fish (a dolphin new in the tank counts as just fed). */
    public static double daysHungry(Dolphin d, long now) {
        var tag = org.overengineer.bettacraft.platform.Platform.get().persistentData(d);
        if (!tag.contains(FED)) tag.putLong(FED, now);
        return Math.max(0, (now - tag.getLongOr(FED, 0L)) / 24000.0);
    }

    /** One care step of the tank ({@code days} since the last). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return;
        List<Dolphin> list = dolphins(level, t);
        if (list.isEmpty()) return;
        long now = level.getGameTime();
        double hungryAfter = ServerConfig.getOr(ServerConfig.BIG_DOLPHIN_HUNGRY_DAYS, 2.0);
        boolean fresh = t.salinity() < BigAnimals.DOLPHIN_MIN_SALT;
        for (Dolphin d : list) {
            d.setAirSupply(d.getMaxAirSupply());
            d.setMoisntessLevel(2400);
            if (!org.overengineer.bettacraft.config.Care.needs()) { // M33 part c, care.mode DECOR: always fed, no harm
                org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(FED, now);
                org.overengineer.bettacraft.platform.Platform.get().persistentData(d).remove(HARM);
                continue;
            }
            // M30 part c: a dolphin a day without fish hunts one of the small fish of its tank
            if (daysHungry(d, now) > Math.min(1.0, hungryAfter * 0.5) && org.overengineer.bettacraft.config.Care.canDie()) hunt(level, c, d, now);
            boolean hungry = daysHungry(d, now) > hungryAfter;
            if (!hungry && !fresh) continue;
            var tag = org.overengineer.bettacraft.platform.Platform.get().persistentData(d);
            double harm = tag.getDoubleOr(HARM, 0d) + HARM_PER_DAY * days * ((hungry ? 1 : 0) + (fresh ? 1 : 0));
            if (harm >= 1) {
                float dmg = (float) Math.floor(harm);
                harm -= dmg;
                d.hurt(d.damageSources().starve(), dmg);
            }
            tag.putDouble(HARM, harm);
            if (now - tag.getLongOr("bettacraft:dolphin_log", 0L) > LOG_TICKS) {
                tag.putLong("bettacraft:dolphin_log", now);
                TankCare.log(level, c, hungry ? "log.bettacraft.dolphin_hungry" : "log.bettacraft.dolphin_fresh", TankCare.nameArg(d));
            }
        }
    }

    /** M30 part c: the dolphin catches and eats one small fish of the tank (the nearest), if there is one. */
    private static void hunt(ServerLevel level, TankBlockEntity c, Dolphin d, long now) {
        Entity prey = null;
        double best = Double.MAX_VALUE;
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            boolean small = e instanceof org.overengineer.bettacraft.entity.tankmate.TankmateEntity m ? !m.species().isLarge()
                    : e instanceof org.overengineer.bettacraft.entity.betta.BettaEntity || e instanceof org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
            if (!small || !e.isAlive()) continue;
            double dist = e.distanceToSqr(d);
            if (dist < best) {
                best = dist;
                prey = e;
            }
        }
        if (prey == null) return;
        Component name = prey.getDisplayName();
        org.overengineer.bettacraft.tankmates.TankmateBreeding.eat(level, c, prey);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(FED, now);
        level.playSound(null, d.blockPosition(), SoundEvents.DOLPHIN_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        TankCare.log(level, c, "log.bettacraft.dolphin_hunted", TankCare.nameArg(d), name.getString());
    }

    /**
     * M30 part c: a feeder on the tank feeds the dolphins raw fish from its slots: one fish for each dolphin that has not had
     * fish for a third of a day. Returns the fish used.
     */
    public static int feedFromFeeder(ServerLevel level, TankBlockEntity c, org.overengineer.bettacraft.platform.item.IItemHandler items) {
        List<Dolphin> list = dolphins(level, c.getTank());
        if (list.isEmpty()) return 0;
        long now = level.getGameTime();
        int used = 0;
        for (Dolphin d : list) {
            if (daysHungry(d, now) < 0.33) continue;
            for (int i = 0; i < items.getSlots(); i++) {
                if (!items.getStackInSlot(i).is(ItemTags.FISHES)) continue;
                if (items.extractItem(i, 1, false).isEmpty()) continue;
                org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(FED, now);
                d.heal(2);
                used++;
                break;
            }
        }
        return used;
    }

    /** Raw fish on a tank with dolphins: the hungriest one eats it. PASS if there is no dolphin or no fish. */
    public static InteractionResult feed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!stack.is(ItemTags.FISHES)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        List<Dolphin> list = dolphins(server, c.getTank());
        if (list.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        long now = server.getGameTime();
        Dolphin pick = list.getFirst();
        for (Dolphin d : list) if (daysHungry(d, now) > daysHungry(pick, now)) pick = d;
        org.overengineer.bettacraft.platform.Platform.get().persistentData(pick).putLong(FED, now);
        pick.heal(2);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        server.sendParticles(ParticleTypes.HEART, pick.getX(), pick.getY() + 0.6, pick.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
        level.playSound(null, pick.blockPosition(), SoundEvents.DOLPHIN_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.bettacraft.big.dolphin_fed", pick.getDisplayName()), true);
        return InteractionResult.CONSUME;
    }
}
