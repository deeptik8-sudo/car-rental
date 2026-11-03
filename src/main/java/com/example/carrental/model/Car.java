package com.example.carrental.model;

import java.util.Objects;

/**
 * A single physical car in the fleet. Immutable.
 *
 * @param id   unique identifier, e.g. "SEDAN-1"
 * @param type the car's type
 */
public record Car(String id, CarType type) {

    public Car {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
