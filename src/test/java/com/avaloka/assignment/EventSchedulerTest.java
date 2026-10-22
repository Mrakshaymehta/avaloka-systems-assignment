package com.avaloka.assignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSchedulerTest {

    @Nested
    @DisplayName("canAttendAll")
    class CanAttendAll {

        @Test
        @DisplayName("overlapping events are rejected")
        void overlappingEventsAreRejected() {
            int[][] events = {{9, 11}, {10, 12}};
            assertFalse(EventScheduler.canAttendAll(events));
        }

        @Test
        @DisplayName("one event contained in another is rejected")
        void containedEventIsRejected() {
            int[][] events = {{9, 18}, {10, 12}};
            assertFalse(EventScheduler.canAttendAll(events));
        }

        @Test
        @DisplayName("non-overlapping events are accepted")
        void nonOverlappingEventsAreAccepted() {
            int[][] events = {{1, 3}, {5, 7}, {9, 10}};
            assertTrue(EventScheduler.canAttendAll(events));
        }

        @Test
        @DisplayName("back-to-back adjacent events are accepted")
        void backToBackAdjacentEventsAreAccepted() {
            int[][] events = {{9, 10}, {10, 11}, {11, 12}};
            assertTrue(EventScheduler.canAttendAll(events));
        }

        @Test
        @DisplayName("unsorted but compatible events are accepted")
        void unsortedButCompatibleEventsAreAccepted() {
            int[][] events = {{9, 10}, {1, 3}, {5, 7}};
            assertTrue(EventScheduler.canAttendAll(events));
        }

        @Test
        @DisplayName("empty and single-event inputs are accepted")
        void emptyAndSingleEventInputsAreAccepted() {
            assertTrue(EventScheduler.canAttendAll(new int[0][0]));
            assertTrue(EventScheduler.canAttendAll(null));
            assertTrue(EventScheduler.canAttendAll(new int[][]{{5, 6}}));
        }
    }

    @Nested
    @DisplayName("minRoomsRequired")
    class MinRoomsRequired {

        @Test
        @DisplayName("empty input needs zero rooms")
        void emptyInputNeedsZeroRooms() {
            assertEquals(0, EventScheduler.minRoomsRequired(new int[0][0]));
            assertEquals(0, EventScheduler.minRoomsRequired(null));
        }

        @Test
        @DisplayName("a single event needs one room")
        void singleEventNeedsOneRoom() {
            assertEquals(1, EventScheduler.minRoomsRequired(new int[][]{{9, 10}}));
        }

        @Test
        @DisplayName("back-to-back events reuse one room")
        void backToBackEventsReuseOneRoom() {
            int[][] events = {{9, 10}, {10, 11}, {11, 12}};
            assertEquals(1, EventScheduler.minRoomsRequired(events));
        }

        @Test
        @DisplayName("overlapping schedules need the peak concurrency of rooms")
        void overlappingSchedulesNeedPeakConcurrency() {
            int[][] events = {{0, 30}, {5, 10}, {15, 20}};
            assertEquals(2, EventScheduler.minRoomsRequired(events));
        }

        @Test
        @DisplayName("three simultaneous events need three rooms")
        void threeSimultaneousEventsNeedThreeRooms() {
            int[][] events = {{1, 5}, {2, 6}, {3, 7}};
            assertEquals(3, EventScheduler.minRoomsRequired(events));
        }

        @Test
        @DisplayName("unsorted input is handled correctly")
        void unsortedInputIsHandledCorrectly() {
            int[][] events = {{4, 9}, {1, 3}, {2, 4}, {6, 8}};
            assertEquals(2, EventScheduler.minRoomsRequired(events));
        }

        @Test
        @DisplayName("the input array is not mutated")
        void inputArrayIsNotMutated() {
            int[][] events = {{9, 11}, {10, 12}};
            EventScheduler.minRoomsRequired(events);
            assertArrayEquals(new int[]{9, 11}, events[0]);
            assertArrayEquals(new int[]{10, 12}, events[1]);
        }
    }

    @Nested
    @DisplayName("RoomAssigner")
    class RoomAssignment {

        @Test
        @DisplayName("sequential events all reuse Room A")
        void sequentialEventsReuseOneRoom() {
            int[][] events = {{9, 10}, {10, 11}, {11, 12}};

            List<ScheduledEvent> result = RoomAssigner.assignRooms(events);

            assertEquals(List.of(
                    new ScheduledEvent(9, 10, "Room A"),
                    new ScheduledEvent(10, 11, "Room A"),
                    new ScheduledEvent(11, 12, "Room A")
            ), result);
        }

        @Test
        @DisplayName("overlapping events receive distinct rooms and free names are recycled")
        void overlappingEventsReceiveDistinctRoomsAndRecycleNames() {
            int[][] events = {{9, 10}, {9, 11}, {10, 12}, {11, 13}};

            List<ScheduledEvent> result = RoomAssigner.assignRooms(events);

            assertEquals(List.of(
                    new ScheduledEvent(9, 10, "Room A"),
                    new ScheduledEvent(9, 11, "Room B"),
                    new ScheduledEvent(10, 12, "Room A"),
                    new ScheduledEvent(11, 13, "Room B")
            ), result);
        }

        @Test
        @DisplayName("three simultaneous events receive Rooms A, B, and C")
        void threeSimultaneousEventsReceiveThreeRooms() {
            int[][] events = {{1, 5}, {2, 6}, {3, 7}};

            List<ScheduledEvent> result = RoomAssigner.assignRooms(events);

            assertEquals(List.of(
                    new ScheduledEvent(1, 5, "Room A"),
                    new ScheduledEvent(2, 6, "Room B"),
                    new ScheduledEvent(3, 7, "Room C")
            ), result);
        }

        @Test
        @DisplayName("results are returned in chronological order regardless of input order")
        void resultsAreChronological() {
            int[][] events = {{11, 13}, {9, 10}, {10, 12}};

            List<ScheduledEvent> result = RoomAssigner.assignRooms(events);

            assertEquals(List.of(
                    new ScheduledEvent(9, 10, "Room A"),
                    new ScheduledEvent(10, 12, "Room A"),
                    new ScheduledEvent(11, 13, "Room B")
            ), result);
        }

        @Test
        @DisplayName("empty input returns an empty list")
        void emptyInputReturnsEmptyList() {
            assertTrue(RoomAssigner.assignRooms(new int[0][0]).isEmpty());
            assertTrue(RoomAssigner.assignRooms(null).isEmpty());
        }

        @Test
        @DisplayName("room names continue past Z using spreadsheet-style labels")
        void roomNamesContinuePastZ() {
            assertEquals("Room A", RoomAssigner.roomName(0));
            assertEquals("Room Z", RoomAssigner.roomName(25));
            assertEquals("Room AA", RoomAssigner.roomName(26));
            assertEquals("Room AB", RoomAssigner.roomName(27));
        }

        @Test
        @DisplayName("the input array is not mutated")
        void inputArrayIsNotMutated() {
            int[][] events = {{10, 12}, {9, 10}};
            RoomAssigner.assignRooms(events);

            assertArrayEquals(new int[]{10, 12}, events[0]);
            assertArrayEquals(new int[]{9, 10}, events[1]);
        }
    }
}
