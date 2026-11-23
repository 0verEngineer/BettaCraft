package org.overengineer.bettacraft.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.compat.info.InfoLines;

/**
 * M64: the tank overlay (Jade, The One Probe, WTHIT): the title is the tank's name (or its kind), the short view has only the
 * terrarium air and the water quality (and the sneak hint), a terrarium shows no water line at all.
 */
class TankOverlayLinesTest {
    private static CompoundTag tank(String kind, String name, long water) {
        CompoundTag d = new CompoundTag();
        d.putString("name", name);
        d.putString("kind", kind);
        d.putFloat("temp", 25F);
        d.putLong("water", water);
        d.putLong("cap", 8000);
        d.putInt("count", 2);
        d.putInt("adults", 2);
        d.putString("nest", "none");
        d.putFloat("quality", 0.9F);
        d.putString("quality_state", "good");
        d.putDouble("evaporation", 12.0);
        if (!kind.equals("aquarium")) {
            d.putString("terra_mode", "gui.bettacraft.terrarium.mode." + kind);
            d.putFloat("air", 27F);
            d.putFloat("rh", 70F);
            d.putFloat("uv", 1.5F);
            d.putFloat("bask", 32F);
            d.putFloat("land", 0.5F);
        }
        return d;
    }

    private static List<String> keys(List<Component> lines) {
        return lines.stream().map(c -> c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString()).toList();
    }

    @Test
    void titleIsTheNameOrTheKind() {
        assertEquals("Living room", InfoLines.tankTitle(tank("aquarium", "Living room", 8000)).getString());
        Component t = InfoLines.tankTitle(tank("terrarium", "", 0));
        assertEquals("gui.bettacraft.terrarium.mode.terrarium", ((TranslatableContents) t.getContents()).getKey());
    }

    @Test
    void shortViewHasOnlyAirAndWaterQuality() {
        assertEquals(List.of("jade.bettacraft.tank.quality", "jade.bettacraft.sneak"), keys(InfoLines.tank(tank("aquarium", "A", 8000), false)));
        assertEquals(List.of("jade.bettacraft.tank.air", "jade.bettacraft.tank.quality", "jade.bettacraft.sneak"),
                keys(InfoLines.tank(tank("paludarium", "P", 4000), false)));
        // a terrarium: the air, no water quality (its water is a dish)
        assertEquals(List.of("jade.bettacraft.tank.air", "jade.bettacraft.sneak"), keys(InfoLines.tank(tank("terrarium", "T", 500), false)));
        // warnings: their number in the hint
        CompoundTag w = tank("aquarium", "A", 8000);
        ListTag warn = new ListTag();
        warn.add(StringTag.valueOf("crowded"));
        w.put("warnings", warn);
        assertEquals("jade.bettacraft.sneak_warnings", keys(InfoLines.tank(w, false)).getLast());
    }

    @Test
    void terrariumHasNoWaterLine() {
        List<String> t = keys(InfoLines.tank(tank("terrarium", "T", 500), true));
        assertFalse(t.contains("jade.bettacraft.tank.water"), "no water line for a terrarium: " + t);
        assertFalse(t.contains("jade.bettacraft.tank.quality"), t.toString());
        assertTrue(t.contains("gui.bettacraft.terrarium.mode.terrarium"), "the kind in the details: " + t);
        List<String> a = keys(InfoLines.tank(tank("aquarium", "A", 8000), true));
        assertTrue(a.contains("jade.bettacraft.tank.water") && a.contains("jade.bettacraft.tank.evaporation") && a.contains("jade.bettacraft.tank.animals"), a.toString());
        assertFalse(a.contains("jade.bettacraft.sneak"), a.toString());
    }
}
