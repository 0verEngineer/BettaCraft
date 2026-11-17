package org.overengineer.bettacraft.mobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.compat.MobCaptures;
import org.overengineer.bettacraft.terrarium.TerrariumMode;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds.Habitat;

/**
 * M31 part d: the table of the mobs in glass (question C2), their habitats and sizes, and the NBT formats of the capture
 * items of other mods (read from the EnderIO 8.2 and Industrial Foregoing 3.6 jars: {@code enderio:soul} =
 * {@code {entity_type, entity_tag}}, older {@code {entityTag}}; {@code industrialforegoing:mob_imprisonment} = the entity
 * with its type in {@code entity}).
 */
class GlassMobTest {
    private static CompoundTag snbt(String s) {
        try {
            return TagParser.parseCompoundFully(s);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    void theListOfQuestionC2() {
        Set<String> ids = GlassMobSpecies.DEFAULTS.stream().map(s -> s.id().getPath()).collect(Collectors.toSet());
        assertEquals(Set.of("drowned", "zombie", "husk", "skeleton", "stray", "spider", "cave_spider", "silverfish", "endermite", "slime", "magma_cube"), ids);
        for (String no : List.of("creeper", "enderman", "witch", "wither_skeleton", "wither", "ender_dragon", "warden")) assertFalse(ids.contains(no), no);
    }

    @Test
    void habitatsAndSizes() {
        assertTrue(Habitat.WET.allows(TerrariumMode.AQUARIUM) && Habitat.WET.allows(TerrariumMode.PALUDARIUM) && !Habitat.WET.allows(TerrariumMode.TERRARIUM));
        for (TerrariumMode m : TerrariumMode.values()) assertTrue(Habitat.ALL.allows(m));
        for (GlassMobSpecies s : GlassMobSpecies.DEFAULTS) {
            assertTrue(s.scale() > 0.0625F && s.scale() < 0.5F, s.id() + " shrunk");
            if (s.breeding() == GlassMobSpecies.Breeding.NONE) assertNull(s.food(), s.id() + " has no breeding food");
            else assertNotNull(s.food(), s.id() + " breeds with food");
        }
        GlassMobSpecies drowned = GlassMobSpecies.DEFAULTS.getFirst();
        assertEquals(Habitat.WET, drowned.needs().habitat());
        GlassMobSpecies spider = GlassMobSpecies.DEFAULTS.stream().filter(s -> s.id().getPath().equals("spider")).findFirst().orElseThrow();
        assertTrue(spider.climbs());
        assertEquals(spider.scale() * 0.55F, spider.scaleAt(0), 1e-5);
        assertEquals(spider.scale(), spider.scaleAt(1), 1e-6);
        assertEquals(drowned.scale(), drowned.scaleAt(0), 1e-6, "a vanilla baby is small by itself");
        GlassMobSpecies other = GlassMobSpecies.generic(net.minecraft.resources.Identifier.parse("othermod:ghoul"), 0.6F, 1.95F);
        assertEquals(0.35, other.scale() * 1.95, 1e-3, "an added mob is about a third of a block tall");
    }

    @Test
    void enderIoSoulVial() {
        CompoundTag e = MobCaptures.parse(MobCaptures.Format.SOUL_VIAL, snbt("{entity_type:\"minecraft:zombie\",entity_tag:{id:\"minecraft:zombie\",Health:4.0f}}"));
        assertNotNull(e);
        assertEquals("minecraft:zombie", e.getStringOr("id", ""));
        assertEquals(4.0F, e.getFloatOr("Health", 0f));
        CompoundTag old = MobCaptures.parse(MobCaptures.Format.SOUL_VIAL, snbt("{entityTag:{id:\"minecraft:spider\",Health:2.0f}}"));
        assertEquals("minecraft:spider", old.getStringOr("id", ""));
        CompoundTag noId = MobCaptures.parse(MobCaptures.Format.SOUL_VIAL, snbt("{entity_type:\"minecraft:husk\",entity_tag:{Health:5.0f}}"));
        assertEquals("minecraft:husk", noId.getStringOr("id", ""));
        assertNull(MobCaptures.parse(MobCaptures.Format.SOUL_VIAL, snbt("{}")), "an empty vial");
    }

    @Test
    void industrialForegoingImprisonmentTool() {
        CompoundTag e = MobCaptures.parse(MobCaptures.Format.IMPRISONMENT, snbt("{entity:\"minecraft:drowned\",Health:3.0f,Pos:[1.0d,2.0d,3.0d]}"));
        assertNotNull(e);
        assertEquals("minecraft:drowned", e.getStringOr("id", ""));
        assertFalse(e.contains("entity"));
        assertEquals(3.0F, e.getFloatOr("Health", 0f));
    }

    @Test
    void genericCaptureItems() {
        CompoundTag e = MobCaptures.parse(MobCaptures.Format.GENERIC, snbt("{StoredEntity:{id:\"minecraft:slime\",Health:1.0f,Size:0}}"));
        assertEquals("minecraft:slime", e.getStringOr("id", ""));
        assertNull(MobCaptures.parse(MobCaptures.Format.GENERIC, snbt("{item:{id:\"minecraft:stone\",count:1}}")), "an item is no mob");
    }
}
