package com.example.carrental.fleet;

import com.example.carrental.model.Car;
import com.example.carrental.model.CarType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The fixed set of cars the agency owns, grouped by type.
 *
 * <p>The fleet is immutable once built, so the number of cars of each type is limited
 * to what was configured at construction time.
 */
public final class Fleet {

    private final Map<CarType, List<Car>> carsByType;

    private Fleet(Map<CarType, List<Car>> carsByType) {
        this.carsByType = carsByType;
    }

    /**
     * Builds a fleet with the given number of cars per type. Types that are not
     * mentioned get zero cars. Car ids are generated as TYPE-n, e.g. SEDAN-1.
     */
    public static Fleet withCapacity(Map<CarType, Integer> capacity) {
        Objects.requireNonNull(capacity, "capacity must not be null");
        Map<CarType, List<Car>> cars = new EnumMap<>(CarType.class);
        for (CarType type : CarType.values()) {
            int count = capacity.getOrDefault(type, 0);
            if (count < 0) {
                throw new IllegalArgumentException("capacity for " + type + " must not be negative");
            }
            List<Car> list = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                list.add(new Car(type.name() + "-" + i, type));
            }
            cars.put(type, Collections.unmodifiableList(list));
        }
        
        return new Fleet(Collections.unmodifiableMap(cars));
    }

    /** All cars of the given type, in a stable order. */
    public List<Car> carsOfType(CarType type) {
        Objects.requireNonNull(type, "type must not be null");
        return carsByType.get(type);
    }

    /** How many cars of the given type the agency owns. */
    public int capacity(CarType type) {
        return carsOfType(type).size();
    }
}
