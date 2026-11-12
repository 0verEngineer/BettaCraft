package org.overengineer.bettacraft.tank.interior;

import net.minecraft.world.level.block.Block;

/**
 * Floor of a tank interior. In M3 a shift-right-click with any block sets the floor texture (substrate) per block;
 * in M0 it is a plain unbreakable block.
 */
public class InteriorFloorBlock extends Block {
    public InteriorFloorBlock(Properties properties) {
        super(properties);
    }
}
