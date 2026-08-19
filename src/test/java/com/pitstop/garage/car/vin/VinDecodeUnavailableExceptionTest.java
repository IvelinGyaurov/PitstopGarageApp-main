package com.pitstop.garage.car.vin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class VinDecodeUnavailableExceptionTest {

    @Test
    void wrapsCause() {
        RuntimeException cause = new RuntimeException("timeout");

        VinDecodeUnavailableException exception = new VinDecodeUnavailableException(cause);

        assertSame(cause, exception.getCause());
    }
}
