package org.overengineer.bettacraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * A glass bottle used on water whose block lies in a betta spawn biome gives a bottle of swamp water, in an ocean
 * ({@code #bettacraft:salt_water_biomes}) a bottle of salt water (PLAN 6.3).
 * The biome at the position of the clicked water block decides, found by the same fluid ray trace as vanilla. Since M12
 * swamp water sometimes brings up duckweed as well (server config floatingPlants.bottleDuckweedChance).
 */
public final class SwampWaterEvents {
    private SwampWaterEvents() {}

}
