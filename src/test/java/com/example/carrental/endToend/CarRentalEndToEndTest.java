package com.example.carrental.endToend;

import com.example.carrental.exception.NoCarAvailableException;
import com.example.carrental.fleet.Fleet;
import com.example.carrental.model.CarType;
import com.example.carrental.model.Reservation;
import com.example.carrental.repository.InMemoryReservationRepository;
import com.example.carrental.service.CarRentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CarRentalEndToEndTest {

    private CarRentalService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // Initialize fleet with 2 SUVs for testing purposes
        Fleet fleet = Fleet.withCapacity(Map.of(
                CarType.SEDAN, 2,
                CarType.SUV, 2,
                CarType.VAN, 2
        ));

        service = new CarRentalService(
                fleet, 
                new InMemoryReservationRepository(), 
                Clock.systemDefaultZone()
        );

        // Start time configured strictly from current time (truncated for clean calculations)
        now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void testMainFlowEndToEnd() throws NoCarAvailableException {
    	// 1. First Reservation (SUV starting now for 3 days)
        Reservation tempFirst = null;
        LocalDateTime suggestedTime = LocalDateTime.now().plusMinutes(10);
        try {
            tempFirst = service.reserve(CarType.SUV, suggestedTime, 3);
            assertNotNull(tempFirst, "First reservation should succeed");
        } catch (NoCarAvailableException e) {
            fail("First reservation failed unexpectedly: " + e.getMessage());
        }
        
        // Declare as effectively final for subsequent block uses
        final Reservation first = tempFirst;
        // 2. Second Reservation (SUV starting 1 day from now for 2 days)
        Reservation second = null;
        try {
            second = service.reserve(CarType.SUV, now.plusDays(1), 2);
            assertNotNull(second, "Second reservation should succeed");
        } catch (NoCarAvailableException e) {
            fail("Second reservation failed unexpectedly: " + e.getMessage());
        }

        // 3. Back-to-Back Reservation (Starts exactly when the first reservation ends)
        Reservation backToBack = null;
        try {
            backToBack = service.reserve(CarType.SUV, first.getEnd(), 2);
            assertNotNull(backToBack, "Back-to-back reservation should succeed because the first one frees up right at end time");
        } catch (NoCarAvailableException e) {
            fail("Back-to-back reservation failed unexpectedly: " + e.getMessage());
        }

        // 4. Cancel First Reservation
        assertDoesNotThrow(() -> {
            service.cancel(first.getId());
        }, "Cancellation of the first reservation should succeed");

        // 5. Rebooked Reservation (Overlaps with original timeframe of the cancelled first reservation)
        Reservation rebooked = null;
        try {
            rebooked = service.reserve(CarType.SUV, now.plusDays(1), 2);
            assertNotNull(rebooked, "Rebooked reservation should now succeed since the first one was cancelled");
        } catch (NoCarAvailableException e) {
            fail("Rebooked reservation failed unexpectedly: " + e.getMessage());
        }

        // 6. Book Another SUV further down the line
        Reservation anotherReservation = null;
        try {
            anotherReservation = service.reserve(CarType.SUV, now.plusDays(5), 5);
            assertNotNull(anotherReservation, "Long-term future reservation should succeed");
        } catch (NoCarAvailableException e) {
            fail("Additional reservation failed unexpectedly: " + e.getMessage());
        }
    }
}