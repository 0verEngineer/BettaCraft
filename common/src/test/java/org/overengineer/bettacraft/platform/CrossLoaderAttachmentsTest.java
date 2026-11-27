package org.overengineer.bettacraft.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** Only our entries of the other loader's attachment container are taken. */
class CrossLoaderAttachmentsTest {
    @Test
    void onlyOurIds() {
        CompoundTag container = new CompoundTag();
        CompoundTag ref = new CompoundTag();
        ref.putInt("schema", 1);
        container.put("bettacraft:tank_ref", ref);
        container.put("bettacraft:glass_mob", new CompoundTag());
        container.put("othermod:data", new CompoundTag());
        var ours = CrossLoaderAttachments.ours(container);
        assertEquals(Set.of("bettacraft:tank_ref", "bettacraft:glass_mob"), ours.keySet());
        assertEquals(ref, ours.get("bettacraft:tank_ref"));
    }

    @Test
    void emptyContainer() {
        assertTrue(CrossLoaderAttachments.ours(new CompoundTag()).isEmpty());
    }
}
