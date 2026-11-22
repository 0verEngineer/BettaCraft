package org.overengineer.bettacraft.terrarium;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * M54: the {@link Habits} of an amphibian or reptile outside a tank (C1 of the milestone: the same behaviour in the world as in
 * a tank). Vanilla goals only, all of them idle inside a tank (the walker moves the animal there):
 * <ul>
 *     <li>{@link Rest}: while it is not active (a gecko by day, a frog at night) it rests for a while; one that hides by its
 *     habits first goes to a covered spot nearby (under leaves or an overhang);</li>
 *     <li>{@link Bask}: a basking species (turtles, the leopard gecko, the snake) lies in the sun for a while on a bright day;</li>
 *     <li>{@link Stroll}: walks at a calm pace, now and then a sprint; more often while it is active;</li>
 *     <li>a player who comes close makes the small and shy ones run (geckos, frogs, the mudskipper, the snake).</li>
 * </ul>
 * Without habits (the switch off) only the old stroll is left.
 */
public final class HerpWorldGoals {
    private HerpWorldGoals() {}

    /** Adds the goals of the habits (they check the habits and the switch whenever they start). */
    public static void register(TerrariumAnimalEntity e, GoalSelector goals) {
        goals.addGoal(2, new AvoidEntityGoal<>(e, Player.class, 3.5F, 1.0, 1.6) {
            @Override
            public boolean canUse() {
                Habits h = e.habits();
                return h != null && !e.inTank() && shy(e) && super.canUse();
            }
        });
        goals.addGoal(3, new Rest(e));
        goals.addGoal(4, new Bask(e));
        goals.addGoal(4, new WallClimb(e));
    }

    /**
     * M62: a wall climber ({@link WorldClimbing.Style#WALL}, the mudskipper on roots and trunks) now and then goes to a face
     * nearby and climbs it: half the time to the top and over, else part of the way to hold on there for a while (then it comes
     * down again). More often while it is active by its hours.
     */
    public static final class WallClimb extends Goal {
        private final TerrariumAnimalEntity e;
        @Nullable
        private WorldClimbing.Spot spot;
        private int ticks;
        private boolean climbed;

        WallClimb(TerrariumAnimalEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        private static boolean wallStyle(WorldClimbing.Style s) {
            return s == WorldClimbing.Style.WALL || s == WorldClimbing.Style.ROOTS;
        }

        @Override
        public boolean canUse() {
            if (!(e instanceof HerpEntity h) || e.inTank() || !e.onGround() || e.isInWater() || h.climbingInWorld()) return false;
            WorldClimbing.Style st = h.worldClimb();
            if (!wallStyle(st)) return false;
            Habits hb = e.habits();
            Level l = e.level();
            boolean active = hb == null || hb.active(l.getDayTime(), l.isDay());
            if (e.getRandom().nextInt(active ? 300 : 900) != 0) return false;
            spot = WorldClimbing.wallSpot(l, e.blockPosition(), 6, st, e.getRandom());
            return spot != null;
        }

        @Override
        public void start() {
            ticks = 0;
            climbed = false;
            if (spot == null) return;
            if (e instanceof HerpEntity h && e.getRandom().nextBoolean() && spot.height() > 2) {
                h.climbPart(2 + e.getRandom().nextInt(Math.min(spot.height(), 6) - 1), 200 + e.getRandom().nextInt(600));
            }
            Vec3 foot = Vec3.atBottomCenterOf(spot.foot());
            e.getNavigation().moveTo(foot.x, foot.y, foot.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            if (spot == null || e.inTank() || !(e instanceof HerpEntity h)) return false;
            if (h.climbingInWorld()) {
                climbed = true;
                return true;
            }
            return !climbed && ticks < 300;
        }

        @Override
        public void tick() {
            ticks++;
            if (spot == null || !(e instanceof HerpEntity h) || h.climbingInWorld()) return;
            Vec3 foot = Vec3.atBottomCenterOf(spot.foot());
            double dx = foot.x - e.getX(), dz = foot.z - e.getZ();
            if (dx * dx + dz * dz < 0.6 * 0.6 || e.getNavigation().isDone() && dx * dx + dz * dz < 1.5 * 1.5) {
                // at the foot: into the face (the climb starts when it touches it)
                e.getNavigation().stop();
                e.getMoveControl().setWantedPosition(foot.x + spot.face().getStepX(), foot.y, foot.z + spot.face().getStepZ(), 1.0);
            } else if (e.getNavigation().isDone()) {
                e.getNavigation().moveTo(foot.x, foot.y, foot.z, 1.0);
            }
        }

        @Override
        public void stop() {
            spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Small and shy animals run from a player (not the turtles, they pull in their heads). */
    static boolean shy(TerrariumAnimalEntity e) {
        return e instanceof HerpEntity h && !h.species().turtle() && !h.isBrumating();
    }

    /** Rests while it is not active by its hours; one that hides goes under cover first. */
    public static final class Rest extends Goal {
        private final TerrariumAnimalEntity e;
        private int ticks;
        @Nullable
        private Vec3 cover;

        Rest(TerrariumAnimalEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Habits h = e.habits();
            if (h == null || e.inTank() || !e.onGround() && !e.isInWater()) return false;
            Level l = e.level();
            boolean active = h.active(l.getDayTime(), l.isDay());
            return e.getRandom().nextInt(active ? 900 : 120) == 0;
        }

        @Override
        public void start() {
            Habits h = e.habits();
            Level l = e.level();
            boolean active = h != null && h.active(l.getDayTime(), l.isDay());
            ticks = (active ? 200 : 600) + e.getRandom().nextInt(active ? 300 : 900);
            // a hider (a gecko by day) always looks for cover, the others now and then
            cover = h != null && (h.hide() >= 0.2 || e.getRandom().nextDouble() < h.hide() * 2.5) ? cover(e) : null;
            if (cover == null || !e.getNavigation().moveTo(cover.x, cover.y, cover.z, 1.0)) {
                cover = null;
                e.getNavigation().stop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return ticks > 0 && e.habits() != null && !e.inTank();
        }

        @Override
        public void tick() {
            ticks--;
            boolean there = cover == null || e.getNavigation().isDone();
            if (there && cover != null && e.position().distanceToSqr(cover) > 0.3 * 0.3) {
                // the path ends a block short when the spot is next to it: the last bit straight into the cover
                e.getMoveControl().setWantedPosition(cover.x, cover.y, cover.z, 1.0);
                there = false;
            }
            e.worldResting = there;
            if (there) e.getNavigation().stop();
        }

        @Override
        public void stop() {
            e.worldResting = false;
            cover = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** The nearest covered spot within 6 blocks (no sky above it, ground below, room for it), or null. */
    @Nullable
    static Vec3 cover(TerrariumAnimalEntity e) {
        Level l = e.level();
        BlockPos at = e.blockPosition();
        if (!l.canSeeSky(at)) return Vec3.atBottomCenterOf(at);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = dx * dx + dz * dz + dy * dy * 2;
            if (d >= bestD) continue;
            p.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
            if (!l.isLoaded(p) || l.canSeeSky(p)) continue;
            if (!l.getBlockState(p).getCollisionShape(l, p).isEmpty() || !l.getFluidState(p).isEmpty()) continue;
            BlockPos below = p.below();
            if (!l.getBlockState(below).isFaceSturdy(l, below, Direction.UP)) continue;
            best = p.immutable();
            bestD = d;
        }
        return best == null ? null : Vec3.atBottomCenterOf(best);
    }

    /** Lies in the sun on a bright day (a basking species); the renderers show it basking (head up, legs out). */
    public static final class Bask extends Goal {
        private final TerrariumAnimalEntity e;
        private int ticks;

        Bask(TerrariumAnimalEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Habits h = e.habits();
            if (h == null || e.inTank() || !e.onGround()) return false;
            Level l = e.level();
            if (!l.isDay() || l.isRaining() || !l.canSeeSky(e.blockPosition().above())) return false;
            TerrariumNeeds n = TerrariumAnimals.needs(e);
            return n != null && n.basking() && e.getRandom().nextInt(200) == 0;
        }

        @Override
        public void start() {
            ticks = 300 + e.getRandom().nextInt(500);
            e.getNavigation().stop();
            e.worldBasking = true;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks > 0 && e.habits() != null && !e.inTank() && e.level().isDay();
        }

        @Override
        public void tick() {
            ticks--;
        }

        @Override
        public void stop() {
            e.worldBasking = false;
        }
    }

    /**
     * The walk around outside a tank: with habits a calm pace (slower than before) and now and then a sprint (its fast phase);
     * more often while it is active. Without habits the old stroll (0.8, every 6 s on average).
     */
    public static final class Stroll extends RandomStrollGoal {
        private final TerrariumAnimalEntity e;

        public Stroll(TerrariumAnimalEntity e) {
            super(e, 0.8);
            this.e = e;
        }

        @Override
        public boolean canUse() {
            if (e.inTank()) return false;
            Habits h = e.habits();
            if (h != null) {
                Level l = e.level();
                setInterval(h.active(l.getDayTime(), l.isDay()) ? 60 : 300);
            } else {
                setInterval(120);
            }
            return super.canUse();
        }

        /** With habits: shorter walks (a small animal does not cross ten blocks at a time). */
        @javax.annotation.Nullable
        @Override
        protected Vec3 getPosition() {
            return e.habits() != null ? net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(e, 6, 3) : super.getPosition();
        }

        @Override
        public void start() {
            Habits h = e.habits();
            double speed = speedModifier;
            if (h != null) {
                boolean dash = e.getRandom().nextDouble() < h.dashChance();
                speed = 0.6 * Math.min(1.6, h.tempo()) * (dash ? Math.min(2.6, h.dashFactor()) : 1.0);
            }
            e.getNavigation().moveTo(wantedX, wantedY, wantedZ, speed);
        }
    }
}
