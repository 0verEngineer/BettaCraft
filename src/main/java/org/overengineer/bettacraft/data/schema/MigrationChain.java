package org.overengineer.bettacraft.data.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A linear chain of {@link Migration}s for one kind of persisted object.
 * Step {@code i} upgrades version {@code i} to {@code i + 1}; the current version equals the number of steps.
 * <p>
 * Data with a version newer than {@link #currentVersion()} (written by a newer mod version) is returned untouched,
 * so a downgrade never destroys it.
 */
public final class MigrationChain {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/schema");

    private final String name;
    private final List<Migration> steps;

    private MigrationChain(String name, List<Migration> steps) {
        this.name = name;
        this.steps = Collections.unmodifiableList(steps);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public int currentVersion() {
        return steps.size();
    }

    /** Returns a migrated copy (input is never modified) stamped with the current version. */
    public CompoundTag migrate(CompoundTag input) {
        int version = Schema.read(input);
        CompoundTag data = input.copy();
        if (version > currentVersion()) {
            LOGGER.warn("{} data has schema {} which is newer than supported {}; keeping it unchanged", name, version, currentVersion());
            return data;
        }
        if (version < 0) {
            LOGGER.warn("{} data has invalid schema {}; treating it as 0", name, version);
            version = 0;
        }
        while (version < currentVersion()) {
            data = Objects.requireNonNull(steps.get(version).apply(data), "migration returned null");
            version++;
        }
        return Schema.write(data, version);
    }

    /** True if {@link #migrate} would change the version (useful to mark data dirty after loading). */
    public boolean needsMigration(CompoundTag input) {
        int v = Schema.read(input);
        return v < currentVersion();
    }

    /** Writes the current version into freshly serialised data. */
    public CompoundTag stamp(CompoundTag output) {
        return Schema.write(output, currentVersion());
    }

    public static final class Builder {
        private final String name;
        private final List<Migration> steps = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        /** Adds the migration from {@code fromVersion} to {@code fromVersion + 1}; steps must be added in order. */
        public Builder step(int fromVersion, Migration migration) {
            if (fromVersion != steps.size()) {
                throw new IllegalArgumentException(name + ": expected migration from version " + steps.size() + " but got " + fromVersion);
            }
            steps.add(Objects.requireNonNull(migration));
            return this;
        }

        public MigrationChain build() {
            return new MigrationChain(name, new ArrayList<>(steps));
        }
    }
}
