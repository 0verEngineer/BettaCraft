package org.overengineer.bettacraft.client;

import net.minecraft.world.entity.Mob;

/**
 * Development only: the client smoke scenes freeze animals for steady screenshots and tag them. Outside a smoke run
 * the tagged animals are released again when they load, so copied smoke worlds are playable (fish swim).
 */
public final class SmokeFreeze {
    static final String TAG = "bettacraft_smoke_frozen";

    private SmokeFreeze() {}

    /** Freezes {@code m} for a screenshot (no AI, optionally no gravity) and tags it for release. */
    public static void freeze(Mob m, boolean noGravity) {
        m.setNoAi(true);
        if (noGravity) m.setNoGravity(true);
        m.addTag(TAG);
    }

    /** Outside smoke runs: release tagged animals as they join a server level. */
    static void registerRelease() {
        org.overengineer.bettacraft.platform.event.GameEvents.listen(org.overengineer.bettacraft.platform.event.EntityJoinLevelEvent.class, e -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m)) return;
            // untagged: tank animals frozen by smoke runs before the tag existed (tank animals never use NoAI otherwise)
            boolean legacy = m.isNoAi() && org.overengineer.bettacraft.tank.TankInhabitants.hasTankRef(m);
            if (!m.entityTags().contains(TAG) && !legacy) return;
            m.setNoAi(false);
            m.setNoGravity(false);
            m.removeTag(TAG);
        });
    }
}
