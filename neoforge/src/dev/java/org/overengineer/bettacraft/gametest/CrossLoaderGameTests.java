package org.overengineer.bettacraft.gametest;

import java.util.List;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.overengineer.bettacraft.platform.CrossLoaderAttachments;
import org.overengineer.bettacraft.registry.ModAttachments;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;

/**
 * A world saved on the other loader (NeoForge keeps our entity attachments under "neoforge:attachments", Fabric under
 * "fabric:attachments"): its animals keep their tanks. Before the fix a showcase built on NeoForge lost every tank_ref on
 * Fabric: the terrarium animals on the land became free animals, the crested gecko climbed out over the open top, the menu
 * listed no animals and the mobs in glass lost their data.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrossLoaderGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CrossLoaderGameTests() {}

    /** The entity saved, its attachments moved into the other loader's container, loaded again as a new entity. */
    private static Entity reloadFromOtherLoader(GameTestHelper h, Entity e) {
        ServerLevel level = h.getLevel();
        CompoundTag tag = org.overengineer.bettacraft.util.NbtCompat.saveEntityWithoutId(e);
        String own = tag.getCompoundOrEmpty(CrossLoaderAttachments.NEOFORGE).contains(BettaCraft.id("tank_ref").toString())
                ? CrossLoaderAttachments.NEOFORGE : CrossLoaderAttachments.FABRIC;
        String other = own.equals(CrossLoaderAttachments.NEOFORGE) ? CrossLoaderAttachments.FABRIC : CrossLoaderAttachments.NEOFORGE;
        if (tag.getCompoundOrEmpty(own).getCompound(BettaCraft.id("tank_ref").toString()).isEmpty()) {
            throw new GameTestFailure(e.getType() + ": no tank_ref saved: " + tag.keySet());
        }
        tag.put(other, tag.getCompoundOrEmpty(own).copy());
        tag.remove(own);
        EntityType<?> type = e.getType();
        e.discard();
        Entity back = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
        if (back == null) throw new GameTestFailure("no " + type);
        back.load(org.overengineer.bettacraft.util.NbtCompat.input(level.registryAccess(), tag));
        if (!level.addFreshEntity(back)) throw new GameTestFailure(type + " not added again");
        return back;
    }

    /**
     * A leopard gecko and a crested gecko on the land of a terrarium and a zombie in glass, saved as by the other loader and
     * loaded: each keeps its tank (and the zombie its glass data), stays listed after the inhabitants are counted and the
     * geckos stay bound to their tank.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void animalsKeepTheirTankInAWorldOfTheOtherLoader(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity terra = HerpGameTests.terrarium(h, 2);
        TankBlockEntity glass = MobsInGlassGameTests.terrarium(h, 14, 2);
        HerpEntity leo = HerpGameTests.put(h, terra, HerpSpecies.LEOPARD_GECKO, true, 0);
        HerpEntity crested = HerpGameTests.put(h, terra, HerpSpecies.CRESTED_GECKO, false, 0);
        var zombie = MobsInGlassGameTests.put(h, glass, EntityType.ZOMBIE);
        List<Entity> back = List.of(reloadFromOtherLoader(h, leo), reloadFromOtherLoader(h, crested), reloadFromOtherLoader(h, zombie));
        for (Entity e : back) {
            TankBlockEntity c = e.getType() == EntityType.ZOMBIE ? glass : terra;
            h.assertTrue(TankInhabitants.hasTankRef(e) && ModAttachments.TANK_REF.get(e).tankId().equals(c.getTank().id()),
                    e.getType() + " lost its tank in a world of the other loader");
            h.assertTrue(TankInhabitants.resolveController(level, e) == c, e.getType() + " not bound to its tank");
        }
        h.assertTrue(GlassMobs.isGlassMob(back.get(2)), "the zombie lost its glass data");
        TankInhabitants.reconcile(level, terra);
        TankInhabitants.reconcile(level, glass);
        h.assertTrue(terra.getTank().inhabitants().contains(back.get(0).getUUID()) && terra.getTank().inhabitants().contains(back.get(1).getUUID()),
                "the geckos are listed in their terrarium");
        h.assertTrue(glass.getTank().inhabitants().contains(back.get(2).getUUID()), "the zombie is listed in its tank");
        h.runAfterDelay(60, () -> {
            for (Entity e : back.subList(0, 2)) {
                h.assertTrue(e.isAlive() && ((HerpEntity) e).inTank() && TankInhabitants.inside(terra.getTank(), e), e.getType() + " left its terrarium: " + e.position());
            }
            h.succeed();
        });
    }
}
