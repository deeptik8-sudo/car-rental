package com.example.carrental.repository;

import com.example.carrental.model.Car;
import com.example.carrental.model.Reservation;

import java.util.List;
import java.util.Optional;

/**
 * Storage for reservations. The service depends on this interface rather than a
 * concrete store, so an in-memory version can be swapped for a database later.
 */
public interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> findById(String id);

    List<Reservation> findByCar(Car car);

    List<Reservation> findAll();

    boolean delete(String id);
}
