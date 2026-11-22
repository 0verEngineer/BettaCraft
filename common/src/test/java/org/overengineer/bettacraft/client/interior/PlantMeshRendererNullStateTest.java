package org.overengineer.bettacraft.client.interior;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * M61: other mods call {@code ClientLevel.sendBlockUpdated(pos, null, null, flags)} (Modern Dynamics re-meshes its pipes so,
 * from {@code MdBlockEntity.remesh}, while the client reads the block entity data of a chunk). Vanilla never reads the two
 * states there; our hook ({@code ClientLevelMixin} -> {@link PlantMeshRenderer#onBlockChanged}) must not fail on them
 * either, or the packet fails and the player is thrown out of the server.
 * <p>
 * The JUnit tests run without a Minecraft bootstrap, so there is no real block state here (even loading the class
 * {@code BlockState} needs the registries). The calls with one real state (stone, a plant) and the real
 * {@code ClientLevel} are in the client smoke M61 ({@code DevClientSmokeM61}), with Modern Dynamics itself under -Ppipes.
 */
class PlantMeshRendererNullStateTest {
    @Test
    void bothStatesMissingAsModernDynamicsSendsThem() {
        // exactly the call of the crash report (no level either: no plant block, so nothing to do). Both states are read
        // one after the other, so this also fails when only one of them is guarded.
        assertDoesNotThrow(() -> PlantMeshRenderer.onBlockChanged(null, BlockPos.ZERO, null, null));
    }

    @Test
    void aMissingStateIsNoPlant() {
        // each half on its own: a missing old state (something appeared) and a missing new one (something went)
        assertFalse(PlantMeshRenderer.meshed(null));
    }
}
