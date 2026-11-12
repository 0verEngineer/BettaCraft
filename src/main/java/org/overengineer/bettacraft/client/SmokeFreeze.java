package org.overengineer.bettacraft.client;

import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

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
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob m)) return;
            // untagged: tank animals frozen by smoke runs before the tag existed (tank animals never use NoAI otherwise)
            boolean legacy = m.isNoAi() && org.overengineer.bettacraft.tank.TankInhabitants.hasTankRef(m);
            if (!m.getTags().contains(TAG) && !legacy) return;
            m.setNoAi(false);
            m.setNoGravity(false);
            m.removeTag(TAG);
        });
    }
}
