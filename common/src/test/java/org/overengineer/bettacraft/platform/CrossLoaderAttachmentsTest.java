package org.overengineer.bettacraft.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** Our entity attachments of a world saved by the other loader are copied into this loader's container. */
class CrossLoaderAttachmentsTest {
    private static CompoundTag ref(int x) {
        CompoundTag t = new CompoundTag();
        t.putInt("schema", 1);
        t.putIntArray("controller", new int[]{x, -59, 168});
        return t;
    }

    @Test
    void neoForgeWorldOnFabric() {
        CompoundTag entity = new CompoundTag();
        CompoundTag neo = new CompoundTag();
        neo.put("bettacraft:tank_ref", ref(11));
        neo.put("othermod:data", new CompoundTag());
        entity.put(CrossLoaderAttachments.NEOFORGE, neo);
        assertEquals(1, CrossLoaderAttachments.merge(entity));
        CompoundTag fabric = entity.getCompound(CrossLoaderAttachments.FABRIC);
        assertEquals(ref(11), fabric.getCompound("bettacraft:tank_ref"));
        assertFalse(fabric.contains("othermod:data"), "only our ids");
    }

    @Test
    void fabricWorldOnNeoForge() {
        CompoundTag entity = new CompoundTag();
        CompoundTag fabric = new CompoundTag();
        fabric.put("bettacraft:glass_mob", new CompoundTag());
        entity.put(CrossLoaderAttachments.FABRIC, fabric);
        assertEquals(1, CrossLoaderAttachments.merge(entity));
        assertTrue(entity.getCompound(CrossLoaderAttachments.NEOFORGE).contains("bettacraft:glass_mob"));
    }

    @Test
    void ownContainerWins() {
        CompoundTag entity = new CompoundTag();
        CompoundTag neo = new CompoundTag();
        neo.put("bettacraft:tank_ref", ref(3));
        CompoundTag fabric = new CompoundTag();
        fabric.put("bettacraft:tank_ref", ref(19));
        entity.put(CrossLoaderAttachments.NEOFORGE, neo);
        entity.put(CrossLoaderAttachments.FABRIC, fabric);
        assertEquals(0, CrossLoaderAttachments.merge(entity));
        assertEquals(ref(3), entity.getCompound(CrossLoaderAttachments.NEOFORGE).getCompound("bettacraft:tank_ref"));
        assertEquals(ref(19), entity.getCompound(CrossLoaderAttachments.FABRIC).getCompound("bettacraft:tank_ref"));
    }

    @Test
    void nothingToCopy() {
        CompoundTag entity = new CompoundTag();
        assertEquals(0, CrossLoaderAttachments.merge(entity));
        assertTrue(entity.isEmpty());
    }
}
