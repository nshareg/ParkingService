package com.inMemoryPersistence;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
    Created by anshanyan
    on 26.05.26
*/
final class Table<V> {

    private final String name;
    private final Set<V> data = new LinkedHashSet<>();
    private List<Index<Object, V>> indexList = new ArrayList<>();

    public Table(String tableName) {
        name = tableName;
    }

    public List<V> get(Integer indexIndex, Predicate<V> filter) {
        Collection<V> source = (indexIndex == null || indexIndex < 0)
                ? data : indexList.get(indexIndex).values();
        return source.stream().filter(filter).toList(); }

    public Set<V> getWithoutIndex(Predicate<V> filter) {
        return data.stream().filter(filter).collect(Collectors.toSet());
    }
    public void add(V value) {
        data.add(value);
        for  (Index<Object, V> index : indexList) {
            index.put(index.extractKey(value), value);
        }

    }
    public int remove(Predicate<V> filter) {
        int before = data.size();
        data.removeIf(filter);
        for (Index<Object, V> index : indexList) {
            index.values().removeIf(filter);
        }
        return before - data.size();
    }
    public int update(Predicate<V> filter, Consumer<V> mutator) {
        int affectedRows = 0;
        for (V value : data) {
            if (!filter.test(value)) continue;
            for (Index<Object, V> index : indexList) {
                index.remove(index.extractKey(value));
            }
            mutator.accept(value);
            for (Index<Object, V> index : indexList) {
                index.put(index.extractKey(value), value);
            }
            affectedRows++;
        }
        return affectedRows;
    }
    public List<Index<Object, V>> getIndexList() {
        return indexList;
    }

    public String getName() {
        return name;
    }
}
