package com.avaloka.assignment;

import java.util.HashMap;
import java.util.Map;

/**
 * A fixed-capacity Least Recently Used (LRU) cache with O(1) average-case
 * {@link #get(int)} and {@link #put(int, int)} operations.
 *
 * <p>The implementation combines two structures:</p>
 * <ul>
 *   <li>A {@link HashMap} from key to {@link Node} for O(1) lookup.</li>
 *   <li>A custom doubly linked list, ordered from most recently used (front)
 *       to least recently used (back), for O(1) insertion, removal, and
 *       promotion.</li>
 * </ul>
 *
 * <p>Dummy {@code head} and {@code tail} sentinel nodes eliminate null checks
 * at the boundaries of the list. The node immediately before {@code tail} is
 * always the least recently used entry and is the eviction candidate.</p>
 *
 * <p><strong>This class is not thread-safe.</strong> See the project README
 * for the concurrency strategies that can be layered on top of it.</p>
 */
public class LRUCache {

    /**
     * Internal doubly linked list node. It stores both the key and the value
     * so that an evicted tail node can remove its own entry from the map.
     */
    private static final class Node {
        private final int key;
        private int value;
        private Node prev;
        private Node next;

        private Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<Integer, Node> cache;
    private final Node head;
    private final Node tail;

    /**
     * Creates an LRU cache that holds at most {@code capacity} entries.
     *
     * @param capacity the maximum number of entries, must be positive
     * @throws IllegalArgumentException if {@code capacity} is not positive
     */
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.cache = new HashMap<>(Math.max(16, (int) (capacity / 0.75f) + 1));
        this.head = new Node(-1, -1);
        this.tail = new Node(-1, -1);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    /**
     * Returns the value associated with {@code key}, or {@code -1} if the key
     * is absent. A successful lookup promotes the entry to most recently used.
     *
     * <p>Time complexity: O(1) average case. Space complexity: O(1).</p>
     *
     * @param key the key to look up
     * @return the cached value, or {@code -1} when absent
     */
    public int get(int key) {
        Node node = cache.get(key);
        if (node == null) {
            return -1;
        }
        moveToFront(node);
        return node.value;
    }

    /**
     * Inserts or updates an entry. If the key already exists, its value is
     * replaced and the entry becomes most recently used. If inserting a new
     * key would exceed the capacity, the least recently used entry is evicted.
     *
     * <p>Time complexity: O(1) average case. Space complexity: O(1).</p>
     *
     * @param key   the key to insert or update
     * @param value the value to associate with the key
     */
    public void put(int key, int value) {
        Node node = cache.get(key);
        if (node != null) {
            node.value = value;
            moveToFront(node);
            return;
        }

        Node newNode = new Node(key, value);
        cache.put(key, newNode);
        addNodeToFront(newNode);

        if (cache.size() > capacity) {
            Node leastRecentlyUsed = popTail();
            if (leastRecentlyUsed != null) {
                cache.remove(leastRecentlyUsed.key);
            }
        }
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Inserts {@code node} immediately after the dummy head, making it the
     * most recently used entry.
     *
     * @param node the node to insert; it must not already be linked
     */
    private void addNodeToFront(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * Unlinks {@code node} from its current position in the doubly linked
     * list.
     *
     * @param node the node to unlink
     */
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    /**
     * Promotes an existing node to the most recently used position.
     *
     * @param node the node to promote
     */
    private void moveToFront(Node node) {
        removeNode(node);
        addNodeToFront(node);
    }

    /**
     * Removes and returns the least recently used node, located immediately
     * before the dummy tail.
     *
     * @return the evicted node, or {@code null} if the cache is empty
     */
    private Node popTail() {
        Node leastRecentlyUsed = tail.prev;
        if (leastRecentlyUsed == head) {
            return null;
        }
        removeNode(leastRecentlyUsed);
        return leastRecentlyUsed;
    }
}
