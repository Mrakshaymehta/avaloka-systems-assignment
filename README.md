# Data Structures & Systems Design Assignment - Avaloka (Guruvai Sciences)

**Akshay Mehta** | **Role: AI / Multi-Agent Systems Intern**

A Java 17 implementation of three systems-design problems:

1. **LRU Cache** with O(1) `get` and `put`.
2. **Event Scheduler** for conflict detection and minimum room calculation.
3. **Room Assigner** that produces deterministic, human-readable room names.

All functionality is covered by a JUnit 5 test suite and built with Maven.

---

## Table of Contents

- [Project Structure](#project-structure)
- [Build and Execution](#build-and-execution)
- [1. LRU Cache](#1-lru-cache)
  - [Architecture](#lru-architecture)
  - [Operations](#lru-operations)
  - [Walkthrough](#lru-walkthrough)
- [2. Event Scheduler](#2-event-scheduler)
  - [Conflict Detection](#conflict-detection)
  - [Minimum Rooms](#minimum-rooms)
  - [Room Assignment](#room-assignment)
- [ASCII Architecture Diagrams](#ascii-architecture-diagrams)
- [Final Discussion & Analysis](#final-discussion--analysis)
  - [1. Time and Space Complexity](#1-time-and-space-complexity)
  - [2. Trade-offs: HashMap + Doubly Linked List](#2-trade-offs-hashmap--doubly-linked-list)
  - [3. Future Proofing: Named Room Assignment](#3-future-proofing-named-room-assignment)
  - [4. Concurrency](#4-concurrency)
- [Test Coverage](#test-coverage)

---

## Project Structure

```
.
├── pom.xml
├── README.md
└── src
    ├── main/java/com/avaloka/assignment
    │   ├── LRUCache.java
    │   ├── EventScheduler.java
    │   ├── RoomAssigner.java
    │   └── ScheduledEvent.java
    └── test/java/com/avaloka/assignment
        ├── LRUCacheTest.java
        └── EventSchedulerTest.java
```

---

## Build and Execution

**Prerequisites:** JDK 17 or newer and Maven 3.8 or newer.

```bash
# Compile and run the complete JUnit 5 suite
mvn clean test
```

A successful run ends with:

```
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 1. LRU Cache

### LRU Architecture

An LRU cache must answer two different questions efficiently:

- *Where is the value for this key?* -> a hash-based lookup.
- *Which entry was used least recently?* -> an ordered structure.

Neither structure can answer both questions in O(1) alone. The implementation
therefore pairs a `HashMap<Integer, Node>` with a custom **doubly linked list**:

- The map gives O(1) access from a key to its node.
- The list maintains recency: **front = most recently used (MRU)**,
  **back = least recently used (LRU)**.
- Dummy `head` and `tail` sentinel nodes remove boundary null checks.
- Each `Node` stores its key as well as its value, so evicting the tail node is
  enough to also remove the corresponding map entry.

```java
private final Map<Integer, Node> cache;
private final Node head;   // sentinel: MRU side
private final Node tail;   // sentinel: LRU side
```

### LRU Operations

| Operation | Steps | Cost |
|---|---|---|
| `get(key)` | Map lookup; if present, unlink node and reinsert at front | O(1) average |
| `put(key, value)` - existing key | Update value, unlink, reinsert at front | O(1) average |
| `put(key, value)` - new key | Insert at front, add to map; if over capacity, evict tail | O(1) average |
| `popTail()` | Unlink `tail.prev` and return it | O(1) |

Helper methods keep each responsibility small:

- `addNodeToFront(node)` wires a node immediately after `head`.
- `removeNode(node)` unlinks a node from its neighbours.
- `moveToFront(node)` = `removeNode` + `addNodeToFront`.
- `popTail()` removes the node immediately before the `tail` sentinel.

### LRU Walkthrough

Capacity = 2:

```
put(1, 10)  ->  [1]                       MRU=1  LRU=1
put(2, 20)  ->  [2, 1]                    MRU=2  LRU=1
get(1)      ->  [1, 2]                    MRU=1  LRU=2
put(3, 30)  ->  evict 2, then [3, 1]      MRU=3  LRU=1
get(2)      ->  -1 (evicted)
```

The eviction decision is always the node before `tail`; no scan is required.

---

## 2. Event Scheduler

Events are represented as `int[][]`, where each row is `{start, end}`.
Intervals are **half-open**: an event ending at time `t` does not conflict with
an event starting at `t`. This convention allows back-to-back meetings.

### Conflict Detection

`canAttendAll(int[][] events)` sorts a defensive copy of the events by start
time, then compares each event with its predecessor:

```java
if (currentStart < previousEnd) {
    return false;
}
```

The comparison is **strict**. Therefore `[9, 10]` and `[10, 11]` are accepted,
while `[9, 11]` and `[10, 12]` conflict. Sorting is O(n log n); the linear scan
is O(n).

### Minimum Rooms

`minRoomsRequired(int[][] events)` uses a sweep-line approach backed by a
**min-heap of end times**:

1. Sort events by start time.
2. Push the first event's end time onto the heap.
3. For each subsequent event, compare its start with the smallest end time at
   the top of the heap.
4. If `heap.peek() <= start`, that room has been released; poll it and reuse it.
5. Push the new event's end time and continue.
6. The heap size at the end is the peak number of simultaneous rooms.

Back-to-back events reuse a room because the release check uses `<=`.

```java
PriorityQueue<Integer> activeEndTimes = new PriorityQueue<>();
for (int[] event : sorted) {
    if (!activeEndTimes.isEmpty() && activeEndTimes.peek() <= event[0]) {
        activeEndTimes.poll();
    }
    activeEndTimes.offer(event[1]);
}
return activeEndTimes.size();
```

### Room Assignment

`RoomAssigner.assignRooms(int[][])` turns the count into actual names:

- Events are sorted chronologically.
- `PriorityQueue<ActiveAllocation>` tracks which room is occupied until when.
- `PriorityQueue<String> availableRooms` recycles freed room names in
  alphabetical order.
- When a room is freed (`endTime <= nextStart`), its name moves to the recycling
  queue.
- If no name is available, a new one is generated: `Room A`, `Room B`, ...,
  `Room Z`, `Room AA`, `Room AB`, and so on.

The returned `List<ScheduledEvent>` is chronological and contains immutable
`ScheduledEvent` objects with `start`, `end`, and `roomId`. The input array is
never mutated.

Example:

```
events = [{9,10}, {9,11}, {10,12}, {11,13}]

{9,10}   -> Room A
{9,11}   -> Room B      (A still occupied)
{10,12}  -> Room A      (A.freeTime = 10 <= start 10)
{11,13}  -> Room B      (B.freeTime = 11 <= start 11)
```

Room reuse is optimal: the number of distinct names equals the peak
concurrency returned by `minRoomsRequired`.

---

## ASCII Architecture Diagrams

### 1. LRU Cache: HashMap + Doubly Linked List

```
                         HashMap<Integer, Node>
              ┌──────────────┬──────────────────────┐
              │     key      │     node reference   │
              ├──────────────┼──────────────────────┤
              │      1       │ ──────────────┐      │
              │      3       │ ────────────┐ │      │
              │      2       │ ──────────┐ │ │      │
              └──────────────┴───────────┼─┼─┼──────┘
                                         │ │ │
                                         ▼ ▼ ▼
                                 Doubly Linked List
        ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
        │  head    │<-->│ Node(1)  │<-->│ Node(3)  │<-->│ Node(2)  │<-->│  tail    │
        │ sentinel │    │ val=10   │    │ val=30   │    │ val=20   │    │ sentinel │
        └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                          MRU                                        LRU /
                                                                    eviction
                                                                   candidate

  get(1): map lookup gives Node(1), then remove + reinsert after head.
  put(4): insert after head; if over capacity, remove tail.prev and its map key.
```

```
Operation flow for get(key):

  key ──► HashMap.get ──► node ──► unlink node ──► insert after head ──► value
            O(1)                     O(1)               O(1)

Operation flow for put(key, value):

  key ──► HashMap.get ──► exists? ──yes──► update value ──► move to front
                             │
                             no
                             ▼
                    create node ──► add to front ──► add to map
                                                        │
                                               size > capacity?
                                                        │ yes
                                                        ▼
                                          pop tail + remove map key
```

### 2. Min-Heap Workflow for `minRoomsRequired`

```
                  Input: [{start, end}, ...]
                              │
                              ▼
                 Sort by start time (O(n log n))
                              │
                              ▼
              ┌───────────────────────────────────┐
              │   Min-Heap of active end times    │
              │   peek() = earliest room release  │
              └───────────────────────────────────┘
                              │
              for each event {start, end}
                              │
                              ▼
              ┌───────────────────────────────────┐
              │  heap.peek() <= start ?           │
              └───────────────────────────────────┘
                    │ yes                 │ no
                    ▼                     │
          poll earliest end               │
          (reuse that room)               │
                    │                     │
                    └──────────┬──────────┘
                               ▼
                     push(current end)
                               │
                               ▼
                 heap.size() = rooms in use
                 for the current event
                               │
                               ▼
                 final heap size = peak concurrency

  Example: [{0,30}, {5,10}, {15,20}]

  {0,30}   heap = [30]              size 1
  {5,10}   10 > 5, push 10          heap = [10, 30]     size 2
  {15,20}  10 <= 15, pop 10,
           push 20                  heap = [20, 30]     size 2  <- answer
```

### 3. Room Assignment: Active Heap + Recycling Queue

```
             sorted events                       active allocations
                  │                              (min-heap by end)
                  ▼                                      │
        ┌────────────────────┐                           │
        │  next {start,end}  │                           │
        └─────────┬──────────┘                           │
                  ▼                                      │
        ┌────────────────────────────────────┐           │
        │ while active.peek().end <= start:  │───────────┘
        │     free room -> availableRooms    │
        └─────────┬──────────────────────────┘
                  ▼
        ┌────────────────────┐       no      ┌──────────────────┐
        │ availableRooms has │──────────────►│ generate Room N  │
        │ a recycled name?   │               └────────┬─────────┘
        └─────────┬──────────┘                        │
               yes│                                    │
                  ▼                                    ▼
        ┌──────────────────────────────────────────────────┐
        │ roomId = recycled name or newly generated name   │
        │ active.offer({end, roomId})                      │
        │ scheduled.add({start, end, roomId})              │
        └──────────────────────────────────────────────────┘
```

---

## Final Discussion & Analysis

### 1. Time and Space Complexity

**Notation:** `n` = number of events, `C` = cache capacity, `k` = number of
distinct room names.

| Function | Time Complexity | Space Complexity | Notes |
|---|---|---|---|
| `LRUCache(int capacity)` | O(C) allocation | O(C) | Initial map capacity is sized from `C` |
| `LRUCache.get(int key)` | O(1) average | O(1) | Map lookup + constant pointer updates |
| `LRUCache.put(int key, int value)` | O(1) average | O(1) | Update or insert + optional O(1) eviction |
| `LRUCache.size()` | O(1) | O(1) | Delegates to `HashMap.size()` |
| `EventScheduler.canAttendAll` | O(n log n) | O(n) | Defensive copy + sort; scan is O(n) |
| `EventScheduler.minRoomsRequired` | O(n log n) | O(n) | Sort + one heap push/pop per event |
| `RoomAssigner.assignRooms` | O(n log n) | O(n) | Sort, active heap, recycling queue, result |
| `RoomAssigner.roomName` | O(log26 k) | O(log26 k) | Label length grows logarithmically |
| `ScheduledEvent` accessors | O(1) | O(1) | Immutable value object |

For `minRoomsRequired`, sorting dominates. Heap operations are O(log n) each,
performed at most once per event, so total work is O(n log n). In the common
case where events are already sorted, the sort still costs O(n log n) unless a
linear-time precondition is explicitly supported.

### 2. Trade-offs: HashMap + Doubly Linked List

The LRU cache needs four operations: lookup by key, insert, unlink, and move to
the front. The chosen combination makes all four O(1):

| Structure | Lookup | Recency update | Eviction | Verdict |
|---|---|---|---|---|
| HashMap only | O(1) | Not tracked | Cannot identify LRU | Insufficient |
| Array / ArrayList | O(n) search | O(n) shift | O(n) shift | Too slow |
| Singly linked list | O(n) search | O(1) with prior node | O(n) to find tail predecessor | Lookup fails |
| Doubly linked list only | O(n) search | O(1) | O(1) with tail pointer | Lookup fails |
| Balanced tree (e.g. TreeMap) | O(log n) | O(log n) | O(log n) | Correct but slower |
| **HashMap + doubly linked list** | **O(1)** | **O(1)** | **O(1)** | **Optimal** |

Why this is optimal:

- The hash map supplies **direct access** to the node for a key, avoiding a
  linear search through the list.
- The doubly linked list supplies **ordering and O(1) splicing** in both
  directions. A singly linked list would require a predecessor scan to unlink a
  node when promoting it or evicting the tail.
- Sentinel `head` and `tail` nodes make the list circular in effect and remove
  special cases for empty lists and boundary inserts, reducing bug surface.
- Storing the key inside each node allows eviction to remove the correct map
  entry without an extra reverse lookup.

The cost is memory overhead: one map entry plus two references and a key per
cached value. This is the standard space-for-time trade-off. The alternative,
`LinkedHashMap` with `accessOrder = true`, implements the same idea internally
and is a reasonable production choice, but a custom implementation gives
explicit control over eviction, synchronization, and instrumentation.

### 3. Future Proofing: Named Room Assignment

The room assignment layer is deliberately separated from the scheduling math.
`EventScheduler` computes *how many* rooms are needed; `RoomAssigner` decides
*which named room* each event uses. This separation makes the system extensible.

**Current naming guarantees:**

- Names are deterministic for the same sorted input: `Room A` is always the
  first room used.
- Freed names are recycled through a min-heap, so a sequential schedule reuses
  `Room A` instead of monotonically creating new names.
- The namespace extends beyond 26 rooms using spreadsheet-style labels:
  `Room Z` -> `Room AA` -> `Room AB` -> ... -> `Room ZZ` -> `Room AAA`.
- The number of distinct names equals the peak concurrency. This is optimal:
  any schedule with peak concurrency `p` provably needs at least `p` rooms.

**Extending to real-world room metadata:**

A production version would replace the generated `String` with a `Room` value
object or registry containing:

- stable external IDs (database or building-management-system identifiers),
- capacity, floor, building, accessibility, and equipment (projector, video
  conferencing),
- availability calendars and maintenance windows,
- time zones for distributed scheduling.

`RoomAssigner` could accept a `List<Room>` inventory and a strategy interface:

```java
interface RoomSelectionStrategy {
    Optional<Room> select(Event event, List<Room> availableRooms);
}
```

Strategies could implement first-fit, best-fit by capacity, least-recently-used
room, or affinity rules such as "keep a team in the same room all day".
If rooms have capacities and events have attendance requirements, the problem
becomes a bin-packing variant and is NP-hard in general, so practical systems
use greedy heuristics plus conflict repair rather than exact optimization.

**Operational future proofing:** persisted assignments should store the external
room ID, not only the display label, so renaming or reordering rooms does not
break historical records. The label remains a presentation concern. The
allocation layer can also add a waitlist or overflow policy when no room is
available, and can emit metrics such as rooms used, utilization, and
recycled-name rate.

### 4. Concurrency

`LRUCache` as implemented is **not thread-safe**. `get` mutates the linked list
by promoting a node, so two concurrent readers can corrupt pointers, and a
`put` can race with both. The same issue applies to the event utilities if they
are later refactored to hold mutable state.

**Strategy A: Coarse-grained `ReentrantLock`**

Wrap every public operation in the same lock:

```java
private final ReentrantLock lock = new ReentrantLock();

public int get(int key) {
    lock.lock();
    try {
        // map lookup + moveToFront
    } finally {
        lock.unlock();
    }
}
```

Advantages: simple, correct, and supports `tryLock(timeout)`, fairness, and
condition variables. Disadvantages: all operations serialize; throughput is
bounded by one core. `tryLock` with a timeout can prevent long stalls in
latency-sensitive services, and an unfair lock usually maximizes throughput
while a fair lock improves tail latency predictability.

**Strategy B: `ReadWriteLock` is not useful here**

A read-write lock helps only when readers do not mutate shared state. In an LRU
cache, **a `get` is logically a read but physically a write** because it updates
the recency list. Taking a write lock for `get` gives no benefit over a plain
mutex. Taking a read lock would allow concurrent `get` calls to corrupt the
list. Workarounds exist:

- batch recency updates and apply them under a write lock periodically
  (approximate LRU),
- use a clock/CLOCK-Pro approximation where reads only set a reference bit,
- use `LongAdder`-style statistics outside the critical section.

Without such a redesign, `ReentrantReadWriteLock` adds overhead without
concurrency. This is a common interview trap: "reads dominate" does not imply a
read-write lock is appropriate when reads have side effects.

**Strategy C: Cache sharding**

Split the cache into `N` independent shards, each with its own map, list, and
lock, and route by `hash(key) % N`:

```
key ──► hash ──► shard = hash % 4
                     │
        ┌────────────┼────────────┬────────────┐
        ▼            ▼            ▼            ▼
     Shard 0      Shard 1      Shard 2      Shard 3
    lock + LRU   lock + LRU   lock + LRU   lock + LRU
    capacity C/N capacity C/N capacity C/N capacity C/N
```

Benefits: lock contention drops by roughly a factor of `N`, and independent
shards can be accessed in parallel. Costs and caveats:

- **Global LRU approximation:** eviction is per shard, so an entry that is hot
  globally may be evicted from a cold shard, lowering the hit ratio.
- **Capacity skew:** hash distribution may fill one shard earlier than another;
  total capacity is still bounded, but effective utilization varies. Virtual
  shards or periodic rebalancing can mitigate this.
- **Cross-key operations** such as global size, snapshots, or multi-key
  transactions require coordinating all shards.
- **Hot keys** still serialize on one shard; a per-key lock or request coalescing
  may be needed.

**Strategy D: Lock-free or fine-grained approaches**

A `ConcurrentHashMap` plus an atomic doubly linked list is possible but
notoriously difficult: promotion touches multiple pointers that must change
atomically, and ABA problems require careful use of `AtomicReference` or
versioned stamps. Production caches often use:

- a single writer thread consuming update events (serialization without lock
  contention on the hot path),
- striped locks over node buckets,
- probabilistic admission and frequency sketches (for example W-TinyLFU in
  Caffeine) to decouple recency maintenance from the read path.

**Correctness requirements for any strategy:**

- Map and list must be updated atomically with respect to each other; taking
  separate locks can deadlock or expose a node that has been unlinked.
- Eviction must not remove a node that was just promoted by another thread.
- Published node fields need safe publication; all access should occur under
  the protecting lock or through volatile/atomic fields.
- `size()` should be derived under the same synchronization domain if it is
  used for eviction decisions.

For this assignment, the single-threaded implementation keeps the core
algorithm clear. In production, a sharded `ReentrantLock` design or a proven
library such as Caffeine would be preferred over hand-rolling a concurrent
linked list.

---

## Test Coverage

| Test class | Focus |
|---|---|
| `LRUCacheTest` | Basic get/put, eviction order, promotion on get and update, missing keys, capacity one, repeated updates, invalid capacity |
| `EventSchedulerTest` | Overlap and adjacency detection, unsorted input, empty input, minimum rooms, room reuse, room naming past Z, chronological output, input immutability |

The suite contains **29 tests** and runs with:

```bash
mvn clean test
```

---

**Author:** Akshay Mehta
**Assignment:** Data Structures & Systems Design - Avaloka (Guruvai Sciences)
