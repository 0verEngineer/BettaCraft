package org.overengineer.bettacraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;

/** M52b: the compound a save method writes to a value output (1.21.6 saves through ValueOutput, the tests compare compounds). */
public final class DevNbt {
    private DevNbt() {}

    public static CompoundTag save(HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writer.accept(out);
        return out.buildResult();
    }
}
