package com.inMemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    private Index<String, Integer> index;

    @BeforeEach
    void setUp() {
        index = Index.createIndex();
    }

    @Test
    void insertAndLookup() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            Optional<Integer> result = index.lookup("a");
            assertTrue(result.isPresent());
            assertEquals(1, result.get());
        });
    }

    @Test
    void updateExistingKey() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            index.update("a", 42);
            assertEquals(42, index.lookup("a").get());
        });
    }

    @Test
    void updateNonExistentKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> index.update("missing", 1));
    }

    @Test
    void deleteExistingKey() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            Optional<Integer> removed = index.delete("a");
            assertTrue(removed.isPresent());
            assertEquals(1, removed.get());
            assertTrue(index.lookup("a").isEmpty());
        });
    }

    @Test
    void selectAllReturnsAllValues() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            index.insert("b", 2);
            index.insert("c", 3);
            List<Integer> all = index.selectAll();
            assertEquals(3, all.size());
        });
    }

    @Test
    void selectAllOnEmptyIndex() {
        assertDoesNotThrow(() -> assertTrue(index.selectAll().isEmpty()));
    }

    @Test
    void scanValueSetFiltersCorrectly() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            index.insert("b", 2);
            index.insert("c", 3);
            index.insert("d", 4);
            List<Integer> evens = index.scanValueSet(v -> v % 2 == 0);
            assertEquals(2, evens.size());
            assertTrue(evens.containsAll(List.of(2, 4)));
        });
    }

    @Test
    void scanValueSetNoMatchesReturnsEmptyList() {
        assertDoesNotThrow(() -> {
            index.insert("a", 1);
            List<Integer> result = index.scanValueSet(v -> v > 100);
            assertTrue(result.isEmpty());
        });
    }

}
