package com.avaloka.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Assigns human-readable room identifiers to a set of events.
 *
 * <p>The algorithm processes events in chronological order. An active
 * allocation min-heap tracks which room is occupied until which end time, and a
 * recycling queue holds the names of rooms that have become free. Reusing rooms
 * whenever possible keeps the total number of distinct room names equal to the
 * peak concurrency, which is optimal.</p>
 *
 * <p>Rooms are named {@code "Room A"} through {@code "Room Z"}, then
 * {@code "Room AA"}, {@code "Room AB"}, and so on, in the same style as
 * spreadsheet column labels. The returned list is ordered by start time.</p>
 */
public final class RoomAssigner {

    private static final String ROOM_PREFIX = "Room ";

    private RoomAssigner() {
        // Utility class.
    }

    /**
     * Assigns a room to every event such that no room hosts overlapping
     * events. End times are exclusive, so an event ending at time {@code t}
     * releases its room for another event starting at {@code t}.
     *
     * <p>Time complexity: O(n log n) due to sorting and heap operations.
     * Space complexity: O(n) for the result, active heap, and recycling
     * queue.</p>
     *
     * @param events array of {@code {start, end}} intervals
     * @return scheduled events in chronological order, each with a room name
     */
    public static List<ScheduledEvent> assignRooms(int[][] events) {
        List<ScheduledEvent> scheduled = new ArrayList<>();
        if (events == null || events.length == 0) {
            return scheduled;
        }

        EventSlot[] ordered = new EventSlot[events.length];
        for (int i = 0; i < events.length; i++) {
            ordered[i] = new EventSlot(events[i][0], events[i][1], i);
        }
        Arrays.sort(ordered, Comparator
                .comparingInt(EventSlot::start)
                .thenComparingInt(EventSlot::end)
                .thenComparingInt(EventSlot::originalIndex));

        PriorityQueue<ActiveAllocation> active = new PriorityQueue<>(
                Comparator.comparingInt(ActiveAllocation::endTime)
                        .thenComparing(ActiveAllocation::roomId));
        PriorityQueue<String> availableRooms = new PriorityQueue<>();

        int nextRoomNumber = 0;
        for (EventSlot event : ordered) {
            releaseFinishedRooms(active, availableRooms, event.start());

            String roomId = availableRooms.poll();
            if (roomId == null) {
                roomId = roomName(nextRoomNumber++);
            }

            active.offer(new ActiveAllocation(event.end(), roomId));
            scheduled.add(new ScheduledEvent(event.start(), event.end(), roomId));
        }
        return scheduled;
    }

    /**
     * Moves every room whose allocation has ended at or before {@code start}
     * from the active heap into the recycling queue.
     *
     * @param active         rooms currently in use, ordered by end time
     * @param availableRooms free room names, ordered alphabetically
     * @param start          start time of the event about to be assigned
     */
    private static void releaseFinishedRooms(PriorityQueue<ActiveAllocation> active,
                                             PriorityQueue<String> availableRooms,
                                             int start) {
        while (!active.isEmpty() && active.peek().endTime() <= start) {
            availableRooms.offer(active.poll().roomId());
        }
    }

    /**
     * Converts a zero-based room index into a human-readable room name:
     * A..Z, AA..AZ, BA.., and so on.
     *
     * @param index zero-based room index
     * @return the room name, for example {@code "Room C"} or {@code "Room AA"}
     */
    static String roomName(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Room index must not be negative");
        }
        StringBuilder label = new StringBuilder();
        int value = index;
        do {
            label.append((char) ('A' + (value % 26)));
            value = (value / 26) - 1;
        } while (value >= 0);
        return ROOM_PREFIX + label.reverse();
    }

    /**
     * Internal sortable view of an input event that remembers its original
     * position for deterministic tie-breaking.
     */
    private record EventSlot(int start, int end, int originalIndex) {
    }

    /**
     * A room currently in use until {@code endTime}.
     */
    private record ActiveAllocation(int endTime, String roomId) {
    }
}
