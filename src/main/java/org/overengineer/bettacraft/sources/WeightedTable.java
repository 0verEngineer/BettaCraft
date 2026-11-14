package org.overengineer.bettacraft.sources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * M22.2: a table of things with integer weights, used by the game to pick one ({@link #pick}) and by the handbook
 * generator to print the chances ({@link #share}). Picking draws {@code nextInt(total)} once and walks the entries in
 * order, so a table with weights summing to 100 behaves exactly like the {@code roll < 35 / < 60 / ...} chains it
 * replaced.
 */
public final class WeightedTable<T> {
    public record Entry<T>(T value, int weight) {}

    private final List<Entry<T>> entries;
    private final int total;

    private WeightedTable(List<Entry<T>> entries) {
        this.entries = Collections.unmodifiableList(entries);
        int t = 0;
        for (Entry<T> e : entries) t += e.weight();
        this.total = t;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** M22.4: a table without entries ({@link #pick} must not be called on it). */
    public static <T> WeightedTable<T> empty() {
        return new WeightedTable<>(new ArrayList<>());
    }

    public boolean isEmpty() {
        return total <= 0;
    }

    /** M22.4: whether {@code value} is in the table with a weight above 0. */
    public boolean contains(T value) {
        for (Entry<T> e : entries) if (e.value().equals(value) && e.weight() > 0) return true;
        return false;
    }

    public List<Entry<T>> entries() {
        return entries;
    }

    public int total() {
        return total;
    }

    public T pick(RandomGenerator rnd) {
        int roll = rnd.nextInt(total);
        for (Entry<T> e : entries) {
            if (roll < e.weight()) return e.value();
            roll -= e.weight();
        }
        return entries.getLast().value();
    }

    /** Share of {@code value} (0..1). */
    public double share(T value) {
        int w = 0;
        for (Entry<T> e : entries) if (e.value().equals(value)) w += e.weight();
        return total == 0 ? 0 : (double) w / total;
    }

    public static final class Builder<T> {
        private final List<Entry<T>> entries = new ArrayList<>();

        public Builder<T> add(T value, int weight) {
            entries.add(new Entry<>(value, weight));
            return this;
        }

        /** M22.4: every entry of another table. */
        public Builder<T> addAll(WeightedTable<T> other) {
            entries.addAll(other.entries());
            return this;
        }

        public WeightedTable<T> build() {
            return new WeightedTable<>(new ArrayList<>(entries));
        }
    }
}
