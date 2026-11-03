package com.example.carrental.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationTest {

    private static final Car SEDAN = new Car("SEDAN-1", CarType.SEDAN);
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 10, 10, 0);

    // Reservation from Jan 10 10:00 to Jan 13 10:00
    private final Reservation reservation = new Reservation("R-1", SEDAN, START, 3);

    @Test
    @DisplayName("End time is start plus the number of days")
    void endIsStartPlusDays() {
        assertEquals(LocalDateTime.of(2026, 1, 13, 10, 0), reservation.getEnd());
    }

    @Test
    @DisplayName("Rejects zero or negative days")
    void rejectsNonPositiveDays() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation("R-2", SEDAN, START, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reservation("R-2", SEDAN, START, -1));
    }

    @Test
    @DisplayName("Identical window overlaps")
    void identicalWindowOverlaps() {
        assertTrue(reservation.overlaps(START, START.plusDays(3)));
    }

    @Test
    @DisplayName("Window inside the reservation overlaps")
    void containedWindowOverlaps() {
        assertTrue(reservation.overlaps(START.plusDays(1), START.plusDays(2)));
    }

    @Test
    @DisplayName("Window covering the reservation overlaps")
    void enclosingWindowOverlaps() {
        assertTrue(reservation.overlaps(START.minusDays(1), START.plusDays(5)));
    }

    @Test
    @DisplayName("Window crossing the start overlaps")
    void windowCrossingStartOverlaps() {
        assertTrue(reservation.overlaps(START.minusDays(1), START.plusHours(1)));
    }

    @Test
    @DisplayName("Window crossing the end overlaps")
    void windowCrossingEndOverlaps() {
        assertTrue(reservation.overlaps(START.plusDays(3).minusHours(1), START.plusDays(5)));
    }

    @Test
    @DisplayName("Window ending exactly at the start does not overlap")
    void windowEndingAtStartDoesNotOverlap() {
        assertFalse(reservation.overlaps(START.minusDays(2), START));
    }

    @Test
    @DisplayName("Window starting exactly at the end does not overlap")
    void windowStartingAtEndDoesNotOverlap() {
        assertFalse(reservation.overlaps(START.plusDays(3), START.plusDays(4)));
    }

    @Test
    @DisplayName("Completely separate window does not overlap")
    void separateWindowDoesNotOverlap() {
        assertFalse(reservation.overlaps(START.plusDays(10), START.plusDays(12)));
    }
}
