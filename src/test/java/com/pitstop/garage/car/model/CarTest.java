package com.pitstop.garage.car.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarTest {

    @Test
    void isDeleted_whenDeletedAtSet_returnsTrue() {
        Car car = Car.builder().deletedAt(LocalDateTime.now()).build();
        assertTrue(car.isDeleted());
    }

    @Test
    void isDeleted_whenDeletedAtNull_returnsFalse() {
        Car car = Car.builder().build();
        assertFalse(car.isDeleted());
    }
}
