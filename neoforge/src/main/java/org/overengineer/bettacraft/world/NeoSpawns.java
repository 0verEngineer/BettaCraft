package org.overengineer.bettacraft.world;

import java.util.List;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.MobSpawnSettingsModifier;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * M52e: adds our spawns to a biome in a NeoForge biome modifier. 26.3 keeps the spawns in the environment attribute
 * {@code gameplay/natural_mob_spawns}; NeoForge changes it only when the biome writes it with the {@code overlay} modifier
 * (as vanilla does). A datapack that writes the spawns as a plain value (an override; Terralith 2.6.5 does it for the
 * vanilla biomes it replaces, swamp and jungle among them) makes NeoForge ignore every spawn a biome modifier adds; then we
 * add ours to that value ourselves.
 */
final class NeoSpawns {
    private NeoSpawns() {}

    static void add(ModifiableBiomeInfo.BiomeInfo.Builder builder, List<WildSpawns.Spawn> spawns) {
        if (spawns.isEmpty()) return;
        var entry = builder.getAttributes().get(EnvironmentAttributes.NATURAL_MOB_SPAWNS);
        if (entry == null || entry.modifier() == MobSpawnSettingsModifier.overlay()) {
            for (var s : spawns) builder.getMobSpawnSettings().addSpawn(s.data().type(), s.category(), s.weight(), s.data().count());
            return;
        }
        // the biome has no layer below its own (the dimension has no spawns), so the override applied to nothing is its value
        MobSpawnSettingsBuilder settings = new MobSpawnSettingsBuilder(entry.applyModifier(MobSpawnSettings.EMPTY));
        for (var s : spawns) settings.addSpawn(s.data().type(), s.category(), s.weight(), s.data().count());
        builder.getAttributes().set(EnvironmentAttributes.NATURAL_MOB_SPAWNS, settings.build());
    }
}
