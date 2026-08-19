package com.pitstop.garage.car.vin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VinDecodeResultTest {

    @Test
    void recordExposesBrandModelYear() {
        VinDecodeResult result = new VinDecodeResult("HONDA", "Accord", 2003);

        assertEquals("HONDA", result.brand());
        assertEquals("Accord", result.model());
        assertEquals(2003, result.year());
    }
}
