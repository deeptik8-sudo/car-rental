package com.example.carrental.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A booking of one specific car for a time window. Immutable.
 *
 * <p>The window is half-open: {@code [start, end)}. A car returned at 10:00 can be
 * picked up by the next customer at 10:00.
 */
public final class Reservation {

    private final String id;
    private final Car car;
    private final LocalDateTime start;
    private final int days;

    public Reservation(String id, Car car, LocalDateTime start, int days) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.car = Objects.requireNonNull(car, "car must not be null");
        this.start = Objects.requireNonNull(start, "start must not be null");
        if (days < 1) {
            throw new IllegalArgumentException("days must be at least 1");
        }
        this.days = days;
    }

    public String getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public CarType getCarType() {
        return car.type();
    }

    public LocalDateTime getStart() {
        return start;
    }

    public int getDays() {
        return days;
    }

    /** End of the rental: start plus the number of days (exclusive). */
    public LocalDateTime getEnd() {
        return start.plusDays(days);
    }

    /**
     * Returns true if this reservation's window overlaps {@code [otherStart, otherEnd)}.
     * Two half-open intervals overlap exactly when each starts before the other ends.
     */
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(getEnd());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", car=" + car.id() + ", start=" + start
                + ", end=" + getEnd() + ", days=" + days + "}";
    }
}
