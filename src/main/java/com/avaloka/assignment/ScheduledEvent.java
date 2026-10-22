package com.avaloka.assignment;

import java.util.Objects;

/**
 * Immutable result of room assignment. Represents one event together with the
 * human-readable identifier of the room it was allocated to.
 */
public final class ScheduledEvent {

    private final int start;
    private final int end;
    private final String roomId;

    /**
     * Creates a scheduled event.
     *
     * @param start  inclusive start time
     * @param end    exclusive end time
     * @param roomId human-readable room identifier, for example {@code "Room A"}
     */
    public ScheduledEvent(int start, int end, String roomId) {
        this.start = start;
        this.end = end;
        this.roomId = Objects.requireNonNull(roomId, "roomId");
    }

    /**
     * @return the inclusive start time
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the exclusive end time
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return the allocated room identifier
     */
    public String getRoomId() {
        return roomId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScheduledEvent)) {
            return false;
        }
        ScheduledEvent that = (ScheduledEvent) other;
        return start == that.start && end == that.end && roomId.equals(that.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, roomId);
    }

    @Override
    public String toString() {
        return "ScheduledEvent{"
                + "start=" + start
                + ", end=" + end
                + ", roomId='" + roomId + '\''
                + '}';
    }
}
