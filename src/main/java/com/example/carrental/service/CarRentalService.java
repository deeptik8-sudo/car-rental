package com.example.carrental.service;

import com.example.carrental.exception.InvalidReservationException;
import com.example.carrental.exception.NoCarAvailableException;
import com.example.carrental.exception.ReservationNotFoundException;
import com.example.carrental.fleet.Fleet;
import com.example.carrental.model.Car;
import com.example.carrental.model.CarType;
import com.example.carrental.model.Reservation;
import com.example.carrental.repository.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entry point of the system: reserves cars of a given type at a given date-time
 * for a given number of days, and cancels reservations.
 *
 * <p>Each reservation is assigned to a specific car. A car can be reserved only if
 * none of its existing reservations overlaps the requested window, which is how the
 * limited number of cars per type is enforced.
 *
 * <p>Public methods that read or change bookings are {@code synchronized} so two
 * concurrent requests can never book the same car for overlapping windows.
 */
public class CarRentalService {

    private final Fleet fleet;
    private final ReservationRepository repository;
    private final Clock clock;
    private final AtomicLong idSequence = new AtomicLong();

    public CarRentalService(Fleet fleet, ReservationRepository repository, Clock clock) {
        this.fleet = Objects.requireNonNull(fleet, "fleet must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Reserves a car of the given type.
     *
     * @param type  the car type wanted
     * @param start pick-up date and time
     * @param days  number of days, at least 1
     * @return the confirmed reservation, including the assigned car
     * @throws InvalidReservationException if the input is invalid
     * @throws NoCarAvailableException     if every car of that type is busy during the window
     */
    public synchronized Reservation reserve(CarType type, LocalDateTime start, int days) {
        validate(type, start, days);
        LocalDateTime end = start.plusDays(days);

        Car car = findFreeCar(type, start, end)
                .orElseThrow(() -> new NoCarAvailableException(type, start, end));

        Reservation reservation = new Reservation(nextId(), car, start, days);
        repository.save(reservation);
        return reservation;
    }

    /**
     * Cancels a reservation so its car becomes available again.
     *
     * @throws ReservationNotFoundException if the id is unknown
     */
    public synchronized void cancel(String reservationId) {
        if (!repository.delete(reservationId)) {
            throw new ReservationNotFoundException(reservationId);
        }
    }

    /** True if at least one car of the type is free for the whole window. */
    public synchronized boolean isAvailable(CarType type, LocalDateTime start, int days) {
        validate(type, start, days);
        return findFreeCar(type, start, start.plusDays(days)).isPresent();
    }

    /** Number of cars of the type that are free for the whole window. */
    public synchronized int availableCount(CarType type, LocalDateTime start, int days) {
        validate(type, start, days);
        LocalDateTime end = start.plusDays(days);
        int count = 0;
        for (Car car : fleet.carsOfType(type)) {
            if (isCarFree(car, start, end)) {
                count++;
            }
        }
        return count;
    }

    public synchronized Optional<Reservation> findReservation(String reservationId) {
        return repository.findById(reservationId);
    }

    public synchronized List<Reservation> allReservations() {
        return repository.findAll();
    }
    
    /**
     * Lists and prints all overlapping reservations and reserved cars of a specific type
     * during the given time window.
     */
    public synchronized void printReservedCars(CarType type, LocalDateTime start, int days) {
        validate(type, start, days);
        LocalDateTime end = start.plusDays(days);
        
        System.out.println("Reserved cars and active reservations for " + type + " from " + start + " to " + end + ":");
        
        List<Reservation> overlappingReservations = getReservationsInRange(type, start, days);
        
        if (overlappingReservations == null || overlappingReservations.isEmpty()) {
            System.out.println("No active reservations for " + type + " in this period.");
            return;
        }

        for (Reservation r : overlappingReservations) {
            System.out.println(" - Reservation ID: " + r.getId() 
                    + ", Car ID: " + r.getCar().id()
                    + ", Model: " + r.getCar().type() 
                    + ", Start Time: " + r.getStart() 
                    + ", End Time: " + r.getEnd());
        }
    }
    
    /** Retrieves all reservations of a specific type that overlap with the given time window. */
    public synchronized List<Reservation> getReservationsInRange(CarType type, LocalDateTime start, int days) {
        validate(type, start, days);
        LocalDateTime end = start.plusDays(days);
        
        List<Reservation> overlapping = new ArrayList<>();
        for (Car car : fleet.carsOfType(type)) {
            for (Reservation reservation : repository.findByCar(car)) {
                if (reservation.overlaps(start, end)) {
                    if (!overlapping.contains(reservation)) {
                        overlapping.add(reservation);
                    }
                }
            }
        }
        return overlapping;
    }

    private void validate(CarType type, LocalDateTime start, int days) {
        if (type == null) {
            throw new InvalidReservationException("Car type is required");
        }
        if (start == null) {
            throw new InvalidReservationException("Start date-time is required");
        }
        if (days < 1) {
            throw new InvalidReservationException("Number of days must be at least 1, was " + days);
        }
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new InvalidReservationException("Start date-time must not be in the past: " + start);
        }
    }

    /** First-fit: return the first car of the type with no overlapping reservation. */
    private Optional<Car> findFreeCar(CarType type, LocalDateTime start, LocalDateTime end) {
        for (Car car : fleet.carsOfType(type)) {
            if (isCarFree(car, start, end)) {
                return Optional.of(car);
            }
        }
        return Optional.empty();
    }

    private boolean isCarFree(Car car, LocalDateTime start, LocalDateTime end) {
        for (Reservation existing : repository.findByCar(car)) {
            if (existing.overlaps(start, end)) {
                return false;
            }
        }
        return true;
    }

    private String nextId() {
        return "R-" + idSequence.incrementAndGet();
    }
}
