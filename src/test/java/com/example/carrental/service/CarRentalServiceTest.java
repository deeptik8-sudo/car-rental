package com.example.carrental.service;

import com.example.carrental.exception.InvalidReservationException;
import com.example.carrental.exception.NoCarAvailableException;
import com.example.carrental.exception.ReservationNotFoundException;
import com.example.carrental.fleet.Fleet;
import com.example.carrental.model.CarType;
import com.example.carrental.model.Reservation;
import com.example.carrental.repository.InMemoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarRentalServiceTest {

    /** "Now" is fixed so tests about past dates are repeatable. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    /** A pick-up time safely in the future. */
    private static final LocalDateTime JAN_10 = LocalDateTime.of(2026, 1, 10, 10, 0);

    private CarRentalService service;

    @BeforeEach
    void setUp() {
        // 2 sedans, 1 SUV, 1 van
        Fleet fleet = Fleet.withCapacity(Map.of(CarType.SEDAN, 2, CarType.SUV, 1, CarType.VAN, 1));
        service = new CarRentalService(fleet, new InMemoryReservationRepository(), FIXED_CLOCK);
    }

    // ------------------------------------------------------------------
    // Requirement: reserve a car of a given type at a date-time for N days
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Reserving a car")
    class Reserving {

        @Test
        @DisplayName("Returns a reservation with the requested type, start, days and end")
        void reservationHasRequestedDetails() {
            Reservation r = service.reserve(CarType.SEDAN, JAN_10, 3);

            assertEquals(CarType.SEDAN, r.getCarType());
            assertEquals(JAN_10, r.getStart());
            assertEquals(3, r.getDays());
            assertEquals(LocalDateTime.of(2026, 1, 13, 10, 0), r.getEnd());
        }

        @Test
        @DisplayName("Reservation can be looked up by id after booking")
        void reservationIsStored() {
            Reservation r = service.reserve(CarType.SUV, JAN_10, 1);

            assertEquals(r, service.findReservation(r.getId()).orElseThrow());
            assertEquals(1, service.allReservations().size());
        }

        @Test
        @DisplayName("Each reservation gets a unique id")
        void reservationIdsAreUnique() {
            Reservation a = service.reserve(CarType.SEDAN, JAN_10, 1);
            Reservation b = service.reserve(CarType.SEDAN, JAN_10, 1);

            assertNotEquals(a.getId(), b.getId());
        }

        @Test
        @DisplayName("A reservation starting right now is allowed")
        void startingNowIsAllowed() {
            assertDoesNotThrow(() -> service.reserve(CarType.VAN, NOW, 1));
        }
    }

    // ------------------------------------------------------------------
    // Requirement: there are three types of car
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Car types")
    class CarTypes {

        @ParameterizedTest(name = "Can reserve a {0}")
        @EnumSource(CarType.class)
        void canReserveEachType(CarType type) {
            Reservation r = service.reserve(type, JAN_10, 2);
            assertEquals(type, r.getCarType());
        }

        @Test
        @DisplayName("Exactly three types exist: sedan, SUV and van")
        void exactlyThreeTypes() {
            assertEquals(List.of(CarType.SEDAN, CarType.SUV, CarType.VAN), List.of(CarType.values()));
        }

        @Test
        @DisplayName("A full type does not block other types")
        void fullTypeDoesNotBlockOthers() {
            service.reserve(CarType.SUV, JAN_10, 5); // the only SUV

            assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SUV, JAN_10, 1));
            assertDoesNotThrow(() -> service.reserve(CarType.SEDAN, JAN_10, 1));
            assertDoesNotThrow(() -> service.reserve(CarType.VAN, JAN_10, 1));
        }
    }

    // ------------------------------------------------------------------
    // Requirement: the number of cars of each type is limited
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Limited capacity")
    class LimitedCapacity {

        @Test
        @DisplayName("Can book as many cars as the fleet has, then the next request fails")
        void cannotExceedCapacity() {
            Reservation first = service.reserve(CarType.SEDAN, JAN_10, 3);
            Reservation second = service.reserve(CarType.SEDAN, JAN_10, 3);

            assertNotEquals(first.getCar(), second.getCar(), "two different sedans must be used");
            NoCarAvailableException ex = assertThrows(NoCarAvailableException.class,
                    () -> service.reserve(CarType.SEDAN, JAN_10, 3));
            assertEquals(CarType.SEDAN, ex.getCarType());
        }

        @Test
        @DisplayName("A partially overlapping request is rejected when the fleet is full")
        void partialOverlapRejected() {
            service.reserve(CarType.SUV, JAN_10, 3); // Jan 10 - Jan 13

            // Jan 12 - Jan 14 overlaps the last day
            assertThrows(NoCarAvailableException.class,
                    () -> service.reserve(CarType.SUV, JAN_10.plusDays(2), 2));
            // Jan 8 - Jan 11 overlaps the first day
            assertThrows(NoCarAvailableException.class,
                    () -> service.reserve(CarType.SUV, JAN_10.minusDays(2), 3));
            // Jan 11 - Jan 12 sits inside the booking
            assertThrows(NoCarAvailableException.class,
                    () -> service.reserve(CarType.SUV, JAN_10.plusDays(1), 1));
        }

        @Test
        @DisplayName("A request that only overlaps by one hour is still rejected")
        void oneHourOverlapRejected() {
            service.reserve(CarType.VAN, JAN_10, 2); // ends Jan 12 10:00

            assertThrows(NoCarAvailableException.class,
                    () -> service.reserve(CarType.VAN, JAN_10.plusDays(2).minusHours(1), 1));
        }

        @Test
        @DisplayName("Back-to-back bookings of the same car are allowed")
        void backToBackAllowed() {
            Reservation first = service.reserve(CarType.SUV, JAN_10, 3);
            Reservation next = service.reserve(CarType.SUV, first.getEnd(), 2);
            Reservation before = service.reserve(CarType.SUV, JAN_10.minusDays(2), 2);

            assertEquals(first.getCar(), next.getCar());
            assertEquals(first.getCar(), before.getCar());
        }

        @Test
        @DisplayName("Non-overlapping dates can reuse the same car")
        void separateDatesReuseCar() {
            service.reserve(CarType.VAN, JAN_10, 2);
            assertDoesNotThrow(() -> service.reserve(CarType.VAN, JAN_10.plusDays(10), 2));
        }

        @Test
        @DisplayName("Availability count goes down as cars are booked")
        void availabilityCountReflectsBookings() {
            assertEquals(2, service.availableCount(CarType.SEDAN, JAN_10, 1));
            service.reserve(CarType.SEDAN, JAN_10, 1);
            assertEquals(1, service.availableCount(CarType.SEDAN, JAN_10, 1));
            service.reserve(CarType.SEDAN, JAN_10, 1);
            assertEquals(0, service.availableCount(CarType.SEDAN, JAN_10, 1));
            assertFalse(service.isAvailable(CarType.SEDAN, JAN_10, 1));
        }

        @Test
        @DisplayName("A type with zero cars can never be reserved")
        void zeroCapacityTypeCannotBeReserved() {
            Fleet noVans = Fleet.withCapacity(Map.of(CarType.SEDAN, 1));
            CarRentalService s = new CarRentalService(noVans, new InMemoryReservationRepository(), FIXED_CLOCK);

            assertThrows(NoCarAvailableException.class, () -> s.reserve(CarType.VAN, JAN_10, 1));
        }
    }

    // ------------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Cancelling a reservation")
    class Cancelling {

        @Test
        @DisplayName("Cancelling frees the car for the same dates")
        void cancelFreesCar() {
            Reservation r = service.reserve(CarType.SUV, JAN_10, 3);
            assertFalse(service.isAvailable(CarType.SUV, JAN_10, 3));

            service.cancel(r.getId());

            assertTrue(service.isAvailable(CarType.SUV, JAN_10, 3));
            assertTrue(service.findReservation(r.getId()).isEmpty());
            assertDoesNotThrow(() -> service.reserve(CarType.SUV, JAN_10, 3));
        }

        @Test
        @DisplayName("Cancelling an unknown id fails")
        void cancelUnknownIdFails() {
            assertThrows(ReservationNotFoundException.class, () -> service.cancel("R-404"));
        }

        @Test
        @DisplayName("Cancelling the same reservation twice fails the second time")
        void cancelTwiceFails() {
            Reservation r = service.reserve(CarType.VAN, JAN_10, 1);
            service.cancel(r.getId());
            assertThrows(ReservationNotFoundException.class, () -> service.cancel(r.getId()));
        }
    }

    // ------------------------------------------------------------------
    // Input validation
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("Zero days is rejected")
        void zeroDaysRejected() {
            assertThrows(InvalidReservationException.class, () -> service.reserve(CarType.SEDAN, JAN_10, 0));
        }

        @Test
        @DisplayName("Negative days is rejected")
        void negativeDaysRejected() {
            assertThrows(InvalidReservationException.class, () -> service.reserve(CarType.SEDAN, JAN_10, -2));
        }

        @Test
        @DisplayName("A start time in the past is rejected")
        void pastStartRejected() {
            assertThrows(InvalidReservationException.class,
                    () -> service.reserve(CarType.SEDAN, NOW.minusMinutes(1), 1));
        }

        @Test
        @DisplayName("Missing car type is rejected")
        void nullTypeRejected() {
            assertThrows(InvalidReservationException.class, () -> service.reserve(null, JAN_10, 1));
        }

        @Test
        @DisplayName("Missing start time is rejected")
        void nullStartRejected() {
            assertThrows(InvalidReservationException.class, () -> service.reserve(CarType.SEDAN, null, 1));
        }

        @Test
        @DisplayName("Invalid requests do not create a reservation")
        void invalidRequestsLeaveNoTrace() {
            assertThrows(InvalidReservationException.class, () -> service.reserve(CarType.SEDAN, JAN_10, 0));
            assertTrue(service.allReservations().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // Concurrency
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Concurrent requests")
    class Concurrency {

        @Test
        @DisplayName("Many simultaneous requests never overbook the fleet")
        void simultaneousRequestsDoNotOverbook() throws Exception {
            int threads = 20;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch startGun = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                Callable<Boolean> attempt = () -> {
                    startGun.await();
                    try {
                        service.reserve(CarType.SEDAN, JAN_10, 2);
                        return true;
                    } catch (NoCarAvailableException e) {
                        return false;
                    }
                };
                results.add(pool.submit(attempt));
            }
            startGun.countDown();

            int successes = 0;
            for (Future<Boolean> f : results) {
                if (f.get(5, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            pool.shutdown();

            assertEquals(2, successes, "only the 2 sedans in the fleet may be booked");
            assertEquals(2, service.allReservations().size());
        }
    }
}
