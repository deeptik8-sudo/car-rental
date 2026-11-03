package com.example.carrental.exception;

/** Thrown when a reservation request has invalid input (e.g. zero days, a past start time). */
public class InvalidReservationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidReservationException(String message) {
        super(message);
    }
}
