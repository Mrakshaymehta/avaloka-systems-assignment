package com.avaloka.assignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LRUCacheTest {

    @Test
    @DisplayName("get and put store and retrieve values")
    void getAndPutStoreAndRetrieveValues() {
        LRUCache cache = new LRUCache(2);

        cache.put(1, 10);
        cache.put(2, 20);

        assertEquals(10, cache.get(1));
        assertEquals(20, cache.get(2));
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("put evicts the least recently used entry at capacity")
    void putEvictsLeastRecentlyUsedEntry() {
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);

        cache.get(1);
        cache.put(3, 3);

        assertEquals(1, cache.get(1));
        assertEquals(-1, cache.get(2));
        assertEquals(3, cache.get(3));
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("put evicts in insertion order when nothing is accessed")
    void putEvictsInInsertionOrderWithoutAccesses() {
        LRUCache cache = new LRUCache(3);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        cache.put(4, 4);

        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
    }

    @Test
    @DisplayName("updating a key replaces its value and promotes it to MRU")
    void updatingKeyReplacesValueAndPromotesToMostRecentlyUsed() {
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);

        cache.put(1, 100);
        cache.put(3, 3);

        assertEquals(100, cache.get(1));
        assertEquals(-1, cache.get(2));
        assertEquals(3, cache.get(3));
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("get promotes the accessed entry to most recently used")
    void getPromotesAccessedEntryToMostRecentlyUsed() {
        LRUCache cache = new LRUCache(3);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        cache.get(1);
        cache.put(4, 4);

        assertEquals(-1, cache.get(2));
        assertEquals(1, cache.get(1));
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
    }

    @Test
    @DisplayName("missing keys return -1 and do not change the cache")
    void missingKeysReturnMinusOne() {
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);

        assertEquals(-1, cache.get(99));
        assertEquals(1, cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("a capacity-one cache evicts before every new insert")
    void capacityOneEvictsOnEveryNewInsert() {
        LRUCache cache = new LRUCache(1);
        cache.put(1, 1);

        assertEquals(1, cache.get(1));

        cache.put(2, 2);

        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("repeated updates to the same key never grow the cache")
    void repeatedUpdatesDoNotGrowCache() {
        LRUCache cache = new LRUCache(2);

        cache.put(1, 1);
        cache.put(1, 2);
        cache.put(1, 3);

        assertEquals(1, cache.size());
        assertEquals(3, cache.get(1));
    }

    @Test
    @DisplayName("non-positive capacity is rejected")
    void nonPositiveCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUCache(-5));
    }
}
