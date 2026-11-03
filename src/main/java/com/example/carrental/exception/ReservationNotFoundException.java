package com.example.carrental.exception;

/** Thrown when a reservation id does not exist. */
public class ReservationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(String id) {
        super("Reservation not found: " + id);
    }
}
