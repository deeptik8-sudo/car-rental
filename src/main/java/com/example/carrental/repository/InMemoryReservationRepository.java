package com.example.carrental.repository;

import com.example.carrental.model.Car;
import com.example.carrental.model.Reservation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple in-memory store. Not thread-safe on its own; the service serialises access.
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> byId = new LinkedHashMap<>();

    @Override
    public void save(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation must not be null");
        byId.put(reservation.getId(), reservation);
    }

    @Override
    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Reservation> findByCar(Car car) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : byId.values()) {
            if (r.getCar().equals(car)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<Reservation> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public boolean delete(String id) {
        return byId.remove(id) != null;
    }
}
