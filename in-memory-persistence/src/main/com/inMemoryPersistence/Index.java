package main.com.inMemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/*
    Created by anshanyan
    on 24.05.26
 */

final class Index<K,V> extends HashMap<K,V> {

    private final Function<V, K> keyExtractor;

    private Index(Function<V, K> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }
    /**
     * Static factory method
     *
     * @return Index of provided key value type
     * @param <K> key type
     * @param <V> value type
     */
    public static <K, V> Index<K, V> createIndex() {
        return new Index<>(null);
    }

    public static <K, V> Index<K, V> createIndex(Function<V, K> keyExtractor) {
        return new Index<>(keyExtractor);
    }

    public K extractKey(V value) {
        return keyExtractor.apply(value);
    }
    /**
     * Wrapper over the put method, ensuring no duplicate key will be entered
     * such decision is done, as parking slot cannot have duplicate key, and trying
     * to put duplicate key, suggest about error.
     *
     * @param key key for the value we are putting
     * @param value value itself
     */
    public void insert(K key, V value){
        if(containsKey(key)){
            throw new IllegalArgumentException("duplicate key");
        }
        put(key, value);
    }

    /**
     * Wrapper over the put method, ensures that slot we are trying to access exists,
     * in converse exception is thrown
     *
     * @param key key for the value we are updating
     * @param value value itself
     */
    public void update(K key, V value){
        if(!containsKey(key)){
            throw new IllegalArgumentException("no entry with given key");
        }
        put(key, value);
    }

    /**
     * Optional wrapper over the value returned by standard remove
     *
     * @param key key for the pair we are removing
     * @return optional.empty if no such key exists, optional of value if exists
     */
    public Optional<V> delete(K key) {
        return Optional.ofNullable(remove(key));
    }

    /**
     * Optional wrapper over the hashmap search
     *
     * @param key for the value we are searching
     * @return Optional of the value
     */
    public Optional<V> lookup(K key) {
        return Optional.ofNullable(get(key));
    }

    /**
     *
     * @return shallow copies for all the entries
     */
    public List<V> selectAll() {
        return new ArrayList<>(values());
    }

    /**
     * Search with predicate
     *
     * @param predicate Predicate to search with
     * @return List of entries that qualify
     */
    public List<V> scanValueSet(Predicate<V> predicate) {
        List<V> result = new ArrayList<>();
        for (V v : values()) {
            if (predicate.test(v)) result.add(v);
        }
        return result;
    }
}
