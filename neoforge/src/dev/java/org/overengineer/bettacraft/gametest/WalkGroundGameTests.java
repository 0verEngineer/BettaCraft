package org.overengineer.bettacraft.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.foreign.ForeignTerrarium;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;

/**
 * M44: the walker sets {@code onGround} while the animal stands (it moves them with {@code setPos}, so vanilla never did; the
 * chicken flapped its wings all the time): a zombie in glass, a Minecraft chicken and our leopard gecko stand on the ground; a
 * hopping slime is not on the ground while it is in the air, a parrot not while it flies.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WalkGroundGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private WalkGroundGameTests() {}

    private static LandWalker walker(LivingEntity e) {
        if (e instanceof HerpEntity h) return h.walker();
        if (e instanceof Mob m && ModAttachments.GLASS_STATE.has(e)) return ModAttachments.GLASS_STATE.get(m).walker();
        return ForeignTerrarium.walker(e);
    }

    private static final class Watch {
        final LivingEntity e;
        final TankBlockEntity c;
        int grounded, airborne;

        Watch(LivingEntity e, TankBlockEntity c) {
            this.e = e;
            this.c = c;
        }
    }

    @GameTest(template = EMPTY, batch = "bettacraft_m44_walk", timeoutTicks = 600)
    public static void walkerSetsOnGround(GameTestHelper h) {
        TankBlockEntity glass = MobsInGlassGameTests.terrarium(h, 2, 2);
        TankBlockEntity slimes = MobsInGlassGameTests.terrarium(h, 6, 2);
        TankBlockEntity herps = HerpGameTests.terrarium(h, 10);
        herps.getTank().setClimate(new Climate.State(28, 28, 32, 40, 0, 0));
        TankBlockEntity pets = ForeignTerrariumGameTests.terrarium(h, 2, 14, 8, 3, 6);
        pets.getTank().setClimate(new Climate.State(22, 22, 28, 55, 2, 0));
        ForeignLandAnimalsGameTests.lid(h, pets);
        Map<String, Watch> w = new LinkedHashMap<>();
        w.put("zombie", new Watch(MobsInGlassGameTests.put(h, glass, EntityTypes.ZOMBIE), glass));
        w.put("slime", new Watch(MobsInGlassGameTests.put(h, slimes, EntityTypes.SLIME), slimes));
        w.put("gecko", new Watch(HerpGameTests.put(h, herps, HerpSpecies.LEOPARD_GECKO, false, 0), herps));
        w.put("chicken", new Watch(ForeignTerrariumGameTests.put(h, pets, ForeignTerrariumGameTests.container(h, EntityTypes.CHICKEN, false, false)), pets));
        w.put("parrot", new Watch(ForeignTerrariumGameTests.put(h, pets, ForeignTerrariumGameTests.container(h, EntityTypes.PARROT, false, false)), pets));
        for (Watch x : w.values()) x.e.setOnGround(false); // as inserted before M44
        String[] fault = {null};
        h.onEachTick(() -> {
            for (Map.Entry<String, Watch> en : w.entrySet()) {
                Watch x = en.getValue();
                LandWalker lw = walker(x.e);
                if (!x.e.isAlive() || lw == null) continue;
                if (h.getTick() % 40 == 0 && !en.getKey().equals("parrot")) lw.forceActivity(LandWalker.Activity.LAND);
                double g = lw.terrain(h.getLevel(), x.c.getTank()).ground(x.e.getX(), x.e.getZ());
                if (!Double.isFinite(g)) continue;
                double above = x.e.getY() - g;
                if (x.e.onGround()) x.grounded++;
                if (above > 0.1) {
                    x.airborne++;
                    if (x.e.onGround() && fault[0] == null && !lw.onWall()) fault[0] = en.getKey() + " on the ground " + above + " above it";
                }
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(fault[0] == null, String.valueOf(fault[0]));
            for (String k : new String[]{"zombie", "gecko", "chicken"}) {
                h.assertTrue(w.get(k).grounded > 40, k + " stands on the ground: " + w.get(k).grounded + " ticks");
            }
            h.assertTrue(w.get("slime").airborne > 3 && w.get("slime").grounded > 10, "the slime hops: in the air " + w.get("slime").airborne
                    + ", on the ground " + w.get("slime").grounded);
            h.assertTrue(h.getTick() > 300, "watched long enough");
        });
    }

    /**
     * M58: an animal that is blocked more than ten times in a row drops its goal; the step must not read the dropped goal
     * (a server crash, "NullPointerException in LandWalker.tick", seen with Minecraft frogs in a paludarium).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m44_walk", timeoutTicks = 200)
    public static void aStuckWalkerDropsItsGoalWithoutCrashing(GameTestHelper h) {
        TankBlockEntity herps = HerpGameTests.terrarium(h, 10);
        herps.getTank().setClimate(new Climate.State(28, 28, 32, 40, 0, 0));
        HerpEntity gecko = (HerpEntity) HerpGameTests.put(h, herps, HerpSpecies.LEOPARD_GECKO, false, 0);
        h.runAfterDelay(5, () -> {
            LandWalker lw = gecko.walker();
            var t = herps.getTank();
            // a goal beyond the glass: every step is blocked
            lw.goTo(gecko, h.getLevel(), t, new Vec3(t.min().getX() - 4.5, t.min().getY() + 0.1, t.min().getZ() + 0.5), 1000);
            for (int i = 0; i < 40; i++) lw.tick(gecko, h.getLevel(), t, gecko.gait(), false);
            h.succeed();
        });
    }
}
