package org.overengineer.bettacraft.mobs;

import javax.annotation.Nullable;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.terrarium.LandWalker;

/**
 * M31 part d: the running state of a mob in glass (a transient entity attachment, server only): its walker, its tank
 * (looked up every second) and how long it has been under water (a zombie becomes a drowned).
 */
public final class GlassMobState {
    final LandWalker walker = new LandWalker();
    @Nullable
    TankBlockEntity controller;
    int nextResolve;
    boolean openTop;
    boolean shrunk;
    int underWater;

    public LandWalker walker() {
        return walker;
    }

    @Nullable
    public TankBlockEntity controller() {
        return controller;
    }
}
