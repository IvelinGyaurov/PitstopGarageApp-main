package com.pitstop.garage.repair.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class RepairCompletedEventListenerTest {

    @InjectMocks
    private RepairCompletedEventListener listener;

    @Test
    void onRepairCompleted_logsWithoutError() {
        RepairCompletedEvent event = new RepairCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                2,
                LocalDateTime.now());

        assertDoesNotThrow(() -> listener.onRepairCompleted(event));
    }
}
