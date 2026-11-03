package com.example.carrental.fleet;

import com.example.carrental.model.Car;
import com.example.carrental.model.CarType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetTest {

    @Test
    @DisplayName("Creates the configured number of cars for each of the three types")
    void createsConfiguredCapacity() {
        Fleet fleet = Fleet.withCapacity(Map.of(CarType.SEDAN, 3, CarType.SUV, 2, CarType.VAN, 1));

        assertEquals(3, fleet.capacity(CarType.SEDAN));
        assertEquals(2, fleet.capacity(CarType.SUV));
        assertEquals(1, fleet.capacity(CarType.VAN));
    }

    @Test
    @DisplayName("Every car has the right type and a unique id")
    void carsHaveCorrectTypeAndUniqueIds() {
        Fleet fleet = Fleet.withCapacity(Map.of(CarType.SEDAN, 2));
        List<Car> sedans = fleet.carsOfType(CarType.SEDAN);

        assertTrue(sedans.stream().allMatch(c -> c.type() == CarType.SEDAN));
        assertEquals(2, sedans.stream().map(Car::id).distinct().count());
    }

    @Test
    @DisplayName("Types not configured have zero cars")
    void missingTypeHasZeroCars() {
        Fleet fleet = Fleet.withCapacity(Map.of(CarType.SEDAN, 1));
        assertEquals(0, fleet.capacity(CarType.VAN));
    }

    @Test
    @DisplayName("Fleet cannot be modified after creation")
    void fleetIsImmutable() {
        Fleet fleet = Fleet.withCapacity(Map.of(CarType.SUV, 1));
        assertThrows(UnsupportedOperationException.class,
                () -> fleet.carsOfType(CarType.SUV).add(new Car("SUV-99", CarType.SUV)));
    }

    @Test
    @DisplayName("Negative capacity is rejected")
    void negativeCapacityRejected() {
        assertThrows(IllegalArgumentException.class, () -> Fleet.withCapacity(Map.of(CarType.VAN, -1)));
    }
}
