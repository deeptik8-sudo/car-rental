package com.example.carrental.exception;

import com.example.carrental.model.CarType;

import java.time.LocalDateTime;

/** Thrown when every car of the requested type is already booked for part of the window. */
public class NoCarAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CarType carType;

    public NoCarAvailableException(CarType carType, LocalDateTime start, LocalDateTime end) {
        super("No " + carType + " available from " + start + " to " + end);
        this.carType = carType;
    }

    public CarType getCarType() {
        return carType;
    }
}
