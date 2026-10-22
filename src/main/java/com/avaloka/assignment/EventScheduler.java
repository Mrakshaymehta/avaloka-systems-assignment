package com.avaloka.assignment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Interval-based utilities for meeting and event scheduling.
 *
 * <p>Every event is represented as a two-element integer array
 * {@code {start, end}}. Intervals are half-open: an event ending at time
 * {@code t} and another starting at time {@code t} are compatible and may use
 * the same room. This is the standard convention that permits back-to-back
 * meetings.</p>
 */
public final class EventScheduler {

    private EventScheduler() {
        // Utility class.
    }

    /**
     * Determines whether a single person can attend every event without any
     * time conflict.
     *
     * <p>The events are defensively copied and sorted by start time. A conflict
     * exists only when the next event starts strictly before the previous event
     * ends ({@code currStart < prevEnd}). Because the comparison is strict,
     * adjacent events such as {@code [9, 10]} and {@code [10, 11]} are
     * accepted.</p>
     *
     * <p>Time complexity: O(n log n) due to sorting.
     * Space complexity: O(n) for the defensive copy.</p>
     *
     * @param events array of {@code {start, end}} intervals
     * @return {@code true} if no two events overlap, {@code false} otherwise
     */
    public static boolean canAttendAll(int[][] events) {
        if (events == null || events.length < 2) {
            return true;
        }

        int[][] sorted = Arrays.stream(events)
                .map(event -> new int[]{event[0], event[1]})
                .toArray(int[][]::new);
        Arrays.sort(sorted, Comparator.comparingInt(event -> event[0]));

        for (int i = 1; i < sorted.length; i++) {
            int previousEnd = sorted[i - 1][1];
            int currentStart = sorted[i][0];
            if (currentStart < previousEnd) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the minimum number of rooms required to host all events.
     *
     * <p>Events are sorted by start time. A min-heap tracks the end time of
     * every meeting currently occupying a room. Before assigning the next
     * meeting, every room whose meeting ends at or before the new start time is
     * released; in practice it is sufficient to check the earliest end time at
     * the top of the heap. The heap size after processing all events is the
     * peak number of simultaneously active rooms.</p>
     *
     * <p>Time complexity: O(n log n) due to sorting and heap operations.
     * Space complexity: O(n) for the defensive copy and heap.</p>
     *
     * @param events array of {@code {start, end}} intervals
     * @return the minimum number of rooms needed, or {@code 0} for no events
     */
    public static int minRoomsRequired(int[][] events) {
        if (events == null || events.length == 0) {
            return 0;
        }

        int[][] sorted = Arrays.stream(events)
                .map(event -> new int[]{event[0], event[1]})
                .toArray(int[][]::new);
        Arrays.sort(sorted, Comparator.comparingInt(event -> event[0]));

        PriorityQueue<Integer> activeEndTimes = new PriorityQueue<>();
        for (int[] event : sorted) {
            int start = event[0];
            int end = event[1];

            if (!activeEndTimes.isEmpty() && activeEndTimes.peek() <= start) {
                activeEndTimes.poll();
            }
            activeEndTimes.offer(end);
        }
        return activeEndTimes.size();
    }
}
