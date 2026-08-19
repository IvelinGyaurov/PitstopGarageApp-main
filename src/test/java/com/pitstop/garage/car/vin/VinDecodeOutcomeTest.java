package com.pitstop.garage.car.vin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VinDecodeOutcomeTest {

    @Test
    void success_returnsSuccessFlashAndMessageKey() {
        VinDecodeOutcome outcome = VinDecodeOutcome.success();

        assertEquals("successMessage", outcome.flashAttribute());
        assertEquals("flash.car.vinDecoded", outcome.messageKey());
    }

    @Test
    void failed_returnsErrorFlashAndMessageKey() {
        VinDecodeOutcome outcome = VinDecodeOutcome.failed();

        assertEquals("errorMessage", outcome.flashAttribute());
        assertEquals("flash.car.vinDecodeFailed", outcome.messageKey());
    }

    @Test
    void unavailable_returnsErrorFlashAndMessageKey() {
        VinDecodeOutcome outcome = VinDecodeOutcome.unavailable();

        assertEquals("errorMessage", outcome.flashAttribute());
        assertEquals("flash.car.vinDecodeUnavailable", outcome.messageKey());
    }
}
