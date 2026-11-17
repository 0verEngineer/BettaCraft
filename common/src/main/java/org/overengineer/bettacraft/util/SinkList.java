package org.overengineer.bettacraft.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * M52b: a list whose {@link #add} also passes the element on to a consumer. Since 1.21.5 tooltips are given to
 * {@code Item#appendHoverText} as a {@code Consumer<Component>}; our tooltip code still fills a list.
 */
public final class SinkList<T> extends AbstractList<T> {
    private final Consumer<T> sink;
    private final List<T> added = new ArrayList<>();

    public SinkList(Consumer<T> sink) {
        this.sink = sink;
    }

    @Override
    public boolean add(T t) {
        added.add(t);
        sink.accept(t);
        return true;
    }

    @Override
    public void add(int index, T t) {
        // the consumer cannot insert: appended in order
        add(t);
    }

    @Override
    public T get(int index) {
        return added.get(index);
    }

    @Override
    public int size() {
        return added.size();
    }
}
